package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer subscriber login
 * information from the client to the server.
 * <p>
 * This object contains the subscriber identifier and username,
 * which are used to authenticate or identify a registered
 * subscriber in the system.
 * </p>
 */
public class SubscriberLoginDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int subscriberId;
    private String username;

    public SubscriberLoginDTO(int subscriberId, String username) {
        this.subscriberId = subscriberId;
        this.username = username;
    }

    public int getSubscriberId() { return subscriberId; }
    public String getUsername() { return username; }
}
