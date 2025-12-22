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
       ---------------------------------------------------------
       Authenticates a subscriber (subscriber / manager / rep)
       using username and password.
       ========================================================= */

    /**
     * Authenticates a subscriber using username and password.
     *
     * @param username login username
     * @param password login password
     * @return Subscriber object if authentication succeeds, null otherwise
     */
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
       CHECK GUEST EXISTENCE
       ---------------------------------------------------------
       Performs a soft check to determine whether a guest with
       the given contact details already exists.
       ========================================================= */

    /**
     * Checks whether a guest with the given phone and email exists.
     */
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

    /* =========================================================
       ENTER AS GUEST
       ---------------------------------------------------------
       Handles guest entry by storing contact details if needed.
       This is NOT an authentication process.
       ========================================================= */

    /**
     * Handles guest entry into the system.
     * Guests are softly identified by their contact details.
     */
    public void enterAsGuest(String phone, String email) throws SQLException {

        if (guestExists(phone, email)) {
            return; // Guest already exists
        }

        String sql =
                "INSERT INTO GUESTS (phone, email) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    /* =========================================================
       REGISTER SUBSCRIBER
       ---------------------------------------------------------
       Creates a new subscriber record in the system.
       Used for subscribers, restaurant representatives,
       and managers.
       ========================================================= */

    /**
     * Registers a new subscriber in the system.
     */
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

    /* =========================================================
       LOAD SUBSCRIBER BY ID
       ---------------------------------------------------------
       Retrieves full subscriber details using the primary key.
       Used after login or for internal system operations.
       ========================================================= */

    /**
     * Retrieves a subscriber by its unique ID.
     */
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

    /* =========================================================
       UPDATE SUBSCRIBER DETAILS
       ---------------------------------------------------------
       Updates personal and contact information of a subscriber.
       Does not modify authentication or role data.
       ========================================================= */

    /**
     * Updates subscriber personal and contact details.
     */
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
    
    /* =========================================================
    DELETE GUEST
    ---------------------------------------------------------
    Removes a guest record from the database.
    Guests are identified by their contact details.
    ========================================================= */

 /**
  * Deletes a guest from the database based on phone and email.
  *
  * @param phone guest phone number
  * @param email guest email address
  * @return true if a guest record was deleted, false otherwise
  * @throws SQLException if a database error occurs
  */
	 public boolean deleteGuest(String phone, String email) throws SQLException {
	
	     String sql =
	             "DELETE FROM GUESTS WHERE phone = ? AND email = ?";
	
	     try (PreparedStatement stmt = conn.prepareStatement(sql)) {
	
	         stmt.setString(1, phone);
	         stmt.setString(2, email);
	
	         return stmt.executeUpdate() > 0;
     }
 }
	 
	 
	 /* =========================================================
	   CHECK USERNAME EXISTENCE
	   ---------------------------------------------------------
	   Checks whether a given username already exists in the
	   SUBSCRIBERS table.
	   ========================================================= */

	/**
	 * Checks if a username is already taken.
	 *
	 * @param username the username to check
	 * @return true if the username exists, false otherwise
	 * @throws SQLException if a database error occurs
	 */
	public boolean usernameExists(String username) throws SQLException {

	    String sql =
	            "SELECT 1 FROM SUBSCRIBERS WHERE username = ?";

	    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

	        stmt.setString(1, username);

	        ResultSet rs = stmt.executeQuery();
	        return rs.next();
	    }
	}
	
	/* =========================================================
	   LOAD ALL SUBSCRIBERS
	   ---------------------------------------------------------
	   Retrieves all subscriber records from the database.
	   This method is intended for future administrative or
	   reporting features and is not part of the current flow.
	   ========================================================= */

	/**
	 * Retrieves all subscribers from the database.
	 *
	 * @return ResultSet containing all subscriber records
	 * @throws SQLException if a database error occurs
	 */
	public ResultSet getAllSubscribers() throws SQLException {

	    String sql = "SELECT * FROM SUBSCRIBERS";

	    PreparedStatement stmt = conn.prepareStatement(sql);
	    return stmt.executeQuery();
	}
	
	
	/**
	 * Checks whether a guest with the given email already exists in the database.
	 *
	 * This method is used during guest login/registration in order to:
	 * - Decide whether to send OTP only
	 * - Or create a new guest record and then send OTP
	 *
	 * @param email the guest's email address
	 * @return true if a guest with this email exists, false otherwise
	 */
	public boolean isGuestEmailExists(String email) {

	    // Query returns a single row if the email exists
	    String query = "SELECT 1 FROM GUESTS WHERE email = ? LIMIT 1";

	    try (PreparedStatement ps = conn.prepareStatement(query)) {

	        // Set email parameter
	        ps.setString(1, email);

	        // Execute query
	        ResultSet rs = ps.executeQuery();

	        // If a row exists – the email is already in the database
	        return rs.next();

	    } catch (SQLException e) {

	        // Log database error (can be replaced with logger)
	        e.printStackTrace();

	        // In case of error, treat as "not exists" or throw exception
	        return false;
	    }
	}




}