package dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object for Monthly Time Report.
 *
 * Used to transfer aggregated arrival and stay-duration statistics
 * for a specific month from the server to the client.
 */
public class TimeReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /* =====================================================
     * Request Parameters
     * ===================================================== */

    /** Report year (e.g. 2026) */
    private int year;

    /** Report month (1-12) */
    private int month;

    /* =====================================================
     * Arrival Status Statistics (TAB 1)
     * ===================================================== */

    /** Number of on-time arrivals */
    private int onTimeCount;

    /** Number of arrivals with minor delay */
    private int minorDelayCount;

    /** Number of arrivals with significant delay */
    private int significantDelayCount;

    /* =====================================================
     * Stay Duration Statistics (TAB 2)
     * ===================================================== */

    /**
     * Average stay duration per day.
     * Key   - day of month (1-31)
     * Value - average stay duration in minutes
     */
    private Map<Integer, Integer> avgStayMinutesPerDay;

    /** Monthly average stay duration (minutes) */
    private int monthlyAvgStay;

    /** Day with the highest average stay duration */
    private int maxAvgDay;

    /** Highest average stay duration (minutes) */
    private int maxAvgMinutes;

    /** Day with the lowest average stay duration */
    private int minAvgDay;

    /** Lowest average stay duration (minutes) */
    private int minAvgMinutes;

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

    public int getOnTimeCount() {
        return onTimeCount;
    }

    public void setOnTimeCount(int onTimeCount) {
        this.onTimeCount = onTimeCount;
    }

    public int getMinorDelayCount() {
        return minorDelayCount;
    }

    public void setMinorDelayCount(int minorDelayCount) {
        this.minorDelayCount = minorDelayCount;
    }

    public int getSignificantDelayCount() {
        return significantDelayCount;
    }

    public void setSignificantDelayCount(int significantDelayCount) {
        this.significantDelayCount = significantDelayCount;
    }

    public Map<Integer, Integer> getAvgStayMinutesPerDay() {
        return avgStayMinutesPerDay;
    }

    public void setAvgStayMinutesPerDay(Map<Integer, Integer> avgStayMinutesPerDay) {
        this.avgStayMinutesPerDay = avgStayMinutesPerDay;
    }

    public int getMonthlyAvgStay() {
        return monthlyAvgStay;
    }

    public void setMonthlyAvgStay(int monthlyAvgStay) {
        this.monthlyAvgStay = monthlyAvgStay;
    }

    public int getMaxAvgDay() {
        return maxAvgDay;
    }

    public void setMaxAvgDay(int maxAvgDay) {
        this.maxAvgDay = maxAvgDay;
    }

    public int getMaxAvgMinutes() {
        return maxAvgMinutes;
    }

    public void setMaxAvgMinutes(int maxAvgMinutes) {
        this.maxAvgMinutes = maxAvgMinutes;
    }

    public int getMinAvgDay() {
        return minAvgDay;
    }

    public void setMinAvgDay(int minAvgDay) {
        this.minAvgDay = minAvgDay;
    }

    public int getMinAvgMinutes() {
        return minAvgMinutes;
    }

    public void setMinAvgMinutes(int minAvgMinutes) {
        this.minAvgMinutes = minAvgMinutes;
    }
}
