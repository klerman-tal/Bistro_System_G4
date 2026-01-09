package entities;

import entities.Enums.UserRole;

public class Subscriber extends User {

    private String username;
    private String firstName;
    private String lastName;

    public Subscriber() {
        super();
        setUserRole(UserRole.Subscriber);
    }

    public Subscriber(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        super(subscriberId, phone, email, UserRole.Subscriber);
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
