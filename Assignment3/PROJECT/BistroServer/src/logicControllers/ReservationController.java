package logicControllers;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import application.RestaurantServer;
import dbControllers.Notification_DB_Controller;
import dbControllers.Receipt_DB_Controller;
import dbControllers.Reservation_DB_Controller;
import dto.CreateReservationDTO;
import entities.Enums.ReservationStatus;
import entities.Reservation;
import entities.Restaurant;
import entities.Table;
import entities.User;
import entities.Notification;
import entities.Enums;
import logicControllers.WaitingController;
import dto.GetTableResultDTO;



public class ReservationController {

    private final Reservation_DB_Controller db;
    private final Notification_DB_Controller notificationDB; // ✅ scheduled notifications
    private final Restaurant restaurant;
    private final RestaurantServer server;
    private final RestaurantController restaurantController;
    private final ReceiptController receiptController;



    // ✅ NEW: link to waiting logic for "table freed" scenario
    private WaitingController waitingController;
 // Pending check-ins: tableNumber -> pending reservation info
    private final Map<Integer, PendingReservationCheckin> pendingCheckins = new ConcurrentHashMap<>();

    private static class PendingReservationCheckin {
        final int reservationId;
        final int userId;
        final String confirmationCode;
        final int tableNumber;

        PendingReservationCheckin(int reservationId, int userId, String confirmationCode, int tableNumber) {
            this.reservationId = reservationId;
            this.userId = userId;
            this.confirmationCode = confirmationCode;
            this.tableNumber = tableNumber;
        }
    }


    /**
     * Constructor: connects controller to DB layer, server logger, and restaurant availability logic.
     */
    public ReservationController(Reservation_DB_Controller db,
            Notification_DB_Controller notificationDB,
            RestaurantServer server,
            RestaurantController rc,
            ReceiptController receiptController) {
    	
		this.db = db;
		this.notificationDB = notificationDB;
		this.server = server;
		this.restaurant = Restaurant.getInstance();
		this.restaurantController = rc;
		this.receiptController = receiptController;
	}


