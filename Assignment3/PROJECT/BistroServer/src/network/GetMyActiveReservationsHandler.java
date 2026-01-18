package network;

import java.util.ArrayList;

import dto.GetMyActiveReservationsDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving the active
 * reservations of a specific user.
 * <p>
 * This handler processes user-specific reservation queries and returns all
 * currently active reservations associated with the given user identifier.
 * </p>
 */
public class GetMyActiveReservationsHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public GetMyActiveReservationsHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a request to retrieve active reservations for a user.
	 * <p>
	 * The method validates the request payload, delegates the retrieval logic to
	 * the reservation controller, and sends the resulting reservation list back to
	 * the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) {
		Object obj = request.getData();
		if (!(obj instanceof GetMyActiveReservationsDTO dto)) {
			safeSend(client, new ResponseDTO(false, "Invalid request data", null));
			return;
		}

		ArrayList<Reservation> list = reservationController.getActiveReservationsForUser(dto.getUserId());

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
