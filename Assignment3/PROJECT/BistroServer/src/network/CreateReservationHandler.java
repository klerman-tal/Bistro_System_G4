package network;

import java.time.LocalTime;
import java.util.ArrayList;

import dto.CreateReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class CreateReservationHandler implements RequestHandler {

    private final ReservationController reservationController;

    public CreateReservationHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        CreateReservationDTO data = (CreateReservationDTO) request.getData();
        ArrayList<LocalTime> availableTimesOut = new ArrayList<>();

        Reservation res = reservationController.CreateTableReservation(data, availableTimesOut);

        if (res != null) {
            ResponseDTO response =
                    new ResponseDTO(true, "Reservation created", res.getConfirmationCode());
            client.sendToClient(response);
            return;
        }

        // No reservation created
        if (availableTimesOut == null || availableTimesOut.isEmpty()) {
            // No suggested times for that day from requested time and onward
            ResponseDTO response =
                    new ResponseDTO(false,
                            "No tables available for 2 hours today from the selected time and onward.",
                            new ArrayList<LocalTime>());
            client.sendToClient(response);
            return;
        }

        // Send suggested times (same day, from requested time and onward)
        ResponseDTO response =
                new ResponseDTO(false,
                        "No availability for requested time. Suggested times:",
                        availableTimesOut);
        client.sendToClient(response);
    }
}
