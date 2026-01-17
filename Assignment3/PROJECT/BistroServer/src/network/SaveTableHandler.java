package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SaveTableDTO;
import entities.Table;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for creating or updating
 * restaurant table definitions.
 * <p>
 * This handler receives table configuration data from the client,
 * constructs a {@link Table} entity, and delegates persistence logic
 * to the {@link RestaurantController}.
 * </p>
 * <p>
 * The same handler supports both ADD and UPDATE operations,
 * depending on whether the table number already exists.
 * </p>
 */
public class SaveTableHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    /**
     * Constructs a new SaveTableHandler with the required restaurant controller.
     *
     * @param restaurantController controller responsible for table management
     */
    public SaveTableHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    /**
     * Handles a request to save or update a restaurant table.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Extracts table data from {@link SaveTableDTO}</li>
     *   <li>Builds a {@link Table} entity from the DTO</li>
     *   <li>Delegates persistence logic to the {@link RestaurantController}</li>
     *   <li>Sends a success or failure response back to the client</li>
     * </ol>
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        try {
            SaveTableDTO dto = (SaveTableDTO) request.getData();

            // Build table entity from incoming DTO
            Table table = new Table();
            table.setTableNumber(dto.getTableNumber());
            table.setSeatsAmount(dto.getSeatsAmount());

            // Delegate save/update logic to controller
            restaurantController.saveOrUpdateTable(table);

            client.sendToClient(
                new ResponseDTO(true, "Table saved successfully", null)
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
