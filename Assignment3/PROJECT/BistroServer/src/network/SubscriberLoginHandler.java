package network;

import dto.SubscriberLoginDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.OnlineUsersRegistry;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class SubscriberLoginHandler implements RequestHandler {

    private final UserController userController;
    private final OnlineUsersRegistry onlineUsers;

    public SubscriberLoginHandler(UserController userController, OnlineUsersRegistry onlineUsers) {
        this.userController = userController;
        this.onlineUsers = onlineUsers;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        Object dataObj = request.getData();
        if (!(dataObj instanceof SubscriberLoginDTO loginData)) {
            send(client, new ResponseDTO(false, "Invalid login data", null));
            return;
        }

        Subscriber subscriber =
                userController.loginSubscriber(
                        loginData.getSubscriberId(),
                        loginData.getUsername()
                );

        if (subscriber == null) {
            send(client, new ResponseDTO(false, "Invalid ID or Username", null));
            return;
        }

        // Session on connection
        client.setInfo("user", subscriber);

        // ✅ Register online user for SMS popups
        if (onlineUsers != null) {
            onlineUsers.setOnline(subscriber.getUserId(), client);
        }

        send(client, new ResponseDTO(true, "Login successful", subscriber));
    }

    private void send(ConnectionToClient client, ResponseDTO res) {
        try {
            client.sendToClient(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
