package entities;

public class User {
	
	// ====== Fields ======
	private Integer userId;
	private String phoneNumber;
	private String userName;
	private String password;
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
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	

}
