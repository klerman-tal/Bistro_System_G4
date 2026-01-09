package logicControllers;

import dbControllers.DBController;
import dbControllers.Reservation_DB_Controller;
import dto.TimeReportDTO;
import entities.Enums;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class ReportsMainTest {

    public static void main(String[] args) throws Exception {

        System.out.println("=== ReportsMainTest started ===");

        DBController db = new DBController();
        DBController.MYSQL_PASSWORD = "Tal16519!";
        db.ConnectToDb();

        Connection conn = db.getConnection();
        if (conn == null) {
            System.out.println("❌ DB connection is NULL");
            return;
        }

        System.out.println("✅ DB connection OK");

        Reservation_DB_Controller reservationDB = new Reservation_DB_Controller(conn);
        ReportsController reportsController = new ReportsController(reservationDB);

        System.out.println("✅ Controllers initialized");

        // ניקוי TEST-*
        deleteTestReservations(conn);

        // הכנסת 20 הזמנות עם מקרי קצה
        insertAdvancedTestReservations(reservationDB);

        // בניית דו"ח ינואר 2025
        TimeReportDTO report = reportsController.buildTimeReport(2025, 1);

        System.out.println("===== TIME REPORT (Jan 2025) =====");
        System.out.println("On Time: " + report.getOnTimeCount());
        System.out.println("Minor Delay: " + report.getMinorDelayCount());
        System.out.println("Significant Delay: " + report.getSignificantDelayCount());

        // ציפיות (מבוסס על הסט שנכניס למטה)
        int expectedOnTime = 5;
        int expectedMinor = 3;
        int expectedSignificant = 3;

        boolean ok =
                report.getOnTimeCount() == expectedOnTime &&
                report.getMinorDelayCount() == expectedMinor &&
                report.getSignificantDelayCount() == expectedSignificant;

        System.out.println("===== EXPECTED =====");
        System.out.println("On Time: " + expectedOnTime);
        System.out.println("Minor Delay: " + expectedMinor);
        System.out.println("Significant Delay: " + expectedSignificant);

        System.out.println(ok ? "✅ TEST PASS" : "❌ TEST FAIL");

        System.out.println("=== ReportsMainTest finished ===");
    }

    // =============================
    // CLEAN ONLY TEST DATA
    // =============================
    private static void deleteTestReservations(Connection conn) throws Exception {
        System.out.println("🧹 Cleaning previous test reservations...");

        String sql = """
            DELETE FROM reservations
            WHERE confirmation_code LIKE 'TEST-%';
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int deleted = ps.executeUpdate();
            System.out.println("🗑️ Deleted " + deleted + " test reservations");
        }
    }

    // =============================
    // INSERT 20 CASES
    // =============================
    private static void insertAdvancedTestReservations(Reservation_DB_Controller reservationDB) throws Exception {

        System.out.println("📥 Inserting advanced test reservations (20 cases)...");

        // בסיס ינואר 2025
        LocalDateTime base = LocalDateTime.of(2025, 1, 5, 19, 0);

        // ---------------------------------------------------
        // A) 11 הזמנות "אמיתיות" שצריך להיכנס לדו"ח:
        //    Month=Jan 2025, status=Finished, checkin!=null, checkout!=null
        //    delays (minutes): 0,5,2,10,15,7,20,30,-3,4,16
        //
        //    Classification (לפי הלוגיקה שלך):
        //    On Time: delay <= 5  -> 0,5,2,-3,4  = 5
        //    Minor:   6..15       -> 10,15,7     = 3
        //    Significant: >15     -> 20,30,16    = 3
        // ---------------------------------------------------

        int[] eligibleDelays = {0, 5, 2, 10, 15, 7, 20, 30, -3, 4, 16};

        for (int i = 0; i < eligibleDelays.length; i++) {

            LocalDateTime reservationTime = base.plusDays(i);
            String code = "TEST-ELIG-" + i;

            int reservationId = reservationDB.addReservation(
                    reservationTime,
                    2,
                    code,
                    1,
                    Enums.UserRole.Subscriber,
                    5
            );

            LocalDateTime checkin = reservationTime.plusMinutes(eligibleDelays[i]);
            LocalDateTime checkout = checkin.plusMinutes(90); // נשארו 90 דקות

            reservationDB.updateCheckinTime(reservationId, checkin);
            reservationDB.updateCheckoutTime(reservationId, checkout);
            reservationDB.updateReservationStatus(reservationId, Enums.ReservationStatus.Finished);
        }

        // ---------------------------------------------------
        // B) 9 הזמנות שלא אמורות להילקח לדו"ח:
        //   1) Active עם checkin+checkout (אבל status לא Finished)
        //   2) Finished בלי checkout
        //   3) Finished בלי checkin
        //   4) Cancelled עם checkout (לא צריך להיכנס)
        //   5) Finished אבל בחודש אחר (Feb)
        //   6) Active בלי כלום
        //   7) Finished אבל checkout NULL (עוד פעם) ביום אחר
        //   8) Finished אבל reservationTime בחודש אחר (Dec 2024)
        //   9) Cancelled בלי checkout
        // ---------------------------------------------------

        // 1) Active (לא Finished) - אפילו אם יש checkin/checkout, לא אמור להיכנס
        {
            LocalDateTime t = base.plusDays(20);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-1", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(8));
            reservationDB.updateCheckoutTime(id, t.plusMinutes(120));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Active);
        }

        // 2) Finished בלי checkout (לא אמור להיכנס אם ה-SQL מסנן checkout IS NOT NULL)
        {
            LocalDateTime t = base.plusDays(21);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-2", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(8));
            // אין updateCheckoutTime
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Finished);
        }

        // 3) Finished בלי checkin (הלוגיקה שלך עושה continue)
        {
            LocalDateTime t = base.plusDays(22);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-3", 1, Enums.UserRole.Subscriber, 5);
            // אין updateCheckinTime
            reservationDB.updateCheckoutTime(id, t.plusMinutes(100));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Finished);
        }

        // 4) Cancelled עם checkout (לא אמור להיכנס אם מסננים Finished בלבד)
        {
            LocalDateTime t = base.plusDays(23);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-4", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(10));
            reservationDB.updateCheckoutTime(id, t.plusMinutes(80));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Cancelled);
        }

        // 5) Finished בפברואר 2025 (לא אמור להיכנס לדו"ח ינואר)
        {
            LocalDateTime t = LocalDateTime.of(2025, 2, 3, 20, 0);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-5", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(20));
            reservationDB.updateCheckoutTime(id, t.plusMinutes(110));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Finished);
        }

        // 6) Active בלי כלום
        {
            LocalDateTime t = base.plusDays(24);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-6", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Active);
        }

        // 7) Finished בלי checkout שוב
        {
            LocalDateTime t = base.plusDays(25);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-7", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(2));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Finished);
        }

        // 8) Finished בדצמבר 2024 (חודש אחר)
        {
            LocalDateTime t = LocalDateTime.of(2024, 12, 28, 18, 0);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-8", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(6));
            reservationDB.updateCheckoutTime(id, t.plusMinutes(90));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Finished);
        }

        // 9) Cancelled בלי checkout
        {
            LocalDateTime t = base.plusDays(26);
            int id = reservationDB.addReservation(t, 2, "TEST-NO-9", 1, Enums.UserRole.Subscriber, 5);
            reservationDB.updateCheckinTime(id, t.plusMinutes(10));
            reservationDB.updateReservationStatus(id, Enums.ReservationStatus.Cancelled);
        }

        System.out.println("✅ Advanced test reservations inserted");
    }
}
