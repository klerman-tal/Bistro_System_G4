package application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dbControllers.DBController;
import dbControllers.Reservation_DB_Controller;
import dbControllers.Restaurant_DB_Controller;
import dbControllers.User_DB_Controller;
import dbControllers.Waiting_DB_Controller;
import dto.RequestDTO;
import javafx.application.Platform;
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
	private Restaurant_DB_Controller restaurantDB;
	private RestaurantController restaurantController;
	private User_DB_Controller userDB;
	private UserController userController;
	private Reservation_DB_Controller reservationDB;
	private Waiting_DB_Controller waitingDB;
	private ReportsController reportsController;

	private ReservationController reservationController;
	private WaitingController waitingController;
	private ScheduledExecutorService waitingScheduler;

	private RequestRouter router;

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

		log("Server started on IP: " + serverIp);
		log("Listening on port " + getPort());

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

			log("⚙️ Ensuring all database tables exist...");
			userDB.createSubscribersTable();
			userDB.createGuestsTable();
			restaurantDB.createRestaurantTablesTable();
			restaurantDB.createOpeningHoursTable();
			reservationDB.createReservationsTable();
			waitingDB.createWaitingListTable();
			log("✅ Database schema ensured.");

			restaurantController = new RestaurantController(restaurantDB);
			userController = new UserController(userDB);
			reservationController = new ReservationController(reservationDB, this, restaurantController);
			reportsController = new ReportsController(reservationDB, waitingDB);

			// ✅ NEW
			waitingController = new WaitingController(waitingDB, this, restaurantController, reservationController);

			// run every 10 seconds
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

	private void registerHandlers() {
		log("⚙️ Registering DTO handlers...");

		// Reservations
		router.register(Commands.CREATE_RESERVATION, new CreateReservationHandler(reservationController));

		router.register(Commands.GET_RESERVATION_HISTORY, new GetReservationHistoryHandler(reservationController));

		// Opening hours
		router.register(Commands.GET_OPENING_HOURS, new GetOpeningHoursHandler(restaurantController));

		router.register(Commands.UPDATE_OPENING_HOURS, new UpdateOpeningHoursHandler(restaurantController));

		// Tables
		router.register(Commands.GET_TABLES, new GetTablesHandler(restaurantController));

		router.register(Commands.SAVE_TABLE, new SaveTableHandler(restaurantController));

		router.register(Commands.DELETE_TABLE, new DeleteTableHandler(restaurantController));

		// Users
		router.register(Commands.SUBSCRIBER_LOGIN, new SubscriberLoginHandler(userController));

		router.register(Commands.GUEST_LOGIN, new GuestLoginHandler(userController));

		router.register(Commands.RECOVER_SUBSCRIBER_CODE, new RecoverSubscriberCodeHandler(userController));

		router.register(Commands.RECOVER_GUEST_CONFIRMATION_CODE,
				new RecoverGuestConfirmationCodeHandler(reservationController, userController));

		router.register(Commands.REGISTER_SUBSCRIBER, new RegisterSubscriberHandler(userController));

		router.register(Commands.UPDATE_SUBSCRIBER_DETAILS, new UpdateSubscriberDetailsHandler(userController));

		// ✅ WAITING LIST
		router.register(Commands.JOIN_WAITING_LIST, new JoinWaitingListHandler(waitingController));

		router.register(Commands.GET_WAITING_STATUS, new GetWaitingStatusHandler(waitingController));
		router.register(Commands.CANCEL_WAITING, new CancelWaitingHandler(waitingController));
		router.register(Commands.CONFIRM_WAITING_ARRIVAL, new ConfirmWaitingArrivalHandler(waitingController));
		router.register(Commands.CANCEL_RESERVATION, new CancelReservationHandler(reservationController));

		// ✅ REPORTS
		router.register(Commands.GET_TIME_REPORT, new GetTimeReportHandler(reportsController));
		router.register(Commands.GET_SUBSCRIBERS_REPORT, new GetSubscribersReportHandler(reportsController));

	}

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

	@Override
	protected void serverStopped() {
		idleScheduler.shutdownNow();
		if (waitingScheduler != null)
			waitingScheduler.shutdownNow();
		log("Server stopped.");
	}

	@Override
	protected synchronized void clientConnected(ConnectionToClient client) {
		touchActivity();
		String ip = client.getInetAddress() != null ? client.getInetAddress().getHostAddress() : "UNKNOWN";
		log("Client connected | IP: " + ip);
	}

	// ================= Idle Auto Shutdown =================
	private static final long IDLE_TIMEOUT_MS = 30L * 60L * 1000L;
	private volatile long lastActivityMs = System.currentTimeMillis();
	private final ScheduledExecutorService idleScheduler = Executors.newSingleThreadScheduledExecutor();

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
		try {
			stopListening();
		} catch (Exception ignored) {
		}
		try {
			close();
		} catch (Exception ignored) {
		}
		idleScheduler.shutdownNow();
		if (onAutoShutdown != null) {
			try {
				onAutoShutdown.run();
			} catch (Exception ignored) {
			}
		}
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