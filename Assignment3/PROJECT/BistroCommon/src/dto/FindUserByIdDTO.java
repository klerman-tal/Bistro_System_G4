package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer user identification data between
 * the client and the server.
 * <p>
 * This object contains the unique ID of the user to be found.
 * </p>
 */
public class FindUserByIdDTO implements Serializable {
	private int userId;

	public FindUserByIdDTO(int userId) {
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}
}