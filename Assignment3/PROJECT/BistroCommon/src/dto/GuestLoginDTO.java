package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer guest login details from the
 * client to the server.
 * <p>
 * This object contains the guest's contact information, which is used to
 * identify or create a temporary guest session in the system.
 * </p>
 */
public class GuestLoginDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private String phone;
	private String email;

	public GuestLoginDTO(String phone, String email) {
		this.phone = phone;
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}
}
