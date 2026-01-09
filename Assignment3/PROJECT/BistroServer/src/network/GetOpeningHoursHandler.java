package network;

import java.util.ArrayList;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.OpeningHouers;
import entities.Restaurant;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class GetOpeningHoursHandler implements RequestHandler {
    private final RestaurantController restaurantController;

    public GetOpeningHoursHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        restaurantController.loadOpeningHoursFromDb(); // המתודה ששלחת
        ArrayList<OpeningHouers> hours = Restaurant.getInstance().getOpeningHours();
        client.sendToClient(new ResponseDTO(true, "Opening hours loaded", hours));
    }
}