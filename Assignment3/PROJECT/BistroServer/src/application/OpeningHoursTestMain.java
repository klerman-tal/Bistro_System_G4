package application;

import dbControllers.DBController;
import dbControllers.Restaurant_DB_Controller;
import entities.OpeningHouers;
import entities.Restaurant;
import logicControllers.RestaurantController;

import java.sql.Connection;

public class OpeningHoursTestMain {

    public static void main(String[] args) {

        DBController.MYSQL_PASSWORD = "michal28";

        DBController dbc = new DBController();
        dbc.ConnectToDb();

        Connection conn = dbc.getConnection();
        if (conn == null) {
            System.out.println("❌ DB connection failed (null)");
            return;
        }

        Restaurant_DB_Controller rdb = new Restaurant_DB_Controller(conn);
        RestaurantController rc = new RestaurantController(rdb);

        try {
            rc.loadOpeningHoursFromDb();

            System.out.println("=== OPENING HOURS (FROM DB) ===");
            for (OpeningHouers oh : Restaurant.getInstance().getOpeningHours()) {
                System.out.println(oh.getDayOfWeek() + " | " + oh.getOpenTime() + " - " + oh.getCloseTime());
            }

            OpeningHouers sunday = null;
            for (OpeningHouers oh : Restaurant.getInstance().getOpeningHours()) {
                if ("Sunday".equalsIgnoreCase(oh.getDayOfWeek())) {
                    sunday = oh;
                    break;
                }
            }

            if (sunday == null) {
                System.out.println("\n⚠ Sunday not found in openinghours");
                return;
            }

            sunday.setOpenTime("09:00");
            sunday.setCloseTime("22:00");
            rc.updateOpeningHours(sunday);

            rc.loadOpeningHoursFromDb();

            System.out.println("\n=== OPENING HOURS (AFTER UPDATE) ===");
            for (OpeningHouers oh : Restaurant.getInstance().getOpeningHours()) {
                System.out.println(oh.getDayOfWeek() + " | " + oh.getOpenTime() + " - " + oh.getCloseTime());
            }

            System.out.println("\n✅ OpeningHours update works");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
