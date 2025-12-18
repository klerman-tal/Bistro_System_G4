package entities;

import java.util.ArrayList;

public class Subscriber {

    // ====== Fields ======
    private int subscriberNumber;
    private String userName;
    private String personalDetails;
    private ArrayList<Reservation> reservationHistory;

    // ====== Constructor ======
    public Subscriber(int subscriberNumber, String userName,String personalDetails) {
                     

        this.subscriberNumber = subscriberNumber;
        this.userName = userName;
        this.personalDetails = personalDetails;
        this.reservationHistory = new ArrayList<>();
    }

    // ====== Getters ======
    public int getSubscriberNumber() {
        return subscriberNumber;
    }

    public String getUserName() {
        return userName;
    }

    public String getPersonalDetails() {
        return personalDetails;
    }

    public ArrayList<Reservation> getReservationHistory() {
        return reservationHistory;
    }
    
    public void setPersonalDetails(String personalDetails) {
        this.personalDetails = personalDetails;
    }
    



    
}
