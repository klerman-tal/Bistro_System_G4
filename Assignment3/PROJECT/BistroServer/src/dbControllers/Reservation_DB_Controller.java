package dbControllers;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import entities.Enums;
import entities.Enums.ReservationStatus;
import entities.Reservation;

public class Reservation_DB_Controller {

    private final Connection conn;

    /**
     * Constructor: stores DB connection reference.
     */
    public Reservation_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // =====================================================
    // TABLE SETUP
    // =====================================================

    /**
     * Creates the reservations table if it does not exist (safe create).
     * Note: CREATE TABLE IF NOT EXISTS will NOT add new columns to an existing table.
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
     * Inserts a new reservation row and returns the generated reservation_id.
     * Also sets reminder_at automatically to 2 hours before reservation_datetime.
     */
    public int addReservation(
            LocalDateTime reservationDateTime,
            int guests,
            String confirmationCode,
            int createdByUserId,
            Enums.UserRole createdByRole,
            int tableNumber) throws SQLException {

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
            Timestamp ts = Timestamp.valueOf(reservationDateTime);

            ps.setTimestamp(1, ts);
            ps.setInt(2, guests);
            ps.setString(3, confirmationCode);
            ps.setInt(4, createdByUserId);
            ps.setString(5, createdByRole.name());
            ps.setInt(6, tableNumber);

            // reminder_at derived from reservation_datetime
            ps.setTimestamp(7, ts);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }
    
    // =====================================================
    // CANCEL
    // =====================================================

    
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
     * DTO for reminders that are due to be sent (minimal data for scheduler/notifications).
     */
    public static class DueReminder {
        public final int reservationId;
        public final int createdByUserId;
        public final LocalDateTime reservationDateTime;
        public final String confirmationCode;

        /**
         * Creates a DueReminder DTO.
         */
        public DueReminder(int reservationId, int createdByUserId,
                           LocalDateTime reservationDateTime, String confirmationCode) {
            this.reservationId = reservationId;
            this.createdByUserId = createdByUserId;
            this.reservationDateTime = reservationDateTime;
            this.confirmationCode = confirmationCode;
        }
    }

    /**
     * Returns reminders that should be sent now (reminder_at <= NOW) for active reservations.
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
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int rid = rs.getInt("reservation_id");
                int uid = rs.getInt("created_by");
                Timestamp ts = rs.getTimestamp("reservation_datetime");
                LocalDateTime rdt = (ts != null) ? ts.toLocalDateTime() : null;
                String code = rs.getString("confirmation_code");

                list.add(new DueReminder(rid, uid, rdt, code));
            }
        }
        return list;
    }

    /**
     * Marks the reminder as sent for the given reservation_id (idempotent).
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
     * Returns all reservations (history), ordered by reservation_datetime.
     */
    public ArrayList<Reservation> getAllReservations() throws SQLException {
        String sql = "SELECT * FROM reservations ORDER BY reservation_datetime;";
        return executeReservationListQuery(sql);
    }

