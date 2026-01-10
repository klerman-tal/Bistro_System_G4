package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

public class ConfirmWaitingArrivalHandler implements RequestHandler {

    private final WaitingController waitingController;

    public ConfirmWaitingArrivalHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        WaitingCodeDTO dto = (WaitingCodeDTO) request.getData();
        if (dto == null || dto.getConfirmationCode() == null || dto.getConfirmationCode().isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Invalid confirmation code", null));
            return;
        }

        boolean ok = waitingController.confirmArrival(dto.getConfirmationCode().trim());
        if (!ok) {
            client.sendToClient(new ResponseDTO(false, "Arrival confirm failed (expired / not ready / not found)", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "Seated successfully", null));
    }
}
