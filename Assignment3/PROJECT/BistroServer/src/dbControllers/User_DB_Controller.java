package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.Enums;
import entities.RestaurantAgent;
import entities.RestaurantManager;
import entities.Subscriber;
import entities.User;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /*
     * ===============================================================
     * TABLE CREATION METHODS
     * ===============================================================
     */

    public void createGuestsTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS GUESTS (" +
                     "guest_id INT PRIMARY KEY, " +
                     "phone VARCHAR(15) NULL, " +
                     "email VARCHAR(100) NULL)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public void createSubscribersTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS SUBSCRIBERS (" +
                     "subscriber_id INT PRIMARY KEY, " +
                     "username VARCHAR(50) NOT NULL, " +
                     "first_name VARCHAR(50), " +
                     "last_name VARCHAR(50), " +
                     "phone VARCHAR(15), " +
                     "email VARCHAR(100), " +
                     "role ENUM('RandomClient','Subscriber','RestaurantAgent','RestaurantManager') NOT NULL)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /*
     * ===============================================================
     * AUTHENTICATION / LOGIN METHODS
     * ===============================================================
     */

    public Subscriber loginSubscriber(int subscriberId, String username) {

        String sql = "SELECT * FROM SUBSCRIBERS WHERE subscriber_id = ? AND username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            stmt.setString(2, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRowToSubscriber(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public User loginGuest(int guestId, String phone, String email) {
        String sql = "INSERT INTO GUESTS (guest_id, phone, email) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guestId);

            if (phone == null || phone.isBlank()) stmt.setNull(2, java.sql.Types.VARCHAR);
            else stmt.setString(2, phone);

            if (email == null || email.isBlank()) stmt.setNull(3, java.sql.Types.VARCHAR);
            else stmt.setString(3, email);

            stmt.executeUpdate();

            User user = new User(phone, email);
            user.setUserId(guestId);
            user.setUserRole(Enums.UserRole.RandomClient);

            return user;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * ===============================================================
     * REGISTRATION METHODS
     * ===============================================================
     */

    public Subscriber registerSubscriber(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role) {

        String sql = "INSERT INTO SUBSCRIBERS " +
                     "(subscriber_id, username, first_name, last_name, phone, email, role) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            stmt.setString(2, username);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, phone);
            stmt.setString(6, email);
            stmt.setString(7, role.name());

            stmt.executeUpdate();

            // ✅ מחזירים אובייקט מהסוג הנכון
            return createSubscriberByRole(
                    subscriberId, username, firstName, lastName, phone, email, role
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * ===============================================================
     * RETRIEVAL (SELECT) METHODS
     * ===============================================================
     */

    public Subscriber getSubscriberById(int subscriberId) {

        String sql = "SELECT * FROM SUBSCRIBERS WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRowToSubscriber(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Subscriber getSubscriberByUsernameAndPhone(String username, String phone) {

        String sql = "SELECT * FROM SUBSCRIBERS WHERE username = ? AND phone = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, phone);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRowToSubscriber(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Subscriber> getAllSubscribers() {

        List<Subscriber> subscribers = new ArrayList<>();
        String sql = "SELECT * FROM SUBSCRIBERS";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                subscribers.add(mapRowToSubscriber(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return subscribers;
    }

    /*
     * ===============================================================
     * UPDATE METHODS
     * ===============================================================
     */

    public boolean updateSubscriberContactDetails(
            int subscriberId,
            String phone,
            String email) {

        String sql = "UPDATE SUBSCRIBERS SET phone = ?, email = ? WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, email);
            stmt.setInt(3, subscriberId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * ===============================================================
     * EXISTENCE CHECK METHODS
     * ===============================================================
     */

    public boolean subscriberExistsById(int subscriberId) {

        String sql = "SELECT 1 FROM SUBSCRIBERS WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean subscriberExistsByUsername(String username) {

        String sql = "SELECT 1 FROM SUBSCRIBERS WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * ===============================================================
     * DELETE METHODS
     * ===============================================================
     */

    public boolean deleteGuest(int guestId) {

        String sql = "DELETE FROM GUESTS WHERE guest_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guestId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteRestaurantAgent(int subscriberId) {

        String sql = "DELETE FROM SUBSCRIBERS WHERE subscriber_id = ? AND role = 'RestaurantAgent'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteSubscriber(int subscriberId) {

        String sql = "DELETE FROM SUBSCRIBERS WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * ===============================================================
     * INTERNAL HELPER METHODS
     * ===============================================================
     */

    private Subscriber mapRowToSubscriber(ResultSet rs) throws SQLException {

        int id = rs.getInt("subscriber_id");
        String username = rs.getString("username");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String phone = rs.getString("phone");
        String email = rs.getString("email");

        Enums.UserRole role = Enums.UserRole.Subscriber; // default safe
        String roleStr = rs.getString("role");
        if (roleStr != null && !roleStr.isBlank()) {
            try { role = Enums.UserRole.valueOf(roleStr); }
            catch (IllegalArgumentException ignore) {}
        }

        return createSubscriberByRole(id, username, first, last, phone, email, role);
    }

    private Subscriber createSubscriberByRole(
            int id,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role) {

        if (role == Enums.UserRole.RestaurantManager) {
            return new RestaurantManager(id, username, firstName, lastName, phone, email);
        }

        if (role == Enums.UserRole.RestaurantAgent) {
            return new RestaurantAgent(id, username, firstName, lastName, phone, email);
        }

        Subscriber s = new Subscriber(id, username, firstName, lastName, phone, email);
        s.setUserRole(role); // לכיסוי מקרים חריגים
        return s;
    }

    public Subscriber getSubscriberByUsernamePhoneEmail(
            String username,
            String phone,
            String email) {

        String sql = """
            SELECT * FROM SUBSCRIBERS
            WHERE username = ?
              AND phone = ?
              AND email = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, phone);
            stmt.setString(3, email);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRowToSubscriber(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateSubscriberDetails(
            int subscriberId,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {

        String sql = """
            UPDATE SUBSCRIBERS
            SET
                first_name = COALESCE(?, first_name),
                last_name  = COALESCE(?, last_name),
                phone      = COALESCE(?, phone),
                email      = COALESCE(?, email)
            WHERE subscriber_id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setInt(5, subscriberId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Counts subscribers that made at least one reservation
     * in the given year/month.
     */
    public int countActiveSubscribersInMonth(int year, int month)
            throws SQLException {

        String sql = """
            SELECT COUNT(DISTINCT r.created_by)
            FROM reservations r
            WHERE r.created_by_role = 'Subscriber'
              AND YEAR(r.reservation_datetime) = ?
              AND MONTH(r.reservation_datetime) = ?;
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Counts subscribers that did NOT make any reservation
     * in the given year/month.
     */
    public int countInactiveSubscribersInMonth(int year, int month)
            throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM subscribers s
            WHERE s.subscriber_id NOT IN (
                SELECT DISTINCT r.created_by
                FROM reservations r
                WHERE r.created_by_role = 'Subscriber'
                  AND YEAR(r.reservation_datetime) = ?
                  AND MONTH(r.reservation_datetime) = ?
            );
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public int getMaxUserIdFromGuestsAndSubscribers() throws SQLException {
        String sql = """
            SELECT 
                GREATEST(
                    COALESCE((SELECT MAX(guest_id) FROM guests), 0),
                    COALESCE((SELECT MAX(subscriber_id) FROM subscribers), 0)
                ) AS max_id
            """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("max_id");
            }

            return 0; // אם משום מה לא חזר כלום
        }
    }


}
