package dto;

import java.io.Serializable;

public class GetReceiptByCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String confirmationCode;

    // ✅ required for serialization / frameworks / safety
    public GetReceiptByCodeDTO() {
    }

    // ✅ your intended constructor
    public GetReceiptByCodeDTO(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
