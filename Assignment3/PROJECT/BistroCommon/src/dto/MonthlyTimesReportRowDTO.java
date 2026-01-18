package dto;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing a single row in a monthly timing
 * statistics report.
 * <p>
 * This object contains aggregated timing data for a specific day, including the
 * average arrival offset in minutes and the average stay duration in minutes.
 * </p>
 */
public class MonthlyTimesReportRowDTO {

	private final LocalDate day;
	private final double avgArrivalOffsetMinutes;
	private final double avgStayDurationMinutes;

	public MonthlyTimesReportRowDTO(LocalDate day, double avgArrivalOffsetMinutes, double avgStayDurationMinutes) {
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
