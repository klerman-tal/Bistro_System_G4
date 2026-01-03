package logicControllers;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import application.RestaurantServer;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import entities.Enums.ReservationStatus;
import entities.Enums.UserRole;
import entities.Reservation;
import entities.Restaurant;
import entities.User;
import gui.ServerGUIController;

public class ReservationController {
	
	private final Reservation_DB_Controller db;
	private final Restaurant restaurant;
	private RestaurantServer server;
    
    public ReservationController(Reservation_DB_Controller db, RestaurantServer server) {
    	
    	this.db = db;
    	this.server = server;
    	this.restaurant = Restaurant.getInstance();
    	
    }
    
    
	public Reservation CreateTableReservation(LocalDate date, LocalTime time, int guestsNumber, User user) {
		
		//לבדוק אם יש פנוי ?
		//אם לא- תחזיר רשימת שעות פנויות
		//אם כן-
		
		LocalDateTime now = LocalDateTime.now();
	    LocalDateTime requested = LocalDateTime.of(date, time);

	    if (requested.isBefore(now.plusHours(1)) || requested.isAfter(now.plusMonths(1))) {
	        server.log("WARN: Invalid reservation time requested. UserId=" + user.getUserId() +
	                   ", Requested=" + requested + ", Now=" + now);
	        return null;
	    }
		
		Reservation res = new Reservation();
		
		res.setCreatedByUserId(user.getUserId());
		res.setGuestAmount(guestsNumber);
		res.setReservationTime(LocalDateTime.of(date, time));
		res.setConfirmed(true);
		
		 try {
		        int reservationId = db.addReservation(
		            res.getReservationTime(),
		            guestsNumber,
		            res.getConfirmationCode(),
		            user.getUserId()
		        );

		        if (reservationId == -1) {
		            server.log("ERROR: Reservation insert failed (no ID returned)");
		            return null;
		        }

		        res.setReservationId(reservationId);
		        //לשנות בטבלה של השולחנות הפנויים לתפוס
		        //לשלוח הודעה עם קוד אימות של ההזמנה
		        //לשנות בהאשמאפ של השולחנות הפנויים (מרגע ההזמנה עד שעתיים אחר כך) 
		        
		    } catch (SQLException e) {
		        server.log(
		            "ERROR: Failed to insert reservation into DB. " +
		            "UserId=" + user.getUserId() +
		            ", Message=" + e.getMessage()
		        );
		        return null;
		    }

		    server.log(
		        "Table reservation created. " +
		        "ReservationId=" + res.getReservationId() +
		        ", ConfirmationCode=" + res.getConfirmationCode()
		    );

		    return res;
		}
	
	public boolean CancelReservation(String confirmationCode) {
		try {
		    if (!db.isActiveReservationExists(confirmationCode)) {
		        server.log("WARN: Cancel request with invalid or inactive confirmation code: " + confirmationCode);
		        return false;
		    }

		    boolean canceled =
		    		db.deactivateReservationByConfirmationCode(confirmationCode);

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
	
	public ArrayList<Reservation> getReservationsForUser(User user) {
	    try {
	        return db.getReservationsByUser(user.getUserId());
	    } catch (SQLException e) {
	        server.log("ERROR: Failed to load reservations for user. UserId=" +
	                   user.getUserId() + ", Message=" + e.getMessage());
	        return null;
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
	        server.log(
	            "ERROR: Failed to find reservation by confirmation code. " +
	            "Code=" + confirmationCode +
	            ", Message=" + e.getMessage()
	        );
	        return null;
	    }
	}
	
	public boolean updateReservationStatus(int reservationId, ReservationStatus status) {
	    try {
	        boolean updated = db.updateReservationStatus(reservationId, status);

	        if (!updated) {
	            server.log("WARN: Reservation not found. Status not updated. ReservationId=" +
	                       reservationId);
	            return false;
	        }

	        server.log("Reservation status updated. ReservationId=" +
	                   reservationId + ", Status=" + status);
	        return true;

	    } catch (Exception e) {
	        server.log("ERROR: Failed to update reservation status. ReservationId=" +
	                   reservationId + ", Message=" + e.getMessage());
	        return false;
	    }
	}


	public boolean updateReservationConfirmation(int reservationId, boolean isConfirmed) {
	    try {
	        boolean updated = db.updateIsConfirmed(reservationId, isConfirmed);

	        if (!updated) {
	            server.log("WARN: Reservation not found. is_confirmed not updated. ReservationId=" +
	                       reservationId);
	            return false;
	        }

	        server.log("Reservation confirmation updated. ReservationId=" +
	                   reservationId + ", isConfirmed=" + isConfirmed);
	        return true;

	    } catch (Exception e) {
	        server.log("ERROR: Failed to update reservation confirmation. ReservationId=" +
	                   reservationId + ", Message=" + e.getMessage());
	        return false;
	    }
	}


	public boolean updateCheckinTime(int reservationId, LocalDateTime checkinTime) {
	    try {
	        boolean updated = db.updateCheckinTime(reservationId, checkinTime);

	        if (!updated) {
	            server.log("WARN: Reservation not found. Check-in not updated. ReservationId=" +
	                       reservationId);
	            return false;
	        }

	        server.log("Check-in time updated. ReservationId=" +
	                   reservationId + ", Checkin=" + checkinTime);
	        return true;

	    } catch (Exception e) {
	        server.log("ERROR: Failed to update check-in time. ReservationId=" +
	                   reservationId + ", Message=" + e.getMessage());
	        return false;
	    }
	}


	public boolean updateCheckoutTime(int reservationId, LocalDateTime checkoutTime) {
	    try {
	        boolean updated = db.updateCheckoutTime(reservationId, checkoutTime);

	        if (!updated) {
	            server.log("WARN: Reservation not found. Check-out not updated. ReservationId=" +
	                       reservationId);
	            return false;
	        }

	        server.log("Check-out time updated. ReservationId=" +
	                   reservationId + ", Checkout=" + checkoutTime);
	        return true;

	    } catch (Exception e) {
	        server.log("ERROR: Failed to update check-out time. ReservationId=" +
	                   reservationId + ", Message=" + e.getMessage());
	        return false;
	    }
	}



}
