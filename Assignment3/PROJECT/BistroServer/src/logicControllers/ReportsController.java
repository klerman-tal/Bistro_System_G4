package logicControllers;

import dbControllers.Reservation_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import dto.SubscribersReportDTO;
import dto.TimeReportDTO;
import entities.Enums.UserRole;
import entities.Reservation;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class ReportsController {

    private final Reservation_DB_Controller reservationDB;
    private final Waiting_DB_Controller waitingDB;

    public ReportsController(Reservation_DB_Controller reservationDB,
                             Waiting_DB_Controller waitingDB) {
        this.reservationDB = reservationDB;
        this.waitingDB = waitingDB;
    }

    /**
     * Builds Time Report for a given month.
     * @param year  e.g. 2025
     * @param month 1-12
     */
    public TimeReportDTO buildTimeReport(int year, int month)
            throws SQLException {

        ArrayList<Reservation> finished =
                reservationDB.getFinishedReservationsByMonth(year, month);

        TimeReportDTO dto = new TimeReportDTO();

        // =====================================================
        // TAB 1: Arrival Status
        // =====================================================

        int onTime = 0;
        int minorDelay = 0;
        int significantDelay = 0;

        for (Reservation r : finished) {

            if (r.getReservationTime() == null || r.getCheckinTime() == null) {
                continue;
            }

            long delayMinutes =
                    Duration.between(
                            r.getReservationTime(),
                            r.getCheckinTime()
                    ).toMinutes();

            if (delayMinutes < 3) {
                onTime++;
            }
            else if (delayMinutes <= 15) {
                minorDelay++;
            }
            else {
                significantDelay++;
            }
        }

        dto.setOnTimeCount(onTime);
        dto.setMinorDelayCount(minorDelay);
        dto.setSignificantDelayCount(significantDelay);

        // =====================================================
        // TAB 2: Stay Duration
        // =====================================================

        Map<Integer, List<Integer>> stayByDay = new HashMap<>();

        for (Reservation r : finished) {

            if (r.getCheckinTime() == null || r.getCheckoutTime() == null) {
                continue;
            }

            long stayMinutes =
                    Duration.between(
                            r.getCheckinTime(),
                            r.getCheckoutTime()
                    ).toMinutes();

            if (stayMinutes <= 0) continue;

            int day = r.getReservationTime().getDayOfMonth();

            stayByDay
                    .computeIfAbsent(day, d -> new ArrayList<>())
                    .add((int) stayMinutes);
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

            int avg =
                    (int) stays.stream()
                            .mapToInt(x -> x)
                            .average()
                            .orElse(0);

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

        return dto;
    }

    /**
     * Builds Subscribers Report for a given month.
     * Subscribers are identified by created_by_role = Subscriber.
     */
    public SubscribersReportDTO buildSubscribersReport(int year, int month)
            throws SQLException {

        SubscribersReportDTO dto = new SubscribersReportDTO();
        dto.setYear(year);
        dto.setMonth(month);

        // =====================================================
        // TAB 1: Active / Inactive Subscribers
        // =====================================================

        int activeSubscribers =
                reservationDB.countDistinctUsersByRoleUntil(
                        UserRole.Subscriber, year, month);

        int inactiveSubscribers =
                reservationDB.countSubscribersWithoutReservationsUntil(
                        year, month);

        dto.setActiveSubscribersCount(activeSubscribers);
        dto.setInactiveSubscribersCount(inactiveSubscribers);

        // =====================================================
        // TAB 2: Waiting List Activity (Subscribers)
        // =====================================================

        Map<Integer, Integer> waitingPerDay =
                waitingDB.getWaitingCountPerDayByRole(
                        UserRole.Subscriber, year, month);

        dto.setWaitingListPerDay(waitingPerDay);

        // =====================================================
        // TAB 3: Reservations Trend (Subscribers)
        // =====================================================

        Map<Integer, Integer> reservationsPerDay =
                reservationDB.getReservationsCountPerDayByRole(
                        UserRole.Subscriber, year, month);

        dto.setReservationsPerDay(reservationsPerDay);

        return dto;
    }
}
