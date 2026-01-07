package logicControllers;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

import application.RestaurantServer;
import dbControllers.Reservation_DB_Controller;
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
            Map<LocalDateTime, Table> map =
                    restaurantController.getOneAvailableTablePerSlot(date, guestsNumber);

            for (LocalDateTime dt : map.keySet()) {
                LocalTime t = dt.toLocalTime();

                // Only times from the requested time and onward
                if (fromTime == null || !t.isBefore(fromTime)) {
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
            LocalDate date,
            LocalTime time,
            int guestsNumber,
            User user,
            ArrayList<LocalTime> availableTimesOut) {

        if (availableTimesOut != null) availableTimesOut.clear();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requested = LocalDateTime.of(date, time);

        if (requested.isBefore(now.plusHours(1)) || requested.isAfter(now.plusMonths(1))) {
            server.log("WARN: Invalid reservation time requested. UserId=" + user.getUserId() +
                       ", Requested=" + requested + ", Now=" + now);
            return null;
        }

        Table table;
        try {
            // Find a table for the first slot
            table = restaurantController.getOneAvailableTableAt(requested, guestsNumber);
        } catch (Exception e) {
            server.log("ERROR: Failed to check availability. " + e.getMessage());
            return null;
        }

        // No table for the requested time -> return alternative times
        if (table == null) {
            fillAvailableTimesForDay(date, guestsNumber, time, availableTimesOut);
            return null;
        }

        // Reserve the table for 2 hours (4 slots)
        boolean locked2h = tryReserveForTwoHours(requested, table.getTableNumber());
        if (!locked2h) {
            fillAvailableTimesForDay(date, guestsNumber, time, availableTimesOut);
            return null;
        }

        Reservation res = new Reservation();
        res.setCreatedByUserId(user.getUserId());
        res.setGuestAmount(guestsNumber);
        res.setReservationTime(requested);
        res.setConfirmed(true);

        try {
            int reservationId = db.addReservation(
                    res.getReservationTime(),
                    guestsNumber,
                    res.getConfirmationCode(),
                    user.getUserId(),
                    user.getUserRole(),
                    table.getTableNumber()
            );

            if (reservationId == -1) {
                server.log("ERROR: Reservation insert failed (no ID returned)");

                // Rollback: release 2 hours (4 slots)
                for (int i = 0; i < 4; i++) {
                    try { restaurantController.releaseSlot(requested.plusMinutes(30L * i), table.getTableNumber()); }
                    catch (Exception ignore) {}
                }
                return null;
            }

            res.setReservationId(reservationId);

        } catch (SQLException e) {
            server.log("ERROR: Failed to insert reservation into DB. UserId=" + user.getUserId() +
                       ", Message=" + e.getMessage());

            // Rollback: release 2 hours (4 slots)
            for (int i = 0; i < 4; i++) {
                try { restaurantController.releaseSlot(requested.plusMinutes(30L * i), table.getTableNumber()); }
                catch (Exception ignore) {}
            }

            return null;
        }

        server.log("Table reservation created. ReservationId=" + res.getReservationId() +
                   ", ConfirmationCode=" + res.getConfirmationCode());

        return res;
    }

    /**
     * Cancels (deactivates) a reservation by confirmation code.
     */
    public boolean CancelReservation(String confirmationCode) {
        try {
            if (!db.isActiveReservationExists(confirmationCode)) {
                server.log("WARN: Cancel request with invalid or inactive confirmation code: " + confirmationCode);
                return false;
            }

            boolean canceled = db.deactivateReservationByConfirmationCode(confirmationCode);

            if (canceled) {
                server.log("Reservation canceled. ConfirmationCode=" + confirmationCode);
                return true;
            } else {
                server.log("WARN: Failed to cancel reservation (no row updated). Code=" + confirmationCode);
                return false;
            }

        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel reservation. Code=" + confirmationCode +
                       ", Message=" + e.getMessage());
            return false;
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
}
