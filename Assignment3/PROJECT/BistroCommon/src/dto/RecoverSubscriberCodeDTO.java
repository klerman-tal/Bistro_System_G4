package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to recover a subscriber's code
 * between the client and the server.
 * <p>
 * This object contains the username, phone number, and email of the subscriber.
 * </p>
 */
public class RecoverSubscriberCodeDTO implements Serializable {

    private final String username;
    private final String phone;
    private final String email;

    public RecoverSubscriberCodeDTO(String username, String phone, String email) {
        this.username = username;
        this.phone = phone;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}