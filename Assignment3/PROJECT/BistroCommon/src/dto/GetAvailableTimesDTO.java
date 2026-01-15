package dto;

import java.io.Serializable;
import java.time.LocalDate;

public class GetAvailableTimesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LocalDate date;
    private final int guests;

    public GetAvailableTimesDTO(LocalDate date, int guests) {
        this.date = date;
        this.guests = guests;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getGuests() {
        return guests;
    }
}
