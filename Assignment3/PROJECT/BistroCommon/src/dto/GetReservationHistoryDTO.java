package dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) used to request the reservation history
 * of a specific subscriber.
 * <p>
 * This object is sent from the client to the server and contains
 * the subscriber identifier required to retrieve past reservations.
 * </p>
 */
public class GetReservationHistoryDTO implements Serializable {
    private final int subscriberId;

    public GetReservationHistoryDTO(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public int getSubscriberId() {
        return subscriberId;
    }
}
