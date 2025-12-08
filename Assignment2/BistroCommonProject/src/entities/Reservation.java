package entities;

import java.time.LocalDateTime;

public class Reservation {

	private int confarmationCode;
	private User createdBy;
	private LocalDateTime reservationTime;
	private int guestAmount;
	private boolean isConfirmed;
	
	
	public int getConfarmationCode() {
		return confarmationCode;
	}
	public void setConfarmationCode(int confarmationCode) {
		this.confarmationCode = confarmationCode;
	}
	public User getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
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
	
}
