package entities;

import entities.Enums.UserRole;

/**
 * Entity representing a restaurant manager user.
 * <p>
 * A restaurant manager is a specialized restaurant agent with higher-level
 * management permissions. This role is typically responsible for overseeing
 * restaurant operations, managing staff actions, and accessing managerial
 * system features.
 * </p>
 */
public class RestaurantManager extends RestaurantAgent {

	/**
	 * Default constructor.
	 * <p>
	 * Initializes a restaurant manager and assigns the
	 * {@link UserRole#RestaurantManager} role.
	 * </p>
	 */
	public RestaurantManager() {
		super();
		setUserRole(UserRole.RestaurantManager);
	}

	/**
	 * Constructs a restaurant manager with full subscriber details.
	 * <p>
	 * The user role is automatically set to {@link UserRole#RestaurantManager}.
	 * </p>
	 */
	public RestaurantManager(int subscriberId, String username, String firstName, String lastName, String phone,
			String email) {
		super(subscriberId, username, firstName, lastName, phone, email);
		setUserRole(UserRole.RestaurantManager);
	}
}
