package logicControllers;

import dto.MonthlyTimesReportRowDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportsController {

    private final Connection conn;

    public ReportsController(Connection conn) {
        this.conn = conn;
    }

    /**
     * Returns monthly report of arrival delays and stay duration
     * for all customers.
     */
    public List<MonthlyTimesReportRowDTO> getMonthlyTimesReport(int year, int month)
            throws SQLException {

        String sql = """
            SELECT
                DATE(reservation_datetime) AS day,
                AVG(TIMESTAMPDIFF(MINUTE, reservation_datetime, checkin)) AS avg_arrival_offset,
                AVG(TIMESTAMPDIFF(MINUTE, checkin, checkout)) AS avg_stay_duration
            FROM reservations
            WHERE
                reservation_status = 'Finished'
                AND checkin IS NOT NULL
                AND checkout IS NOT NULL
                AND YEAR(reservation_datetime) = ?
                AND MONTH(reservation_datetime) = ?
            GROUP BY DATE(reservation_datetime)
            ORDER BY day;
        """;

        List<MonthlyTimesReportRowDTO> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate day = rs.getDate("day").toLocalDate();
                    double avgArrival = rs.getDouble("avg_arrival_offset");
                    double avgStay = rs.getDouble("avg_stay_duration");

                    result.add(new MonthlyTimesReportRowDTO(
                            day,
                            avgArrival,
                            avgStay
                    ));
                }
            }
        }

        return result;
    }
}
