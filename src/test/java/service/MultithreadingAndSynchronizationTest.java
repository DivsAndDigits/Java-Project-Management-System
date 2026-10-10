package service;

import dao.*;
import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import service.concurrent.AppSchedulerManager;
import service.concurrent.DeadlineReminderTask;
import service.concurrent.NotificationBatchProcessor;
import service.concurrent.NotificationCoordinator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multithreading & Synchronization (Academic Requirement: 4 Marks) Test Suite")
public class MultithreadingAndSynchronizationTest {

    private UserDao userDao;
    private ProjectDao projectDao;
    private TaskDao taskDao;
    private NotificationDao notificationDao;

    private NotificationService notificationService;
    private TaskService taskService;
    private ProjectService projectService;

    private NotificationCoordinator coordinator;
    private AppSchedulerManager schedulerManager;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        projectDao = new InMemoryProjectDao(userDao);
        taskDao = new InMemoryTaskDao(userDao);
        notificationDao = new InMemoryNotificationDao();

        notificationService = new NotificationServiceImpl(notificationDao);
        taskService = new TaskServiceImpl(taskDao, projectDao);
        projectService = new ProjectServiceImpl(projectDao);

        coordinator = new NotificationCoordinator(notificationService);
        schedulerManager = AppSchedulerManager.getInstance();
        schedulerManager.configure(taskDao, notificationService);
    }

    @AfterEach
    void tearDown() {
        // Ensure all background executors stop cleanly after every test
        if (schedulerManager != null && schedulerManager.isRunning()) {
            schedulerManager.stop();
        }
    }

    // =========================================================================
    // 1. Background Scheduler Creation & Periodic Execution
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("1. Scheduler Lifecycle: Background ScheduledExecutorService starts, runs, and stops cleanly")
    void testBackgroundSchedulerLifecycle() throws InterruptedException {
        assertFalse(schedulerManager.isRunning(), "Scheduler should initially be stopped");

        // Start scheduler with fast 100ms interval for testing
        schedulerManager.start(10, 100, TimeUnit.MILLISECONDS);
        assertTrue(schedulerManager.isRunning(), "Scheduler must be active after start()");

        // Allow scheduler to execute at least one sweep
        Thread.sleep(350);

        DeadlineReminderTask task = schedulerManager.getReminderTask();
        assertNotNull(task);
        assertTrue(task.getExecutionCount() >= 1, "Periodic task must execute at least once");

        // Stop scheduler cleanly
        schedulerManager.stop();
        assertFalse(schedulerManager.isRunning(), "Scheduler must report stopped after stop()");
    }

    // =========================================================================
    // 2. Deadline Reminder Task Execution
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("2. Deadline Task: Scans tasks and dispatches overdue and approaching reminders")
    void testDeadlineReminderTaskExecution() {
        int initialNotifCount = notificationDao.findAll().size();

        DeadlineReminderTask reminderTask = new DeadlineReminderTask(taskDao, coordinator);
        reminderTask.run();

        // Seed data in InMemoryTaskDao has at least 1 overdue task (Task 7) and tasks approaching deadline
        assertTrue(coordinator.getTotalDispatchedCount() > 0, "Reminders should have been dispatched");
        assertTrue(notificationDao.findAll().size() > initialNotifCount, "Notifications must be stored in repository");

        // Re-running immediately should detect duplicates and dispatch 0 new notifications
        long countBeforeSecondRun = coordinator.getTotalDispatchedCount();
        reminderTask.run();
        assertEquals(countBeforeSecondRun, coordinator.getTotalDispatchedCount(),
                "Subsequent run must not re-dispatch duplicate reminders for the same event keys");
        assertTrue(coordinator.getDuplicatesSuppressedCount() > 0, "Duplicate reminders must be tracked as suppressed");
    }

    // =========================================================================
    // 3. Concurrent Duplicate Notification Prevention (Synchronization)
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("3. Synchronization: 20 concurrent threads trying to dispatch same event key results in exactly 1 notification")
    void testConcurrentDuplicateNotificationPrevention() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        String eventKey = "OVERDUE_CRITICAL_TASK_999";

        AtomicInteger deliveryAttempts = new AtomicInteger(0);
        List<Callable<Boolean>> callables = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            callables.add(() -> coordinator.dispatchIfAbsent(eventKey, () -> {
                deliveryAttempts.incrementAndGet();
                return new Notification(null, 1L, "Critical Alert", "Task #999 is past deadline",
                        "danger", false, LocalDateTime.now(), "now", "tasks");
            }));
        }

        // Execute all 20 threads simultaneously
        List<Future<Boolean>> futures = executor.invokeAll(callables);
        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

        int successCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) {
                successCount++;
            }
        }

        // Exactly one thread must have acquired the critical section first and succeeded
        assertEquals(1, successCount, "Only exactly 1 thread should succeed in dispatching");
        assertEquals(1, deliveryAttempts.get(), "Notification supplier must be invoked only once");
        assertEquals(threadCount - 1, coordinator.getDuplicatesSuppressedCount(),
                "Remaining 19 threads must be intercepted and suppressed as duplicates");
        assertTrue(coordinator.isAlreadyDispatched(eventKey));
    }

    // =========================================================================
    // 4. Thread-Safe Shared Resource Access (NotificationCoordinator Metrics)
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("4. Shared Resource: Concurrent dispatch across distinct event keys updates metrics safely")
    void testConcurrentDistinctDispatches() throws InterruptedException {
        int distinctKeyCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(distinctKeyCount);

        for (int i = 0; i < distinctKeyCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String key = "EVENT_DISTINCT_" + index;
                    coordinator.dispatchIfAbsent(key, () ->
                            new Notification(null, 1L, "Alert " + index, "Message", "info", false, LocalDateTime.now(), "now", "tasks")
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(distinctKeyCount, coordinator.getTotalDispatchedCount(),
                "All 50 distinct keys must be dispatched without race conditions");
        assertEquals(distinctKeyCount, coordinator.getDispatchedEventKeys().size());
    }

    // =========================================================================
    // 5. Concurrent Task & Project Update Safety
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("5. Progress Safety: Concurrent task completions atomically update project progress without lost updates")
    void testConcurrentProjectProgressUpdate() throws InterruptedException {
        Project project = new Project();
        project.setId(88L);
        project.setName("Concurrent Benchmark Initiative");
        project.setCode("PRJ-CONCUR");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.updateTaskMetrics(100, 0); // 100 total tasks, 0 completed initially
        projectDao.save(project);

        int workers = 10;
        int incrementsPerWorker = 5;
        // Total tasks to be marked completed: 10 * 5 = 50
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(workers);

        for (int i = 0; i < workers; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerWorker; j++) {
                        project.recordTaskCompletion(true, false);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire all worker threads concurrently
        assertTrue(endGate.await(3, TimeUnit.SECONDS));
        executor.shutdown();

        // 50 tasks completed out of 100 -> progress must be exactly 50%
        assertEquals(50, project.getCompletedTasks(), "Completed tasks count must be exactly 50 (no lost updates)");
        assertEquals(50, project.getProgressPercentage(), "Progress percentage must be atomically updated to 50%");
    }

    // =========================================================================
    // 6. Multi-Threaded Batch Processing with ExecutorService
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("6. Batch Processor: NotificationBatchProcessor concurrently evaluates task collection")
    void testNotificationBatchProcessor() throws InterruptedException {
        NotificationBatchProcessor batchProcessor = new NotificationBatchProcessor(coordinator, 4);

        List<Task> testTasks = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Create mock tasks: 5 overdue, 5 approaching deadline, 5 future
        for (int i = 1; i <= 5; i++) {
            testTasks.add(new Task((long) (200 + i), 1L, "Batch Proj", "Overdue " + i, "Desc",
                    TaskStatus.IN_PROGRESS, Priority.HIGH, null, null, today.minusDays(i), 8, 0));
        }
        for (int i = 1; i <= 5; i++) {
            testTasks.add(new Task((long) (300 + i), 1L, "Batch Proj", "Upcoming " + i, "Desc",
                    TaskStatus.TODO, Priority.MEDIUM, null, null, today.plusDays(1), 8, 0));
        }
        for (int i = 1; i <= 5; i++) {
            testTasks.add(new Task((long) (400 + i), 1L, "Batch Proj", "Future " + i, "Desc",
                    TaskStatus.TODO, Priority.LOW, null, null, today.plusDays(30), 8, 0));
        }

        NotificationBatchProcessor.BatchProcessingResult result =
                batchProcessor.processTaskBatchConcurrently(testTasks);

        assertNotNull(result);
        assertEquals(15, result.getTotalTasksEvaluated());
        assertEquals(10, result.getNotificationsDispatched(),
                "10 tasks (5 overdue + 5 upcoming) should trigger notifications");

        // Running second batch concurrently should suppress all 10 as duplicates
        NotificationBatchProcessor.BatchProcessingResult result2 =
                batchProcessor.processTaskBatchConcurrently(testTasks);
        assertEquals(0, result2.getNotificationsDispatched());
        assertEquals(10, result2.getDuplicatesSuppressed());

        batchProcessor.shutdown();
        assertTrue(batchProcessor.awaitTermination(2, TimeUnit.SECONDS));
    }

    // =========================================================================
    // 7. Concurrent Team Members CopyOnWriteArraySet Stress
    // =========================================================================
    @Test
    @Timeout(5)
    @DisplayName("7. Thread-Safe Collections: CopyOnWriteArraySet supports concurrent reads while adding members")
    void testConcurrentReadWriteTeamMembers() throws InterruptedException {
        Project project = projectService.getProjectById(1L);
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    // Half threads write, half threads read/iterate
                    if (id % 2 == 0) {
                        User temp = new TeamMember((long) (500 + id), "user" + id, "User " + id,
                                "u" + id + "@corp.com", "Dev", "U", "QA", 20);
                        project.addTeamMember(temp);
                    } else {
                        // Iterating during concurrent writes must not throw ConcurrentModificationException
                        int count = 0;
                        for (User u : project.getTeamMembers()) {
                            count++;
                        }
                        assertTrue(count > 0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        executor.shutdown();

        // Clean up temporary members
        for (int i = 0; i < threadCount; i += 2) {
            project.removeTeamMemberById((long) (500 + i));
        }
    }
}
