package network;

import dto.CheckinReservationDTO;
import dto.GetTableResultDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

/**
 * Server-side request handler responsible for processing
 * reservation check-in requests.
 * <p>
 * This handler validates the incoming check-in request,
 * delegates the check-in logic to the {@link ReservationController},
 * and returns the table allocation result to the client.
 * </p>
 */
public class CheckinReservationHandler implements RequestHandler {

    private final ReservationController reservationController;

    /**
     * Constructs a handler with the required reservation controller dependency.
     */
    public CheckinReservationHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    /**
     * Handles a reservation check-in request received from the client.
     * <p>
     * The method validates the request payload, performs the check-in
     * operation using the reservation controller, and sends the
     * resulting table assignment or error message back to the client.
     * </p>
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        Object dataObj = request.getData();
        if (!(dataObj instanceof CheckinReservationDTO dto)) {
            send(client, new ResponseDTO(false, "Invalid check-in data", null));
            return;
        }

        String code = dto.getConfirmationCode();
        GetTableResultDTO result = reservationController.checkinReservationByCode(code);

        boolean success = result != null && result.isSuccess();
        String msg = (result != null && result.getMessage() != null) ? result.getMessage() : "Request processed";

        send(client, new ResponseDTO(success, msg, result));
    }

    /**
     * Sends a response back to the client in a safe manner.
     * <p>
     * Any communication exception is caught and logged to avoid
     * interrupting the server execution flow.
     * </p>
     */
    private void send(ConnectionToClient client, ResponseDTO res) {
        try {
            client.sendToClient(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
