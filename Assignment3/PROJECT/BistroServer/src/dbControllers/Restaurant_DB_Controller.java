package dbControllers;

import entities.OpeningHouers;
import entities.Restaurant;
import entities.Table;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * Provides persistence operations for restaurant-related data.
 * <p>
 * This DB controller manages:
 * <ul>
 * <li>{@code restaurant_tables} (tables metadata: number + seats)</li>
 * <li>{@code openinghours} (weekly opening hours per day)</li>
 * <li>{@code table_availability_grid} (half-hour availability grid per
 * table)</li>
 * </ul>
 * It also supports schema creation/verification, seeding default opening hours
 * rows, and utility methods for converting between SQL TIME values and
 * string-based time fields.
 * </p>
 */
public class Restaurant_DB_Controller {

	private final Connection conn;

	/**
	 * Constructs a Restaurant_DB_Controller with the given JDBC connection.
	 *
	 * @param conn active JDBC connection used for restaurant persistence
	 */
	public Restaurant_DB_Controller(Connection conn) {
		this.conn = conn;
	}

	// =========================
	// CREATE TABLES (SAFE)
	// =========================

	/**
	 * Creates the {@code restaurant_tables} table if it does not already exist.
	 * <p>
	 * Schema:
	 * <ul>
	 * <li>{@code table_number} (primary key)</li>
	 * <li>{@code seats_amount}</li>
	 * </ul>
	 * </p>
	 */
	public void createRestaurantTablesTable() {
		String sql = """
				CREATE TABLE IF NOT EXISTS restaurant_tables (
				    table_number INT NOT NULL,
				    seats_amount INT NOT NULL,
				    PRIMARY KEY (table_number),
				    INDEX (seats_amount)
				);
				""";

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the {@code openinghours} table if it does not already exist and seeds
	 * default weekday rows.
	 * <p>
	 * Schema:
	 * <ul>
	 * <li>{@code dayOfWeek} (primary key, English day name, e.g.
	 * {@code "Sunday"})</li>
	 * <li>{@code openTime} (nullable)</li>
	 * <li>{@code closeTime} (nullable)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Seeding behavior: inserts all 7 English weekdays with {@code NULL} times
	 * (idempotent via {@code ON DUPLICATE KEY}).
	 * </p>
	 */
	public void createOpeningHoursTable() {
		String createSql = """
				CREATE TABLE IF NOT EXISTS openinghours (
				    dayOfWeek VARCHAR(10) NOT NULL,
				    openTime TIME NULL,
				    closeTime TIME NULL,
				    PRIMARY KEY (dayOfWeek)
				);
				""";

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(createSql);
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}

		String seedSql = """
				INSERT INTO openinghours (dayOfWeek, openTime, closeTime)
				VALUES (?, NULL, NULL)
				ON DUPLICATE KEY UPDATE dayOfWeek = dayOfWeek;
				""";

		String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

		try (PreparedStatement ps = conn.prepareStatement(seedSql)) {
			for (String d : days) {
				ps.setString(1, d);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// =========================
	// TABLES: LOAD / UPSERT / DELETE
	// =========================

	/**
	 * Loads all restaurant tables from {@code restaurant_tables} and updates the
	 * Restaurant singleton cache.
	 *
	 * @throws SQLException if a database error occurs during query execution
	 */
	public void loadTables() throws SQLException {
		createRestaurantTablesTable();
		Restaurant r = Restaurant.getInstance();

		String sql = "SELECT table_number, seats_amount FROM restaurant_tables";
		ArrayList<Table> tables = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				Table t = new Table();
				t.setTableNumber(rs.getInt("table_number"));
				t.setSeatsAmount(rs.getInt("seats_amount"));
				tables.add(t);
			}
		}

		r.setTables(tables);
	}

	/**
	 * Inserts a new table or updates an existing table in {@code restaurant_tables}
	 * (UPSERT).
	 * <p>
	 * If a row with the same {@code table_number} already exists,
	 * {@code seats_amount} is updated.
	 * </p>
	 *
	 * @param t table entity to persist
	 * @throws SQLException if a database error occurs during the update
	 */
	public void saveTable(Table t) throws SQLException {
		String sql = """
				INSERT INTO restaurant_tables (table_number, seats_amount)
				VALUES (?, ?)
				ON DUPLICATE KEY UPDATE
				  seats_amount = VALUES(seats_amount)
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, t.getTableNumber());
			ps.setInt(2, t.getSeatsAmount());
			ps.executeUpdate();
		}
	}

	// =========================
	// TABLE DELETE SAFETY (NEXT 30 DAYS)
	// =========================

	/**
	 * Checks whether a given table has at least one reserved (occupied) slot in the
	 * next {@code days} days.
	 * <p>
	 * In {@code table_availability_grid}: {@code 1 = free}, {@code 0 = reserved}.
	 * </p>
	 *
	 * @param tableNumber table identifier
	 * @param days        forward-looking window in days
	 * @return {@code true} if at least one slot is reserved, otherwise
	 *         {@code false}
	 * @throws SQLException if a database error occurs during the query
	 */
	public boolean hasReservedSlotsInNextDays(int tableNumber, int days) throws SQLException {
		String col = "t_" + tableNumber;

		String sql = "SELECT 1 " + "FROM table_availability_grid " + "WHERE slot_datetime >= NOW() "
				+ "  AND slot_datetime < (NOW() + INTERVAL ? DAY) " + "  AND " + col + " = 0 " + "LIMIT 1";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, days);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * Deletes a table from {@code restaurant_tables} by {@code table_number}.
	 * <p>
	 * This method blocks deletion if the table has reserved slots in the next 30
	 * days in the availability grid.
	 * </p>
	 *
	 * @param tableNumber table identifier
	 * @return {@code true} if the row was deleted, {@code false} if blocked or not
	 *         found
	 * @throws SQLException if a database error occurs during deletion or
	 *                      reservation-check query
	 */
	public boolean deleteTable(int tableNumber) throws SQLException {

		if (hasReservedSlotsInNextDays(tableNumber, 30)) {
			return false;
		}

		String sql = "DELETE FROM restaurant_tables WHERE table_number=?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, tableNumber);
			return ps.executeUpdate() == 1;
		}
	}

	/**
	 * Drops the availability grid column for a given table number.
	 * <p>
	 * Intended to be called only after the table deletion is allowed and executed.
	 * </p>
	 *
	 * @param tableNumber table identifier
	 * @throws SQLException if a database error occurs during schema alteration
	 */
	public void dropTableColumnFromGrid(int tableNumber) throws SQLException {
		String col = "t_" + tableNumber;
		String sql = "ALTER TABLE table_availability_grid DROP COLUMN " + col;

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		}
	}

	// =========================
	// OPENING HOURS: LOAD / UPDATE
	// =========================

	/**
	 * Loads weekly opening hours from {@code openinghours} into the Restaurant
	 * singleton cache.
	 * <p>
	 * {@code openTime}/{@code closeTime} may be {@code NULL} in the database and
	 * are read using {@link #readTimeAsString(ResultSet, String)}. Results are
	 * ordered by weekday in English.
	 * </p>
	 *
	 * @throws SQLException if a database error occurs during query execution
	 */
	public void loadOpeningHours() throws SQLException {
		createOpeningHoursTable();
		Restaurant r = Restaurant.getInstance();

		String sql = """
				SELECT dayOfWeek, openTime, closeTime
				FROM openinghours
				ORDER BY FIELD(dayOfWeek,
				    'Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'
				)
				""";

		ArrayList<OpeningHouers> hours = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				OpeningHouers oh = new OpeningHouers();
				oh.setDayOfWeek(rs.getString("dayOfWeek"));

				String openStr = readTimeAsString(rs, "openTime");
				String closeStr = readTimeAsString(rs, "closeTime");

				oh.setOpenTime(openStr);
				oh.setCloseTime(closeStr);

				hours.add(oh);
			}
		}

		r.setOpeningHours(hours);
	}

	/**
	 * Updates opening and closing time for a given weekday in {@code openinghours}.
	 * <p>
	 * If {@code openTime}/{@code closeTime} are {@code null} or blank, {@code NULL}
	 * is stored in the database. Time strings are normalized to {@code HH:mm}
	 * before saving.
	 * </p>
	 *
	 * @param oh opening hours entity containing weekday and time values
	 * @throws SQLException if a database error occurs during update execution
	 */
	public void updateOpeningHours(OpeningHouers oh) throws SQLException {
		createOpeningHoursTable();
		String sql = """
				UPDATE openinghours
				SET openTime = ?, closeTime = ?
				WHERE dayOfWeek = ?
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			setTimeParamAsHHMM(ps, 1, oh.getOpenTime());
			setTimeParamAsHHMM(ps, 2, oh.getCloseTime());
			ps.setString(3, oh.getDayOfWeek());
			ps.executeUpdate();
		}
	}

