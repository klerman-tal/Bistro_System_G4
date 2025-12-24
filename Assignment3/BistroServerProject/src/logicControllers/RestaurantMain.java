package logicControllers;

import dbControllers.Restaurant_DB_Controller;
import entities.Restaurant;
import entities.Table;

import java.sql.Connection;
import java.sql.DriverManager;

public class RestaurantMain {
////////////////////////////////////////////////////////////
///בדיקת חלוגעיקה resturant למחוק אחר כך!!!!
    public static void main(String[] args) {

        try {
            // ===== 1. חיבור ל-DB =====
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false",
                    "root",
                    "michal28"
            );

            Restaurant_DB_Controller dbController =
                    new Restaurant_DB_Controller(conn);

            RestaurantController controller =
                    new RestaurantController(dbController);

            // ===== 2. טעינת נתונים קיימים =====
            controller.loadTablesFromDb();
            System.out.println("Tables loaded from DB");

            // ===== 3. יצירת שולחן ראשון =====
            Table table1 = new Table();
            table1.setTableNumber(1);
            table1.setSeatsAmount(4);
            table1.setIsAvailable(true);

            // ===== 4. יצירת שולחן שני =====
            Table table2 = new Table();
            table2.setTableNumber(2);
            table2.setSeatsAmount(2);
            table2.setIsAvailable(true);

            // ===== 5. הוספה למערכת =====
            controller.addTable(table1);
            controller.addTable(table2);

            // ===== 6. בדיקות =====
            Restaurant r = Restaurant.getInstance();

            System.out.println("Total tables: " + r.TableCounter());
            System.out.println("Available tables: " + r.getAvailableTables().size());

            // ===== 7. בדיקת הזמנת שולחן =====
            Table reserved = controller.getAvailableTableAndReserve();
            System.out.println("Reserved table: " + reserved.getTableNumber());

            System.out.println("Available tables after reservation: "
                    + r.getAvailableTables().size());

            // ===== 8. מחיקת שולחן =====
            System.out.println("Deleting table number 2...");
            boolean deleted = controller.removeTable(2);
            System.out.println("Deleted successfully? " + deleted);

            // ===== 9. בדיקה אחרי מחיקה =====
            System.out.println("Total tables after delete: " + r.TableCounter());
            System.out.println("Available tables after delete: "
                    + r.getAvailableTables().size());

            // ===== 10. סגירת חיבור =====
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
