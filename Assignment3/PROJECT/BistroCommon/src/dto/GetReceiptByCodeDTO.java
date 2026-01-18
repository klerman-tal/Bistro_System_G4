package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request a receipt based on a reservation
 * confirmation code.
 * <p>
 * This object is typically sent from the client to the server in order to
 * retrieve the receipt associated with a specific reservation.
 * </p>
 */
public class GetReceiptByCodeDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private String confirmationCode;

	public GetReceiptByCodeDTO() {
	}

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
