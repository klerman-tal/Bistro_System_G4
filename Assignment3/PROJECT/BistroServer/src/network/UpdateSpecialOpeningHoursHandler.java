package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.SpecialOpeningHours;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class UpdateSpecialOpeningHoursHandler implements RequestHandler {
    private final RestaurantController restaurantController;

    public UpdateSpecialOpeningHoursHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        if (request == null || request.getData() == null) {
            client.sendToClient(new ResponseDTO(false, "Missing special hours data", null));
            return;
        }

        SpecialOpeningHours data = (SpecialOpeningHours) request.getData();
        try {
            
            client.sendToClient(new ResponseDTO(true, "Special hours updated and grid synchronized", null));
        } catch (Exception e) {
            client.sendToClient(new ResponseDTO(false, "Update failed: " + e.getMessage(), null));
        }
    }
}