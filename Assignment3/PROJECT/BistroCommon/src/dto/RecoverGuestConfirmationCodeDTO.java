package dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) used to recover a guest's confirmation code
 * between the client and the server.
 * <p>
 * This object contains the phone number, email, and reservation date and time
 * of the guest.
 * </p>
 */
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