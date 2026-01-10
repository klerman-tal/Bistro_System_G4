package network;

import dto.RegisterSubscriberDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class RegisterSubscriberHandler implements RequestHandler {

    private final UserController userController;

    public RegisterSubscriberHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object dataObj = request.getData();
        if (!(dataObj instanceof RegisterSubscriberDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid register data", null));
            return;
        }

        Object userObj = client.getInfo("user");
        if (!(userObj instanceof Subscriber performedBy)) {
            client.sendToClient(new ResponseDTO(false, "Unauthorized", null));
            return;
        }

        Subscriber created =
                userController.registerSubscriber(
                        dto.getUsername(),
                        dto.getFirstName(),
                        dto.getLastName(),
                        dto.getPhone(),
                        dto.getEmail(),
                        dto.getRole(),
                        performedBy
                );

        if (created == null) {
            client.sendToClient(
                    new ResponseDTO(false, "Failed to create subscriber", null)
            );
            return;
        }

        client.sendToClient(
                new ResponseDTO(true, "Subscriber created successfully", created)
        );
    }
}
