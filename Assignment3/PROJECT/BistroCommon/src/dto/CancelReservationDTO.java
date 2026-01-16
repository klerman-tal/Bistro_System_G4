package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer reservation cancellation data
 * between the client and the server.
 * <p>
 * This object contains the reservation confirmation code and an optional
 * cancellation reason provided by the user.
 * </p>
 */
public class CancelReservationDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private String confirmationCode; 
    private String cancelReason;

   
    public CancelReservationDTO(String confirmationCode, String cancelReason) {
        this.confirmationCode = confirmationCode;
        this.cancelReason = cancelReason;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    
    public String getCancelReason() {
        return cancelReason;
    }
}
