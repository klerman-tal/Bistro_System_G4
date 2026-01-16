package network;

import dto.RecoverSubscriberCodeDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Subscriber;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for recovering
 * a subscriber's identification code.
 * <p>
 * This handler processes recovery requests by validating
 * subscriber credentials (username, phone, and email) and
 * delegating the recovery logic to the {@link UserController}.
 * If a matching subscriber is found, the subscriber ID is
 * returned to the client.
 * </p>
 */
public class RecoverSubscriberCodeHandler implements RequestHandler {

    private final UserController userController;

    /**
     * Constructs a handler with the required user controller dependency.
     */
    public RecoverSubscriberCodeHandler(UserController userController) {
        this.userController = userController;
    }

    /**
     * Handles a subscriber code recovery request.
     * <p>
     * The method extracts identifying details from the request,
     * attempts to locate the matching subscriber, and returns
     * the recovered subscriber identifier if successful.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        /**
         * Step 1: Extract recovery details from the request.
         */
        RecoverSubscriberCodeDTO dto =
                (RecoverSubscriberCodeDTO) request.getData();

        /**
         * Step 2: Attempt to recover the subscriber using
         * identifying credentials (username, phone, email).
         */
        Subscriber subscriber =
                userController.recoverSubscriberCode(
                        dto.getUsername(),
                        dto.getPhone(),
                        dto.getEmail()
                );

        /**
         * Step 3: Handle recovery failure (subscriber not found).
         */
        if (subscriber == null) {
            client.sendToClient(
                    new ResponseDTO(false, "Subscriber not found", null)
            );
            return;
        }

        /**
         * Step 4: Send recovered subscriber identifier to the client.
         */
        client.sendToClient(
                new ResponseDTO(true,
                        "Subscriber code recovered",
                        subscriber.getUserId())
        );
    }
}
