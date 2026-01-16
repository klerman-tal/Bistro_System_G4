package network;

import dto.DeleteTableDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for deleting
 * a restaurant table.
 * <p>
 * This handler processes table deletion requests, delegates
 * the removal logic to the {@link RestaurantController},
 * and returns an appropriate response to the client.
 * </p>
 */
public class DeleteTableHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    /**
     * Constructs a handler with the required restaurant controller dependency.
     */
    public DeleteTableHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    /**
     * Handles a delete table request received from the client.
     * <p>
     * The method attempts to remove the specified table and sends
     * a success or failure response back to the client.
     * </p>
     */
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
