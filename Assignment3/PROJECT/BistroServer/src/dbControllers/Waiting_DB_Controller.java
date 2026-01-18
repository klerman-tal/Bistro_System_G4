package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import entities.Waiting;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;

/**
 * Provides JDBC-based persistence operations for the restaurant waiting list.
 * <p>
 * This controller manages the {@code waiting_list} table, including creation,
 * insertion, cancellation, seating updates, and retrieval queries used by the
 * waiting-list flow. It also includes helper queries for end-of-day operations
 * and basic reporting.
 * </p>
 * <p>
 * Waiting-list lifecycle (high level):
 * <ul>
 * <li>Join: insert a new row with status {@code Waiting}</li>
 * <li>Table freed: update row with {@code table_freed_time} and
 * {@code table_number}</li>
 * <li>Confirm / seat: mark status {@code Seated}</li>
 * <li>Cancel: mark status {@code Cancelled} (no deletion)</li>
 * <li>Auto-cancel: cancel entries not confirmed within 15 minutes after a table
 * was freed</li>
 * </ul>
 * </p>
 */
public class Waiting_DB_Controller {

	private final Connection conn;

	/**
	 * Constructs a Waiting_DB_Controller with the given JDBC connection.
	 *
	 * @param conn active JDBC connection used for waiting-list persistence
	 */
	public Waiting_DB_Controller(Connection conn) {
		this.conn = conn;
	}

	// ===== Waiting List Table =====

	/**
	 * Creates the {@code waiting_list} table if it does not exist.
	 * <p>
	 * The table stores who created the waiting entry, number of guests, a unique
	 * confirmation code, timestamps for join and table availability, an optional
	 * table number, and a status enum.
	 * </p>
	 * <p>
	 * {@code joined_at} is automatically set by the database using
	 * {@code DEFAULT CURRENT_TIMESTAMP}.
	 * </p>
	 */
	public void createWaitingListTable() {
		String sql = """
				CREATE TABLE IF NOT EXISTS waiting_list (
				    waiting_id INT AUTO_INCREMENT PRIMARY KEY,

				    created_by INT NOT NULL,
				    created_by_role ENUM(
				        'RandomClient',
				        'Subscriber',
				        'RestaurantAgent',
				        'RestaurantManager'
				    ) NOT NULL,

				    number_of_guests INT NOT NULL,
				    confirmation_code VARCHAR(20) NOT NULL,

				    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

				    table_freed_time DATETIME NULL,
				    table_number INT NULL,

				    waiting_status ENUM(
				        'Waiting',
				        'Seated',
				        'Cancelled'
				    ) NOT NULL DEFAULT 'Waiting',

				    UNIQUE (confirmation_code),
				    INDEX (created_by),
				    INDEX (waiting_status),
				    INDEX (joined_at),
				    INDEX (table_freed_time),
				    INDEX (table_number)
				);
				""";

		try (Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves all waiting entries ordered by newest first.
	 *
	 * @return list of {@link Waiting} entries (may include
	 *         {@code Cancelled}/{@code Seated} depending on DB state)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public ArrayList<Waiting> getAllWaitings() throws SQLException {
		String sql = "SELECT * FROM waiting_list ORDER BY waiting_id DESC;";

		ArrayList<Waiting> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(mapRowToWaiting(rs));
			}
		}
		return list;
	}

