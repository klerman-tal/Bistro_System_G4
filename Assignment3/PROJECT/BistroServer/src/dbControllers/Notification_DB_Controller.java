package dbControllers;

import entities.Enums;
import entities.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides persistence operations for scheduled notifications (SMS / Email
 * simulation).
 * <p>
 * This DB controller manages the {@code notifications} table, supporting:
 * <ul>
 * <li>Schema creation</li>
 * <li>Insertion of scheduled notifications</li>
 * <li>Querying notifications due to be sent (unsent + scheduled time
 * reached)</li>
 * <li>Marking notifications as sent</li>
 * </ul>
 * </p>
 * <p>
 * The logic layer can use this controller as part of a scheduler/dispatcher
 * that periodically pulls due notifications and sends them through simulated
 * channels.
 * </p>
 */
public class Notification_DB_Controller {

	private final Connection conn;

	/**
	 * Constructs a Notification_DB_Controller with the given JDBC connection.
	 *
	 * @param conn active JDBC connection used for notification persistence
	 */
	public Notification_DB_Controller(Connection conn) {
		this.conn = conn;
	}

	// =====================================================
	// TABLE CREATION
	// =====================================================

	/**
	 * Creates the {@code notifications} table if it does not already exist.
	 * <p>
	 * The table stores:
	 * <ul>
	 * <li>Notification metadata (user, channel, type)</li>
	 * <li>Delivery payload (message)</li>
	 * <li>Scheduling information (scheduled_for)</li>
	 * <li>Delivery state (is_sent, sent_at)</li>
	 * </ul>
	 * </p>
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
				      reservation_id INT NULL,
				confirmation_code VARCHAR(20) NULL,

				INDEX (reservation_id),
				INDEX (confirmation_code),
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
	 * Inserts a new scheduled notification record.
	 *
	 * @param n notification to insert
	 * @return generated {@code notification_id}, or {@code -1} if insertion
	 *         succeeded but no key was returned
	 * @throws SQLException if a database error occurs during insertion
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
	 * Retrieves all notifications that are due to be sent by the given timestamp.
	 * <p>
	 * A notification is considered due when:
	 * <ul>
	 * <li>{@code is_sent = 0}</li>
	 * <li>{@code scheduled_for <= now}</li>
	 * </ul>
	 * Results are ordered by {@code scheduled_for} ascending to support
	 * chronological dispatch.
	 * </p>
	 *
	 * @param now timestamp used as the upper bound for due notifications
	 * @return list of due unsent notifications (possibly empty)
	 * @throws SQLException if a database error occurs while reading notifications
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
	 * <p>
	 * This updates:
	 * <ul>
	 * <li>{@code is_sent = 1}</li>
	 * <li>{@code sent_at = sentAt}</li>
	 * </ul>
	 * </p>
	 *
	 * @param notificationId notification identifier
	 * @param sentAt         timestamp when the notification was sent
	 * @throws SQLException if a database error occurs while updating the record
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

	/**
	 * Maps the current row of the given {@link ResultSet} into a
	 * {@link Notification} instance.
	 *
	 * @param rs result set positioned on a valid row
	 * @return mapped {@link Notification} instance
	 * @throws SQLException if reading column values fails
	 */
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

		return new Notification(id, userId, channel, type, message, scheduledFor, sent, sentAt);
	}
}
