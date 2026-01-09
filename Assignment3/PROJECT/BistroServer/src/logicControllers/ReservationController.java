package logicControllers;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import application.RestaurantServer;
import dbControllers.Reservation_DB_Controller;
import dto.CreateReservationDTO;
import entities.Enums.ReservationStatus;
import entities.Reservation;
import entities.Restaurant;
import entities.Table;
import entities.User;

public class ReservationController {

    private final Reservation_DB_Controller db;
    private final Restaurant restaurant;
    private final RestaurantServer server;
    private final RestaurantController restaurantController;

    /**
     * Constructor: connects controller to DB layer, server logger, and restaurant availability logic.
     */
    public ReservationController(Reservation_DB_Controller db, RestaurantServer server, RestaurantController rc) {
        this.db = db;
        this.server = server;
        this.restaurant = Restaurant.getInstance();
        this.restaurantController = rc;
    }

    // =====================================================
    // AVAILABILITY HELPERS
    // =====================================================

    /**
     * Fills a list with available times for the given day (starting from fromTime).
     */
    private void fillAvailableTimesForDay(
            LocalDate date,
            int guestsNumber,
            LocalTime fromTime,
            ArrayList<LocalTime> out) {

        if (out == null) return;
        out.clear();

        try {
            // For each slot in that day -> one candidate table that fits guests (single-slot availability)
            Map<LocalDateTime, Table> map =
                    restaurantController.getOneAvailableTablePerSlot(date, guestsNumber);

            // keySet order is not guaranteed -> sort by time
            ArrayList<LocalDateTime> times = new ArrayList<>(map.keySet());
            times.sort(Comparator.naturalOrder());

            for (LocalDateTime dt : times) {
                LocalTime t = dt.toLocalTime();

                // Only times from the requested time and onward
                if (fromTime != null && t.isBefore(fromTime)) continue;

                Table table = map.get(dt);
                if (table == null) continue;

                // IMPORTANT: must be free for 2 hours (4 slots), not just one slot
                if (restaurantController.isTableFreeForTwoHours(dt, table.getTableNumber())) {
                    out.add(t);
                }
            }

        } catch (Exception e) {
            server.log("ERROR: Failed to load available times. " + e.getMessage());
        }
    }



