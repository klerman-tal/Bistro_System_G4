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

    /* =========================
       LOGIN
       ========================= */

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

    /* =========================
       REGISTER (Guest → Subscriber)
       ========================= */

    /**
     * Handles registration of a RandomClient (guest).
     * The guest becomes a permanent Subscriber.
     *
     * Flow:
     * 1. Create User (with role Subscriber)
     * 2. Create Subscriber linked to that User
     *
     * @param data registration data
     * @return Subscriber object or error message
     */
    public Object register(ArrayList<?> data) {

        try {
            String username  = data.get(1).toString();
            String password  = data.get(2).toString();
            String firstName = data.get(3).toString();
            String lastName  = data.get(4).toString();
            String email     = data.get(5).toString();
            String phone     = data.get(6).toString();

            // Step 1: Create User (permanent account)
            User newUser = userDB.createUser(
                    username,
                    password,
                    email,
                    phone
            );

            // Step 2: Create Subscriber (activity entity)
            Subscriber subscriber = userDB.createSubscriber(
                    newUser.getUserId(),
                    firstName,
                    lastName
            );

            return subscriber;

        } catch (Exception e) {
            return "REGISTER_FAILED: " + e.getMessage();
        }
    }

    /* =========================
       UPDATE SUBSCRIBER DETAILS
       ========================= */

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
                    updatedSubscriber.getUserName()
            );


            if (!updated) {
                return "UPDATE_FAILED";
            }

            // Return the updated subscriber from DB
            return userDB.getSubscriberByUserId(currentUser.getUserId());

        } catch (Exception e) {
            return "FAILED_TO_UPDATE_SUBSCRIBER_DETAILS";
        }
    }

    /* =========================
       VIEW RESERVATION HISTORY
       ========================= */

    /**
     * Returns reservation history for the current user.
     * RandomClient is NOT allowed to view reservation history.
     *
     * Subscriber / RestaurantAgent / RestaurantManager are allowed.
     */
    public Object viewReservationHistory(User currentUser) {

        if (currentUser == null) {
            return "USER_NOT_LOGGED_IN";
        }

        if (currentUser.getUserRole() == Enums.UserRole.RandomClient) {
            return "ACCESS_DENIED_RANDOM_CLIENT";
        }

        try {
            Subscriber subscriber =
                    userDB.getSubscriberByUserId(currentUser.getUserId());

            if (subscriber == null) {
                return "USER_IS_NOT_SUBSCRIBER";
            }

            return userDB.getReservationHistoryBySubscriber(
                    subscriber.getSubscriberNumber()
            );

        } catch (Exception e) {
            return "FAILED_TO_LOAD_RESERVATION_HISTORY";
        }
    }
    
    /* =========================
    UPDATE USER CONTACT DETAILS
    ========================= */

 /**
  * Updates contact details (phone and email) of the logged-in user.
  * Only Subscribers and restaurant staff are allowed to perform this action.
  * RandomClient users are NOT allowed.
  */
 public Object updateUserContactDetails(
         User currentUser,
         String phone,
         String email
 ) {

     if (currentUser == null) {
         return "USER_NOT_LOGGED_IN";
     }

     if (currentUser.getUserRole() == Enums.UserRole.RandomClient) {
         return "ACCESS_DENIED_RANDOM_CLIENT";
     }

     try {
         boolean updated = userDB.updateUserContactDetails(
                 currentUser.getUserId(),
                 phone,
                 email
         );

         if (!updated) {
             return "UPDATE_FAILED";
         }

         return "UPDATE_SUCCESS";

     } catch (Exception e) {
         return "FAILED_TO_UPDATE_CONTACT_DETAILS";
     }
 }

}
