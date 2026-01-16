package network;

import dto.FindUserByIdDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.Subscriber;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class FindUserByIdHandler implements RequestHandler {

    private final UserController userController;

    public FindUserByIdHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object performerObj = client.getInfo("user");
        if (!(performerObj instanceof User performer)) {
            client.sendToClient(new ResponseDTO(false, "Not logged in", null));
            return;
        }

        // Only RestaurantAgent / RestaurantManager
        if (performer.getUserRole() != Enums.UserRole.RestaurantAgent &&
            performer.getUserRole() != Enums.UserRole.RestaurantManager) {
            client.sendToClient(new ResponseDTO(false, "Permission denied", null));
            return;
        }

        Object dataObj = request.getData();
        if (!(dataObj instanceof FindUserByIdDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid data", null));
            return;
        }

        int id = dto.getUserId();
        if (id <= 0) {
            client.sendToClient(new ResponseDTO(false, "Invalid ID", null));
            return;
        }

        // Reuse existing logic: currently you can fetch subscribers by id
        Subscriber s = userController.getSubscriberById(id, (Subscriber) performerObj /* performer is a User but must be Subscriber subtype */);

        if (s == null) {
            client.sendToClient(new ResponseDTO(false, "User not found", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "User found", s));
    }
}
