package network;

import dto.FindUserByIdDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving
 * user information by user ID.
 * <p>
 * This handler is restricted to restaurant staff roles
 * (RestaurantAgent or RestaurantManager) and allows them
 * to search for a user using a unique identifier.
 * </p>
 */
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

        // ✅ Use a safe method that returns User (Subscriber or Guest)
        // Assumes you added: userController.getUserById(int id, User performedBy)
        User found = userController.getUserById(id, performer);

        if (found == null) {
            client.sendToClient(new ResponseDTO(false, "User not found", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "User found", found));
    }
}
