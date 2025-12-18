package dbControllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import entities.Enums;
import entities.Reservation;
import entities.Subscriber;
import entities.User;

public class User_DB_Controller {

    private Connection conn;

    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /* =========================
       LOGIN
       ========================= */

    public User loginUser(String username, String password) {

        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?"
            );

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUserName(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setPhoneNumber(rs.getString("phone"));
                user.setUserRole(
                        Enums.UserRole.valueOf(rs.getString("role"))
                );
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /* =========================
       CREATE USER (REGISTER – step 1)
       ========================= */

    public User createUser(
            String username,
            String password,
            String email,
            String phone
    ) throws SQLException {

        String sql =
                "INSERT INTO users (username, password, role, email, phone) " +
                "VALUES (?, ?, 'Subscriber', ?, ?)";

        PreparedStatement stmt =
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setString(3, email);
        stmt.setString(4, phone);

        stmt.executeUpdate();

        ResultSet keys = stmt.getGeneratedKeys();
        if (keys.next()) {
            User user = new User();
            user.setUserId(keys.getInt(1));
            user.setUserName(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setPhoneNumber(phone);
            user.setUserRole(Enums.UserRole.Subscriber);
            return user;
        }

        throw new SQLException("Failed to create user");
    }

    /* =========================
       CREATE SUBSCRIBER (REGISTER – step 2)
       ========================= */

    public Subscriber createSubscriber(
            int userId,
            String firstName,
            String lastName
    ) throws SQLException {

        String sql =
                "INSERT INTO subscribers (user_id, first_name, last_name) " +
                "VALUES (?, ?, ?)";

        PreparedStatement stmt =
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        stmt.setInt(1, userId);
        stmt.setString(2, firstName);
        stmt.setString(3, lastName);

        stmt.executeUpdate();

        ResultSet keys = stmt.getGeneratedKeys();
        if (keys.next()) {
            return new Subscriber(
                    keys.getInt(1),                     // subscriberNumber
                    firstName + " " + lastName,         // userName
                    "Created on registration"           // personalDetails
            );
        }

        throw new SQLException("Failed to create subscriber");
    }

    /* =========================
       GET SUBSCRIBER BY USER ID
       ========================= */

    public Subscriber getSubscriberByUserId(int userId) throws SQLException {

        String sql = "SELECT * FROM subscribers WHERE user_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new Subscriber(
                    rs.getInt("subscriber_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    "Loaded from DB"
            );
        }

        return null;
    }

    /* =========================
       UPDATE SUBSCRIBER DETAILS
       ========================= */

    public boolean updateSubscriberDetails(
            int subscriberNumber,
            String userName
    ) throws SQLException {

        String sql =
                "UPDATE subscribers " +
                "SET first_name = ?, last_name = ? " +
                "WHERE subscriber_id = ?";

        String[] nameParts = userName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = (nameParts.length > 1) ? nameParts[1] : "";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, firstName);
        stmt.setString(2, lastName);
        stmt.setInt(3, subscriberNumber);

        return stmt.executeUpdate() > 0;
    }


    /* =========================
       RESERVATION HISTORY
       ========================= */

    public ArrayList<Reservation> getReservationHistoryBySubscriber(int subscriberNumber)
            throws SQLException {

        ArrayList<Reservation> history = new ArrayList<>();

        String sql =
                "SELECT * FROM reservations WHERE subscriber_number = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, subscriberNumber);

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            Reservation reservation = new Reservation();
            reservation.setConfarmationCode(rs.getInt("confarmation_code"));
            reservation.setReservationTime(
                    rs.getTimestamp("reservation_time").toLocalDateTime()
            );
            reservation.setGuestAmount(rs.getInt("guest_amount"));
            reservation.setConfirmed(rs.getBoolean("is_confirmed"));
            history.add(reservation);
        }

        return history;
    }
    /* =========================
    UPDATE USER CONTACT DETAILS
    ========================= */

 /**
  * Updates the contact details of a user in the database.
  * This method updates ONLY the users table (phone and email).
  *
  * @param userId the unique ID of the user
  * @param phone updated phone number
  * @param email updated email address
  * @return true if the update succeeded, false otherwise
  * @throws SQLException if a database error occurs
  */
		 public boolean updateUserContactDetails(
		         int userId,
		         String phone,
		         String email
		 ) throws SQLException {
		
		     String sql =
		             "UPDATE users " +
		             "SET phone = ?, email = ? " +
		             "WHERE user_id = ?";
		
		     PreparedStatement stmt = conn.prepareStatement(sql);
		     stmt.setString(1, phone);
		     stmt.setString(2, email);
		     stmt.setInt(3, userId);
		
		     return stmt.executeUpdate() > 0;
 }

    
    
    
}
