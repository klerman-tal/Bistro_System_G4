package network;

import dto.GuestLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.User;
import logicControllers.OnlineUsersRegistry;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for guest login operations.
 * <p>
 * This handler processes guest login requests, creates or retrieves
 * a guest user session based on phone and email, stores the user in
 * the connection session, and registers the guest as an online user
 * when applicable.
 * </p>
 */
public class GuestLoginHandler implements RequestHandler {

    private final UserController userController;
    private final OnlineUsersRegistry onlineUsers;

    /**
     * Constructs a handler with the required user controller
     * and online users registry dependencies.
     */
    public GuestLoginHandler(UserController userController, OnlineUsersRegistry onlineUsers) {
        this.userController = userController;
        this.onlineUsers = onlineUsers;
    }

    /**
     * Handles a guest login request received from the client.
     * <p>
     * The method validates the login data, delegates the login logic
     * to the user controller, stores the guest user in the session,
     * optionally registers the guest as an online user, and sends
     * the login result back to the client.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object dataObj = request.getData();
        if (!(dataObj instanceof GuestLoginDTO loginData)) {
            client.sendToClient(new ResponseDTO(false, "Invalid login data", null));
            return;
        }

        User guest = userController.loginGuest(loginData.getPhone(), loginData.getEmail());

        if (guest != null) {

            client.setInfo("user", guest);

            if (onlineUsers != null) {
                onlineUsers.setOnline(guest.getUserId(), client);
            }

            client.sendToClient(new ResponseDTO(true, "Guest login successful", guest));
        } else {
            client.sendToClient(new ResponseDTO(false, "Failed to create guest session", null));
        }
    }
}
