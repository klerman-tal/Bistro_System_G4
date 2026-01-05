package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.Enums;
import entities.Subscriber;
import entities.User;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }
// חסר טיפול האם למחוק מנוי מהטבלה לאחר חודש ?
    // חסר טיפול בקוד הזמנה + הרשאות
    /*
     * ===============================================================
     * TABLE CREATION METHODS
     * ===============================================================
     */

    public void createGuestsTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS GUESTS (" +
                     "guest_id INT PRIMARY KEY, " +
                     "phone VARCHAR(15) NOT NULL, " +
                     "email VARCHAR(100) NOT NULL)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public void createSubscribersTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS SUBSCRIBERS (" +
                     "subscriber_id INT PRIMARY KEY, " +
                     "username VARCHAR(50) NOT NULL UNIQUE, " +
                     "first_name VARCHAR(50), " +
                     "last_name VARCHAR(50), " +
                     "phone VARCHAR(15), " +
                     "email VARCHAR(100), " +
                     "role ENUM('SUBSCRIBER','MANAGER','REPRESENTATIVE') NOT NULL)";

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
            stmt.setString(2, phone);
            stmt.setString(3, email);
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

            return new Subscriber(
                    subscriberId,
                    username,
                    firstName,
                    lastName,
                    phone,
                    email,
                    role
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

        return new Subscriber(
                rs.getInt("subscriber_id"),
                rs.getString("username"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("email"),
                Enums.UserRole.valueOf(rs.getString("role"))
        );
    }
}