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

/**
 * Main OCSF-based server for the restaurant system.
 *
 * <p>
 * This server is responsible for: initializing the database connection and
 * schema, constructing database and logic controllers, registering DTO
 * handlers, routing incoming {@link RequestDTO} messages, maintaining online
 * user state, running background schedulers (waiting expiration, reservation
 * grace-period cancellation, end-of-day cleanup, and daily availability grid
 * refresh), and managing automatic shutdown on extended inactivity.
 * </p>
 *
 * <p>
 * The server logs events to both stdout and (optionally) a JavaFX UI
 * controller.
 * </p>
 */
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

	private final ScheduledExecutorService idleScheduler = Executors.newSingleThreadScheduledExecutor();

	private LocalDate lastClosingHandledDate = null;

	private RequestRouter router;
	private gui.ServerGUIController uiController;
	private String serverIp;

	// ===== Idle Watchdog =====
	private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L;
	private volatile long lastActivityMs = System.currentTimeMillis();
	private Runnable onAutoShutdown;

	// ================= UI =================

	/**
	 * Sets the JavaFX UI controller used to display server logs.
	 *
	 * @param controller the UI controller that receives log messages
	 */
	public void setUiController(gui.ServerGUIController controller) {
		this.uiController = controller;
	}

	/**
	 * Sets a callback that will be invoked when the server auto-shuts down due to
	 * inactivity.
	 *
	 * @param r callback to run on auto-shutdown
	 */
	public void setOnAutoShutdown(Runnable r) {
		this.onAutoShutdown = r;
	}

	/**
	 * Logs a message to stdout and, if available, to the JavaFX UI.
	 *
	 * @param msg message to log
	 */
	public void log(String msg) {
		System.out.println(msg);
		if (uiController != null) {
			Platform.runLater(() -> uiController.addLog(msg));
		}
	}

	// ================= Constructor =================

	/**
	 * Creates a new server instance bound to the given port.
	 *
	 * <p>
	 * This constructor initializes the {@link DBController} reference, configures
	 * the server reference inside the DB controller, creates a
	 * {@link RequestRouter}, and attempts to resolve the local server IP.
	 * </p>
	 *
	 * @param port the port to listen on
	 */
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

	/**
	 * Called by the OCSF framework when the server starts listening.
	 *
	 * <p>
	 * This method initializes the database connection and schema, creates all DB
	 * and logic controllers, starts background schedulers, initializes notification
	 * runtime services, registers request handlers, and ensures the availability
	 * grid for the upcoming days.
	 * </p>
	 */
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

			restaurantController = new RestaurantController(restaurantDB);
			userController = new UserController(userDB);
			receiptController = new ReceiptController(receiptDB);
			restaurantController.setReservationDB(reservationDB);

			reservationController = new ReservationController(reservationDB, notificationDB, this, restaurantController,
					receiptController);
			restaurantController.setReservationController(reservationController);

			waitingController = new WaitingController(waitingDB, notificationDB, this, restaurantController,
					reservationController);

			reservationController.setWaitingController(waitingController);
			restaurantController.setSpecialOpeningHoursDB(specialOpeningHoursDB);

			reportsController = new ReportsController(reservationDB, waitingDB, userDB);

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

			reservationScheduler = Executors.newSingleThreadScheduledExecutor();
			reservationScheduler.scheduleAtFixedRate(() -> {
				try {
					int c = reservationController.cancelReservationsWithoutCheckinAfterGracePeriod();
					if (c > 0)
						log("⏳ Auto-cancelled reservations without check-in: " + c);
				} catch (Exception e) {
					log("❌ Reservation scheduler error: " + e.getMessage());
				}
			}, 30, 30, TimeUnit.SECONDS);

			closingScheduler = Executors.newSingleThreadScheduledExecutor();
			closingScheduler.scheduleAtFixedRate(() -> {
				try {
					LocalDate today = LocalDate.now();
					if (today.equals(lastClosingHandledDate))
						return;

					OpeningHouers oh = restaurantController.getEffectiveOpeningHoursForDate(today);
					if (oh == null || oh.getCloseTime() == null)
						return;

					LocalTime closeTime = LocalTime.parse(oh.getCloseTime().substring(0, 5));

					if (LocalTime.now().isAfter(closeTime)) {
						waitingController.cancelAllWaitingsEndOfDay(today);
						lastClosingHandledDate = today;
						log("🌙 End-of-day waitings cancelled.");
					}
				} catch (Exception e) {
					log("❌ Closing scheduler error: " + e.getMessage());
				}
			}, 10, 60, TimeUnit.SECONDS);

			onlineUsersRegistry = new OnlineUsersRegistry();
			notificationDispatcher = new NotificationDispatcher(onlineUsersRegistry, this::log);
			notificationScheduler = new NotificationSchedulerService(notificationDB, notificationDispatcher, this::log);
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
			}, 1, 24 * 60, TimeUnit.MINUTES);

			log("✅ Server fully initialized.");

		} catch (Exception e) {
			log("❌ Initialization error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ================= Router =================

	/**
	 * Registers all command handlers into the {@link RequestRouter}.
	 *
	 * <p>
	 * Each handler is responsible for validating and processing a specific
	 * {@link Commands} request and producing the appropriate response back to the
	 * client.
	 * </p>
	 */
	private void registerHandlers() {
		log("⚙️ Registering DTO handlers...");

		router.register(Commands.CREATE_RESERVATION, new CreateReservationHandler(reservationController));
		router.register(Commands.GET_RESERVATION_HISTORY, new GetReservationHistoryHandler(reservationController));
		router.register(Commands.CANCEL_RESERVATION, new CancelReservationHandler(reservationController));
		router.register(Commands.GET_AVAILABLE_TIMES_FOR_DATE,
				new GetAvailableTimesForDateHandler(reservationController));
		router.register(Commands.GET_ALL_RESERVATIONS, new GetAllReservationsHandler(reservationController));
		router.register(Commands.GET_MY_ACTIVE_RESERVATIONS, new GetMyActiveReservationsHandler(reservationController));
		router.register(Commands.CHECKIN_RESERVATION, new CheckinReservationHandler(reservationController));
		router.register(Commands.GET_CURRENT_DINERS, new GetCurrentDinersHandler(reservationController));

		router.register(Commands.JOIN_WAITING_LIST, new JoinWaitingListHandler(waitingController));
		router.register(Commands.GET_WAITING_STATUS, new GetWaitingStatusHandler(waitingController));
		router.register(Commands.CANCEL_WAITING, new CancelWaitingHandler(waitingController));
		router.register(Commands.CONFIRM_WAITING_ARRIVAL, new ConfirmWaitingArrivalHandler(waitingController));
		router.register(Commands.GET_WAITING_LIST, new GetWaitingListHandler(waitingController));
		router.register(Commands.GET_MY_ACTIVE_WAITINGS, new GetMyActiveWaitingsHandler(waitingController));

		router.register(Commands.SUBSCRIBER_LOGIN, new SubscriberLoginHandler(userController, onlineUsersRegistry));
		router.register(Commands.GUEST_LOGIN, new GuestLoginHandler(userController, onlineUsersRegistry));
		router.register(Commands.REGISTER_SUBSCRIBER, new RegisterSubscriberHandler(userController));
		router.register(Commands.UPDATE_SUBSCRIBER_DETAILS, new UpdateSubscriberDetailsHandler(userController));
		router.register(Commands.RECOVER_SUBSCRIBER_CODE, new RecoverSubscriberCodeHandler(userController));
		router.register(Commands.RECOVER_GUEST_CONFIRMATION_CODE, new RecoverGuestConfirmationCodeHandler(
				reservationController, userController, notificationDB, this::log));
		router.register(Commands.GET_ALL_SUBSCRIBERS, new GetAllSubscribersHandler(userController));
		router.register(Commands.DELETE_SUBSCRIBER, new DeleteSubscriberHandler(userController));
		router.register(Commands.FIND_USER_BY_ID, new FindUserByIdHandler(userController));
		router.register(Commands.CREATE_GUEST_BY_PHONE, new CreateGuestByPhoneHandler(userController));
		router.register(Commands.BARCODE_LOGIN, new BarcodeLoginHandler(userController, onlineUsersRegistry));

		router.register(Commands.GET_TIME_REPORT, new GetTimeReportHandler(reportsController));
		router.register(Commands.GET_SUBSCRIBERS_REPORT, new GetSubscribersReportHandler(reportsController));

		router.register(Commands.GET_TABLES, new GetTablesHandler(restaurantController));
		router.register(Commands.SAVE_TABLE, new SaveTableHandler(restaurantController));
		router.register(Commands.DELETE_TABLE, new DeleteTableHandler(restaurantController));
		router.register(Commands.GET_OPENING_HOURS, new GetOpeningHoursHandler(restaurantController));
		router.register(Commands.UPDATE_OPENING_HOURS,
				new UpdateOpeningHoursHandler(restaurantController, reservationController));

		router.register(Commands.GET_SPECIAL_OPENING_HOURS, new GetSpecialOpeningHoursHandler(specialOpeningHoursDB));
		router.register(Commands.UPDATE_SPECIAL_OPENING_HOURS, new UpdateSpecialOpeningHoursHandler(
				specialOpeningHoursDB, restaurantController, reservationController));

		router.register(Commands.PAY_RECEIPT,
				new PayReceiptHandler(reservationController, receiptController, userController));
		router.register(Commands.GET_RECEIPT_BY_CODE,
				new GetReceiptByCodeHandler(reservationController, receiptController));
	}

	// ================= Messages =================

	/**
	 * Handles incoming messages from clients and routes {@link RequestDTO} objects
	 * to the appropriate handler.
	 *
	 * @param msg    the received message object
	 * @param client the client connection that sent the message
	 */
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

	/**
	 * Called when a client connects to the server.
	 *
	 * @param client the connected client
	 */
	@Override
	protected synchronized void clientConnected(ConnectionToClient client) {
		touchActivity();
		String ip = client.getInetAddress() != null ? client.getInetAddress().getHostAddress() : "UNKNOWN";
		log("🔌 Client connected | IP: " + ip);
	}

	/**
	 * Called when a client disconnects from the server.
	 *
	 * <p>
	 * The client is removed from the online registry (if available) to avoid stale
	 * online state.
	 * </p>
	 *
	 * @param client the disconnected client
	 */
	@Override
	protected synchronized void clientDisconnected(ConnectionToClient client) {
		touchActivity();

		String ip = client.getInetAddress() != null ? client.getInetAddress().getHostAddress() : "UNKNOWN";

		if (onlineUsersRegistry != null) {
			onlineUsersRegistry.removeClient(client);
		}

		log("🔌 Client disconnected (Logout or window closed) | IP: " + ip);
	}

	// ================= Shutdown =================

	/**
	 * Called by the OCSF framework when the server stops.
	 *
	 * <p>
	 * This method stops all background schedulers and notification services.
	 * </p>
	 */
	@Override
	protected void serverStopped() {
		idleScheduler.shutdownNow();
		if (waitingScheduler != null)
			waitingScheduler.shutdownNow();
		if (reservationScheduler != null)
			reservationScheduler.shutdownNow();
		if (closingScheduler != null)
			closingScheduler.shutdownNow();
		if (notificationScheduler != null)
			notificationScheduler.stop();
		if (gridDailyScheduler != null)
			gridDailyScheduler.shutdownNow();
		log("🛑 Server stopped.");
	}

	// ================= Idle Watchdog =================

	/**
	 * Updates the last activity timestamp used by the idle watchdog.
	 */
	private void touchActivity() {
		lastActivityMs = System.currentTimeMillis();
	}

	/**
	 * Starts a watchdog that periodically checks for inactivity and triggers
	 * auto-shutdown.
	 *
	 * <p>
	 * Auto-shutdown occurs when no activity was detected for
	 * {@link #IDLE_TIMEOUT_MS} and there are no connected clients.
	 * </p>
	 */
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

	/**
	 * Shuts down the server immediately by stopping listening, closing all
	 * connections, stopping schedulers, and invoking the optional shutdown
	 * callback.
	 */
	private void shutdownServerNow() {
		try {
			stopListening();
		} catch (Exception ignored) {
		}
		try {
			close();
		} catch (Exception ignored) {
		}
		idleScheduler.shutdownNow();

		if (notificationScheduler != null)
			notificationScheduler.stop();

		if (onAutoShutdown != null) {
			try {
				onAutoShutdown.run();
			} catch (Exception ignored) {
			}
		}
	}

	// ================= Monthly Report Notification =================

	/**
	 * Creates a "monthly report ready" notification for the restaurant manager if
	 * needed.
	 *
	 * <p>
	 * This method creates a notification only on the first day of the month,
	 * referring to the previous calendar month.
	 * </p>
	 */
	private void createMonthlyReportNotificationIfNeeded() {
		try {
			if (LocalDate.now().getDayOfMonth() != 1)
				return;

			var reportMonth = java.time.YearMonth.now().minusMonths(1);
			int managerId = userDB.getRestaurantManagerId();

			notificationDB.addNotification(new entities.Notification(0, managerId, entities.Enums.Channel.SMS,
					entities.Enums.NotificationType.MONTHLY_REPORT_READY,
					"Monthly report for " + reportMonth + " is now available.",
					java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0), false, null));

			log("📊 Monthly report notification created for manager | month=" + reportMonth);

		} catch (Exception e) {
			log("❌ Failed to create monthly report notification: " + e.getMessage());
		}
	}

	/**
	 * Handles exceptional client disconnects such as abrupt window close or
	 * connection failures.
	 *
	 * <p>
	 * The client is removed from the online registry (if available) to avoid stale
	 * online state, and a warning is logged.
	 * </p>
	 *
	 * @param client    the affected client connection
	 * @param exception the exception that occurred
	 */
	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		touchActivity();

		String ip = (client.getInetAddress() != null) ? client.getInetAddress().getHostAddress() : "UNKNOWN";

		if (onlineUsersRegistry != null) {
			onlineUsersRegistry.removeClient(client);
		}

		log("⚠️ Client connection lost (Window closed or crash) | IP: " + ip);
	}

	// ================= Main =================

	/**
	 * Starts the server from the command line.
	 *
	 * <p>
	 * If a port is provided in {@code args[0]}, it will be used; otherwise
	 * {@link #DEFAULT_PORT} is used.
	 * </p>
	 *
	 * @param args command-line arguments; optional first argument is the port
	 *             number
	 */
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
