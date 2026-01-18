package dto;

import java.io.Serializable;
import entities.Enums.UserRole;

/**
 * Data Transfer Object (DTO) used to request joining the restaurant waiting
 * list.
 * <p>
 * This object is sent from the client to the server and contains information
 * about the number of guests, the user identifier, and the user's role in the
 * system.
 * </p>
 */
public class JoinWaitingDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private final int guests;
	private final int userId;
	private final UserRole role;

	public JoinWaitingDTO(int guests, int userId, UserRole role) {
		this.guests = guests;
		this.userId = userId;
		this.role = role;
	}

	public int getGuests() {
		return guests;
	}

	public int getUserId() {
		return userId;
	}

	public UserRole getRole() {
		return role;
	}
}
