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

/**
 * Server-side request handler responsible for recovering a guest's reservation
 * confirmation code and delivering it through simulated notification channels.
 * <p>
 * This handler:
 * <ul>
 *   <li>Validates guest identification details (phone, email, reservation date/time)</li>
 *   <li>Delegates code recovery to {@link ReservationController}</li>
 *   <li>Creates simulated EMAIL and SMS notifications (stored and marked as sent in DB)</li>
 *   <li>Sends a safe in-app popup message (without sensitive data) via {@link NotificationDTO}</li>
 *   <li>Sends a final {@link ResponseDTO} containing the recovered code</li>
 * </ul>
 * </p>
 * <p>
 * Note: This flow intentionally stores {@code userId=0} for guest/unknown users
 * to preserve an audit trail without requiring a registered user identity.
 * </p>
 */
public class RecoverGuestConfirmationCodeHandler implements RequestHandler {

    private final ReservationController reservationController;
    private final UserController userController;

    private final Notification_DB_Controller notificationDB;
    private final Consumer<String> logger;

    /**
     * Constructs a handler with required dependencies for recovery and notifications.
     *
     * @param rc             controller used for reservation lookup and code recovery
     * @param uc             user controller (dependency provided for consistency / future use)
     * @param notificationDB database controller used to persist notification audit records
     * @param logger         optional logger callback for server-side tracing/debugging
     */
    public RecoverGuestConfirmationCodeHandler(ReservationController rc,
                                              UserController uc,
                                              Notification_DB_Controller notificationDB,
                                              Consumer<String> logger) {
        this.reservationController = rc;
        this.userController = uc;
        this.notificationDB = notificationDB;
        this.logger = logger;
    }

    /**
     * Handles a request to recover a guest confirmation code.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Validates request payload</li>
     *   <li>Recovers the confirmation code from the reservation controller</li>
     *   <li>Persists simulated EMAIL/SMS notifications and marks them as sent</li>
     *   <li>Sends an in-app popup notification (safe display text, SMS body includes the code)</li>
     *   <li>Sends a final response to the client with the recovered code</li>
     * </ol>
     * </p>
     *
     * @param request the incoming request containing {@link RecoverGuestConfirmationCodeDTO}
     * @param client  the client connection associated with this request
     * @throws Exception if an unrecoverable error occurs during request handling
     */
    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        /**
         * Step 1: Extract and validate request payload.
         */
        RecoverGuestConfirmationCodeDTO data = (RecoverGuestConfirmationCodeDTO) request.getData();

        if (data == null) {
            client.sendToClient(new ResponseDTO(false, "Missing data.", null));
            return;
        }

        /**
         * Step 2: Attempt to recover the reservation confirmation code based on
         * guest identifying details and reservation date/time.
         */
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

        /**
         * Step 3: Create audit records for simulated SMS/Email notifications.
         * These records are stored in DB and marked as sent for traceability.
         */
        LocalDateTime now = LocalDateTime.now();

        // Audit uses 0 because guest users might not have a persisted userId.
        int userIdForAudit = 0;

        // Safe display text (no code), while channel content contains the recovered code.
        String displayMessage = "Verification code has been sent via SMS.";
        String smsBody = "Your confirmation code is: " + code;
        String emailBody = "Your confirmation code is: " + code;

        /**
         * Step 4: Save EMAIL notification in DB and mark it as sent (simulation).
         */
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

        /**
         * Step 5: Save SMS notification in DB and mark it as sent (simulation).
         */
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

        /**
         * Step 6: Send in-app popup notification (safe UI message without sensitive data).
         * The SMS simulation content contains the actual code.
         */
        client.sendToClient(new NotificationDTO(
                NotificationDTO.Type.INFO,
                "SMS",
                displayMessage,
                smsBody
        ));

        /**
         * Step 7: Send a final response message to complete the request.
         * A silent response avoids triggering an additional popup on the client.
         */
        client.sendToClient(new ResponseDTO(true, "Confirmation code recovered.", code));

    }
}
