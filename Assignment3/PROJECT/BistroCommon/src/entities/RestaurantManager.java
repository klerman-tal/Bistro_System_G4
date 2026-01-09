package entities;

import entities.Enums.UserRole;

public class RestaurantManager extends RestaurantAgent {

    public RestaurantManager() {
        super();
        setUserRole(UserRole.RestaurantManager);
    }

    public RestaurantManager(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        super(subscriberId, username, firstName, lastName, phone, email);
        setUserRole(UserRole.RestaurantManager);
    }
}
