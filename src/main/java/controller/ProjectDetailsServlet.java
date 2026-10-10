package controller;

import exception.ProjectNotFoundException;
import exception.ResourceNotFoundException;
import exception.UnauthorizedAccessException;
import exception.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.*;
import service.ProjectService;
import service.ServiceFactory;
import service.TaskService;
import service.UserService;
import util.SecurityUtils;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Controller providing dedicated deep-dive view and management for a single project:
 * - Project metadata, dates, budget, and dynamic progress bar
 * - Assigned team members roster with Add / Remove actions
 * - Project tasks list with status, priority, assignee, deadline, progress/hours
 * - Inline task creation and quick completion
 *
 * Enforces server-side RBAC and resource-level authorization checks:
 * - ADMIN: Unrestricted access across all projects
 * - PROJECT_MANAGER: Full control over managed projects
 * - TEAM_MEMBER: Read-only access to assigned projects; can only update task status for assigned tasks
 */
@WebServlet(name = "ProjectDetailsServlet", urlPatterns = {"/project-details"})
public class ProjectDetailsServlet extends HttpServlet {

    private ProjectService projectService;
    private TaskService taskService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.projectService = ServiceFactory.getInstance().getProjectService();
        this.taskService = ServiceFactory.getInstance().getTaskService();
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public ProjectDetailsServlet() {}

    public ProjectDetailsServlet(ProjectService projectService, TaskService taskService, UserService userService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            return (User) session.getAttribute("currentUser");
        }
        return userService.getCurrentUser();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getAuthenticatedUser(request);
        String idParam = request.getParameter("id");
        if (ValidationUtils.isBlank(idParam)) {
            idParam = request.getParameter("projectId");
        }

        if (ValidationUtils.isBlank(idParam)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.sendRedirect(request.getContextPath() + "/projects?error=" +
                    URLEncoder.encode("Please select a valid project to view details.", StandardCharsets.UTF_8));
            return;
        }

        Long projectId = ValidationUtils.parseLongSafe(idParam, null);
        if (projectId == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.sendRedirect(request.getContextPath() + "/projects?error=" +
                    URLEncoder.encode("Invalid project identifier.", StandardCharsets.UTF_8));
            return;
        }

