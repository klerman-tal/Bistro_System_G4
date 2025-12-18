package dbControllers;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import entities.Reservation;

public class Reservation_DB_Controller {

    private final Connection conn;

    public Reservation_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // Creates reservations table if it doesn't exist
    public void createReservationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                reservation_id INT AUTO_INCREMENT PRIMARY KEY,
                reservation_date DATE NOT NULL,
                reservation_time TIME NOT NULL,
                number_of_guests INT NOT NULL,
                confirmation_code INT NOT NULL,
                created_by VARCHAR(255) NOT NULL,
                is_confirmed TINYINT(1) NOT NULL DEFAULT 0,
                table_number INT NULL,
                UNIQUE (confirmation_code),
                INDEX (reservation_date),
                INDEX (created_by),
                INDEX (table_number)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Inserts a reservation (without assigning a table yet) and returns generated reservation_id
    public int addReservation(LocalDate date, LocalTime time, int guests, int confirmationCode, String createdBy) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (reservation_date, reservation_time, number_of_guests, confirmation_code, created_by, is_confirmed, table_number)
            VALUES (?, ?, ?, ?, ?, 0, NULL);
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(time));
            ps.setInt(3, guests);
            ps.setInt(4, confirmationCode);
            ps.setString(5, createdBy);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // Returns reservation by reservation_id (or null if not found)
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

    // Returns reservation by confirmation_code (or null if not found)
    public Reservation getReservationByConfirmationCode(int confirmationCode) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE confirmation_code = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, confirmationCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToReservation(rs);
            }
        }
        return null;
    }

    // Returns all reservations created by a given user (created_by)
    public ArrayList<Reservation> getReservationsByUser(String createdBy) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE created_by = ?
            ORDER BY reservation_date, reservation_time;
            """;

        ArrayList<Reservation> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, createdBy);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReservation(rs));
                }
            }
        }
        return list;
    }

    // Updates date/time/guests/is_confirmed for an existing reservation
    public boolean updateReservation(int reservationId, LocalDate date, LocalTime time, int guests, boolean isConfirmed) throws SQLException {
        String sql = """
            UPDATE reservations
            SET reservation_date = ?, reservation_time = ?, number_of_guests = ?, is_confirmed = ?
            WHERE reservation_id = ?;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(time));
            ps.setInt(3, guests);
            ps.setInt(4, isConfirmed ? 1 : 0);
            ps.setInt(5, reservationId);

            return ps.executeUpdate() == 1;
        }
    }

    // Assigns a table to a reservation AND marks the table as not available (transaction)
    public boolean assignTableToReservation(int reservationId, int tableNumber) throws SQLException {
        String checkTable = "SELECT is_available FROM tables WHERE table_number = ?;";
        String updateRes = "UPDATE reservations SET table_number = ? WHERE reservation_id = ?;";
        String markTable = "UPDATE tables SET is_available = 0 WHERE table_number = ?;";

        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (
            PreparedStatement psCheck = conn.prepareStatement(checkTable);
            PreparedStatement psRes = conn.prepareStatement(updateRes);
            PreparedStatement psTable = conn.prepareStatement(markTable)
        ) {
            psCheck.setInt(1, tableNumber);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (!rs.next() || rs.getInt("is_available") != 1) {
                    conn.rollback();
                    return false;
                }
            }

            psRes.setInt(1, tableNumber);
            psRes.setInt(2, reservationId);
            if (psRes.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }

            psTable.setInt(1, tableNumber);
            if (psTable.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    // Deletes reservation by id; if it had a table, it releases it (transaction)
    public boolean deleteReservationById(int reservationId) throws SQLException {
        String getTable = "SELECT table_number FROM reservations WHERE reservation_id = ?;";
        String release = "UPDATE tables SET is_available = 1 WHERE table_number = ?;";
        String del = "DELETE FROM reservations WHERE reservation_id = ?;";

        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        Integer tableNumber = null;

        try (
            PreparedStatement psGet = conn.prepareStatement(getTable);
            PreparedStatement psRelease = conn.prepareStatement(release);
            PreparedStatement psDel = conn.prepareStatement(del)
        ) {
            psGet.setInt(1, reservationId);
            try (ResultSet rs = psGet.executeQuery()) {
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                int tn = rs.getInt("table_number");
                if (!rs.wasNull()) tableNumber = tn;
            }

            if (tableNumber != null) {
                psRelease.setInt(1, tableNumber);
                psRelease.executeUpdate(); // even if 0 rows, still continue
            }

            psDel.setInt(1, reservationId);
            boolean ok = psDel.executeUpdate() == 1;

            if (!ok) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    // Deletes reservation by confirmation code
    public boolean deleteReservationByConfirmationCode(int confirmationCode) throws SQLException {
        String sql = "DELETE FROM reservations WHERE confirmation_code = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, confirmationCode);
            return ps.executeUpdate() == 1;
        }
    }

    // Maps a DB row into your Reservation entity (createdBy stays null for now)
    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();

        int code = rs.getInt("confirmation_code");
        int guests = rs.getInt("number_of_guests");
        boolean confirmed = rs.getInt("is_confirmed") == 1;

        LocalDate d = rs.getDate("reservation_date").toLocalDate();
        LocalTime t = rs.getTime("reservation_time").toLocalTime();
        LocalDateTime dt = LocalDateTime.of(d, t);

        r.setConfarmationCode(code);
        r.setGuestAmount(guests);
        r.setConfirmed(confirmed);
        r.setReservationTime(dt);

        // Note: created_by is a String in DB but your Reservation has User createdBy.
        // You can later add a String field to Reservation OR load a User object by username/email.
        return r;
    }
    
 // Creates waiting_list table if it doesn't exist
    public void createWaitingListTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS waiting_list (
                waiting_list_id INT AUTO_INCREMENT PRIMARY KEY,
                number_of_guests INT NOT NULL,
                confirmation_code INT NOT NULL,
                created_by VARCHAR(255) NOT NULL,
                UNIQUE (confirmation_code),
                INDEX (created_by)
            );
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
 // Inserts a new entry into waiting list and returns generated waiting_list_id
    public int addToWaitingList(int guests, int confirmationCode, String createdBy) throws SQLException {
        String sql = """
            INSERT INTO waiting_list
            (number_of_guests, confirmation_code, created_by)
            VALUES (?, ?, ?);
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, guests);
            ps.setInt(2, confirmationCode);
            ps.setString(3, createdBy);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }
    
 // Checks if a confirmation code exists in waiting list
    public boolean isInWaitingList(int confirmationCode) throws SQLException {
        String sql = "SELECT 1 FROM waiting_list WHERE confirmation_code = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, confirmationCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    
 // Returns all waiting list entries created by a given user
    public ArrayList<Integer> getWaitingListByUser(String createdBy) throws SQLException {
        String sql = """
            SELECT confirmation_code
            FROM waiting_list
            WHERE created_by = ?;
            """;

        ArrayList<Integer> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, createdBy);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("confirmation_code"));
                }
            }
        }
        return list;
    }

 // Deletes entry from waiting list by confirmation code
    public boolean deleteFromWaitingList(int confirmationCode) throws SQLException {
        String sql = "DELETE FROM waiting_list WHERE confirmation_code = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, confirmationCode);
            return ps.executeUpdate() == 1;
        }
    }

    



}
