package entities;

import entities.Enums.UserRole;

/**
 * Entity representing a registered subscriber user.
 * <p>
 * A subscriber is a type of user with an account in the system,
 * identified by a username and personal details. Subscribers can
 * create and manage reservations and may later be promoted to
 * staff roles such as restaurant agent or manager.
 * </p>
 */
public class Subscriber extends User {

    private String username;
    private String firstName;
    private String lastName;

    /**
     * Default constructor.
     * <p>
     * Initializes a subscriber with the {@link UserRole#Subscriber} role.
     * Typically used for framework initialization or database mapping.
     * </p>
     */
    public Subscriber() {
        super();
        setUserRole(UserRole.Subscriber);
    }

    /**
     * Constructs a subscriber with full personal and account details.
     * <p>
     * The user role is automatically set to {@link UserRole#Subscriber}.
     * </p>
     */
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
