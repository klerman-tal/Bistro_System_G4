package network;

import dto.RecoverGuestConfirmationCodeDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import logicControllers.ReservationController;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class RecoverGuestConfirmationCodeHandler implements RequestHandler {

    private final ReservationController reservationController;
    private final UserController userController;

    public RecoverGuestConfirmationCodeHandler(ReservationController rc, UserController uc) {
        this.reservationController = rc;
        this.userController = uc;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        RecoverGuestConfirmationCodeDTO data = (RecoverGuestConfirmationCodeDTO) request.getData();

        if (data == null) {
            client.sendToClient(new ResponseDTO(false, "Missing data.", null));
            return;
        }

        String code = reservationController.recoverGuestConfirmationCode(
                data.getPhone(),
                data.getEmail(),
                data.getReservationDateTime()
        );

        if (code == null) {
            client.sendToClient(new ResponseDTO(false,
                    "Reservation not found. Please check details.", null));
        } else {
            client.sendToClient(new ResponseDTO(true,
                    "Confirmation code recovered.", code));
        }
    }
}
