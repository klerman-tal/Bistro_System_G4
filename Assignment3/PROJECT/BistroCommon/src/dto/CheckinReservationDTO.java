package dto;

import java.io.Serializable;

/**
 * Sent by client to request getting table for an existing reservation (check-in).
 */
public class CheckinReservationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String confirmationCode;

    public CheckinReservationDTO(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }
}
