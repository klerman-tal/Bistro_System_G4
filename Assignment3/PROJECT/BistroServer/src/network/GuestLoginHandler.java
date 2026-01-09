package network;

import dto.GuestLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class GuestLoginHandler implements RequestHandler {
    private final UserController userController;

    public GuestLoginHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        GuestLoginDTO loginData = (GuestLoginDTO) request.getData();
        
        // קריאה ללוגיקה ב-UserController (כבר קיימת אצלך)
        User guest = userController.loginGuest(loginData.getPhone(), loginData.getEmail());

        if (guest != null) {
            client.sendToClient(new ResponseDTO(true, "Guest login successful", guest));
        } else {
            client.sendToClient(new ResponseDTO(false, "Failed to create guest session", null));
        }
    }
}