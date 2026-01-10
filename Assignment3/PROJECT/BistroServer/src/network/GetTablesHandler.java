package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Table;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

import java.util.List;

public class GetTablesHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    public GetTablesHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        try {
            // 🔴 🔴 🔴 החלק שהיה חסר
            restaurantController.loadTablesFromDb();

            List<Table> tables = restaurantController.getAllTables();

            client.sendToClient(
                new ResponseDTO(true, "Tables loaded", tables)
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
