package entities;

import entities.Enums.UserRole;

public class RestaurantAgent extends Subscriber {

    public RestaurantAgent() {
        super();
        setUserRole(UserRole.RestaurantAgent);
    }

    public RestaurantAgent(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
        super(subscriberId, username, firstName, lastName, phone, email);
        setUserRole(UserRole.RestaurantAgent);
    }
}
