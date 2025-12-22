package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import entities.Enums;
import entities.Subscriber;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /* =========================================================
       LOGIN SUBSCRIBER
       ========================================================= */

    public Subscriber loginSubscriber(String username, String password) {

        String sql =
                "SELECT * FROM SUBSCRIBERS " +
                "WHERE username = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Subscriber subscriber = new Subscriber();

                subscriber.setSubscriberId(rs.getInt("subscriber_id"));
                subscriber.setUsername(rs.getString("username"));
                subscriber.setPassword(rs.getString("password"));
                subscriber.setFirstName(rs.getString("first_name"));
                subscriber.setLastName(rs.getString("last_name"));
                subscriber.setPhone(rs.getString("phone"));
                subscriber.setEmail(rs.getString("email"));
                subscriber.setRole(
                        Enums.UserRole.valueOf(rs.getString("role"))
                );

                return subscriber;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /* =========================================================
       GUEST METHODS
       ========================================================= */

    public boolean guestExists(String phone, String email) throws SQLException {

        String sql =
                "SELECT 1 FROM GUESTS WHERE phone = ? AND email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, email);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public void enterAsGuest(String phone, String email) throws SQLException {

        if (guestExists(phone, email)) {
            return;
        }

        String sql =
                "INSERT INTO GUESTS (phone, email) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    public boolean deleteGuest(String phone, String email) throws SQLException {

        String sql =
                "DELETE FROM GUESTS WHERE phone = ? AND email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, email);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean isGuestEmailExists(String email) {

        String sql =
                "SELECT 1 FROM GUESTS WHERE email = ? LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* =========================================================
       REGISTER / LOAD SUBSCRIBER
       ========================================================= */

    public Subscriber registerSubscriber(
            String username,
            String password,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role
    ) throws SQLException {

        String sql =
                "INSERT INTO SUBSCRIBERS " +
                "(username, password, first_name, last_name, phone, email, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, phone);
            stmt.setString(6, email);
            stmt.setString(7, role.name());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                Subscriber subscriber = new Subscriber();

                subscriber.setSubscriberId(keys.getInt(1));
                subscriber.setUsername(username);
                subscriber.setPassword(password);
                subscriber.setFirstName(firstName);
                subscriber.setLastName(lastName);
                subscriber.setPhone(phone);
                subscriber.setEmail(email);
                subscriber.setRole(role);

                return subscriber;
            }
        }

        throw new SQLException("Failed to register subscriber");
    }

    public Subscriber getSubscriberById(int subscriberId) throws SQLException {

        String sql =
                "SELECT * FROM SUBSCRIBERS WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Subscriber subscriber = new Subscriber();

                subscriber.setSubscriberId(rs.getInt("subscriber_id"));
                subscriber.setUsername(rs.getString("username"));
                subscriber.setPassword(rs.getString("password"));
                subscriber.setFirstName(rs.getString("first_name"));
                subscriber.setLastName(rs.getString("last_name"));
                subscriber.setPhone(rs.getString("phone"));
                subscriber.setEmail(rs.getString("email"));
                subscriber.setRole(
                        Enums.UserRole.valueOf(rs.getString("role"))
                );

                return subscriber;
            }
        }

        return null;
    }

    public boolean updateSubscriberDetails(
            int subscriberId,
            String firstName,
            String lastName,
            String phone,
            String email
    ) throws SQLException {

        String sql =
                "UPDATE SUBSCRIBERS " +
                "SET first_name = ?, last_name = ?, phone = ?, email = ? " +
                "WHERE subscriber_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setInt(5, subscriberId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean usernameExists(String username) throws SQLException {

        String sql =
                "SELECT 1 FROM SUBSCRIBERS WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public ResultSet getAllSubscribers() throws SQLException {

        String sql = "SELECT * FROM SUBSCRIBERS";
        PreparedStatement stmt = conn.prepareStatement(sql);
        return stmt.executeQuery();
    }
}
