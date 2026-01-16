package network;

import java.util.ArrayList;

import dto.CurrentDinerDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class GetCurrentDinersHandler implements RequestHandler {

    private final ReservationController reservationController;

    public GetCurrentDinersHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        ArrayList<Reservation> reservations = reservationController.getCurrentDiners();
        ArrayList<CurrentDinerDTO> result = new ArrayList<>();

        for (Reservation r : reservations) {
            // הגנה: אם אין תפקיד, נכתוב "Unknown" במקום לקרוס
            String roleStr = (r.getCreatedByRole() != null) ? r.getCreatedByRole().name() : "Guest";
            
            result.add(new CurrentDinerDTO(
                r.getCreatedByUserId(),
                roleStr,
                r.getTableNumber()
            ));
        }

        client.sendToClient(new ResponseDTO(true, "Current diners loaded", result));
    }

}
