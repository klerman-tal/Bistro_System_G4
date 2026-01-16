package dto;

import java.io.Serializable;

public class GetMyActiveReservationsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;

    public GetMyActiveReservationsDTO(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
