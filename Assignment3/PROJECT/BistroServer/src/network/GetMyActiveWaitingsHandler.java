package network;

import java.util.ArrayList;

import dto.GetMyActiveWaitingsDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving the active waiting
 * list entries of a specific user.
 * <p>
 * This handler processes user-specific waiting list queries and returns all
 * currently active waiting records associated with the given user identifier.
 * </p>
 */
public class GetMyActiveWaitingsHandler implements RequestHandler {

	private final WaitingController waitingController;

	/**
	 * Constructs a handler with the required waiting controller dependency.
	 */
	public GetMyActiveWaitingsHandler(WaitingController waitingController) {
		this.waitingController = waitingController;
	}

	/**
	 * Handles a request to retrieve active waiting list entries for a user.
	 * <p>
	 * The method validates the request payload, delegates the retrieval logic to
	 * the waiting controller, and sends the resulting waiting list back to the
	 * client.
	 * </p>
	 */
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

	/**
	 * Safely sends a response to the client.
	 * <p>
	 * Any communication exception is caught and logged to prevent server
	 * interruption.
	 * </p>
	 */
	private void safeSend(ConnectionToClient client, ResponseDTO res) {
		try {
			client.sendToClient(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
