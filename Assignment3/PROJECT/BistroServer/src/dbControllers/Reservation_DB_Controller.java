package dbControllers;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

import entities.Enums.ReservationStatus;
import entities.Reservation;

public class Reservation_DB_Controller {

    private final Connection conn;

    public Reservation_DB_Controller(Connection conn) {
        this.conn = conn;
    }

 // ===== Reservations Table =====
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

                UNIQUE (confirmation_code),
                INDEX (reservation_datetime),
                INDEX (created_by),
                INDEX (table_number),
                INDEX (reservation_status),
                INDEX checkin (checkin),
                INDEX checkout (checkout)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    // Inserts reservation
    public int addReservation(
            LocalDateTime reservationDateTime,
            int guests,
            String confirmationCode,
            int createdByUserId) throws SQLException {

        String sql = """
            INSERT INTO reservations
            (reservation_datetime, number_of_guests, confirmation_code, created_by, is_confirmed, is_active, table_number)
            VALUES (?, ?, ?, ?, 0, 1, NULL);
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(reservationDateTime));
            ps.setInt(2, guests);
            ps.setString(3, confirmationCode);
            ps.setInt(4, createdByUserId);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return -1;
    }
    
 // Deactivates a reservation by confirmation code (soft delete)
    public boolean deactivateReservationByConfirmationCode(String confirmationCode) throws SQLException {
        String sql = """
            UPDATE reservations
            SET is_active = 0
            WHERE confirmation_code = ?;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationCode);
            return ps.executeUpdate() == 1;
        }
    }

    
    public ArrayList<Reservation> getAllReservations() throws SQLException {
        String sql = "SELECT * FROM reservations ORDER BY reservation_datetime;";
        return executeReservationListQuery(sql);
    }

    public ArrayList<Reservation> getActiveReservations() throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE is_active = 1
            ORDER BY reservation_datetime;
            """;
        return executeReservationListQuery(sql);
    }

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





    // ===== Mapping =====
    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();

        r.setConfirmationCode(rs.getString("confirmation_code"));
        r.setGuestAmount(rs.getInt("number_of_guests"));
        r.setConfirmed(rs.getInt("is_confirmed") == 1);
        r.setReservationTime(rs.getTimestamp("reservation_datetime").toLocalDateTime());
        r.setCreatedByUserId(rs.getInt("created_by"));

        return r;
    }

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
    
 // Checks if an active reservation exists for the given confirmation code
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

}
