package entities;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

//Singleton class
public class Restaurant {
	
	// ====== Fields ======
	private static Restaurant instance;
	private ArrayList<Table> tables;
	private Deque<Table> availableTables = new ArrayDeque<>();
	private ArrayList<Reservation> tableReservations;
	private ArrayList<Reservation> waitingList;
	private ArrayList<User> currentGuestList;
	private ArrayList<Subscriber> subscribers;
	private ArrayList<Integer> subscribersNumbers;
	private Map<Enums.Days, LocalTime> openingHours;

	// ====== Constructors ======
	private Restaurant() {
		tables = new ArrayList<>();
        availableTables = new ArrayDeque<>();
        tableReservations = new ArrayList<>();
        waitingList = new ArrayList<>();
        currentGuestList = new ArrayList<>();
        subscribers = new ArrayList<>();
        subscribersNumbers = new ArrayList<>();
        openingHours = new HashMap<>();
	}
	
	// ====== Properties ======
	public static synchronized Restaurant getInstance() {
        if (instance == null) {
            instance = new Restaurant();
        }
        return instance;
    }
	public ArrayList<Table> getTables() {
		return tables;
	}
	public void setAvailableTables(Deque<Table> availableTables) {
		this.availableTables = availableTables;
	}
	public Deque<Table> getAvailableTables() {
		return availableTables;
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
	public ArrayList<Integer> getSubscribersNumbers() {
		return subscribersNumbers;
	}
	public void setSubscribersNumbers(ArrayList<Integer> subscribersNumbers) {
		this.subscribersNumbers = subscribersNumbers;
	}
	
	// ====== Methods ======
	
	public int TableCounter() {
		
		return tables.size();
	}
	
	public int TableReservationCounter() {
		
		return tableReservations.size();
	}
	
	public Table GetAvailableTable() {

	    if (this.TableCounter() <= 0) {
	        throw new IllegalStateException("No available tables in the restaurant");
	    }

	    return availableTables.pop();
	}


}
