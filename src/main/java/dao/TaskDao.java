package dao;

import model.Priority;
import model.Task;
import model.TaskStatus;
import java.util.List;

/**
 * Data Access Object contract for Task work items.
 */
public interface TaskDao extends BaseDao<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findPriorityTasks(int limit);

    List<Task> findOverdueTasks();

    List<Task> searchByKeyword(String keyword);

    int countByStatus(TaskStatus status);

    boolean assignTask(Long taskId, Long assigneeId);
}
