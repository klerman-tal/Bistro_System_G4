package logicControllers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dbControllers.User_DB_Controller;
import entities.Enums;
import entities.Subscriber;
import entities.User;

/**
 * Handles user-related business logic for authentication and user management.
 * <p>
 * This controller provides a logic layer above {@link User_DB_Controller} and is responsible for:
 * <ul>
 *   <li>Logging in subscribers and guests</li>
 *   <li>Registering subscribers and restaurant agents (with role-based authorization rules)</li>
 *   <li>Fetching and searching subscriber records</li>
 *   <li>Updating and deleting user records via the DB layer</li>
 *   <li>Generating new user identifiers based on the current maximum in persistence</li>
 * </ul>
 * </p>
 */
public class UserController {

    private User_DB_Controller userDB;

    /**
     * Constructs a UserController with the given database controller.
     *
     * @param userDB database controller used for user and subscriber persistence
     */
    public UserController(User_DB_Controller userDB) {
        this.userDB = userDB;
    }

    /**
     * Authenticates a subscriber by subscriber ID and username.
     *
     * @param subscriberId subscriber identifier
     * @param username     subscriber username
     * @return a {@link Subscriber} on successful login, or {@code null} if validation fails or user not found
     */
    public Subscriber loginSubscriber(int subscriberId, String username) {

        if (subscriberId <= 0 || username == null || username.isBlank()) {
            return null;
        }

        Subscriber subscriber =
                userDB.loginSubscriber(subscriberId, username);

        if (subscriber == null) {
            return null;
        }

        return subscriber;
    }

    /**
     * Logs in or creates a guest user using phone and/or email.
     * <p>
     * At least one of the contact fields (phone, email) must be provided.
     * A new guest ID is generated using {@link #generateNextUserId()}.
     * </p>
     *
     * @param phone guest phone (may be blank if email is provided)
     * @param email guest email (may be blank if phone is provided)
     * @return a {@link User} representing the guest on success, or {@code null} on validation failure or DB error
     */
    public User loginGuest(String phone, String email) {
        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }

