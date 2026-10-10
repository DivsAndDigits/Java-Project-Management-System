package util;

import exception.UnauthorizedAccessException;
import model.Project;
import model.Role;
import model.Task;
import model.User;

import java.util.Set;

/**
 * Server-side Role-Based Access Control (RBAC) and Resource-Level authorization validator.
 * 
 * Enforces permissions at the server layer:
 * - ADMIN: Full management over users, projects, and teams.
 * - PROJECT_MANAGER: Project creation/updates, task creation/assignment, and managing project team members.
 * - TEAM_MEMBER: Read-only access to assigned projects and tasks, with ability to update task status.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public static boolean isProjectManager(User user) {
        return user != null && user.getRole() == Role.PROJECT_MANAGER;
    }

    public static boolean isTeamMember(User user) {
        return user != null && user.getRole() == Role.TEAM_MEMBER;
    }

    /**
     * Checks if user has permission to manage users (create, edit, delete user accounts).
     */
    public static void requireCanManageUsers(User user) {
        if (!isAdmin(user)) {
            String name = user != null ? user.getUsername() : "anonymous";
            throw new UnauthorizedAccessException(name, "User administration requires Administrator privileges.");
        }
    }

    /**
     * Checks if user has permission to create new projects.
     */
    public static void requireCanCreateProject(User user) {
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.PROJECT_MANAGER)) {
            String name = user != null ? user.getUsername() : "anonymous";
            throw new UnauthorizedAccessException(name, "Creating projects requires Project Manager or Administrator role.");
        }
    }

    /**
     * Checks if user has permission to delete a project.
     */
    public static void requireCanDeleteProject(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Deleting projects is restricted to Administrators and the assigned Project Manager.");
    }

    /**
     * Checks if user has permission to edit project metadata.
     */
    public static void requireCanUpdateProject(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Updating this project requires Project Manager ownership or Administrator privileges.");
    }

    /**
     * Checks if user has permission to add or remove team members from a project.
     */
    public static void requireCanManageProjectTeam(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Managing project members requires Project Manager ownership or Administrator privileges.");
    }

    /**
     * Checks if user has permission to view a project and its details.
     */
    public static void requireCanViewProject(User user, Project project, Set<User> teamMembers) {
        if (user == null) {
            throw new UnauthorizedAccessException("anonymous", "Authentication required to view project.");
        }
        if (isAdmin(user) || isProjectManager(user)) {
            return;
        }
        // Team member check: must be assigned to project roster or manager
        if (project != null && project.getManagerId() != null && project.getManagerId().equals(user.getId())) {
            return;
        }
        if (teamMembers != null && teamMembers.stream().anyMatch(u -> u.getId() != null && u.getId().equals(user.getId()))) {
            return;
        }
        throw new UnauthorizedAccessException(user.getUsername(), "You are not a designated member of this project.");
    }

    /**
     * Checks if user has permission to create tasks in a project.
     */
    public static void requireCanCreateTask(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Creating and scheduling tasks requires Project Manager or Administrator role.");
    }

    /**
     * Checks if user has permission to assign tasks to team members.
     */
    public static void requireCanAssignTask(User user, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Assigning tasks is restricted to Project Managers and Administrators.");
    }

    /**
     * Checks if user has permission to delete a task.
     */
    public static void requireCanDeleteTask(User user, Task task, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Deleting tasks is restricted to Project Managers and Administrators.");
    }

    /**
     * Checks if user has permission to edit task metadata (title, dates, estimation).
     */
    public static void requireCanEditTask(User user, Task task, Project project) {
        if (isAdmin(user)) {
            return;
        }
        if (isProjectManager(user)) {
            if (project == null || project.getManagerId() == null || project.getManagerId().equals(user.getId())) {
                return;
            }
        }
        String name = user != null ? user.getUsername() : "anonymous";
        throw new UnauthorizedAccessException(name, "Editing task definitions requires Project Manager or Administrator privileges.");
    }

    /**
     * Checks if user has permission to update the status of a task.
     */
    public static void requireCanUpdateTaskStatus(User user, Task task) {
        if (user == null) {
            throw new UnauthorizedAccessException("anonymous", "Authentication required to update task status.");
        }
        if (isAdmin(user) || isProjectManager(user)) {
            return;
        }
        // Team member can update tasks assigned to them
        Long assigneeId = task.getAssigneeId();
        if (assigneeId == null && task.getAssignee() != null) {
            assigneeId = task.getAssignee().getId();
        }
        if (assigneeId != null && assigneeId.equals(user.getId())) {
            return;
        }
        throw new UnauthorizedAccessException(user.getUsername(), "Team members can only update the status of tasks assigned to them.");
    }
}
