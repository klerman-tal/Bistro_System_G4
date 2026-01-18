package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dto.DueBillDTO;
import entities.Enums;
import entities.Enums.ReservationStatus;
import entities.Reservation;

/**
 * Provides persistence operations for reservations.
 * <p>
 * This DB controller manages the {@code reservations} table and supports:
 * <ul>
 * <li>Schema creation</li>
 * <li>Creating reservations and generating DB identifiers</li>
 * <li>Canceling and finishing reservations</li>
 * <li>Updating reservation fields (status, confirmation, check-in, check-out,
 * table number)</li>
 * <li>Queries for user reservations, active reservations, history, and
 * time-based reports</li>
 * <li>Scheduler-driven queries (due reminders, due bills, auto-cancel
 * candidates)</li>
 * <li>Real-time occupancy checks based on check-in/check-out columns</li>
 * </ul>
 * </p>
 */
public class Reservation_DB_Controller {

	private final Connection conn;

	/**
	 * Constructs a Reservation_DB_Controller with the given JDBC connection.
	 *
	 * @param conn active JDBC connection used for reservation persistence
	 */
	public Reservation_DB_Controller(Connection conn) {
		this.conn = conn;
	}

	// =====================================================
	// TABLE SETUP
	// =====================================================

	/**
	 * Creates the {@code reservations} table if it does not already exist.
	 * <p>
	 * Note: {@code CREATE TABLE IF NOT EXISTS} will not add new columns to an
	 * existing table.
	 * </p>
	 */
	public void createReservationsTable() {
		String sql = """
				  CREATE TABLE IF NOT EXISTS reservations (
				      reservation_id INT AUTO_INCREMENT PRIMARY KEY,

				      reservation_datetime DATETIME NOT NULL,
				      number_of_guests INT NOT NULL,
				      confirmation_code VARCHAR(20) NOT NULL,

				      created_by INT NOT NULL,
				      created_by_role ENUM(
				          'RandomClient',
				          'Subscriber',
				          'RestaurantAgent',
				          'RestaurantManager'
				      ) NOT NULL,

				      is_confirmed TINYINT(1) NOT NULL DEFAULT 0,
				      is_active TINYINT(1) NOT NULL DEFAULT 1,
				      table_number INT NULL,

				      reservation_status ENUM(
				          'Active',
				          'Finished',
				          'Cancelled'
				      ) NOT NULL DEFAULT 'Active',

				      checkin DATETIME NULL,
				      checkout DATETIME NULL,
				      reminder_at DATETIME NULL,
				      reminder_sent TINYINT(1) NOT NULL DEFAULT 0,
				      bill_at DATETIME NULL,
				bill_sent TINYINT(1) NOT NULL DEFAULT 0,

				INDEX idx_bill_due (bill_sent, bill_at),
				      UNIQUE (confirmation_code),
				      INDEX (reservation_datetime),
				      INDEX (created_by),
				      INDEX (table_number),
				      INDEX (reservation_status),
				      INDEX checkin (checkin),
				      INDEX checkout (checkout),
				      INDEX idx_reminder_due (reminder_sent, reminder_at)
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
	 * Inserts a new reservation row and returns the generated
	 * {@code reservation_id}.
	 * <p>
	 * This insert sets:
	 * <ul>
	 * <li>{@code is_confirmed = 1}</li>
	 * <li>{@code is_active = 1}</li>
	 * <li>{@code reminder_at = reservation_datetime - 2 hours}</li>
	 * <li>{@code reminder_sent = 0}</li>
	 * </ul>
	 * </p>
	 *
	 * @param reservationDateTime reservation date/time
	 * @param guests              number of guests
	 * @param confirmationCode    unique confirmation code
	 * @param createdByUserId     user identifier who created the reservation
	 * @param createdByRole       role of the user who created the reservation
	 * @param tableNumber         assigned table number
	 * @return generated {@code reservation_id}, or {@code -1} if insertion failed
	 *         or no key was returned
	 * @throws SQLException if a database error occurs during insertion
	 */
	public int addReservation(LocalDateTime reservationDateTime, int guests, String confirmationCode,
			int createdByUserId, Enums.UserRole createdByRole, int tableNumber) throws SQLException {

		String sql = """
				INSERT INTO reservations
				(reservation_datetime,
				 number_of_guests,
				 confirmation_code,
				 created_by,
				 created_by_role,
				 is_confirmed,
				 is_active,
				 table_number,
				 reminder_at,
				 reminder_sent)
				VALUES (?, ?, ?, ?, ?, 1, 1, ?, DATE_SUB(?, INTERVAL 2 HOUR), 0);
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setObject(1, reservationDateTime);

			ps.setInt(2, guests);
			ps.setString(3, confirmationCode);
			ps.setInt(4, createdByUserId);
			ps.setString(5, createdByRole.name());
			ps.setInt(6, tableNumber);

			ps.setObject(7, reservationDateTime);

			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next())
					return rs.getInt(1);
			}
		}
		return -1;
	}

	// =====================================================
	// CANCEL
	// =====================================================

	/**
	 * Cancels an active reservation using its confirmation code.
	 * <p>
	 * The update is applied only when the reservation is currently active and has
	 * {@code reservation_status = 'Active'}.
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return {@code true} if the reservation was updated to cancelled,
	 *         {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean cancelReservationByConfirmationCode(String confirmationCode) throws SQLException {
		String sql = """
				UPDATE reservations
				SET is_active = 0,
				    reservation_status = 'Cancelled'
				WHERE confirmation_code = ?
				  AND is_active = 1
				  AND reservation_status = 'Active';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// REMINDERS
	// =====================================================

	/**
	 * Minimal DTO representing a reservation reminder due to be sent.
	 * <p>
	 * This class is used by scheduler logic to avoid loading full
	 * {@link Reservation} entities.
	 * </p>
	 */
	public static class DueReminder {
		public final int reservationId;
		public final int createdByUserId;
		public final LocalDateTime reservationDateTime;
		public final String confirmationCode;

		/**
		 * Constructs a DueReminder DTO.
		 *
		 * @param reservationId       reservation identifier
		 * @param createdByUserId     creator user identifier
		 * @param reservationDateTime reservation date/time
		 * @param confirmationCode    reservation confirmation code
		 */
		public DueReminder(int reservationId, int createdByUserId, LocalDateTime reservationDateTime,
				String confirmationCode) {
			this.reservationId = reservationId;
			this.createdByUserId = createdByUserId;
			this.reservationDateTime = reservationDateTime;
			this.confirmationCode = confirmationCode;
		}
	}

	/**
	 * Retrieves reminders that should be sent now.
	 * <p>
	 * A reminder is due when:
	 * <ul>
	 * <li>{@code reminder_at <= NOW()}</li>
	 * <li>{@code reminder_sent = 0}</li>
	 * <li>{@code is_active = 1}</li>
	 * <li>{@code reservation_status = 'Active'}</li>
	 * <li>{@code NOW() < reservation_datetime} (do not remind after the reservation
	 * time)</li>
	 * </ul>
	 * </p>
	 *
	 * @return list of due reminders (possibly empty)
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<DueReminder> getDueReminders() throws SQLException {
		String sql = """
				SELECT reservation_id, created_by, reservation_datetime, confirmation_code
				FROM reservations
				WHERE reminder_at IS NOT NULL
				  AND reminder_sent = 0
				  AND is_active = 1
				  AND reservation_status = 'Active'
				  AND reminder_at <= NOW()
				  AND NOW() < reservation_datetime
				ORDER BY reminder_at;
				""";

		ArrayList<DueReminder> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				int rid = rs.getInt("reservation_id");
				int uid = rs.getInt("created_by");
				LocalDateTime rdt = rs.getObject("reservation_datetime", LocalDateTime.class);
				String code = rs.getString("confirmation_code");

				list.add(new DueReminder(rid, uid, rdt, code));
			}
		}
		return list;
	}

	/**
	 * Marks a reminder as sent for the given reservation.
	 * <p>
	 * This update is idempotent and only affects rows where
	 * {@code reminder_sent = 0}.
	 * </p>
	 *
	 * @param reservationId reservation identifier
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean markReminderSent(int reservationId) throws SQLException {
		String sql = """
				UPDATE reservations
				SET reminder_sent = 1
				WHERE reservation_id = ?
				  AND reminder_sent = 0;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, reservationId);
			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// READ (QUERIES)
	// =====================================================

	/**
	 * Retrieves all reservations (including history), ordered by reservation
	 * date/time.
	 *
	 * @return list of reservations
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getAllReservations() throws SQLException {
		String sql = "SELECT * FROM reservations ORDER BY reservation_datetime;";
		return executeReservationListQuery(sql);
	}

	/**
	 * Retrieves all active reservations ({@code is_active = 1}), ordered by
	 * reservation date/time.
	 *
	 * @return list of active reservations
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getActiveReservations() throws SQLException {
		String sql = """
				SELECT * FROM reservations
				WHERE is_active = 1
				ORDER BY reservation_datetime;
				""";
		return executeReservationListQuery(sql);
	}

	/**
	 * Retrieves a reservation by its primary key.
	 *
	 * @param reservationId reservation identifier
	 * @return reservation if found, otherwise {@code null}
	 * @throws SQLException if a database error occurs during the query
	 */
	public Reservation getReservationById(int reservationId) throws SQLException {
		String sql = "SELECT * FROM reservations WHERE reservation_id = ?;";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, reservationId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return mapRowToReservation(rs);
			}
		}
		return null;
	}

	/**
	 * Retrieves a reservation by confirmation code.
	 *
	 * @param confirmationCode reservation confirmation code
	 * @return reservation if found, otherwise {@code null}
	 * @throws SQLException if a database error occurs during the query
	 */
	public Reservation getReservationByConfirmationCode(String confirmationCode) throws SQLException {
		String sql = "SELECT * FROM reservations WHERE confirmation_code = ?;";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return mapRowToReservation(rs);
			}
		}
		return null;
	}

	/**
	 * Retrieves reservations created by a specific user.
	 *
	 * @param userId creator user identifier
	 * @return list of reservations created by the given user
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getReservationsByUser(int userId) throws SQLException {
		String sql = """
				SELECT * FROM reservations
				WHERE created_by = ?
				ORDER BY reservation_datetime;
				""";

		ArrayList<Reservation> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToReservation(rs));
				}
			}
		}
		return list;
	}

	/**
	 * Checks whether an active reservation exists for the given confirmation code.
	 *
	 * @param confirmationCode confirmation code to check
	 * @return {@code true} if at least one active reservation exists, {@code false}
	 *         otherwise
	 * @throws SQLException if a database error occurs during the query
	 */
	public boolean isActiveReservationExists(String confirmationCode) throws SQLException {
		String sql = """
				SELECT 1
				FROM reservations
				WHERE confirmation_code = ?
				  AND is_active = 1;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, confirmationCode);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * Retrieves finished reservations for a given year and month.
	 * <p>
	 * A reservation is included when it has:
	 * <ul>
	 * <li>{@code reservation_status = 'Finished'}</li>
	 * <li>{@code checkin IS NOT NULL}</li>
	 * <li>{@code checkout IS NOT NULL}</li>
	 * </ul>
	 * </p>
	 *
	 * @param year  report year
	 * @param month report month (1-12)
	 * @return list of finished reservations
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getFinishedReservationsByMonth(int year, int month) throws SQLException {

		String sql = """
				SELECT *
				FROM reservations
				WHERE reservation_status = 'Finished'
				  AND checkin IS NOT NULL
				  AND checkout IS NOT NULL
				  AND YEAR(reservation_datetime) = ?
				  AND MONTH(reservation_datetime) = ?
				ORDER BY reservation_datetime;
				""";

		ArrayList<Reservation> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, year);
			ps.setInt(2, month);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToReservation(rs));
				}
			}
		}

		return list;
	}

	/**
	 * Checks whether there is at least one active reservation for a table within
	 * the next given number of days.
	 *
	 * @param tableNumber table identifier
	 * @param days        forward-looking window in days
	 * @return {@code true} if an active reservation exists in the window,
	 *         {@code false} otherwise
	 * @throws SQLException if a database error occurs during the query
	 */
	public boolean hasActiveReservationForTableInNextDays(int tableNumber, int days) throws SQLException {
		String sql = """
				SELECT 1
				FROM reservations
				WHERE table_number = ?
				  AND is_active = 1
				  AND reservation_status = 'Active'
				  AND reservation_datetime >= NOW()
				  AND reservation_datetime < (NOW() + INTERVAL ? DAY)
				LIMIT 1;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, tableNumber);
			ps.setInt(2, days);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * Retrieves active reservations for a table within the next given number of
	 * days.
	 *
	 * @param tableNumber table identifier
	 * @param days        forward-looking window in days
	 * @return list of matching reservations (possibly empty)
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getActiveReservationsForTableInNextDays(int tableNumber, int days)
			throws SQLException {
		String sql = """
				SELECT *
				FROM reservations
				WHERE table_number = ?
				  AND is_active = 1
				  AND reservation_status = 'Active'
				  AND reservation_datetime >= NOW()
				  AND reservation_datetime < (NOW() + INTERVAL ? DAY)
				ORDER BY reservation_datetime;
				""";

		ArrayList<Reservation> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, tableNumber);
			ps.setInt(2, days);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(mapRowToReservation(rs));
			}
		}
		return list;
	}

	/**
	 * Updates the table number assigned to an active reservation.
	 *
	 * @param reservationId  reservation identifier
	 * @param newTableNumber new table number
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateReservationTableNumber(int reservationId, int newTableNumber) throws SQLException {
		String sql = """
				UPDATE reservations
				SET table_number = ?
				WHERE reservation_id = ?
				  AND is_active = 1
				  AND reservation_status = 'Active';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, newTableNumber);
			ps.setInt(2, reservationId);
			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// UPDATE
	// =====================================================

	/**
	 * Updates the check-in time for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param checkinTime   check-in timestamp
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateCheckinTime(int reservationId, LocalDateTime checkinTime) throws SQLException {
		String sql = """
				UPDATE reservations
				SET checkin = ?
				WHERE reservation_id = ?;
				""";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setObject(1, checkinTime);
			pstmt.setInt(2, reservationId);
			return pstmt.executeUpdate() > 0;
		}
	}

	/**
	 * Updates the check-out time for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param checkoutTime  check-out timestamp
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateCheckoutTime(int reservationId, LocalDateTime checkoutTime) throws SQLException {
		String sql = """
				UPDATE reservations
				SET checkout = ?
				WHERE reservation_id = ?;
				""";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setObject(1, checkoutTime);
			pstmt.setInt(2, reservationId);
			return pstmt.executeUpdate() > 0;
		}
	}

	/**
	 * Updates {@code reservation_status} for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param status        new reservation status
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateReservationStatus(int reservationId, ReservationStatus status) throws SQLException {
		String sql = """
				UPDATE reservations
				SET reservation_status = ?
				WHERE reservation_id = ?;
				""";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, status.name());
			pstmt.setInt(2, reservationId);
			return pstmt.executeUpdate() > 0;
		}
	}

	/**
	 * Updates {@code is_confirmed} flag for a reservation.
	 *
	 * @param reservationId reservation identifier
	 * @param isConfirmed   confirmation flag value
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateIsConfirmed(int reservationId, boolean isConfirmed) throws SQLException {
		String sql = """
				UPDATE reservations
				SET is_confirmed = ?
				WHERE reservation_id = ?;
				""";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setBoolean(1, isConfirmed);
			pstmt.setInt(2, reservationId);
			return pstmt.executeUpdate() > 0;
		}
	}

	// =====================================================
	// MAPPING / HELPERS
	// =====================================================

	/**
	 * Maps a result set row into a {@link Reservation} entity.
	 *
	 * @param rs result set positioned on a valid row
	 * @return mapped reservation instance
	 * @throws SQLException if reading column values fails
	 */
	private Reservation mapRowToReservation(ResultSet rs) throws SQLException {

		Reservation r = new Reservation();

		r.setReservationId(rs.getInt("reservation_id"));
		r.setConfirmationCode(rs.getString("confirmation_code"));
		r.setGuestAmount(rs.getInt("number_of_guests"));

		LocalDateTime resTime = rs.getObject("reservation_datetime", LocalDateTime.class);
		r.setReservationTime(resTime);

		r.setCreatedByUserId(rs.getInt("created_by"));
		r.setCreatedByRole(Enums.UserRole.valueOf(rs.getString("created_by_role")));

		r.setConfirmed(rs.getInt("is_confirmed") == 1);
		r.setActive(rs.getInt("is_active") == 1);

		String status = rs.getString("reservation_status");
		if (status != null) {
			r.setReservationStatus(Enums.ReservationStatus.valueOf(status));
		}

		int tableNum = rs.getInt("table_number");
		r.setTableNumber(rs.wasNull() ? null : tableNum);

		LocalDateTime checkin = rs.getObject("checkin", LocalDateTime.class);
		if (checkin != null) {
			r.setCheckinTime(checkin);
		}

		LocalDateTime checkout = rs.getObject("checkout", LocalDateTime.class);
		if (checkout != null) {
			r.setCheckoutTime(checkout);
		}

		return r;
	}

	/**
	 * Executes a SQL query that returns reservation rows and maps each row into
	 * {@link Reservation} objects.
	 *
	 * @param sql select query returning reservation rows
	 * @return mapped list of reservations
	 * @throws SQLException if a database error occurs during query execution
	 */
	private ArrayList<Reservation> executeReservationListQuery(String sql) throws SQLException {
		ArrayList<Reservation> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(mapRowToReservation(rs));
			}
		}
		return list;
	}

	/**
	 * Marks an active reservation as finished using its confirmation code.
	 * <p>
	 * This update sets:
	 * <ul>
	 * <li>{@code reservation_status = 'Finished'}</li>
	 * <li>{@code checkout = checkoutTime}</li>
	 * <li>{@code is_active = 0}</li>
	 * </ul>
	 * and only applies to active reservations.
	 * </p>
	 *
	 * @param confirmationCode reservation confirmation code
	 * @param checkoutTime     check-out timestamp to store
	 * @return {@code true} if the reservation was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean finishReservationByConfirmationCode(String confirmationCode, LocalDateTime checkoutTime)
			throws SQLException {
		String sql = """
				UPDATE reservations
				SET reservation_status = 'Finished',
				    checkout = ?,
				    is_active = 0
				WHERE confirmation_code = ?
				  AND is_active = 1
				  AND reservation_status = 'Active';
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, checkoutTime);
			ps.setString(2, confirmationCode);
			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Finds the most recent guest confirmation code for a given reservation time
	 * among a set of guest IDs.
	 *
	 * @param reservationDateTime reservation date/time to match
	 * @param guestIds            guest identifiers to match in {@code created_by}
	 * @return confirmation code if found, otherwise {@code null}
	 * @throws SQLException if a database error occurs during the query
	 */
	public String findGuestConfirmationCodeByDateTimeAndGuestIds(java.time.LocalDateTime reservationDateTime,
			java.util.ArrayList<Integer> guestIds) throws SQLException {

		if (reservationDateTime == null || guestIds == null || guestIds.isEmpty())
			return null;

		StringBuilder in = new StringBuilder();
		for (int i = 0; i < guestIds.size(); i++) {
			if (i > 0)
				in.append(",");
			in.append("?");
		}

		String sql = "SELECT confirmation_code " + "FROM reservations " + "WHERE reservation_datetime = ? "
				+ "  AND created_by_role = 'RandomClient' " + "  AND created_by IN (" + in + ") "
				+ "ORDER BY reservation_id DESC " + "LIMIT 1;";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, reservationDateTime);

			int idx = 2;
			for (Integer id : guestIds) {
				ps.setInt(idx++, id);
			}

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getString("confirmation_code");
			}
		}

		return null;
	}

	/**
	 * Retrieves guest IDs from the {@code guests} table matching the given contact
	 * information.
	 * <p>
	 * The query filters by phone and/or email depending on which values are
	 * provided.
	 * </p>
	 *
	 * @param phone guest phone (optional)
	 * @param email guest email (optional)
	 * @return list of matching guest IDs (possibly empty)
	 * @throws SQLException if a database error occurs during the query
	 */
	public java.util.ArrayList<Integer> getGuestIdsByContact(String phone, String email) throws SQLException {

		String p = (phone == null) ? "" : phone.trim();
		String e = (email == null) ? "" : email.trim();

		StringBuilder sql = new StringBuilder("SELECT guest_id FROM GUESTS WHERE 1=1 ");
		if (!p.isBlank())
			sql.append(" AND phone = ? ");
		if (!e.isBlank())
			sql.append(" AND email = ? ");

		java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int idx = 1;
			if (!p.isBlank())
				ps.setString(idx++, p);
			if (!e.isBlank())
				ps.setString(idx++, e);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ids.add(rs.getInt("guest_id"));
				}
			}
		}

		return ids;
	}

	/**
	 * Finds a guest reservation by contact info and reservation time.
	 *
	 * @param phone    guest phone (optional)
	 * @param email    guest email (optional)
	 * @param dateTime reservation date/time to match
	 * @return matching reservation if found, otherwise {@code null}
	 * @throws SQLException if a database error occurs during the query
	 */
	public Reservation findGuestReservationByContactAndTime(String phone, String email, LocalDateTime dateTime)
			throws SQLException {

		if (dateTime == null)
			return null;

		boolean hasPhone = phone != null && !phone.isBlank();
		boolean hasEmail = email != null && !email.isBlank();
		if (!hasPhone && !hasEmail)
			return null;

		String sql = """
				SELECT r.*
				FROM reservations r
				JOIN guests g ON g.guest_id = r.created_by
				WHERE r.created_by_role = 'RandomClient'
				  AND r.reservation_datetime = ?
				  AND (
				        (? = 1 AND g.phone = ?)
				     OR (? = 1 AND g.email = ?)
				  )
				ORDER BY r.reservation_datetime DESC
				LIMIT 1;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, dateTime);

			ps.setInt(2, hasPhone ? 1 : 0);
			ps.setString(3, hasPhone ? phone : "");

			ps.setInt(4, hasEmail ? 1 : 0);
			ps.setString(5, hasEmail ? email : "");

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return mapRowToReservation(rs);
				}
			}
		}

		return null;
	}

	/**
	 * Aggregates reservation counts per day for a given creator role and month.
	 *
	 * @param role  creator role filter
	 * @param year  target year
	 * @param month target month (1-12)
	 * @return map of day-of-month to reservation count
	 * @throws SQLException if a database error occurs during the query
	 */
	public Map<Integer, Integer> getReservationsCountPerDayByRole(Enums.UserRole role, int year, int month)
			throws SQLException {

		String sql = """
				SELECT DAY(reservation_datetime) AS day, COUNT(*) AS cnt
				FROM reservations
				WHERE created_by_role = ?
				  AND YEAR(reservation_datetime) = ?
				  AND MONTH(reservation_datetime) = ?
				GROUP BY DAY(reservation_datetime)
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
	 * Updates core reservation fields based on the given {@link Reservation}
	 * entity.
	 *
	 * @param res reservation entity containing the updated values
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean updateFullReservationDetails(Reservation res) throws SQLException {
		String sql = """
				UPDATE reservations
				SET reservation_datetime = ?,
				    number_of_guests = ?,
				    table_number = ?,
				    reservation_status = ?,
				    confirmation_code = ?
				WHERE reservation_id = ?
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setTimestamp(1, Timestamp.valueOf(res.getReservationTime()));
			ps.setInt(2, res.getGuestAmount());

			if (res.getTableNumber() != null)
				ps.setInt(3, res.getTableNumber());
			else
				ps.setNull(3, Types.INTEGER);

			ps.setString(4, res.getReservationStatus().name());
			ps.setString(5, res.getConfirmationCode());
			ps.setInt(6, res.getReservationId());

			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// REAL-TIME OCCUPANCY (CHECKIN/CHECKOUT)
	// =====================================================

	/**
	 * Checks whether a table is currently occupied based on reservation
	 * check-in/check-out columns.
	 * <p>
	 * A table is considered occupied when there exists an active reservation where:
	 * <ul>
	 * <li>{@code checkin IS NOT NULL}</li>
	 * <li>{@code checkout IS NULL}</li>
	 * </ul>
	 * </p>
	 *
	 * @param tableNumber          table identifier
	 * @param excludeReservationId reservation identifier to exclude from the check
	 *                             (useful for self-check scenarios)
	 * @return {@code true} if the table is occupied, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the query
	 */
	public boolean isTableOccupiedNow(int tableNumber, int excludeReservationId) throws SQLException {
		String sql = """
				SELECT 1
				FROM reservations
				WHERE table_number = ?
				  AND is_active = 1
				  AND reservation_status = 'Active'
				  AND checkin IS NOT NULL
				  AND checkout IS NULL
				  AND reservation_id <> ?
				LIMIT 1;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, tableNumber);
			ps.setInt(2, excludeReservationId);

			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	/**
	 * Retrieves bills that are due to be sent now for currently seated diners.
	 *
	 * @return list of due bill DTOs (possibly empty)
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<DueBillDTO> getDueBills() throws SQLException {
		String sql = """
				SELECT reservation_id, created_by, confirmation_code
				FROM reservations
				WHERE bill_at IS NOT NULL
				  AND bill_sent = 0
				  AND is_active = 1
				  AND reservation_status = 'Active'
				  AND checkin IS NOT NULL
				  AND checkout IS NULL
				  AND bill_at <= NOW()
				ORDER BY bill_at;
				""";

		ArrayList<DueBillDTO> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new DueBillDTO(rs.getInt("reservation_id"), rs.getInt("created_by"),
						rs.getString("confirmation_code")));
			}
		}
		return list;
	}

	/**
	 * Retrieves reservations for diners currently in the restaurant.
	 * <p>
	 * A diner is considered present when {@code checkin IS NOT NULL} and
	 * {@code checkout IS NULL}.
	 * </p>
	 *
	 * @return list of current diners (possibly empty)
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getCurrentDiners() throws SQLException {
		String sql = """
				SELECT *
				FROM reservations
				WHERE checkin IS NOT NULL
				  AND checkout IS NULL
				ORDER BY checkin;
				""";

		return executeReservationListQuery(sql);
	}

	/**
	 * Sets the bill due timestamp for a reservation and resets {@code bill_sent} to
	 * 0.
	 *
	 * @param reservationId reservation identifier
	 * @param billAt        bill due timestamp
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean setBillDueAt(int reservationId, LocalDateTime billAt) throws SQLException {
		String sql = """
				UPDATE reservations
				SET bill_at = ?,
				    bill_sent = 0
				WHERE reservation_id = ?;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, billAt);
			ps.setInt(2, reservationId);
			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Marks a reservation bill notification as sent.
	 * <p>
	 * This update is idempotent and only affects rows where {@code bill_sent = 0}.
	 * </p>
	 *
	 * @param reservationId reservation identifier
	 * @return {@code true} if the row was updated, {@code false} otherwise
	 * @throws SQLException if a database error occurs during the update
	 */
	public boolean markBillSent(int reservationId) throws SQLException {
		String sql = """
				UPDATE reservations
				SET bill_sent = 1
				WHERE reservation_id = ?
				  AND bill_sent = 0;
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, reservationId);
			return ps.executeUpdate() > 0;
		}
	}

	// =====================================================
	// AUTO-CANCEL: reservations without check-in (15 min)
	// =====================================================

	/**
	 * Retrieves confirmation codes for active reservations that have not checked in
	 * by a given threshold.
	 * <p>
	 * Intended for auto-cancel logic where the threshold is typically
	 * {@code now.minusMinutes(15)}.
	 * </p>
	 *
	 * @param threshold upper bound timestamp; reservations at or before this time
	 *                  are considered expired
	 * @return list of confirmation codes for expired, not-checked-in reservations
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<String> getReservationsWithoutCheckinExpired(LocalDateTime threshold) throws SQLException {

		String sql = """
				SELECT confirmation_code
				FROM reservations
				WHERE is_active = 1
				  AND reservation_status = 'Active'
				  AND checkin IS NULL
				  AND reservation_datetime <= ?
				""";

		ArrayList<String> list = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setObject(1, threshold);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(rs.getString("confirmation_code"));
				}
			}
		}

		return list;
	}

	/**
	 * Retrieves active reservations by date (ignoring time-of-day).
	 *
	 * @param date target date
	 * @return list of active reservations on the given date
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getActiveReservationsByDate(LocalDate date) throws SQLException {
		ArrayList<Reservation> list = new ArrayList<>();

		String sql = """
				    SELECT * FROM reservations
				    WHERE reservation_status = 'Active'
				      AND DATE(reservation_datetime) = ?
				""";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(date));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToReservation(rs));
				}
			}
		}

		return list;
	}

	/**
	 * Retrieves active reservations created by a specific user.
	 *
	 * @param userId creator user identifier
	 * @return list of active reservations created by the user
	 * @throws SQLException if a database error occurs during the query
	 */
	public ArrayList<Reservation> getActiveReservationsByUser(int userId) throws SQLException {
		String sql = """
				SELECT *
				FROM reservations
				WHERE created_by = ?
				  AND is_active = 1
				  AND reservation_status = 'Active'
				ORDER BY reservation_datetime;
				""";

		ArrayList<Reservation> list = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapRowToReservation(rs));
				}
			}
		}
		return list;
	}
}
