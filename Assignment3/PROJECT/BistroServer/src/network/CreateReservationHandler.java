package network;

import java.time.LocalTime;
import java.util.ArrayList;

import dto.CreateReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for creating new table reservations.
 * <p>
 * This handler processes reservation creation requests, delegates availability
 * checks and reservation logic to the {@link ReservationController}, and
 * returns either a successful confirmation code or alternative available time
 * suggestions to the client.
 * </p>
 */
public class CreateReservationHandler implements RequestHandler {

	private final ReservationController reservationController;

	/**
	 * Constructs a handler with the required reservation controller dependency.
	 */
	public CreateReservationHandler(ReservationController reservationController) {
		this.reservationController = reservationController;
	}

	/**
	 * Handles a create reservation request received from the client.
	 * <p>
	 * The method attempts to create a reservation for the requested date and time.
	 * If no reservation can be created, it returns suggested alternative times when
	 * available.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		CreateReservationDTO data = (CreateReservationDTO) request.getData();
		ArrayList<LocalTime> availableTimesOut = new ArrayList<>();

		Reservation res = reservationController.CreateTableReservation(data, availableTimesOut);

		if (res != null) {
			ResponseDTO response = new ResponseDTO(true, "Reservation created", res.getConfirmationCode());
			client.sendToClient(response);
			return;
		}

		if (availableTimesOut == null || availableTimesOut.isEmpty()) {
			ResponseDTO response = new ResponseDTO(false,
					"No tables available for 2 hours today from the selected time and onward.",
					new ArrayList<LocalTime>());
			client.sendToClient(response);
			return;
		}

		ResponseDTO response = new ResponseDTO(false, "No availability for requested time. Suggested times:",
				availableTimesOut);
		client.sendToClient(response);
	}
}
