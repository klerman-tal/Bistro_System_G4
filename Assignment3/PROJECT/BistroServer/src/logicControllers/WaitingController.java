package logicControllers;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import application.RestaurantServer;
import dbControllers.Waiting_DB_Controller;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import entities.Reservation;
import entities.Table;
import entities.User;
import entities.Waiting;

public class WaitingController {

    private final Waiting_DB_Controller db;
    private final RestaurantServer server;

    private final RestaurantController restaurantController;
    private final ReservationController reservationController;

    public WaitingController(
            Waiting_DB_Controller db,
            RestaurantServer server,
            RestaurantController restaurantController,
            ReservationController reservationController
    ) {
        this.db = db;
        this.server = server;
        this.restaurantController = restaurantController;
        this.reservationController = reservationController;
    }

    /**
     * JOIN WAITING LIST (NOW)
     * דרישה:
     * 1) תמיד נוצרת רשומה ב-waiting_list (תיעוד לדוחות).
     * 2) אם יש שולחן זמין מיידית -> אותה רשומה תעודכן ל-Seated + table_number + table_freed_time
     *    וגם תיווצר Reservation עם אותו confirmation_code.
     * 3) אם אין שולחן -> נשאר Waiting (table_freed_time NULL) ועדיין אין Reservation.
     */
    public Waiting joinWaitingListNow(int guestsNumber, User user) {
        if (user == null || guestsNumber <= 0) return null;

        Waiting w = new Waiting();
        w.setCreatedByUserId(user.getUserId());
        w.setCreatedByRole(user.getUserRole());
        w.setGuestAmount(guestsNumber);

        // 🔴 חובה
        w.generateAndSetConfirmationCode();


        // 1) Always insert waiting row first (for reports)
        try {
            int waitingId = db.addToWaitingList(
                    guestsNumber,
                    w.getConfirmationCode(),
                    user.getUserId(),
                    user.getUserRole()
            );

            if (waitingId == -1) {
                server.log("ERROR: Waiting insert failed (no ID returned)");
                return null;
            }

            w.setWaitingId(waitingId);

        } catch (SQLException e) {
            server.log("ERROR: Failed to insert waiting entry. " + e.getMessage());
            return null;
        }

        // 2) Try immediate seating (NOW -> rounded to next half hour slot)
        LocalDateTime nowSlot = restaurantController.roundUpToNextHalfHour(LocalDateTime.now());

        Reservation res = reservationController.createNowReservationFromWaiting(
                nowSlot,
                guestsNumber,
                user,
                w.getConfirmationCode()
        );

        if (res != null) {
            // Mark waiting row as SEATED + set table number + set freed time NOW (for reports)
            try {
                boolean ok = db.markWaitingAsSeatedWithTable(w.getConfirmationCode(), res.getTableNumber());
                if (ok) {
                    w.setWaitingStatus(WaitingStatus.Seated);
                    w.setTableNumber(res.getTableNumber());
                    w.setTableFreedTime(LocalDateTime.now());
                }
            } catch (SQLException e) {
                server.log("ERROR: Failed to mark waiting as seated (immediate). " + e.getMessage());
            }

            return w;
        }

        // 3) Otherwise stays WAITING
        w.setWaitingStatus(WaitingStatus.Waiting);
        return w;
    }

    /**
     * Cancel waiting by confirmation code.
     * אם כבר נוצרה Reservation עבור code הזה - CancelReservation תשחרר סלוטים + תעדכן DB.
     */
    public boolean leaveWaitingList(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return false;

        String code = confirmationCode.trim();

        try {
            boolean cancelled = db.cancelWaiting(code);

            if (!cancelled) {
                server.log("WARN: Waiting not found/active. Cancel failed. Code=" + code);
                return false;
            }

            // אם קיימת הזמנה פעילה עם אותו קוד -> מבטלים גם אותה
            try { reservationController.CancelReservation(code); } catch (Exception ignore) {}

            server.log("Waiting cancelled. ConfirmationCode=" + code);
            return true;

        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel waiting. Code=" + code + ", Message=" + e.getMessage());
            return false;
        }
    }

