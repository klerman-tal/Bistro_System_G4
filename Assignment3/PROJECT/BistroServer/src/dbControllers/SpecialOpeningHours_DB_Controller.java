package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import entities.SpecialOpeningHours;

public class SpecialOpeningHours_DB_Controller {
    private Connection conn;

    public SpecialOpeningHours_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // יצירת הטבלה אם היא לא קיימת (כמו שאר הטבלאות בשרת שלך)
    public void createSpecialOpeningHoursTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS special_opening_hours (" +
                     "special_date DATE PRIMARY KEY, " +
                     "open_time TIME, " +
                     "close_time TIME, " +
                     "is_closed BOOLEAN DEFAULT FALSE)";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public boolean updateSpecialHours(SpecialOpeningHours special) {
        String sql = "INSERT INTO special_opening_hours (special_date, open_time, close_time, is_closed) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE open_time = VALUES(open_time), " +
                     "close_time = VALUES(close_time), is_closed = VALUES(is_closed)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(special.getSpecialDate()));
            pstmt.setTime(2, special.getOpenTime());
            pstmt.setTime(3, special.getCloseTime());
            pstmt.setBoolean(4, special.isClosed());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}