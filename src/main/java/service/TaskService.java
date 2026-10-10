package service;

import model.Priority;
import model.Task;
import model.TaskStatus;

import java.util.List;
import java.util.Map;

/**
 * Service contract for Task management and assignment.
 * Demonstrates Collections (List, Map) and Generics integration.
 */
public interface TaskService {

    // =========================================================================
    // [Collections: List]
    // =========================================================================
    List<Task> getAllTasks();

    Task getTaskById(Long id);

    Task createTask(Task task);

    boolean updateTask(Task task);

    /**
     * [Multithreading Concept: Concurrent Task Update Safety]
     * Safely transitions task status and coordinates atomic project progress recalculation.
     */
    boolean updateTaskStatus(Long taskId, TaskStatus newStatus);

    boolean deleteTask(Long id);

    /**
     * Assigns a task to a user.
     *
     * @param taskId     task ID
     * @param assigneeId user ID to assign, or null to unassign
     * @return true if successfully assigned
     */
    boolean assignTask(Long taskId, Long assigneeId);

    List<Task> getTasksByProjectId(Long projectId);

    List<Task> getTasksByAssigneeId(Long assigneeId);

    // Overloads accepting integer IDs matching academic prompt specifications
    List<Task> getTasksByProject(int projectId);

    List<Task> getTasksAssignedToUser(int userId);

    List<Task> getTasksByStatus(TaskStatus status);

    List<Task> getPriorityTasks(int limit);

    List<Task> getOverdueTasks();

    // =========================================================================
    // [Collections: Map]
    // O(1) indexed lookup and dynamic aggregations
    // =========================================================================
    Map<Long, Task> getTaskMap();

    Map<TaskStatus, Long> getTaskCountByStatus();

    Map<Priority, Long> getTaskCountByPriority();

    Map<TaskStatus, List<Task>> getTasksGroupedByStatus();

    Map<Priority, List<Task>> getTasksGroupedByPriority();

    // =========================================================================
    // [Collections: Sorting]
    // Supports: "deadline", "duedate", "priority", "status", "estimatedhours", "title"
    // =========================================================================
    List<Task> getTasksSortedBy(String sortBy);

    // =========================================================================
    // [Collections: Filtering & Search]
    // =========================================================================
    List<Task> filterTasksByStatus(TaskStatus status);

    List<Task> filterTasksByPriority(Priority priority);

    List<Task> filterTasksByProject(Long projectId);

    List<Task> filterTasksByAssignee(Long assigneeId);

    List<Task> searchTasks(String keyword);

    /**
     * Combined multi-criteria task filtering and sorting.
     *
     * @param search     optional title/description keyword
     * @param projectId  optional project filter
     * @param assigneeId optional assignee filter
     * @param status     optional status filter
     * @param priority   optional priority filter
     * @param overdue    optional boolean filter for overdue tasks
     * @param sortBy     optional sort field ("deadline", "priority", "status", "hours", "title")
     * @return filtered and sorted task list
     */
    List<Task> filterTasks(String search, Long projectId, Long assigneeId, TaskStatus status, Priority priority, Boolean overdue, String sortBy);
}
