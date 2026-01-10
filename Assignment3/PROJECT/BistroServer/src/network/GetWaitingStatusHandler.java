package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

public class GetWaitingStatusHandler implements RequestHandler {

    private final WaitingController waitingController;

    public GetWaitingStatusHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        WaitingCodeDTO dto = (WaitingCodeDTO) request.getData();
        if (dto == null || dto.getConfirmationCode() == null || dto.getConfirmationCode().isBlank()) {
            client.sendToClient(new ResponseDTO(false, "Invalid confirmation code", null));
            return;
        }

        Waiting w = waitingController.getWaitingByCode(dto.getConfirmationCode().trim());
        if (w == null) {
            client.sendToClient(new ResponseDTO(false, "Waiting entry not found", null));
            return;
        }

        client.sendToClient(new ResponseDTO(true, "Waiting status", w));
    }
}
