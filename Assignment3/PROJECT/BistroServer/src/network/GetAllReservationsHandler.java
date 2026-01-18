package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

import java.util.List;

/**
 * Server-side request handler responsible for retrieving the complete
 * reservations history.
 * <p>
 * This handler fetches all reservation records from the system via the
 * {@link ReservationController} and returns them to the client.
 * </p>
 */
public class GetAllReservationsHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public GetAllReservationsHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a request to retrieve all reservations.
	 * <p>
	 * The method loads the full reservations history and sends it back to the
	 * client as part of the response.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {

		try {
			List<Reservation> reservations = reservationController.getAllReservationsHistory();

			client.sendToClient(new ResponseDTO(true, "Reservations loaded successfully", reservations));

		} catch (Exception e) {
			try {
				client.sendToClient(new ResponseDTO(false, e.getMessage(), null));
			} catch (Exception ignored) {
			}
		}
	}
}
