package entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton entity that acts as an in-memory cache for restaurant data.
 * <p>
 * This class holds collections used across the system, such as tables,
 * reservations, waiting list entries, subscribers, and opening hours. It is
 * commonly used as a shared state container between controllers and database
 * layers.
 * </p>
 */
public class Restaurant {

	// ====== Fields ======
	private static Restaurant instance;
	private ArrayList<Table> tables;

	private HashMap<LocalDateTime, Table> avelibleTableAtSpecificTimeAndDate;

	private ArrayList<Reservation> tableReservations;
	private ArrayList<Reservation> waitingList;
	private ArrayList<User> currentGuestList;
	private ArrayList<Subscriber> subscribers;
	private ArrayList<Integer> subscribersNumbers;
	private ArrayList<OpeningHouers> openingHours;

	// ====== Constructor ======

	/**
	 * Private constructor to enforce the Singleton pattern.
	 * <p>
	 * Initializes the internal collections used as cached restaurant data.
	 * </p>
	 */
	private Restaurant() {
		tables = new ArrayList<>();

		avelibleTableAtSpecificTimeAndDate = new HashMap<>();

		tableReservations = new ArrayList<>();
		waitingList = new ArrayList<>();
		currentGuestList = new ArrayList<>();
		subscribers = new ArrayList<>();
		subscribersNumbers = new ArrayList<>();
		openingHours = new ArrayList<>();
	}

	// ====== Singleton ======

	/**
	 * Returns the single Restaurant instance (thread-safe).
	 * <p>
	 * Creates the instance lazily on the first call.
	 * </p>
	 *
	 * @return the singleton Restaurant instance
	 */
	public static synchronized Restaurant getInstance() {
		if (instance == null) {
			instance = new Restaurant();
		}
		return instance;
	}

	// ====== Tables ======
	public ArrayList<Table> getTables() {
		return tables;
	}

	public void setTables(ArrayList<Table> tables) {
		this.tables = tables;
	}

	/**
	 * Returns the number of tables currently stored in the cache.
	 *
	 * @return the amount of tables, or {@code 0} if the list is {@code null}
	 */
	public int TableCounter() {
		return tables == null ? 0 : tables.size();
	}

	// ====== NEW: Availability Map ======
	public HashMap<LocalDateTime, Table> getAvelibleTableAtSpecificTimeAndDate() {
		return avelibleTableAtSpecificTimeAndDate;
	}

	public void setAvelibleTableAtSpecificTimeAndDate(HashMap<LocalDateTime, Table> map) {
		this.avelibleTableAtSpecificTimeAndDate = map;
	}

	/**
	 * Adds or updates an available table entry for the given date and time.
	 * <p>
	 * Does nothing if {@code dateTime} or {@code table} is {@code null}.
	 * </p>
	 */
	public void putAvailableTable(LocalDateTime dateTime, Table table) {
		if (dateTime == null || table == null)
			return;
		avelibleTableAtSpecificTimeAndDate.put(dateTime, table);
	}

	/**
	 * Returns the available table mapped to the given date and time.
	 *
	 * @param dateTime the date and time key
	 * @return the available table, or {@code null} if not found or if
	 *         {@code dateTime} is {@code null}
	 */
	public Table getAvailableTable(LocalDateTime dateTime) {
		if (dateTime == null)
			return null;
		return avelibleTableAtSpecificTimeAndDate.get(dateTime);
	}

	/**
	 * Removes an available table entry for the given date and time.
	 * <p>
	 * Does nothing if {@code dateTime} is {@code null}.
	 * </p>
	 */
	public void removeAvailableTable(LocalDateTime dateTime) {
		if (dateTime == null)
			return;
		avelibleTableAtSpecificTimeAndDate.remove(dateTime);
	}

	/**
	 * Clears all cached availability mappings.
	 */
	public void clearAvailabilityMap() {
		avelibleTableAtSpecificTimeAndDate.clear();
	}

	// ====== Reservations / Lists ======
	public ArrayList<Reservation> getTableReservations() {
		return tableReservations;
	}

	public void setTableReservations(ArrayList<Reservation> tableReservations) {
		this.tableReservations = tableReservations;
	}

	/**
	 * Returns the number of reservations currently stored in the cache.
	 *
	 * @return the amount of reservations, or {@code 0} if the list is {@code null}
	 */
	public int TableReservationCounter() {
		return tableReservations == null ? 0 : tableReservations.size();
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

	public ArrayList<Integer> getSubscribersNumbers() {
		return subscribersNumbers;
	}

	public void setSubscribersNumbers(ArrayList<Integer> subscribersNumbers) {
		this.subscribersNumbers = subscribersNumbers;
	}

	// ====== Opening Hours ======
	public ArrayList<OpeningHouers> getOpeningHours() {
		return openingHours;
	}

	public void setOpeningHours(ArrayList<OpeningHouers> openingHours) {
		this.openingHours = openingHours;
	}
}
