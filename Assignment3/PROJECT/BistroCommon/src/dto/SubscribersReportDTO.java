package dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object for Monthly Subscribers Report.
 *
 * Used to transfer aggregated subscribers-related statistics
 * for a specific month from the server to the client.
 */
public class SubscribersReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /* =====================================================
     * Request Parameters
     * ===================================================== */

    /** Report year (e.g. 2026) */
    private int year;

    /** Report month (1-12) */
    private int month;

    /* =====================================================
     * Subscribers Status Statistics (TAB 1)
     * ===================================================== */

    /** Number of active subscribers */
    private int activeSubscribersCount;

    /** Number of inactive subscribers */
    private int inactiveSubscribersCount;

    /* =====================================================
     * Waiting List Statistics (TAB 2)
     * ===================================================== */

    /**
     * Number of waiting-list entries per day.
     * Key   - day of month (1-31)
     * Value - number of waiting-list entries
     */
    private Map<Integer, Integer> waitingListPerDay;

    /* =====================================================
     * Reservations Statistics (TAB 3)
     * ===================================================== */

    /**
     * Number of reservations made by subscribers per day.
     * Key   - day of month (1-31)
     * Value - number of reservations
     */
    private Map<Integer, Integer> reservationsPerDay;

    /* =====================================================
     * Getters and Setters
     * ===================================================== */

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getActiveSubscribersCount() {
        return activeSubscribersCount;
    }

    public void setActiveSubscribersCount(int activeSubscribersCount) {
        this.activeSubscribersCount = activeSubscribersCount;
    }

    public int getInactiveSubscribersCount() {
        return inactiveSubscribersCount;
    }

    public void setInactiveSubscribersCount(int inactiveSubscribersCount) {
        this.inactiveSubscribersCount = inactiveSubscribersCount;
    }

    public Map<Integer, Integer> getWaitingListPerDay() {
        return waitingListPerDay;
    }

    public void setWaitingListPerDay(Map<Integer, Integer> waitingListPerDay) {
        this.waitingListPerDay = waitingListPerDay;
    }

    public Map<Integer, Integer> getReservationsPerDay() {
        return reservationsPerDay;
    }

    public void setReservationsPerDay(Map<Integer, Integer> reservationsPerDay) {
        this.reservationsPerDay = reservationsPerDay;
    }
}
