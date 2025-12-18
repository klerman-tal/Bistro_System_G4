package dbControllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import application.RestaurantServer;

public class DBController {

    // User-provided MySQL password (set in ServerUI)
    public static String MYSQL_PASSWORD = "";

    private Connection conn;

    // Reference to the server to enable logging into the server UI
    private RestaurantServer server;

    // Link this DBController instance to a specific server instance
    public void setServer(RestaurantServer server) {
        this.server = server;
    }

    // Central logging function:
    // If server exists -> log to UI, otherwise log to console
    private void log(String msg) {
        if (server != null) {
            server.log(msg);
        } else {
            System.out.println(msg);
        }
    }

    // Establish a connection to the MySQL database
    public void ConnectToDb() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false",
                    "root",
                    MYSQL_PASSWORD
            );
 
            log("SQL connection succeed");

            // ===== DEBUG / VERIFY (new) =====
            logCurrentDatabase();                 // prints which DB we're connected to
            logIfRestaurantTablesExists();        // prints YES/NO
            ensureRestaurantTablesTableExists();  // creates only if missing
            logRestaurantTablesRowCount();        // prints row count

        } catch (SQLException ex) {
            log("SQLException: " + ex.getMessage());
            log("SQLState: " + ex.getSQLState());
            log("VendorError: " + ex.getErrorCode());
        }
    }

    public Connection getConnection() {
        return conn;
    }

    // ==========================
    // NEW HELPERS (diagnostics)
    // ==========================
    private void logCurrentDatabase() {
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            if (rs.next()) {
                log("Connected to DB: " + rs.getString(1));
            }
        } catch (SQLException e) {
            log("Failed to read current DB: " + e.getMessage());
        }
    }

    private void logIfRestaurantTablesExists() {
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SHOW TABLES LIKE 'restaurant_tables'")) {
            log("restaurant_tables exists? " + (rs.next() ? "YES" : "NO"));
        } catch (SQLException e) {
            log("Failed to check if restaurant_tables exists: " + e.getMessage());
        }
    }

    private void logRestaurantTablesRowCount() {
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM restaurant_tables")) {
            if (rs.next()) {
                log("restaurant_tables row count: " + rs.getInt("c"));
            }
        } catch (SQLException e) {
            // If table doesn't exist yet, this can fail. That's ok.
            log("Could not read restaurant_tables row count: " + e.getMessage());
        }
    }

	
}