    /**
     * Confirm arrival:
     * הצלחה רק אם:
     * - waiting_status=Waiting
     * - table_freed_time != null
     * - לא עברו 15 דקות מה-table_freed_time
     *
     * הערה: Reservation כבר אמורה להיות קיימת בשלב הזה (נוצרה כששולחן התפנה).
     */
    public boolean confirmArrival(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return false;

        String code = confirmationCode.trim();

        try {
            Waiting w = db.getWaitingByConfirmationCode(code);
            if (w == null) return false;

            if (w.getWaitingStatus() != WaitingStatus.Waiting) return false;
            if (w.getTableFreedTime() == null) return false;

            LocalDateTime now = LocalDateTime.now();
            if (w.getTableFreedTime().plusMinutes(15).isBefore(now)) {
                // expired -> cancel waiting + cancel reservation (release slots)
                db.cancelWaiting(code);
                try { reservationController.CancelReservation(code); } catch (Exception ignore) {}
                return false;
            }

            // mark seated
            return db.markWaitingAsSeated(code);

        } catch (Exception e) {
            server.log("ERROR: confirmArrival failed. Code=" + code + ", Msg=" + e.getMessage());
            return false;
        }
    }

    /**
     * כל 10 שניות:
     * 1) מבטל waitings שפגו (15 דקות) + מבטל reservation תואמת + משחרר סלוטים.
     */
    public int cancelExpiredWaitingsAndReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1) bring codes that are expired (before update)
            ArrayList<String> expiredCodes = db.getExpiredWaitingCodes(now);

            // 2) cancel them in waiting_list
            int count = db.cancelExpiredWaitings(now);

            // 3) cancel matching reservations (releases slots)
            for (String code : expiredCodes) {
                if (code == null) continue;
                try { reservationController.CancelReservation(code); } catch (Exception ignore) {}
            }

            if (count > 0) server.log("Cancelled expired waitings. Count=" + count);
            return count;

        } catch (Exception e) {
            server.log("ERROR: cancelExpiredWaitingsAndReservations failed. Msg=" + e.getMessage());
            return 0;
        }
    }

    /**
     * Called when a table is freed (finish/checkout flow).
     *
     * Desired behavior:
     * - Pick next waiting (FIFO) that fits seats AND that doesn't already have a freed table.
     * - Create Reservation for NOW (rounded slot) with SAME confirmation code.
     * - Update waiting row: set table_freed_time + table_number (status stays Waiting)
     *   so the client has 15 minutes to confirm arrival.
     */
    public boolean handleTableFreed(Table freedTable) {
        if (freedTable == null) return false;

        try {
            Waiting next = db.getNextWaitingForSeats(freedTable.getSeatsAmount());
            if (next == null) return false;

            // Build a minimal User from waiting row
            User u = new User();
            u.setUserId(next.getCreatedByUserId());
            u.setUserRole(next.getCreatedByRole() == null ? UserRole.RandomClient : next.getCreatedByRole());

            // Reservation starts "now" (aligned)
            LocalDateTime start = restaurantController.roundUpToNextHalfHour(LocalDateTime.now());

            // 1) Create reservation with the SAME confirmation code (locks 2h + inserts to DB)
            boolean created = reservationController.createReservationFromWaiting(
                    next.getConfirmationCode(),
                    start,
                    next.getGuestAmount(),
                    u,
                    freedTable.getTableNumber()
            );

            if (!created) {
                server.log("WARN: handleTableFreed - failed creating reservation from waiting. Code=" +
                        next.getConfirmationCode() + ", Table=" + freedTable.getTableNumber());
                return false;
            }

            // 2) Update waiting row: set freed time + table number (status stays Waiting)
            boolean updated = db.setTableFreedForWaiting(
                    next.getConfirmationCode(),
                    LocalDateTime.now(),
                    freedTable.getTableNumber()
            );

            if (updated) {
                server.log("Assigned freed table to waiting. WaitingCode=" + next.getConfirmationCode() +
                        ", Table=" + freedTable.getTableNumber());
            } else {
                // If waiting row wasn't updated, we should cancel reservation to avoid holding slots with no waiting row
                try { reservationController.CancelReservation(next.getConfirmationCode()); } catch (Exception ignore) {}
                server.log("WARN: handleTableFreed - waiting row not updated, reservation cancelled. Code=" +
                        next.getConfirmationCode());
            }

            return updated;

        } catch (Exception e) {
            server.log("ERROR: handleTableFreed failed. " + e.getMessage());
            return false;
        }
    }
    
    public Waiting getWaitingByCode(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return null;
        try {
            return db.getWaitingByConfirmationCode(confirmationCode.trim());
        } catch (Exception e) {
            server.log("ERROR: getWaitingByCode failed. Code=" + confirmationCode + ", Msg=" + e.getMessage());
            return null;
        }
    }

}
