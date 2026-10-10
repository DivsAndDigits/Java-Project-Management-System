package dao;

import model.Priority;
import model.Project;
import model.ProjectStatus;
import java.util.List;

/**
 * Data Access Object contract for Project entities.
 */
public interface ProjectDao extends BaseDao<Project, Long> {

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByPriority(Priority priority);

    List<Project> findRecent(int limit);

    List<Project> findByManagerId(Long managerId);

    List<Project> searchByName(String keyword);

    int countByStatus(ProjectStatus status);

    /**
     * [Database Operations: Transactions]
     * Atomically creates a new project and assigns initial team members within a single
     * ACID transaction (setAutoCommit(false), commit, rollback on failure).
     *
     * @param project       the project entity to persist
     * @param memberUserIds set of user identifiers to assign to this project
     * @return the saved Project with generated ID
     */
    Project createProjectWithTeam(Project project, java.util.Set<Long> memberUserIds);
}
