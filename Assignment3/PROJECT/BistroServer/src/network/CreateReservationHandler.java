package network;

import java.time.LocalTime;
import java.util.ArrayList;

import dto.CreateReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Reservation;
import logicControllers.ReservationController;
import network.RequestHandler;
import ocsf.server.ConnectionToClient;

public class CreateReservationHandler implements RequestHandler {

    private final ReservationController reservationController;

    public CreateReservationHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {
        // 1) חילוץ הנתונים
        CreateReservationDTO data = (CreateReservationDTO) request.getData();
        ArrayList<LocalTime> availableTimesOut = new ArrayList<>();

        Reservation res = reservationController.CreateTableReservation(data, availableTimesOut);

        // 3) בדיקה אם ההזמנה הצליחה ושליחת תשובה
        if (res != null) {
            // שלחי לקליינט את קוד האישור (confirmationCode)
            ResponseDTO response = new ResponseDTO(true, "Reservation created", res.getConfirmationCode());
            client.sendToClient(response);
        } else {
            ResponseDTO response = new ResponseDTO(false, "No availability for requested time", null);
            client.sendToClient(response);
        }
    }
}
