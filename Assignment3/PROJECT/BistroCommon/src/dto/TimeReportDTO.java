package dto;

import java.io.Serializable;
import java.util.Map;

public class TimeReportDTO implements Serializable {

    // ===== Request fields =====
    private int year;
    private int month;

    // ===== Arrival Status (TAB 1) =====
    private int onTimeCount;
    private int minorDelayCount;
    private int significantDelayCount;

    // ===== Stay Duration (TAB 2) =====

    // key = day of month (1–31), value = avg stay minutes
    private Map<Integer, Integer> avgStayMinutesPerDay;

    private int monthlyAvgStay;

    private int maxAvgDay;
    private int maxAvgMinutes;

    private int minAvgDay;
    private int minAvgMinutes;

    // ===== getters / setters =====

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
