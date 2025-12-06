// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Platform;
import ocsf.server.*;

public class RestaurantServer extends AbstractServer {

    // ===================== Class variables =====================

    /**
     * The default port to listen on.
     */
    public static final int DEFAULT_PORT = 5556;

    DBController conn;

    // Reference to the GUI window (server log screen)
    private gui.ServerGUIController uiController;

    // ===================== GUI logging support =====================

    // Attach the UI controller to this server instance
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    // Unified log method: prints to console and to the GUI (if available)
    void log(String msg) {   // not 'private' so DBController can use it
        System.out.println(msg);

        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // ===================== Constructors =====================

    /**
     * Constructs an instance of the restaurant server.
     *
     * @param port The port number to connect on.
     */
    public RestaurantServer(int port) {
        super(port);
        conn = new DBController();
        conn.setServer(this);   // Allow DBController to forward logs to server UI
    }

    // ===================== Instance methods =====================

    /**
     * Handles any messages received from the client.
     *
     * @param msg    The message received from the client.
     * @param client The connection from which the message originated.
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("Message received: " + msg + " from " + client);

        try {
            if (msg instanceof ArrayList) {
                ArrayList<?> arr = (ArrayList<?>) msg;
                if (arr.isEmpty()) return;

                String command = arr.get(0).toString();
                log("Command = " + command);

                switch (command) {

                    case "PRINT_ORDERS": {
                        // Retrieve all orders and send them to the client
                        ArrayList<String> lines = conn.getOrdersForClient();
                        for (String line : lines) {
                            client.sendToClient(line);
                        }
                        break;
                    }

                    case "UPDATE_DATE": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        String dateStr = arr.get(2).toString(); // yyyy-mm-dd
                        java.sql.Date newDate = java.sql.Date.valueOf(dateStr);

                        conn.UpdateOrderDate(orderNumber, newDate);
                        client.sendToClient("Order " + orderNumber + " date updated to " + dateStr);
                        log("Updated date for order " + orderNumber + " -> " + dateStr);
                        break;
                    }

                    case "UPDATE_GUESTS": {
                        int orderNumber = Integer.parseInt(arr.get(1).toString());
                        int newGuests = Integer.parseInt(arr.get(2).toString());

                        conn.UpdateNumberOfGuests(orderNumber, newGuests);
                        client.sendToClient("Order " + orderNumber + " guest count updated to " + newGuests);
                        log("Updated guests for order " + orderNumber + " -> " + newGuests);
                        break;
                    }

                    default:
                        // Fallback: unknown command → send echo
                        log("Unknown command, sending echo to all clients");
                        sendToAllClients(msg);
                        break;
                }
            } else {
                // Non-ArrayList messages → treat as regular echo
                log("Non-ArrayList message, echoing to all clients");
                sendToAllClients(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Error while handling command: " + e.getMessage();
            log(errorMsg);
            try {
                client.sendToClient(errorMsg);
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
                log("Error sending error message to client: " + ioEx.getMessage());
            }
        }
    }

    /**
     * Called when the server starts listening for connections.
     */
    @Override
    protected void serverStarted() {
        log("Server listening for connections on port " + getPort());
        conn.ConnectToDb();
    }

    /**
     * Called when the server stops listening for connections.
     */
    @Override
    protected void serverStopped() {
        log("Server has stopped listening for connections.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        String status = "CONNECTED";

        log("Client connected | IP: " + ip + " | Host: " + host + " | Status: " + status);
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        String status = "DISCONNECTED";

        log("Client disconnected | IP: " + ip + " | Host: " + host + " | Status: " + status);
    }

    // ===================== Main method (no GUI) =====================

    /**
     * Responsible for creating the server instance
     * when running without a GUI.
     *
     * @param args[0] The port number to listen on. Defaults to 5556
     *                if no argument is entered.
     */
    public static void main(String[] args) {
        int port = 0; // Port to listen on

        try {
            port = Integer.parseInt(args[0]); // Get port from command line
        } catch (Throwable t) {
            port = DEFAULT_PORT; // Default to 5556
        }

        RestaurantServer sv = new RestaurantServer(port);

        try {
            sv.listen(); // Start listening for connections
        } catch (Exception ex) {
            sv.log("ERROR - Could not listen for clients! " + ex.getMessage());
        }
    }
}
//End of RestaurantServer class
