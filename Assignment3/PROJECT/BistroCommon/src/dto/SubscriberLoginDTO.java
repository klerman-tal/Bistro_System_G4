package dto;

import java.io.Serializable;

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