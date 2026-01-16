package network;

import dto.CheckinReservationDTO;
import dto.GetTableResultDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

public class CheckinReservationHandler implements RequestHandler {

    private final ReservationController reservationController;

    public CheckinReservationHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

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

    private void send(ConnectionToClient client, ResponseDTO res) {
        try {
            client.sendToClient(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
