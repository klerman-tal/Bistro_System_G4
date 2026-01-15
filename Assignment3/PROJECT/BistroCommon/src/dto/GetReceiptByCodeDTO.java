package dto;

import java.io.Serializable;

public class GetReceiptByCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String confirmationCode;

    public GetReceiptByCodeDTO(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }
}
