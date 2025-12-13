package application;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import dbControllers.DBController;

import javafx.application.Platform;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    // Main DB controller (manages SQL connection)
    private DBController conn;

    // GUI controller for server window
    private gui.ServerGUIController uiController;

    private String serverIp;

    // Allow UI controller to attach to server for logging
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    // Unified logging method (console + GUI)
    public void log(String msg) {
        System.out.println(msg);
        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // Server constructor
    public RestaurantServer(int port) {
        super(port);

        conn = new DBController();
        conn.setServer(this); // Attach server for logging

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "UNKNOWN";
            log("Could not determine server IP: " + e.getMessage());
        }
    }

    @Override
    protected void serverStarted() {
        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        // Open SQL connection
        conn.ConnectToDb();

        log("Database connection initialized.");
    }

    @Override
    protected void serverStopped() {
        log("Server has stopped listening.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
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
        // No extra logic needed for now
    }

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
                        var orders = conn.getOrdersForClient();
                        for (String order : orders)
                            client.sendToClient(order);
                        break;
                    }

                    case "UPDATE_DATE": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        String dateStr = arr.get(2).toString();
                        java.sql.Date newDate = java.sql.Date.valueOf(dateStr);

                        conn.UpdateOrderDate(orderNumber, newDate);
                        client.sendToClient("Order " + orderNumber +
                               " date updated to " + dateStr);

                        break;
                    }

                    case "UPDATE_GUESTS": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        int guests = Integer.parseInt(arr.get(2).toString());

                        conn.UpdateNumberOfGuests(orderNumber, guests);
                        client.sendToClient("Order " + orderNumber +
                               " guest count updated to " + guests);

                        break;
                    }

                    case "CLIENT_LOGOUT": {
                        String clientIp = "UNKNOWN";
                        InetAddress addr = client.getInetAddress();

                        if (addr != null)
                            clientIp = addr.getHostAddress();

                        log("Client logout | Client IP: " + clientIp);

                        try {
                            client.close();
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
            log("Error in handleMessageFromClient: " + e.getMessage());
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
            server.listen();
        } catch (Exception e) {
            server.log("ERROR - Could not listen for clients!");
        }
    }
}
