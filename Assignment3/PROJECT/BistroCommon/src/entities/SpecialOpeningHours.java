package entities;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;

public class SpecialOpeningHours implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate specialDate;
    private Time openTime;
    private Time closeTime;
    private boolean isClosed;

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