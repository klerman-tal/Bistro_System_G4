package logicControllers;

import dbControllers.Reservation_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import dbControllers.User_DB_Controller;

import dto.SubscribersReportDTO;
import dto.TimeReportDTO;
import entities.Enums.UserRole;
import entities.Reservation;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

/**
 * Handles monthly report generation for the restaurant management system.
 * <p>
 * This controller aggregates data from the relevant DB controllers and builds:
 * <ul>
 *   <li>Time Report (arrival status and stay duration analytics)</li>
 *   <li>Subscribers Report (active/inactive subscribers, waiting list activity, and reservations trend)</li>
 * </ul>
 * </p>
 * <p>
 * The controller acts as a logic layer that performs computations and grouping on
 * raw database results, returning structured DTO objects for the UI layer.
 * </p>
 */
public class ReportsController {

	private final Reservation_DB_Controller reservationDB;
	private final Waiting_DB_Controller waitingDB;
	private final User_DB_Controller userDB;

	/**
	 * Constructs a ReportsController with the required database controllers.
	 *
	 * @param reservationDB database controller used to fetch reservation-related report data
	 * @param waitingDB     database controller used to fetch waiting-list report data
	 * @param userDB        database controller used to fetch subscriber-related report data
	 */
	public ReportsController(Reservation_DB_Controller reservationDB, Waiting_DB_Controller waitingDB,
			User_DB_Controller userDB) {
		this.reservationDB = reservationDB;
		this.waitingDB = waitingDB;
		this.userDB = userDB;
	}

	// =====================================================
	// TIME REPORT
	// =====================================================

	/**
	 * Builds the monthly Time Report for the given year and month.
	 * <p>
	 * The report includes:
	 * <ul>
	 *   <li><b>Arrival status</b>: counts of on-time, minor delay, and significant delay check-ins</li>
	 *   <li><b>Stay duration</b>: average stay duration per day and overall monthly summary (min/max day averages)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Only finished reservations for the given month are included. Reservations with missing
	 * relevant timestamps are skipped.
	 * </p>
	 *
	 * @param year  report year (e.g. 2025)
	 * @param month report month (1–12)
	 * @return a populated {@link TimeReportDTO} containing computed monthly analytics
	 * @throws SQLException if a database error occurs while fetching reservations
	 */
	public TimeReportDTO buildTimeReport(int year, int month) throws SQLException {

		ArrayList<Reservation> finished = reservationDB.getFinishedReservationsByMonth(year, month);

		TimeReportDTO dto = new TimeReportDTO();

		// ================= TAB 1: Arrival Status =================

		int onTime = 0;
		int minorDelay = 0;
		int significantDelay = 0;

		for (Reservation r : finished) {

			if (r.getReservationTime() == null || r.getCheckinTime() == null)
				continue;

			long delayMinutes = Duration.between(r.getReservationTime(), r.getCheckinTime()).toMinutes();

			if (delayMinutes < 3) {
				onTime++;
			} else if (delayMinutes <= 10) {
				minorDelay++;
			} else {
				significantDelay++;
			}
		}

		dto.setOnTimeCount(onTime);
		dto.setMinorDelayCount(minorDelay);
		dto.setSignificantDelayCount(significantDelay);

		// ================= TAB 2: Stay Duration =================

		Map<Integer, List<Integer>> stayByDay = new HashMap<>();

		for (Reservation r : finished) {

			if (r.getCheckinTime() == null || r.getCheckoutTime() == null)
				continue;

			long stayMinutes = Duration.between(r.getCheckinTime(), r.getCheckoutTime()).toMinutes();

			if (stayMinutes <= 0)
				continue;

			int day = r.getReservationTime().getDayOfMonth();

			stayByDay.computeIfAbsent(day, d -> new ArrayList<>()).add((int) stayMinutes);
		}

		Map<Integer, Integer> avgStayPerDay = new TreeMap<>();

		int monthlySum = 0;
		int daysCount = 0;

		int maxAvg = Integer.MIN_VALUE;
		int minAvg = Integer.MAX_VALUE;
		int maxDay = -1;
		int minDay = -1;

		for (Map.Entry<Integer, List<Integer>> entry : stayByDay.entrySet()) {

			int day = entry.getKey();
			List<Integer> stays = entry.getValue();

			int avg = (int) stays.stream().mapToInt(x -> x).average().orElse(0);

			avgStayPerDay.put(day, avg);

			monthlySum += avg;
			daysCount++;

			if (avg > maxAvg) {
				maxAvg = avg;
				maxDay = day;
			}

			if (avg < minAvg) {
				minAvg = avg;
				minDay = day;
			}
		}

		dto.setAvgStayMinutesPerDay(avgStayPerDay);

		if (daysCount > 0) {
			dto.setMonthlyAvgStay(monthlySum / daysCount);
			dto.setMaxAvgDay(maxDay);
			dto.setMaxAvgMinutes(maxAvg);
			dto.setMinAvgDay(minDay);
			dto.setMinAvgMinutes(minAvg);
		}
		
		// ================= Month-over-Month Comparison =================

		int prevYear = year;
		int prevMonth = month - 1;

		if (prevMonth == 0) {
		    prevMonth = 12;
		    prevYear--;
		}

		ArrayList<Reservation> prevFinished =
		        reservationDB.getFinishedReservationsByMonth(prevYear, prevMonth);

		if (!prevFinished.isEmpty()) {

		    int prevOnTime = 0;
		    int prevMinor = 0;
		    int prevMajor = 0;

		    for (Reservation r : prevFinished) {

		        if (r.getReservationTime() == null || r.getCheckinTime() == null)
		            continue;

		        long delayMinutes =
		                Duration.between(r.getReservationTime(), r.getCheckinTime()).toMinutes();

		        if (delayMinutes < 3) {
		            prevOnTime++;
		        } else if (delayMinutes <= 10) {
		            prevMinor++;
		        } else {
		            prevMajor++;
		        }
		    }

		    dto.setOnTimeDelta(onTime - prevOnTime);
		    dto.setMinorDelayDelta(minorDelay - prevMinor);
		    dto.setSignificantDelayDelta(significantDelay - prevMajor);
		}


		return dto;
	}

