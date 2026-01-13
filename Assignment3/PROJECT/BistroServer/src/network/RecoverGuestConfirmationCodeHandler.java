package network;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import dbControllers.Notification_DB_Controller;
import dto.NotificationDTO;
import dto.RecoverGuestConfirmationCodeDTO;
import dto.RequestDTO;
import dto.ResponseDTO;
import entities.Enums;
import entities.Notification;
import logicControllers.ReservationController;
import logicControllers.UserController;
import ocsf.server.ConnectionToClient;

public class RecoverGuestConfirmationCodeHandler implements RequestHandler {

    private final ReservationController reservationController;
    private final UserController userController;

    private final Notification_DB_Controller notificationDB;
    private final Consumer<String> logger;

    public RecoverGuestConfirmationCodeHandler(ReservationController rc,
                                              UserController uc,
                                              Notification_DB_Controller notificationDB,
                                              Consumer<String> logger) {
        this.reservationController = rc;
        this.userController = uc;
        this.notificationDB = notificationDB;
        this.logger = logger;
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
            return;
        }

        // =========================
        // Simulated SMS + Email
        // =========================
        LocalDateTime now = LocalDateTime.now();

        // We don't necessarily have a real userId for guest here.
        // We store 0 as "guest/unknown userId" for audit.
        int userIdForAudit = 0;

        // English texts
        String displayMessage = "Verification code has been sent via SMS.";
        String smsBody = "Your confirmation code is: " + code;
        String emailBody = "Your confirmation code is: " + code;

        // 1) Save EMAIL in DB + mark sent (simulation)
        int emailId = notificationDB.addNotification(new Notification(
                userIdForAudit,
                Enums.Channel.EMAIL,
                Enums.NotificationType.RESEND_CONFIRMATION_CODE,
                emailBody,
                now
        ));
        if (emailId != -1) {
            notificationDB.markAsSent(emailId, now);
        }
        if (logger != null) {
            logger.accept("📧 EMAIL-SIM | Guest recover code | phone=" + data.getPhone() + " | " + emailBody);
        }

        // 2) Save SMS in DB + mark sent (simulation)
        int smsId = notificationDB.addNotification(new Notification(
                userIdForAudit,
                Enums.Channel.SMS,
                Enums.NotificationType.RESEND_CONFIRMATION_CODE,
                smsBody,
                now
        ));
        if (smsId != -1) {
            notificationDB.markAsSent(smsId, now);
        }

        // 3) Popup in app shows only safe text (no code), but SMS simulation carries the code
        client.sendToClient(new NotificationDTO(
                NotificationDTO.Type.INFO,
                "SMS",
                displayMessage,
                smsBody
        ));

        // 4) Silent response (avoid a second popup)
        client.sendToClient(new ResponseDTO(true, "Confirmation code recovered.", code));

    }
}
