package dao;

import model.User;
import java.util.Set;

/**
 * ==============================================================================
 * Database Operations: Team Member DAO Contract (7 marks foundation)
 * ==============================================================================
 *
 * Contract defining operations for Project Team Member associations.
 * Manages the many-to-many relationship between Projects and Users stored in the
 * 'team_members' join table in MySQL.
 * 
 * Enforces:
 * - Duplicate prevention via database PRIMARY KEY / UNIQUE constraints.
 * - Set collections to guarantee identity uniqueness in Java domain models.
 */
public interface TeamMemberDao {

    /**
     * Associates a user with a project.
     * Prevents duplicate assignments via database UNIQUE constraints.
     *
     * @param projectId project primary identifier
     * @param userId    user primary identifier
     * @return true if added, false if association already exists
     */
    boolean addTeamMember(Long projectId, Long userId);

    /**
     * Removes a user's association from a project.
     *
     * @param projectId project primary identifier
     * @param userId    user primary identifier
     * @return true if removed, false if association was not found
     */
    boolean removeTeamMember(Long projectId, Long userId);

    /**
     * Retrieves all unique team members assigned to a specific project.
     *
     * @param projectId project primary identifier
     * @return Set of unique Users assigned to this project
     */
    Set<User> findProjectTeam(Long projectId);

    /**
     * Checks if a user is an active team member of a project.
     *
     * @param projectId project primary identifier
     * @param userId    user primary identifier
     * @return true if association exists
     */
    boolean isMember(Long projectId, Long userId);

    /**
     * Counts the total number of team members assigned to a project.
     *
     * @param projectId project primary identifier
     * @return count of members
     */
    int countByProjectId(Long projectId);

    /**
     * Removes all team member associations for a given project (e.g. during project deletion).
     *
     * @param projectId project primary identifier
     */
    void removeAllForProject(Long projectId);
}
