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
    
    public ArrayList<Waiting> getAllWaitings() throws SQLException {
        // הורדנו את ה-WHERE, עכשיו הוא מביא הכל. 
        // הוספתי DESC כדי שההזמנות הכי חדשות יופיעו למעלה.
        String sql = "SELECT * FROM waiting_list ORDER BY waiting_id DESC;"; 
        
        ArrayList<Waiting> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToWaiting(rs));
            }
        }
        return list;
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

    // NEW: get expired confirmation codes (for cancelling reservations too)
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
                if (rs.next()) return mapRowToWaiting(rs);
            }
        }
        return null;
    }

    private Waiting mapRowToWaiting(ResultSet rs) throws SQLException {
        Waiting w = new Waiting();

        // שליפת כל השדות מה-DB והשמתם באובייקט ה-Entity
        w.setWaitingId(rs.getInt("waiting_id"));
        w.setGuestAmount(rs.getInt("number_of_guests"));
        w.setConfirmationCode(rs.getString("confirmation_code"));
        w.setCreatedByUserId(rs.getInt("created_by"));
        
        // טיפול ב-Enum של ה-Role
        String roleStr = rs.getString("created_by_role");
        if (roleStr != null) {
            w.setCreatedByRole(UserRole.valueOf(roleStr));
        }

        // טיפול בזמן (יכול להיות NULL)
        Timestamp freed = rs.getTimestamp("table_freed_time");
        w.setTableFreedTime(freed == null ? null : freed.toLocalDateTime());

        // טיפול במספר שולחן (יכול להיות NULL)
        int tableNum = rs.getInt("table_number");
        w.setTableNumber(rs.wasNull() ? null : tableNum);

        // טיפול בסטטוס (כולל תיקון פורמט טקסט)
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
            if (tableNumber == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, tableNumber);

            ps.setString(2, confirmationCode);

            return ps.executeUpdate() > 0;
        }
    }
    
 // =====================================================
 // REPORTS – WAITING LIST
 // =====================================================

 /**
  * Returns waiting list entries count per day for a given role.
  */
 public Map<Integer, Integer> getWaitingCountPerDayByRole(
         UserRole role, int year, int month) throws SQLException {

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
                 map.put(
                         rs.getInt("day"),
                         rs.getInt("cnt")
                 );
             }
         }
     }
     return map;
 }

 public ArrayList<Waiting> getActiveWaitingsForToday(LocalDate today) throws SQLException {

	    String sql = """
	        SELECT *
	        FROM waiting_list
	        WHERE waiting_status = 'Waiting'
	          AND DATE(table_freed_time) IS NULL
	        """;

	    ArrayList<Waiting> list = new ArrayList<>();

	    try (PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            list.add(mapRowToWaiting(rs));
	        }
	    }
	    return list;
	}
 
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



}
