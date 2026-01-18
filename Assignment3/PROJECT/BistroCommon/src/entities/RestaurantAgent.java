package entities;

import entities.Enums.UserRole;

/**
 * Entity representing a restaurant agent user.
 * <p>
 * A restaurant agent is a specialized type of subscriber with management
 * permissions in the system. This role is typically assigned to staff members
 * who manage reservations and tables.
 * </p>
 */
public class RestaurantAgent extends Subscriber {

	/**
	 * Default constructor.
	 * <p>
	 * Initializes a restaurant agent and assigns the
	 * {@link UserRole#RestaurantAgent} role.
	 * </p>
	 */
	public RestaurantAgent() {
		super();
		setUserRole(UserRole.RestaurantAgent);
	}

	/**
	 * Constructs a restaurant agent with full subscriber details.
	 * <p>
	 * The user role is automatically set to {@link UserRole#RestaurantAgent}.
	 * </p>
	 */
	public RestaurantAgent(int subscriberId, String username, String firstName, String lastName, String phone,
			String email) {
		super(subscriberId, username, firstName, lastName, phone, email);
		setUserRole(UserRole.RestaurantAgent);
	}
}
