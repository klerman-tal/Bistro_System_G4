package network;

import java.util.ArrayList;
import java.util.List;

import dto.GetReservationHistoryDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import entities.User;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;
import network.RequestHandler;

/**
 * Server-side request handler responsible for retrieving the reservation
 * history of a specific subscriber.
 * <p>
 * This handler processes history requests by subscriber ID and returns all past
 * reservations associated with that user.
 * </p>
 */
public class GetReservationHistoryHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public GetReservationHistoryHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a request to retrieve a user's reservation history.
	 * <p>
	 * The method extracts the subscriber identifier from the request, retrieves the
	 * corresponding reservation history, and sends it back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		GetReservationHistoryDTO dto = (GetReservationHistoryDTO) request.getData();

		int subscriberId = dto.getSubscriberId();

		ArrayList<Reservation> history = reservationController.getReservationsForUser(subscriberId);

		client.sendToClient(new ResponseDTO(true, "Reservation history loaded", history));
	}
}
