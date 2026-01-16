package entities;

/**
 * Central container class for all system enumerations.
 * <p>
 * This class groups together enums that represent fixed sets of values
 * used across the system, such as user roles, reservation statuses,
 * payment types, and notification metadata.
 * </p>
 */
public class Enums {
	
    /**
     * Represents the role of a user in the system.
     */
	public enum UserRole{
		RandomClient,
		Subscriber,
		RestaurantAgent,
		RestaurantManager
	}
	
    /**
     * Represents the days of the week.
     */
	public enum Days{
		Sunday,
		Monday,
		Tuesday,
		Wednesday,
		Thursday,
		Friday,
		Saturday
	}
	
    /**
     * Represents supported payment methods.
     */
	public enum TypeOfPayment{
		CreditCard,
		Cash
	}
	
    /**
     * Represents the lifecycle status of a reservation.
     */
	public enum ReservationStatus{
		Active, 
		Finished,
		Cancelled
	}
	
    /**
     * Represents the current status of a waiting list entry.
     */
	public enum WaitingStatus {
	    Waiting,   
	    Seated,      
	    Cancelled
	}

    /**
     * Represents the communication channel used for notifications.
     */
	public enum Channel {
        SMS,
        EMAIL
    }

    /**
     * Represents the type of notification sent by the system.
     */
    public enum NotificationType {
        RESERVATION_REMINDER_2H,
        TABLE_AVAILABLE,
        RESEND_CONFIRMATION_CODE,
        BILL_AFTER_2H_FROM_CHECKIN,
        RESERVATION_CANCELLED_OPENING_HOURS,
        MONTHLY_REPORT_READY
    }
}
