package logicControllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import dbControllers.Notification_DB_Controller;
import entities.Notification;

/**
 * Background service responsible for scheduling and dispatching notifications.
 * <p>
 * This service periodically queries the database for notifications that are
 * due to be sent and have not yet been marked as sent. Each due notification
 * is dispatched through {@link NotificationDispatcher} and then marked as sent
 * to prevent duplicate delivery.
 * </p>
 * <p>
 * The scheduler runs on a single-threaded {@link ScheduledExecutorService}
 * to ensure ordered and consistent processing.
 * </p>
 */
public class NotificationSchedulerService {

    private final Notification_DB_Controller db;
    private final NotificationDispatcher dispatcher;
    private final Consumer<String> logger;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructs a notification scheduler service.
     *
     * @param db         database controller used to fetch and update notifications
     * @param dispatcher dispatcher responsible for delivering notifications
     * @param logger     callback used for logging scheduler activity
     */
    public NotificationSchedulerService(Notification_DB_Controller db,
                                        NotificationDispatcher dispatcher,
                                        Consumer<String> logger) {
        this.db = db;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    /**
     * Starts the notification scheduler.
     * <p>
     * The scheduler periodically invokes the internal {@code tick} method
     * at a fixed rate, checking for due notifications and dispatching them.
     * </p>
     */
    public void start() {
        executor.scheduleAtFixedRate(this::tick, 2, 30, TimeUnit.SECONDS);
        logger.accept("⏱️ Notification scheduler started.");
    }

    /**
     * Stops the notification scheduler immediately.
     * <p>
     * All scheduled tasks are cancelled and no further notifications
     * will be processed until the service is started again.
     * </p>
     */
    public void stop() {
        executor.shutdownNow();
        logger.accept("⏹️ Notification scheduler stopped.");
    }

    /**
     * Performs a single scheduler cycle.
     * <p>
     * This method:
     * <ol>
     *   <li>Fetches all notifications that are due and not yet sent</li>
     *   <li>Dispatches each notification via {@link NotificationDispatcher}</li>
     *   <li>Marks each notification as sent in the database</li>
     * </ol>
     * </p>
     * <p>
     * Any exception during processing is caught and logged to ensure
     * the scheduler continues running.
     * </p>
     */
    private void tick() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Notification> due = db.getDueUnsent(now);

            if (!due.isEmpty()) {
                logger.accept("⏳ Due notifications found: " + due.size());
            }

            for (Notification n : due) {
                dispatcher.dispatch(n);
                db.markAsSent(n.getNotificationId(), now);
            }

        } catch (Exception e) {
            logger.accept("❌ Notification scheduler error: " + e.getMessage());
        }
    }
}
