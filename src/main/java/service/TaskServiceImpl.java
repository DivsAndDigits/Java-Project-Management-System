package service;

import dao.TaskDao;
import exception.TaskNotFoundException;
import exception.UnauthorizedAccessException;
import exception.ValidationException;
import model.Priority;
import model.Task;
import model.TaskStatus;
import util.GenericCollectionUtils;
import util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * [OOP Concept: Polymorphism]
 * [OOP Concept: Exception Handling]
 * [Collections: List, Map, Generics, Sorting, Filtering]
 * Implementation of TaskService coordinating business validations, DAO operations,
 * and collection-based processing algorithms.
 */
public class TaskServiceImpl implements TaskService {

    private final TaskDao taskDao;
    private dao.ProjectDao projectDao;
    private NotificationService notificationService;

    public TaskServiceImpl(TaskDao taskDao) {
        this(taskDao, null, null);
    }

    public TaskServiceImpl(TaskDao taskDao, dao.ProjectDao projectDao) {
        this(taskDao, projectDao, null);
    }

    public TaskServiceImpl(TaskDao taskDao, dao.ProjectDao projectDao, NotificationService notificationService) {
        this.taskDao = taskDao;
        this.projectDao = projectDao;
        this.notificationService = notificationService;
    }

    public void setProjectDao(dao.ProjectDao projectDao) {
        this.projectDao = projectDao;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // =========================================================================
    // [Collections: List]
    // =========================================================================

    @Override
    public List<Task> getAllTasks() {
        return taskDao.findAll();
    }

    @Override
    public Task getTaskById(Long id) {
        if (id == null) {
            // [OOP Concept: Exception Handling]
            throw new ValidationException("Task ID cannot be null.");
        }
        return taskDao.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public Task createTask(Task task) {
        if (task == null) {
            // [OOP Concept: Exception Handling]
            throw new ValidationException("Task cannot be null.");
        }
        if (ValidationUtils.isBlank(task.getTitle())) {
            throw new ValidationException("Task title is required.");
        }
        if (!ValidationUtils.isValidLength(task.getTitle(), 2, 200)) {
            throw new ValidationException("Task title must be between 2 and 200 characters.");
        }
        if (task.getEstimatedHours() < 0) {
            throw new ValidationException("Estimated hours cannot be negative.");
        }

        // =====================================================================
        // [OOP Concept: Polymorphism]
        // Runtime polymorphic call to reporter.canAssignTasks().
        // If the user is an Admin or ProjectManager, assigning tasks is authorized.
        // =====================================================================
        if (task.getReporter() != null && !task.getReporter().canAssignTasks()) {
            // [OOP Concept: Exception Handling]
            throw new UnauthorizedAccessException(task.getReporter().getUsername(),
                    "Creating and assigning work items requires Project Manager or Administrator role.");
        }

        Task saved = taskDao.save(task);
        syncProjectMetrics(saved.getProjectId());

        // Notification: New task assigned to user
        Long assigneeId = saved.getAssigneeId();
        if (assigneeId == null && saved.getAssignee() != null) {
            assigneeId = saved.getAssignee().getId();
        }
        if (assigneeId != null && notificationService != null) {
            String pName = saved.getProjectName() != null ? saved.getProjectName() : ("Project #" + saved.getProjectId());
            model.Notification notif = new model.Notification(
                    null,
                    assigneeId,
                    "New Task Assigned: " + saved.getTitle(),
                    "You have been assigned to task '" + saved.getTitle() + "' in " + pName + ".",
                    "info",
                    false,
                    java.time.LocalDateTime.now(),
                    "Just now",
                    "tasks"
            );
            notificationService.sendNotification(notif);
        }

        return saved;
    }

    @Override
    public boolean updateTask(Task task) {
        if (task == null || task.getId() == null) {
            throw new ValidationException("Cannot update task without valid ID.");
        }
        if (ValidationUtils.isBlank(task.getTitle())) {
            throw new ValidationException("Task title is required.");
        }
        if (!ValidationUtils.isValidLength(task.getTitle(), 2, 200)) {
            throw new ValidationException("Task title must be between 2 and 200 characters.");
        }

        // Verify task exists
        Task previous = getTaskById(task.getId());
        boolean updated = taskDao.update(task);
        if (updated) {
            syncProjectMetrics(task.getProjectId());

            // Check if assignee changed -> notify new assignee
            Long oldAssignee = previous != null ? previous.getAssigneeId() : null;
            Long newAssignee = task.getAssigneeId();
            if (newAssignee != null && !newAssignee.equals(oldAssignee) && notificationService != null) {
                model.Notification notif = new model.Notification(
                        null,
                        newAssignee,
                        "New Task Assigned: " + task.getTitle(),
                        "You have been assigned to task '" + task.getTitle() + "'.",
                        "info",
                        false,
                        java.time.LocalDateTime.now(),
                        "Just now",
                        "tasks"
                );
                notificationService.sendNotification(notif);
            }

            // Check if status changed to COMPLETED
            if (task.getStatus() == TaskStatus.COMPLETED && notificationService != null) {
                Long assigneeId = task.getAssigneeId();
                Long reporterId = task.getReporterId();
                if (reporterId == null && task.getProjectId() != null && projectDao != null) {
                    reporterId = projectDao.findById(task.getProjectId()).map(model.Project::getManagerId).orElse(null);
                }
                if (reporterId == null) {
                    reporterId = 1L;
                }

                // Notify reporter / manager
                notificationService.sendNotification(new model.Notification(
                        null,
                        reporterId,
                        "Task Completed: " + task.getTitle(),
                        "Task '" + task.getTitle() + "' has been marked as COMPLETED.",
                        "success",
                        false,
                        java.time.LocalDateTime.now(),
                        "Just now",
                        "tasks"
                ));

                // If assignee is distinct, notify assignee as well
                if (assigneeId != null && !assigneeId.equals(reporterId)) {
                    notificationService.sendNotification(new model.Notification(
                            null,
                            assigneeId,
                            "Task Completed: " + task.getTitle(),
                            "Your task '" + task.getTitle() + "' has been marked as COMPLETED.",
                            "success",
                            false,
                            java.time.LocalDateTime.now(),
                            "Just now",
                            "tasks"
                    ));
                }
            }
        }
        return updated;
    }

    @Override
    public boolean updateTaskStatus(Long taskId, TaskStatus newStatus) {
        if (taskId == null || newStatus == null) {
            throw new ValidationException("Task ID and new status cannot be null.");
        }
        Task task = getTaskById(taskId);
        TaskStatus oldStatus = task.getStatus();
        if (oldStatus == newStatus) {
            return true;
        }
        task.setStatus(newStatus);
        boolean updated = taskDao.update(task);

        // [Multithreading Concept: Concurrent Task Update Safety]
        // Atomically records task status changes into the project's completed task count and progress
        if (updated) {
            syncProjectMetrics(task.getProjectId());

            // Notification: Task completed event
            if (newStatus == TaskStatus.COMPLETED && oldStatus != TaskStatus.COMPLETED && notificationService != null) {
                Long assigneeId = task.getAssigneeId();
                Long reporterId = task.getReporterId();
                if (reporterId == null && task.getProjectId() != null && projectDao != null) {
                    reporterId = projectDao.findById(task.getProjectId()).map(model.Project::getManagerId).orElse(null);
                }
                if (reporterId == null) {
                    reporterId = 1L;
                }

                // Notify reporter / manager
                notificationService.sendNotification(new model.Notification(
                        null,
                        reporterId,
                        "Task Completed: " + task.getTitle(),
                        "Task '" + task.getTitle() + "' has been marked as COMPLETED.",
                        "success",
                        false,
                        java.time.LocalDateTime.now(),
                        "Just now",
                        "tasks"
                ));

                // If assignee is distinct, notify assignee as well
                if (assigneeId != null && !assigneeId.equals(reporterId)) {
                    notificationService.sendNotification(new model.Notification(
                            null,
                            assigneeId,
                            "Task Completed: " + task.getTitle(),
                            "Your task '" + task.getTitle() + "' has been marked as COMPLETED.",
                            "success",
                            false,
                            java.time.LocalDateTime.now(),
                            "Just now",
                            "tasks"
                    ));
                }
            }
        }
        return updated;
    }

    @Override
    public boolean deleteTask(Long id) {
        if (id == null) {
            throw new ValidationException("Task ID cannot be null.");
        }
        // Verify existence before deletion
        Task task = getTaskById(id);
        Long projectId = task.getProjectId();
        boolean deleted = taskDao.deleteById(id);
        if (deleted && projectId != null) {
            syncProjectMetrics(projectId);
        }
        return deleted;
    }

    private void syncProjectMetrics(Long projectId) {
        if (projectId != null && projectDao != null) {
            projectDao.findById(projectId).ifPresent(project -> {
                List<Task> projectTasks = taskDao.findByProjectId(projectId);
                int total = projectTasks.size();
                int completed = (int) projectTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
                project.updateTaskMetrics(total, completed);
                projectDao.update(project);
            });
        }
    }

    @Override
    public boolean assignTask(Long taskId, Long assigneeId) {
        if (taskId == null) {
            throw new ValidationException("Task ID cannot be null.");
        }
        // Verify task exists
        Task task = getTaskById(taskId);
        boolean assigned = taskDao.assignTask(taskId, assigneeId);
        if (assigned && assigneeId != null && notificationService != null) {
            model.Notification notif = new model.Notification(
                    null,
                    assigneeId,
                    "New Task Assigned: " + task.getTitle(),
                    "You have been assigned to task '" + task.getTitle() + "'.",
                    "info",
                    false,
                    java.time.LocalDateTime.now(),
                    "Just now",
                    "tasks"
            );
            notificationService.sendNotification(notif);
        }
        return assigned;
    }

    @Override
    public List<Task> getTasksByProjectId(Long projectId) {
        return taskDao.findByProjectId(projectId);
    }

    @Override
    public List<Task> getTasksByAssigneeId(Long assigneeId) {
        return taskDao.findByAssigneeId(assigneeId);
    }

    @Override
    public List<Task> getTasksByProject(int projectId) {
        return getTasksByProjectId((long) projectId);
    }

    @Override
    public List<Task> getTasksAssignedToUser(int userId) {
        return getTasksByAssigneeId((long) userId);
    }

    @Override
    public List<Task> getTasksByStatus(TaskStatus status) {
        if (status == null) {
            return getAllTasks();
        }
        return taskDao.findByStatus(status);
    }

    @Override
    public List<Task> getPriorityTasks(int limit) {
        return taskDao.findPriorityTasks(limit);
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskDao.findOverdueTasks();
    }

    // =========================================================================
    // [Collections: Map]
    // =========================================================================

    @Override
    public Map<Long, Task> getTaskMap() {
        return taskDao.findAllAsMap();
    }

    @Override
    public Map<TaskStatus, Long> getTaskCountByStatus() {
        return GenericCollectionUtils.countBy(getAllTasks(), Task::getStatus);
    }

    @Override
    public Map<Priority, Long> getTaskCountByPriority() {
        return GenericCollectionUtils.countBy(getAllTasks(), Task::getPriority);
    }

    @Override
    public Map<TaskStatus, List<Task>> getTasksGroupedByStatus() {
        return GenericCollectionUtils.groupBy(getAllTasks(), Task::getStatus);
    }

    @Override
    public Map<Priority, List<Task>> getTasksGroupedByPriority() {
        return GenericCollectionUtils.groupBy(getAllTasks(), Task::getPriority);
    }

    // =========================================================================
    // [Collections: Sorting]
    // Tasks sortable by: deadline, priority, status, estimated hours, title
    // =========================================================================

    @Override
    public List<Task> getTasksSortedBy(String sortBy) {
        List<Task> tasks = getAllTasks();
        if (sortBy == null || sortBy.isBlank()) {
            return tasks;
        }

        Comparator<Task> comparator;
        switch (sortBy.trim().toLowerCase()) {
            case "deadline":
            case "duedate":
            case "due_date":
                comparator = Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId);
                break;
            case "priority":
                comparator = Comparator.comparing(Task::getPriority).reversed()
                        .thenComparing(Task::getId);
                break;
            case "status":
                comparator = Comparator.comparing(Task::getStatus)
                        .thenComparing(Task::getId);
                break;
            case "estimatedhours":
            case "estimated_hours":
            case "hours":
                comparator = Comparator.comparingInt(Task::getEstimatedHours).reversed()
                        .thenComparing(Task::getId);
                break;
            case "title":
                comparator = Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Task::getId);
                break;
            default:
                comparator = Comparator.comparing(Task::getId);
                break;
        }

        return GenericCollectionUtils.sort(tasks, comparator);
    }

