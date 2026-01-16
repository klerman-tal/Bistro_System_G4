package entities;

import java.time.LocalDateTime;

import entities.Enums.Channel;
import entities.Enums.NotificationType;

/**
 * Entity representing a scheduled notification stored on the server side.
 * <p>
 * This entity is used to simulate SMS or email notifications and is persisted
 * in the database. It contains scheduling information, delivery status, and
 * metadata related to the notification channel and type.
 * </p>
 */
public class Notification {

    private int notificationId;
    private int userId;

    private Channel channel;
    private NotificationType notificationType;

    private String message;
    private LocalDateTime scheduledFor;

    private boolean sent;
    private LocalDateTime sentAt;

    /**
     * Default constructor required for frameworks and database mapping.
     */
    public Notification() {}

    /**
     * Constructs a new notification instance intended for insertion into the database.
     * <p>
     * The notification is initialized as not sent.
     * </p>
     */
    public Notification(int userId,
    							Channel channel,
                              NotificationType notificationType,
                              String message,
                              LocalDateTime scheduledFor) {
        this.userId = userId;
        this.channel = channel;
        this.notificationType = notificationType;
        this.message = message;
        this.scheduledFor = scheduledFor;
        this.sent = false;
    }

    /**
     * Constructs a notification instance loaded from the database.
     * <p>
     * This constructor is typically used when reconstructing the entity
     * from persisted data.
     * </p>
     */
    public Notification(int notificationId,
                              int userId,
                              Channel channel,
                              NotificationType notificationType,
                              String message,
                              LocalDateTime scheduledFor,
                              boolean sent,
                              LocalDateTime sentAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.channel = channel;
        this.notificationType = notificationType;
        this.message = message;
        this.scheduledFor = scheduledFor;
        this.sent = sent;
        this.sentAt = sentAt;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
