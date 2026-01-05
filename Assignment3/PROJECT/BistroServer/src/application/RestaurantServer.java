package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;

import dbControllers.DBController;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.User_DB_Controller;

import entities.OpeningHouers;
import entities.Restaurant;
import entities.Table;
import entities.User;

import javafx.application.Platform;

import logicControllers.RestaurantController;
import logicControllers.UserController;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    private DBController conn;

    private RestaurantController restaurantController;
    private User_DB_Controller userDB;
    private UserController userController;
    private Reservation_DB_Controller reservationDB;

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

    @Override
    protected void serverStarted() {
        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        conn.ConnectToDb();

        try {
            Connection sqlConn = conn.getConnection();
            if (sqlConn == null) {
                log("DB connection failed.");
                return;
            }

            Restaurant_DB_Controller rdb = new Restaurant_DB_Controller(sqlConn);
            restaurantController = new RestaurantController(rdb);

            reservationDB = new Reservation_DB_Controller(sqlConn);
            reservationDB.createReservationsTable();


            userDB = new User_DB_Controller(sqlConn);
            userDB.createSubscribersTable();
            userDB.createGuestsTable();

            userController = new UserController(userDB);

            log("All controllers and database tables initialized.");

            // ✅ (3) Always keep next 30 days + cleanup + schema ensure
            try {
                restaurantController.initAvailabilityGridNext30Days();
                log("Availability grid ensured and initialized for the next 30 days.");
            } catch (Exception e) {
                log("Grid init failed: " + e.getMessage());
            }

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

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {

        try {
            if (!(msg instanceof ArrayList<?>)) return;

            ArrayList<?> arr = (ArrayList<?>) msg;
            if (arr.isEmpty()) return;

            String command = arr.get(0).toString();

            switch (command) {



                case "CLIENT_LOGOUT": {
                    client.close();
                    break;
                }

                // =========================
                // TABLES CRUD
                // =========================

                case "RM_GET_TABLES": {
                    restaurantController.loadTablesFromDb();

                    StringBuilder sb = new StringBuilder("RM_TABLES|");
                    for (Table t : Restaurant.getInstance().getTables()) {
                        sb.append(t.getTableNumber()).append(",")
                          .append(t.getSeatsAmount())
                          .append(";");
                    }
                    client.sendToClient(sb.toString());
                    break;
                }

                case "RM_SAVE_TABLE": {
                    int num = Integer.parseInt(arr.get(1).toString());
                    int seats = Integer.parseInt(arr.get(2).toString());

                    Table t = new Table();
                    t.setTableNumber(num);
                    t.setSeatsAmount(seats);

                    restaurantController.saveOrUpdateTable(t);

                    // ✅ after adding a table, ensure grid schema & keep month ahead
                    try {
                        restaurantController.initAvailabilityGridNext30Days();
                    } catch (Exception ignored) {}

                    client.sendToClient("RM_OK|");
                    break;
                }

                case "RM_DELETE_TABLE": {
                    int num = Integer.parseInt(arr.get(1).toString());
                    boolean ok = restaurantController.removeTable(num);

                    client.sendToClient(ok ? "RM_OK|" : "RM_ERROR|Delete failed");
                    break;
                }

                // =========================
                // OPENING HOURS
                // =========================

                case "RM_GET_OPENING_HOURS": {
                    restaurantController.loadOpeningHoursFromDb();

                    StringBuilder sb = new StringBuilder("RM_OPENING_HOURS|");
                    for (OpeningHouers oh : Restaurant.getInstance().getOpeningHours()) {
                        String day = oh.getDayOfWeek();
                        String open = safeHHMM(oh.getOpenTime());
                        String close = safeHHMM(oh.getCloseTime());

                        sb.append(day).append(",")
                          .append(open).append(",")
                          .append(close)
                          .append(";");
                    }

                    client.sendToClient(sb.toString());
                    break;
                }

                case "RM_UPDATE_OPENING_HOURS": {
                    String day = arr.get(1).toString();
                    String open = arr.get(2).toString();
                    String close = arr.get(3).toString();

                    OpeningHouers oh = new OpeningHouers();
                    oh.setDayOfWeek(day);
                    oh.setOpenTime(open);
                    oh.setCloseTime(close);

                    restaurantController.updateOpeningHours(oh);

                    // ✅ hours changed -> rebuild month ahead for correct ranges
                    try {
                        restaurantController.initAvailabilityGridNext30Days();
                    } catch (Exception ignored) {}

                    client.sendToClient("RM_OK|");
                    break;
                }

                // =========================
                // AVAILABILITY GRID (DB) - WITH 4 ADDITIONS
                // =========================

                // Force init month ahead (manual)
                case "RM_INIT_AVAILABILITY_MONTH": {
                    restaurantController.initAvailabilityGridNext30Days();
                    client.sendToClient("RM_OK|");
                    break;
                }

                // ✅ (1) Get grid from DB for a date
                // expected: ["RM_GET_GRID_FROM_DB", "YYYY-MM-DD"]
                case "RM_GET_GRID_FROM_DB": {
                    String dateStr = arr.get(1).toString();
                    java.time.LocalDate date = java.time.LocalDate.parse(dateStr);

                    String payload = restaurantController.getGridFromDbPayload(date);
                    client.sendToClient("RM_GRID|" + payload);
                    break;
                }

                // ✅ (4) Safe reserve (conditional update)
                // expected: ["RM_TRY_RESERVE_SLOT", "YYYY-MM-DDTHH:MM", "<tableNumber>"]
                case "RM_TRY_RESERVE_SLOT": {
                    java.time.LocalDateTime slot = java.time.LocalDateTime.parse(arr.get(1).toString());
                    int tableNum = Integer.parseInt(arr.get(2).toString());

                    boolean success = restaurantController.tryReserveSlot(slot, tableNum);
                    client.sendToClient(success ? "RM_OK|" : "RM_TAKEN|");
                    break;
                }

                // Release slot (set free)
                // expected: ["RM_RELEASE_SLOT", "YYYY-MM-DDTHH:MM", "<tableNumber>"]
                case "RM_RELEASE_SLOT": {
                    java.time.LocalDateTime slot = java.time.LocalDateTime.parse(arr.get(1).toString());
                    int tableNum = Integer.parseInt(arr.get(2).toString());

                    boolean ok = restaurantController.releaseSlot(slot, tableNum);
                    client.sendToClient(ok ? "RM_OK|" : "RM_ERROR|Release failed");
                    break;
                }

                default:
                    log("Unknown command: " + command);
            }

        } catch (Exception e) {
            log("Error handling message: " + e.getMessage());
            try {
                client.sendToClient("RM_ERROR|" + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    private String safeHHMM(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.matches("^\\d{2}:\\d{2}:\\d{2}$")) return t.substring(0, 5);
        if (t.matches("^\\d{2}:\\d{2}$")) return t;
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

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
