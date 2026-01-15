package entities;

public class Enums {
	
	public enum UserRole{
		RandomClient,
		Subscriber,
		RestaurantAgent,
		RestaurantManager
	}
	
	public enum Days{
		Sunday,
		Monday,
		Tuesday,
		Wednesday,
		Thursday,
		Friday,
		Saturday
	}
	
	public enum TypeOfPayment{
		CreditCard,
		Cash
	}
	
	public enum ReservationStatus{
		Active, 
		Finished,
		Cancelled
	}
	
	public enum WaitingStatus {
	    Waiting,   
	    Seated,      
	    Cancelled
	}

	public enum Channel {
        SMS,
        EMAIL
    }

    public enum NotificationType {
        RESERVATION_REMINDER_2H,
        TABLE_AVAILABLE,
        RESEND_CONFIRMATION_CODE,
        BILL_AFTER_2H_FROM_CHECKIN,
        MONTHLY_REPORT_READY
    }

}
