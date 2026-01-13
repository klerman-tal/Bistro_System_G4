package dto;

import java.io.Serializable;

public class DeleteSubscriberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int subscriberId;

    public DeleteSubscriberDTO(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public int getSubscriberId() {
        return subscriberId;
    }
}
