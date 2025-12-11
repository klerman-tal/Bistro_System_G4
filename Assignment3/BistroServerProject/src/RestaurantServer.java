import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javafx.application.Platform;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    private DBController conn;
    private gui.ServerGUIController uiController;
    private String serverIp;

    // Assign GUI controller to allow logging to the UI
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    // Log message to console and UI (JavaFX thread-safe)
    void log(String msg) {
        System.out.println(msg);
        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // Constructor: initialize DB connection and detect server IP
    public RestaurantServer(int port) {
        super(port);

        conn = new DBController();
        conn.setServer(this);

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "UNKNOWN";
            log("Could not determine server IP: " + e.getMessage());
        }
    }

    @Override
    protected void serverStarted() {
        // Called when server successfully starts listening
        log("Server started on IP: " + serverIp);
        log("Server listening for connections on port " + getPort());
        conn.ConnectToDb();
    }

    @Override
    protected void serverStopped() {
        // Called when server stops listening
        log("Server has stopped listening for connections.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        // Triggered when a new client connects
        String clientIp = "UNKNOWN";
        InetAddress addr = client.getInetAddress();

        if (addr != null)
            clientIp = addr.getHostAddress();

        log("Client connected | Client IP: " + clientIp +
            " | Server IP: " + serverIp +
            " | Status: CONNECTED");
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        // No action needed on disconnect
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        // Main message handler for all client requests
        log("Message received: " + msg + " from " + client);

        try {
            if (msg instanceof ArrayList) {

                ArrayList<?> arr = (ArrayList<?>) msg;
                if (arr.isEmpty()) return;

                String command = arr.get(0).toString().trim();
                log("Command = '" + command + "'");

                switch (command) {

                    case "PRINT_ORDERS": {
                        // Send all orders back to the client
                        ArrayList<String> orders = conn.getOrdersForClient();
                        for (String order : orders)
                            client.sendToClient(order);
                        break;
                    }

                    case "UPDATE_DATE": {
                        // Update order date
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        String dateStr = arr.get(2).toString();
                        java.sql.Date newDate = java.sql.Date.valueOf(dateStr);

                        conn.UpdateOrderDate(orderNumber, newDate);
                        client.sendToClient("Order " + orderNumber + " date updated to " + dateStr);

                        log("Updated date for order " + orderNumber);
                        break;
                    }

                    case "UPDATE_GUESTS": {
                        // Update number of guests for an order
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        int guests = Integer.parseInt(arr.get(2).toString());

                        conn.UpdateNumberOfGuests(orderNumber, guests);
                        client.sendToClient("Order " + orderNumber + " guest count updated to " + guests);

                        log("Updated guests for order " + orderNumber);
                        break;
                    }

                    case "CLIENT_LOGOUT": {
                        // Handle client logout request
                        String clientIp = "UNKNOWN";
                        InetAddress addr = client.getInetAddress();

                        if (addr != null)
                            clientIp = addr.getHostAddress();

                        log("Client requested logout | Client IP: " + clientIp +
                            " | Server IP: " + serverIp +
                            " | Status: DISCONNECTED");

                        try {
                            client.close();
                        } catch (IOException e) {
                            log("Error closing client: " + e.getMessage());
                        }
                        break;
                    }

                    default:
                        // Unknown command type
                        log("Unknown command received");
                        break;
                }
            }

        } catch (Exception e) {
            log("Error while handling command: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Throwable t) {
            port = DEFAULT_PORT;
        }

        RestaurantServer server = new RestaurantServer(port);

        try {
            server.listen(); // Start listening for clients
        } catch (Exception e) {
            server.log("ERROR - Could not listen for clients!");
        }
    }
}
