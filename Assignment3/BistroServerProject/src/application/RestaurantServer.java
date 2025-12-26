package application;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;

import dbControllers.DBController;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.User_DB_Controller;

import entities.Restaurant;
import entities.Table;
import entities.User;

import javafx.application.Platform;

import logicControllers.RestaurantController;
import logicControllers.UserController;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class RestaurantServer extends AbstractServer {
	//Tal test
	//Tal test 2
	//Tal test 3
	//Tal test 4
	//Lior test 5
    public static final int DEFAULT_PORT = 5556;

    // DB
    private DBController conn;

    // Controllers
    private RestaurantController restaurantController;
    private User_DB_Controller userDB;
    private UserController userController;
    private Reservation_DB_Controller reservationDB;

    // GUI (Server side)
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

    // =====================
    // Constructor
    // =====================
    public RestaurantServer(int port) {
        super(port);

        conn = new DBController();
        conn.setServer(this);

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "UNKNOWN";
        }
    }

    // =====================
    // Server lifecycle
    // =====================
    @Override
    protected void serverStarted() {

        // Log basic server info
        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        // Step 1: connect to the database
        conn.ConnectToDb();

        try {
            // Step 2: get SQL connection object
            Connection sqlConn = conn.getConnection();
            if (sqlConn == null) {
                log("DB connection failed.");
                return;
            }

            // Step 3: initialize restaurant DB & logic
            Restaurant_DB_Controller rdb = new Restaurant_DB_Controller(sqlConn);
            restaurantController = new RestaurantController(rdb);

            // Step 4: initialize reservation DB and create its tables
            reservationDB = new Reservation_DB_Controller(sqlConn);
            reservationDB.createReservationsTable();
            reservationDB.createWaitingListTable();

            // Step 5: initialize user DB and create user tables
            userDB = new User_DB_Controller(sqlConn);
            userDB.createSubscribersTable();   // SUBSCRIBERS table
            userDB.createGuestsTable();        // GUESTS table

            // Step 6: initialize user logic controller
            userController = new UserController(userDB);

            // Step 7: final log
            log("All controllers and database tables initialized.");

        } catch (Exception e) {
            log("Initialization error: " + e.getMessage());
        }
    }


    @Override
    protected void serverStopped() {
        log("Server stopped.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {

        String ip = client.getInetAddress() != null
                ? client.getInetAddress().getHostAddress()
                : "UNKNOWN";

        log("Client connected | IP: " + ip);
    }

    // =====================
    // Message handler
    // =====================
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {

        try {
            if (!(msg instanceof ArrayList<?>)) return;

            ArrayList<?> arr = (ArrayList<?>) msg;
            if (arr.isEmpty()) return;

            String command = arr.get(0).toString();

            switch (command) {

                // =====================
                // LOGIN
                // =====================
                case "LOGIN": {

                    // arr = ["LOGIN", username, password]
                    String username = arr.get(1).toString();
                    String password = arr.get(2).toString();

                    User user = userController.login(username, password);

                    client.sendToClient(user);
                    break;
                }

                // =====================
                // CLIENT LOGOUT
                // =====================
                case "CLIENT_LOGOUT": {
                    client.close();
                    break;
                }

                // =====================
                // RESTAURANT MANAGER
                // =====================
                case "RM_GET_TABLES": {

                    restaurantController.loadTablesFromDb();

                    StringBuilder sb = new StringBuilder("RM_TABLES|");
                    for (Table t : Restaurant.getInstance().getTables()) {
                        sb.append(t.getTableNumber()).append(",")
                          .append(t.getSeatsAmount()).append(",")
                          .append(t.getIsAvailable() ? 1 : 0)
                          .append(";");
                    }

                    client.sendToClient(sb.toString());
                    break;
                }

                case "RM_SAVE_TABLE": {

                    int num = Integer.parseInt(arr.get(1).toString());
                    int seats = Integer.parseInt(arr.get(2).toString());
                    boolean available = Boolean.parseBoolean(arr.get(3).toString());

                    Table t = new Table();
                    t.setTableNumber(num);
                    t.setSeatsAmount(seats);
                    t.setIsAvailable(available);

                    restaurantController.saveOrUpdateTable(t);
                    client.sendToClient("RM_OK");
                    break;
                }

                case "RM_DELETE_TABLE": {

                    int num = Integer.parseInt(arr.get(1).toString());
                    boolean ok = restaurantController.removeTable(num);

                    client.sendToClient(ok ? "RM_OK" : "RM_ERROR");
                    break;
                }

                default:
                    log("Unknown command: " + command);
            }

        } catch (Exception e) {
            log("Error handling message: " + e.getMessage());
        }
    }

    // =====================
    // Main
    // =====================
    public static void main(String[] args) {

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            port = DEFAULT_PORT;
        }

        RestaurantServer server = new RestaurantServer(port);

        try {
            server.listen();
        } catch (Exception e) {
            server.log("ERROR: Could not start server");
        }
    }
}
