package entities;

import java.io.Serializable;

/**
 * Base entity representing a system user.
 * <p>
 * This class serves as a superclass for all user types in the system,
 * such as subscribers and restaurant staff. It stores common user
 * attributes including contact information, role, and active reservation
 * reference.
 * </p>
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected Integer userId;
    protected String phoneNumber;
    protected String email;
    protected Enums.UserRole userRole;
    protected Reservation activeReservation;

    /**
     * Default constructor.
     * <p>
     * Initializes an empty user instance. Typically used for framework
     * initialization or database mapping.
     * </p>
     */
    public User() {}

    /**
     * Constructs a user with full identification and role information.
     */
    public User(Integer userId, String phoneNumber, String email, Enums.UserRole userRole) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.userRole = userRole;
    }

    /**
     * Constructs a user with basic contact information.
     * <p>
     * This constructor is commonly used for guest users.
     * </p>
     */
    public User(String phone, String email) {
        this.phoneNumber = phone;
        this.email = email;
    }

    // Getters & Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Enums.UserRole getUserRole() { return userRole; }
    public void setUserRole(Enums.UserRole userRole) { this.userRole = userRole; }
}
