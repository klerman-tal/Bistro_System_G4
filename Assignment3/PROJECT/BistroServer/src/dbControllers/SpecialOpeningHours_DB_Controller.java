package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

import entities.SpecialOpeningHours;

public class SpecialOpeningHours_DB_Controller {

    private final Connection conn;

    public SpecialOpeningHours_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    public void createSpecialOpeningHoursTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS special_opening_hours (
                special_date DATE PRIMARY KEY,
                open_time TIME NULL,
                close_time TIME NULL,
                is_closed BOOLEAN NOT NULL DEFAULT FALSE
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public boolean upsertSpecialHours(SpecialOpeningHours special) throws SQLException {
        createSpecialOpeningHoursTable();

        String sql = """
            INSERT INTO special_opening_hours (special_date, open_time, close_time, is_closed)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                open_time  = VALUES(open_time),
                close_time = VALUES(close_time),
                is_closed  = VALUES(is_closed)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(special.getSpecialDate()));
            ps.setTime(2, special.getOpenTime());
            ps.setTime(3, special.getCloseTime());
            ps.setBoolean(4, special.isClosed());
            return ps.executeUpdate() > 0;
        }
    }

    public SpecialOpeningHours getSpecialHoursByDate(LocalDate date) throws SQLException {
        createSpecialOpeningHoursTable();

        String sql = "SELECT * FROM special_opening_hours WHERE special_date = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new SpecialOpeningHours(
                        rs.getDate("special_date").toLocalDate(),
                        rs.getTime("open_time"),
                        rs.getTime("close_time"),
                        rs.getBoolean("is_closed")
                );
            }
        }
    }

    public ArrayList<SpecialOpeningHours> getAllSpecialOpeningHours() throws SQLException {
        createSpecialOpeningHoursTable();

        String sql = "SELECT * FROM special_opening_hours ORDER BY special_date";

        ArrayList<SpecialOpeningHours> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new SpecialOpeningHours(
                        rs.getDate("special_date").toLocalDate(),
                        rs.getTime("open_time"),
                        rs.getTime("close_time"),
                        rs.getBoolean("is_closed")
                ));
            }
        }
        return list;
    }
}
