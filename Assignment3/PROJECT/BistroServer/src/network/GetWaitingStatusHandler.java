package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.WaitingCodeDTO;
import entities.Waiting;
import logicControllers.WaitingController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving the current status of
 * a waiting list entry.
 * <p>
 * This handler receives a waiting confirmation code, validates it, retrieves
 * the corresponding waiting entry, and returns its current status to the
 * client.
 * </p>
 */
public class GetWaitingStatusHandler implements RequestHandler {

	private final WaitingController waitingController;

	/**
	 * Constructs a handler with the required waiting controller dependency.
	 */
	public GetWaitingStatusHandler(WaitingController waitingController) {
		this.waitingController = waitingController;
	}

	/**
	 * Handles a request to retrieve the status of a waiting entry.
	 * <p>
	 * The method validates the confirmation code, fetches the waiting record from
	 * the waiting controller, and returns the current waiting state to the client.
	 * </p>
	 */
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
