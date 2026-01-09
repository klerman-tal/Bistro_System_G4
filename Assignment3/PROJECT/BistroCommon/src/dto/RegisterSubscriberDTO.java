package dto;

import java.io.Serializable;

public class RegisterSubscriberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;

    // מי ביצע את הפעולה (Agent/Manager)
    private final int performedById;

    public RegisterSubscriberDTO(String username, String firstName, String lastName,
                                 String phone, String email, int performedById) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.performedById = performedById;
    }

    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getPerformedById() { return performedById; }
}
