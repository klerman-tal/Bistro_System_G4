package dto;

import java.io.Serializable;

public class NotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        INFO, SUCCESS, WARNING, ERROR
    }

    // What the app should show in popup (NO sensitive info like codes)
    private final String displayMessage;

    // The simulated SMS/Email content (can include the code)
    private final String channelMessage;

    // Optional: for future use (SMS / EMAIL)
    private final String channel;

    private final Type type;

    public NotificationDTO(Type type, String channel, String displayMessage, String channelMessage) {
        this.type = type;
        this.channel = channel;
        this.displayMessage = displayMessage;
        this.channelMessage = channelMessage;
    }

    public Type getType() { return type; }
    public String getChannel() { return channel; }

    public String getDisplayMessage() { return displayMessage; }
    public String getChannelMessage() { return channelMessage; }
}
