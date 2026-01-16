package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dbControllers.*;
import dto.RequestDTO;
import entities.OpeningHouers;
import javafx.application.Platform;
import logicControllers.*;
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
    private Receipt_DB_Controller receiptDB;
    private SpecialOpeningHours_DB_Controller specialOpeningHoursDB;

    // ===== Logic Controllers =====
    private RestaurantController restaurantController;
    private ReservationController reservationController;
    private UserController userController;
    private WaitingController waitingController;
    private ReportsController reportsController;
    private ReceiptController receiptController;

    // ===== Notifications runtime =====
    private OnlineUsersRegistry onlineUsersRegistry;
    private NotificationDispatcher notificationDispatcher;
    private NotificationSchedulerService notificationScheduler;

    // ===== Schedulers =====
    private ScheduledExecutorService waitingScheduler;
    private ScheduledExecutorService reservationScheduler;
    private ScheduledExecutorService closingScheduler;
    private ScheduledExecutorService gridDailyScheduler;

    private final ScheduledExecutorService idleScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private LocalDate lastClosingHandledDate = null;

    private RequestRouter router;
    private gui.ServerGUIController uiController;
    private String serverIp;

    // ===== Idle Watchdog =====
    private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L;
    private volatile long lastActivityMs = System.currentTimeMillis();
    private Runnable onAutoShutdown;

    // ================= UI =================
    public void setUiController(gui.ServerGUIController controller) {
        this.uiController = controller;
    }

    public void setOnAutoShutdown(Runnable r) {
        this.onAutoShutdown = r;
    }

    public void log(String msg) {
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

        log("🚀 Server started on IP: " + serverIp);
        log("📡 Listening on port " + getPort());

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
            receiptDB = new Receipt_DB_Controller(sqlConn);
            specialOpeningHoursDB = new SpecialOpeningHours_DB_Controller(sqlConn);

            log("⚙️ Ensuring all database tables exist...");
            userDB.createSubscribersTable();
            userDB.createGuestsTable();
            restaurantDB.createRestaurantTablesTable();
            restaurantDB.createOpeningHoursTable();
            reservationDB.createReservationsTable();
            waitingDB.createWaitingListTable();
            notificationDB.createNotificationsTable();
            receiptDB.createReceiptsTable();
            specialOpeningHoursDB.createSpecialOpeningHoursTable();
            log("✅ Database schema ensured.");

            // ===== Logic Controllers =====
            restaurantController = new RestaurantController(restaurantDB);
            userController = new UserController(userDB);
            receiptController = new ReceiptController(receiptDB);
            restaurantController.setReservationDB(reservationDB);


            reservationController =
                    new ReservationController(
                            reservationDB,
                            notificationDB,
                            this,
                            restaurantController,
                            receiptController
                    );
            restaurantController.setReservationController(reservationController);

            waitingController =
                    new WaitingController(
                            waitingDB,
                            notificationDB,
                            this,
                            restaurantController,
                            reservationController
                    );

            reservationController.setWaitingController(waitingController);
            restaurantController.setSpecialOpeningHoursDB(specialOpeningHoursDB);

            reportsController =
                    new ReportsController(reservationDB, waitingDB, userDB);

            // ===== Waiting auto-cancel (WAITINGS ONLY) =====
            waitingScheduler = Executors.newSingleThreadScheduledExecutor();
            waitingScheduler.scheduleAtFixedRate(() -> {
                try {
                    int c = waitingController.cancelExpiredWaitings();
                    if (c > 0)
                        log("⏳ Auto-cancelled expired waitings: " + c);
                } catch (Exception e) {
                    log("❌ Waiting scheduler error: " + e.getMessage());
                }
            }, 10, 10, TimeUnit.SECONDS);

            // ===== Reservation auto-cancel (NO CHECK-IN AFTER 15 MIN) =====
            reservationScheduler = Executors.newSingleThreadScheduledExecutor();
            reservationScheduler.scheduleAtFixedRate(() -> {
                try {
                    int c =
                            reservationController
                                    .cancelReservationsWithoutCheckinAfterGracePeriod();
                    if (c > 0)
                        log("⏳ Auto-cancelled reservations without check-in: " + c);
                } catch (Exception e) {
                    log("❌ Reservation scheduler error: " + e.getMessage());
                }
            }, 30, 30, TimeUnit.SECONDS);

            // ===== Closing Scheduler (End of Day) =====
            closingScheduler = Executors.newSingleThreadScheduledExecutor();
            closingScheduler.scheduleAtFixedRate(() -> {
                try {
                    LocalDate today = LocalDate.now();
                    if (today.equals(lastClosingHandledDate)) return;

                    OpeningHouers oh =
                            restaurantController.getEffectiveOpeningHoursForDate(today);
                    if (oh == null || oh.getCloseTime() == null) return;

                    LocalTime closeTime =
                            LocalTime.parse(oh.getCloseTime().substring(0, 5));

                    if (LocalTime.now().isAfter(closeTime)) {
                        waitingController.cancelAllWaitingsEndOfDay(today);
                        lastClosingHandledDate = today;
                        log("🌙 End-of-day waitings cancelled.");
                    }
                } catch (Exception e) {
                    log("❌ Closing scheduler error: " + e.getMessage());
                }
            }, 10, 60, TimeUnit.SECONDS);

            // ===== Notifications runtime =====
            onlineUsersRegistry = new OnlineUsersRegistry();
            notificationDispatcher =
                    new NotificationDispatcher(onlineUsersRegistry, this::log);
            notificationScheduler =
                    new NotificationSchedulerService(
                            notificationDB,
                            notificationDispatcher,
                            this::log
                    );
            notificationScheduler.start();

            createMonthlyReportNotificationIfNeeded();

            registerHandlers();

            try {
                restaurantController.initAvailabilityGridNext30Days();
                log("📅 Availability grid ensured for the next 30 days.");
            } catch (Exception e) {
                log("⚠️ Grid init failed: " + e.getMessage());
            }
            gridDailyScheduler = Executors.newSingleThreadScheduledExecutor();

            gridDailyScheduler.scheduleAtFixedRate(() -> {
                try {
                    restaurantController.initAvailabilityGridNext30Days();
                    log("📅 Daily availability grid refresh completed.");
                } catch (Exception e) {
                    log("❌ Daily grid refresh failed: " + e.getMessage());
                }
            }, 
            1,              // דיליי ראשון (דקה אחרי עליית שרת)
            24 * 60,        // כל 24 שעות
            TimeUnit.MINUTES);


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
        router.register(Commands.GET_AVAILABLE_TIMES_FOR_DATE,
                new GetAvailableTimesForDateHandler(reservationController));
        router.register(Commands.GET_ALL_RESERVATIONS,
                new GetAllReservationsHandler(reservationController));
        router.register(Commands.GET_MY_ACTIVE_RESERVATIONS,
                new GetMyActiveReservationsHandler(reservationController));
        router.register(Commands.CHECKIN_RESERVATION,
                new CheckinReservationHandler(reservationController));
        router.register(Commands.GET_CURRENT_DINERS,
                new GetCurrentDinersHandler(reservationController));

        // ===== Waiting =====
        router.register(Commands.JOIN_WAITING_LIST,
                new JoinWaitingListHandler(waitingController));
        router.register(Commands.GET_WAITING_STATUS,
                new GetWaitingStatusHandler(waitingController));
        router.register(Commands.CANCEL_WAITING,
                new CancelWaitingHandler(waitingController));
        router.register(Commands.CONFIRM_WAITING_ARRIVAL,
                new ConfirmWaitingArrivalHandler(waitingController));
        router.register(Commands.GET_WAITING_LIST,
                new GetWaitingListHandler(waitingController));
        router.register(Commands.GET_MY_ACTIVE_WAITINGS,
                new GetMyActiveWaitingsHandler(waitingController));

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
                        this::log));
        router.register(Commands.GET_ALL_SUBSCRIBERS,
                new GetAllSubscribersHandler(userController));
        router.register(Commands.DELETE_SUBSCRIBER,
                new DeleteSubscriberHandler(userController));
        router.register(Commands.FIND_USER_BY_ID,
                new FindUserByIdHandler(userController));
        router.register(Commands.CREATE_GUEST_BY_PHONE,
                new CreateGuestByPhoneHandler(userController));

        // ===== Reports =====
        router.register(Commands.GET_TIME_REPORT,
                new GetTimeReportHandler(reportsController));
        router.register(Commands.GET_SUBSCRIBERS_REPORT,
                new GetSubscribersReportHandler(reportsController));

        // ===== Tables / Opening Hours =====
        router.register(Commands.GET_TABLES,
                new GetTablesHandler(restaurantController));
        router.register(Commands.SAVE_TABLE,
                new SaveTableHandler(restaurantController));
        router.register(Commands.DELETE_TABLE,
                new DeleteTableHandler(restaurantController));
        router.register(Commands.GET_OPENING_HOURS,
                new GetOpeningHoursHandler(restaurantController));
        router.register(Commands.UPDATE_OPENING_HOURS,
                new UpdateOpeningHoursHandler(
                        restaurantController,
                        reservationController));

        // ===== Special Opening Hours =====
        router.register(Commands.GET_SPECIAL_OPENING_HOURS,
                new GetSpecialOpeningHoursHandler(specialOpeningHoursDB));
        router.register(Commands.UPDATE_SPECIAL_OPENING_HOURS,
                new UpdateSpecialOpeningHoursHandler(
                        specialOpeningHoursDB,
                        restaurantController,
                        reservationController));

        // ===== Payments / Receipts =====
        router.register(Commands.PAY_RECEIPT,
                new PayReceiptHandler(
                        reservationController,
                        receiptController,
                        userController));
        router.register(Commands.GET_RECEIPT_BY_CODE,
                new GetReceiptByCodeHandler(
                        reservationController,
                        receiptController));
    }
    public boolean pushPopupToUserIfOnline(int userId, dto.NotificationDTO dto) {
        if (onlineUsersRegistry == null) return false;

        var client = onlineUsersRegistry.getClient(userId);
        if (client == null) return false;

        try {
            client.sendToClient(dto);
            return true;
        } catch (Exception e) {
            log("❌ pushPopupToUserIfOnline failed | userId=" + userId + " | " + e.getMessage());
            return false;
        }
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

    // ================= Clients =================
    @Override
    protected synchronized void clientConnected(ConnectionToClient client) {
        touchActivity();
        String ip =
                client.getInetAddress() != null
                        ? client.getInetAddress().getHostAddress()
                        : "UNKNOWN";
        log("🔌 Client connected | IP: " + ip);
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        touchActivity();

        String ip =
                client.getInetAddress() != null
                        ? client.getInetAddress().getHostAddress()
                        : "UNKNOWN";

        if (onlineUsersRegistry != null) {
            onlineUsersRegistry.removeClient(client);
        }

        log("🔌 Client disconnected (Logout or window closed) | IP: " + ip);
    }


    // ================= Shutdown =================
    @Override
    protected void serverStopped() {
        idleScheduler.shutdownNow();
        if (waitingScheduler != null) waitingScheduler.shutdownNow();
        if (reservationScheduler != null) reservationScheduler.shutdownNow();
        if (closingScheduler != null) closingScheduler.shutdownNow();
        if (notificationScheduler != null) notificationScheduler.stop();
        if (gridDailyScheduler != null)
            gridDailyScheduler.shutdownNow();
        log("🛑 Server stopped.");
    }

    // ================= Idle Watchdog =================
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
            try {
                onAutoShutdown.run();
            } catch (Exception ignored) {}
        }
    }

    // ================= Monthly Report Notification =================
    private void createMonthlyReportNotificationIfNeeded() {
        try {
            if (LocalDate.now().getDayOfMonth() != 1) return;

            var reportMonth =
                    java.time.YearMonth.now().minusMonths(1);
            int managerId =
                    userDB.getRestaurantManagerId();

            notificationDB.addNotification(
                    new entities.Notification(
                            0,
                            managerId,
                            entities.Enums.Channel.SMS,
                            entities.Enums.NotificationType.MONTHLY_REPORT_READY,
                            "Monthly report for " + reportMonth + " is now available.",
                            java.time.LocalDateTime.now()
                                    .withHour(0).withMinute(0).withSecond(0),
                            false,
                            null));

            log("📊 Monthly report notification created for manager | month=" + reportMonth);

        } catch (Exception e) {
            log("❌ Failed to create monthly report notification: " + e.getMessage());
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