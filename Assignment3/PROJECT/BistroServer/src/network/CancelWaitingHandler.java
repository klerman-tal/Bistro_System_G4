package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for processing waiting list
 * cancellation requests.
 * <p>
 * This handler validates the waiting confirmation code, delegates the
 * cancellation logic to the {@link WaitingController}, and sends an appropriate
 * response back to the client.
 * </p>
 */
public class CancelWaitingHandler implements RequestHandler {

	private final WaitingController waitingController;

	/**
	 * Constructs a handler with the required waiting controller dependency.
	 */
	public CancelWaitingHandler(WaitingController waitingController) {
		this.waitingController = waitingController;
	}

	/**
	 * Handles a cancel waiting request received from the client.
	 * <p>
	 * The method validates the confirmation code, attempts to remove the user from
	 * the waiting list, and sends a success or failure response back to the client.
	 * </p>
	 */
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
