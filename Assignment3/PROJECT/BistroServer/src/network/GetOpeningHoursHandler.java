package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.OpeningHouers;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

import java.util.ArrayList;

public class GetOpeningHoursHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    public GetOpeningHoursHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        try {
            ArrayList<OpeningHouers> openingHours =
                    restaurantController.getOpeningHours();

            client.sendToClient(
                    new ResponseDTO(true, "Opening hours loaded", openingHours)
            );

        } catch (Exception e) {
            e.printStackTrace();
            client.sendToClient(
                    new ResponseDTO(false, "Failed to load opening hours", null)
            );
        }
    }
}
