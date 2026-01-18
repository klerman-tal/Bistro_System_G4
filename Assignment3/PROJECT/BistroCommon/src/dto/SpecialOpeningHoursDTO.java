package dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.sql.Time;

/**
 * Data Transfer Object (DTO) used to transfer special opening hours data
 * between the client and the server.
 * <p>
 * This object contains details about the special date, opening time, closing
 * time, and whether the restaurant is closed on that date.
 * </p>
 * Implements Serializable to allow network transfer.
 */
public class SpecialOpeningHoursDTO implements Serializable {
	private static final long serialVersionUID = 1L;

	private LocalDate specialDate;
	private Time openTime;
	private Time closeTime;
	private boolean isClosed;

	public SpecialOpeningHoursDTO(LocalDate specialDate, Time openTime, Time closeTime, boolean isClosed) {
		this.specialDate = specialDate;
		this.openTime = openTime;
		this.closeTime = closeTime;
		this.isClosed = isClosed;
	}

	// Getters and Setters
	public LocalDate getSpecialDate() {
		return specialDate;
	}

	public void setSpecialDate(LocalDate specialDate) {
		this.specialDate = specialDate;
	}

	public Time getOpenTime() {
		return openTime;
	}

	public void setOpenTime(Time openTime) {
		this.openTime = openTime;
	}

	public Time getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(Time closeTime) {
		this.closeTime = closeTime;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean closed) {
		isClosed = closed;
	}
}