package entities;

public class User {
	
	// ====== Fields ======
	private String phoneNumber;
	private String email;
	private Enums.UserRole userRole;
	private Reservation activeReservation;
	
	// ====== Properties ======
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Enums.UserRole getUserRole() {
		return userRole;
	}
	public void setUserRole(Enums.UserRole userRole) {
		this.userRole = userRole;
	}
	public Reservation getActiveReservation() {
		return activeReservation;
	}
	public void setActiveReservation(Reservation activeReservation) {
		this.activeReservation = activeReservation;
	}
	

}
