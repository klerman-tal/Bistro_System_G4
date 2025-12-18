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
                    MYSQL_PASSWORD   // Uses password provided by the user
            );

            log("SQL connection succeed");

        } catch (SQLException ex) {
            log("SQLException: " + ex.getMessage());
            log("SQLState: " + ex.getSQLState());
            log("VendorError: " + ex.getErrorCode());
        }
    }
    
    
    public Connection getConnection() {
        return conn;
    }


	
}
