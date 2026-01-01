package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import entities.Enums;
import entities.Subscriber;
import entities.User;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /*
     * ===============================================================
     * createGuestsTable
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

    /*
     * ===============================================================
     * createSubscribersTable
     * ===============================================================
     */
    public void createSubscribersTable() throws SQLException {

        String sql = "CREATE TABLE IF NOT EXISTS SUBSCRIBERS (" +
                     "subscriber_id INT PRIMARY KEY, " +
                     "username VARCHAR(50) NOT NULL UNIQUE, " +
                     "password VARCHAR(255) NOT NULL, " +
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
     * loginSubscriber (NO ID CREATION HERE!)
     * ===============================================================
     */
    public Subscriber loginSubscriber(int subscriberId, String username, String password) {

        String sql = "SELECT * FROM SUBSCRIBERS WHERE subscriber_id = ? AND username = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            stmt.setString(2, username);
            stmt.setString(3, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Subscriber(
                        rs.getInt("subscriber_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        Enums.UserRole.valueOf(rs.getString("role"))
                );
            }
 
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * ===============================================================
     * loginGuest (CREATES NEW USER + NEW ID)
     * ===============================================================
     */
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
     * registerSubscriber (CREATES NEW USER + NEW ID)
     * ===============================================================
     */
    public Subscriber registerSubscriber(
            int subscriberId,
            String username,
            String password,
            String firstName,
            String lastName,
            String phone,
            String email,
            Enums.UserRole role) {

        String sql = "INSERT INTO SUBSCRIBERS " +
                     "(subscriber_id, username, password, first_name, last_name, phone, email, role) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, firstName);
            stmt.setString(5, lastName);
            stmt.setString(6, phone);
            stmt.setString(7, email);
            stmt.setString(8, role.name());

            stmt.executeUpdate();

            return new Subscriber(
                    subscriberId,
                    username,
                    password,
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
    
    public boolean subscriberExists(int subscriberId, String email) {

        String sql = "SELECT 1 FROM SUBSCRIBERS WHERE subscriber_id = ? AND email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, subscriberId);
            stmt.setString(2, email);

            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    
    

}
