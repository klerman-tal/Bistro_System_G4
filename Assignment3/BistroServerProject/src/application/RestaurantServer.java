package application;

import java.sql.Connection;

import dbControllers.Restaurant_DB_Controller;
import logicControllers.RestaurantController;
import entities.Restaurant;
import entities.Table;

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
import java.sql.Connection;

import dbControllers.Restaurant_DB_Controller;
import entities.Restaurant;
import entities.Table;
import logicControllers.RestaurantController;


public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    // Main DB controller
    private DBController conn;
    private RestaurantController restaurantController;

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
        try {
            // צריך לקבל Connection מתוך DBController
            Connection sqlConn = conn.getConnection();  // אם אין מתודה כזו - תוסיפי (סעיף 4)
            Restaurant_DB_Controller rdb = new Restaurant_DB_Controller(sqlConn);
            restaurantController = new RestaurantController(rdb);

            log("RestaurantController initialized.");
        } catch (Exception e) {
            log("Failed to init RestaurantController: " + e.getMessage());
        }


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
                    case "RM_GET_TABLES": {
                        try {
                            if (restaurantController == null) {
                                client.sendToClient("RM_ERROR|RestaurantController not initialized");
                                break;
                            }

                            restaurantController.loadTablesFromDb();

                            StringBuilder sb = new StringBuilder("RM_TABLES|");
                            for (Table t : Restaurant.getInstance().getTables()) {
                                sb.append(t.getTableNumber()).append(",")
                                  .append(t.getSeatsAmount()).append(",")
                                  .append(t.getIsAvailable() ? 1 : 0)
                                  .append(";");
                            }

                            client.sendToClient(sb.toString());
                        } catch (Exception e) {
                            client.sendToClient("RM_ERROR|" + e.getMessage());
                        }
                        break;
                    }

                    case "RM_SAVE_TABLE": {
                        try {
                            if (restaurantController == null) {
                                client.sendToClient("RM_ERROR|RestaurantController not initialized");
                                break;
                            }

                            int num = Integer.parseInt(arr.get(1).toString());
                            int seats = Integer.parseInt(arr.get(2).toString());
                            boolean available = Boolean.parseBoolean(arr.get(3).toString());

                            Table t = new Table();
                            t.setTableNumber(num);
                            t.setSeatsAmount(seats);
                            t.setIsAvailable(available);

                            // UPSERT ל-DB (יש לך ON DUPLICATE KEY UPDATE)
                            // הכי נקי: להוסיף מתודה בלוגיקה שמשתמשת ב-db.saveTable
                            restaurantController.saveOrUpdateTable(t);  // נוסיף עכשיו מתודה קטנה (סעיף 2)

                            client.sendToClient("RM_OK|SAVED_OR_UPDATED");

                        } catch (Exception e) {
                            client.sendToClient("RM_ERROR|" + e.getMessage());
                        }
                        break;
                    }


                    case "RM_TOGGLE_AVAILABILITY": {
                        try {
                            if (restaurantController == null) {
                                client.sendToClient("RM_ERROR|RestaurantController not initialized");
                                break;
                            }

                            int num = Integer.parseInt(arr.get(1).toString());

                            Table found = null;
                            for (Table t : Restaurant.getInstance().getTables()) {
                                if (t.getTableNumber() == num) { found = t; break; }
                            }
                            if (found == null) {
                                client.sendToClient("RM_ERROR|Table not found");
                                break;
                            }

                            found.setIsAvailable(!found.getIsAvailable());
                            // עדכון DB – הכי פשוט כרגע
                            restaurantController.syncAllTablesToDb();

                            client.sendToClient("RM_OK|TOGGLED");

                        } catch (Exception e) {
                            client.sendToClient("RM_ERROR|" + e.getMessage());
                        }
                        break;
                    }

                    case "RM_DELETE_TABLE": {
                        try {
                            if (restaurantController == null) {
                                client.sendToClient("RM_ERROR|RestaurantController not initialized");
                                break;
                            }

                            int num = Integer.parseInt(arr.get(1).toString());
                            boolean ok = restaurantController.removeTable(num);

                            if (!ok) client.sendToClient("RM_ERROR|Table not found");
                            else client.sendToClient("RM_OK|DELETED");

                        } catch (Exception e) {
                            client.sendToClient("RM_ERROR|" + e.getMessage());
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