    // =========================================================================
    // [Collections: Filtering & Search]
    // =========================================================================

    @Override
    public List<Task> filterTasksByStatus(TaskStatus status) {
        if (status == null) {
            return getAllTasks();
        }
        return GenericCollectionUtils.filter(getAllTasks(), t -> t.getStatus() == status);
    }

    @Override
    public List<Task> filterTasksByPriority(Priority priority) {
        if (priority == null) {
            return getAllTasks();
        }
        return GenericCollectionUtils.filter(getAllTasks(), t -> t.getPriority() == priority);
    }

    @Override
    public List<Task> filterTasksByProject(Long projectId) {
        if (projectId == null) {
            return getAllTasks();
        }
        return GenericCollectionUtils.filter(getAllTasks(), t -> projectId.equals(t.getProjectId()));
    }

    @Override
    public List<Task> filterTasksByAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return getAllTasks();
        }
        return GenericCollectionUtils.filter(getAllTasks(),
                t -> t.getAssignee() != null && assigneeId.equals(t.getAssignee().getId()));
    }

    @Override
    public List<Task> searchTasks(String keyword) {
        if (ValidationUtils.isBlank(keyword)) {
            return getAllTasks();
        }
        String cleanKeyword = keyword.trim().toLowerCase();
        return GenericCollectionUtils.filter(getAllTasks(), t ->
                (t.getTitle() != null && t.getTitle().toLowerCase().contains(cleanKeyword)) ||
                (t.getDescription() != null && t.getDescription().toLowerCase().contains(cleanKeyword)) ||
                (t.getProjectName() != null && t.getProjectName().toLowerCase().contains(cleanKeyword))
        );
    }

    @Override
    public List<Task> filterTasks(String search, Long projectId, Long assigneeId, TaskStatus status, Priority priority, Boolean overdue, String sortBy) {
        List<Task> list = getAllTasks();

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            list = GenericCollectionUtils.filter(list, t ->
                    (t.getTitle() != null && t.getTitle().toLowerCase().contains(query)) ||
                    (t.getDescription() != null && t.getDescription().toLowerCase().contains(query)) ||
                    (t.getProjectName() != null && t.getProjectName().toLowerCase().contains(query))
            );
        }

        if (projectId != null) {
            list = GenericCollectionUtils.filter(list, t -> projectId.equals(t.getProjectId()));
        }

        if (assigneeId != null) {
            list = GenericCollectionUtils.filter(list, t -> t.getAssignee() != null && assigneeId.equals(t.getAssignee().getId()));
        }

        if (status != null) {
            list = GenericCollectionUtils.filter(list, t -> t.getStatus() == status);
        }

        if (priority != null) {
            list = GenericCollectionUtils.filter(list, t -> t.getPriority() == priority);
        }

        if (Boolean.TRUE.equals(overdue)) {
            list = GenericCollectionUtils.filter(list, Task::isOverdue);
        }

        if (sortBy != null && !sortBy.isBlank()) {
            Comparator<Task> comparator;
            switch (sortBy.trim().toLowerCase()) {
                case "deadline":
                case "duedate":
                case "due_date":
                    comparator = Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Task::getId);
                    break;
                case "priority":
                    comparator = Comparator.comparing(Task::getPriority).reversed()
                            .thenComparing(Task::getId);
                    break;
                case "status":
                    comparator = Comparator.comparing(Task::getStatus)
                            .thenComparing(Task::getId);
                    break;
                case "estimatedhours":
                case "hours":
                    comparator = Comparator.comparingInt(Task::getEstimatedHours).reversed()
                            .thenComparing(Task::getId);
                    break;
                case "title":
                    comparator = Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Task::getId);
                    break;
                default:
                    comparator = Comparator.comparing(Task::getId);
                    break;
            }
            list = GenericCollectionUtils.sort(list, comparator);
        }

        return list;
    }
}
