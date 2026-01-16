package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;

/**
 * Server-side request handler responsible for retrieving
 * the active waiting list.
 * <p>
 * This handler fetches all currently active waiting entries
 * from the {@link WaitingController} and returns them to the client.
 * It is typically used by restaurant staff to monitor the waiting list.
 * </p>
 */
public class GetWaitingListHandler implements RequestHandler {

    private final WaitingController waitingController;

    /**
     * Constructs a handler with the required waiting controller dependency.
     */
    public GetWaitingListHandler(WaitingController waitingController) {
        this.waitingController = waitingController;
    }

    /**
     * Handles a request to retrieve the active waiting list.
     * <p>
     * The method loads all active waiting entries and sends them
     * back to the client as part of the response. In case of an
     * error, a failure response is returned.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {
        ResponseDTO response;

        try {
            ArrayList<Waiting> waitingList =
                    waitingController.getActiveWaitingList();

            response = new ResponseDTO(
                    true,
                    "Waiting list retrieved successfully",
                    waitingList
            );
        } catch (Exception e) {
            response = new ResponseDTO(
                    false,
                    "Failed to retrieve waiting list: " + e.getMessage(),
                    null
            );
        }

        try {
            client.sendToClient(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
