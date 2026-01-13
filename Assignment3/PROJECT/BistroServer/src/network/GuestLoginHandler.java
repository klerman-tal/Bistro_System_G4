package network;

import dto.GuestLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.User;
import logicControllers.OnlineUsersRegistry;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class GuestLoginHandler implements RequestHandler {

    private final UserController userController;
    private final OnlineUsersRegistry onlineUsers;

    public GuestLoginHandler(UserController userController, OnlineUsersRegistry onlineUsers) {
        this.userController = userController;
        this.onlineUsers = onlineUsers;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object dataObj = request.getData();
        if (!(dataObj instanceof GuestLoginDTO loginData)) {
            client.sendToClient(new ResponseDTO(false, "Invalid login data", null));
            return;
        }

        // Call logic
        User guest = userController.loginGuest(loginData.getPhone(), loginData.getEmail());

        if (guest != null) {

            // Keep session on this connection (optional but useful like subscriber)
            client.setInfo("user", guest);

            // ✅ Register online user for SMS popups
            if (onlineUsers != null) {
                onlineUsers.setOnline(guest.getUserId(), client);
            }

            client.sendToClient(new ResponseDTO(true, "Guest login successful", guest));
        } else {
            client.sendToClient(new ResponseDTO(false, "Failed to create guest session", null));
        }
    }
}
