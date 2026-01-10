package network;

import dto.DeleteTableDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class DeleteTableHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    public DeleteTableHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        try {
            DeleteTableDTO dto =
                (DeleteTableDTO) request.getData();

            boolean removed =
                restaurantController.removeTable(dto.getTableNumber());

            if (!removed) {
                client.sendToClient(
                    new ResponseDTO(false,
                        "Table cannot be deleted", null)
                );
                return;
            }

            client.sendToClient(
                new ResponseDTO(true, "Table deleted successfully", null)
            );

        } catch (Exception e) {
            try {
                client.sendToClient(
                    new ResponseDTO(false, e.getMessage(), null)
                );
            } catch (Exception ignored) {}
        }
    }
}
