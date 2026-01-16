package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to transfer subscriber deletion data
 * between the client and the server.
 * <p>
 * This object contains the unique ID of the subscriber to be deleted.
 * </p>
 */
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