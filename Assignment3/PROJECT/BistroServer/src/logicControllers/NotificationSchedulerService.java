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
 * Periodically checks DB for due notifications and dispatches them.
 */
public class NotificationSchedulerService {

    private final Notification_DB_Controller db;
    private final NotificationDispatcher dispatcher;
    private final Consumer<String> logger;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public NotificationSchedulerService(Notification_DB_Controller db,
                                        NotificationDispatcher dispatcher,
                                        Consumer<String> logger) {
        this.db = db;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    public void start() {
        // You can change 30 seconds -> 1 minute if you want
        executor.scheduleAtFixedRate(this::tick, 2, 30, TimeUnit.SECONDS);
        logger.accept("⏱️ Notification scheduler started.");
    }

    public void stop() {
        executor.shutdownNow();
        logger.accept("⏹️ Notification scheduler stopped.");
    }

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
