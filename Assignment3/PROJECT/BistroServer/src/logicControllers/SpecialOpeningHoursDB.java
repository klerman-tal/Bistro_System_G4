package logicControllers;

import entities.SpecialOpeningHours;
import java.sql.*;
import java.time.LocalDate;

public class SpecialOpeningHoursDB {

    private Connection conn;

    public SpecialOpeningHoursDB(Connection conn) {
        this.conn = conn;
    }

    /**
     * מעדכן או מוסיף החרגת שעות לתאריך ספציפי.
     * משתמש ב-ON DUPLICATE KEY UPDATE כדי לחסוך בדיקה כפולה.
     */
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

    /**
     * שליפת החרגה לפי תאריך. 
     * ישמש אותנו גם במסך ההזמנות כדי לבדוק אם יש שעות מיוחדות.
     */
    public SpecialOpeningHours getSpecialHoursByDate(LocalDate date) {
        String sql = "SELECT * FROM special_opening_hours WHERE special_date = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new SpecialOpeningHours(
                    rs.getDate("special_date").toLocalDate(),
                    rs.getTime("open_time"),
                    rs.getTime("close_time"),
                    rs.getBoolean("is_closed")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // אין החרגה לתאריך זה
    }
}