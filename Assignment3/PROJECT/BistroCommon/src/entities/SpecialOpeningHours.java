package entities;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;

/**
 * Entity representing special opening hours for a specific calendar date.
 * <p>
 * This class is used to override regular opening hours on special dates,
 * such as holidays, events, or exceptional closures.
 * </p>
 */
public class SpecialOpeningHours implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate specialDate;
    private Time openTime;
    private Time closeTime;
    private boolean isClosed;

    /**
     * Constructs a special opening hours entry for a specific date.
     * <p>
     * This constructor allows defining custom opening and closing times,
     * or marking the date as fully closed.
     * </p>
     */
    public SpecialOpeningHours(LocalDate specialDate, Time openTime, Time closeTime, boolean isClosed) {
        this.specialDate = specialDate;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }

    public LocalDate getSpecialDate() { return specialDate; }
    public Time getOpenTime() { return openTime; }
    public Time getCloseTime() { return closeTime; }
    public boolean isClosed() { return isClosed; }
}
