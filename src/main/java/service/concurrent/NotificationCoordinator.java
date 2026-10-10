package service.concurrent;

import model.Notification;
import service.NotificationService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * [Multithreading Concept: Synchronization & Thread-Safe Shared Resources]
 *
 * Coordinates notification dispatch across multiple concurrent threads (e.g., background
 * scheduled tasks, asynchronous batch worker threads, and HTTP Servlet request threads).
 *
 * =============================================================================
 * MULTITHREADING & SYNCHRONIZATION ARCHITECTURAL ANALYSIS:
 * =============================================================================
 * 1. Shared Resource:
 *    - The {@code dispatchedEventKeys} set and the dispatch state of {@code NotificationService}.
 * 2. Why Multiple Threads Access It:
 *    - The {@code DeadlineReminderTask} background thread runs periodically on a timer.
 *    - The {@code NotificationBatchProcessor} runs multiple concurrent worker threads.
 *    - Web users trigger manual deadline checks or task status changes in Servlet threads.
 * 3. Race Condition (Check-Then-Act):
 *    - Without synchronization, Thread A and Thread B simultaneously evaluate an overdue task.
 *    - Both threads check {@code dispatchedEventKeys.contains(key)} at the exact same instant;
 *      both see {@code false}.
 *    - Both threads proceed to instantiate a {@code Notification} and invoke
 *      {@code notificationService.sendNotification(notification)}.
 *    - Both threads insert duplicate notifications into the user's feed and trigger duplicate emails.
 * 4. How Synchronization Prevents the Race Condition:
 *    - A {@link ReentrantLock} defines an atomic critical section. Only one thread can evaluate
 *      the key's existence, generate the notification, deliver it, and record the key at a time.
 *    - Any subsequent thread entering the critical section finds the key already present and
 *      safely suppresses the duplicate.
 * =============================================================================
 */
public class NotificationCoordinator {

    // [Collections: Thread-Safe Collections]
    // ConcurrentHashMap.newKeySet() provides a thread-safe set view backed by ConcurrentHashMap,
    // ensuring memory visibility across threads according to the Java Memory Model (JMM).
    private final Set<String> dispatchedEventKeys = ConcurrentHashMap.newKeySet();

    // [Multithreading: Synchronization Lock]
    // ReentrantLock provides explicit mutual exclusion for compound check-then-act operations.
    private final ReentrantLock dispatchLock = new ReentrantLock();

    // Atomic counters for thread-safe lock-free metric updates
    private final AtomicLong totalDispatched = new AtomicLong(0);
    private final AtomicLong duplicatesSuppressed = new AtomicLong(0);

    private final NotificationService notificationService;

    public NotificationCoordinator(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Atomically dispatches a notification if the unique event key has not already been processed.
     * Guarantees that duplicate notifications are never issued across concurrent threads.
     *
     * @param eventKey             unique deduplication signature (e.g. "OVERDUE:TASK:12:2026-10-10")
     * @param notificationSupplier supplier generating the Notification instance if absent
     * @return true if notification was dispatched; false if suppressed as a duplicate
     */
    public boolean dispatchIfAbsent(String eventKey, Supplier<Notification> notificationSupplier) {
        if (eventKey == null || notificationSupplier == null) {
            return false;
        }

        // Fast path read: if already known, avoid lock contention
        if (dispatchedEventKeys.contains(eventKey)) {
            duplicatesSuppressed.incrementAndGet();
            return false;
        }

        // Critical section guarded by explicit ReentrantLock
        dispatchLock.lock();
        try {
            // Second check inside critical section (Double-Checked pattern)
            if (dispatchedEventKeys.contains(eventKey)) {
                duplicatesSuppressed.incrementAndGet();
                return false;
            }

            // Generate and deliver notification atomically
            Notification notification = notificationSupplier.get();
            if (notification != null) {
                notificationService.sendNotification(notification);
                dispatchedEventKeys.add(eventKey);
                totalDispatched.incrementAndGet();
                return true;
            }
            return false;
        } finally {
            dispatchLock.unlock();
        }
    }

    /**
     * Checks if an event key has already been dispatched.
     */
    public boolean isAlreadyDispatched(String eventKey) {
        if (eventKey == null) {
            return false;
        }
        return dispatchedEventKeys.contains(eventKey);
    }

    /**
     * Resets the deduplication cache and metrics (primarily used in test environments).
     */
    public void reset() {
        dispatchLock.lock();
        try {
            dispatchedEventKeys.clear();
            totalDispatched.set(0);
            duplicatesSuppressed.set(0);
        } finally {
            dispatchLock.unlock();
        }
    }

    public long getTotalDispatchedCount() {
        return totalDispatched.get();
    }

    public long getDuplicatesSuppressedCount() {
        return duplicatesSuppressed.get();
    }

    public Set<String> getDispatchedEventKeys() {
        return Collections.unmodifiableSet(dispatchedEventKeys);
    }

    public boolean isLocked() {
        return dispatchLock.isLocked();
    }
}
