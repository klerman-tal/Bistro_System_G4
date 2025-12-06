import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBController {

    private Connection conn;

    // Reference to the server so we can send log messages to the UI window
    private RestaurantServer server;

    // Links this DBController instance to the server
    public void setServer(RestaurantServer server) {
        this.server = server;
    }

    // Unified log function: 
    // If ServerUI exists → write to UI, otherwise write to console
    private void log(String msg) {
        if (server != null) {
            server.log(msg);   // Calls the server's log method
        } else {
            System.out.println(msg);
        }
    }

    // Establish connection to the MySQL database
    public void ConnectToDb() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bistrodb?serverTimezone=Asia/Jerusalem&useSSL=false",
                    "root",
                    "Liem010799"
            );

            log("SQL connection succeed");

        } catch (SQLException ex) {
            log("SQLException: " + ex.getMessage());
            log("SQLState: " + ex.getSQLState());
            log("VendorError: " + ex.getErrorCode());
        }
    }

    // Retrieve all orders and format them for the client output
    public ArrayList<String> getOrdersForClient() {
        ArrayList<String> result = new ArrayList<>();

        // Table header for the client
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

                // Formatting each row
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

            // Convert java.util.Date → java.sql.Date
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

    // Update the number_of_guests field for a specific order
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
