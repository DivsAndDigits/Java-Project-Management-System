package service.concurrent;

import dao.TaskDao;
import service.NotificationService;
import service.ServiceFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [Multithreading Concept: Executor Lifecycle & Thread Management]
 *
 * Centralized manager controlling the lifecycle of background concurrency infrastructure:
 * <ul>
 *     <li>{@link ScheduledExecutorService} for periodic deadline checking</li>
 *     <li>{@link DeadlineReminderTask} scheduled execution</li>
 *     <li>{@link NotificationCoordinator} thread-safe deduplication</li>
 *     <li>{@link NotificationBatchProcessor} parallel worker pool</li>
 * </ul>
 *
 * Ensures background threads are initialized on application startup and gracefully
 * terminated on container shutdown without memory or thread leaks.
 */
public class AppSchedulerManager {

    private static volatile AppSchedulerManager instance;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private DeadlineReminderTask reminderTask;
    private NotificationCoordinator notificationCoordinator;
    private NotificationBatchProcessor batchProcessor;
    private volatile boolean running = false;

    private AppSchedulerManager() {
        initializeComponents();
    }

    public static AppSchedulerManager getInstance() {
        if (instance == null) {
            synchronized (AppSchedulerManager.class) {
                if (instance == null) {
                    instance = new AppSchedulerManager();
                }
            }
        }
        return instance;
    }

    private void initializeComponents() {
        ServiceFactory factory = ServiceFactory.getInstance();
        NotificationService notificationService = factory.getNotificationService();
        TaskDao taskDao = factory.getTaskDao();
        dao.NotificationDao notificationDao = factory.getNotificationDao();

        this.notificationCoordinator = new NotificationCoordinator(notificationService);
        this.batchProcessor = new NotificationBatchProcessor(this.notificationCoordinator);
        if (taskDao != null) {
            this.reminderTask = new DeadlineReminderTask(taskDao, this.notificationCoordinator, notificationDao);
        }
    }

    /**
     * Initializes components with explicit dependencies (used for custom wiring and test suites).
     */
    public synchronized void configure(TaskDao taskDao, NotificationService notificationService) {
        this.notificationCoordinator = new NotificationCoordinator(notificationService);
        this.batchProcessor = new NotificationBatchProcessor(this.notificationCoordinator);
        this.reminderTask = new DeadlineReminderTask(taskDao, this.notificationCoordinator);
    }

    /**
     * Starts the periodic background scheduler with default intervals (initial delay 5s, period 60s).
     */
    public synchronized void start() {
        start(5, 60, TimeUnit.SECONDS);
    }

    /**
     * Starts the periodic background scheduler with specified timing parameters.
     */
    public synchronized void start(long initialDelay, long period, TimeUnit timeUnit) {
        if (running && scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        if (reminderTask == null) {
            initializeComponents();
        }

        AtomicInteger threadCounter = new AtomicInteger(1);
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "OPMT-DeadlineChecker-" + threadCounter.getAndIncrement());
            t.setDaemon(true); // Daemon thread ensures JVM shutdown is never blocked
            return t;
        });

        if (reminderTask != null) {
            this.scheduledFuture = this.scheduler.scheduleAtFixedRate(
                    reminderTask, initialDelay, period, timeUnit);
        }

        this.running = true;
        System.out.println("[AppSchedulerManager] Background DeadlineReminderTask scheduled successfully.");
    }

    /**
     * Gracefully stops the scheduler and associated thread pools.
     * Implements two-phase shutdown (shutdown -> awaitTermination -> shutdownNow).
     */
    public synchronized void stop() {
        if (!running && (scheduler == null || scheduler.isShutdown())) {
            return;
        }

        System.out.println("[AppSchedulerManager] Shutting down background executors...");
        running = false;

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (batchProcessor != null && !batchProcessor.isShutdown()) {
            batchProcessor.shutdown();
            try {
                if (!batchProcessor.awaitTermination(3, TimeUnit.SECONDS)) {
                    // Timeout passed
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[AppSchedulerManager] Background executors stopped cleanly.");
    }

    /**
     * Triggers an immediate execution of the deadline check synchronously.
     * Useful for on-demand HTTP requests or automated JUnit test verifications.
     */
    public void triggerImmediateCheck() {
        if (reminderTask != null) {
            reminderTask.run();
        }
    }

    public boolean isRunning() {
        return running && scheduler != null && !scheduler.isShutdown();
    }

    public NotificationCoordinator getNotificationCoordinator() {
        return notificationCoordinator;
    }

    public NotificationBatchProcessor getBatchProcessor() {
        return batchProcessor;
    }

    public DeadlineReminderTask getReminderTask() {
        return reminderTask;
    }
}
