package logicControllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import application.RestaurantServer;
import dbControllers.Notification_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import entities.Enums;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import entities.Table;
import entities.User;
import entities.Waiting;
import entities.Notification;

public class WaitingController {

    private final Waiting_DB_Controller db;
    private final Notification_DB_Controller notificationDB; // ✅ NEW
    private final RestaurantServer server;

    private final RestaurantController restaurantController;
    private final ReservationController reservationController;

    public WaitingController(
            Waiting_DB_Controller db,
            Notification_DB_Controller notificationDB,
            RestaurantServer server,
            RestaurantController restaurantController,
            ReservationController reservationController
    ) {
        this.db = db;
        this.notificationDB = notificationDB;
        this.server = server;
        this.restaurantController = restaurantController;
        this.reservationController = reservationController;
    }

    public Waiting joinWaitingListNow(int guestsNumber, User user) {
        if (user == null || guestsNumber <= 0) return null;

        Waiting w = new Waiting();
        w.setCreatedByUserId(user.getUserId());
        w.setCreatedByRole(user.getUserRole());
        w.setGuestAmount(guestsNumber);

        w.generateAndSetConfirmationCode();

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

        } catch (Exception e) {
            server.log("ERROR: Failed to insert waiting entry. " + e.getMessage());
            return null;
        }

        // immediate seating (scenario 1)
        LocalDateTime nowSlot = restaurantController.roundUpToNextHalfHour(LocalDateTime.now());

        var res = reservationController.createNowReservationFromWaiting(
                nowSlot,
                guestsNumber,
                user,
                w.getConfirmationCode()
        );

        if (res != null) {
            try {
                boolean ok = db.markWaitingAsSeatedWithTable(w.getConfirmationCode(), res.getTableNumber());
                if (ok) {
                    w.setWaitingStatus(WaitingStatus.Seated);
                    w.setTableNumber(res.getTableNumber());
                    w.setTableFreedTime(LocalDateTime.now());
                }
            } catch (Exception e) {
                server.log("ERROR: Failed to mark waiting as seated (immediate). " + e.getMessage());
            }
            return w;
        }

