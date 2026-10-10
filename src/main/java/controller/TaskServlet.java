package controller;

import exception.ResourceNotFoundException;
import exception.TaskNotFoundException;
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

/**
 * Controller handling Task CRUD and sprint management operations:
 * - Create task (POST)
 * - List tasks (GET)
 * - Assign task (POST/GET action=assign)
 * - Update task status (POST/GET action=status)
 * - Delete task (POST/GET action=delete)
 *
 * Enforces server-side RBAC:
 * - ADMIN / PROJECT_MANAGER: Full task planning, assignment, creation, and deletion
 * - TEAM_MEMBER: Read-only access to assigned tasks; can only update status of assigned tasks
 */
@WebServlet(name = "TaskServlet", urlPatterns = {"/tasks"})
public class TaskServlet extends HttpServlet {

    private TaskService taskService;
    private ProjectService projectService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.taskService = ServiceFactory.getInstance().getTaskService();
        this.projectService = ServiceFactory.getInstance().getProjectService();
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public TaskServlet() {}

    public TaskServlet(TaskService taskService, ProjectService projectService, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
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
        String action = request.getParameter("action");
        String idParam = request.getParameter("id");
        if (ValidationUtils.isBlank(idParam)) {
            idParam = request.getParameter("taskId");
        }

        try {
            // 1. Delete Task via GET
            if ("delete".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long taskId = ValidationUtils.parseLongSafe(idParam, null);
                if (taskId != null) {
                    Task task = taskService.getTaskById(taskId);
                    Project project = task.getProjectId() != null ? projectService.getProjectById(task.getProjectId()) : null;
                    SecurityUtils.requireCanDeleteTask(currentUser, task, project);
                    taskService.deleteTask(taskId);
                }
                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Task deleted successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 2. Update Status via GET
            if ("status".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long taskId = ValidationUtils.parseLongSafe(idParam, null);
                String newStatusStr = request.getParameter("newStatus");
                if (ValidationUtils.isBlank(newStatusStr)) {
                    newStatusStr = request.getParameter("status");
                }
                if (taskId != null && ValidationUtils.isNotBlank(newStatusStr)) {
                    Task task = taskService.getTaskById(taskId);
                    SecurityUtils.requireCanUpdateTaskStatus(currentUser, task);
                    taskService.updateTaskStatus(taskId, TaskStatus.valueOf(newStatusStr.toUpperCase()));
                }
                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Task status updated.", StandardCharsets.UTF_8));
                return;
            }

            // 3. Assign Task via GET
            if ("assign".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long taskId = ValidationUtils.parseLongSafe(idParam, null);
                Long assigneeId = ValidationUtils.parseLongSafe(request.getParameter("assigneeId"), null);
                if (taskId != null) {
                    Task task = taskService.getTaskById(taskId);
                    Project project = task.getProjectId() != null ? projectService.getProjectById(task.getProjectId()) : null;
                    SecurityUtils.requireCanAssignTask(currentUser, project);
                    taskService.assignTask(taskId, assigneeId);
                }
                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Task assignment updated.", StandardCharsets.UTF_8));
                return;
            }

            // 4. View Task details
            if ("view".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long taskId = ValidationUtils.parseLongSafe(idParam, null);
                if (taskId != null) {
                    Task selected = taskService.getTaskById(taskId);
                    request.setAttribute("selectedTask", selected);
                }
            }

            // 5. List Tasks (with combined filter, search, sort)
            String statusParam = request.getParameter("status");
            String projectIdParam = request.getParameter("projectId");
            String assigneeIdParam = request.getParameter("assigneeId");
            String priorityParam = request.getParameter("priority");
            String searchParam = request.getParameter("search");
            String sortParam = request.getParameter("sort");
            String overdueParam = request.getParameter("overdue");

            TaskStatus status = null;
            if (ValidationUtils.isNotBlank(statusParam) && !"ALL".equalsIgnoreCase(statusParam)) {
                try {
                    status = TaskStatus.valueOf(statusParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            Priority priority = null;
            if (ValidationUtils.isNotBlank(priorityParam) && !"ALL".equalsIgnoreCase(priorityParam)) {
                try {
                    priority = Priority.valueOf(priorityParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            Long projectId = ValidationUtils.parseLongSafe(projectIdParam, null);
            Long assigneeId = ValidationUtils.parseLongSafe(assigneeIdParam, null);

            // TEAM_MEMBER: Default to tasks assigned to current user unless filtered
            if (currentUser != null && currentUser.getRole() == Role.TEAM_MEMBER && assigneeId == null) {
                assigneeId = currentUser.getId();
            }

            Boolean overdue = "true".equalsIgnoreCase(overdueParam) ? Boolean.TRUE : null;

            List<Task> tasks = taskService.filterTasks(searchParam, projectId, assigneeId, status, priority, overdue, sortParam);

            // Set presentation attributes for JSP
            request.setAttribute("tasks", tasks);
            request.setAttribute("projects", projectService.getAllProjects());
            request.setAttribute("users", userService.getAllUsers());
            request.setAttribute("taskStatuses", TaskStatus.values());
            request.setAttribute("priorities", Priority.values());
            request.setAttribute("selectedStatus", statusParam != null ? statusParam : "ALL");
            request.setAttribute("selectedPriority", priorityParam != null ? priorityParam : "ALL");
            request.setAttribute("selectedSort", sortParam != null ? sortParam : "");
            request.setAttribute("searchQuery", searchParam != null ? searchParam : "");
            request.setAttribute("selectedProjectId", projectIdParam);
            request.setAttribute("activeNav", "tasks");
            request.setAttribute("pageTitle", "Task Management");

            request.getRequestDispatcher("/WEB-INF/views/tasks.jsp").forward(request, response);

        } catch (UnauthorizedAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode("Task not found.", StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[TaskServlet GET Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode("Unable to complete request. Please try again.", StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getAuthenticatedUser(request);
        String action = request.getParameter("action");

        try {
            // 1. Update Status via POST
            if ("status".equalsIgnoreCase(action)) {
                Long id = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (id == null) {
                    id = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                }
                if (id == null) {
                    throw new ValidationException("Task ID is required.");
                }
                Task task = taskService.getTaskById(id);
                SecurityUtils.requireCanUpdateTaskStatus(currentUser, task);

                String newStatusStr = request.getParameter("newStatus");
                if (ValidationUtils.isBlank(newStatusStr)) {
                    newStatusStr = request.getParameter("status");
                }
                if (ValidationUtils.isNotBlank(newStatusStr)) {
                    taskService.updateTaskStatus(id, TaskStatus.valueOf(newStatusStr.toUpperCase()));
                }
                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Status updated.", StandardCharsets.UTF_8));
                return;
            }

            // 2. Assign Task via POST
            if ("assign".equalsIgnoreCase(action)) {
                Long id = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (id == null) {
                    id = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                }
                if (id == null) {
                    throw new ValidationException("Task ID is required.");
                }
                Task task = taskService.getTaskById(id);
                Project project = task.getProjectId() != null ? projectService.getProjectById(task.getProjectId()) : null;
                SecurityUtils.requireCanAssignTask(currentUser, project);

                Long assigneeId = ValidationUtils.parseLongSafe(request.getParameter("assigneeId"), null);
                taskService.assignTask(id, assigneeId);

                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Assignee updated.", StandardCharsets.UTF_8));
                return;
            }

            // 3. Delete Task via POST
            if ("delete".equalsIgnoreCase(action)) {
                Long id = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (id == null) {
                    id = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                }
                if (id == null) {
                    throw new ValidationException("Task ID is required.");
                }
                Task task = taskService.getTaskById(id);
                Project project = task.getProjectId() != null ? projectService.getProjectById(task.getProjectId()) : null;
                SecurityUtils.requireCanDeleteTask(currentUser, task, project);

                taskService.deleteTask(id);

                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Task deleted.", StandardCharsets.UTF_8));
                return;
            }

            // 4. Update Task (full edit)
            if ("update".equalsIgnoreCase(action)) {
                Long id = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (id == null) {
                    id = ValidationUtils.parseLongSafe(request.getParameter("taskId"), null);
                }
                if (id == null) {
                    throw new ValidationException("Task ID is required for update.");
                }

                Task task = taskService.getTaskById(id);
                Project currentProject = task.getProjectId() != null ? projectService.getProjectById(task.getProjectId()) : null;
                SecurityUtils.requireCanEditTask(currentUser, task, currentProject);

                String title = request.getParameter("title");
                String description = request.getParameter("description");
                String projectIdStr = request.getParameter("projectId");
                String assigneeIdStr = request.getParameter("assigneeId");
                String statusStr = request.getParameter("status");
                String priorityStr = request.getParameter("priority");
                String dueDateStr = request.getParameter("dueDate");
                String estimatedHoursStr = request.getParameter("estimatedHours");
                String loggedHoursStr = request.getParameter("loggedHours");

                if (ValidationUtils.isNotBlank(title)) {
                    if (!ValidationUtils.isValidLength(title, 2, 200)) {
                        throw new ValidationException("Task title must be between 2 and 200 characters.");
                    }
                    task.setTitle(ValidationUtils.sanitize(title));
                }
                if (description != null) {
                    task.setDescription(ValidationUtils.sanitize(description));
                }
                if (ValidationUtils.isNotBlank(projectIdStr)) {
                    Long pId = ValidationUtils.parseLongSafe(projectIdStr, null);
                    if (pId != null) {
                        task.setProjectId(pId);
                        Project proj = projectService.getProjectById(pId);
                        task.setProjectName(proj.getName());
                    }
                }
                if (ValidationUtils.isNotBlank(assigneeIdStr)) {
                    Long aId = ValidationUtils.parseLongSafe(assigneeIdStr, null);
                    if (aId != null) {
                        task.setAssignee(userService.getUserById(aId));
                    }
                }
                if (ValidationUtils.isNotBlank(statusStr)) {
                    task.setStatus(TaskStatus.valueOf(statusStr.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(priorityStr)) {
                    task.setPriority(Priority.valueOf(priorityStr.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(dueDateStr)) {
                    LocalDate parsedDue = ValidationUtils.parseLocalDateSafe(dueDateStr, null);
                    if (parsedDue != null) {
                        task.setDueDate(parsedDue);
                    }
                }
                if (ValidationUtils.isNotBlank(estimatedHoursStr)) {
                    task.setEstimatedHours(ValidationUtils.parseIntSafe(estimatedHoursStr, task.getEstimatedHours()));
                }
                if (ValidationUtils.isNotBlank(loggedHoursStr)) {
                    task.setLoggedHours(ValidationUtils.parseIntSafe(loggedHoursStr, task.getLoggedHours()));
                }

                taskService.updateTask(task);

                response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                        URLEncoder.encode("Task updated successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 5. Create Task (default POST action)
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String projectIdStr = request.getParameter("projectId");
            String assigneeIdStr = request.getParameter("assigneeId");
            String priorityStr = request.getParameter("priority");
            String dueDateStr = request.getParameter("dueDate");
            String estimatedHoursStr = request.getParameter("estimatedHours");

            if (ValidationUtils.isBlank(title) || !ValidationUtils.isValidLength(title, 2, 200)) {
                throw new ValidationException("Task title is required (2 to 200 characters).");
            }
            if (ValidationUtils.isBlank(projectIdStr)) {
                throw new ValidationException("Project selection is required.");
            }

            Long projectId = ValidationUtils.parseLongSafe(projectIdStr, null);
            if (projectId == null) {
                throw new ValidationException("Valid project identifier is required.");
            }

            Project project = projectService.getProjectById(projectId);
            SecurityUtils.requireCanCreateTask(currentUser, project);

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

            response.sendRedirect(request.getContextPath() + "/tasks?success=" + 
                    URLEncoder.encode("Task created successfully.", StandardCharsets.UTF_8));

        } catch (UnauthorizedAccessException e) {
            System.err.printf("[Security Warning] Unauthorized task action: %s%n", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (ValidationException | ResourceNotFoundException e) {
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[TaskServlet POST Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/tasks?error=" + 
                    URLEncoder.encode("An unexpected error occurred while saving the task.", StandardCharsets.UTF_8));
        }
    }
}
