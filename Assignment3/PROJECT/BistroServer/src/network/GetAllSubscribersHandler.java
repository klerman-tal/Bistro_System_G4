package network;

import java.io.IOException;
import java.util.List;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

public class GetAllSubscribersHandler implements RequestHandler {

    private final UserController userController;

    public GetAllSubscribersHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
            // המשתמש המחובר – מה-session
            User performedBy = (User) client.getInfo("user");

            if (!(performedBy instanceof Subscriber)) {
                client.sendToClient(
                    new ResponseDTO(false, "Unauthorized", null)
                );
                return;
            }

            List<Subscriber> subscribers =
                    userController.getAllSubscribers((Subscriber) performedBy);

            ResponseDTO response =
                    new ResponseDTO(true, "Subscribers loaded", subscribers);

            client.sendToClient(response);

        } catch (IOException e) {
            e.printStackTrace();
            try {
                client.sendToClient(
                    new ResponseDTO(false, "Server error", null)
                );
            } catch (IOException ignore) {}
        }
    }
}
