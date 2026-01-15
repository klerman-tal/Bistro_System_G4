package network;

import dto.GuestLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class CreateGuestByPhoneHandler implements RequestHandler {

    private final UserController userController;

    public CreateGuestByPhoneHandler(UserController userController) {
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
        if (!(dataObj instanceof GuestLoginDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid data", null));
            return;
        }

        String phone = dto.getPhone();

        if (phone == null || phone.isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Phone is required", null));
            return;
        }

        // We intentionally ignore email here (acting guest by phone)
        User guest = userController.loginGuest(phone.trim(), null);

        if (guest == null) {
            client.sendToClient(new ResponseDTO(false, "Failed to create guest", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "Guest created", guest));
    }
}
