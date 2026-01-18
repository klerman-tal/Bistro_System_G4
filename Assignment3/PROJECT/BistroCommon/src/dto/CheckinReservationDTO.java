package dto;

import java.io.Serializable;

/**
 * Sent by client to request getting table for an existing reservation
 * (check-in).
 * <p>
 * This object contains the reservation confirmation code required for checking
 * in to a reservation.
 * </p>
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