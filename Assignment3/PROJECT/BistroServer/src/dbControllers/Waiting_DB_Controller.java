package dbControllers;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

import entities.Waiting;
import entities.Enums.UserRole;
import entities.Enums.WaitingStatus;

public class Waiting_DB_Controller {

    private final Connection conn;

    public Waiting_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    // ===== Waiting List Table =====
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

    // Inserts waiting entry
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
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // Cancel by confirmation code (only if still Waiting)
    public boolean cancelWaiting(String confirmationCode) throws SQLException {
        String sql = """
            UPDATE waiting_list
            SET waiting_status = 'Cancelled'
            WHERE confirmation_code = ?
              AND waiting_status = 'Waiting';
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationCode);
            return ps.executeUpdate() > 0;
        }
    }

    // Mark as seated (only if still Waiting)
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

    // When a table becomes available for this client:
    // set table_freed_time = now, optionally set table_number (status stays Waiting)
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

            if (tableNumber == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, tableNumber);

            ps.setString(3, confirmationCode);

            return ps.executeUpdate() > 0;
        }
    }

    // Auto-cancel after 15 minutes from table_freed_time (still Waiting)
    public int cancelExpiredWaitings(LocalDateTime now) throws SQLException {
        String sql = """
            UPDATE waiting_list
            SET waiting_status = 'Cancelled'
            WHERE waiting_status = 'Waiting'
              AND table_freed_time IS NOT NULL
              AND table_freed_time < ?;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now.minusMinutes(15)));
            return ps.executeUpdate();
        }
    }

    public boolean isActiveWaitingExists(String confirmationCode) throws SQLException {
        String sql = """
            SELECT 1
            FROM waiting_list
            WHERE confirmation_code = ?
              AND waiting_status = 'Waiting';
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Waiting getWaitingByConfirmationCode(String confirmationCode) throws SQLException {
        String sql = """
            SELECT * FROM waiting_list
            WHERE confirmation_code = ?;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToWaiting(rs);
            }
        }
        return null;
    }

    public ArrayList<Waiting> getActiveWaitingList() throws SQLException {
        String sql = """
            SELECT * FROM waiting_list
            WHERE waiting_status = 'Waiting'
            ORDER BY waiting_id;
            """;
        return executeWaitingListQuery(sql);
    }

    public ArrayList<Waiting> getWaitingListByUser(int userId) throws SQLException {
        String sql = """
            SELECT * FROM waiting_list
            WHERE created_by = ?
            ORDER BY waiting_id;
            """;

        ArrayList<Waiting> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowToWaiting(rs));
            }
        }
        return list;
    }

    // ===== Mapping =====
    private Waiting mapRowToWaiting(ResultSet rs) throws SQLException {
        Waiting w = new Waiting();

        w.setWaitingId(rs.getInt("waiting_id"));
        w.setConfirmationCode(rs.getString("confirmation_code"));

        w.setGuestAmount(rs.getInt("number_of_guests"));

        w.setCreatedByUserId(rs.getInt("created_by"));
        w.setCreatedByRole(UserRole.valueOf(rs.getString("created_by_role")));

        Timestamp freed = rs.getTimestamp("table_freed_time");
        w.setTableFreedTime(freed == null ? null : freed.toLocalDateTime());

        int tableNum = rs.getInt("table_number");
        w.setTableNumber(rs.wasNull() ? null : tableNum);

        w.setWaitingStatus(WaitingStatus.valueOf(rs.getString("waiting_status")));

        return w;
    }

    private ArrayList<Waiting> executeWaitingListQuery(String sql) throws SQLException {
        ArrayList<Waiting> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToWaiting(rs));
            }
        }
        return list;
    }
}
