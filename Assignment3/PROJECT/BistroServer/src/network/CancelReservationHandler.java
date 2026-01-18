package network;

import dto.CancelReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for processing reservation
 * cancellation requests.
 * <p>
 * This handler validates the cancellation request, delegates the cancellation
 * logic to the {@link ReservationController} and returns an appropriate
 * response to the client.
 * </p>
 */
public class CancelReservationHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public CancelReservationHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a cancel reservation request received from the client.
	 * <p>
	 * The method validates the confirmation code, attempts to cancel the
	 * reservation, and sends a success or failure response back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		CancelReservationDTO dto = (CancelReservationDTO) request.getData();

		ResponseDTO response;

		if (dto == null || dto.getConfirmationCode() == null || dto.getConfirmationCode().isBlank()) {

			response = new ResponseDTO(false, "Confirmation code is required", null);

		} else {

			boolean cancelled = reservationController.CancelReservation(dto.getConfirmationCode());

			if (cancelled) {
				response = new ResponseDTO(true, "Reservation cancelled successfully", null);
			} else {
				response = new ResponseDTO(false, "Reservation not found or not active", null);
			}
		}

		try {
			client.sendToClient(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
