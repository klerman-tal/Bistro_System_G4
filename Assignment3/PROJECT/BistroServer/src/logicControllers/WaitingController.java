package logicControllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import application.RestaurantServer;
import dbControllers.Notification_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import entities.Enums;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import entities.Notification;
import entities.OpeningHouers;
import entities.Table;
import entities.User;
import entities.Waiting;

/**
 * Handles waiting-list business logic for walk-in customers and table assignment workflows.
 * <p>
 * This controller provides a logic layer above {@link Waiting_DB_Controller} and coordinates with:
 * <ul>
 *   <li>{@link RestaurantController} for opening hours checks and time rounding utilities</li>
 *   <li>{@link ReservationController} for creating reservations when a waiting entry is seated</li>
 *   <li>{@link Notification_DB_Controller} for notifying users about table availability and cancellations</li>
 * </ul>
 * </p>
 * <p>
 * Core responsibilities include:
 * <ul>
 *   <li>Joining the waiting list (with opening-hours enforcement)</li>
 *   <li>Immediate seating when possible (creating a reservation right away)</li>
 *   <li>Assigning a freed table to the next waiting entry (FIFO and seat-capacity based)</li>
 *   <li>Confirming arrival within a grace period (15 minutes) to create a reservation</li>
 *   <li>Auto-cancelling expired waitings and end-of-day cancellations with notifications</li>
 * </ul>
 * </p>
 */
public class WaitingController {

    private final Waiting_DB_Controller db;
    private final Notification_DB_Controller notificationDB;
    private final RestaurantServer server;

    private final RestaurantController restaurantController;
    private final ReservationController reservationController;

    /**
     * Constructs a WaitingController with its required dependencies.
     *
     * @param db                    database controller for waiting-list persistence
     * @param notificationDB        database controller for notification persistence
     * @param server                server instance used for logging
     * @param restaurantController  restaurant logic controller (opening hours, rounding, etc.)
     * @param reservationController reservation logic controller used when converting waiting to reservations
     */
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

    /**
     * Exception thrown when joining the waiting list is blocked due to business rules
     * (e.g., restaurant is closed or outside opening hours).
     */
    public static class JoinWaitingBlockedException extends Exception {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the exception with a human-readable reason.
         *
         * @param message explanation of why joining was blocked
         */
        public JoinWaitingBlockedException(String message) {
            super(message);
        }
    }

    /**
     * Attempts to join the waiting list immediately; throws an exception if joining is blocked.
     *
     * @param guestsNumber number of guests
     * @param user         user requesting to join
     * @return a created {@link Waiting} entry (possibly already seated)
     * @throws JoinWaitingBlockedException if the restaurant is closed or joining is blocked
     */
    public Waiting joinWaitingListNowOrThrow(int guestsNumber, User user) throws JoinWaitingBlockedException {
        Waiting w = joinWaitingListNow(guestsNumber, user);
        if (w == null) {
            throw new JoinWaitingBlockedException("Restaurant is closed. Cannot join waiting list.");
        }
        return w;
    }