    /**
     * ✅ NEW: connect waiting controller after construction (avoid circular constructor dependency).
     */
    public void setWaitingController(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    // =====================================================
    // NOTIFICATIONS (SCHEDULED)
    // =====================================================

    /**
     * Schedules reminder notifications 2 hours before reservation time (SMS + Email).
     * English texts:
     * - SMS/Email content: "Reminder: Your reservation is in 2 hours. Confirmation code: <CODE>"
     */
    private void scheduleReservationReminder2HoursBefore(int userId, LocalDateTime reservationDateTime, String confirmationCode) {
        try {
            if (notificationDB == null) return;
            if (reservationDateTime == null) return;
            if (confirmationCode == null || confirmationCode.isBlank()) return;

            LocalDateTime scheduledFor = reservationDateTime.minusHours(2);

            // If it's already too late, skip scheduling
            if (scheduledFor.isBefore(LocalDateTime.now())) {
                return;
            }

            String smsBody = "Reminder: Your reservation is in 2 hours. Confirmation code: " + confirmationCode;
            String emailBody = "Reminder: Your reservation is in 2 hours. Confirmation code: " + confirmationCode;

            notificationDB.addNotification(new Notification(
                    userId,
                    Enums.Channel.SMS,
                    Enums.NotificationType.RESERVATION_REMINDER_2H,
                    smsBody,
                    scheduledFor
            ));

            notificationDB.addNotification(new Notification(
                    userId,
                    Enums.Channel.EMAIL,
                    Enums.NotificationType.RESERVATION_REMINDER_2H,
                    emailBody,
                    scheduledFor
            ));

        } catch (Exception e) {
            server.log("ERROR: Failed to schedule 2h reminder. UserId=" + userId + ", Msg=" + e.getMessage());
        }
    }

    // =====================================================
    // AVAILABILITY HELPERS
    // =====================================================

    private void fillAvailableTimesForDay(
            LocalDate date,
            int guestsNumber,
            LocalTime fromTime,
            ArrayList<LocalTime> out) {

        if (out == null) return;
        out.clear();
 
        try {
            Map<LocalDateTime, Table> map =
                    restaurantController.getOneAvailableTablePerSlot(date, guestsNumber);

            ArrayList<LocalDateTime> times = new ArrayList<>(map.keySet());
            times.sort(Comparator.naturalOrder());

            for (LocalDateTime dt : times) {
                LocalTime t = dt.toLocalTime();

                if (fromTime != null && t.isBefore(fromTime)) continue;

                Table table = map.get(dt);
                if (table == null) continue;

                if (restaurantController.isTableFreeForTwoHours(dt, table.getTableNumber())) {
                    out.add(t);
                }
            }

        } catch (Exception e) {
            server.log("ERROR: Failed to load available times. " + e.getMessage());
        }
    }

    private boolean tryReserveForTwoHours(LocalDateTime start, int tableNumber) {
        ArrayList<LocalDateTime> locked = new ArrayList<>();
        LocalDateTime slot = start;

        try {
            for (int i = 0; i < 4; i++) {
                boolean ok = restaurantController.tryReserveSlot(slot, tableNumber);
                if (!ok) {
                    for (LocalDateTime s : locked) {
                        try { restaurantController.releaseSlot(s, tableNumber); }
                        catch (Exception ignore) {}
                    }
                    return false;
                }

                locked.add(slot);
                slot = slot.plusMinutes(30);
            }

            return true;

        } catch (Exception e) {
            for (LocalDateTime s : locked) {
                try { restaurantController.releaseSlot(s, tableNumber); }
                catch (Exception ignore) {}
            }

            server.log("ERROR: Failed to reserve 2 hours slots. " + e.getMessage());
            return false;
        }
    }

    // =====================================================
    // TABLE FREED -> WAITING HOOK
    // =====================================================

    /**
     * ✅ NEW: after cancel/finish releases a table, notify waiting list (scenario 2B).
     */
    private void notifyWaitingTableFreed(Integer tableNumber) {
        if (waitingController == null) return;
        if (tableNumber == null) return;

        try {
            // Try to find the real table object from Restaurant cache (so we have correct seats_amount)
            Table freed = null;

            try {
                // If your Restaurant singleton keeps tables in memory, use it
                // (common in your project because you load tables into Restaurant cache).
                for (Table t : restaurant.getTables()) {
                    if (t != null && t.getTableNumber() == tableNumber) {
                        freed = t;
                        break;
                    }
                }
            } catch (Exception ignore) {}

            // Fallback: minimal table (won't crash, but seats may be missing if cache isn't loaded)
            if (freed == null) {
                freed = new Table();
                freed.setTableNumber(tableNumber);
                // IMPORTANT: if you reach here and seats_amount is needed, ensure Restaurant cache is loaded.
                freed.setSeatsAmount(Integer.MAX_VALUE);
            }

            waitingController.handleTableFreed(freed);

        } catch (Exception e) {
            server.log("ERROR: notifyWaitingTableFreed failed. Table=" + tableNumber + ", Msg=" + e.getMessage());
        }
    }

    // =====================================================
    // CREATE / CANCEL RESERVATION
    // =====================================================

    public Reservation CreateTableReservation(
            CreateReservationDTO dto,
            ArrayList<LocalTime> availableTimesOut) {

        if (availableTimesOut != null) availableTimesOut.clear();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requested = LocalDateTime.of(dto.getDate(), dto.getTime());

        if (requested.isBefore(now.plusHours(1)) || requested.isAfter(now.plusMonths(1))) {
            server.log("WARN: Invalid reservation time. UserId=" + dto.getUserId());
            return null;
        }

        Table table;
        try {
            table = restaurantController.getOneAvailableTableAt(requested, dto.getGuests());
        } catch (Exception e) {
            server.log("ERROR: Failed to check availability. " + e.getMessage());
            return null;
        }

        if (table == null) {
            fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
            return null;
        }

        boolean locked2h = tryReserveForTwoHours(requested, table.getTableNumber());
        if (!locked2h) {
            fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
            return null;
        }

        Reservation res = new Reservation();
        res.setCreatedByUserId(dto.getUserId());
        res.setGuestAmount(dto.getGuests());
        res.setReservationTime(requested);
        res.setConfirmed(true);
        res.setTableNumber(table.getTableNumber());
        res.setActive(true);
        res.setReservationStatus(ReservationStatus.Active);
        res.setCreatedByRole(dto.getRole());
        res.generateAndSetConfirmationCode();

        try {
            int reservationId = db.addReservation(
                    res.getReservationTime(),
                    dto.getGuests(),
                    res.getConfirmationCode(),
                    dto.getUserId(),
                    dto.getRole(),
                    table.getTableNumber()
            );

            if (reservationId == -1) {
                rollbackReservation(requested, table.getTableNumber());
                return null;
            }
            res.setReservationId(reservationId);

            // ✅ schedule reminder 2 hours before
            scheduleReservationReminder2HoursBefore(dto.getUserId(), requested, res.getConfirmationCode());

        } catch (SQLException e) {
            server.log("ERROR: DB Insert failed: " + e.getMessage());
            rollbackReservation(requested, table.getTableNumber());
            return null;
        }

        return res;
    }
    public int relocateOrCancelReservationsForDeletedTable(int tableNumber, int days) {
        int affected = 0;

        try {
            ArrayList<Reservation> list =
                    db.getActiveReservationsForTableInNextDays(tableNumber, days);

            for (Reservation r : list) {
                if (r == null || r.getReservationTime() == null) continue;

                LocalDateTime start = r.getReservationTime();
                int guests = r.getGuestAmount();

                // 1) מצא שולחן חלופי (לא כולל השולחן שנמחק)
                Table newTable = restaurantController.getOneAvailableTableAtExcluding(start, guests, tableNumber);

                // 2) אם אין חלופי -> בטל
                if (newTable == null) {
                    server.log("DELETE TABLE: Cancelling reservation (no alternative table). Code=" +
                            r.getConfirmationCode() + " Time=" + start + " OldTable=" + tableNumber);

                    CancelReservation(r.getConfirmationCode()); // משחרר סלוטים + מעדכן DB
                    affected++;
                    continue;
                }

                int newTableNum = newTable.getTableNumber();

                // 3) ודא 2 שעות פנויות + נעל
                boolean locked = tryReserveForTwoHours(start, newTableNum);
                if (!locked) {
                    server.log("DELETE TABLE: Cancelling reservation (failed locking new table). Code=" +
                            r.getConfirmationCode() + " Time=" + start + " NewTable=" + newTableNum);

                    CancelReservation(r.getConfirmationCode());
                    affected++;
                    continue;
                }

                // 4) שחרר 2 שעות מהשולחן הישן
                for (int i = 0; i < 4; i++) {
                    try { restaurantController.releaseSlot(start.plusMinutes(30L * i), tableNumber); }
                    catch (Exception ignore) {}
                }

                // 5) עדכן DB לשולחן החדש
                boolean updated = db.updateReservationTableNumber(r.getReservationId(), newTableNum);
                if (!updated) {
                    // אם נכשל עדכון DB: rollback לוגי
                    for (int i = 0; i < 4; i++) {
                        try { restaurantController.releaseSlot(start.plusMinutes(30L * i), newTableNum); }
                        catch (Exception ignore) {}
                    }
                    // ואת הישנה נחזיר לתפוס כדי לא “לשחרר בטעות”
                    for (int i = 0; i < 4; i++) {
                        try { restaurantController.tryReserveSlot(start.plusMinutes(30L * i), tableNumber); }
                        catch (Exception ignore) {}
                    }

                    server.log("DELETE TABLE: Failed updating reservation table_number -> kept original. Code=" +
                            r.getConfirmationCode());
                    continue;
                }

                server.log("DELETE TABLE: Reservation relocated. Code=" + r.getConfirmationCode() +
                        " Time=" + start + " " + tableNumber + " -> " + newTableNum);

                affected++;
            }

        } catch (Exception e) {
            server.log("ERROR: relocateOrCancelReservationsForDeletedTable failed. " + e.getMessage());
        }

        return affected;
    }


    public boolean createReservationFromWaiting(
            String confirmationCode,
            LocalDateTime start,
            int guests,
            User user,
            int tableNumber) {

        if (confirmationCode == null || confirmationCode.isBlank()) return false;
        if (start == null || user == null) return false;

        boolean locked2h = tryReserveForTwoHours(start, tableNumber);
        if (!locked2h) return false;

        try {
            int reservationId = db.addReservation(
                    start,
                    guests,
                    confirmationCode.trim(),
                    user.getUserId(),
                    user.getUserRole(),
                    tableNumber
            );

            if (reservationId == -1) {
                rollbackReservation(start, tableNumber);
                return false;
            }

            scheduleReservationReminder2HoursBefore(user.getUserId(), start, confirmationCode.trim());

            server.log("Reservation created from waiting. Code=" + confirmationCode +
                       ", Table=" + tableNumber + ", ReservationId=" + reservationId);
            return true;

        } catch (Exception e) {
            rollbackReservation(start, tableNumber);
            server.log("ERROR: createReservationFromWaiting failed. Code=" + confirmationCode +
                       ", Msg=" + e.getMessage());
            return false;
        }
    }

    public boolean CancelReservation(String confirmationCode) {

        try {
            Reservation r = db.getReservationByConfirmationCode(confirmationCode);
            if (r == null || !r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
                server.log("WARN: Cancel request invalid/inactive. Code=" + confirmationCode);
                return false;
            }

            Integer tableNum = r.getTableNumber();
            LocalDateTime start = r.getReservationTime();

            if (tableNum == null || start == null) {
                server.log("ERROR: Cancel failed - missing table/reservation time. Code=" + confirmationCode);
                return false;
            }

            // 1) release 2 hours (4 slots)
            for (int i = 0; i < 4; i++) {
                try { restaurantController.releaseSlot(start.plusMinutes(30L * i), tableNum); }
                catch (Exception e) {
                    server.log("ERROR: Failed releasing slot during cancel. Slot=" +
                               start.plusMinutes(30L * i) + ", Table=" + tableNum + ", Msg=" + e.getMessage());
                }
            }

            // 2) update DB
            boolean cancelled = db.cancelReservationByConfirmationCode(confirmationCode);

            if (cancelled) {
                scheduleReservationCancelledPopupForLogin(r, "Your reservation was cancelled.");
                server.log("Reservation canceled. Code=" + confirmationCode);

                notifyWaitingTableFreed(tableNum);
                notifyPendingReservationCheckins(tableNum);
                return true;
            }


            server.log("WARN: Cancel DB update did not affect row. Code=" + confirmationCode);
            return false;

        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
            return false;
        }
    }
    private void scheduleReservationCancelledPopupForLogin(Reservation r, String reason) {
        if (notificationDB == null || r == null) return;

        try {
            LocalDateTime now = LocalDateTime.now();

            String smsBody =
                    "Your reservation was cancelled. Confirmation code: " + r.getConfirmationCode() +
                    (reason != null && !reason.isBlank() ? " Reason: " + reason : "");

            notificationDB.addNotification(new Notification(
                    r.getCreatedByUserId(),
                    Enums.Channel.SMS,
                    Enums.NotificationType.RESERVATION_CANCELLED, // תוסיפי ENUM כזה
                    smsBody,
                    now
            ));

        } catch (Exception e) {
            server.log("ERROR: scheduleReservationCancelledPopupForLogin failed: " + e.getMessage());
        }
    }


    private void notifyUserReservationCancelledIfOnline(Reservation r, String reason) {
        if (r == null) return;

        int userId = r.getCreatedByUserId();
        if (userId <= 0) return;

        String display = "Your reservation was cancelled.";
        String smsBody = "Your reservation was cancelled. Confirmation code: " + r.getConfirmationCode();
        if (reason != null && !reason.isBlank()) {
            smsBody += " Reason: " + reason;
        }

        // ✅ Popup only if user is connected
        server.pushPopupToUserIfOnline(userId, new dto.NotificationDTO(
                dto.NotificationDTO.Type.INFO,
                "SMS",
                display,
                smsBody
        ));
    }

    private void rollbackReservation(LocalDateTime requested, int tableNumber) {
        server.log("Rolling back: releasing 2 hours (4 slots) for table " + tableNumber);
        for (int i = 0; i < 4; i++) {
            try {
                restaurantController.releaseSlot(requested.plusMinutes(30L * i), tableNumber);
            } catch (Exception ignore) {}
        }
    }
    
    
    /**
     * Returns current diners (checked-in but not checked-out).
     */
    public ArrayList<Reservation> getCurrentDiners() {
        try {
            return db.getCurrentDiners();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load current diners. Msg=" + e.getMessage());
            return new ArrayList<>();
        }
    }


    // =====================================================
    // READ (QUERIES)
    // =====================================================

    public ArrayList<Reservation> getReservationsForUser(int userId) {
        try {
            return db.getReservationsByUser(userId);
        } catch (SQLException e) {
            server.log("ERROR: Failed to load reservations for user. UserId=" +
                       userId + ", Message=" + e.getMessage());
            return new ArrayList<>();
        }
    }

    public ArrayList<Reservation> getAllActiveReservations() {
        try {
            return db.getActiveReservations();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load active reservations. Message=" + e.getMessage());
            return null;
        }
    }

    public ArrayList<Reservation> getAllReservationsHistory() {
        try {
            return db.getAllReservations();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load reservations history. Message=" + e.getMessage());
            return null;
        }
    }

    public Reservation getReservationByConfirmationCode(String confirmationCode) {
        try {
            return db.getReservationByConfirmationCode(confirmationCode);
        } catch (SQLException e) {
            server.log("ERROR: Failed to find reservation by confirmation code. Code=" + confirmationCode +
                       ", Message=" + e.getMessage());
            return null;
        }
    }

    // =====================================================
    // UPDATE
    // =====================================================

    public boolean updateReservationStatus(int reservationId, ReservationStatus status) {
        try {
            boolean updated = db.updateReservationStatus(reservationId, status);

            if (!updated) {
                server.log("WARN: Reservation not found. Status not updated. ReservationId=" + reservationId);
                return false;
            }

            server.log("Reservation status updated. ReservationId=" + reservationId + ", Status=" + status);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to update reservation status. ReservationId=" + reservationId +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    public boolean updateReservationConfirmation(int reservationId, boolean isConfirmed) {
        try {
            boolean updated = db.updateIsConfirmed(reservationId, isConfirmed);

            if (!updated) {
                server.log("WARN: Reservation not found. is_confirmed not updated. ReservationId=" + reservationId);
                return false;
            }

            server.log("Reservation confirmation updated. ReservationId=" + reservationId + ", isConfirmed=" + isConfirmed);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to update reservation confirmation. ReservationId=" + reservationId +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    public boolean updateCheckinTime(int reservationId, LocalDateTime checkinTime) {
        try {
            boolean updated = db.updateCheckinTime(reservationId, checkinTime);

            if (!updated) {
                server.log("WARN: Reservation not found. Check-in not updated. ReservationId=" + reservationId);
                return false;
            }

            server.log("Check-in time updated. ReservationId=" + reservationId + ", Checkin=" + checkinTime);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to update check-in time. ReservationId=" + reservationId +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    public boolean updateCheckoutTime(int reservationId, LocalDateTime checkoutTime) {
        try {
            boolean updated = db.updateCheckoutTime(reservationId, checkoutTime);

            if (!updated) {
                server.log("WARN: Reservation not found. Check-out not updated. ReservationId=" + reservationId);
                return false;
            }

            server.log("Check-out time updated. ReservationId=" + reservationId + ", Checkout=" + checkoutTime);
            return true;

        } catch (Exception e) {
            server.log("ERROR: Failed to update check-out time. ReservationId=" + reservationId +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    public boolean FinishReservation(String confirmationCode) {

        try {
            Reservation r = db.getReservationByConfirmationCode(confirmationCode);
            if (r == null || !r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
                server.log("WARN: Finish request invalid/inactive. Code=" + confirmationCode);
                return false;
            }

            Integer tableNum = r.getTableNumber();
            LocalDateTime start = r.getReservationTime();

            if (tableNum == null || start == null) {
                server.log("ERROR: Finish failed - missing table/reservation time. Code=" + confirmationCode);
                return false;
            }

            // 1) Release 2 hours (4 slots)
            for (int i = 0; i < 4; i++) {
                LocalDateTime slot = start.plusMinutes(30L * i);
                try {
                    restaurantController.releaseSlot(slot, tableNum);
                } catch (Exception e) {
                    server.log("ERROR: Failed releasing slot during finish. Slot=" +
                               slot + ", Table=" + tableNum + ", Msg=" + e.getMessage());
                }
            }

            // 2) Update DB: Finished + checkout + inactive
            LocalDateTime checkoutTime = LocalDateTime.now();
            boolean finished = db.finishReservationByConfirmationCode(confirmationCode, checkoutTime);

            if (finished) {
                server.log("Reservation finished. Code=" + confirmationCode + ", Checkout=" + checkoutTime);

                // ✅ NEW: after payment/finish -> try to notify waiting list
                notifyWaitingTableFreed(tableNum);

                return true;
            }

            server.log("WARN: Finish DB update did not affect row. Code=" + confirmationCode);
            return false;

        } catch (SQLException e) {
            server.log("ERROR: Failed to finish reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
            return false;
        }
    }

    // ========= recovery helpers (kept as-is) =========

    public Reservation findGuestReservationByContactAndTime(
            String phone,
            String email,
            LocalDateTime dateTime) {

        if (((phone == null || phone.isBlank()) &&
             (email == null || email.isBlank())) ||
             dateTime == null) {
            return null;
        }

        try {
            return db.findGuestReservationByContactAndTime(phone, email, dateTime);
        } catch (Exception e) {
            server.log("ERROR: Recover guest confirmation failed. " + e.getMessage());
            return null;
        }
    }

    public String recoverGuestConfirmationCode(
            String phone,
            String email,
            java.time.LocalDateTime reservationDateTime) {

        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }
        if (reservationDateTime == null) {
            return null;
        }

        try {
            java.util.ArrayList<Integer> guestIds =
                    db.getGuestIdsByContact(phone, email);

            if (guestIds == null || guestIds.isEmpty()) {
                return null;
            }

            return db.findGuestConfirmationCodeByDateTimeAndGuestIds(reservationDateTime, guestIds);

        } catch (Exception e) {
            server.log("ERROR: recoverGuestConfirmationCode failed: " + e.getMessage());
            return null;
        }
    }

    public Reservation createNowReservationFromWaiting(
            LocalDateTime start,
            int guests,
            User user,
            String confirmationCode
    ) {
        if (start == null || user == null || confirmationCode == null || confirmationCode.isBlank()) return null;

        Table table;
        try {
            table = restaurantController.getOneAvailableTableAt(start, guests);
        } catch (Exception e) {
            server.log("ERROR: Waiting -> check availability failed. " + e.getMessage());
            return null;
        }

        if (table == null) return null;

        boolean locked2h = tryReserveForTwoHours(start, table.getTableNumber());
        if (!locked2h) return null;

        Reservation res = new Reservation();
        res.setCreatedByUserId(user.getUserId());
        res.setCreatedByRole(user.getUserRole());
        res.setGuestAmount(guests);
        res.setReservationTime(start);
        res.setConfirmed(true);
        res.setActive(true);
        res.setTableNumber(table.getTableNumber());
        res.setReservationStatus(ReservationStatus.Active);

        res.setConfirmationCode(confirmationCode);

        try {
            int reservationId = db.addReservation(
                    start,
                    guests,
                    confirmationCode,
                    user.getUserId(),
                    user.getUserRole(),
                    table.getTableNumber()
            );

            if (reservationId == -1) {
                rollbackReservation(start, table.getTableNumber());
                return null;
            }

            res.setReservationId(reservationId);

            scheduleReservationReminder2HoursBefore(user.getUserId(), start, confirmationCode);

            return res;

        } catch (SQLException e) {
            server.log("ERROR: Waiting -> addReservation failed. " + e.getMessage());
            rollbackReservation(start, table.getTableNumber());
            return null;
        }
    }
    

 // =====================================================
 // CHECK-IN (GET TABLE) FROM RESERVATION
 // Rule: allowed only from reservation time until +15 minutes
 // =====================================================
    public GetTableResultDTO checkinReservationByCode(String confirmationCode) {

        if (confirmationCode == null || confirmationCode.isBlank()) {
            return new GetTableResultDTO(false, false, null, "Confirmation code is required.");
        }

        String code = confirmationCode.trim();

        try {
            Reservation r = db.getReservationByConfirmationCode(code);

            if (r == null) {
                return new GetTableResultDTO(false, false, null, "Reservation not found.");
            }

            if (!r.isActive() || r.getReservationStatus() != ReservationStatus.Active) {
                return new GetTableResultDTO(false, false, null, "Reservation is not active.");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime resTime = r.getReservationTime();

            if (resTime == null) {
                return new GetTableResultDTO(false, false, null, "Reservation time is missing.");
            }

            // TOO EARLY -> DO NOT EXPOSE TABLE NUMBER
            if (now.isBefore(resTime)) {
                return new GetTableResultDTO(
                        false,
                        false,
                        null,
                        "It is too early to check in. Please arrive at your reservation time."
                );
            }

            // TOO LATE -> DO NOT EXPOSE TABLE NUMBER
            if (now.isAfter(resTime.plusMinutes(15))) {
                return new GetTableResultDTO(
                        false,
                        false,
                        null,
                        "Check-in time has expired (15 minutes after reservation time)."
                );
            }

            Integer tableNumber = r.getTableNumber();
            if (tableNumber == null) {
                return new GetTableResultDTO(false, false, null,
                        "No table has been assigned yet. Please contact the restaurant.");
            }

            // prevent double check-in
            if (r.getCheckinTime() != null) {
                return new GetTableResultDTO(
                        true,
                        false,
                        tableNumber,
                        "You are already checked-in. Your table number is: " + tableNumber
                );
            }

            boolean occupied = db.isTableOccupiedNow(tableNumber, r.getReservationId());

            // If table is free -> do real check-in + schedule bill time + create receipt
            if (!occupied) {

                boolean updated = db.updateCheckinTime(r.getReservationId(), now);
                if (!updated) {
                    return new GetTableResultDTO(false, false, null, "Failed to update check-in time.");
                }

                // mark bill due = checkin + 2 hours
                try {
                    db.setBillDueAt(r.getReservationId(), now.plusHours(2));
                } catch (Exception ignore) {}

                // create Receipt now (random amount) - via ReceiptController
                try {
                    if (receiptController != null) {
                        receiptController.createReceiptIfMissingForCheckin(r, now);
                    }
                } catch (Exception ignore) {}

                return new GetTableResultDTO(
                        true,
                        false,
                        tableNumber,
                        "Checked-in successfully. Your table number is: " + tableNumber
                );
            }

            // Table is occupied -> remember pending check-in and notify later when table freed
            pendingCheckins.put(tableNumber, new PendingReservationCheckin(
                    r.getReservationId(),
                    r.getCreatedByUserId(),
                    r.getConfirmationCode(),
                    tableNumber
            ));

            return new GetTableResultDTO(
                    false,
                    true,
                    tableNumber,
                    "Your table is not ready yet. Please wait for a notification."
            );

        } catch (Exception e) {
            server.log("ERROR: checkinReservationByCode failed. Code=" + code + ", Msg=" + e.getMessage());
            return new GetTableResultDTO(false, false, null, "Server error. Please try again.");
        }
    }



    private void notifyPendingReservationCheckins(Integer tableNumber) {
        if (tableNumber == null) return;

        PendingReservationCheckin pending = pendingCheckins.remove(tableNumber);
        if (pending == null) return;

        try {
            LocalDateTime now = LocalDateTime.now();

            if (notificationDB != null) {
                String body =
                        "Your reserved table is now available. Please check in with your confirmation code: " +
                        pending.confirmationCode;

                notificationDB.addNotification(new Notification(
                        pending.userId,
                        Enums.Channel.SMS,
                        Enums.NotificationType.TABLE_AVAILABLE,
                        body,
                        now
                ));

                notificationDB.addNotification(new Notification(
                        pending.userId,
                        Enums.Channel.EMAIL,
                        Enums.NotificationType.TABLE_AVAILABLE,
                        body,
                        now
                ));
            }

            server.log("Pending reservation notified. Code=" + pending.confirmationCode +
                       ", Table=" + tableNumber + ", UserId=" + pending.userId);

        } catch (Exception e) {
            server.log("ERROR: notifyPendingReservationCheckins failed. Table=" + tableNumber +
                       ", Msg=" + e.getMessage());
        }
    }

    

    
    /**
     * ✅ NEW: Updates the full reservation details based on manager input.
     */
    public boolean updateReservationFromManager(Reservation res) {
        if (res == null || res.getReservationId() <= 0) return false;
        
        try {
            boolean success = db.updateFullReservationDetails(res);
            if (success) {
                server.log("Reservation updated by manager. ID=" + res.getReservationId());
            }
            return success;
        } catch (SQLException e) {
            server.log("ERROR: Failed to update reservation. ID=" + res.getReservationId() + ", Msg=" + e.getMessage());
            return false;
        }
    }

    public ArrayList<LocalTime> getAvailableTimesForDay(LocalDate date, int guests) {
        ArrayList<LocalTime> out = new ArrayList<>();
        if (date == null || guests <= 0) return out;

        LocalTime fromTime = null;

        // אם זה היום - רק משעה+1 קדימה, מעוגל לחצי שעה
        if (date.equals(LocalDate.now())) {
            LocalDateTime rounded =
                    restaurantController.roundUpToNextHalfHour(LocalDateTime.now().plusHours(1));
            fromTime = rounded.toLocalTime();
        }

        fillAvailableTimesForDay(date, guests, fromTime, out);
        return out;
    }
    
 // =====================================================
 // AUTO-CANCEL: reservations without check-in (15 min)
 // =====================================================
 public int cancelReservationsWithoutCheckinAfterGracePeriod() {

     int cancelledCount = 0;

     try {
         LocalDateTime now = LocalDateTime.now();

         // מביא רק הזמנות שעברו 15 דק' ממועד ההזמנה
         // ועדיין אין להן check-in
         ArrayList<String> expiredCodes =
                 db.getReservationsWithoutCheckinExpired(now.minusMinutes(15));

         for (String code : expiredCodes) {
             if (code == null) continue;

             boolean cancelled = CancelReservation(code);
             if (cancelled) {
                 cancelledCount++;

                 // ✅ לוג פשוט וברור – בדיוק כמו שביקשת
                 server.log(
                     "Reservation auto-cancelled (no check-in after 15 min): " + code
                 );
             }
         }

         return cancelledCount;

     } catch (Exception e) {
         server.log(
             "ERROR: cancelReservationsWithoutCheckinAfterGracePeriod failed. " + e.getMessage()
         );
         return 0;
     }
 }



    public int cancelReservationsDueToOpeningHoursChange(
            LocalDate date,
            LocalTime newOpen,
            LocalTime newClose,
            boolean isClosed
    ) {

        int cancelledCount = 0;

        try {
            // בטיחות: רק חודש קדימה
            if (date.isAfter(LocalDate.now().plusMonths(1))) {
                server.log("Skip cancel: date beyond 1 month: " + date);
                return 0;
            }

            ArrayList<Reservation> reservations =
                    db.getActiveReservationsByDate(date);

            for (Reservation r : reservations) {

                LocalTime resTime = r.getReservationTime().toLocalTime();

                boolean invalid;

                if (isClosed) {
                    invalid = true;
                } else {
                    invalid =
                            resTime.isBefore(newOpen) ||
                            resTime.isAfter(newClose.minusHours(2));
                }

                if (!invalid) continue;

                // משתמשים בלוגיקה הקיימת שלך
                boolean cancelled = CancelReservation(r.getConfirmationCode());

                if (cancelled) {
                    cancelledCount++;

                    notifyReservationCancelledOpeningHours(r);
                }
            }

        } catch (Exception e) {
            server.log("ERROR: cancelReservationsDueToOpeningHoursChange failed: " + e.getMessage());
        }

        return cancelledCount;
    }
    
    private void notifyReservationCancelledOpeningHours(Reservation r) {

        if (notificationDB == null || r == null) return;

        LocalDateTime now = LocalDateTime.now();

        String body =
            "Your reservation on " +
            r.getReservationTime().toLocalDate() +
            " at " +
            r.getReservationTime().toLocalTime().toString().substring(0,5) +
            " was cancelled due to a change in opening hours.";

        try {
			notificationDB.addNotification(new Notification(
			        r.getCreatedByUserId(),
			        Enums.Channel.SMS,
			        Enums.NotificationType.RESERVATION_CANCELLED_OPENING_HOURS,
			        body,
			        now
			));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        try {
			notificationDB.addNotification(new Notification(
			        r.getCreatedByUserId(),
			        Enums.Channel.EMAIL,
			        Enums.NotificationType.RESERVATION_CANCELLED_OPENING_HOURS,
			        body,
			        now
			));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public ArrayList<Reservation> getActiveReservationsForUser(int userId) {
        try {
            return db.getActiveReservationsByUser(userId);
        } catch (Exception e) {
            server.log("ERROR: getActiveReservationsForUser failed. UserId=" + userId + ", Msg=" + e.getMessage());
            return new ArrayList<>();
        }
    }



}
