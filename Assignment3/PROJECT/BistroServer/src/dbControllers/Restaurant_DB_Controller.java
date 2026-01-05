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

    /**
     * בנאי: מקבל Connection פעיל למסד הנתונים ושומר אותו לשימוש בכל השאילתות.
     */
    public Restaurant_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // =========================
    // CREATE TABLES (SAFE)
    // =========================

    /**
     * יוצר את הטבלה restaurant_tables אם היא לא קיימת.
     * הטבלה מכילה:
     * - table_number (PK)
     * - seats_amount
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
     * יוצר את הטבלה openinghours אם היא לא קיימת.
     * הטבלה מכילה:
     * - dayOfWeek (PK) לדוגמה: "Sunday"
     * - openTime (יכול להיות NULL)
     * - closeTime (יכול להיות NULL)
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

        // ⭐⭐⭐ NEW: seed 7 days (English) with NULL times ⭐⭐⭐
        String seedSql = """
            INSERT INTO openinghours (dayOfWeek, openTime, closeTime)
            VALUES (?, NULL, NULL)
            ON DUPLICATE KEY UPDATE dayOfWeek = dayOfWeek;
            """;

        String[] days = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

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
     * טוען את כל השולחנות מהטבלה restaurant_tables
     * ובונה רשימת Table, ואז מעדכן את Restaurant Singleton (r.setTables).
     */
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

    /**
     * מכניס שולחן חדש או מעדכן שולחן קיים (UPSERT) בטבלה restaurant_tables.
     * אם כבר קיים PK עם אותו table_number - יעדכן seats_amount.
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
  * בודק האם לשולחן יש סלוטים תפוסים (0) ב-30 ימים הקרובים.
  * בטבלת הגריד: 1=פנוי, 0=תפוס.
  *
  * @return true אם יש לפחות סלוט אחד תפוס בחודש הקרוב, אחרת false
  */
 public boolean hasReservedSlotsInNextDays(int tableNumber, int days) throws SQLException {
     String col = "t_" + tableNumber;

     String sql =
         "SELECT 1 " +
         "FROM table_availability_grid " +
         "WHERE slot_datetime >= NOW() " +
         "  AND slot_datetime < (NOW() + INTERVAL ? DAY) " +
         "  AND " + col + " = 0 " +
         "LIMIT 1";

     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, days);
         try (ResultSet rs = ps.executeQuery()) {
             return rs.next();
         }
     }
 }

 /**
  * מוחק שולחן לפי table_number מהטבלה restaurant_tables.
  * ❗ לא מאפשר מחיקה אם לשולחן יש תפוסים בחודש הקרוב בגריד.
  *
  * @return true אם נמחק, false אם חסום (תפוס) או לא נמצא
  */
 public boolean deleteTable(int tableNumber) throws SQLException {

     // אם אין גריד/אין עמודה - זה יכול לזרוק שגיאה.
     // לכן מומלץ לוודא לפני: ensureAvailabilityGridSchema(...)
     // (ב-RestaurantController את כבר עושה לפני רוב הפעולות)
     if (hasReservedSlotsInNextDays(tableNumber, 30)) {
         return false; // חסום: יש תפוסים בחודש הקרוב
     }

     String sql = "DELETE FROM restaurant_tables WHERE table_number=?";
     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, tableNumber);
         return ps.executeUpdate() == 1;
     }
 }

 /**
  * אופציונלי: אם את רוצה לנקות גם את העמודה של השולחן מהגריד.
  * להריץ רק אחרי שמותר למחוק ושבאמת מחקת את השולחן.
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
     * טוען את שעות הפתיחה מהטבלה openinghours,
     * בונה ArrayList של OpeningHouers,
     * ומעדכן את Restaurant Singleton (r.setOpeningHours).
     *
     * הערה: openTime/closeTime יכולים להיות NULL במסד ולכן קוראים באמצעות readTimeAsString.
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
    
    
  
    /**
     * מעדכן שעות פתיחה/סגירה עבור יום מסוים בטבלה openinghours.
     * אם openTime/closeTime ריקים/NULL - מכניס NULL למסד.
     *
     * הערה: השמירה מתבצעת בפורמט HH:MM (גם אם הגיע HH:MM:SS).
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
     * מוודא שטבלת הגריד table_availability_grid קיימת ושיש לה עמודות לכל השולחנות הקיימים.
     * עושה:
     * 1) CREATE TABLE IF NOT EXISTS (עם העמודות הנוכחיות)
     * 2) בדיקה ב־INFORMATION_SCHEMA אילו עמודות קיימות
     * 3) ALTER TABLE כדי להוסיף עמודות חסרות עבור שולחנות חדשים
     */
    public void ensureAvailabilityGridSchema(List<Table> tables) throws SQLException {
        createAvailabilityGridTableIfNotExists(tables);

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

    /**
     * יוצר את table_availability_grid אם היא לא קיימת.
     * הטבלה בנויה "רחב":
     * - slot_datetime הוא ה־PK
     * - לכל שולחן יש עמודה t_<tableNumber> (1=פנוי, 0=תפוס) עם DEFAULT 1
     */
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

    /**
     * מחזיר סט של כל שמות העמודות שקיימות בפועל בטבלה table_availability_grid,
     * באמצעות INFORMATION_SCHEMA.
     * משתמשים בזה כדי לזהות האם חסרות עמודות של שולחנות.
     */
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
     * מוחק סלוטים ישנים מהגריד (slot_datetime לפני הזמן הנוכחי),
     * כדי שהטבלה לא תתנפח לאורך זמן.
     */
    public void deletePastSlots() throws SQLException {
        String sql = "DELETE FROM table_availability_grid WHERE slot_datetime < NOW()";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * מאתחל סלוטים של חצי שעה ליום אחד, בטווח open..close.
     * מכניס רק את slot_datetime, וכל העמודות t_# יקבלו DEFAULT 1 (פנוי).
     *
     * הערה: אם close מוקדם מדי (אין סלוטים), לא עושה כלום.
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
     * מאתחל את הגריד ל־N ימים קדימה החל מ־startDate.
     * לכל יום:
     * - מקבל שעות פתיחה/סגירה דרך hoursProvider
     * - אם אין שעות (NULL/ריק) מדלג על אותו יום
     * - אחרת קורא ל־initGridForDate
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
     * ניסיון להזמנה בטוח (מונע הזמנה כפולה):
     * מעדכן את העמודה של השולחן ל־0 רק אם היא עדיין 1.
     *
     * מחזיר true אם הצליח (היה פנוי), false אם לא (כבר תפוס).
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
     * משנה זמינות של שולחן בסלוט מסוים בצורה לא-מותנית (unconditional).
     * isFree=true -> מגדיר 1 (פנוי), isFree=false -> מגדיר 0 (תפוס).
     *
     * מחזיר true אם עודכנה שורה קיימת, אחרת false.
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
     * מחזיר Payload יומי לגריד (לטובת GUI).
     * עבור כל שורה (slot) מחזיר:
     * "YYYY-MM-DDTHH:MM,1,0,1,...;"
     * לפי סדר השולחנות שנשלח במערך tables.
     */
    public String getGridPayloadForDate(LocalDate date, List<Table> tables) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

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

                    int colIdx = 2; // אחרי slot_datetime
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
    // ✅ NEW METHODS
    // =========================

    /**
     * מחפש ומחזיר מספר שולחן פנוי אחד בסלוט ספציפי (תאריך+שעה).
     * הבחירה: "השולחן הראשון הפנוי" לפי סדר הרשימה tables (לכן חשוב שהיא תהיה ממוינת מראש).
     *
     * מחזיר:
     * - Integer (מספר שולחן) אם נמצא שולחן פנוי
     * - null אם אין שולחן פנוי או שאין בכלל שורה לסלוט (לא אותחל)
     */
    public Integer findOneFreeTableNumberAtSlot(LocalDateTime slot, List<Table> tables) throws SQLException {
        if (slot == null || tables == null || tables.isEmpty()) return null;

        StringBuilder select = new StringBuilder("SELECT ");
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0) select.append(", ");
            select.append("t_").append(tables.get(i).getTableNumber());
        }
        select.append(" FROM table_availability_grid WHERE slot_datetime = ?");

        try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(slot));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null; // אין שורה לסלוט

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
     * ליום שלם: מחזיר Map של "שעה (slot_datetime)" -> "מספר שולחן פנוי אחד".
     * עבור כל סלוט באותו יום:
     * - אם יש לפחות שולחן אחד פנוי, בוחר את הראשון הפנוי (לפי סדר tables) ושומר במפה.
     * - אם אין שולחן פנוי, מדלג ולא מוסיף את השעה למפה.
     *
     * מחזיר תמיד Map (יכול להיות ריק אם אין אף סלוט פנוי).
     */
    public Map<LocalDateTime, Integer> findOneFreeTableNumberPerSlotForDate(LocalDate date, List<Table> tables) throws SQLException {
        Map<LocalDateTime, Integer> out = new LinkedHashMap<>();
        if (date == null || tables == null || tables.isEmpty()) return out;

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        StringBuilder select = new StringBuilder("SELECT slot_datetime");
        for (Table t : tables) {
            select.append(", t_").append(t.getTableNumber());
        }

        select.append(" FROM table_availability_grid ")
              .append("WHERE slot_datetime >= ? AND slot_datetime < ? ")
              .append("ORDER BY slot_datetime");

        try (PreparedStatement ps = conn.prepareStatement(select.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime slot = rs.getTimestamp("slot_datetime").toLocalDateTime();

                    Integer chosen = null;

                    int colIdx = 2; // אחרי slot_datetime
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
     * קורא ערך זמן (TIME) מ־ResultSet ומחזיר אותו כ־String.
     * מתמודד עם:
     * - NULL
     * - java.sql.Time
     * - מחרוזת בפורמטים HH:MM או HH:MM:SS
     */
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

    /**
     * מגדיר פרמטר זמן ל־PreparedStatement בפורמט HH:MM.
     * אם timeStr ריק/NULL -> מכניס NULL למסד.
     * אחרת:
     * - מנרמל ל־HH:MM:SS
     * - לוקח רק HH:MM ושולח כמחרוזת
     */
    private void setTimeParamAsHHMM(PreparedStatement ps, int idx, String timeStr) throws SQLException {
        if (timeStr == null || timeStr.isBlank()) {
        	ps.setNull(idx, Types.TIME);;
            return;
        }

        String normalized = normalizeTimeString(timeStr); // "HH:MM:SS"
        String hhmm = normalized.substring(0, 5);        // "HH:MM"
        ps.setString(idx, hhmm);
    }

    /**
     * מנרמל מחרוזת זמן לפורמט HH:MM:SS.
     * תומך ב:
     * - "HH:MM:SS" (מחזיר כמו שהוא)
     * - "HH:MM" (מוסיף ":00")
     * - "HH:MM:SS.xxx..." (חותך ל־8 תווים)
     *
     * זורק IllegalArgumentException אם הפורמט לא תקין.
     */
    private String normalizeTimeString(String s) {
        s = s.trim();

        if (s.matches("^\\d{2}:\\d{2}:\\d{2}$")) return s;
        if (s.matches("^\\d{2}:\\d{2}$")) return s + ":00";

        if (s.length() >= 8 && s.matches("^\\d{2}:\\d{2}:\\d{2}.*$")) {
            return s.substring(0, 8);
        }

        throw new IllegalArgumentException("Bad time format: '" + s + "'. Expected HH:MM or HH:MM:SS");
    }

    /**
     * מחלץ HH:MM ממחרוזת זמן שיכולה להיות HH:MM:SS או HH:MM.
     * אם הזמן ריק/NULL מחזיר מחרוזת ריקה.
     * אם יש פורמט מוזר - מנסה לחלץ את 5 התווים הראשונים (אם קיימים).
     */
    private String safeHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.matches("^\\d{2}:\\d{2}:\\d{2}$")) return t.substring(0, 5);
        if (t.matches("^\\d{2}:\\d{2}$")) return t;
        return t.length() >= 5 ? t.substring(0, 5) : "";
    }
}
