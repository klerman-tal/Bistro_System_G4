package entities;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

public class Restaurant {
	
	private ArrayList<Table> tables;
	private ArrayList<Reservation> tableReservations;
	private ArrayList<Reservation> waitingList;
	private ArrayList<User> currentGuestList;
	private ArrayList<Subscriber> subscribers;
	private Map<Enums.Days, LocalTime> openingHours;
	
	
	public ArrayList<Table> getTables() {
		return tables;
	}
	public void setTables(ArrayList<Table> tables) {
		this.tables = tables;
	}
	public ArrayList<Reservation> getTableReservations() {
		return tableReservations;
	}
	public void setTableReservations(ArrayList<Reservation> tableReservations) {
		this.tableReservations = tableReservations;
	}
	public ArrayList<Reservation> getWaitingList() {
		return waitingList;
	}
	public void setWaitingList(ArrayList<Reservation> waitingList) {
		this.waitingList = waitingList;
	}
	public ArrayList<User> getCurrentGuestList() {
		return currentGuestList;
	}
	public void setCurrentGuestList(ArrayList<User> currentGuestList) {
		this.currentGuestList = currentGuestList;
	}
	public ArrayList<Subscriber> getSubscribers() {
		return subscribers;
	}
	public void setSubscribers(ArrayList<Subscriber> subscribers) {
		this.subscribers = subscribers;
	}
	public Map<Enums.Days, LocalTime> getOpeningHours() {
		return openingHours;
	}
	public void setOpeningHours(Map<Enums.Days, LocalTime> openingHours) {
		this.openingHours = openingHours;
	}
	

}
