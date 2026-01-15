package dto;

import java.io.Serializable;

/**
 * DTO for requesting the entire waiting list from the server.
 * Used by restaurant managers to view all active waiting entries.
 */
public class GetWaitingListDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // אין צורך בשדות נוספים כי אנחנו שולפים את כל הרשימה, 
    // בדומה ל-GetAllReservationsDTO שלך.
}