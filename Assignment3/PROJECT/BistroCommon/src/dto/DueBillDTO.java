package dto;

import java.io.Serializable;

public class DueBillDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int reservationId;
    private final int userId;
    private final String confirmationCode;

    public DueBillDTO(int reservationId, int userId, String confirmationCode) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.confirmationCode = confirmationCode;
    }

    public int getReservationId() { return reservationId; }
    public int getUserId() { return userId; }
    public String getConfirmationCode() { return confirmationCode; }
}
