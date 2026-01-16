package network;

import java.util.ArrayList;

import dto.GetMyActiveWaitingsDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

public class GetMyActiveWaitingsHandler implements RequestHandler {

    private final WaitingController waitingController;

    public GetMyActiveWaitingsHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        Object obj = request.getData();
        if (!(obj instanceof GetMyActiveWaitingsDTO dto)) {
            safeSend(client, new ResponseDTO(false, "Invalid request data", null));
            return;
        }

        ArrayList<Waiting> list = waitingController.getActiveWaitingsForUser(dto.getUserId());
        safeSend(client, new ResponseDTO(true, "OK", list));
    }

    private void safeSend(ConnectionToClient client, ResponseDTO res) {
        try { client.sendToClient(res); } catch (Exception e) { e.printStackTrace(); }
    }
}
