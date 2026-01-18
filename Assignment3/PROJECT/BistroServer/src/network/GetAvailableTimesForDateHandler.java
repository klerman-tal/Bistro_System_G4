package network;

import java.time.LocalTime;
import java.util.ArrayList;

import dto.GetAvailableTimesDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for retrieving available reservation
 * times for a specific date.
 * <p>
 * This handler processes availability queries based on the requested date and
 * number of guests, and returns a list of possible reservation times.
 * </p>
 */
public class GetAvailableTimesForDateHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public GetAvailableTimesForDateHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a request to retrieve available reservation times.
	 * <p>
	 * The method validates the request payload, delegates the availability
	 * computation to the reservation controller, and sends the resulting time slots
	 * back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		Object obj = request.getData();
		if (!(obj instanceof GetAvailableTimesDTO dto)) {
			client.sendToClient(new ResponseDTO(false, "Invalid available-times request", new ArrayList<LocalTime>()));
			return;
		}

		ArrayList<LocalTime> times = reservationController.getAvailableTimesForDay(dto.getDate(), dto.getGuests());

		client.sendToClient(new ResponseDTO(true, "Available times loaded", times));
	}
}
