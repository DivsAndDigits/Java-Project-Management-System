package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import service.concurrent.AppSchedulerManager;

/**
 * [Multithreading Concept: Executor Lifecycle Management]
 *
 * ServletContextListener orchestrating the lifecycle of background worker threads.
 * Automatically registered by the Jakarta Servlet container via {@link WebListener}.
 *
 * Guarantees that:
 * <ul>
 *     <li>The background {@link java.util.concurrent.ScheduledExecutorService} starts on deployment.</li>
 *     <li>All worker threads and schedulers terminate cleanly on container shutdown,
 *         preventing classloader leaks or lingering unmanaged threads.</li>
 * </ul>
 */
@WebListener
public class ApplicationLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[ApplicationLifecycleListener] Online Project Management Tool initializing...");
        try {
            // Start background deadline reminder scheduler (5s initial delay, 60s period)
            AppSchedulerManager.getInstance().start();
            System.out.println("[ApplicationLifecycleListener] Background scheduler started successfully.");
        } catch (Exception e) {
            System.err.printf("[ApplicationLifecycleListener ERROR] Could not initialize scheduler: %s%n", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[ApplicationLifecycleListener] Online Project Management Tool shutting down...");
        try {
            // Gracefully terminate all background thread pools and schedulers
            AppSchedulerManager.getInstance().stop();
            System.out.println("[ApplicationLifecycleListener] Background scheduler stopped cleanly.");
        } catch (Exception e) {
            System.err.printf("[ApplicationLifecycleListener ERROR] Exception during executor shutdown: %s%n", e.getMessage());
        }
    }
}
