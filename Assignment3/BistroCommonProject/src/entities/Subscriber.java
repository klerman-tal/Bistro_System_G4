package entities;

import entities.Enums.UserRole;

/**
 * Represents a subscriber entity.
 * This class maps directly to the SUBSCRIBERS table in the database.ש
 * It contains only identification and personal information.
 */
public class Subscriber {

    // ====== Fields ======

    /** Primary key of the subscriber (also serves as subscriber number) */
    private int subscriberId;

    /** Login username */
    private String username;

    /** Encrypted password */
    private String password;

    /** Subscriber first name */
    private String firstName;

    /** Subscriber last name */
    private String lastName;

    /** Subscriber phone number */
    private String phone;

    /** Subscriber email address */
    private String email;

    /** Role of the subscriber (SUBSCRIBER / RESTAURANT_REP / MANAGER) */
    private UserRole role;

    // ====== Constructors ======

    /**
     * Empty constructor required for DB mapping.
     */
    public Subscriber() {
    }

    /**
     * Full constructor for creating a complete Subscriber object.
     */
    public Subscriber(
            int subscriberId,
            String username,
            String password,
            String firstName,
            String lastName,
            String phone,
            String email,
            UserRole role
    ) {
        this.subscriberId = subscriberId;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.role = role;
    }

    // ====== Getters & Setters ======

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
