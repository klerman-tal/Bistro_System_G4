package logicControllers;
//Test
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

        // Validate input parameters
        if (phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return null;
        }

        // Generate a global user ID
        int guestId = Restaurant.getInstance().generateNewUserId();

        // Delegate guest creation to DB layer
        User guest = userDB.loginGuest(guestId, phone, email);

        // Handle DB failure
        if (guest == null) {
            return null;
        }

        return guest;
    }

    public Subscriber registerSubscriber(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role,
            Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return null;
        }

        // Authorization rules:
        // - RestaurantAgent can create Subscriber only
        // - RestaurantManager can create Subscriber or RestaurantAgent
        if (performedBy.getRole() == Enums.UserRole.RestaurantAgent &&
            role != Enums.UserRole.Subscriber) {
            return null;
        }

        if (performedBy.getRole() == Enums.UserRole.Subscriber ||
            performedBy.getRole() == Enums.UserRole.RandomClient) {
            return null;
        }

        // Validate input data
        if (username == null || username.isBlank() ||
            firstName == null || firstName.isBlank() ||
            lastName == null || lastName.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return null;
        }

        // Generate global subscriber ID
        int subscriberId = Restaurant.getInstance().generateNewUserId();

        // Delegate persistence to DB layer
        return userDB.registerSubscriber(
                subscriberId,
                username,
                firstName,
                lastName,
                phone,
                email,
                role
        );
    }
    
    public Subscriber addRestaurantAgent(
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return null;
        }

        // Only RestaurantManager can add restaurant agents
        if (performedBy.getRole() != Enums.UserRole.RestaurantManager) {
            return null;
        }

        // Validate input
        if (username == null || username.isBlank() ||
            firstName == null || firstName.isBlank() ||
            lastName == null || lastName.isBlank() ||
            phone == null || phone.isBlank() ||
            email == null || email.isBlank()) {
            return null;
        }

        // Generate global ID
        int agentId = Restaurant.getInstance().generateNewUserId();

        // Delegate creation to DB layer
        return userDB.registerSubscriber(
                agentId,
                username,
                firstName,
                lastName,
                phone,
                email,
                Enums.UserRole.RestaurantAgent
        );
    }


    public Subscriber getSubscriberById(int subscriberId, Subscriber performedBy) {

        // Validate performing user
        if (performedBy == null) {
            return null;
        }

        // Authorization rules:
        // Only RestaurantAgent or RestaurantManager can fetch a subscriber by ID
        if (performedBy.getRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getRole() != Enums.UserRole.RestaurantManager) {
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
        if (performedBy.getRole() != Enums.UserRole.RestaurantAgent &&
            performedBy.getRole() != Enums.UserRole.RestaurantManager) {
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
        if (performedBy.getRole() != Enums.UserRole.RestaurantManager) {
            return false;
        }

        // Validate input
        if (agentId <= 0) {
            return false;
        }

        // Delegate deletion to DB layer
        return userDB.deleteRestaurantAgent(agentId);
    }

    
    

}
