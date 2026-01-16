package network;

import java.util.ArrayList;

import dto.GetMyActiveReservationsDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class GetMyActiveReservationsHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetMyActiveReservationsHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

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

    private void safeSend(ConnectionToClient client, ResponseDTO res) {
        try { client.sendToClient(res); } catch (Exception e) { e.printStackTrace(); }
    }
}
