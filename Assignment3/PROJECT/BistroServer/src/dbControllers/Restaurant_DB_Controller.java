package dbControllers;

import entities.OpeningHouers;
import entities.Restaurant;
import entities.Table;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

public class Restaurant_DB_Controller {

    private final Connection conn;

    public Restaurant_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // =========================
    // CREATE TABLES (SAFE)
    // =========================

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

    public void createOpeningHoursTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS openinghours (
                dayOfWeek VARCHAR(10) NOT NULL,
                openTime TIME NULL,
                closeTime TIME NULL,
                PRIMARY KEY (dayOfWeek)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // TABLES: LOAD / UPSERT / DELETE
    // =========================

    public void loadTables() throws SQLException {
        createRestaurantTablesTable();

        Restaurant r = Restaurant.getInstance();

        String sql = "SELECT table_number, seats_amount FROM restaurant_tables";
        ArrayList<Table> tables = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Table t = new Table();
                t.setTableNumber(rs.getInt("table_number"));
                t.setSeatsAmount(rs.getInt("seats_amount"));
                tables.add(t);
            }
        }

        r.setTables(tables);
    }

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

    public boolean deleteTable(int tableNumber) throws SQLException {
        String sql = "DELETE FROM restaurant_tables WHERE table_number=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableNumber);
            return ps.executeUpdate() == 1;
        }
    }

    // =========================
    // OPENING HOURS: LOAD / UPDATE
    // =========================

    public void loadOpeningHours() throws SQLException {
        Restaurant r = Restaurant.getInstance();

        String sql = "SELECT dayOfWeek, openTime, closeTime FROM openinghours";
        ArrayList<OpeningHouers> hours = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

    public void updateOpeningHours(OpeningHouers oh) throws SQLException {
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
    // table_availability_grid:
    // slot_datetime (PK) + t_<tableNumber> columns, default TRUE (1)
    // =========================

    /**
     * ✅ ensures table exists AND adds missing columns for new tables.
     * (Create-if-not-exists + ALTER add missing cols)
     */
    public void ensureAvailabilityGridSchema(List<Table> tables) throws SQLException {

        // 1) Create if not exists (with current tables)
        createAvailabilityGridTableIfNotExists(tables);

        // 2) Add missing columns
        Set<String> existingCols = getExistingGridColumns();
        for (Table t : tables) {
            String col = "t_" + t.getTableNumber();
            if (!existingCols.contains(col.toLowerCase(Locale.ROOT))) {

                String alter = "ALTER TABLE table_availability_grid " +
                        "ADD COLUMN " + col + " TINYINT(1) NOT NULL DEFAULT 1";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alter);
                }
            }
        }
    }

    private void createAvailabilityGridTableIfNotExists(List<Table> tables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS table_availability_grid (")
          .append("slot_datetime DATETIME NOT NULL PRIMARY KEY");

        for (Table t : tables) {
            sb.append(", t_").append(t.getTableNumber())
              .append(" TINYINT(1) NOT NULL DEFAULT 1");
        }

        sb.append(");");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sb.toString());
        }
    }

    private Set<String> getExistingGridColumns() throws SQLException {
        Set<String> cols = new HashSet<>();
        String sql = """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'table_availability_grid'
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return cols;
    }

    /**
     * ✅ delete old slots so the table doesn't grow forever.
     */
    public void deletePastSlots() throws SQLException {
        String sql = "DELETE FROM table_availability_grid WHERE slot_datetime < NOW()";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Insert slots for a single day.
     * Only inserts slot_datetime, so all t_# get DEFAULT 1 (free).
     */
    public void initGridForDate(LocalDate date, LocalTime open, LocalTime close) throws SQLException {
        LocalTime lastStart = close.minusMinutes(30);
        if (lastStart.isBefore(open)) return;

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
     * Initialize grid for next N days (e.g., 30 days), based on each day's OpeningHouers.
     */
    public void initGridForNextDays(LocalDate startDate,
                                    int days,
                                    Function<LocalDate, OpeningHouers> hoursProvider) throws SQLException {

        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            OpeningHouers oh = hoursProvider.apply(d);
            if (oh == null) continue;

            String openStr = safeHHMM(oh.getOpenTime());
            String closeStr = safeHHMM(oh.getCloseTime());
            if (openStr.isBlank() || closeStr.isBlank()) continue;

            LocalTime open = LocalTime.parse(openStr);
            LocalTime close = LocalTime.parse(closeStr);

            initGridForDate(d, open, close);
        }
    }

    /**
     * ✅ reserve with conditional update (prevents double booking).
     * returns true if success, false if already taken.
     */
    public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws SQLException {
        String col = "t_" + tableNumber;
        String sql = "UPDATE table_availability_grid SET " + col +
                     " = 0 WHERE slot_datetime = ? AND " + col + " = 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(slot));
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Release / set free or busy explicitly (unconditional).
     * returns true if row existed and updated.
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
     * Read grid from DB for a specific date and return payload:
     * each row: "YYYY-MM-DDTHH:MM,<t_#>,<t_#>...;"
     *
     * IMPORTANT: tables must be sorted in the order you want in the GUI.
     */
    public String getGridPayloadForDate(LocalDate date, List<Table> tables) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // build SELECT: slot_datetime, t_1, t_2...
        StringBuilder select = new StringBuilder("SELECT slot_datetime");
        for (Table t : tables) {
            select.append(", t_").append(t.getTableNumber());
        }

        select.append(" FROM table_availability_grid ")
              .append("WHERE slot_datetime >= ? AND slot_datetime < ? ")
              .append("ORDER BY slot_datetime");

        StringBuilder out = new StringBuilder();

        try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("slot_datetime");
                    LocalDateTime slot = ts.toLocalDateTime();

                    out.append(slot.toString(), 0, 16); // yyyy-MM-ddTHH:mm

                    int colIdx = 2; // after slot_datetime
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
    // Helpers (Opening Hours)
    // =========================

    private String readTimeAsString(ResultSet rs, String col) throws SQLException {
        Object val = rs.getObject(col);
        if (val == null) return null;

        if (val instanceof Time) {
            return ((Time) val).toString(); // "HH:MM:SS"
        }

        String s = rs.getString(col);
        if (s == null || s.isBlank()) return null;

        return normalizeTimeString(s);
    }

    private void setTimeParamAsHHMM(PreparedStatement ps, int idx, String timeStr) throws SQLException {
        if (timeStr == null || timeStr.isBlank()) {
            ps.setNull(idx, Types.VARCHAR);
            return;
        }

        String normalized = normalizeTimeString(timeStr); // "HH:MM:SS"
        String hhmm = normalized.substring(0, 5);        // "HH:MM"
        ps.setString(idx, hhmm);
    }

    private String normalizeTimeString(String s) {
        s = s.trim();

        if (s.matches("^\\d{2}:\\d{2}:\\d{2}$")) return s;
        if (s.matches("^\\d{2}:\\d{2}$")) return s + ":00";

        if (s.length() >= 8 && s.matches("^\\d{2}:\\d{2}:\\d{2}.*$")) {
            return s.substring(0, 8);
        }

        throw new IllegalArgumentException("Bad time format: '" + s + "'. Expected HH:MM or HH:MM:SS");
    }

    private String safeHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.matches("^\\d{2}:\\d{2}:\\d{2}$")) return t.substring(0, 5);
        if (t.matches("^\\d{2}:\\d{2}$")) return t;
        return t.length() >= 5 ? t.substring(0, 5) : "";
    }
}