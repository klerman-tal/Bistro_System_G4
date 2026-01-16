package dto;

import java.io.Serializable;

/**
 * DTO for updating opening hours of a specific day.
 * openTime / closeTime can be null -> means CLOSED.
 */
public class UpdateOpeningHoursDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dayOfWeek;
    private String openTime;   // "HH:MM" or null
    private String closeTime;  // "HH:MM" or null

    public UpdateOpeningHoursDTO(String dayOfWeek, String openTime, String closeTime) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }
}