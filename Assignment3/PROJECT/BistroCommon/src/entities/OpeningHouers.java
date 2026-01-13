package entities;

import java.io.Serializable; // 1. ייבוא הממשק

/**
 * Represents restaurant opening hours for a specific day.
 */
public class OpeningHouers implements Serializable { // 2. מימוש הממשק
    private static final long serialVersionUID = 1L; // 3. הוספת מזהה גרסה

    private String dayOfWeek;
    private String openTime;
    private String closeTime;

    // קונסטרקטורים, גטרים וסטרים נשארים ללא שינוי...
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}