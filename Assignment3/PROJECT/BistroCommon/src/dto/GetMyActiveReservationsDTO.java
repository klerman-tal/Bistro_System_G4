package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request all active reservations associated
 * with a specific user.
 * <p>
 * This object is sent from the client to the server and contains the user
 * identifier required to retrieve the user's active reservations.
 * </p>
 */
public class GetMyActiveReservationsDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private final int userId;

	public GetMyActiveReservationsDTO(int userId) {
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}
}
