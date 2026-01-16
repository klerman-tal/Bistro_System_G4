package entities;

import java.io.Serializable;

/**
 * Entity representing the opening hours of the restaurant for a specific day.
 * <p>
 * This class stores the day of the week along with the opening and closing
 * times, and is typically used for configuration, display, and persistence
 * of restaurant operating hours.
 * </p>
 */
public class OpeningHouers implements Serializable {
    private static final long serialVersionUID = 1L;

    private String dayOfWeek;
    private String openTime;
    private String closeTime;

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}
