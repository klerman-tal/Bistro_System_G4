package logicControllers;

import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.SpecialOpeningHours_DB_Controller;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.SpecialOpeningHours;
import entities.Table;

import java.sql.SQLException;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Provides restaurant-level business logic for tables, opening hours, and
 * availability management.
 * <p>
 * This controller acts as a logic layer above {@link Restaurant_DB_Controller}
 * and coordinates:
 * <ul>
 * <li>Loading, saving, and removing {@link Table} entities (including cache
 * updates)</li>
 * <li>Managing weekly opening hours and date-specific overrides (special
 * opening hours)</li>
 * <li>Building and synchronizing the availability grid for the next 30
 * days</li>
 * <li>Querying and reserving availability slots for reservation operations</li>
 * </ul>
 * </p>
 * <p>
 * The controller maintains a reference to the {@link Restaurant} singleton as
 * an in-memory cache for relatively stable data (e.g., tables and opening
 * hours).
 * </p>
 */
public class RestaurantController {

	private final Restaurant_DB_Controller db;
	private final Restaurant restaurant;
	private ReservationController reservationController;
	private Reservation_DB_Controller reservationDB;

	/**
	 * Sets the reservation DB controller dependency after construction.
	 *
	 * @param reservationDB database controller used for reservation-related
	 *                      persistence
	 */
	public void setReservationDB(Reservation_DB_Controller reservationDB) {
		this.reservationDB = reservationDB;

	}

