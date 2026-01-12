package entities;

import java.time.LocalDateTime;

import entities.Enums.Channel;
import entities.Enums.NotificationType;

/**
 * Server-side entity stored in DB for scheduling messages (SMS/Email simulation).
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

    public Notification() {}

    // For insert
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

    // For DB load
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
