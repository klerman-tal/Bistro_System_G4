package application;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import dbControllers.DBController;
import dbControllers.User_DB_Controller;
import logicControllers.UserController;

import javafx.application.Platform;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    // Main DB controller
    private DBController conn;

    // User controllers
    private User_DB_Controller userDB;
    private UserController userController;

    // GUI
    private gui.ServerGUIController uiController;

    private String serverIp;

    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    public void log(String msg) {
        System.out.println(msg);
        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // Constructor
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
        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        // Connect to DB
        conn.ConnectToDb();

        // Create DB controllers for users
        userDB = new User_DB_Controller(conn.getConnection());

        // Create logic controllers
        userController = new UserController(userDB);

        log("User controllers initialized.");
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
        // nothing yet
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {

        log("Message received: " + msg + " from " + client);

        try {
            if (msg instanceof ArrayList<?>) {

                ArrayList<?> arr = (ArrayList<?>) msg;
                if (arr.isEmpty()) return;

                String command = arr.get(0).toString().trim();
                log("Command = '" + command + "'");

                switch (command) {

                    // =====================
                    //        LOGIN
                    // =====================
                    case "LOGIN": {
                        log("Handling LOGIN request...");

                        Object response = userController.login(arr);

                        client.sendToClient(response);
                        break;
                    }

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
                        log("Unknown command received: " + command);
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
