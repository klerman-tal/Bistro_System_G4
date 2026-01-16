package network;

import java.io.IOException;

import dto.DeleteSubscriberDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import entities.User;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for deleting a subscriber account.
 * <p>
 * This handler verifies that the request is performed by an authenticated
 * subscriber, delegates the deletion logic to the {@link UserController},
 * and returns a success or failure response to the client.
 * </p>
 */
public class DeleteSubscriberHandler implements RequestHandler {

    private final UserController userController;

    /**
     * Constructs a handler with the required user controller dependency.
     */
    public DeleteSubscriberHandler(UserController userController) {
        this.userController = userController;
    }

    /**
     * Handles a delete subscriber request received from the client.
     * <p>
     * The method checks the session user for authorization, performs the
     * deletion using the user controller, and sends a response indicating
     * whether the operation succeeded.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
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
