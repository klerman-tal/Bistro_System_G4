package network;

import java.util.ArrayList;

import dto.CurrentDinerDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving the list of current
 * diners in the restaurant.
 * <p>
 * This handler fetches active reservations representing diners currently seated
 * in the restaurant and converts them into lightweight DTO objects for client
 * display.
 * </p>
 */
public class GetCurrentDinersHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public GetCurrentDinersHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a request to retrieve the current diners.
	 * <p>
	 * The method loads active diner reservations, maps them to
	 * {@link CurrentDinerDTO} objects, and sends the result back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
		ArrayList<Reservation> reservations = reservationController.getCurrentDiners();
		ArrayList<CurrentDinerDTO> result = new ArrayList<>();

		for (Reservation r : reservations) {
			String roleStr = (r.getCreatedByRole() != null) ? r.getCreatedByRole().name() : "Guest";

			result.add(new CurrentDinerDTO(r.getCreatedByUserId(), roleStr, r.getTableNumber()));
		}

		client.sendToClient(new ResponseDTO(true, "Current diners loaded", result));
	}
}