	/**
	 * Sets the reservation controller dependency after construction.
	 *
	 * @param reservationController reservation logic controller used for
	 *                              reservation-related operations
	 */
	public void setReservationController(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	private SpecialOpeningHours_DB_Controller specialDB;

	/**
	 * Constructs a RestaurantController with the given database controller.
	 *
	 * @param db database controller used for restaurant tables, opening hours, and
	 *           availability grid persistence
	 */
	public RestaurantController(Restaurant_DB_Controller db) {
		this.db = db;
		this.restaurant = Restaurant.getInstance();
	}

	/**
	 * Sets the special opening hours DB controller after construction.
	 *
	 * @param specialDB database controller used for date-specific opening hours
	 *                  overrides
	 */
	public void setSpecialOpeningHoursDB(SpecialOpeningHours_DB_Controller specialDB) {
		this.specialDB = specialDB;
	}

	// ====TABLES====

	/**
	 * Loads tables from the database into the {@link Restaurant} cache.
	 *
	 * @throws SQLException if the database query fails
	 */
	public void loadTablesFromDb() throws SQLException {
		db.loadTables();
	}

	/**
	 * Saves a new table or updates an existing table, both in the cache and in the
	 * database.
	 * <p>
	 * After saving, the availability grid schema is ensured to match the current
	 * sorted table list.
	 * </p>
	 *
	 * @param t table to save or update
	 * @throws SQLException if a database operation fails
	 */
	public void saveOrUpdateTable(Table t) throws SQLException {
		if (t == null)
			return;

		if (restaurant.getTables() == null) {
			restaurant.setTables(new ArrayList<>());
		}

		Table existing = null;
		for (Table x : restaurant.getTables()) {
			if (x.getTableNumber() == t.getTableNumber()) {
				existing = x;
				break;
			}
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

	/**
	 * Removes a table from the system and updates the availability grid schema
	 * accordingly.
	 * <p>
	 * If a {@link ReservationController} is available, upcoming reservations for
	 * the deleted table are first relocated or cancelled to ensure consistency
	 * before the deletion occurs.
	 * </p>
	 *
	 * @param tableNumber table number to remove
	 * @return {@code true} if the table was deleted successfully, {@code false}
	 *         otherwise
	 * @throws SQLException if a database operation fails
	 */
	public boolean removeTable(int tableNumber) throws SQLException {
		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		if (reservationController != null) {
			reservationController.relocateOrCancelReservationsForDeletedTable(tableNumber, 30);
		}

		boolean deleted = db.deleteTable(tableNumber);
		if (!deleted)
			return false;

		Table toRemove = null;
		for (Table t : restaurant.getTables()) {
			if (t.getTableNumber() == tableNumber) {
				toRemove = t;
				break;
			}
		}
		if (toRemove != null)
			restaurant.getTables().remove(toRemove);

		db.dropTableColumnFromGrid(tableNumber);

		return true;
	}

	/**
	 * Finds one available table for a given slot while excluding a specific table
	 * number.
	 *
	 * @param slot               reservation slot date-time
	 * @param peopleCount        number of guests
	 * @param excludeTableNumber table number to exclude from the search
	 * @return an available {@link Table} or {@code null} if none is available
	 * @throws Exception if database access or availability checks fail
	 */
	public Table getOneAvailableTableAtExcluding(LocalDateTime slot, int peopleCount, int excludeTableNumber)
			throws Exception {
		if (slot == null || peopleCount <= 0)
			return null;

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		List<Table> candidates = new ArrayList<>();
		for (Table t : tables) {
			if (t.getTableNumber() == excludeTableNumber)
				continue;
			if (t.getSeatsAmount() >= peopleCount)
				candidates.add(t);
		}
		if (candidates.isEmpty())
			return null;

		Integer freeTableNumber = db.findOneFreeTableNumberAtSlot(slot, candidates);
		if (freeTableNumber == null)
			return null;

		for (Table t : candidates) {
			if (t.getTableNumber() == freeTableNumber)
				return t;
		}
		return null;
	}

	// ====OPENING HOURS====

	/**
	 * Loads weekly opening hours from the database into the {@link Restaurant}
	 * cache.
	 *
	 * @throws SQLException if the database query fails
	 */
	public void loadOpeningHoursFromDb() throws SQLException {
		db.loadOpeningHours();
	}

	/**
	 * Updates weekly opening hours in the database and refreshes the cache.
	 *
	 * @param oh opening-hours record to update
	 * @throws SQLException if a database operation fails
	 */
	public void updateOpeningHours(OpeningHouers oh) throws SQLException {
		if (oh == null)
			return;
		db.updateOpeningHours(oh);
		loadOpeningHoursFromDb();
	}

	/**
	 * Retrieves weekly opening hours from the cache, refreshing it from the
	 * database first.
	 *
	 * @return a copy of the opening hours list (never {@code null})
	 * @throws SQLException if loading opening hours fails
	 */
	public ArrayList<OpeningHouers> getOpeningHours() throws SQLException {
		loadOpeningHoursFromDb();

		ArrayList<OpeningHouers> list = restaurant.getOpeningHours();
		if (list == null)
			return new ArrayList<>();
		return new ArrayList<>(list);
	}

	/**
	 * Finds weekly opening hours for the given date based on the day of week
	 * (English full name).
	 *
	 * @param date target date
	 * @return matching {@link OpeningHouers}, or {@code null} if not found
	 */
	private OpeningHouers findOpeningHoursForDate(LocalDate date) {
		String fullEn = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

		if (restaurant.getOpeningHours() == null)
			return null;

		for (OpeningHouers oh : restaurant.getOpeningHours()) {
			if (oh == null || oh.getDayOfWeek() == null)
				continue;
			if (oh.getDayOfWeek().trim().equalsIgnoreCase(fullEn)) {
				return oh;
			}
		}
		return null;
	}

	/**
	 * Resolves the effective opening hours for a specific date.
	 * <p>
	 * The resolution order:
	 * <ol>
	 * <li>If a special opening-hours override exists for the date, it is used.</li>
	 * <li>Otherwise, the weekly opening hours for that weekday are used.</li>
	 * </ol>
	 * </p>
	 * <p>
	 * When the date is marked as closed in special opening hours, the returned
	 * {@link OpeningHouers} contains {@code null} open/close times.
	 * </p>
	 *
	 * @param date target date
	 * @return effective {@link OpeningHouers} for that date, or {@code null} if no
	 *         data exists
	 */
	public OpeningHouers getEffectiveOpeningHoursForDate(LocalDate date) {

		if (date == null)
			return null;

		if (specialDB != null) {
			try {
				SpecialOpeningHours sp = specialDB.getSpecialHoursByDate(date);
				if (sp != null) {
					OpeningHouers oh = new OpeningHouers();
					oh.setDayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

					if (sp.isClosed()) {
						oh.setOpenTime(null);
						oh.setCloseTime(null);
					} else {
						oh.setOpenTime(sp.getOpenTime() == null ? null : sp.getOpenTime().toString());
						oh.setCloseTime(sp.getCloseTime() == null ? null : sp.getCloseTime().toString());
					}
					return oh;
				}
			} catch (Exception e) {
			}
		}

		return findOpeningHoursForDate(date);
	}

	// ====AVAILABILITY GRID====

	/**
	 * Initializes the availability grid for the next 30 days.
	 * <p>
	 * The method ensures table schema is aligned, deletes past slots, and then
	 * initializes new slots using the effective opening hours for each date.
	 * </p>
	 *
	 * @throws Exception if loading data or grid initialization fails
	 */
	public void initAvailabilityGridNext30Days() throws Exception {
		loadTablesFromDb();
		loadOpeningHoursFromDb();

		List<Table> tables = getSortedTablesEnsured();

		db.ensureAvailabilityGridSchema(tables);
		db.deletePastSlots();

		LocalDate start = LocalDate.now();
		db.initGridForNextDays(start, 30, this::getEffectiveOpeningHoursForDate);
	}

	/**
	 * Rebuilds availability grid slots for a specific date based on effective
	 * opening hours.
	 * <p>
	 * Existing grid slots for the date are deleted first, then the grid is
	 * re-initialized. If the effective hours indicate the date is closed, the grid
	 * remains empty for that date.
	 * </p>
	 *
	 * @param date date to synchronize
	 * @throws Exception if schema validation or database operations fail
	 */
	public void syncGridForSpecialDate(LocalDate date) throws Exception {
		if (date == null)
			return;

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		db.deleteGridSlotsForDate(date);

		OpeningHouers oh = getEffectiveOpeningHoursForDate(date);
		if (oh == null)
			return;

		String openStr = safeHHMM(oh.getOpenTime());
		String closeStr = safeHHMM(oh.getCloseTime());
		if (openStr.isBlank() || closeStr.isBlank()) {
			return;
		}

		LocalTime open = LocalTime.parse(openStr);
		LocalTime close = LocalTime.parse(closeStr);

		db.initGridForDate(date, open, close);
	}

	/**
	 * Retrieves a serialized grid payload for a specific date from the database.
	 *
	 * @param date target date
	 * @return grid payload string for the date
	 * @throws Exception if schema validation or payload retrieval fails
	 */
	public String getGridFromDbPayload(LocalDate date) throws Exception {
		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);
		return db.getGridPayloadForDate(date, tables);
	}

	/**
	 * Finds one available table for a given slot and guest count.
	 *
	 * @param slot        reservation slot date-time
	 * @param peopleCount number of guests
	 * @return an available {@link Table} or {@code null} if none is available
	 * @throws Exception if database access or availability checks fail
	 */
	public Table getOneAvailableTableAt(LocalDateTime slot, int peopleCount) throws Exception {
		if (slot == null)
			return null;
		if (peopleCount <= 0)
			return null;

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		List<Table> candidates = new ArrayList<>();
		for (Table t : tables) {
			if (t.getSeatsAmount() >= peopleCount) {
				candidates.add(t);
			}
		}
		if (candidates.isEmpty())
			return null;

		Integer freeTableNumber = db.findOneFreeTableNumberAtSlot(slot, candidates);
		if (freeTableNumber == null)
			return null;

		for (Table t : candidates) {
			if (t.getTableNumber() == freeTableNumber)
				return t;
		}
		return null;
	}

	/**
	 * Finds one available table for a given slot while excluding a list of table
	 * numbers.
	 *
	 * @param slot                 reservation slot date-time
	 * @param peopleCount          number of guests
	 * @param excludedTableNumbers list of table numbers to exclude (may be
	 *                             {@code null})
	 * @return an available {@link Table} or {@code null} if none is available
	 * @throws Exception if database access or availability checks fail
	 */
	public Table getOneAvailableTableAtExcludingTables(LocalDateTime slot, int peopleCount,
			ArrayList<Integer> excludedTableNumbers) throws Exception {
		if (slot == null)
			return null;
		if (peopleCount <= 0)
			return null;

		Set<Integer> excluded = new HashSet<>();
		if (excludedTableNumbers != null)
			excluded.addAll(excludedTableNumbers);

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		List<Table> candidates = new ArrayList<>();
		for (Table t : tables) {
			if (t.getSeatsAmount() >= peopleCount && !excluded.contains(t.getTableNumber())) {
				candidates.add(t);
			}
		}
		if (candidates.isEmpty())
			return null;

		Integer freeTableNumber = db.findOneFreeTableNumberAtSlot(slot, candidates);
		if (freeTableNumber == null)
			return null;

		for (Table t : candidates) {
			if (t.getTableNumber() == freeTableNumber)
				return t;
		}
		return null;
	}

	/**
	 * Returns a mapping of available reservation slots to one available table per
	 * slot for a given date.
	 *
	 * @param date        target date
	 * @param peopleCount number of guests
	 * @return map from slot time to an available table for that slot (may be empty)
	 * @throws Exception if database access or availability checks fail
	 */
	public Map<LocalDateTime, Table> getOneAvailableTablePerSlot(LocalDate date, int peopleCount) throws Exception {
		Map<LocalDateTime, Table> result = new LinkedHashMap<>();
		if (date == null)
			return result;
		if (peopleCount <= 0)
			return result;

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		List<Table> candidates = new ArrayList<>();
		for (Table t : tables) {
			if (t.getSeatsAmount() >= peopleCount) {
				candidates.add(t);
			}
		}
		if (candidates.isEmpty())
			return result;

		Map<LocalDateTime, Integer> chosenNumbers = db.findOneFreeTableNumberPerSlotForDate(date, candidates);

		Map<Integer, Table> byNumber = new HashMap<>();
		for (Table t : candidates)
			byNumber.put(t.getTableNumber(), t);

		for (Map.Entry<LocalDateTime, Integer> e : chosenNumbers.entrySet()) {
			Table chosen = byNumber.get(e.getValue());
			if (chosen != null)
				result.put(e.getKey(), chosen);
		}

		return result;
	}

	/**
	 * Attempts to reserve a specific slot for a specific table.
	 *
	 * @param slot        slot date-time to reserve
	 * @param tableNumber table number to reserve
	 * @return {@code true} if the slot was reserved successfully, {@code false}
	 *         otherwise
	 * @throws Exception if slot is invalid, table does not exist, or a database
	 *                   operation fails
	 */
	public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws Exception {
		if (slot == null)
			throw new IllegalArgumentException("slot is null");

		List<Table> tables = getSortedTablesEnsured();
		boolean exists = tables.stream().anyMatch(t -> t.getTableNumber() == tableNumber);
		if (!exists)
			throw new IllegalArgumentException("Table number not found: " + tableNumber);

		return db.tryReserveSlot(slot, tableNumber);
	}

	/**
	 * Releases (marks available) a specific slot for a specific table.
	 *
	 * @param slot        slot date-time to release
	 * @param tableNumber table number to mark as available
	 * @return {@code true} if the slot was marked available successfully,
	 *         {@code false} otherwise
	 * @throws Exception if slot is invalid or a database operation fails
	 */
	public boolean releaseSlot(LocalDateTime slot, int tableNumber) throws Exception {
		if (slot == null)
			throw new IllegalArgumentException("slot is null");
		getSortedTablesEnsured();
		return db.setTableAvailability(slot, tableNumber, true);
	}

	// ====Helpers====

	/**
	 * Returns a sorted table list, ensuring tables are loaded into cache first.
	 *
	 * @return sorted list of tables by table number
	 * @throws SQLException if loading tables from DB fails
	 */
	private List<Table> getSortedTablesEnsured() throws SQLException {
		if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) {
			loadTablesFromDb();
		}
		List<Table> tables = new ArrayList<>(restaurant.getTables());
		tables.sort(Comparator.comparingInt(Table::getTableNumber));
		return tables;
	}

	/**
	 * Checks whether a specific table is free for a full two-hour window starting
	 * at {@code start}.
	 * <p>
	 * A two-hour window is represented by 4 consecutive 30-minute slots.
	 * </p>
	 *
	 * @param start       start date-time of the desired reservation
	 * @param tableNumber target table number
	 * @return {@code true} if the table is free for all 4 slots, {@code false}
	 *         otherwise
	 * @throws Exception if schema validation or database access fails
	 */
	public boolean isTableFreeForTwoHours(LocalDateTime start, int tableNumber) throws Exception {
		if (start == null)
			return false;

		List<Table> tables = getSortedTablesEnsured();
		db.ensureAvailabilityGridSchema(tables);

		for (int i = 0; i < 4; i++) {
			LocalDateTime slot = start.plusMinutes(30L * i);
			if (!db.isTableFreeAtSlot(slot, tableNumber))
				return false;
		}
		return true;
	}

	/**
	 * Retrieves all tables from the cache (loading from DB if needed).
	 *
	 * @return list of all tables
	 * @throws SQLException if loading tables fails
	 */
	public ArrayList<Table> getAllTables() throws SQLException {
		if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) {
			loadTablesFromDb();
		}
		return new ArrayList<>(restaurant.getTables());
	}

