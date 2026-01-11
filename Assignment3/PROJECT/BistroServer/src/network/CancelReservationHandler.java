package network;

import dto.CancelReservationDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import ocsf.server.ConnectionToClient;

public class CancelReservationHandler implements RequestHandler {

    private final ReservationController reservationController;

    public CancelReservationHandler(ReservationController reservationController) {
        this.reservationController = reservationController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) {

        CancelReservationDTO dto =
                (CancelReservationDTO) request.getData();

        ResponseDTO response;

        if (dto == null ||
            dto.getConfirmationCode() == null ||
            dto.getConfirmationCode().isBlank()) {

            response = new ResponseDTO(
                    false,
                    "Confirmation code is required",
                    null
            );

        } else {

            boolean cancelled =
                    reservationController.CancelReservation(
                            dto.getConfirmationCode()
                    );

            if (cancelled) {
                response = new ResponseDTO(
                        true,
                        "Reservation cancelled successfully",
                        null
                );
            } else {
                response = new ResponseDTO(
                        false,
                        "Reservation not found or not active",
                        null
                );
            }
        }

        // 🔥 שליחת התגובה ללקוח
        try {
            client.sendToClient(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