	// =========================
	// AVAILABILITY GRID (WIDE TABLE)
	// =========================

	/**
	 * Ensures {@code table_availability_grid} exists and contains columns for every
	 * table in {@code tables}.
	 * <p>
	 * Behavior:
	 * <ol>
	 * <li>Creates the grid table if missing (with current table columns)</li>
	 * <li>Reads existing columns from {@code INFORMATION_SCHEMA}</li>
	 * <li>Adds missing {@code t_<tableNumber>} columns for newly created
	 * tables</li>
	 * </ol>
	 * </p>
	 *
	 * @param tables current list of tables to ensure schema for
	 * @throws SQLException if a database error occurs during schema checks or
	 *                      updates
	 */
	public void ensureAvailabilityGridSchema(List<Table> tables) throws SQLException {
		createAvailabilityGridTableIfNotExists(tables);

		Set<String> existingCols = getExistingGridColumns();
		for (Table t : tables) {
			String col = "t_" + t.getTableNumber();
			if (!existingCols.contains(col.toLowerCase(Locale.ROOT))) {

				String alter = "ALTER TABLE table_availability_grid " + "ADD COLUMN " + col
						+ " TINYINT(1) NOT NULL DEFAULT 1";

				try (Statement stmt = conn.createStatement()) {
					stmt.execute(alter);
				}
			}
		}
	}

