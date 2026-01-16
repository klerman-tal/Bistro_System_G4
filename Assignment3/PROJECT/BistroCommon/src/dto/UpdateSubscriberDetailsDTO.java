package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to update subscriber details
 * between the client and the server.
 * <p>
 * This object contains details such as the subscriber ID, username, first name,
 * last name, phone number, and email.
 * </p>
 */
public class UpdateSubscriberDetailsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int subscriberId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;

    public UpdateSubscriberDetailsDTO(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        this.subscriberId = subscriberId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }

    public int getSubscriberId() { return subscriberId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}