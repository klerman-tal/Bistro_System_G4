package dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RecoverGuestConfirmationCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String phone;
    private final String email;
    private final LocalDateTime reservationDateTime;

    public RecoverGuestConfirmationCodeDTO(String phone, String email, LocalDateTime reservationDateTime) {
        this.phone = phone;
        this.email = email;
        this.reservationDateTime = reservationDateTime;
    }

    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDateTime getReservationDateTime() { return reservationDateTime; }
}
