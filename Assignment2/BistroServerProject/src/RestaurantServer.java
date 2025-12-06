// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javafx.application.Platform;
import ocsf.server.*;

public class RestaurantServer extends AbstractServer {

    // ===================== Class variables =====================

    /**
     * The default port to listen on.
     */
    public static final int DEFAULT_PORT = 5556;

    private DBController conn;

    // Reference to the GUI window (server log screen)
    private gui.ServerGUIController uiController;

    // Server IP (host machine)
    private String serverIp;

    // ===================== GUI logging support =====================

    // Attach the UI controller to this server instance
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    // Unified log method: prints to console and to the GUI (if available)
    void log(String msg) {
        System.out.println(msg);

        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // ===================== Constructors =====================

    /**
     * Constructs an instance of the restaurant server.
     */
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

    // ===================== Message handling =====================

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("Message received: " + msg + " from " + client);

        try {
            if (msg instanceof ArrayList) {
                ArrayList<?> arr = (ArrayList<?>) msg;
                if (arr.isEmpty()) return;

                String command = arr.get(0).toString().trim();
                log("Command = '" + command + "'");

                switch (command) {

                    case "PRINT_ORDERS": {
                        ArrayList<String> orders = conn.getOrdersForClient();
                        for (String order : orders) {
                            client.sendToClient(order);
                        }
                        break;
                    }

                    case "UPDATE_DATE": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        String dateStr = arr.get(2).toString();
                        java.sql.Date newDate = java.sql.Date.valueOf(dateStr);

                        conn.UpdateOrderDate(orderNumber, newDate);
                        client.sendToClient(
                            "Order " + orderNumber + " date updated to " + dateStr
                        );
                        log("Updated date for order " + orderNumber);
                        break;
                    }

                    case "UPDATE_GUESTS": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        int guests = Integer.parseInt(arr.get(2).toString());

                        conn.UpdateNumberOfGuests(orderNumber, guests);
                        client.sendToClient(
                            "Order " + orderNumber + " guest count updated to " + guests
                        );
                        log("Updated guests for order " + orderNumber);
                        break;
                    }

                    case "CLIENT_LOGOUT": {
                        String clientIp = "UNKNOWN";
                        InetAddress addr = client.getInetAddress();
                        if (addr != null) {
                            clientIp = addr.getHostAddress();
                        }

                        log("Client requested logout | Client IP: " + clientIp +
                            " | Server IP: " + serverIp +
                            " | Status: DISCONNECTED");

                        try {
                            client.close(); // graceful disconnect
                        } catch (IOException e) {
                            log("Error closing client: " + e.getMessage());
                        }
                        break;
                    }

                    default:
                        log("Unknown command received");
                        break;
                }
            }
        } catch (Exception e) {
            log("Error while handling command: " + e.getMessage());
        }
    }

    // ===================== Server lifecycle =====================

    @Override
    protected void serverStarted() {
        log("Server listening for connections on port " + getPort());
        conn.ConnectToDb();
    }

    @Override
    protected void serverStopped() {
        log("Server has stopped listening for connections.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        String clientIp = "UNKNOWN";
        InetAddress addr = client.getInetAddress();
        if (addr != null) {
            clientIp = addr.getHostAddress();
        }

        log("Client connected | Client IP: " + clientIp +
            " | Server IP: " + serverIp +
            " | Status: CONNECTED");
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        // intentionally left blank
        // disconnect already handled explicitly via CLIENT_LOGOUT
    }

    // ===================== Main =====================

    public static void main(String[] args) {
        int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (Throwable t) {
            port = DEFAULT_PORT;
        }

        RestaurantServer server = new RestaurantServer(port);

        try {
            server.listen();
        } catch (Exception e) {
            server.log("ERROR - Could not listen for clients!");
        }
    }
}
