package entities;

import java.time.LocalDateTime;
import java.util.Random;

import entities.Enums.ReservationStatus;

public class Reservation {

    // ===== Fields =====
	private int reservationId;
    private String confirmationCode;
    private int createdByUserId;
    private LocalDateTime reservationTime;
    private int guestAmount;
    private boolean isConfirmed;
    private ReservationStatus reservationStatus;

    // ===== Constructor =====
    public Reservation() {
        confirmationCode = createConfirmationCode();
        createdByUserId = -1;
        reservationTime = null;
        guestAmount = 0;
        isConfirmed = false;
        reservationStatus = ReservationStatus.Active;
    }

    // ===== Getters / Setters =====
    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confarmationCode) {
        this.confirmationCode = confarmationCode;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public int getGuestAmount() {
        return guestAmount;
    }

    public void setGuestAmount(int guestAmount) {
        this.guestAmount = guestAmount;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }
    
    public ReservationStatus getReservationStatus() {
		return reservationStatus;
	}

	public void setReservationStatus(ReservationStatus reservationStatus) {
		this.reservationStatus = reservationStatus;
	}

    // ===== Utils =====
    private String createConfirmationCode() {
        int code = new Random().nextInt(900000) + 100000;
        return String.valueOf(code);
    }

	
}
