package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

import entities.SpecialOpeningHours;

/**
 * Provides persistence operations for special (date-specific) opening hours overrides.
 * <p>
 * This controller manages the {@code special_opening_hours} table, which can override
 * regular weekly opening hours for a specific date. A special date can either define
 * explicit opening/closing times or mark the restaurant as closed.
 * </p>
 * <p>
 * Typical use-cases:
 * <ul>
 *   <li>Holiday hours (shorter day)</li>
 *   <li>Special events (different schedule)</li>
 *   <li>Full-day closure</li>
 * </ul>
 * </p>
 */
public class SpecialOpeningHours_DB_Controller {

    private final Connection conn;

    /**
     * Constructs a SpecialOpeningHours_DB_Controller with the given JDBC connection.
     *
     * @param conn active JDBC connection used for special opening hours persistence
     */
    public SpecialOpeningHours_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /**
     * Creates the {@code special_opening_hours} table if it does not already exist.
     * <p>
     * Schema:
     * <ul>
     *   <li>{@code special_date} (primary key)</li>
     *   <li>{@code open_time} (nullable)</li>
     *   <li>{@code close_time} (nullable)</li>
     *   <li>{@code is_closed} (boolean flag, default {@code false})</li>
     * </ul>
     * </p>
     *
     * @throws SQLException if a database error occurs during table creation
     */
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

    /**
     * Inserts or updates a special opening hours override (UPSERT) for a specific date.
     * <p>
     * Uses {@code ON DUPLICATE KEY UPDATE} so the operation is idempotent for the same {@code special_date}.
     * </p>
     *
     * @param special special opening hours entity containing date, times, and closed flag
     * @return {@code true} if the statement affected rows, otherwise {@code false}
     * @throws SQLException if a database error occurs during insert/update execution
     */
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

    /**
     * Fetches a special opening hours override for a given date.
     *
     * @param date target date
     * @return a {@link SpecialOpeningHours} instance if found; otherwise {@code null}
     * @throws SQLException if a database error occurs during query execution
     */
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

    /**
     * Returns all special opening hours overrides ordered by date ascending.
     *
     * @return list of all {@link SpecialOpeningHours} rows (possibly empty)
     * @throws SQLException if a database error occurs during query execution
     */
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