        try {
            // Load Project domain entity
            Project project = projectService.getProjectById(projectId);
            Set<User> teamMembers = projectService.getProjectTeamMembers(projectId);

            // Server-side RBAC: Verify resource access
            SecurityUtils.requireCanViewProject(currentUser, project, teamMembers);

            // Handle inline GET actions
            String action = request.getParameter("action");
            if ("removeMember".equalsIgnoreCase(action)) {
                SecurityUtils.requireCanManageProjectTeam(currentUser, project);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (userId != null) {
                    projectService.removeTeamMemberFromProject(projectId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Team member removed from project.", StandardCharsets.UTF_8));
                return;
            }

            if ("taskStatus".equalsIgnoreCase(action)) {
                Long taskId = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                String newStatus = request.getParameter("newStatus");
                if (taskId != null && ValidationUtils.isNotBlank(newStatus)) {
                    Task task = taskService.getTaskById(taskId);
                    SecurityUtils.requireCanUpdateTaskStatus(currentUser, task);
                    taskService.updateTaskStatus(taskId, TaskStatus.valueOf(newStatus.toUpperCase()));
                }
                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Task status updated.", StandardCharsets.UTF_8));
                return;
            }

            if ("deleteTask".equalsIgnoreCase(action)) {
                Long taskId = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                if (taskId != null) {
                    Task task = taskService.getTaskById(taskId);
                    SecurityUtils.requireCanDeleteTask(currentUser, task, project);
                    taskService.deleteTask(taskId);
                }
                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Task deleted.", StandardCharsets.UTF_8));
                return;
            }

            // Load associated team members and project tasks
            List<Task> tasks = taskService.getTasksByProjectId(projectId);
            List<User> allUsers = userService.getAllUsers();

            // Calculate project-specific task metrics dynamically
            int totalTasks = tasks.size();
            long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
            long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
            long overdueTasks = tasks.stream().filter(Task::isOverdue).count();
            int progress = totalTasks == 0 ? 0 : (int) Math.round((completedTasks * 100.0) / totalTasks);

            project.updateTaskMetrics(totalTasks, (int) completedTasks);

            boolean canManageProject = SecurityUtils.isAdmin(currentUser) ||
                    (SecurityUtils.isProjectManager(currentUser) && (project.getManagerId() == null || project.getManagerId().equals(currentUser.getId())));
            request.setAttribute("canManageProject", canManageProject);
            request.setAttribute("project", project);
            request.setAttribute("teamMembers", teamMembers);
            request.setAttribute("tasks", tasks);
            request.setAttribute("allUsers", allUsers);
            request.setAttribute("totalTasks", totalTasks);
            request.setAttribute("completedTasks", completedTasks);
            request.setAttribute("inProgressTasks", inProgressTasks);
            request.setAttribute("overdueTasks", overdueTasks);
            request.setAttribute("progressPercentage", progress);
            request.setAttribute("activeNav", "projects");
            request.setAttribute("pageTitle", project.getName() + " - Overview");

            request.getRequestDispatcher("/WEB-INF/views/project-details.jsp").forward(request, response);

        } catch (UnauthorizedAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("errorCode", 403);
            request.setAttribute("errorMessage", "Access Denied: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.sendRedirect(request.getContextPath() + "/projects?error=" +
                    URLEncoder.encode("Project not found.", StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[ProjectDetailsServlet Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/projects?error=" +
                    URLEncoder.encode("Unable to load project details.", StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getAuthenticatedUser(request);
        String action = request.getParameter("action");
        String idParam = request.getParameter("id");
        if (ValidationUtils.isBlank(idParam)) {
            idParam = request.getParameter("projectId");
        }
        Long projectId = ValidationUtils.parseLongSafe(idParam, null);

        if (projectId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.sendRedirect(request.getContextPath() + "/projects?error=" +
                    URLEncoder.encode("Project ID required.", StandardCharsets.UTF_8));
            return;
        }

        try {
            Project project = projectService.getProjectById(projectId);

            // 1. Add Task directly to this Project
            if ("addTask".equalsIgnoreCase(action)) {
                SecurityUtils.requireCanCreateTask(currentUser, project);

                String title = request.getParameter("title");
                String description = request.getParameter("description");
                String priorityStr = request.getParameter("priority");
                String assigneeIdStr = request.getParameter("assigneeId");
                String dueDateStr = request.getParameter("dueDate");
                String estimatedHoursStr = request.getParameter("estimatedHours");

                if (ValidationUtils.isBlank(title) || !ValidationUtils.isValidLength(title, 2, 200)) {
                    throw new ValidationException("Task title is required (2 to 200 characters).");
                }

                Long assigneeId = ValidationUtils.parseLongSafe(assigneeIdStr, null);
                User assignee = assigneeId != null ? userService.getUserById(assigneeId) : null;

                Task task = new Task();
                task.setTitle(ValidationUtils.sanitize(title));
                task.setDescription(ValidationUtils.sanitize(description));
                task.setProjectId(projectId);
                task.setProjectName(project.getName());
                task.setStatus(TaskStatus.TODO);

                if (ValidationUtils.isNotBlank(priorityStr)) {
                    task.setPriority(Priority.valueOf(priorityStr.toUpperCase()));
                } else {
                    task.setPriority(Priority.MEDIUM);
                }

                task.setAssignee(assignee);
                task.setReporter(currentUser);

                LocalDate dueDate = ValidationUtils.parseLocalDateSafe(dueDateStr, LocalDate.now().plusDays(7));
                task.setDueDate(dueDate);
                task.setEstimatedHours(ValidationUtils.parseIntSafe(estimatedHoursStr, 8));
                task.setLoggedHours(0);

                taskService.createTask(task);

                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Task added to project.", StandardCharsets.UTF_8));
                return;
            }

            // 2. Add Team Member to this Project
            if ("addMember".equalsIgnoreCase(action)) {
                SecurityUtils.requireCanManageProjectTeam(currentUser, project);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (userId != null) {
                    projectService.addTeamMemberToProject(projectId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Team member added to project.", StandardCharsets.UTF_8));
                return;
            }

            // 3. Remove Team Member from this Project
            if ("removeMember".equalsIgnoreCase(action)) {
                SecurityUtils.requireCanManageProjectTeam(currentUser, project);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (userId != null) {
                    projectService.removeTeamMemberFromProject(projectId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Team member removed from project.", StandardCharsets.UTF_8));
                return;
            }

            // 4. Update Project Details
            if ("updateProject".equalsIgnoreCase(action)) {
                SecurityUtils.requireCanUpdateProject(currentUser, project);

                String updatedName = request.getParameter("name");
                String updatedCode = request.getParameter("code");
                String updatedDesc = request.getParameter("description");
                String updatedStatus = request.getParameter("status");
                String updatedPriority = request.getParameter("priority");
                String updatedBudget = request.getParameter("budget");
                String updatedEndDate = request.getParameter("endDate");

                if (ValidationUtils.isNotBlank(updatedName)) {
                    if (!ValidationUtils.isValidLength(updatedName, 3, 150)) {
                        throw new ValidationException("Project name must be between 3 and 150 characters.");
                    }
                    project.setName(ValidationUtils.sanitize(updatedName));
                }
                if (ValidationUtils.isNotBlank(updatedCode)) {
                    project.setCode(ValidationUtils.sanitize(updatedCode));
                }
                if (updatedDesc != null) {
                    project.setDescription(ValidationUtils.sanitize(updatedDesc));
                }
                if (ValidationUtils.isNotBlank(updatedStatus)) {
                    project.setStatus(ProjectStatus.valueOf(updatedStatus.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(updatedPriority)) {
                    project.setPriority(Priority.valueOf(updatedPriority.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(updatedBudget)) {
                    project.setBudget(ValidationUtils.parseDoubleSafe(updatedBudget, project.getBudget()));
                }
                if (ValidationUtils.isNotBlank(updatedEndDate)) {
                    LocalDate parsedEnd = ValidationUtils.parseLocalDateSafe(updatedEndDate, null);
                    if (parsedEnd != null) {
                        project.setEndDate(parsedEnd);
                    }
                }

                if (!ValidationUtils.isValidDateRange(project.getStartDate(), project.getEndDate())) {
                    throw new ValidationException("Project end date cannot be earlier than start date.");
                }

                projectService.updateProject(project);

                response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&success=" +
                        URLEncoder.encode("Project details updated successfully.", StandardCharsets.UTF_8));
                return;
            }

        } catch (UnauthorizedAccessException e) {
            System.err.printf("[Security Warning] Unauthorized project-details action: %s%n", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&error=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&error=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[ProjectDetailsServlet POST Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/project-details?id=" + projectId + "&error=" +
                    URLEncoder.encode("Failed to process project request.", StandardCharsets.UTF_8));
        }
    }
}
