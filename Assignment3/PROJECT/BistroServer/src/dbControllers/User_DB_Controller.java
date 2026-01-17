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

/**
 * Provides JDBC-based persistence operations for guest and subscriber users.
 * <p>
 * This controller is responsible for creating the relevant user tables and performing
 * authentication, registration, retrieval, update, and deletion operations.
 * </p>
 * <p>
 * Main tables handled:
 * <ul>
 *   <li>{@code GUESTS} - guest users identified by {@code guest_id} with optional phone/email</li>
 *   <li>{@code SUBSCRIBERS} - registered users identified by {@code subscriber_id} with roles</li>
 * </ul>
 * </p>
 */
public class User_DB_Controller {

    private Connection conn;

    /**
     * Constructs a User_DB_Controller with the given JDBC connection.
     *
     * @param conn active JDBC connection used for user persistence
     */
    public User_DB_Controller(Connection conn) {
        this.conn = conn;
    }

    /*
     * ===============================================================
     * TABLE CREATION METHODS
     * ===============================================================
     */

    /**
     * Creates the {@code GUESTS} table if it does not exist.
     * <p>
     * The table stores a guest id and optional contact fields.
     * </p>
     *
     * @throws SQLException if a database error occurs during table creation
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

    /**
     * Creates the {@code SUBSCRIBERS} table if it does not exist.
     * <p>
     * The table stores subscriber identity fields and a required {@code role} enum value.
     * </p>
     *
     * @throws SQLException if a database error occurs during table creation
     */
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

    /**
     * Authenticates a subscriber by id and username.
     *
     * @param subscriberId subscriber identifier
     * @param username     subscriber username
     * @return a {@link Subscriber} instance if credentials match; otherwise {@code null}
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

    /**
     * Inserts a new guest row and returns a {@link User} instance representing the guest.
     * <p>
     * Phone and email are optional; blank values are persisted as {@code NULL}.
     * </p>
     *
     * @param guestId guest identifier to insert
     * @param phone   guest phone (nullable/blank allowed)
     * @param email   guest email (nullable/blank allowed)
     * @return a {@link User} instance on success; otherwise {@code null}
     */
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

    /**
     * Registers a new subscriber row and returns an entity instance according to the requested role.
     * <p>
     * The returned instance type can be {@link Subscriber}, {@link RestaurantAgent}, or
     * {@link RestaurantManager}, depending on {@code role}.
     * </p>
     *
     * @param subscriberId subscriber identifier to insert
     * @param username     username (not null)
     * @param firstName    first name
     * @param lastName     last name
     * @param phone        phone
     * @param email        email
     * @param role         desired role for the subscriber
     * @return created subscriber entity if insert succeeded; otherwise {@code null}
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

    /**
     * Retrieves a subscriber by id.
     *
     * @param subscriberId subscriber identifier
     * @return subscriber entity if found; otherwise {@code null}
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

    /**
     * Retrieves a subscriber by username and phone.
     *
     * @param username username
     * @param phone    phone
     * @return subscriber entity if found; otherwise {@code null}
     */
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

    /**
     * Returns all rows from {@code SUBSCRIBERS}.
     *
     * @return list of subscribers (possibly empty)
     */
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

    /**
     * Updates phone and email for a subscriber id.
     *
     * @param subscriberId subscriber identifier
     * @param phone        new phone value
     * @param email        new email value
     * @return {@code true} if at least one row was updated; otherwise {@code false}
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

    /**
     * Checks whether a subscriber exists by id.
     *
     * @param subscriberId subscriber identifier
     * @return {@code true} if a matching row exists; otherwise {@code false}
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

    /**
     * Checks whether a subscriber exists by username.
     *
     * @param username username to check
     * @return {@code true} if a matching row exists; otherwise {@code false}
     */
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

    /**
     * Deletes a guest row by id.
     *
     * @param guestId guest identifier
     * @return {@code true} if a row was deleted; otherwise {@code false}
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

    /**
     * Deletes a subscriber only if the row role is {@code RestaurantAgent}.
     *
     * @param subscriberId subscriber identifier
     * @return {@code true} if a row was deleted; otherwise {@code false}
     */
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

    /**
     * Deletes a subscriber row by id.
     *
     * @param subscriberId subscriber identifier
     * @return {@code true} if a row was deleted; otherwise {@code false}
     */
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

