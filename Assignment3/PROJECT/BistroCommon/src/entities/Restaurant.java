package entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Singleton class
public class Restaurant {

    // ====== Fields ======
    private static Restaurant instance;
    private int globalUserCounter = 0;
    private ArrayList<Table> tables;

    private HashMap<LocalDateTime, Table> avelibleTableAtSpecificTimeAndDate;

    private ArrayList<Reservation> tableReservations;
    private ArrayList<Reservation> waitingList;
    private ArrayList<User> currentGuestList;
    private ArrayList<Subscriber> subscribers;
    private ArrayList<Integer> subscribersNumbers;
    private ArrayList<OpeningHouers> openingHours;

    // ====== Constructor ======
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
    public static synchronized Restaurant getInstance() {
        if (instance == null) {
            instance = new Restaurant();
        }
        return instance;
    }
    //=======Lior=============
  

    public synchronized int getNextUserId() {
        return ++globalUserCounter;
    }
    
    
    

    // ====== Tables ======
    public ArrayList<Table> getTables() {
        return tables;
    }

    public void setTables(ArrayList<Table> tables) {
        this.tables = tables;
    }

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

    // Optional helpers (נוח לשימוש)
    public void putAvailableTable(LocalDateTime dateTime, Table table) {
        if (dateTime == null || table == null) return;
        avelibleTableAtSpecificTimeAndDate.put(dateTime, table);
    }

    public Table getAvailableTable(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return avelibleTableAtSpecificTimeAndDate.get(dateTime);
    }

    public void removeAvailableTable(LocalDateTime dateTime) {
        if (dateTime == null) return;
        avelibleTableAtSpecificTimeAndDate.remove(dateTime);
    }

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