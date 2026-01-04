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

    public RestaurantController(Restaurant_DB_Controller db) {
        this.db = db;
        this.restaurant = Restaurant.getInstance();
    }

    // =========================
    // TABLES
    // =========================

    public void loadTablesFromDb() throws SQLException {
        db.loadTables();
    }

    public boolean removeTable(int tableNumber) throws SQLException {
        Table toRemove = null;
        for (Table t : restaurant.getTables()) {
            if (t.getTableNumber() == tableNumber) { toRemove = t; break; }
        }
        if (toRemove == null) return false;

        restaurant.getTables().remove(toRemove);
        return db.deleteTable(tableNumber);
    }

    public void saveOrUpdateTable(Table t) throws SQLException {
        if (t == null) return;

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
    }

    // =========================
    // OPENING HOURS
    // =========================

    public void loadOpeningHoursFromDb() throws SQLException {
        db.loadOpeningHours();
    }

    public void updateOpeningHours(OpeningHouers oh) throws SQLException {
        if (oh == null) return;
        db.updateOpeningHours(oh);
    }

    private OpeningHouers findOpeningHoursForDate(LocalDate date) {
        String fullEn = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH); // "Sunday"

        for (OpeningHouers oh : restaurant.getOpeningHours()) {
            if (oh == null || oh.getDayOfWeek() == null) continue;
            if (oh.getDayOfWeek().trim().equalsIgnoreCase(fullEn)) {
                return oh;
            }
        }
        return null;
    }

    // =========================
    // AVAILABILITY GRID (DB) - WITH ALL 4 ADDITIONS
    // =========================

    public void initAvailabilityGridNext30Days() throws Exception {
        loadTablesFromDb();
        loadOpeningHoursFromDb();

        List<Table> tables = new ArrayList<>(restaurant.getTables());
        tables.sort(Comparator.comparingInt(Table::getTableNumber));

        // ✅ ensure schema (create + ALTER missing columns)
        db.ensureAvailabilityGridSchema(tables);

        // ✅ cleanup old
        db.deletePastSlots();

        // ✅ always keep next 30 days
        LocalDate start = LocalDate.now();
        db.initGridForNextDays(start, 30, this::findOpeningHoursForDate);
    }

    public String getGridFromDbPayload(LocalDate date) throws Exception {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) loadTablesFromDb();

        List<Table> tables = new ArrayList<>(restaurant.getTables());
        tables.sort(Comparator.comparingInt(Table::getTableNumber));

        // schema safety (in case new table added)
        db.ensureAvailabilityGridSchema(tables);

        return db.getGridPayloadForDate(date, tables);
    }

    /** ✅ (4) reserve safely */
    public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) loadTablesFromDb();

        boolean exists = restaurant.getTables().stream().anyMatch(t -> t.getTableNumber() == tableNumber);
        if (!exists) throw new IllegalArgumentException("Table number not found: " + tableNumber);

        return db.tryReserveSlot(slot, tableNumber);
    }

    /** release = set to free */
    public boolean releaseSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) loadTablesFromDb();
        return db.setTableAvailability(slot, tableNumber, true);
    }
}