package dto;

import java.io.Serializable;

public class UpdateSubscriberDetailsDTO implements Serializable {

    private int subscriberId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    public UpdateSubscriberDetailsDTO(
            int subscriberId,
            String firstName,
            String lastName,
            String phone,
            String email) {

        this.subscriberId = subscriberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }

    public int getSubscriberId() { return subscriberId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
