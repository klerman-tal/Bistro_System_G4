package dto;

import java.io.Serializable;

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
