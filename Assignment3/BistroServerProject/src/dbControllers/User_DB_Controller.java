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

    /**
     * Authenticates a user using username and password.
     *
     * @return User object if credentials are valid, otherwise null
     */
    public User loginUser(String username, String password) {

        User user = null;

        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?"
            );

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                user = new User();

                user.setUserId(rs.getInt("user_id"));
                user.setUserName(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setPhoneNumber(rs.getString("phone_number"));
                user.setEmail(rs.getString("email"));

                // Convert role from DB string to Enum
                user.setUserRole(
                        Enums.UserRole.valueOf(rs.getString("role"))
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    /**
     * Creates a new subscriber in the database.
     *
     * @return Subscriber object with generated subscriber number
     * @throws SQLException if subscriber already exists or insert fails
     */
    public Subscriber createSubscriber(
            String firstName,
            String lastName,
            String email,
            String phone
    ) throws SQLException {

        String checkSql =
                "SELECT subscriber_id FROM subscribers WHERE email = ?";

        String insertSql =
                "INSERT INTO subscribers (first_name, last_name, email, phone) VALUES (?, ?, ?, ?)";

        // Check if subscriber already exists
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setString(1, email);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            throw new SQLException("Subscriber already exists");
        }

        // Insert new subscriber
        PreparedStatement insertStmt =
                conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

        insertStmt.setString(1, firstName);
        insertStmt.setString(2, lastName);
        insertStmt.setString(3, email);
        insertStmt.setString(4, phone);

        insertStmt.executeUpdate();

        // Retrieve generated subscriber number
        ResultSet keys = insertStmt.getGeneratedKeys();
        if (keys.next()) {
            int subscriberNumber = keys.getInt(1);

            return new Subscriber(
                    subscriberNumber,
                    email,
                    firstName + " " + lastName + ", phone: " + phone
            );
        }

        throw new SQLException("Failed to create subscriber");
    }

    /**
     * Retrieves subscriber entity associated with a specific user ID.
     *
     * @param userId ID of the logged-in user
     * @return Subscriber object or null if not found
     */
    public Subscriber getSubscriberByUserId(int userId) throws SQLException {

        String sql = "SELECT * FROM subscribers WHERE user_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new Subscriber(
                    rs.getInt("subscriber_number"),
                    rs.getString("user_name"),
                    rs.getString("personal_details")
            );
        }

        return null;
    }

    /**
     * Retrieves reservation history for a specific subscriber.
     *
     * @param subscriberNumber subscriber unique identifier
     * @return list of Reservation objects
     */
    public ArrayList<Reservation> getReservationHistoryBySubscriber(int subscriberNumber)
            throws SQLException {

        ArrayList<Reservation> history = new ArrayList<>();

        String sql = "SELECT * FROM reservations WHERE subscriber_number = ?";
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
    
    /**
     * Updates subscriber details in the database.
     *
     * @param subscriberNumber unique subscriber identifier
     * @param userName new user name
     * @param personalDetails new personal details
     * @return true if update succeeded, false otherwise
     */
    public boolean updateSubscriberDetails(
            int subscriberNumber,String userName ,String personalDetails ) throws SQLException {
        String sql ="UPDATE subscribers " + "SET user_name = ?, personal_details = ? " + "WHERE subscriber_number = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);

        // Set new values
        stmt.setString(1, userName);
        stmt.setString(2, personalDetails);

        // Identify which subscriber to update
        stmt.setInt(3, subscriberNumber);

        int rowsUpdated = stmt.executeUpdate();

        // If at least one row was updated → success
        return rowsUpdated > 0;
    }
    
    
    

}
