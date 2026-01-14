package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateOpeningHoursDTO;
import entities.OpeningHouers;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class UpdateOpeningHoursHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    public UpdateOpeningHoursHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        if (request == null || request.getData() == null) {
            client.sendToClient(new ResponseDTO(false, "Missing update data", null));
            return;
        }

        UpdateOpeningHoursDTO data = (UpdateOpeningHoursDTO) request.getData();

        OpeningHouers oh = new OpeningHouers();
        oh.setDayOfWeek(data.getDayOfWeek());

        // שימי לב: ריק/NULL => ה-DB controller מכניס NULL (CLOSED)
        oh.setOpenTime(data.getOpenTime());
        oh.setCloseTime(data.getCloseTime());

        try {
            restaurantController.updateOpeningHours(oh);
            client.sendToClient(new ResponseDTO(true, "Opening hours updated", null));
        } catch (Exception e) {
            client.sendToClient(new ResponseDTO(false, "Failed to update opening hours: " + e.getMessage(), null));
        }
    }
}