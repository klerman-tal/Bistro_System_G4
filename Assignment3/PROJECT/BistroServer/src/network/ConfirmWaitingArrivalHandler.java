package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for confirming
 * a waiting list arrival.
 * <p>
 * This handler validates the waiting confirmation code,
 * delegates the arrival confirmation logic to the
 * {@link WaitingController}, and returns the updated
 * waiting entry to the client upon success.
 * </p>
 */
public class ConfirmWaitingArrivalHandler implements RequestHandler {

    private final WaitingController waitingController;

    /**
     * Constructs a handler with the required waiting controller dependency.
     */
    public ConfirmWaitingArrivalHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    /**
     * Handles a waiting arrival confirmation request received from the client.
     * <p>
     * The method validates the confirmation code, confirms the user's arrival
     * in the waiting list, and returns the updated waiting state if successful.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        WaitingCodeDTO dto = (WaitingCodeDTO) request.getData();
        if (dto == null || dto.getConfirmationCode() == null || dto.getConfirmationCode().isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Invalid confirmation code", null));
            return;
        }

        String code = dto.getConfirmationCode().trim();

        boolean ok = waitingController.confirmArrival(code);
        if (!ok) {
            client.sendToClient(new ResponseDTO(false, "Arrival confirm failed (expired / not ready / not found)", null));
            return;
        }

        Waiting w = waitingController.getWaitingByCode(code);

        client.sendToClient(new ResponseDTO(true, "Seated successfully", w));
    }
}