	/**
	 * Creates {@code table_availability_grid} if it does not exist.
	 * <p>
	 * Table structure:
	 * <ul>
	 * <li>{@code slot_datetime} is the primary key</li>
	 * <li>Each table has a column {@code t_<tableNumber>} with {@code 1 = free},
	 * {@code 0 = reserved}, default {@code 1}</li>
	 * </ul>
	 * </p>
	 *
	 * @param tables tables used to define initial grid columns
	 * @throws SQLException if a database error occurs during table creation
	 */
	private void createAvailabilityGridTableIfNotExists(List<Table> tables) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS table_availability_grid (")
				.append("slot_datetime DATETIME NOT NULL PRIMARY KEY");

		for (Table t : tables) {
			sb.append(", t_").append(t.getTableNumber()).append(" TINYINT(1) NOT NULL DEFAULT 1");
		}

		sb.append(");");

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sb.toString());
		}
	}

	/**
	 * Reads the set of column names that currently exist in
	 * {@code table_availability_grid}.
	 * <p>
	 * This is used to detect missing {@code t_<tableNumber>} columns.
	 * </p>
	 *
	 * @return set of existing column names (lowercased)
	 * @throws SQLException if a database error occurs while reading metadata
	 */
	private Set<String> getExistingGridColumns() throws SQLException {
		Set<String> cols = new HashSet<>();
		String sql = """
				SELECT COLUMN_NAME
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = 'table_availability_grid'
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
			}
		}
		return cols;
	}

	/**
	 * Deletes past slots from {@code table_availability_grid} where
	 * {@code slot_datetime < NOW()}.
	 *
	 * @throws SQLException if a database error occurs during deletion
	 */
	public void deletePastSlots() throws SQLException {
		String sql = "DELETE FROM table_availability_grid WHERE slot_datetime < NOW()";
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(sql);
		}
	}

	/**
	 * Initializes half-hour slots for a specific date within the time range
	 * {@code open..close}.
	 * <p>
	 * Only inserts {@code slot_datetime}. Table columns keep their default value
	 * ({@code 1 = free}). If no valid half-hour slots exist (close too early), the
	 * method does nothing.
	 * </p>
	 *
	 * @param date  target date
	 * @param open  opening time (inclusive)
	 * @param close closing time (exclusive boundary used to compute last start)
	 * @throws SQLException if a database error occurs during batch insertion
	 */
	public void initGridForDate(LocalDate date, LocalTime open, LocalTime close) throws SQLException {
		LocalTime lastStart = close.minusMinutes(30);
		if (lastStart.isBefore(open))
			return;

		String sql = """
				INSERT INTO table_availability_grid (slot_datetime)
				VALUES (?)
				ON DUPLICATE KEY UPDATE slot_datetime = slot_datetime
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			LocalDateTime slot = LocalDateTime.of(date, open);

			while (!slot.toLocalTime().isAfter(lastStart)) {
				ps.setTimestamp(1, Timestamp.valueOf(slot));
				ps.addBatch();
				slot = slot.plusMinutes(30);
			}
			ps.executeBatch();
		}
	}

	/**
	 * Initializes the availability grid for a forward-looking window of days
	 * starting at {@code startDate}.
	 * <p>
	 * For each date:
	 * <ul>
	 * <li>Calls {@code hoursProvider} to fetch opening hours (including any special
	 * overrides)</li>
	 * <li>If hours are missing/blank, skips the date</li>
	 * <li>Otherwise, calls
	 * {@link #initGridForDate(LocalDate, LocalTime, LocalTime)}</li>
	 * </ul>
	 * </p>
	 *
	 * @param startDate     start date (inclusive)
	 * @param days          number of days to initialize
	 * @param hoursProvider function that returns opening hours for a given date
	 * @throws SQLException if a database error occurs during initialization
	 */
	public void initGridForNextDays(LocalDate startDate, int days, Function<LocalDate, OpeningHouers> hoursProvider)
			throws SQLException {

		for (int i = 0; i < days; i++) {
			LocalDate d = startDate.plusDays(i);
			OpeningHouers oh = hoursProvider.apply(d);
			if (oh == null)
				continue;

			String openStr = safeHHMM(oh.getOpenTime());
			String closeStr = safeHHMM(oh.getCloseTime());
			if (openStr.isBlank() || closeStr.isBlank())
				continue;

			LocalTime open = LocalTime.parse(openStr);
			LocalTime close = LocalTime.parse(closeStr);

			initGridForDate(d, open, close);
		}
	}

	/**
	 * Attempts to reserve a slot for a table in a safe (concurrency-friendly)
	 * manner.
	 * <p>
	 * The update sets the table column to {@code 0} only if it is currently
	 * {@code 1}.
	 * </p>
	 *
	 * @param slot        slot timestamp
	 * @param tableNumber table identifier
	 * @return {@code true} if the reservation succeeded (slot was free),
	 *         {@code false} otherwise
	 * @throws SQLException if a database error occurs during update execution
	 */
	public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws SQLException {
		String col = "t_" + tableNumber;
		String sql = "UPDATE table_availability_grid SET " + col + " = 0 WHERE slot_datetime = ? AND " + col + " = 1";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(slot));
			return ps.executeUpdate() == 1;
		}
	}

	/**
	 * Updates a table's availability at a specific slot without checking the
	 * current value.
	 * <p>
	 * When {@code isFree = true}, the method stores {@code 1}. When
	 * {@code isFree = false}, the method stores {@code 0}.
	 * </p>
	 *
	 * @param slot        slot timestamp
	 * @param tableNumber table identifier
	 * @param isFree      availability flag
	 * @return {@code true} if an existing row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during update execution
	 */
	public boolean setTableAvailability(LocalDateTime slot, int tableNumber, boolean isFree) throws SQLException {
		String col = "t_" + tableNumber;
		String sql = "UPDATE table_availability_grid SET " + col + "=? WHERE slot_datetime=?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setBoolean(1, isFree);
			ps.setTimestamp(2, Timestamp.valueOf(slot));
			return ps.executeUpdate() == 1;
		}
	}

	/**
	 * Builds a daily availability payload for GUI consumption.
	 * <p>
	 * Format per slot: {@code "YYYY-MM-DDTHH:MM,1,0,1,...;"} where the
	 * comma-separated values correspond to the {@code tables} order.
	 * </p>
	 *
	 * @param date   target date
	 * @param tables table list (ordering determines payload column order)
	 * @return serialized payload string (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public String getGridPayloadForDate(LocalDate date, List<Table> tables) throws SQLException {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();

		StringBuilder select = new StringBuilder("SELECT slot_datetime");
		for (Table t : tables) {
			select.append(", t_").append(t.getTableNumber());
		}

		select.append(" FROM table_availability_grid ").append("WHERE slot_datetime >= ? AND slot_datetime < ? ")
				.append("ORDER BY slot_datetime");

		StringBuilder out = new StringBuilder();

		try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
			ps.setTimestamp(1, Timestamp.valueOf(start));
			ps.setTimestamp(2, Timestamp.valueOf(end));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Timestamp ts = rs.getTimestamp("slot_datetime");
					LocalDateTime slot = ts.toLocalDateTime();

					out.append(slot.toString(), 0, 16);

					int colIdx = 2;
					for (int i = 0; i < tables.size(); i++) {
						boolean isFree = rs.getBoolean(colIdx++);
						out.append(",").append(isFree ? "1" : "0");
					}
					out.append(";");
				}
			}
		}

		return out.toString();
	}

	// =========================
	// METHODS: FIND FREE TABLES
	// =========================

	/**
	 * Finds and returns one free table number at a specific slot.
	 * <p>
	 * The chosen table is the first free table according to the order of
	 * {@code tables}.
	 * </p>
	 *
	 * @param slot   target slot timestamp
	 * @param tables candidate tables (ordering affects selection)
	 * @return table number if a free table exists, otherwise {@code null}
	 * @throws SQLException if a database error occurs during query execution
	 */
	public Integer findOneFreeTableNumberAtSlot(LocalDateTime slot, List<Table> tables) throws SQLException {
		if (slot == null || tables == null || tables.isEmpty())
			return null;

		StringBuilder select = new StringBuilder("SELECT ");
		for (int i = 0; i < tables.size(); i++) {
			if (i > 0)
				select.append(", ");
			select.append("t_").append(tables.get(i).getTableNumber());
		}
		select.append(" FROM table_availability_grid WHERE slot_datetime = ?");

		try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
			ps.setTimestamp(1, Timestamp.valueOf(slot));

			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;

				for (int i = 0; i < tables.size(); i++) {
					boolean isFree = rs.getBoolean(i + 1);
					if (isFree) {
						return tables.get(i).getTableNumber();
					}
				}
			}
		}

		return null;
	}

	/**
	 * For a full date, returns a map of
	 * {@code slot_datetime -> one free table number}.
	 * <p>
	 * For each slot:
	 * <ul>
	 * <li>If at least one table is free, chooses the first free table (by
	 * {@code tables} order) and stores it.</li>
	 * <li>If no table is free, the slot is skipped.</li>
	 * </ul>
	 * </p>
	 *
	 * @param date   target date
	 * @param tables candidate tables (ordering affects selection)
	 * @return map of slot times to a chosen free table number (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public Map<LocalDateTime, Integer> findOneFreeTableNumberPerSlotForDate(LocalDate date, List<Table> tables)
			throws SQLException {
		Map<LocalDateTime, Integer> out = new LinkedHashMap<>();
		if (date == null || tables == null || tables.isEmpty())
			return out;

		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();

		StringBuilder select = new StringBuilder("SELECT slot_datetime");
		for (Table t : tables) {
			select.append(", t_").append(t.getTableNumber());
		}

		select.append(" FROM table_availability_grid ").append("WHERE slot_datetime >= ? AND slot_datetime < ? ")
				.append("ORDER BY slot_datetime");

		try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
			ps.setTimestamp(1, Timestamp.valueOf(start));
			ps.setTimestamp(2, Timestamp.valueOf(end));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					LocalDateTime slot = rs.getTimestamp("slot_datetime").toLocalDateTime();

					Integer chosen = null;

					int colIdx = 2;
					for (int i = 0; i < tables.size(); i++) {
						boolean isFree = rs.getBoolean(colIdx++);
						if (isFree) {
							chosen = tables.get(i).getTableNumber();
							break;
						}
					}

					if (chosen != null) {
						out.put(slot, chosen);
					}
				}
			}
		}

		return out;
	}

	// =========================
	// Helpers (Opening Hours)
	// =========================

	/**
	 * Reads a SQL {@code TIME} value from a ResultSet column and returns it as a
	 * string.
	 * <p>
	 * Handles:
	 * <ul>
	 * <li>{@code NULL} values</li>
	 * <li>{@link java.sql.Time} values</li>
	 * <li>String values formatted as {@code HH:mm} or {@code HH:mm:ss}</li>
	 * </ul>
	 * </p>
	 *
	 * @param rs  result set
	 * @param col column name
	 * @return time string, or {@code null} if the column is {@code NULL} / blank
	 * @throws SQLException if column access fails
	 */
	private String readTimeAsString(ResultSet rs, String col) throws SQLException {
		Object val = rs.getObject(col);
		if (val == null)
			return null;

		if (val instanceof Time) {
			return ((Time) val).toString();
		}

		String s = rs.getString(col);
		if (s == null || s.isBlank())
			return null;

		return normalizeTimeString(s);
	}

	/**
	 * Sets a prepared statement parameter as a time string normalized to
	 * {@code HH:mm}.
	 * <p>
	 * If {@code timeStr} is {@code null} or blank, the parameter is set to SQL
	 * {@code NULL}.
	 * </p>
	 *
	 * @param ps      prepared statement
	 * @param idx     parameter index (1-based)
	 * @param timeStr input time string
	 * @throws SQLException if setting the parameter fails
	 */
	private void setTimeParamAsHHMM(PreparedStatement ps, int idx, String timeStr) throws SQLException {
		if (timeStr == null || timeStr.isBlank()) {
			ps.setNull(idx, Types.TIME);
			return;
		}

		String normalized = normalizeTimeString(timeStr);
		String hhmm = normalized.substring(0, 5);
		ps.setString(idx, hhmm);
	}

	/**
	 * Normalizes a time string into {@code HH:mm:ss}.
	 * <p>
	 * Supported inputs:
	 * <ul>
	 * <li>{@code HH:mm:ss} (returned as-is)</li>
	 * <li>{@code HH:mm} (seconds {@code :00} are appended)</li>
	 * <li>{@code HH:mm:ss.xxx...} (trimmed to the first 8 chars)</li>
	 * </ul>
	 * </p>
	 *
	 * @param s input time string
	 * @return normalized {@code HH:mm:ss} time string
	 * @throws IllegalArgumentException if the input format is not supported
	 */
	private String normalizeTimeString(String s) {
		s = s.trim();

		if (s.matches("^\\d{2}:\\d{2}:\\d{2}$"))
			return s;
		if (s.matches("^\\d{2}:\\d{2}$"))
			return s + ":00";

		if (s.length() >= 8 && s.matches("^\\d{2}:\\d{2}:\\d{2}.*$")) {
			return s.substring(0, 8);
		}

		throw new IllegalArgumentException("Bad time format: '" + s + "'. Expected HH:MM or HH:MM:SS");
	}

	/**
	 * Extracts {@code HH:mm} from a time string that may include seconds.
	 *
	 * @param t time string
	 * @return {@code HH:mm} or empty string when input is {@code null} or
	 *         invalid/too short
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

	/**
	 * Checks whether a specific table column is free ({@code 1}) at a given slot.
	 *
	 * @param slot        slot timestamp
	 * @param tableNumber table identifier
	 * @return {@code true} if the slot exists and the table is free, otherwise
	 *         {@code false}
	 * @throws SQLException if a database error occurs during query execution
	 */
	public boolean isTableFreeAtSlot(LocalDateTime slot, int tableNumber) throws SQLException {
		if (slot == null)
			return false;

		String col = "t_" + tableNumber;
		String sql = "SELECT " + col + " FROM table_availability_grid WHERE slot_datetime = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(slot));
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return false;
				return rs.getInt(1) == 1;
			}
		}
	}

	/**
	 * Deletes all grid slots for a specific date.
	 *
	 * @param date target date
	 * @throws SQLException if a database error occurs during deletion
	 */
	public void deleteGridSlotsForDate(LocalDate date) throws SQLException {
		if (date == null)
			return;

		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();

		String sql = "DELETE FROM table_availability_grid WHERE slot_datetime >= ? AND slot_datetime < ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(start));
			ps.setTimestamp(2, Timestamp.valueOf(end));
			ps.executeUpdate();
		}
	}

}