    /**
     * Adds the user to the waiting list if business rules allow it.
     * <p>
     * The method blocks joining when the restaurant is closed or when the current time is outside
     * of effective opening hours for today.
     * </p>
     * <p>
     * If the user can be seated immediately, this method attempts to create an immediate reservation
     * (scenario 1) and marks the waiting entry as seated.
     * </p>
     *
     * @param guestsNumber number of guests
     * @param user         user requesting to join
     * @return the created {@link Waiting} entry, or {@code null} if blocked or on failure
     */
    public Waiting joinWaitingListNow(int guestsNumber, User user) {

        try {
            LocalDate today = LocalDate.now();

            OpeningHouers oh =
                    restaurantController.getEffectiveOpeningHoursForDate(today);

            if (oh == null || oh.getOpenTime() == null || oh.getCloseTime() == null) {
                server.log("JOIN_WAITING blocked: restaurant closed (no opening hours).");
                return null;
            }

            LocalTime open = LocalTime.parse(oh.getOpenTime().substring(0, 5));
            LocalTime close = LocalTime.parse(oh.getCloseTime().substring(0, 5));
            LocalTime now = LocalTime.now();

            if (now.isBefore(open) || now.isAfter(close)) {
                server.log("JOIN_WAITING blocked: outside business hours. now=" + now + ", open=" + open + ", close=" + close);
                return null;
            }

        } catch (Exception e) {
            server.log("JOIN_WAITING blocked: failed checking opening hours. " + e.getMessage());
            return null;
        }

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

    /**
     * Retrieves the active waiting list entries.
     *
     * @return list of active waiting entries (empty list on error)
     */
    public ArrayList<Waiting> getActiveWaitingList() {
        try {
            return db.getAllWaitings();
        } catch (Exception e) {
            server.log("ERROR: Failed to fetch waiting list: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Cancels a waiting entry by confirmation code.
     * <p>
     * If a reservation exists with the same confirmation code, the method attempts to cancel it as well.
     * </p>
     *
     * @param confirmationCode waiting confirmation code
     * @return {@code true} if cancellation succeeded, {@code false} otherwise
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

            try { reservationController.CancelReservation(code); } catch (Exception ignore) {}

            server.log("Waiting cancelled. ConfirmationCode=" + code);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to cancel waiting. Code=" + code + ", Message=" + e.getMessage());
            return false;
        }
    }

    /**
     * Confirms customer arrival for a waiting entry and attempts to create a reservation.
     * <p>
     * Arrival is valid only if:
     * <ul>
     *   <li>The waiting entry is in {@link WaitingStatus#Waiting}</li>
     *   <li>A table was assigned (table number and freed time exist)</li>
     *   <li>The customer arrives within 15 minutes from the table-freed timestamp</li>
     * </ul>
     * </p>
     *
     * @param confirmationCode waiting confirmation code
     * @return {@code true} if the waiting entry was marked seated, {@code false} otherwise
     */
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

    /**
     * Cancels waiting entries that expired (e.g., no arrival confirmation within allowed time).
     *
     * @return number of cancelled waiting entries, or 0 on error
     */
    public int cancelExpiredWaitings() {

        try {
            LocalDateTime now = LocalDateTime.now();
            
            ArrayList<String> expiredCodes =
                    db.getExpiredWaitingCodes(now);

            int count =
                    db.cancelExpiredWaitings(now);

            for (String code : expiredCodes) {
                if (code == null) continue;

                server.log(
                    "Waiting auto-cancelled (no check-in after 15 min): " + code
                );
            }

            return count;

        } catch (Exception e) {
            server.log(
                "ERROR: cancelExpiredWaitings failed. Msg=" + e.getMessage()
            );
            return 0;
        }
    }

    /**
     * Handles the "table freed" workflow (scenario 2B).
     * <p>
     * The method:
     * <ul>
     *   <li>Finds the next waiting entry that fits the freed table capacity</li>
     *   <li>Updates the waiting entry with the freed time and assigned table number</li>
     *   <li>Schedules immediate notifications (SMS and Email)</li>
     * </ul>
     * </p>
     * <p>
     * This method does not create a reservation directly; the user must confirm arrival
     * within the grace period to convert waiting to a reservation.
     * </p>
     *
     * @param freedTable table that became available
     * @return {@code true} if a waiting entry was assigned and updated, {@code false} otherwise
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

            if (notificationDB != null) {
                String smsEmailBody =
                        "A table is now available. Your waiting code is: " + next.getConfirmationCode() +
                        ". Please arrive within 15 minutes to avoid cancellation.";

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

    /**
     * Retrieves a waiting entry by confirmation code.
     *
     * @param confirmationCode waiting confirmation code
     * @return the matching {@link Waiting}, or {@code null} if not found or on error
     */
    public Waiting getWaitingByCode(String confirmationCode) {
        if (confirmationCode == null || confirmationCode.isBlank()) return null;
        try {
            return db.getWaitingByConfirmationCode(confirmationCode.trim());
        } catch (Exception e) {
            server.log("ERROR: getWaitingByCode failed. Code=" + confirmationCode + ", Msg=" + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves active waiting entries for a specific user.
     *
     * @param userId user identifier
     * @return list of active waiting entries (empty list on error)
     */
    public ArrayList<Waiting> getActiveWaitingsForUser(int userId) {
        try {
            return db.getActiveWaitingsByUser(userId);
        } catch (Exception e) {
            server.log("ERROR: getActiveWaitingsForUser failed. UserId=" + userId + ", Msg=" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Cancels all active waiting entries for a given date (end-of-day workflow) and notifies users.
     *
     * @param date date to cancel waitings by (based on joined-at date)
     * @return number of cancelled entries, or 0 on error
     */
    public int cancelAllWaitingsEndOfDay(LocalDate date) {
        if (date == null) return 0;

        try {
            ArrayList<Waiting> toCancel = db.getActiveWaitingsByDate(date);

            int count = db.cancelAllWaitingsByDate(date);

            if (count > 0 && notificationDB != null && toCancel != null) {
                LocalDateTime now = LocalDateTime.now();

                for (Waiting w : toCancel) {
                    if (w == null) continue;

                    String msg = "The restaurant is now closed. Your waiting request was cancelled.";

                    notificationDB.addNotification(new Notification(
                            w.getCreatedByUserId(),
                            Enums.Channel.SMS,
                            Enums.NotificationType.TABLE_AVAILABLE,
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
                server.log("End of day: cancelled waitings + notifications sent. Date=" + date + ", Count=" + count);
            }

            return count;

        } catch (Exception e) {
            server.log("cancelAllWaitingsEndOfDay failed. Date=" + date + ", Msg=" + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Retrieves a list of table numbers currently locked by waiting/reservation allocation logic.
     *
     * @return list of locked table numbers (empty list on error)
     */
    public ArrayList<Integer> getLockedTableNumbersNow() {
        try {
            return db.getLockedTableNumbersNow();
        } catch (Exception e) {
            server.log("ERROR: getLockedTableNumbersNow failed. " + e.getMessage());
            return new ArrayList<>();
        }
    }

}