		try {
			int guestId = generateNextUserId();
			
	        User guest = userDB.loginGuest(guestId, phone, email);

	        if (guest == null) {
	            return null;
	        }

	        return guest;
		
		} catch (Exception e) {
			e.printStackTrace();
			
			return null;
		}
    }
       

    /**
     * Registers a new subscriber or restaurant agent, subject to role-based authorization rules.
     * <p>
     * Authorization rules implemented:
     * <ul>
     *   <li>RestaurantAgent can create {@code Subscriber} only</li>
     *   <li>RestaurantManager can create {@code Subscriber} or {@code RestaurantAgent}</li>
     *   <li>Subscriber and RandomClient cannot register users</li>
     * </ul>
     * </p>
     * <p>
     * A new global user ID is generated using {@link #generateNextUserId()}.
     * </p>
     *
     * @param username     new user's username
     * @param firstName    new user's first name
     * @param lastName     new user's last name
     * @param phone        new user's phone
     * @param email        new user's email
     * @param role         role to assign to the new user
     * @param performedBy  the authenticated subscriber performing the action
     * @return the new subscriber/agent ID on success, or {@code -1} on validation/authorization/DB failure
     */
    public int registerSubscriber(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role,
            Subscriber performedBy) {

        if (performedBy == null) {
            return -1;
        }

        if (performedBy.getUserRole() == Enums.UserRole.RestaurantAgent &&
            role != Enums.UserRole.Subscriber) {
            return -1;
        }

        if (performedBy.getUserRole() == Enums.UserRole.Subscriber ||
            performedBy.getUserRole() == Enums.UserRole.RandomClient) {
            return -1;
        }

        if (username == null || username.isBlank() ||
            firstName == null || firstName.isBlank() ||
            lastName == null || lastName.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return -1;
        }

		try {
			 int subscriberId = generateNextUserId();
			
	        userDB.registerSubscriber(
	                subscriberId,
	                username,
	                firstName,
	                lastName,
	                phone,
	                email,
	                role
	        );
	        
	        return subscriberId;
	        
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
    }
    
    /**
     * Creates a new restaurant agent user.
     * <p>
     * Only a {@code RestaurantManager} is authorized to perform this action.
     * A new global user ID is generated using {@link #generateNextUserId()}.
     * </p>
     *
     * @param username    agent username
     * @param firstName   agent first name
     * @param lastName    agent last name
     * @param phone       agent phone
     * @param email       agent email
     * @param performedBy the authenticated subscriber performing the action
     * @return the new agent ID on success, or {@code -1} on validation/authorization/DB failure
     */
    public int addRestaurantAgent(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Subscriber performedBy) {

        if (performedBy == null) {
            return -1;
        }

        if (performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return -1;
        }

        if (username == null || username.isBlank() ||
            firstName == null || firstName.isBlank() ||
            lastName == null || lastName.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return -1;
        }

		try {
			int agentId = generateNextUserId();
			
			 userDB.registerSubscriber(
		                agentId,
		                username,
		                firstName,
		                lastName,
		                phone,
		                email,
		                Enums.UserRole.RestaurantAgent
		        );
			 
			 return agentId;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
    }
    
    /**
     * Authenticates a subscriber using only a barcode-derived subscriber ID.
     *
     * @param subscriberId subscriber identifier extracted from the barcode
     * @return a {@link Subscriber} if found, or {@code null} if the ID is invalid or not found
     */
    public Subscriber loginByBarcode(int subscriberId) {
        if (subscriberId <= 0) {
            return null;
        }

        Subscriber sub = userDB.getSubscriberById(subscriberId);
        
        if (sub != null) {
            return sub;
        }
        
        return null;
    }


    /**
     * Retrieves a subscriber by ID with role-based authorization checks.
     *
     * @param subscriberId subscriber identifier
     * @param performedBy  the authenticated subscriber performing the action
     * @return the matching {@link Subscriber}, or {@code null} if validation/authorization fails or not found
     */
    public Subscriber getSubscriberById(int subscriberId, Subscriber performedBy) {

        if (performedBy == null) {
            return null;
        }

        if (performedBy.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return null;
        }

        if (subscriberId <= 0) {
            return null;
        }

        return userDB.getSubscriberById(subscriberId);
    }

    /**
     * Finds a subscriber by username and phone number.
     *
     * @param username subscriber username
     * @param phone    subscriber phone number
     * @return the matching {@link Subscriber}, or {@code null} if input is invalid or no match exists
     */
    public Subscriber findSubscriberByUsernameAndPhone(String username, String phone) {

        if (username == null || username.isBlank() ||
            phone == null || phone.isBlank()) {
            return null;
        }

        return userDB.getSubscriberByUsernameAndPhone(username, phone);
    }

    /**
     * Retrieves all subscribers, subject to role-based authorization.
     *
     * @param performedBy the authenticated subscriber performing the action
     * @return list of subscribers (empty list if unauthorized or none exist)
     */
    public List<Subscriber> getAllSubscribers(Subscriber performedBy) {

        if (performedBy == null) {
            return new ArrayList<>();
        }

        if (performedBy.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return new ArrayList<>();
        }

        return userDB.getAllSubscribers();
    }
    
    /**
     * Deletes a guest user record after payment is completed.
     *
     * @param guestId guest identifier
     * @return {@code true} if deletion succeeded, {@code false} otherwise
     */
    public boolean deleteGuestAfterPayment(int guestId) {

        if (guestId <= 0) {
            return false;
        }

        return userDB.deleteGuest(guestId);
    }
    
    /**
     * Deletes a subscriber record.
     *
     * @param subscriberId subscriber identifier
     * @param performedBy  the authenticated subscriber performing the action
     * @return {@code true} if deletion succeeded, {@code false} otherwise
     */
    public boolean deleteSubscriber(int subscriberId, Subscriber performedBy) {

        if (subscriberId <= 0) {
            return false;
        }

        return userDB.deleteSubscriber(subscriberId);
    }

    /**
     * Attempts to recover a subscriber record using username, phone, and email.
     *
     * @param username subscriber username
     * @param phone    subscriber phone number
     * @param email    subscriber email address
     * @return the matching {@link Subscriber}, or {@code null} if input is invalid or no match exists
     */
    public Subscriber recoverSubscriberCode(
            String username,
            String phone,
            String email) {

        if (username == null || username.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return null;
        }

        Subscriber subscriber =
                userDB.getSubscriberByUsernamePhoneEmail(
                        username, phone, email);

        return subscriber;
    }
    
    /**
     * Updates subscriber profile details in persistence.
     *
     * @param subscriberId subscriber identifier
     * @param username     new username
     * @param firstName    new first name
     * @param lastName     new last name
     * @param phone        new phone
     * @param email        new email
     * @return {@code true} if update succeeded, {@code false} otherwise
     * @throws SQLException if the database operation fails
     */
    public boolean updateSubscriberDetails(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) throws SQLException {
        if (subscriberId <= 0) return false;

        return userDB.updateSubscriberDetails(
                subscriberId,
                username,
                firstName,
                lastName,
                phone,
                email
        );
    }

    /**
     * Generates the next available user ID across guests and subscribers.
     *
     * @return next user identifier
     * @throws SQLException if reading the maximum user ID fails
     */
    public int generateNextUserId() throws SQLException {
        int max = userDB.getMaxUserIdFromGuestsAndSubscribers();
        return max + 1;
    }
    
    public User getUserById(int id, User performedBy) {

        if (performedBy == null) return null;

        if (performedBy.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return null;
        }

        if (id <= 0) return null;

        Subscriber s = userDB.getSubscriberById(id);
        if (s != null) return s;

        return userDB.getGuestById(id);
    }

}