    /**
     * Returns reservations that are currently active in the system (is_active=1).
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
     * Returns a reservation by its DB primary key (reservation_id), or null if not found.
     */
    public Reservation getReservationById(int reservationId) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE reservation_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToReservation(rs);
            }
        }
        return null;
    }

    /**
     * Returns a reservation by confirmation code, or null if not found.
     */
    public Reservation getReservationByConfirmationCode(String confirmationCode) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE confirmation_code = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToReservation(rs);
            }
        }
        return null;
    }

    /**
     * Returns all reservations created by a specific user (created_by=userId).
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
     * Checks if an active reservation exists for the given confirmation code (is_active=1).
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
     * Returns reservations that were completed (have checkout)
     * for a specific year and month.
     */
    public ArrayList<Reservation> getFinishedReservationsByMonth(int year, int month)
            throws SQLException {

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



    // =====================================================
    // UPDATE
    // =====================================================


    /**
     * Updates the check-in time for a reservation_id.
     */
    public boolean updateCheckinTime(int reservationId, LocalDateTime checkinTime) throws SQLException {
        String sql = """
            UPDATE reservations
            SET checkin = ?
            WHERE reservation_id = ?;
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(checkinTime));
            pstmt.setInt(2, reservationId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates the check-out time for a reservation_id.
     */
    public boolean updateCheckoutTime(int reservationId, LocalDateTime checkoutTime) throws SQLException {
        String sql = """
            UPDATE reservations
            SET checkout = ?
            WHERE reservation_id = ?;
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(checkoutTime));
            pstmt.setInt(2, reservationId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates reservation_status for a reservation_id.
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
     * Updates is_confirmed flag for a reservation_id.
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
     * Maps a ResultSet row to a Reservation entity (partial mapping for current project needs).
     */
    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {

        Reservation r = new Reservation();

        r.setReservationId(rs.getInt("reservation_id"));
        r.setConfirmationCode(rs.getString("confirmation_code"));
        r.setGuestAmount(rs.getInt("number_of_guests"));

        r.setReservationTime(
                rs.getTimestamp("reservation_datetime").toLocalDateTime()
        );

        r.setCreatedByUserId(rs.getInt("created_by"));
        r.setCreatedByRole(
                Enums.UserRole.valueOf(rs.getString("created_by_role"))
        );

        r.setConfirmed(rs.getInt("is_confirmed") == 1);
        r.setActive(rs.getInt("is_active") == 1);

        String status = rs.getString("reservation_status");
        if (status != null) {
            r.setReservationStatus(
                    Enums.ReservationStatus.valueOf(status)
            );
        }

        int tableNum = rs.getInt("table_number");
        r.setTableNumber(rs.wasNull() ? null : tableNum);

        // ===== ✅ CHECK-IN / CHECK-OUT MAPPING =====
        Timestamp checkinTs = rs.getTimestamp("checkin");
        if (checkinTs != null) {
            r.setCheckinTime(checkinTs.toLocalDateTime());
        }

        Timestamp checkoutTs = rs.getTimestamp("checkout");
        if (checkoutTs != null) {
            r.setCheckoutTime(checkoutTs.toLocalDateTime());
        }

        return r;
    }



    /**
     * Executes a SELECT query that returns a list of reservations and maps each row.
     */
    private ArrayList<Reservation> executeReservationListQuery(String sql) throws SQLException {
        ArrayList<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToReservation(rs));
            }
        }
        return list;
    }
    
    public boolean finishReservationByConfirmationCode(String confirmationCode, LocalDateTime checkoutTime) throws SQLException {
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
            ps.setTimestamp(1, Timestamp.valueOf(checkoutTime));
            ps.setString(2, confirmationCode);
            return ps.executeUpdate() > 0;
        }
    }


    public String findGuestConfirmationCodeByDateTimeAndGuestIds(
            java.time.LocalDateTime reservationDateTime,
            java.util.ArrayList<Integer> guestIds) throws SQLException {

        if (reservationDateTime == null || guestIds == null || guestIds.isEmpty())
            return null;

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < guestIds.size(); i++) {
            if (i > 0) in.append(",");
            in.append("?");
        }

        String sql =
            "SELECT confirmation_code " +
            "FROM reservations " +
            "WHERE reservation_datetime = ? " +
            "  AND created_by_role = 'RandomClient' " +
            "  AND created_by IN (" + in + ") " +
            "ORDER BY reservation_id DESC " +
            "LIMIT 1;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(reservationDateTime));

            int idx = 2;
            for (Integer id : guestIds) {
                ps.setInt(idx++, id);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("confirmation_code");
            }
        }

        return null;
    }



    public java.util.ArrayList<Integer> getGuestIdsByContact(String phone, String email) throws SQLException {

        String p = (phone == null) ? "" : phone.trim();
        String e = (email == null) ? "" : email.trim();

        StringBuilder sql = new StringBuilder("SELECT guest_id FROM GUESTS WHERE 1=1 ");
        if (!p.isBlank()) sql.append(" AND phone = ? ");
        if (!e.isBlank()) sql.append(" AND email = ? ");

        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!p.isBlank()) ps.setString(idx++, p);
            if (!e.isBlank()) ps.setString(idx++, e);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("guest_id"));
                }
            }
        }

        return ids;
    }
    
 // בתוך Reservation_DB_Controller

    public Reservation findGuestReservationByContactAndTime(
            String phone,
            String email,
            LocalDateTime dateTime) throws SQLException {

        if (dateTime == null) return null;

        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (!hasPhone && !hasEmail) return null;

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
            ps.setTimestamp(1, Timestamp.valueOf(dateTime));

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

 // =====================================================
 // REPORTS – SUBSCRIBERS
 // =====================================================

 /**
  * Returns number of reservations per day for a given role.
  * Used for reservations trend chart.
  */
 public Map<Integer, Integer> getReservationsCountPerDayByRole(
         Enums.UserRole role, int year, int month) throws SQLException {

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
                 map.put(
                         rs.getInt("day"),
                         rs.getInt("cnt")
                 );
             }
         }
     }
     return map;
 }
 
 
 /**
  * Updates core reservation details in the database.
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
         
         if (res.getTableNumber() != null) ps.setInt(3, res.getTableNumber());
         else ps.setNull(3, Types.INTEGER);
         
         ps.setString(4, res.getReservationStatus().name());
         ps.setString(5, res.getConfirmationCode());
         ps.setInt(6, res.getReservationId());

         return ps.executeUpdate() > 0;
     }
 }


}
