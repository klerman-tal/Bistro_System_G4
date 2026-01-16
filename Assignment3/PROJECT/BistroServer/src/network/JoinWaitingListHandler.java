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

/**
 * Server-side request handler responsible for joining
 * the restaurant waiting list.
 * <p>
 * This handler processes waiting list join requests, validates
 * the input data, constructs a minimal user context, and delegates
 * the join logic to the {@link WaitingController}. The response
 * indicates whether the user was seated immediately or added
 * to the waiting list.
 * </p>
 */
public class JoinWaitingListHandler implements RequestHandler {

    private final WaitingController waitingController;

    /**
     * Constructs a handler with the required waiting controller dependency.
     */
    public JoinWaitingListHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    /**
     * Handles a request to join the waiting list.
     * <p>
     * The method validates the number of guests, builds a minimal
     * user object from the request data, attempts to join the waiting
     * list, and returns the resulting waiting status to the client.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        JoinWaitingDTO dto = (JoinWaitingDTO) request.getData();
        if (dto == null || dto.getGuests() <= 0) {
            client.sendToClient(new ResponseDTO(false, "Invalid guests number", null));
            return;
        }

        User u = new User();
        u.setUserId(dto.getUserId());
        u.setUserRole(dto.getRole() == null ? UserRole.RandomClient : dto.getRole());

        Waiting w;

        try {
            w = waitingController.joinWaitingListNowOrThrow(dto.getGuests(), u);

        } catch (WaitingController.JoinWaitingBlockedException e) {
            client.sendToClient(new ResponseDTO(false, e.getMessage(), null));
            return;

        } catch (Exception e) {
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