    /**
     * Maps a {@link ResultSet} row to a {@link Subscriber}-typed instance based on the {@code role} column.
     *
     * @param rs result set positioned on a subscriber row
     * @return a role-specific subscriber entity
     * @throws SQLException if reading columns from the result set fails
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

    /**
     * Factory method that creates a role-specific subscriber entity.
     * <p>
     * If the role is {@code RestaurantManager} or {@code RestaurantAgent}, a specialized entity type is created.
     * Otherwise, a standard {@link Subscriber} instance is created and its role is set.
     * </p>
     *
     * @param id        subscriber identifier
     * @param username  username
     * @param firstName first name
     * @param lastName  last name
     * @param phone     phone
     * @param email     email
     * @param role      desired role
     * @return subscriber entity instance matching the role
     */
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
        s.setUserRole(role);
        return s;
    }

    /**
     * Retrieves a subscriber by the triplet of username, phone, and email.
     *
     * @param username username
     * @param phone    phone
     * @param email    email
     * @return subscriber entity if found; otherwise {@code null}
     */
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

    /**
     * Updates multiple subscriber fields using "keep existing if blank" semantics.
     * <p>
     * Each field is updated only if the incoming parameter is not {@code null} and not empty ({@code ""}).
     * </p>
     *
     * @param subscriberId subscriber identifier
     * @param username     new username or blank to keep existing
     * @param firstName    new first name or blank to keep existing
     * @param lastName     new last name or blank to keep existing
     * @param phone        new phone or blank to keep existing
     * @param email        new email or blank to keep existing
     * @return {@code true} if a row was updated; otherwise {@code false}
     * @throws SQLException if a database error occurs during update execution
     */
    public boolean updateSubscriberDetails(
            int subscriberId,
            String username,
            String firstName,
            String lastName,
            String phone,
            String email
    ) throws SQLException {

        String sql = """
                UPDATE SUBSCRIBERS
                SET
                    username   = COALESCE(NULLIF(?, ''), username),
                    first_name = COALESCE(NULLIF(?, ''), first_name),
                    last_name  = COALESCE(NULLIF(?, ''), last_name),
                    phone      = COALESCE(NULLIF(?, ''), phone),
                    email      = COALESCE(NULLIF(?, ''), email)
                WHERE subscriber_id = ?
                """;


        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, phone);
            stmt.setString(5, email);
            stmt.setInt(6, subscriberId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Counts subscribers who made at least one reservation during the given year/month.
     *
     * @param year  target year
     * @param month target month (1-12)
     * @return number of distinct subscribers that created reservations in that month
     * @throws SQLException if a database error occurs during query execution
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
     * Counts subscribers who did not make any reservation during the given year/month.
     *
     * @param year  target year
     * @param month target month (1-12)
     * @return number of subscribers with no reservations in that month
     * @throws SQLException if a database error occurs during query execution
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

    /**
     * Returns the maximum user id across {@code guests.guest_id} and {@code subscribers.subscriber_id}.
     * <p>
     * Used to generate a new unique user identifier in a single numeric id space.
     * </p>
     *
     * @return the maximum id found, or 0 if both tables are empty
     * @throws SQLException if a database error occurs during query execution
     */
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

            return 0;
        }
    }

    /**
     * Retrieves one restaurant manager id (first match) from {@code subscribers}.
     *
     * @return a subscriber_id whose role is {@code RestaurantManager}
     * @throws SQLException if no manager is found or if a database error occurs
     */
    public int getRestaurantManagerId() throws SQLException {
        String sql = """
            SELECT subscriber_id
            FROM subscribers
            WHERE role = 'RestaurantManager'
            LIMIT 1
        """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("subscriber_id");
            }
        }

        throw new SQLException("RestaurantManager not found");
    }

    /**
     * Creates a new login session id and inserts it into {@code login_sessions}.
     * <p>
     * This is used for a barcode/QR-style login flow where a session id is displayed
     * and later associated with a subscriber id.
     * </p>
     *
     * @return the generated short session id, or {@code null} on failure
     */
    public String createLoginSession() {
        String sessionId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String query = "INSERT INTO login_sessions (session_id) VALUES (?)";
        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, sessionId);
            ps.executeUpdate();
            return sessionId;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks whether a login session has been claimed by a subscriber.
     *
     * @param sessionId session identifier
     * @return subscriber id if present in {@code login_sessions}; otherwise {@code null}
     */
    public Integer checkSessionStatus(String sessionId) {
        String query = "SELECT subscriber_id FROM login_sessions WHERE session_id = ? AND subscriber_id IS NOT NULL";
        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("subscriber_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
