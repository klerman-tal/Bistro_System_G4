package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

import java.util.List;

public class GetAllReservationsHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetAllReservationsHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        try {
            List<Reservation> reservations =
                    reservationController.getAllReservationsHistory();

            client.sendToClient(
                    new ResponseDTO(
                            true,
                            "Reservations loaded successfully",
                            reservations
                    )
            );

        } catch (Exception e) {
            try {
                client.sendToClient(
                        new ResponseDTO(
                                false,
                                e.getMessage(),
                                null
                        )
                );
            } catch (Exception ignored) {}
        }
    }
}
