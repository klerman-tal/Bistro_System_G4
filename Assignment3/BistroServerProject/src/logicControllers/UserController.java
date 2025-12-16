package logicControllers;

import java.util.ArrayList;

import dbControllers.User_DB_Controller;
import entities.Enums;
import entities.Subscriber;
import entities.User;

public class UserController {

    private User_DB_Controller userDB;

    public UserController(User_DB_Controller userDB) {
        this.userDB = userDB;
    }

    /**
     * Handles user login.
     * 
     * @param data ArrayList containing username and password
     * @return User object if login succeeds, otherwise error string
     */
    public Object login(ArrayList<?> data) {

        String username = data.get(1).toString();
        String password = data.get(2).toString();

        User user = userDB.loginUser(username, password);

        if (user == null) {
            return "LOGIN_FAILED";
        }

        return user;
    }

    /**
     * Handles subscriber registration.
     * Only RandomClient is allowed to register.
     */
    public Object register(ArrayList<?> data, User currentUser) {

        // Permission check: only RandomClient can register
        if (currentUser == null ||
            currentUser.getUserRole() != Enums.UserRole.RandomClient) {
            return "REGISTER_NOT_ALLOWED";
        }

        String firstName = data.get(1).toString();
        String lastName  = data.get(2).toString();
        String email     = data.get(3).toString();
        String phone     = data.get(4).toString();

        try {
            return userDB.createSubscriber(firstName, lastName, email, phone);
        } catch (Exception e) {
            return "REGISTER_FAILED: " + e.getMessage();
        }
    }

    /**
     * Updates subscriber details after validating permissions.
     *
     * @param currentUser the logged-in user
     * @param updatedSubscriber subscriber object with updated data
     * @return updated Subscriber or error message
     */
    public Object updateSubscriberDetails(
            User currentUser,
            Subscriber updatedSubscriber
    ) {

        if (currentUser == null) {
            return "USER_NOT_LOGGED_IN";
        }

        if (currentUser.getUserRole() == Enums.UserRole.RandomClient) {
            return "ACCESS_DENIED_RANDOM_CLIENT";
        }

        try {
            Subscriber existingSubscriber =
                    userDB.getSubscriberByUserId(currentUser.getUserId());

            if (existingSubscriber == null) {
                return "USER_IS_NOT_SUBSCRIBER";
            } 

            boolean updated = userDB.updateSubscriberDetails(
                    existingSubscriber.getSubscriberNumber(),
                    updatedSubscriber.getUserName(),
                    updatedSubscriber.getPersonalDetails()
            );

            if (!updated) {
                return "UPDATE_FAILED";
            }

            // Return the updated subscriber (fresh from DB)
            return userDB.getSubscriberByUserId(currentUser.getUserId());

        } catch (Exception e) {
            return "FAILED_TO_UPDATE_SUBSCRIBER_DETAILS";
        }
    }
 /**
     * Returns reservation history for the current user.
     * RandomClient is NOT allowed to view reservation history.
     *
     * Subscriber / RestaurantAgent / RestaurantManager are allowed.
     */
    public Object viewReservationHistory(User currentUser) {

        // 1. Check if user is logged in
        if (currentUser == null) {
            return "USER_NOT_LOGGED_IN";
        }

        // 2. Check permissions
        if (currentUser.getUserRole() == Enums.UserRole.RandomClient) {
            return "ACCESS_DENIED_RANDOM_CLIENT";
        }

        try {
            // 3. Retrieve Subscriber entity linked to this user
            Subscriber subscriber =
                    userDB.getSubscriberByUserId(currentUser.getUserId());

            if (subscriber == null) {
                return "USER_IS_NOT_SUBSCRIBER";
            }

            // 4. Fetch reservation history for this subscriber
            return userDB.getReservationHistoryBySubscriber(
                    subscriber.getSubscriberNumber()
            );

        } catch (Exception e) {
            return "FAILED_TO_LOAD_RESERVATION_HISTORY";
        }
    }
    
    
    
}
