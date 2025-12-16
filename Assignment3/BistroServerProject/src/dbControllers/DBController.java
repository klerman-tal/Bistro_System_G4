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


    // Retrieve all orders from the database and format them for the client
    public ArrayList<String> getOrdersForClient() {
        ArrayList<String> result = new ArrayList<>();

        // Header rows sent to the client before the data
        String header = "Order number | Order date | Number of guests | Confirmation code | Subscriber ID | Date of placing order";
        result.add(header);
        result.add("-----------------------------------------------------------------------------------------------------");

        String sql = "SELECT * FROM orders";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;

            while (rs.next()) {
                int orderNumber = rs.getInt("order_number");
                Date orderDate = rs.getDate("order_date");
                int numGuests = rs.getInt("number_of_guests");
                int confirmationCode = rs.getInt("confirmation_code");
                int subscriberId = rs.getInt("subscriber_id");
                Date placingDate = rs.getDate("date_of_placing_order");

                // Format each database row for output
                String line = orderNumber + " | " + orderDate + " | " + numGuests
                        + " | " + confirmationCode + " | " + subscriberId + " | " + placingDate;

                result.add(line);
                count++;
            }

            log("getOrdersForClient: fetched " + count + " orders from DB");

        } catch (SQLException e) {
            String err = "ERROR while reading orders: " + e.getMessage();
            log(err);
            result.add(err);
        }

        return result;
    }

    // Update the order_date field for a specific order
    public void UpdateOrderDate(int orderNumber, Date newDate) {

        String sql = "UPDATE orders SET order_date = ? WHERE order_number = ?";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);

            // Convert java.util.Date to java.sql.Date (ensures correct SQL format)
            java.sql.Date sqlDate = new java.sql.Date(newDate.getTime());

            stmt.setDate(1, sqlDate);
            stmt.setInt(2, orderNumber);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0)
                log("Order date updated successfully for order " + orderNumber +
                        " to " + sqlDate.toString());
            else
                log("No order found with order number " + orderNumber);

        } catch (SQLException ex) {
            log("Error in UpdateOrderDate: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Update the number_of_guests field for a given order
    public void UpdateNumberOfGuests(int orderNumber, int newGuestsCount) {

        String sql = "UPDATE orders SET number_of_guests = ? WHERE order_number = ?";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, newGuestsCount);
            stmt.setInt(2, orderNumber);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0)
                log("Number of guests updated successfully for order " +
                        orderNumber + " to " + newGuestsCount);
            else
                log("No order found with order number " + orderNumber);

        } catch (SQLException ex) {
            log("Error in UpdateNumberOfGuests: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
        
              
      


	

