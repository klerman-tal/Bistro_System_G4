package dbControllers;

import entities.Enums;
import entities.Notification;


import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DB Controller for scheduled notifications (SMS / Email simulation).
 */
public class Notification_DB_Controller {

    private final Connection conn;

    public Notification_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // =====================================================
    // TABLE CREATION
    // =====================================================

    /**
     * Creates the notifications table if it does not exist.
     */
    public void createNotificationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS notifications (
                notification_id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,

                channel VARCHAR(10) NOT NULL,
                notification_type VARCHAR(60) NOT NULL,

                message VARCHAR(800) NOT NULL,
                scheduled_for DATETIME NOT NULL,

                is_sent TINYINT(1) NOT NULL DEFAULT 0,
                sent_at DATETIME NULL,

                INDEX (user_id),
                INDEX (scheduled_for),
                INDEX (is_sent),
                INDEX (channel),
                INDEX (notification_type)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // INSERT
    // =====================================================

    /**
     * Inserts a new scheduled notification.
     * @return generated notification_id
     */
    public int addNotification(Notification n) throws SQLException {
        String sql = """
            INSERT INTO notifications
            (user_id, channel, notification_type, message, scheduled_for)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getChannel().name());
            ps.setString(3, n.getNotificationType().name());
            ps.setString(4, n.getMessage());
            ps.setTimestamp(5, Timestamp.valueOf(n.getScheduledFor()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    // =====================================================
    // SELECT - Scheduler use
    // =====================================================

    /**
     * Returns all unsent notifications that should be sent now.
     */
    public List<Notification> getDueUnsent(LocalDateTime now) throws SQLException {
        String sql = """
            SELECT *
            FROM notifications
            WHERE is_sent = 0
              AND scheduled_for <= ?
            ORDER BY scheduled_for ASC
            """;

        List<Notification> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    // =====================================================
    // UPDATE
    // =====================================================

    /**
     * Marks a notification as sent.
     */
    public void markAsSent(int notificationId, LocalDateTime sentAt) throws SQLException {
        String sql = """
            UPDATE notifications
            SET is_sent = 1,
                sent_at = ?
            WHERE notification_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(sentAt));
            ps.setInt(2, notificationId);
            ps.executeUpdate();
        }
    }

    // =====================================================
    // ROW MAPPING
    // =====================================================

    private Notification mapRow(ResultSet rs) throws SQLException {

        int id = rs.getInt("notification_id");
        int userId = rs.getInt("user_id");

        Enums.Channel channel = Enums.Channel.valueOf(rs.getString("channel"));
        Enums.NotificationType type = Enums.NotificationType.valueOf(rs.getString("notification_type"));

        String message = rs.getString("message");
        LocalDateTime scheduledFor = rs.getTimestamp("scheduled_for").toLocalDateTime();

        boolean sent = rs.getBoolean("is_sent");
        Timestamp sentTs = rs.getTimestamp("sent_at");
        LocalDateTime sentAt = (sentTs == null) ? null : sentTs.toLocalDateTime();

        return new Notification(
                id,
                userId,
                channel,
                type,
                message,
                scheduledFor,
                sent,
                sentAt
        );
    }
}
