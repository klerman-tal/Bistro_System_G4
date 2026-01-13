package dto;

import java.io.Serializable;

public class CancelReservationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String confirmationCode;
    private String cancelReason; // optional

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
