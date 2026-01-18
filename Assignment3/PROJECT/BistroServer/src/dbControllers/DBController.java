package dbControllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import application.RestaurantServer;

/**
 * Manages the application's JDBC connection to the MySQL database.
 * <p>
 * This class is responsible for establishing and holding a single
 * {@link Connection} instance used by the different DB controller classes. It
 * also provides lightweight diagnostics/logging to verify connectivity and
 * basic schema presence.
 * </p>
 * <p>
 * Logging is routed through {@link RestaurantServer} when available; otherwise
 * it falls back to standard output.
 * </p>
 */
public class DBController {

	/**
	 * MySQL password configured externally (e.g., via Server UI) before connecting.
	 */
	public static String MYSQL_PASSWORD = "";

	private Connection conn;

	/**
	 * Server reference used for logging to the server UI.
	 */
	private RestaurantServer server;

	/**
	 * Links this DBController instance to a specific server instance for logging.
	 *
	 * @param server server instance used for UI logging
	 */
	public void setServer(RestaurantServer server) {
		this.server = server;
	}

	/**
	 * Logs a message to the server UI if available; otherwise prints to console.
	 *
	 * @param msg message to log
	 */
	private void log(String msg) {
		if (server != null) {
			server.log(msg);
		} else {
			System.out.println(msg);
		}
	}

	/**
	 * Establishes a JDBC connection to the application's MySQL database.
	 * <p>
	 * On successful connection, this method runs diagnostic helpers that log:
	 * <ul>
	 * <li>Which database is currently selected</li>
	 * <li>Whether the {@code restaurant_tables} table exists</li>
	 * <li>The row count of {@code restaurant_tables} (if the table exists)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * On failure, connection error details are logged (message, SQL state, vendor
	 * code).
	 * </p>
	 */
	public void ConnectToDb() {
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false", "root",
					MYSQL_PASSWORD);

			log("SQL connection succeed");

			logCurrentDatabase();
			logIfRestaurantTablesExists();
			logRestaurantTablesRowCount();

		} catch (SQLException ex) {
			log("SQLException: " + ex.getMessage());
			log("SQLState: " + ex.getSQLState());
			log("VendorError: " + ex.getErrorCode());
		}
	}

	/**
	 * Returns the currently active JDBC connection.
	 *
	 * @return active {@link Connection} instance, or {@code null} if not connected
	 */
	public Connection getConnection() {
		return conn;
	}

	/**
	 * Logs the currently selected database name using {@code SELECT DATABASE()}.
	 */
	private void logCurrentDatabase() {
		if (conn == null)
			return;
		try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
			if (rs.next()) {
				log("Connected to DB: " + rs.getString(1));
			}
		} catch (SQLException e) {
			log("Failed to read current DB: " + e.getMessage());
		}
	}

	/**
	 * Logs whether the {@code restaurant_tables} table exists in the current
	 * database.
	 */
	private void logIfRestaurantTablesExists() {
		if (conn == null)
			return;
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SHOW TABLES LIKE 'restaurant_tables'")) {
			log("restaurant_tables exists? " + (rs.next() ? "YES" : "NO"));
		} catch (SQLException e) {
			log("Failed to check if restaurant_tables exists: " + e.getMessage());
		}
	}

	/**
	 * Logs the row count of the {@code restaurant_tables} table.
	 * <p>
	 * If the table does not exist yet, this query may fail and will be logged as a
	 * non-fatal diagnostic.
	 * </p>
	 */
	private void logRestaurantTablesRowCount() {
		if (conn == null)
			return;
		try (Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM restaurant_tables")) {
			if (rs.next()) {
				log("restaurant_tables row count: " + rs.getInt("c"));
			}
		} catch (SQLException e) {
			log("Could not read restaurant_tables row count: " + e.getMessage());
		}
	}

}
