package logicControllers;

import dbControllers.Restaurant_DB_Controller;
import entities.Restaurant;
import entities.Table;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class RestaurantMain {

    ////////////////////////////////////////////////////////////
    /// בדיקת Availability Grid + המתודות החדשות - למחוק אחר כך!!!!
    public static void main(String[] args) {

        try {
            // ===== 1) חיבור ל-DB =====
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false",
                    "root",
                    "michal28"
            );

            Restaurant_DB_Controller dbController = new Restaurant_DB_Controller(conn);
            RestaurantController controller = new RestaurantController(dbController);

            // ===== 2) טעינה ראשונית (שולחנות + שעות פתיחה) =====
            controller.loadTablesFromDb();
            controller.loadOpeningHoursFromDb();
            System.out.println("Tables + OpeningHours loaded");

            // אם אין שולחנות בכלל, יוצרים דוגמאות
            if (Restaurant.getInstance().getTables() == null || Restaurant.getInstance().getTables().isEmpty()) {
                System.out.println("No tables found. Creating sample tables 1..3...");

                Table t1 = new Table();
                t1.setTableNumber(1);
                t1.setSeatsAmount(2);
                controller.saveOrUpdateTable(t1);

                Table t2 = new Table();
                t2.setTableNumber(2);
                t2.setSeatsAmount(4);
                controller.saveOrUpdateTable(t2);

                Table t3 = new Table();
                t3.setTableNumber(3);
                t3.setSeatsAmount(6);
                controller.saveOrUpdateTable(t3);

                controller.loadTablesFromDb();
            }

            // לוג: סדר שולחנות
            Restaurant.getInstance().getTables().sort(Comparator.comparingInt(Table::getTableNumber));
            System.out.println("Tables in system: " + Restaurant.getInstance().getTables().size());
            for (Table t : Restaurant.getInstance().getTables()) {
                System.out.println(" - table " + t.getTableNumber() + " seats=" + t.getSeatsAmount());
            }

            // ===== 3) INIT GRID (create/alter/cleanup/init next 30 days) =====
            System.out.println("\n[INIT GRID] Ensuring schema + cleaning past + init next 30 days...");
            controller.initAvailabilityGridNext30Days();
            System.out.println("[INIT GRID] Done.");

            // ===== 4) בדיקה: האם הטבלה קיימת + עמודות =====
            System.out.println("\n[CHECK] Does table_availability_grid exist?");
            boolean exists = tableExists(conn, "table_availability_grid");
            System.out.println("table_availability_grid exists? " + exists);

            System.out.println("\n[CHECK] Columns in table_availability_grid:");
            printColumns(conn, "table_availability_grid");

            // ===== 5) GET GRID payload להיום =====
            LocalDate today = LocalDate.now();
            System.out.println("\n[GET GRID FROM DB PAYLOAD] for date: " + today);
            String payload = controller.getGridFromDbPayload(today);

            if (payload == null || payload.isBlank()) {
                System.out.println("No rows returned for today (maybe opening hours missing for today).");
            } else {
                System.out.println("Payload sample (first ~5 rows):");
                printFirstRows(payload, 5);
            }

            // שולפים סלוט ראשון כדי לבדוק עליו הכל
            LocalDateTime firstSlot = extractFirstSlot(payload);
            if (firstSlot == null) {
                System.out.println("\nNo slots in payload => cannot test slot-specific methods.");
                conn.close();
                return;
            }
            System.out.println("\nFirst slot detected: " + firstSlot);

            // ===== 6) בדיקת המתודות החדשות: getOneAvailableTableAt(slot, peopleCount) =====
            System.out.println("\n[TEST] getOneAvailableTableAt(slot, peopleCount)");

            int people2 = 2;
            Table oneAt2 = controller.getOneAvailableTableAt(firstSlot, people2);
            System.out.println("peopleCount=" + people2 + " => " + tableToString(oneAt2));

            int people4 = 4;
            Table oneAt4 = controller.getOneAvailableTableAt(firstSlot, people4);
            System.out.println("peopleCount=" + people4 + " => " + tableToString(oneAt4));

            int people100 = 100;
            Table oneAt100 = controller.getOneAvailableTableAt(firstSlot, people100);
            System.out.println("peopleCount=" + people100 + " => " + tableToString(oneAt100) + " (should be null)");

            // ===== 7) בדיקת המתודה היומית: getOneAvailableTablePerSlot(date, peopleCount) =====
            System.out.println("\n[TEST] getOneAvailableTablePerSlot(date, peopleCount)");

            Map<LocalDateTime, Table> dayMap2 = controller.getOneAvailableTablePerSlot(today, 2);
            System.out.println("peopleCount=2 => slots returned: " + dayMap2.size());
            printMapSample(dayMap2, 5);

            Map<LocalDateTime, Table> dayMap6 = controller.getOneAvailableTablePerSlot(today, 6);
            System.out.println("\npeopleCount=6 => slots returned: " + dayMap6.size());
            printMapSample(dayMap6, 5);

            Map<LocalDateTime, Table> dayMap100 = controller.getOneAvailableTablePerSlot(today, 100);
            System.out.println("\npeopleCount=100 => slots returned: " + dayMap100.size() + " (should be 0)");

            // ===== 8) בדיקת tryReserveSlot + השפעה על המתודות החדשות =====
            // נשריין את השולחן שמצאנו ל-peopleCount=2 (אם יש)
            if (oneAt2 != null) {
                int tableNum = oneAt2.getTableNumber();
                System.out.println("\n[TRY RESERVE] slot=" + firstSlot + " table=" + tableNum);
                boolean reserved = controller.tryReserveSlot(firstSlot, tableNum);
                System.out.println("Reserved success? " + reserved);

                System.out.println("\n[AFTER RESERVE] getOneAvailableTableAt(slot, peopleCount=2) again:");
                Table afterReserve2 = controller.getOneAvailableTableAt(firstSlot, 2);
                System.out.println("=> " + tableToString(afterReserve2) + " (should be different table or null)");

                System.out.println("\n[TRY RESERVE AGAIN] same slot/table (should be false):");
                boolean reservedAgain = controller.tryReserveSlot(firstSlot, tableNum);
                System.out.println("Reserved again success? " + reservedAgain);

                // ===== 9) RELEASE SLOT =====
                System.out.println("\n[RELEASE] slot=" + firstSlot + " table=" + tableNum);
                boolean released = controller.releaseSlot(firstSlot, tableNum);
                System.out.println("Released success? " + released);

                System.out.println("\n[AFTER RELEASE] getOneAvailableTableAt(slot, peopleCount=2) again:");
                Table afterRelease2 = controller.getOneAvailableTableAt(firstSlot, 2);
                System.out.println("=> " + tableToString(afterRelease2) + " (should return a valid table again)");

                // ===== 10) reserve after release =====
                System.out.println("\n[TRY RESERVE AFTER RELEASE] slot=" + firstSlot + " table=" + tableNum);
                boolean reservedAfterRelease = controller.tryReserveSlot(firstSlot, tableNum);
                System.out.println("Reserved after release success? " + reservedAfterRelease);
            } else {
                System.out.println("\nSkipping reserve tests (no available table found for peopleCount=2).");
            }

            // ===== 11) סגירת חיבור =====
            conn.close();
            System.out.println("\nDONE. You can now run in MySQL:");
            System.out.println("SELECT * FROM table_availability_grid LIMIT 50;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // Helpers for testing only
    // =========================

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void printColumns(Connection conn, String tableName) throws SQLException {
        String sql = """
            SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String type = rs.getString("COLUMN_TYPE");
                    String nullable = rs.getString("IS_NULLABLE");
                    String def = rs.getString("COLUMN_DEFAULT");
                    System.out.println(" - " + name + " | " + type + " | NULL? " + nullable + " | DEFAULT=" + def);
                }
            }
        }
    }

    private static void printFirstRows(String payload, int maxRows) {
        String[] rows = payload.split(";");
        for (int i = 0; i < rows.length && i < maxRows; i++) {
            if (!rows[i].isBlank()) System.out.println(rows[i]);
        }
    }

    private static LocalDateTime extractFirstSlot(String payload) {
        if (payload == null || payload.isBlank()) return null;
        String[] rows = payload.split(";");
        for (String row : rows) {
            if (row == null || row.isBlank()) continue;
            String[] parts = row.split(",");
            if (parts.length >= 2) {
                try {
                    return LocalDateTime.parse(parts[0].trim());
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String tableToString(Table t) {
        if (t == null) return "null";
        return "Table{num=" + t.getTableNumber() + ", seats=" + t.getSeatsAmount() + "}";
    }

    private static void printMapSample(Map<LocalDateTime, Table> map, int limit) {
        int i = 0;
        for (Map.Entry<LocalDateTime, Table> e : map.entrySet()) {
            System.out.println(" - " + e.getKey() + " => " + tableToString(e.getValue()));
            i++;
            if (i >= limit) break;
        }
        if (map.isEmpty()) {
            System.out.println(" (empty)");
        } else if (map.size() > limit) {
            System.out.println(" ... (" + (map.size() - limit) + " more)");
        }
    }
}
