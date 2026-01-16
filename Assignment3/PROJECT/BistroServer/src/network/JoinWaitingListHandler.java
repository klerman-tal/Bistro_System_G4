package network;

import dto.JoinWaitingDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;
import entities.User;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

public class JoinWaitingListHandler implements RequestHandler {

    private final WaitingController waitingController;

    public JoinWaitingListHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        JoinWaitingDTO dto = (JoinWaitingDTO) request.getData();
        if (dto == null || dto.getGuests() <= 0) {
            client.sendToClient(new ResponseDTO(false, "Invalid guests number", null));
            return;
        }

        // build minimal User from dto
        User u = new User();
        u.setUserId(dto.getUserId());
        u.setUserRole(dto.getRole() == null ? UserRole.RandomClient : dto.getRole());

        Waiting w;

        try {
            // Accurate reason (restaurant closed) comes from controller exception
            w = waitingController.joinWaitingListNowOrThrow(dto.getGuests(), u);

        } catch (WaitingController.JoinWaitingBlockedException e) {
            client.sendToClient(new ResponseDTO(false, e.getMessage(), null));
            return;

        } catch (Exception e) {
            // Any other failure (DB/network/etc.)
            client.sendToClient(new ResponseDTO(false, "Failed to join waiting list", null));
            return;
        }

        if (w.getWaitingStatus() == WaitingStatus.Seated) {
            client.sendToClient(new ResponseDTO(true,
                    "Table available now. Please go to the table.",
                    w));
        } else {
            client.sendToClient(new ResponseDTO(true,
                    "Joined waiting list. Please wait for a table.",
                    w));
        }
    }
}
