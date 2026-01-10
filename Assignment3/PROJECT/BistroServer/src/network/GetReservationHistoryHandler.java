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

public class GetReservationHistoryHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetReservationHistoryHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        GetReservationHistoryDTO dto =
                (GetReservationHistoryDTO) request.getData();

        int subscriberId = dto.getSubscriberId();

        ArrayList<Reservation> history =
                reservationController.getReservationsForUser(subscriberId);

        client.sendToClient(
                new ResponseDTO(true, "Reservation history loaded", history)
        );
    }

}
