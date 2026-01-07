package network;

import dto.CreateReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
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

        // 1) extract payload
        CreateReservationDTO data = (CreateReservationDTO) request.getData();

        // 2) call business logic (you implement this)
        int confirmationCode = reservationController.CreateTableReservation(data);

        // 3) respond back to client
        ResponseDTO response = new ResponseDTO(true, "Reservation created", confirmationCode);
        client.sendToClient(response);
    }
}
