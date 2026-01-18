package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to deliver notification information from the
 * server to the client.
 * <p>
 * This object represents a user-facing notification, including its type, the
 * message to be displayed in the application, and an optional channel message
 * intended for external communication such as SMS or email.
 * </p>
 */
public class NotificationDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Type {
		INFO, SUCCESS, WARNING, ERROR
	}

	private final String displayMessage;
	private final String channelMessage;
	private final String channel;
	private final Type type;

	public NotificationDTO(Type type, String channel, String displayMessage, String channelMessage) {
		this.type = type;
		this.channel = channel;
		this.displayMessage = displayMessage;
		this.channelMessage = channelMessage;
	}

	public Type getType() {
		return type;
	}

	public String getChannel() {
		return channel;
	}

	public String getDisplayMessage() {
		return displayMessage;
	}

	public String getChannelMessage() {
		return channelMessage;
	}
}
