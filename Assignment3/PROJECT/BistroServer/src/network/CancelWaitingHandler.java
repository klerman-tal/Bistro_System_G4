package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

public class CancelWaitingHandler implements RequestHandler {

    private final WaitingController waitingController;

    public CancelWaitingHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        WaitingCodeDTO dto = (WaitingCodeDTO) request.getData();
        if (dto == null || dto.getConfirmationCode() == null || dto.getConfirmationCode().isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Invalid confirmation code", null));
            return;
        }

        boolean ok = waitingController.leaveWaitingList(dto.getConfirmationCode().trim());
        if (!ok) {
            client.sendToClient(new ResponseDTO(false, "Cancel failed (not active or not found)", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "Waiting cancelled", null));
    }
}
