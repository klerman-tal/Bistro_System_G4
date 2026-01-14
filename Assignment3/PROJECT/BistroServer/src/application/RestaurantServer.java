package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dbControllers.DBController;
import dbControllers.Notification_DB_Controller;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.SpecialOpeningHours_DB_Controller;
import dbControllers.User_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import dto.RequestDTO;
import javafx.application.Platform;
import logicControllers.NotificationDispatcher;
import logicControllers.NotificationSchedulerService;
import logicControllers.OnlineUsersRegistry;
import logicControllers.ReservationController;
import logicControllers.RestaurantController;
import logicControllers.UserController;
import logicControllers.WaitingController;
import logicControllers.ReportsController;
import network.*;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import protocol.Commands;

public class RestaurantServer extends AbstractServer {

    public static final int DEFAULT_PORT = 5556;

    private DBController conn;

    // ===== DB Controllers =====
    private Restaurant_DB_Controller restaurantDB;
    private Reservation_DB_Controller reservationDB;
    private User_DB_Controller userDB;
    private Waiting_DB_Controller waitingDB;
    private Notification_DB_Controller notificationDB;

    // ===== Logic Controllers =====
    private RestaurantController restaurantController;
    private ReservationController reservationController;
    private UserController userController;
    private WaitingController waitingController;
    private ReportsController reportsController;

    // ===== Notifications runtime =====
    private OnlineUsersRegistry onlineUsersRegistry;
    private NotificationDispatcher notificationDispatcher;
    private NotificationSchedulerService notificationScheduler;

    // ===== Schedulers =====
    private ScheduledExecutorService waitingScheduler;
    SpecialOpeningHours_DB_Controller specialOpeningHoursDB;
    private final ScheduledExecutorService idleScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private RequestRouter router;

    private gui.ServerGUIController uiController;
    private String serverIp;

    // ================= UI =================
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    public void log(String msg) {
        // אם את רוצה בלי console בכלל - תמחקי את השורה הזו:
        System.out.println(msg);

        if (uiController != null) {
            Platform.runLater(() -> uiController.addLog(msg));
        }
    }

