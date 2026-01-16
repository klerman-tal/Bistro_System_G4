package network;

import dto.GuestLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for creating
 * a guest user based on a phone number.
 * <p>
 * This handler is restricted to restaurant staff roles
 * (RestaurantAgent or RestaurantManager) and allows them
 * to create or log in a guest user using a phone number.
 * </p>
 */
public class CreateGuestByPhoneHandler implements RequestHandler {

    private final UserController userController;

    /**
     * Constructs a handler with the required user controller dependency.
     */
    public CreateGuestByPhoneHandler(UserController userController) {
        this.userController = userController;
    }

    /**
     * Handles a create-guest-by-phone request received from the client.
     * <p>
     * The method verifies that the requesting user is authenticated and
     * authorized, validates the input data, and delegates the guest
     * creation logic to the {@link UserController}.
     * </p>
     */
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
        if (!(dataObj instanceof GuestLoginDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid data", null));
            return;
        }

        String phone = dto.getPhone();

        if (phone == null || phone.isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Phone is required", null));
            return;
        }

        User guest = userController.loginGuest(phone.trim(), null);

        if (guest == null) {
            client.sendToClient(new ResponseDTO(false, "Failed to create guest", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "Guest created", guest));
    }
}
