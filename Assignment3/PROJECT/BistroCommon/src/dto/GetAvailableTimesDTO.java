package dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) used to request available reservation times from
 * the server.
 * <p>
 * This object contains the desired reservation date and the number of guests.
 * </p>
 */
public class GetAvailableTimesDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private final LocalDate date;
	private final int guests;

	public GetAvailableTimesDTO(LocalDate date, int guests) {
		this.date = date;
		this.guests = guests;
	}

	public LocalDate getDate() {
		return date;
	}

	public int getGuests() {
		return guests;
	}
}