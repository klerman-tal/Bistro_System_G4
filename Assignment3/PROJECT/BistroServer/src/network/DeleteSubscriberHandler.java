package network;

import java.io.IOException;

import dto.DeleteSubscriberDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class DeleteSubscriberHandler implements RequestHandler {

    private final UserController userController;

    public DeleteSubscriberHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
            // 🔒 שליפת המשתמש מה-session + בדיקה
            Object userObj = client.getInfo("user");

            if (!(userObj instanceof Subscriber performedBy)) {
                client.sendToClient(
                        new ResponseDTO(false, "Unauthorized", null)
                );
                return;
            }

            DeleteSubscriberDTO data =
                    (DeleteSubscriberDTO) request.getData();

            boolean deleted =
                    userController.deleteSubscriber(
                            data.getSubscriberId(),
                            performedBy
                    );

            client.sendToClient(
                    new ResponseDTO(
                            deleted,
                            deleted ? "Subscriber deleted successfully" : "Delete failed",
                            null
                    )
            );

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