	/**
	 * Inserts a new waiting entry with status {@code Waiting}.
	 * <p>
	 * {@code joined_at} is not explicitly inserted because it is set automatically
	 * by the DB.
	 * </p>
	 *
	 * @param guests           number of guests for this waiting request
	 * @param confirmationCode unique confirmation code
	 * @param userId           creator user id
	 * @param role             creator role (stored as enum name)
	 * @return generated {@code waiting_id}, or {@code -1} if insert failed
	 * @throws SQLException if a database error occurs during insert
	 */
	public int addToWaitingList(int guests, String confirmationCode, int userId, UserRole role) throws SQLException {
		String sql = """
				INSERT INTO waiting_list
				(number_of_guests, confirmation_code, created_by, created_by_role, waiting_status, table_freed_time, table_number)
				VALUES (?, ?, ?, ?, 'Waiting', NULL, NULL);
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setInt(1, guests);
			ps.setString(2, confirmationCode);
			ps.setInt(3, userId);
			ps.setString(4, role.name());

			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					return rs.getInt(1);
			}
		}
		return -1;
	}

	/**
	 * Cancels a waiting entry by confirmation code only if it is still in
	 * {@code Waiting} status.
	 * <p>
	 * Also clears {@code table_number} and {@code table_freed_time}.
	 * </p>
	 *
	 * @param confirmationCode confirmation code identifying the waiting entry
	 * @return {@code true} if a row was updated; otherwise {@code false}
	 * @throws SQLException if a database error occurs during update
	 */
	public boolean cancelWaiting(String confirmationCode) throws SQLException {
		String sql = """
				UPDATE waiting_list
				SET waiting_status = 'Cancelled',
				    table_number = NULL,
				    table_freed_time = NULL
				WHERE confirmation_code = ?
				  AND waiting_status = 'Waiting';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Marks a waiting entry as {@code Seated} by confirmation code, only if it is
	 * still {@code Waiting}.
	 *
	 * @param confirmationCode confirmation code identifying the waiting entry
	 * @return {@code true} if a row was updated; otherwise {@code false}
	 * @throws SQLException if a database error occurs during update
	 */
	public boolean markWaitingAsSeated(String confirmationCode) throws SQLException {
		String sql = """
				UPDATE waiting_list
				SET waiting_status = 'Seated'
				WHERE confirmation_code = ?
				  AND waiting_status = 'Waiting';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Updates the waiting row with the time a table was freed and the assigned
	 * table number.
	 * <p>
	 * This update is applied only while the entry is still {@code Waiting}.
	 * </p>
	 *
	 * @param confirmationCode confirmation code identifying the waiting entry
	 * @param freedTime        timestamp indicating when a table became available
	 * @param tableNumber      assigned table number (nullable)
	 * @return {@code true} if a row was updated; otherwise {@code false}
	 * @throws SQLException if a database error occurs during update
	 */
	public boolean setTableFreedForWaiting(String confirmationCode, LocalDateTime freedTime, Integer tableNumber)
			throws SQLException {

		String sql = """
				UPDATE waiting_list
				SET table_freed_time = ?,
				    table_number = ?
				WHERE confirmation_code = ?
				  AND waiting_status = 'Waiting';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(freedTime));

			if (tableNumber == null)
				ps.setNull(2, Types.INTEGER);
			else
				ps.setInt(2, tableNumber);

			ps.setString(3, confirmationCode);

			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Auto-cancels waiting entries that have a table assigned but were not
	 * confirmed within 15 minutes.
	 * <p>
	 * The caller supplies {@code now}; the method cancels any row whose
	 * {@code table_freed_time} is older than {@code now - 15 minutes}.
	 * </p>
	 *
	 * @param now current timestamp used to compute the 15-minute threshold
	 * @return number of rows updated (cancelled)
	 * @throws SQLException if a database error occurs during update
	 */
	public int cancelExpiredWaitings(LocalDateTime now) throws SQLException {
		String sql = """
				UPDATE waiting_list
				SET waiting_status = 'Cancelled',
				    table_number = NULL,
				    table_freed_time = NULL
				WHERE waiting_status = 'Waiting'
				  AND table_number IS NOT NULL
				  AND table_freed_time IS NOT NULL
				  AND table_freed_time <= ?;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(now.minusMinutes(15)));
			return ps.executeUpdate();
		}
	}

	/**
	 * Returns confirmation codes for waiting entries that are considered expired
	 * (older than 15 minutes since {@code table_freed_time}) and still
	 * {@code Waiting}.
	 * <p>
	 * This is typically used to coordinate cancellation of related reservations.
	 * </p>
	 *
	 * @param now current timestamp used to compute the 15-minute threshold
	 * @return list of expired confirmation codes (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public ArrayList<String> getExpiredWaitingCodes(LocalDateTime now) throws SQLException {
		String sql = """
				SELECT confirmation_code
				FROM waiting_list
				WHERE waiting_status = 'Waiting'
				  AND table_freed_time IS NOT NULL
				  AND table_freed_time < ?;
				""";

		ArrayList<String> codes = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(now.minusMinutes(15)));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					codes.add(rs.getString("confirmation_code"));
				}
			}
		}
		return codes;
	}

	/**
	 * Retrieves a waiting entry by confirmation code.
	 *
	 * @param confirmationCode confirmation code identifying the waiting entry
	 * @return {@link Waiting} if found; otherwise {@code null}
	 * @throws SQLException if a database error occurs during query execution
	 */
	public Waiting getWaitingByConfirmationCode(String confirmationCode) throws SQLException {
		String sql = """
				SELECT * FROM waiting_list
				WHERE confirmation_code = ?;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return mapRowToWaiting(rs);
			}
		}
		return null;
	}

	/**
	 * Returns the next waiting entry (FIFO) that can fit within a table capacity.
	 * <p>
	 * Only considers entries with:
	 * <ul>
	 * <li>{@code waiting_status = 'Waiting'}</li>
	 * <li>{@code table_freed_time IS NULL} (table not yet offered)</li>
	 * <li>{@code number_of_guests <= maxGuests}</li>
	 * </ul>
	 * </p>
	 *
	 * @param maxGuests maximum guests capacity to match
	 * @return the next eligible {@link Waiting} entry, or {@code null} if none
	 *         found
	 * @throws SQLException if a database error occurs during query execution
	 */
	public Waiting getNextWaitingForSeats(int maxGuests) throws SQLException {
		String sql = """
				SELECT * FROM waiting_list
				WHERE waiting_status = 'Waiting'
				  AND table_freed_time IS NULL
				  AND number_of_guests <= ?
				ORDER BY waiting_id
				LIMIT 1;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, maxGuests);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return mapRowToWaiting(rs);
			}
		}
		return null;
	}

	/**
	 * Retrieves active waiting entries for the given date using {@code joined_at}.
	 *
	 * @param date date to match against {@code DATE(joined_at)}
	 * @return list of waiting entries with status {@code Waiting} on that date
	 * @throws SQLException if a database error occurs during query execution
	 */
	public ArrayList<Waiting> getActiveWaitingsByDate(LocalDate date) throws SQLException {
		String sql = """
				SELECT *
				FROM waiting_list
				WHERE waiting_status = 'Waiting'
				  AND DATE(joined_at) = ?
				ORDER BY waiting_id DESC;
				""";

		ArrayList<Waiting> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(date));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToWaiting(rs));
				}
			}
		}
		return list;
	}

	/**
	 * Maps a {@link ResultSet} row from {@code waiting_list} into a {@link Waiting}
	 * entity.
	 * <p>
	 * This method performs defensive parsing for {@code waiting_status} by
	 * normalizing the database string to a matching {@link WaitingStatus} enum
	 * value.
	 * </p>
	 *
	 * @param rs result set positioned on a waiting_list row
	 * @return mapped {@link Waiting} instance
	 * @throws SQLException if a database error occurs while reading columns
	 */
	private Waiting mapRowToWaiting(ResultSet rs) throws SQLException {
		Waiting w = new Waiting();

		w.setWaitingId(rs.getInt("waiting_id"));
		w.setGuestAmount(rs.getInt("number_of_guests"));
		w.setConfirmationCode(rs.getString("confirmation_code"));
		w.setCreatedByUserId(rs.getInt("created_by"));

		String roleStr = rs.getString("created_by_role");
		if (roleStr != null) {
			w.setCreatedByRole(UserRole.valueOf(roleStr));
		}

		Timestamp freed = rs.getTimestamp("table_freed_time");
		w.setTableFreedTime(freed == null ? null : freed.toLocalDateTime());

		int tableNum = rs.getInt("table_number");
		w.setTableNumber(rs.wasNull() ? null : tableNum);

		String statusStr = rs.getString("waiting_status");
		if (statusStr != null) {
			try {
				String formattedStatus = statusStr.substring(0, 1).toUpperCase() + statusStr.substring(1).toLowerCase();
				w.setWaitingStatus(WaitingStatus.valueOf(formattedStatus));
			} catch (Exception e) {
				w.setWaitingStatus(WaitingStatus.Waiting);
			}
		}

		return w;
	}

	/**
	 * Marks a waiting entry as {@code Seated} and stores an assigned table number.
	 * <p>
	 * This method also sets {@code table_freed_time = NOW()}.
	 * </p>
	 *
	 * @param confirmationCode confirmation code identifying the waiting entry
	 * @param tableNumber      assigned table number (nullable)
	 * @return {@code true} if a row was updated; otherwise {@code false}
	 * @throws SQLException if a database error occurs during update
	 */
	public boolean markWaitingAsSeatedWithTable(String confirmationCode, Integer tableNumber) throws SQLException {
		String sql = """
				UPDATE waiting_list
				SET waiting_status = 'Seated',
				    table_freed_time = NOW(),
				    table_number = ?
				WHERE confirmation_code = ?
				  AND waiting_status = 'Waiting';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			if (tableNumber == null)
				ps.setNull(1, Types.INTEGER);
			else
				ps.setInt(1, tableNumber);

			ps.setString(2, confirmationCode);

			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// REPORTS – WAITING LIST
	// =====================================================

	/**
	 * Returns a map of day-of-month to waiting count for the given role in the
	 * given month, based on {@code table_freed_time} (i.e., entries for which a
	 * table was offered).
	 *
	 * @param role  creator role to filter by
	 * @param year  target year
	 * @param month target month (1-12)
	 * @return map where key is day of month and value is count (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public Map<Integer, Integer> getWaitingCountPerDayByRole(UserRole role, int year, int month) throws SQLException {

		String sql = """
				SELECT DAY(table_freed_time) AS day, COUNT(*) AS cnt
				FROM waiting_list
				WHERE created_by_role = ?
				  AND table_freed_time IS NOT NULL
				  AND YEAR(table_freed_time) = ?
				  AND MONTH(table_freed_time) = ?
				GROUP BY DAY(table_freed_time)
				ORDER BY day;
				""";

		Map<Integer, Integer> map = new HashMap<>();

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, role.name());
			ps.setInt(2, year);
			ps.setInt(3, month);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					map.put(rs.getInt("day"), rs.getInt("cnt"));
				}
			}
		}
		return map;
	}

	/**
	 * Retrieves waiting entries that are currently {@code Waiting} and have not
	 * been assigned a table.
	 * <p>
	 * This query uses {@code table_freed_time IS NULL} to indicate "not offered a
	 * table yet".
	 * </p>
	 *
	 * @param today unused parameter (kept to preserve the existing method
	 *              signature)
	 * @return list of matching {@link Waiting} rows (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public ArrayList<Waiting> getActiveWaitingsForToday(LocalDate today) throws SQLException {
		String sql = """
				SELECT *
				FROM waiting_list
				WHERE waiting_status = 'Waiting'
				  AND DATE(table_freed_time) IS NULL
				""";

		ArrayList<Waiting> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(mapRowToWaiting(rs));
			}
		}
		return list;
	}

	/**
	 * Retrieves active waiting entries created by a specific user.
	 *
	 * @param userId creator user id
	 * @return list of waiting entries with status {@code Waiting} for that user
	 *         (possibly empty)
	 * @throws SQLException if a database error occurs during query execution
	 */
	public ArrayList<Waiting> getActiveWaitingsByUser(int userId) throws SQLException {
		String sql = """
				SELECT *
				FROM waiting_list
				WHERE created_by = ?
				  AND waiting_status = 'Waiting'
				ORDER BY waiting_id DESC;
				""";

		ArrayList<Waiting> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToWaiting(rs));
				}
			}
		}
		return list;
	}

	/**
	 * End-of-day operation: cancels all entries still in {@code Waiting} for the
	 * given date based on {@code joined_at}.
	 *
	 * @param date date to match against {@code DATE(joined_at)}
	 * @return number of rows updated (cancelled)
	 * @throws SQLException if a database error occurs during update
	 */
	public int cancelAllWaitingsByDate(LocalDate date) throws SQLException {
		String sql = """
				UPDATE waiting_list
				SET waiting_status = 'Cancelled'
				WHERE waiting_status = 'Waiting'
				  AND DATE(joined_at) = ?;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(date));
			return ps.executeUpdate();
		}
	}

	/**
	 * Returns table numbers that are currently "locked" by the waiting flow.
	 * <p>
	 * A table is considered locked if it is assigned to a waiting entry
	 * (table_number not null) and the {@code table_freed_time} is within the last
	 * 15 minutes while the status is still {@code Waiting}.
	 * </p>
	 *
	 * @return list of distinct locked table numbers (possibly empty)
	 * @throws Exception if a database error occurs during query execution
	 */
	public ArrayList<Integer> getLockedTableNumbersNow() throws Exception {
		String sql = """
				    SELECT DISTINCT table_number
				    FROM waiting_list
				    WHERE waiting_status = 'Waiting'
				      AND table_number IS NOT NULL
				      AND table_freed_time IS NOT NULL
				      AND table_freed_time > DATE_SUB(NOW(), INTERVAL 15 MINUTE)
				""";

		ArrayList<Integer> out = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				out.add(rs.getInt(1));
		}
		return out;
	}

}
