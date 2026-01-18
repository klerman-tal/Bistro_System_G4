package dto;

import java.io.Serializable;

import entities.Enums.UserRole;

/**
 * Data Transfer Object (DTO) used to transfer current diner information between
 * the client and the server.
 * <p>
 * This object contains details about the diner, including the creator's ID,
 * role, and the table number assigned.
 * </p>
 */
public class CurrentDinerDTO implements Serializable {
	private int createdBy;
	private String createdByRole;
	private Integer tableNumber;

	public CurrentDinerDTO(int createdBy, String createdByRole, Integer tableNumber) {
		this.createdBy = createdBy;
		this.createdByRole = createdByRole;
		this.tableNumber = tableNumber;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public String getCreatedByRole() {
		return createdByRole;
	}

	public Integer getTableNumber() {
		return tableNumber;
	}
}