	// =====================================================
	// SUBSCRIBERS REPORT
	// =====================================================

	/**
	 * Builds the monthly Subscribers Report for the given year and month.
	 * <p>
	 * The report includes:
	 * <ul>
	 *   <li><b>Active vs. inactive subscribers</b> during the selected month</li>
	 *   <li><b>Waiting list activity</b>: number of subscriber waiting-list entries per day</li>
	 *   <li><b>Reservations trend</b>: number of subscriber reservations per day</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Subscriber-related rows are identified using {@code created_by_role = Subscriber}
	 * on the relevant database queries.
	 * </p>
	 *
	 * @param year  report year
	 * @param month report month (1–12)
	 * @return a populated {@link SubscribersReportDTO} containing computed monthly analytics
	 * @throws SQLException if a database error occurs while fetching report data
	 */
	public SubscribersReportDTO buildSubscribersReport(int year, int month) throws SQLException {

	    SubscribersReportDTO dto = new SubscribersReportDTO();
	    dto.setYear(year);
	    dto.setMonth(month);

	    // ================= TAB 1: Active / Inactive Subscribers =================

	    // TAB 1: Active / Inactive Subscribers (MONTHLY)
	    int activeSubscribers = userDB.countActiveSubscribersInMonth(year, month);
	    int inactiveSubscribers = userDB.countInactiveSubscribersInMonth(year, month);

	    dto.setActiveSubscribersCount(activeSubscribers);
	    dto.setInactiveSubscribersCount(inactiveSubscribers);

	    // ================= TAB 2: Waiting List Activity =================

	    Map<Integer, Integer> waitingPerDay =
	            waitingDB.getWaitingCountPerDayByRole(UserRole.Subscriber, year, month);

	    dto.setWaitingListPerDay(
	            waitingPerDay != null ? waitingPerDay : new HashMap<>()
	    );

	    // ================= TAB 3: Reservations Trend =================

	    Map<Integer, Integer> reservationsPerDay =
	            reservationDB.getReservationsCountPerDayByRole(
	                    UserRole.Subscriber, year, month);

	    dto.setReservationsPerDay(
	            reservationsPerDay != null ? reservationsPerDay : new HashMap<>()
	    );

	    // ================= Month-over-Month Comparison =================

	    int prevYear = year;
	    int prevMonth = month - 1;

	    if (prevMonth == 0) {
	        prevMonth = 12;
	        prevYear--;
	    }

	    int prevActive = userDB.countActiveSubscribersInMonth(prevYear, prevMonth);

	    int prevWaitingTotal =
	            waitingDB.getWaitingCountPerDayByRole(UserRole.Subscriber, prevYear, prevMonth)
	                     .values().stream().mapToInt(Integer::intValue).sum();

	    int prevReservationsTotal =
	            reservationDB.getReservationsCountPerDayByRole(
	                    UserRole.Subscriber, prevYear, prevMonth)
	                    .values().stream().mapToInt(Integer::intValue).sum();

	    int currentWaitingTotal =
	            waitingPerDay != null
	                    ? waitingPerDay.values().stream().mapToInt(Integer::intValue).sum()
	                    : 0;

	    int currentReservationsTotal =
	            reservationsPerDay != null
	                    ? reservationsPerDay.values().stream().mapToInt(Integer::intValue).sum()
	                    : 0;

	    dto.setActiveSubscribersDelta(activeSubscribers - prevActive);
	    dto.setWaitingTotalDelta(currentWaitingTotal - prevWaitingTotal);
	    dto.setReservationsTotalDelta(currentReservationsTotal - prevReservationsTotal);

	    return dto;
	}

}