        w.setWaitingStatus(WaitingStatus.Waiting);
        return w;
    }
    
    public ArrayList<Waiting> getActiveWaitingList() {
        try { 
            return db.getAllWaitings();
        } catch (Exception e) {
            server.log("ERROR: Failed to fetch waiting list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
    

    public boolean leaveWaitingList(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return false;

        String code = confirmationCode.trim();

        try {
            boolean cancelled = db.cancelWaiting(code);

            if (!cancelled) {
                server.log("WARN: Waiting not found/active. Cancel failed. Code=" + code);
                return false;
            }

            // if reservation exists with same code - cancel it too
            try { reservationController.CancelReservation(code); } catch (Exception ignore) {}

            server.log("Waiting cancelled. ConfirmationCode=" + code);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to cancel waiting. Code=" + code + ", Message=" + e.getMessage());
            return false;
        }
    }

    public boolean confirmArrival(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return false;

        String code = confirmationCode.trim();

        try {
            Waiting w = db.getWaitingByConfirmationCode(code);
            if (w == null) return false;

            if (w.getWaitingStatus() != WaitingStatus.Waiting) return false;
            if (w.getTableFreedTime() == null) return false;
            if (w.getTableNumber() == null) return false;

            LocalDateTime now = LocalDateTime.now();
            if (w.getTableFreedTime().plusMinutes(15).isBefore(now)) {
                db.cancelWaiting(code);
                return false;
            }

            LocalDateTime start = restaurantController.roundUpToNextHalfHour(w.getTableFreedTime());

            User u = new User();
            u.setUserId(w.getCreatedByUserId());
            u.setUserRole(w.getCreatedByRole() == null ? UserRole.RandomClient : w.getCreatedByRole());

            boolean created = reservationController.createReservationFromWaiting(
                    code,
                    start,
                    w.getGuestAmount(),
                    u,
                    w.getTableNumber()
            );

            if (!created) {
                server.log("WARN: confirmArrival - failed creating reservation. Code=" + code);
                return false;
            }

            return db.markWaitingAsSeated(code);

        } catch (Exception e) {
            server.log("ERROR: confirmArrival failed. Code=" + code + ", Msg=" + e.getMessage());
            return false;
        }
    }

    public int cancelExpiredWaitingsAndReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();

            ArrayList<String> expiredCodes = db.getExpiredWaitingCodes(now);
            int count = db.cancelExpiredWaitings(now);

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
     * handleTableFreed (scenario 2B):
     * - assigns table to next waiting (FIFO)
     * - updates waiting row with freed time + table number
     * - schedules notifications (EMAIL + SMS) immediately (scheduled_for = NOW)
     * - DOES NOT create reservation here
     *
     * English texts:
     * - Popup (safe, no code): "A table is now available. Please confirm your arrival within 15 minutes."
     * - SMS/Email (includes code): "A table is now available. Your waiting code is: <CODE>. Please confirm your arrival within 15 minutes."
     */
    public boolean handleTableFreed(Table freedTable) {
        if (freedTable == null) return false;

        try {
            Waiting next = db.getNextWaitingForSeats(freedTable.getSeatsAmount());
            if (next == null) return false;

            LocalDateTime now = LocalDateTime.now();

            boolean updated = db.setTableFreedForWaiting(
                    next.getConfirmationCode(),
                    now,
                    freedTable.getTableNumber()
            );

            if (!updated) return false;

            // ✅ schedule SMS + EMAIL notifications (immediate)
            if (notificationDB != null) {
                String smsEmailBody =
                        "A table is now available. Your waiting code is: " + next.getConfirmationCode() +
                        ". Please confirm your arrival within 15 minutes.";

                notificationDB.addNotification(new Notification(
                        next.getCreatedByUserId(),
                        Enums.Channel.SMS,
                        Enums.NotificationType.TABLE_AVAILABLE,
                        smsEmailBody,
                        now
                ));

                notificationDB.addNotification(new Notification(
                        next.getCreatedByUserId(),
                        Enums.Channel.EMAIL,
                        Enums.NotificationType.TABLE_AVAILABLE,
                        smsEmailBody,
                        now
                ));
            }

            server.log("Assigned freed table to waiting. WaitingCode=" + next.getConfirmationCode() +
                       ", Table=" + freedTable.getTableNumber());

            return true;

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
    
    public int cancelAllWaitingDueToClosing(LocalDate today) {

        int count = 0;

        try {
            ArrayList<Waiting> waitings = db.getActiveWaitingsForToday(today);

            for (Waiting w : waitings) {

                db.cancelWaiting(w.getConfirmationCode());
                count++;

                if (notificationDB != null) {

                    LocalDateTime now = LocalDateTime.now();

                    String msg =
                        "The restaurant is now closed. Your waiting request was cancelled.";

                    notificationDB.addNotification(new Notification(
                            w.getCreatedByUserId(),
                            Enums.Channel.SMS,
                            Enums.NotificationType.TABLE_AVAILABLE, // או להוסיף enum חדש אם תרצי
                            msg,
                            now
                    ));

                    notificationDB.addNotification(new Notification(
                            w.getCreatedByUserId(),
                            Enums.Channel.EMAIL,
                            Enums.NotificationType.TABLE_AVAILABLE,
                            msg,
                            now
                    ));
                }
            }

            if (count > 0) {
                server.log("🌙 Waiting cancelled due to closing hour. Count=" + count);
            }

        } catch (Exception e) {
            server.log("❌ cancelAllWaitingDueToClosing failed: " + e.getMessage());
        }

        return count;
    }
    
    public ArrayList<Waiting> getActiveWaitingsForUser(int userId) {
        try {
            return db.getActiveWaitingsByUser(userId);
        } catch (Exception e) {
            server.log("ERROR: getActiveWaitingsForUser failed. UserId=" + userId + ", Msg=" + e.getMessage());
            return new ArrayList<>();
        }
    }


}
