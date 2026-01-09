package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dbControllers.DBController;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.User_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import dto.RequestDTO;
import entities.OpeningHouers;
import entities.Restaurant;
import entities.Table;
import entities.User;

import javafx.application.Platform;
import logicControllers.ReportsController;
import logicControllers.ReservationController;
import logicControllers.RestaurantController;
import logicControllers.UserController;
import network.RequestRouter;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import protocol.Commands;
import network.*;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    private DBController conn;
    private Restaurant_DB_Controller restaurantDB;
    private RestaurantController restaurantController;
    private User_DB_Controller userDB;
    private UserController userController;
    private Reservation_DB_Controller reservationDB;
    private Waiting_DB_Controller waitingDB;
    private ReservationController reservationController;
    private RequestRouter router;
    private ReportsController reportsController;

    
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
        this.router = new RequestRouter();

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "UNKNOWN";
        }
    }

    @Override
    protected void serverStarted() {
        touchActivity();
        startIdleWatchdog();
        
        // ✨ הלוגים שביקשת להחזיר:
        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        conn.ConnectToDb();

        try {
            Connection sqlConn = conn.getConnection();
            if (sqlConn == null) {
                log("❌ DB connection failed.");
                return;
            }

            // --- אתחול קונטרולרים של DB ---
            restaurantDB = new Restaurant_DB_Controller(sqlConn);
            reservationDB = new Reservation_DB_Controller(sqlConn);
            userDB = new User_DB_Controller(sqlConn);
            waitingDB = new Waiting_DB_Controller(sqlConn);

            // --- יצירת טבלאות אוטומטית ---
            log("⚙️ Ensuring all database tables exist...");
            
            userDB.createSubscribersTable();
            userDB.createGuestsTable();
            restaurantDB.createRestaurantTablesTable();
            restaurantDB.createOpeningHoursTable();
            reservationDB.createReservationsTable();
            waitingDB.createWaitingListTable();

            log("✅ Database schema ensured.");

            // --- אתחול קונטרולרים לוגיים ---
            restaurantController = new RestaurantController(restaurantDB);
            userController = new UserController(userDB);
            reservationController = new ReservationController(reservationDB, this, restaurantController);
            reportsController = new ReportsController(reservationDB);


            registerHandlers();

            try {
                restaurantController.initAvailabilityGridNext30Days();
                log("📅 Availability grid ensured for the next 30 days.");
            } catch (Exception e) {
                log("⚠️ Grid init failed: " + e.getMessage());
            }

            log("All controllers and database tables initialized.");

        } catch (Exception e) {
            log("❌ Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerHandlers() {
        log("⚙️ Registering DTO handlers...");
        router.register(protocol.Commands.CREATE_RESERVATION, new CreateReservationHandler(reservationController));
        router.register(protocol.Commands.GET_OPENING_HOURS, new GetOpeningHoursHandler(restaurantController));
        router.register(Commands.SUBSCRIBER_LOGIN, new SubscriberLoginHandler(userController));
        router.register(protocol.Commands.GUEST_LOGIN, new GuestLoginHandler(userController));
        router.register(Commands.RECOVER_SUBSCRIBER_CODE,new RecoverSubscriberCodeHandler(userController));
        router.register(protocol.Commands.RECOVER_GUEST_CONFIRMATION_CODE,new RecoverGuestConfirmationCodeHandler(reservationController, userController));
                
        router.register(protocol.Commands.GET_TIME_REPORT, new GetTimeReportHandler(reportsController));

        	


    }

    @Override
    protected void serverStopped() {
        idleScheduler.shutdownNow();
        log("Server stopped.");
    }

    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        touchActivity();
        String ip = client.getInetAddress() != null
                ? client.getInetAddress().getHostAddress()
                : "UNKNOWN";
        log("Client connected | IP: " + ip);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        touchActivity();
        try {
            if (msg instanceof RequestDTO) {
                RequestDTO request = (RequestDTO) msg;
                log("📨 Received DTO command: " + request.getCommand());
                router.route(request, client);
                return;
            }
             
            if (!(msg instanceof ArrayList<?>)) return;
            ArrayList<?> arr = (ArrayList<?>) msg;
            if (arr.isEmpty()) return;
            String command = arr.get(0).toString();

            switch (command) {
                case "CLIENT_LOGOUT": {
                    client.close();
                    break;
                }
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
                default:
                    log("Unknown command: " + command);
            }
        } catch (Exception e) {
            log("Error handling message: " + e.getMessage());
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
    
    private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L; 
    private volatile long lastActivityMs = System.currentTimeMillis();
    private final ScheduledExecutorService idleScheduler = Executors.newSingleThreadScheduledExecutor();

    private Runnable onAutoShutdown;
    public void setOnAutoShutdown(Runnable r) { this.onAutoShutdown = r; }

    private void touchActivity() {
        lastActivityMs = System.currentTimeMillis();
    }

    private void startIdleWatchdog() {
        idleScheduler.scheduleAtFixedRate(() -> {
            try {
                long idle = System.currentTimeMillis() - lastActivityMs;
                if (idle >= IDLE_TIMEOUT_MS && getNumberOfClients() == 0) {
                    log("AUTO-SHUTDOWN: No activity for 30 minutes. Closing server...");
                    shutdownServerNow();
                }
            } catch (Exception e) {
                log("IdleWatchdog error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void shutdownServerNow() {
        try { stopListening(); } catch (Exception ignored) {}
        try { close(); } catch (Exception ignored) {}
        idleScheduler.shutdownNow();
        if (onAutoShutdown != null) {
            try { onAutoShutdown.run(); } catch (Exception ignored) {}
        }
    }
}