package logicControllers;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import application.RestaurantServer;
import dbControllers.Waiting_DB_Controller;
import entities.User;
import entities.Waiting;

public class WaitingController {
//test
    private final Waiting_DB_Controller db;
    private final RestaurantServer server;

    public WaitingController(Waiting_DB_Controller db, RestaurantServer server) {
        this.db = db;
        this.server = server;
    }

    public Waiting joinWaitingList(int guestsNumber, User user) {
        Waiting w = new Waiting();

        w.setCreatedByUserId(user.getUserId());
        w.setCreatedByRole(user.getUserRole());
        w.setGuestAmount(guestsNumber);

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
            server.log("ERROR: Failed to insert waiting entry. UserId=" +
                       user.getUserId() + ", Message=" + e.getMessage());
            return null;
        }

        server.log("Waiting entry created. WaitingId=" + w.getWaitingId() +
                   ", ConfirmationCode=" + w.getConfirmationCode());
        return w;
    }

    public boolean leaveWaitingList(String confirmationCode) {
        try {
            boolean cancelled = db.cancelWaiting(confirmationCode);

            if (!cancelled) {
                server.log("WARN: Waiting not found/active. Cancel failed. Code=" + confirmationCode);
                return false;
            }

            server.log("Waiting cancelled. ConfirmationCode=" + confirmationCode);
            return true;

        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel waiting. Code=" + confirmationCode +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    // Call when a table becomes available for this confirmation code
    // This starts the 15 minutes countdown (table_freed_time = now)
    public boolean notifyTableFreed(String confirmationCode, Integer tableNumber) {
        try {
            boolean updated = db.setTableFreedForWaiting(confirmationCode, LocalDateTime.now(), tableNumber);

            if (!updated) {
                server.log("WARN: Waiting not found/active. Table freed not set. Code=" + confirmationCode);
                return false;
            }

            server.log("Table freed for waiting. Code=" + confirmationCode +
                       ", Table=" + tableNumber);
            return true;

        } catch (SQLException e) {
            server.log("ERROR: Failed to set table freed time. Code=" + confirmationCode +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    // Called when the client arrives and enters the confirmation code
    public boolean confirmArrivalAndSeat(String confirmationCode) {
        try {
            boolean seated = db.markWaitingAsSeated(confirmationCode);

            if (!seated) {
                server.log("WARN: Waiting not found/active. Seat failed. Code=" + confirmationCode);
                return false;
            }

            server.log("Waiting seated. ConfirmationCode=" + confirmationCode);
            return true;

        } catch (SQLException e) {
            server.log("ERROR: Failed to seat waiting. Code=" + confirmationCode +
                       ", Message=" + e.getMessage());
            return false;
        }
    }

    // Run periodically on server (e.g., every minute)
    public int cancelExpiredWaitings() {
        try {
            int count = db.cancelExpiredWaitings(LocalDateTime.now());
            if (count > 0) server.log("Cancelled expired waitings. Count=" + count);
            return count;
        } catch (SQLException e) {
            server.log("ERROR: Failed to cancel expired waitings. Message=" + e.getMessage());
            return 0;
        }
    }

    public ArrayList<Waiting> getActiveWaitingList() {
        try {
            return db.getActiveWaitingList();
        } catch (SQLException e) {
            server.log("ERROR: Failed to load active waiting list. Message=" + e.getMessage());
            return null;
        }
    }

    public Waiting getWaitingByConfirmationCode(String confirmationCode) {
        try {
            return db.getWaitingByConfirmationCode(confirmationCode);
        } catch (SQLException e) {
            server.log("ERROR: Failed to find waiting by confirmation code. Code=" +
                       confirmationCode + ", Message=" + e.getMessage());
            return null;
        }
    }

    public ArrayList<Waiting> getWaitingListForUser(User user) {
        try {
            return db.getWaitingListByUser(user.getUserId());
        } catch (SQLException e) {
            server.log("ERROR: Failed to load waiting list for user. UserId=" +
                       user.getUserId() + ", Message=" + e.getMessage());
            return null;
        }
    }
}
