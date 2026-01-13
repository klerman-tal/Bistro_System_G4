package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.UpdateSubscriberDetailsDTO;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class UpdateSubscriberDetailsHandler implements RequestHandler {

    private final UserController userController;

    public UpdateSubscriberDetailsHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        UpdateSubscriberDetailsDTO dto =
                (UpdateSubscriberDetailsDTO) request.getData();

        boolean success =
                userController.updateSubscriberDetails(
                        dto.getSubscriberId(),
                        dto.getUsername(),
                        dto.getFirstName(),
                        dto.getLastName(),
                        dto.getPhone(),
                        dto.getEmail()
                );

        client.sendToClient(
                new ResponseDTO(
                        success,
                        success ? "Details updated successfully" : "Update failed",
                        null
                )
        );
    }
}
