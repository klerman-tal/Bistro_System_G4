package entities;

import java.util.ArrayList;

public class Subscriber extends User {
	
	private int subscriberNumber;
	private String userName;
	private String personalDetails;
	private ArrayList<Reservation> reservationHistory;
	
	
	public int getSubscriberNumber() {
		return subscriberNumber;
	}
	public void setSubscriberNumber(int subscriberNumber) {
		this.subscriberNumber = subscriberNumber;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPersonalDetails() {
		return personalDetails;
	}
	public void setPersonalDetails(String personalDetails) {
		this.personalDetails = personalDetails;
	}
	public ArrayList<Reservation> getReservationHistory() {
		return reservationHistory;
	}
	public void setReservationHistory(ArrayList<Reservation> reservationHistory) {
		this.reservationHistory = reservationHistory;
	}

}