	/**
	 * Rounds a date-time up to the next half-hour boundary.
	 *
	 * @param dt input date-time
	 * @return rounded date-time, or {@code null} if input is {@code null}
	 */
	public LocalDateTime roundUpToNextHalfHour(LocalDateTime dt) {
		if (dt == null)
			return null;

		int minute = dt.getMinute();
		int mod = minute % 30;

		if (mod == 0 && dt.getSecond() == 0 && dt.getNano() == 0) {
			return dt;
		}

		int add = 30 - mod;
		LocalDateTime rounded = dt.plusMinutes(add);
		return rounded.withSecond(0).withNano(0);
	}

	/**
	 * Normalizes a time string into {@code HH:MM} form.
	 * <p>
	 * Supports time inputs in {@code HH:MM:SS} or {@code HH:MM} formats. Returns an
	 * empty string if the input is {@code null} or does not match supported
	 * formats.
	 * </p>
	 *
	 * @param t time string to normalize
	 * @return normalized time string in {@code HH:MM}, or empty string on invalid
	 *         input
	 */
	private String safeHHMM(String t) {
		if (t == null)
			return "";
		t = t.trim();
		if (t.matches("^\\d{2}:\\d{2}:\\d{2}$"))
			return t.substring(0, 5);
		if (t.matches("^\\d{2}:\\d{2}$"))
			return t;
		return t.length() >= 5 ? t.substring(0, 5) : "";
	}

