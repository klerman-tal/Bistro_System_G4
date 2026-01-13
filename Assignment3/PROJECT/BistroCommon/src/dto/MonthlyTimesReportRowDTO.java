package dto;

import java.time.LocalDate;

public class MonthlyTimesReportRowDTO {

    private final LocalDate day;
    private final double avgArrivalOffsetMinutes;
    private final double avgStayDurationMinutes;

    public MonthlyTimesReportRowDTO(LocalDate day,
                                    double avgArrivalOffsetMinutes,
                                    double avgStayDurationMinutes) {
        this.day = day;
        this.avgArrivalOffsetMinutes = avgArrivalOffsetMinutes;
        this.avgStayDurationMinutes = avgStayDurationMinutes;
    }

    public LocalDate getDay() {
        return day;
    }

    public double getAvgArrivalOffsetMinutes() {
        return avgArrivalOffsetMinutes;
    }

    public double getAvgStayDurationMinutes() {
        return avgStayDurationMinutes;
    }
}
