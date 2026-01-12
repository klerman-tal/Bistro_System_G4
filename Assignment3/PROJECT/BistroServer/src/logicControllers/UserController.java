package logicControllers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;   // ✅ FIX: correct List import

import dbControllers.User_DB_Controller;
import entities.Enums;
import entities.Restaurant;
import entities.Subscriber;
import entities.User;

public class UserController {

    private User_DB_Controller userDB;

    public UserController(User_DB_Controller userDB) {
        this.userDB = userDB;
    }

    public Subscriber loginSubscriber(int subscriberId, String username) {

        // Validate input parameters
        if (subscriberId <= 0 || username == null || username.isBlank()) {
            return null;
        }

        // Delegate authentication check to DB layer
        Subscriber subscriber =
                userDB.loginSubscriber(subscriberId, username);

        // Return null if subscriber does not exist
        if (subscriber == null) {
            return null;
        }

        // Successful login
        return subscriber;
    }

    public User loginGuest(String phone, String email) {
        // שינוי: ולידציה שבודקת שלפחות אחד קיים
        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }

		try {
			int guestId = generateNextUserId();
			
			 // שליחה ל-DB (ה-DB יקבל null עבור השדה הריק)
	        User guest = userDB.loginGuest(guestId, phone, email);

	        if (guest == null) {
	            return null;
	        }

	        return guest;
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;
		}
    }
       

    public int registerSubscriber(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role,
            Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return -1;
        }

        // Authorization rules:
        // - RestaurantAgent can create Subscriber only
        // - RestaurantManager can create Subscriber or RestaurantAgent
        if (performedBy.getUserRole() == Enums.UserRole.RestaurantAgent &&
            role != Enums.UserRole.Subscriber) {
            return -1;
        }

        if (performedBy.getUserRole() == Enums.UserRole.Subscriber ||
            performedBy.getUserRole() == Enums.UserRole.RandomClient) {
            return -1;
        }

        // Validate input data
        if (username == null || username.isBlank() ||
            firstName == null || firstName.isBlank() ||
            lastName == null || lastName.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return -1;
        }

        // Generate global subscriber ID
		try {
			 int subscriberId = generateNextUserId();
			
			// Delegate persistence to DB layer
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

        
    }
    
    public int addRestaurantAgent(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return -1;
        }

        // Only RestaurantManager can add restaurant agents
        if (performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return -1;
        }

        // Validate input
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
        // Delegate creation to DB layer
       
    }


    public Subscriber getSubscriberById(int subscriberId, Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return null;
        }

        // Authorization rules:
        // Only RestaurantAgent or RestaurantManager can fetch a subscriber by ID
        if (performedBy.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return null;
        }

        // Validate input
        if (subscriberId <= 0) {
            return null;
        }

        // Delegate DB access
        return userDB.getSubscriberById(subscriberId);
    }

    public Subscriber findSubscriberByUsernameAndPhone(String username, String phone) {

        // Validate input parameters
        if (username == null || username.isBlank() ||
            phone == null || phone.isBlank()) {
            return null;
        }

        // Delegate lookup to DB layer
        return userDB.getSubscriberByUsernameAndPhone(username, phone);
    }

    public List<Subscriber> getAllSubscribers(Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return new ArrayList<>();
        }

        // Authorization rules:
        // Only RestaurantAgent or RestaurantManager can view all subscribers
        if (performedBy.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return new ArrayList<>();
        }

        // Delegate DB access
        return userDB.getAllSubscribers();
    }
    
    public boolean deleteGuestAfterPayment(int guestId) {

        // Validate input
        if (guestId <= 0) {
            return false;
        }

        // Delegate deletion to DB layer
        return userDB.deleteGuest(guestId);
    }
    
    public boolean deleteRestaurantAgent(int agentId, Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return false;
        }

        // Only RestaurantManager can delete restaurant agents
        if (performedBy.getUserRole() != Enums.UserRole.RestaurantManager) {
            return false;
        }

        // Validate input
        if (agentId <= 0) {
            return false;
        }

        // Delegate deletion to DB layer
        return userDB.deleteRestaurantAgent(agentId);
    }

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
    
    public boolean updateSubscriberDetails(
            int subscriberId,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        if (subscriberId <= 0) return false;

        return userDB.updateSubscriberDetails(
                subscriberId,
                firstName,
                lastName,
                phone,
                email
        );
    }

    public int generateNextUserId() throws SQLException {
        int max = userDB.getMaxUserIdFromGuestsAndSubscribers();
        return max + 1;
    }

    

}