	public OpeningHouers getEffectiveOpeningHoursForDatePublic(LocalDate date) {
		return getEffectiveOpeningHoursForDate(date);
	}

	/**
	 * Returns all dates in the next 30 days (including today) that match the given
	 * weekday.
	 * <p>
	 * The input is expected to be a weekday name supported by
	 * {@link DayOfWeek#valueOf(String)} after uppercasing (e.g.,
	 * {@code "Thursday"}).
	 * </p>
	 *
	 * @param dayOfWeekEn weekday name in English (e.g., {@code "Thursday"})
	 * @return list of matching dates within the next 30 days (including today)
	 */
	public List<LocalDate> getDatesForWeekdayNext30Days(String dayOfWeekEn) {

		List<LocalDate> out = new ArrayList<>();
		if (dayOfWeekEn == null || dayOfWeekEn.isBlank())
			return out;

		DayOfWeek target;
		try {
			target = DayOfWeek.valueOf(dayOfWeekEn.trim().toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return out;
		}

		LocalDate start = LocalDate.now();
		LocalDate end = start.plusDays(30);

		for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
			if (d.getDayOfWeek() == target)
				out.add(d);
		}

		return out;
	}

	/**
	 * Normalizes a time string into {@code HH:MM} form.
	 * <p>
	 * Supports time inputs in {@code HH:MM:SS} or {@code HH:MM} formats.
	 * </p>
	 *
	 * @param t time string to normalize
	 * @return normalized time string in {@code HH:MM}, or empty string on invalid
	 *         input
	 */
	public String toHHMM(String t) {
		if (t == null)
			return "";
		t = t.trim();
		if (t.matches("^\\d{2}:\\d{2}:\\d{2}$"))
			return t.substring(0, 5);
		if (t.matches("^\\d{2}:\\d{2}$"))
			return t;
		return t.length() >= 5 ? t.substring(0, 5) : "";
	}

}
