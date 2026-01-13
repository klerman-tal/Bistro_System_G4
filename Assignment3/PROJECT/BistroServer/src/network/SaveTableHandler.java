package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SaveTableDTO;
import entities.Table;
import logicControllers.RestaurantController;
import ocsf.server.ConnectionToClient;

public class SaveTableHandler implements RequestHandler {

    private final RestaurantController restaurantController;

    public SaveTableHandler(RestaurantController restaurantController) {
        this.restaurantController = restaurantController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        try {
            SaveTableDTO dto = (SaveTableDTO) request.getData();

            Table table = new Table();
            table.setTableNumber(dto.getTableNumber());
            table.setSeatsAmount(dto.getSeatsAmount());


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