    // ================= Constructor =================
    public RestaurantServer(int port) {
        super(port);
        conn = new DBController();
        conn.setServer(this);
        router = new RequestRouter();

        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "UNKNOWN";
        }
    }

 // ================= Server Start =================
    @Override
    protected void serverStarted() {
        touchActivity();
        startIdleWatchdog();

        log("Server started on IP: " + serverIp);
        log("Listening on port " + getPort());

        conn.ConnectToDb();

        try {
            Connection sqlConn = conn.getConnection();
            if (sqlConn == null) {
                log("❌ DB connection failed.");
                return;
            }

            // ===== DB Controllers =====
            restaurantDB = new Restaurant_DB_Controller(sqlConn);
            reservationDB = new Reservation_DB_Controller(sqlConn);
            userDB = new User_DB_Controller(sqlConn);
            waitingDB = new Waiting_DB_Controller(sqlConn);
            notificationDB = new Notification_DB_Controller(sqlConn);
            
            // ✨ הוספה: אתחול הקונטרולר החדש לשעות מיוחדות
            specialOpeningHoursDB = new SpecialOpeningHours_DB_Controller(sqlConn);

            log("⚙️ Ensuring all database tables exist...");
            userDB.createSubscribersTable();
            userDB.createGuestsTable();
            restaurantDB.createRestaurantTablesTable();
            restaurantDB.createOpeningHoursTable();
            
            // ✨ הוספה: יצירת טבלת החרגות תאריכים
            specialOpeningHoursDB.createSpecialOpeningHoursTable();
            
            reservationDB.createReservationsTable();
            waitingDB.createWaitingListTable();
            notificationDB.createNotificationsTable();
            log("✅ Database schema ensured.");

            // ===== Logic Controllers =====
            // מעדכנים את ה-RestaurantController שיכיר גם את ה-DB של השעות המיוחדות
            restaurantController = new RestaurantController(restaurantDB, specialOpeningHoursDB);
            userController = new UserController(userDB);

            reservationController =
                    new ReservationController(reservationDB, notificationDB, this, restaurantController);

            waitingController =
                    new WaitingController(waitingDB, notificationDB, this, restaurantController, reservationController);

            reservationController.setWaitingController(waitingController);

            reportsController =
                    new ReportsController(reservationDB, waitingDB, userDB);

            // ===== Waiting auto-cancel scheduler =====
            waitingScheduler = Executors.newSingleThreadScheduledExecutor();
            waitingScheduler.scheduleAtFixedRate(() -> {
                try {
                    int c = waitingController.cancelExpiredWaitingsAndReservations();
                    if (c > 0)
                        log("⏳ Auto-cancelled expired waitings: " + c);
                } catch (Exception e) {
                    log("Waiting scheduler error: " + e.getMessage());
                }
            }, 10, 10, TimeUnit.SECONDS);

            // ✅ Notifications runtime (ONLINE USERS + DISPATCHER + SCHEDULER)
            onlineUsersRegistry = new OnlineUsersRegistry();
            notificationDispatcher = new NotificationDispatcher(onlineUsersRegistry, this::log);
            notificationScheduler = new NotificationSchedulerService(notificationDB, notificationDispatcher, this::log);
            notificationScheduler.start();

            registerHandlers();

            try {
                restaurantController.initAvailabilityGridNext30Days();
                log("📅 Availability grid ensured for the next 30 days.");
            } catch (Exception e) {
                log("⚠️ Grid init failed: " + e.getMessage());
            }

            log("✅ Server fully initialized.");

        } catch (Exception e) {
            log("❌ Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= Router =================
    private void registerHandlers() {
        log("⚙️ Registering DTO handlers...");

        // ===== Reservations =====
        router.register(Commands.CREATE_RESERVATION,
                new CreateReservationHandler(reservationController));

        router.register(Commands.GET_RESERVATION_HISTORY,
                new GetReservationHistoryHandler(reservationController));

        router.register(Commands.CANCEL_RESERVATION,
                new CancelReservationHandler(reservationController));

        // ===== Opening Hours =====
        router.register(Commands.GET_OPENING_HOURS,
                new GetOpeningHoursHandler(restaurantController));

        router.register(Commands.UPDATE_OPENING_HOURS,
                new UpdateSpecialOpeningHoursHandler(restaurantController));

        // ===== Tables =====
        router.register(Commands.GET_TABLES,
                new GetTablesHandler(restaurantController));

        router.register(Commands.SAVE_TABLE,
                new SaveTableHandler(restaurantController));

        router.register(Commands.DELETE_TABLE,
                new DeleteTableHandler(restaurantController));

        // ===== Users =====
        router.register(Commands.SUBSCRIBER_LOGIN,
        		new SubscriberLoginHandler(userController, onlineUsersRegistry));

        router.register(Commands.GUEST_LOGIN,
        		new GuestLoginHandler(userController, onlineUsersRegistry));

        router.register(Commands.REGISTER_SUBSCRIBER,
                new RegisterSubscriberHandler(userController));

        router.register(Commands.UPDATE_SUBSCRIBER_DETAILS,
                new UpdateSubscriberDetailsHandler(userController));

        router.register(Commands.RECOVER_SUBSCRIBER_CODE,
                new RecoverSubscriberCodeHandler(userController));

        router.register(Commands.RECOVER_GUEST_CONFIRMATION_CODE,
                new RecoverGuestConfirmationCodeHandler(
                        reservationController,
                        userController,
                        notificationDB,
                        this::log
                ));

        // ===== Waiting List =====
        router.register(Commands.JOIN_WAITING_LIST,
                new JoinWaitingListHandler(waitingController));

        router.register(Commands.GET_WAITING_STATUS,
                new GetWaitingStatusHandler(waitingController));

        router.register(Commands.CANCEL_WAITING,
                new CancelWaitingHandler(waitingController));

        router.register(Commands.CONFIRM_WAITING_ARRIVAL,
                new ConfirmWaitingArrivalHandler(waitingController));

        // ===== Reports =====
        router.register(Commands.GET_TIME_REPORT,
                new GetTimeReportHandler(reportsController));

        router.register(Commands.GET_SUBSCRIBERS_REPORT,
                new GetSubscribersReportHandler(reportsController));
        
        router.register(
        	    Commands.GET_ALL_SUBSCRIBERS,
        	    new GetAllSubscribersHandler(userController)
        	);
        router.register(
                Commands.DELETE_SUBSCRIBER,

                new DeleteSubscriberHandler(userController)
        );
        
        router.register(Commands.CHECKIN_RESERVATION,
                new CheckinReservationHandler(reservationController));


                new DeleteSubscriberHandler(userController);
        router.register(
                Commands.GET_ALL_RESERVATIONS,
                new GetAllReservationsHandler(reservationController));
        router.register(Commands.UPDATE_SPECIAL_OPENING_HOURS, 
        	    new UpdateSpecialOpeningHoursHandler(restaurantController));
        
        
        

        
                
        



    }

    // ================= Messages =================
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        touchActivity();

        try {
            if (msg instanceof RequestDTO request) {
                log("📨 Received DTO command: " + request.getCommand());
                router.route(request, client);
            }
        } catch (Exception e) {
            log("❌ Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= Shutdown =================
    @Override
    protected void serverStopped() {
        idleScheduler.shutdownNow();

        if (waitingScheduler != null)
            waitingScheduler.shutdownNow();

        if (notificationScheduler != null)
            notificationScheduler.stop();

        log("Server stopped.");
    }

    // ================= Clients =================
    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        touchActivity();
        String ip = client.getInetAddress() != null
                ? client.getInetAddress().getHostAddress()
                : "UNKNOWN";
        log("Client connected | IP: " + ip);

        // NOTE:
        // כדי ש-SMS popup יעבוד, OnlineUsersRegistry חייב לדעת איזה userId מחובר על איזה client.
        // את הרישום בפועל עושים בדרך כלל בזמן LOGIN (SubscriberLoginHandler / GuestLoginHandler)
        // כי רק שם יודעים את ה-userId.
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        touchActivity();
        onlineUsersRegistry.removeClient(client);
        log("Client disconnected.");

        // Optional: אם ה-OnlineUsersRegistry שלך תומך בזה:
        // onlineUsersRegistry.removeClient(client);
    }

    // ================= Idle Watchdog =================
    private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L;
    private volatile long lastActivityMs = System.currentTimeMillis();
    private Runnable onAutoShutdown;

    public void setOnAutoShutdown(Runnable r) {
        this.onAutoShutdown = r;
    }

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

        if (notificationScheduler != null)
            notificationScheduler.stop();

        if (onAutoShutdown != null) {
            try { onAutoShutdown.run(); } catch (Exception ignored) {}
        }
    }

    // ================= Main =================
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
