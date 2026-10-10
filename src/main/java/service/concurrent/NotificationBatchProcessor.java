package service.concurrent;

import model.Notification;
import model.Task;
import model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [Multithreading Concept: Concurrent Batch Processing with ExecutorService]
 *
 * Demonstrates high-throughput parallel work item processing. Uses a dedicated
 * {@link ExecutorService} worker pool to evaluate large batches of tasks concurrently
 * across multiple threads, dispatching proactive reminders through {@link NotificationCoordinator}.
 */
public class NotificationBatchProcessor {

    private final ExecutorService executorService;
    private final NotificationCoordinator notificationCoordinator;
    private final int workerPoolSize;

    public NotificationBatchProcessor(NotificationCoordinator notificationCoordinator) {
        this(notificationCoordinator, 4);
    }

    public NotificationBatchProcessor(NotificationCoordinator notificationCoordinator, int poolSize) {
        this.notificationCoordinator = notificationCoordinator;
        this.workerPoolSize = poolSize;

        AtomicInteger threadId = new AtomicInteger(1);
        this.executorService = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "OPMT-BatchWorker-" + threadId.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Data Transfer Object containing consolidated metrics from concurrent batch processing.
     */
    public static class BatchProcessingResult {
        private final int totalTasksEvaluated;
        private final int notificationsDispatched;
        private final int duplicatesSuppressed;
        private final long durationMs;
        private final int workerThreadsUsed;

        public BatchProcessingResult(int totalTasksEvaluated, int notificationsDispatched,
                                     int duplicatesSuppressed, long durationMs, int workerThreadsUsed) {
            this.totalTasksEvaluated = totalTasksEvaluated;
            this.notificationsDispatched = notificationsDispatched;
            this.duplicatesSuppressed = duplicatesSuppressed;
            this.durationMs = durationMs;
            this.workerThreadsUsed = workerThreadsUsed;
        }

        public int getTotalTasksEvaluated() {
            return totalTasksEvaluated;
        }

        public int getNotificationsDispatched() {
            return notificationsDispatched;
        }

        public int getDuplicatesSuppressed() {
            return duplicatesSuppressed;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public int getWorkerThreadsUsed() {
            return workerThreadsUsed;
        }

        @Override
        public String toString() {
            return String.format("BatchProcessingResult [evaluated=%d, dispatched=%d, suppressed=%d, duration=%dms, threads=%d]",
                    totalTasksEvaluated, notificationsDispatched, duplicatesSuppressed, durationMs, workerThreadsUsed);
        }
    }

    /**
     * Concurrently evaluates a collection of tasks across worker threads.
     *
     * @param tasks list of tasks to evaluate
     * @return BatchProcessingResult with execution statistics
     * @throws InterruptedException if interrupted while awaiting workers
     */
    public BatchProcessingResult processTaskBatchConcurrently(List<Task> tasks) throws InterruptedException {
        if (tasks == null || tasks.isEmpty()) {
            return new BatchProcessingResult(0, 0, 0, 0, workerPoolSize);
        }

        long startTime = System.currentTimeMillis();
        LocalDate today = LocalDate.now();

        AtomicInteger dispatchedCount = new AtomicInteger(0);
        AtomicInteger suppressedCount = new AtomicInteger(0);
        AtomicInteger evaluatedCount = new AtomicInteger(0);

        List<Callable<Void>> workerCallables = new ArrayList<>();

        for (Task task : tasks) {
            workerCallables.add(() -> {
                if (task.getStatus() == TaskStatus.COMPLETED || task.getDueDate() == null) {
                    return null;
                }

                evaluatedCount.incrementAndGet();
                Long recipientId = task.getAssignee() != null ? task.getAssignee().getId() : 1L;

                if (task.getDueDate().isBefore(today)) {
                    String key = "OVERDUE_TASK_" + task.getId() + "_" + task.getDueDate();
                    boolean dispatched = notificationCoordinator.dispatchIfAbsent(key, () ->
                            new Notification(null, recipientId, "Overdue Work Item: " + task.getTitle(),
                                    "Task is past deadline (" + task.getDueDate() + ").", "danger",
                                    false, LocalDateTime.now(), "Just now", "tasks")
                    );
                    if (dispatched) {
                        dispatchedCount.incrementAndGet();
                    } else {
                        suppressedCount.incrementAndGet();
                    }
                } else if (task.getDueDate().isBefore(today.plusDays(3))) {
                    String key = "APPROACHING_DEADLINE_TASK_" + task.getId() + "_" + task.getDueDate();
                    boolean dispatched = notificationCoordinator.dispatchIfAbsent(key, () ->
                            new Notification(null, recipientId, "Upcoming Deadline: " + task.getTitle(),
                                    "Task is due soon on " + task.getDueDate() + ".", "warning",
                                    false, LocalDateTime.now(), "Just now", "tasks")
                    );
                    if (dispatched) {
                        dispatchedCount.incrementAndGet();
                    } else {
                        suppressedCount.incrementAndGet();
                    }
                }
                return null;
            });
        }

        // Parallel execution across worker threads in the pool
        List<Future<Void>> futures = executorService.invokeAll(workerCallables);
        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (ExecutionException ignored) {}
        }

        long duration = System.currentTimeMillis() - startTime;
        return new BatchProcessingResult(evaluatedCount.get(), dispatchedCount.get(),
                suppressedCount.get(), duration, workerPoolSize);
    }

    /**
     * Gracefully shuts down the worker pool.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }
}
