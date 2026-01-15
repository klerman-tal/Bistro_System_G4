package network;

import java.time.LocalTime;
import java.util.ArrayList;

import dto.GetAvailableTimesDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class GetAvailableTimesForDateHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetAvailableTimesForDateHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        Object obj = request.getData();
        if (!(obj instanceof GetAvailableTimesDTO dto)) {
            client.sendToClient(new ResponseDTO(false, "Invalid available-times request", new ArrayList<LocalTime>()));
            return;
        }

        ArrayList<LocalTime> times =
                reservationController.getAvailableTimesForDay(dto.getDate(), dto.getGuests());

        client.sendToClient(new ResponseDTO(true, "Available times loaded", times));
    }
}
