package logicControllers;

import dbControllers.Restaurant_DB_Controller;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.Table;

import java.sql.SQLException;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public class RestaurantController {

    private final Restaurant_DB_Controller db;
    private final Restaurant restaurant;

    /**
     * Constructor: gets DB controller and keeps Restaurant singleton reference (cache).
     */
    public RestaurantController(Restaurant_DB_Controller db) {
        this.db = db;
        this.restaurant = Restaurant.getInstance();
    }

    // =========================
    // TABLES (cache + DB)
    // =========================

    public void loadTablesFromDb() throws SQLException {
        db.loadTables();
    }

    public void saveOrUpdateTable(Table t) throws SQLException {
        if (t == null) return;

        // Ensure cache list exists
        if (restaurant.getTables() == null) {
            restaurant.setTables(new ArrayList<>());
        }

        Table existing = null;
        for (Table x : restaurant.getTables()) {
            if (x.getTableNumber() == t.getTableNumber()) { existing = x; break; }
        }

        if (existing == null) {
            restaurant.getTables().add(t);
        } else {
            existing.setSeatsAmount(t.getSeatsAmount());
        }

        db.saveTable(t);

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);
    }

    public boolean removeTable(int tableNumber) throws SQLException {
        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        Table toRemove = null;
        if (restaurant.getTables() != null) {
            for (Table t : restaurant.getTables()) {
                if (t.getTableNumber() == tableNumber) { toRemove = t; break; }
            }
        }
        if (toRemove == null) return false;

        boolean deleted = db.deleteTable(tableNumber);
        if (!deleted) return false;

        restaurant.getTables().remove(toRemove);

        List<Table> remaining = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(remaining);

        return true;
    }

    // =========================
    // OPENING HOURS (cache + DB)
    // =========================

    public void loadOpeningHoursFromDb() throws SQLException {
        db.loadOpeningHours();
    }

    public void updateOpeningHours(OpeningHouers oh) throws SQLException {
        if (oh == null) return;
        db.updateOpeningHours(oh);
        loadOpeningHoursFromDb();
    }

    /**
     * ✅ NEW: Returns opening hours list for the client.
     * Ensures cache is loaded from DB first.
     */
    public ArrayList<OpeningHouers> getOpeningHours() throws SQLException {
        loadOpeningHoursFromDb();

        ArrayList<OpeningHouers> list = restaurant.getOpeningHours();
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list); // return copy (safe)
    }

    private OpeningHouers findOpeningHoursForDate(LocalDate date) {
        String fullEn = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH); // "Sunday"

        if (restaurant.getOpeningHours() == null) return null;

        for (OpeningHouers oh : restaurant.getOpeningHours()) {
            if (oh == null || oh.getDayOfWeek() == null) continue;
            if (oh.getDayOfWeek().trim().equalsIgnoreCase(fullEn)) {
                return oh;
            }
        }
        return null;
    }

    // =========================
    // AVAILABILITY GRID (DB is truth)
    // =========================

    public void initAvailabilityGridNext30Days() throws Exception {
        loadTablesFromDb();
        loadOpeningHoursFromDb();

        List<Table> tables = getSortedTablesEnsured();

        db.ensureAvailabilityGridSchema(tables);
        db.deletePastSlots();

        LocalDate start = LocalDate.now();
        db.initGridForNextDays(start, 30, this::findOpeningHoursForDate);
    }

    public String getGridFromDbPayload(LocalDate date) throws Exception {
        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);
        return db.getGridPayloadForDate(date, tables);
    }

    public Table getOneAvailableTableAt(LocalDateTime slot, int peopleCount) throws Exception {
        if (slot == null) return null;
        if (peopleCount <= 0) return null;

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        List<Table> candidates = new ArrayList<>();
        for (Table t : tables) {
            if (t.getSeatsAmount() >= peopleCount) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) return null;

        Integer freeTableNumber = db.findOneFreeTableNumberAtSlot(slot, candidates);
        if (freeTableNumber == null) return null;

        for (Table t : candidates) {
            if (t.getTableNumber() == freeTableNumber) return t;
        }
        return null;
    }

    public Map<LocalDateTime, Table> getOneAvailableTablePerSlot(LocalDate date, int peopleCount) throws Exception {
        Map<LocalDateTime, Table> result = new LinkedHashMap<>();
        if (date == null) return result;
        if (peopleCount <= 0) return result;

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        List<Table> candidates = new ArrayList<>();
        for (Table t : tables) {
            if (t.getSeatsAmount() >= peopleCount) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) return result;

        Map<LocalDateTime, Integer> chosenNumbers =
                db.findOneFreeTableNumberPerSlotForDate(date, candidates);

        Map<Integer, Table> byNumber = new HashMap<>();
        for (Table t : candidates) byNumber.put(t.getTableNumber(), t);

        for (Map.Entry<LocalDateTime, Integer> e : chosenNumbers.entrySet()) {
            Table chosen = byNumber.get(e.getValue());
            if (chosen != null) result.put(e.getKey(), chosen);
        }

        return result;
    }

    public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (slot == null) throw new IllegalArgumentException("slot is null");

        List<Table> tables = getSortedTablesEnsured();
        boolean exists = tables.stream().anyMatch(t -> t.getTableNumber() == tableNumber);
        if (!exists) throw new IllegalArgumentException("Table number not found: " + tableNumber);

        return db.tryReserveSlot(slot, tableNumber);
    }

    public boolean releaseSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (slot == null) throw new IllegalArgumentException("slot is null");
        getSortedTablesEnsured();
        return db.setTableAvailability(slot, tableNumber, true);
    }

    // =========================
    // Helpers
    // =========================

    private List<Table> getSortedTablesEnsured() throws SQLException {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) {
            loadTablesFromDb();
        }
        List<Table> tables = new ArrayList<>(restaurant.getTables());
        tables.sort(Comparator.comparingInt(Table::getTableNumber));
        return tables;
    }

    /**
     * Checks if a specific table is free for 2 hours starting from 'start'
     * (4 slots of 30 minutes) WITHOUT reserving anything.
     */
    public boolean isTableFreeForTwoHours(LocalDateTime start, int tableNumber) throws Exception {
        if (start == null) return false;

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        for (int i = 0; i < 4; i++) {
            LocalDateTime slot = start.plusMinutes(30L * i);
            if (!db.isTableFreeAtSlot(slot, tableNumber)) return false;
        }
        return true;
    }

    public ArrayList<Table> getAllTables() throws SQLException {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) {
            loadTablesFromDb();
        }
        return new ArrayList<>(restaurant.getTables());
    }
}
