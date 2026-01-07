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
     * בנאי: מקבל DB Controller (שמבצע גישה למסד הנתונים),
     * ושומר רפרנס ל־Restaurant Singleton (קאש של נתונים יחסית קבועים).
     */
    public RestaurantController(Restaurant_DB_Controller db) {
        this.db = db;
        this.restaurant = Restaurant.getInstance();
    }

    // =========================
    // TABLES (cache + DB)
    // =========================

    /**
     * טוען את רשימת השולחנות מה־DB אל ה־Restaurant Singleton (קאש).
     * (כלומר: אחרי קריאה למתודה זו, restaurant.getTables() יהיה מעודכן לפי הטבלה restaurant_tables)
     */
    public void loadTablesFromDb() throws SQLException {
        db.loadTables();
    }

    /**
     * מוסיף שולחן חדש או מעדכן שולחן קיים (לפי tableNumber).
     * 1) מעדכן את הקאש ב־Restaurant (רשימת tables).
     * 2) שומר/מעדכן במסד הנתונים (UPSERT).
     * 3) מבטיח שלטבלת הזמינות (table_availability_grid) יש עמודה מתאימה לשולחן הזה.
     */
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

        // בטיחות סכמה: אם נוסף שולחן חדש, מוסיפים עמודה t_<num> בגריד במידת הצורך
        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);
    }

    /**
     * מוחק שולחן לפי מספר שולחן.
     * 1) מוודא שהשולחנות טעונים לקאש.
     * 2) מוחק את השולחן מה־DB (restaurant_tables).
     * 3) אם המחיקה הצליחה - מסיר גם מהקאש.
     * 4) מריץ ensureAvailabilityGridSchema על השולחנות שנותרו (לשמירה על תקינות סכמה עבור הקיימים).
     *
     * הערה: בטבלת גריד "רחבה" בדרך כלל לא מוחקים עמודות אוטומטית עבור שולחן שנמחק,
     * אבל זה לא מפריע למערכת כל עוד לא משתמשים בעמודה הזאת.
     */
    public boolean removeTable(int tableNumber) throws SQLException {
        // לוודא tables טעונים
        List<Table> tables = getSortedTablesEnsured();

        // לוודא שלגריד יש את העמודות (כולל זו של השולחן שרוצים למחוק)
        db.ensureAvailabilityGridSchema(tables);

        // למצוא את השולחן בקאש
        Table toRemove = null;
        for (Table t : restaurant.getTables()) {
            if (t.getTableNumber() == tableNumber) { toRemove = t; break; }
        }
        if (toRemove == null) return false;

        // כאן ה-DB יחסום מחיקה אם יש תפוסים בחודש הקרוב
        boolean deleted = db.deleteTable(tableNumber);
        if (!deleted) return false;

        restaurant.getTables().remove(toRemove);

        // לשמור על סכמה תקינה עבור הנותרים
        List<Table> remaining = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(remaining);

        // אופציונלי: למחוק גם את העמודה מהגריד
        // db.dropTableColumnFromGrid(tableNumber);

        return true;
    }


    // =========================
    // OPENING HOURS (cache + DB)
    // =========================

    /**
     * טוען את שעות הפתיחה מה־DB אל ה־Restaurant Singleton (קאש).
     * (כלומר: אחרי קריאה למתודה זו, restaurant.getOpeningHours() יהיה מעודכן לפי הטבלה openinghours)
     */
    public void loadOpeningHoursFromDb() throws SQLException {
        db.loadOpeningHours();
    }

    /**
     * מעדכן שעות פתיחה/סגירה ליום מסוים בטבלה openinghours.
     * לאחר העדכון מבצע רענון לקאש (loadOpeningHoursFromDb) כדי שה־Restaurant יתאים ל־DB.
     */
    public void updateOpeningHours(OpeningHouers oh) throws SQLException {
        if (oh == null) return;
        db.updateOpeningHours(oh);

        // רענון קאש כדי שהלוגיקה (כמו initGridForNextDays) תשתמש בשעות החדשות
        loadOpeningHoursFromDb();
    }

    /**
     * מחפש את אובייקט OpeningHouers המתאים לתאריך נתון (לפי יום בשבוע באנגלית מלאה).
     * לדוגמה: LocalDate -> "Sunday" ואז מחפש ב־restaurant.getOpeningHours() התאמה ל־"Sunday".
     *
     * מחזיר:
     * - OpeningHouers אם נמצא יום מתאים
     * - null אם לא נמצא (למשל יום ללא שעות פתיחה)
     */
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
    // AVAILABILITY GRID (DB is truth)
    // =========================

    /**
     * מאתחל את טבלת הזמינות (table_availability_grid) ל־30 הימים הקרובים.
     * מה המתודה עושה בפועל:
     * 1) טוענת Tables ו־OpeningHours לקאש (Restaurant).
     * 2) מוודאת שסכמת הגריד קיימת ושיש עמודות לכל השולחנות (t_1, t_2...).
     * 3) מוחקת סלוטים ישנים (לפני NOW()) כדי שהטבלה לא תגדל לנצח.
     * 4) מוסיפה סלוטים של חצי שעה ל־30 ימים קדימה לפי שעות פתיחה של כל יום.
     */
    public void initAvailabilityGridNext30Days() throws Exception {
        loadTablesFromDb();
        loadOpeningHoursFromDb();

        List<Table> tables = getSortedTablesEnsured();

        db.ensureAvailabilityGridSchema(tables);
        db.deletePastSlots();

        LocalDate start = LocalDate.now();
        db.initGridForNextDays(start, 30, this::findOpeningHoursForDate);
    }

    /**
     * מחזיר "מחרוזת Payload" של כל הגריד ליום מסוים (כדי שה־GUI יצייר טבלה/גריד).
     * המתודה:
     * 1) מוודאת שיש שולחנות טעונים וממוינת לפי מספר שולחן.
     * 2) מוודאת שסכמת הגריד תואמת לשולחנות הקיימים.
     * 3) קוראת ל־DB ומחזירה את הפלט בפורמט שהגדרתם (שורות של slot + 1/0 לכל שולחן).
     */
    public String getGridFromDbPayload(LocalDate date) throws Exception {
        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);
        return db.getGridPayloadForDate(date, tables);
    }

    /**
     * מחזיר שולחן פנוי אחד עבור סלוט מסוים (תאריך+שעה) שמתאים גם לכמות האנשים.
     * תנאים לשולחן:
     * 1) השולחן פנוי בסלוט (לפי table_availability_grid)
     * 2) מספר הכיסאות בשולחן >= peopleCount (לפי restaurant_tables / הקאש של tables)
     *
     * אם אין שולחן שעומד בשני התנאים (או שאין שורה לגריד) -> מחזיר null.
     *
     * הבחירה היא "השולחן הראשון הפנוי שמתאים" לפי סדר המיון של tables.
     */
    public Table getOneAvailableTableAt(LocalDateTime slot, int peopleCount) throws Exception {
        if (slot == null) return null;
        if (peopleCount <= 0) return null;

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        // מסננים מראש לפי כיסאות כדי לא לבדוק סתם שולחנות קטנים מדי
        List<Table> candidates = new ArrayList<>();
        for (Table t : tables) {
            if (t.getSeatsAmount() >= peopleCount) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) return null;

        Integer freeTableNumber = db.findOneFreeTableNumberAtSlot(slot, candidates);
        if (freeTableNumber == null) return null;

        // המרה ממספר שולחן לאובייקט Table מהקאש
        for (Table t : candidates) {
            if (t.getTableNumber() == freeTableNumber) return t;
        }
        return null;
    }

    /**
     * ליום שלם: מחזיר Map של "שעה (slot)" -> "שולחן פנוי אחד" שמתאים גם לכמות האנשים.
     * עבור כל סלוט בגריד באותו יום:
     * - בוחרים שולחן אחד שהוא גם פנוי וגם seats_amount >= peopleCount.
     * - אם אין אף שולחן מתאים בסלוט הזה, מדלגים עליו (הוא לא יופיע במפה).
     *
     * אם peopleCount לא תקין או אין בכלל שולחנות עם מספיק כיסאות -> מוחזר Map ריק.
     */
    public Map<LocalDateTime, Table> getOneAvailableTablePerSlot(LocalDate date, int peopleCount) throws Exception {
        Map<LocalDateTime, Table> result = new LinkedHashMap<>();
        if (date == null) return result;
        if (peopleCount <= 0) return result;

        List<Table> tables = getSortedTablesEnsured();
        db.ensureAvailabilityGridSchema(tables);

        // מסננים מראש רק שולחנות שיכולים להכיל את כמות האנשים
        List<Table> candidates = new ArrayList<>();
        for (Table t : tables) {
            if (t.getSeatsAmount() >= peopleCount) {
                candidates.add(t);
            }
        }
        if (candidates.isEmpty()) return result;

        // DB מחזיר לכל סלוט: מספר שולחן פנוי אחד מתוך ה-candidates (אחרת מדלג על הסלוט)
        Map<LocalDateTime, Integer> chosenNumbers =
                db.findOneFreeTableNumberPerSlotForDate(date, candidates);

        // להמיר מספר שולחן -> אובייקט Table
        Map<Integer, Table> byNumber = new HashMap<>();
        for (Table t : candidates) byNumber.put(t.getTableNumber(), t);

        for (Map.Entry<LocalDateTime, Integer> e : chosenNumbers.entrySet()) {
            Table chosen = byNumber.get(e.getValue());
            if (chosen != null) result.put(e.getKey(), chosen);
        }

        return result;
    }


    /**
     * ניסיון להזמנה בטוח (מונע double booking):
     * מבצע UPDATE מותנה: משנה את העמודה של השולחן מ־1 ל־0 רק אם היא עדיין 1.
     *
     * מחזיר:
     * - true אם הצליח לתפוס את השולחן (היה פנוי)
     * - false אם לא הצליח (כבר היה תפוס)
     */
    public boolean tryReserveSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (slot == null) throw new IllegalArgumentException("slot is null");

        List<Table> tables = getSortedTablesEnsured();
        boolean exists = tables.stream().anyMatch(t -> t.getTableNumber() == tableNumber);
        if (!exists) throw new IllegalArgumentException("Table number not found: " + tableNumber);

        return db.tryReserveSlot(slot, tableNumber);
    }

    /**
     * משחרר סלוט (מגדיר שולחן כפנוי) ללא תנאי.
     * מחזיר true אם נמצאה שורה והייתה השפעה לעדכון, אחרת false.
     */
    public boolean releaseSlot(LocalDateTime slot, int tableNumber) throws Exception {
        if (slot == null) throw new IllegalArgumentException("slot is null");
        getSortedTablesEnsured(); // לוודא שהשולחנות טעונים
        return db.setTableAvailability(slot, tableNumber, true);
    }

    // =========================
    // Helpers
    // =========================

    /**
     * עזר פנימי:
     * - מוודא ש־restaurant.getTables() טעון (אם לא, טוען מה־DB).
     * - מחזיר עותק ממוין של רשימת השולחנות לפי מספר שולחן.
     *
     * המיון חשוב כי:
     * - ההצגה ב־GUI תהיה עקבית
     * - בחירת "השולחן הראשון הפנוי" תהיה דטרמיניסטית
     */
    private List<Table> getSortedTablesEnsured() throws SQLException {
        if (restaurant.getTables() == null || restaurant.getTables().isEmpty()) {
            loadTablesFromDb();
        }
        List<Table> tables = new ArrayList<>(restaurant.getTables());
        tables.sort(Comparator.comparingInt(Table::getTableNumber));
        return tables;
    }
    
    
    //liem
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
}
