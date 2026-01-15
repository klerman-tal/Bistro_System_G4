package logicControllers;

import java.util.function.Consumer;

import dto.NotificationDTO;
import dto.NotificationDTO.Type;
import entities.Enums.Channel;
import entities.Notification;
import ocsf.server.ConnectionToClient;
//test
/**
 * Sends notifications through different channels (SMS/EMAIL simulation).
 * Uses logger callback instead of System.out.println.
 */
public class NotificationDispatcher {

    private final OnlineUsersRegistry onlineUsers;
    private final Consumer<String> logger;

    public NotificationDispatcher(OnlineUsersRegistry onlineUsers, Consumer<String> logger) {
        this.onlineUsers = onlineUsers;
        this.logger = logger;
    }

    public void dispatch(Notification n) {

        // EMAIL simulation -> log only (and mark as sent by scheduler)
        if (n.getChannel() == Channel.EMAIL) {
            logger.accept("📧 EMAIL-SIM | userId=" + n.getUserId() + " | " + n.getMessage());
            return;
        }

        // SMS simulation -> popup (safe text) + channel message (full SMS content) for the client logs
        if (n.getChannel() == Channel.SMS) {
            ConnectionToClient client = onlineUsers.getClient(n.getUserId());

            if (client == null) {
                logger.accept("📩 SMS-SIM (OFFLINE) | userId=" + n.getUserId() + " | " + n.getMessage());
                return;
            }

            try {
                Type popupType = Type.INFO;

                // Safe popup text (NO code / sensitive details)
                String displayMessage = getSafeDisplayMessage(n);

                // The actual simulated SMS body (can include the code)
                String smsBody = n.getMessage();

                client.sendToClient(new NotificationDTO(
                        popupType,
                        "SMS",
                        displayMessage,
                        smsBody
                ));

                logger.accept("📩 SMS-SIM (SENT) | userId=" + n.getUserId() + " | " + smsBody);

            } catch (Exception e) {
                logger.accept("❌ SMS-SIM error | userId=" + n.getUserId() + " | " + e.getMessage());
            }
        }
    }

    /**
     * Returns safe UI text for popup (no sensitive info like codes).
     */
    private String getSafeDisplayMessage(Notification n) {

        // If you want more accurate messages per type, map by notificationType:
        switch (n.getNotificationType()) {

            case RESEND_CONFIRMATION_CODE:
                return "Verification code has been sent via SMS.";

            case RESERVATION_REMINDER_2H:
                return "Reminder: your reservation is in 2 hours.";

            case TABLE_AVAILABLE:
                return "A table is now available. Please confirm your arrival.";

            case BILL_AFTER_2H_FROM_CHECKIN:
                return "Your bill details have been sent via SMS.";
                
            case RESERVATION_CANCELLED_OPENING_HOURS:
                return "Your reservation was cancelled due to a change in opening hours.";

            default:
                return "You have a new notification.";
        }
    }
}
