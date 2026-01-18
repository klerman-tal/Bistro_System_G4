package dto;

import java.io.Serializable;
import entities.Enums;

/**
 * Data Transfer Object (DTO) used to transfer subscriber registration data
 * between the client and the server.
 * <p>
 * This object contains details such as the username, first name, last name,
 * phone number, email, and user role of the subscriber.
 * </p>
 */
public class RegisterSubscriberDTO implements Serializable {

	private final String username;
	private final String firstName;
	private final String lastName;
	private final String phone;
	private final String email;
	private final Enums.UserRole role;

	public RegisterSubscriberDTO(String username, String firstName, String lastName, String phone, String email,
			Enums.UserRole role) {

		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.email = email;
		this.role = role;
	}

	public String getUsername() {
		return username;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public Enums.UserRole getRole() {
		return role;
	}
}