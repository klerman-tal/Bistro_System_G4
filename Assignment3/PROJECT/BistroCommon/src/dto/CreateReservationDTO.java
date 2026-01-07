package dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import entities.Enums.UserRole;

public class CreateReservationDTO implements Serializable{
	
	private LocalDate date;
	private LocalTime time;
	private int guests;
	private int userId;
	private UserRole role;
	
	public CreateReservationDTO(LocalDate date, LocalTime time, int guests, int userId, UserRole role) {
		super();
		this.date = date;
		this.time = time;
		this.guests = guests;
		this.userId = userId;
		this.role = role;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getTime() {
		return time;
	}

	public int getGuests() {
		return guests;
	}

	public int getUserId() {
		return userId;
	}

	public UserRole getRole() {
		return role;
	}
	
	

}