    /**
     * Attempts to reserve a table for 2 hours (4 slots of 30 minutes). Rolls back on failure.
     */
    private boolean tryReserveForTwoHours(LocalDateTime start, int tableNumber) {
        ArrayList<LocalDateTime> locked = new ArrayList<>();
        LocalDateTime slot = start;

        try {
            for (int i = 0; i < 4; i++) {
                boolean ok = restaurantController.tryReserveSlot(slot, tableNumber);
                if (!ok) {
                    // Rollback: release what we already locked
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
            // Rollback: release what we already locked
            for (LocalDateTime s : locked) {
                try { restaurantController.releaseSlot(s, tableNumber); }
                catch (Exception ignore) {}
            }

            server.log("ERROR: Failed to reserve 2 hours slots. " + e.getMessage());
            return false;
        }
    }

    // =====================================================
    // CREATE / CANCEL RESERVATION
    // =====================================================

    /**
     * Creates a table reservation:
     * - validates time window (1 hour ahead up to 1 month)
     * - finds a suitable table
     * - locks the table for 2 hours (4 slots)
     * - inserts reservation into DB
     * If not available, fills availableTimesOut and returns null.
     */
    public Reservation CreateTableReservation(
            CreateReservationDTO dto, 
            ArrayList<LocalTime> availableTimesOut) {

        if (availableTimesOut != null) availableTimesOut.clear();

        LocalDateTime now = LocalDateTime.now();
        // שימוש בנתונים מה-DTO
        LocalDateTime requested = LocalDateTime.of(dto.getDate(), dto.getTime());

        // בדיקת חלון זמן (שעה קדימה עד חודש)
        if (requested.isBefore(now.plusHours(1)) || requested.isAfter(now.plusMonths(1))) {
            server.log("WARN: Invalid reservation time. UserId=" + dto.getUserId());
            return null;
        }

        Table table;
        try {
            // בדיקת זמינות שולחן לפי כמות האורחים מה-DTO
            table = restaurantController.getOneAvailableTableAt(requested, dto.getGuests());
        } catch (Exception e) {
            server.log("ERROR: Failed to check availability. " + e.getMessage());
            return null;
        }

        if (table == null) {
            fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
            return null;
        }

        // ניסיון נעילת שולחן ל-2 פעימות (שעה) או יותר לפי הלוגיקה שלך
        boolean locked2h = tryReserveForTwoHours(requested, table.getTableNumber());
        if (!locked2h) {
            fillAvailableTimesForDay(dto.getDate(), dto.getGuests(), dto.getTime(), availableTimesOut);
            return null;
        }

        // יצירת אובייקט ה-Entity לשמירה ב-DB
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

        } catch (SQLException e) {
            server.log("ERROR: DB Insert failed: " + e.getMessage());
            rollbackReservation(requested, table.getTableNumber());
            return null;
        }

        return res;
    }
    /**
     * Cancels (deactivates) a reservation by confirmation code.
     */
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
                    // ממשיכים לשחרר אחרים כדי לא להשאיר חצי נעול
                }
            }

            // 2) update DB
            boolean cancelled = db.cancelReservationByConfirmationCode(confirmationCode);

            if (cancelled) {
                server.log("Reservation canceled. Code=" + confirmationCode);
                return true;
            }

            server.log("WARN: Cancel DB update did not affect row. Code=" + confirmationCode);
            return false;

        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
            return false;
        }
    }
    

    /**
     * מתודת עזר לשחרור השולחן במידה וההרשמה ל-DB נכשלה
     */
    private void rollbackReservation(LocalDateTime requested, int tableNumber) {
        server.log("Rolling back: releasing 2 hours (4 slots) for table " + tableNumber);
        for (int i = 0; i < 4; i++) {
            try { 
                restaurantController.releaseSlot(requested.plusMinutes(30L * i), tableNumber); 
            } catch (Exception ignore) {
                // אנחנו כבר בתוך שגיאה, אז רק מנסים לנקות כמה שאפשר
            }
        }
    }

    // =====================================================
    // READ (QUERIES)
    // =====================================================

    /**
     * Returns all reservations created by the given user.
     */
    public ArrayList<Reservation> getReservationsForUser(User user) {
        try {
            return db.getReservationsByUser(user.getUserId());
        } catch (SQLException e) {
            server.log("ERROR: Failed to load reservations for user. UserId=" +
                       user.getUserId() + ", Message=" + e.getMessage());
            return null;
        }
    }

    /**
     * Returns all active reservations (DB filter).
     */
    public ArrayList<Reservation> getAllActiveReservations() {
        try {
            return db.getActiveReservations();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load active reservations. Message=" + e.getMessage());
            return null;
        }
    }

    /**
     * Returns full reservations history.
     */
    public ArrayList<Reservation> getAllReservationsHistory() {
        try {
            return db.getAllReservations();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load reservations history. Message=" + e.getMessage());
            return null;
        }
    }

    /**
     * Returns reservation by confirmation code (or null if not found).
     */
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

    /**
     * Updates the reservation status in DB.
     */
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

    /**
     * Updates the reservation confirmation flag (is_confirmed) in DB.
     */
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

    /**
     * Updates check-in time in DB.
     */
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

    /**
     * Updates check-out time in DB.
     */
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

            // 1) Release 2 hours (4 slots) in availability grid
            for (int i = 0; i < 4; i++) {
                LocalDateTime slot = start.plusMinutes(30L * i);
                try {
                    restaurantController.releaseSlot(slot, tableNum);
                } catch (Exception e) {
                    server.log("ERROR: Failed releasing slot during finish. Slot=" +
                               slot + ", Table=" + tableNum + ", Msg=" + e.getMessage());
                    // Continue releasing the rest to avoid partial lock
                }
            }

            // 2) Update DB: Finished + checkout + inactive
            LocalDateTime checkoutTime = LocalDateTime.now();
            boolean finished = db.finishReservationByConfirmationCode(confirmationCode, checkoutTime);

            if (finished) {
                server.log("Reservation finished. Code=" + confirmationCode + ", Checkout=" + checkoutTime);
                return true;
            }

            server.log("WARN: Finish DB update did not affect row. Code=" + confirmationCode);
            return false;

        } catch (SQLException e) {
            server.log("ERROR: Failed to finish reservation. Code=" + confirmationCode + ", Message=" + e.getMessage());
            return false;
        }
    }
    
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

        // Validate: must have at least one contact
        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }
        if (reservationDateTime == null) {
            return null;
        }

        try {
            // 1) Find all guest ids that match phone/email
            java.util.ArrayList<Integer> guestIds =
                    db.getGuestIdsByContact(phone, email);

            if (guestIds == null || guestIds.isEmpty()) {
                return null;
            }

            // 2) Find reservation by exact datetime + created_by in those guestIds
            return db.findGuestConfirmationCodeByDateTimeAndGuestIds(reservationDateTime, guestIds);

        } catch (Exception e) {
            server.log("ERROR: recoverGuestConfirmationCode failed: " + e.getMessage());
            return null;
        }
    }



}
