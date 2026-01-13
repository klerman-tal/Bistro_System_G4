package network;

import dto.RecoverSubscriberCodeDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class RecoverSubscriberCodeHandler implements RequestHandler {

    private final UserController userController;

    public RecoverSubscriberCodeHandler(UserController userController) {
        this.userController = userController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        RecoverSubscriberCodeDTO dto =
                (RecoverSubscriberCodeDTO) request.getData();

        Subscriber subscriber =
                userController.recoverSubscriberCode(
                        dto.getUsername(),
                        dto.getPhone(),
                        dto.getEmail()
                );

        if (subscriber == null) {
            client.sendToClient(
                    new ResponseDTO(false, "Subscriber not found", null)
            );
            return;
        }

        client.sendToClient(
                new ResponseDTO(true,
                        "Subscriber code recovered",
                        subscriber.getUserId())
        );
    }
}
