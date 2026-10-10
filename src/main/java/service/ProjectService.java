package service;

import model.Priority;
import model.Project;
import model.ProjectStatus;
import model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service contract for Project lifecycle management and business operations.
 * Demonstrates Collections (List, Set, Map) and Generics integration.
 */
public interface ProjectService {

    // =========================================================================
    // [Collections: List]
    // =========================================================================
    List<Project> getAllProjects();

    Project getProjectById(Long id);

    Project createProject(Project project);

    boolean updateProject(Project project);

    boolean deleteProject(Long id);

    List<Project> getProjectsByStatus(ProjectStatus status);

    List<Project> getRecentProjects(int limit);

    // =========================================================================
    // [Collections: Set]
    // Prevents duplicate team members and manages unique project contributors
    // =========================================================================
    boolean addTeamMemberToProject(Long projectId, User user);

    boolean addTeamMemberToProject(Long projectId, Long userId);

    boolean removeTeamMemberFromProject(Long projectId, Long userId);

    Set<User> getProjectTeamMembers(Long projectId);

    Set<User> getAllUniqueProjectContributors();

    // =========================================================================
    // [Collections: Map]
    // Fast O(1) ID lookups and dynamic statistical aggregations
    // =========================================================================
    Map<Long, Project> getProjectMap();

    Map<ProjectStatus, Long> getProjectCountByStatus();

    Map<Priority, Long> getProjectCountByPriority();

    Map<ProjectStatus, List<Project>> getProjectsGroupedByStatus();

    // =========================================================================
    // [Collections: Sorting]
    // Supports: "startDate", "endDate", "progress", "priority", "name"
    // =========================================================================
    List<Project> getProjectsSortedBy(String sortBy);

    // =========================================================================
    // [Collections: Filtering & Search]
    // =========================================================================
    List<Project> filterProjectsByStatus(ProjectStatus status);

    List<Project> filterProjectsByPriority(Priority priority);

    List<Project> searchProjects(String query);

    /**
     * Combined multi-criteria project filtering and sorting.
     *
     * @param search   optional keyword query for name, code, description
     * @param status   optional ProjectStatus filter
     * @param priority optional Priority filter
     * @param sortBy   optional sort field ("progress", "priority", "startDate", "endDate", "name")
     * @return filtered and sorted project list
     */
    List<Project> filterProjects(String search, ProjectStatus status, Priority priority, String sortBy);
}

