package service.concurrent;

import dao.NotificationDao;
import dao.TaskDao;
import model.Notification;
import model.Task;
import model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [Multithreading Concept: Background Scheduled Task]
 *
 * Periodic background task that scans active project tasks, identifies work items
 * approaching their target deadline or currently overdue, and dispatches proactive
 * notifications via {@link NotificationCoordinator}.
 *
 * Designed to execute independently in a background {@link java.util.concurrent.ScheduledExecutorService}
 * thread without blocking user-facing HTTP Servlet requests. Handles database anomalies safely.
 */
public class DeadlineReminderTask implements Runnable {

    private final TaskDao taskDao;
    private final NotificationCoordinator notificationCoordinator;
    private NotificationDao notificationDao;
    private final AtomicInteger executionCount = new AtomicInteger(0);

    public DeadlineReminderTask(TaskDao taskDao, NotificationCoordinator notificationCoordinator) {
        this(taskDao, notificationCoordinator, null);
    }

    public DeadlineReminderTask(TaskDao taskDao, NotificationCoordinator notificationCoordinator, NotificationDao notificationDao) {
        this.taskDao = taskDao;
        this.notificationCoordinator = notificationCoordinator;
        this.notificationDao = notificationDao;
    }

    public void setNotificationDao(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public void run() {
        int runId = executionCount.incrementAndGet();
        try {
            LocalDate today = LocalDate.now();
            List<Task> allTasks;
            try {
                allTasks = taskDao.findAll();
            } catch (Exception e) {
                System.err.printf("[DeadlineReminderTask #%d ERROR] Could not read tasks from database: %s%n",
                        runId, e.getMessage());
                return;
            }

            int evaluatedCount = 0;
            int notificationsSent = 0;

            for (Task task : allTasks) {
                try {
                    if (task == null || task.getStatus() == TaskStatus.COMPLETED || task.getDueDate() == null) {
                        continue;
                    }

                    evaluatedCount++;
                    Long recipientId = getRecipientUserId(task);

                    // Case 1: Overdue Task Check
                    if (task.getDueDate().isBefore(today)) {
                        String eventKey = "OVERDUE_TASK_" + task.getId() + "_" + task.getDueDate();
                        boolean dispatched = notificationCoordinator.dispatchIfAbsent(eventKey, () -> {
                            String message = String.format("Task '%s' in project '%s' was due on %s and is now overdue.",
                                    task.getTitle(), task.getProjectName(), task.getDueDate());
                            return new Notification(null, recipientId, "Overdue Task Alert: " + task.getTitle(),
                                    message, "danger", false, LocalDateTime.now(), "Just now", "tasks");
                        });
                        if (dispatched) {
                            notificationsSent++;
                        }
                    }
                    // Case 2: Approaching Deadline Check (within next 3 days)
                    else if (task.getDueDate().isBefore(today.plusDays(3))) {
                        String eventKey = "APPROACHING_DEADLINE_TASK_" + task.getId() + "_" + task.getDueDate();
                        boolean dispatched = notificationCoordinator.dispatchIfAbsent(eventKey, () -> {
                            String message = String.format("Task '%s' is approaching its deadline on %s. Please review progress.",
                                    task.getTitle(), task.getDueDate());
                            return new Notification(null, recipientId, "Deadline Approaching: " + task.getTitle(),
                                    message, "warning", false, LocalDateTime.now(), "Just now", "tasks");
                        });
                        if (dispatched) {
                            notificationsSent++;
                        }
                    }
                } catch (Exception itemException) {
                    System.err.printf("[DeadlineReminderTask #%d WARN] Handled anomaly for task %s safely: %s%n",
                            runId, task != null ? task.getId() : "null", itemException.getMessage());
                }
            }

            if (notificationsSent > 0) {
                System.out.printf("[DeadlineReminderTask #%d] Evaluated %d tasks; generated %d reminders.%n",
                        runId, evaluatedCount, notificationsSent);
            }
        } catch (Throwable t) {
            // Guarantee ScheduledExecutorService thread never aborts
            System.err.printf("[DeadlineReminderTask #%d ERROR] Unhandled failure in deadline sweep: %s%n",
                    runId, t.getMessage());
        }
    }

    private Long getRecipientUserId(Task task) {
        if (task.getAssigneeId() != null) {
            return task.getAssigneeId();
        }
        if (task.getAssignee() != null && task.getAssignee().getId() != null) {
            return task.getAssignee().getId();
        }
        if (task.getReporterId() != null) {
            return task.getReporterId();
        }
        if (task.getReporter() != null && task.getReporter().getId() != null) {
            return task.getReporter().getId();
        }
        return 1L; // Fallback to primary administrator / manager
    }

    public int getExecutionCount() {
        return executionCount.get();
    }
}
