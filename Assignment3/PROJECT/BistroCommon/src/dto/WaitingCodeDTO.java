package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer waiting code information
 * between the client and the server.
 * <p>
 * This object contains the confirmation code for a waiting list entry.
 * </p>
 */
public class WaitingCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String confirmationCode;

    public WaitingCodeDTO(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }
}