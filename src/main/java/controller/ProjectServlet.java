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
import model.Priority;
import model.Project;
import model.ProjectStatus;
import model.Role;
import model.User;
import service.ProjectService;
import service.ServiceFactory;
import service.UserService;
import util.SecurityUtils;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller handling Project CRUD and portfolio operations:
 * - Create project (POST)
 * - List projects (GET)
 * - View project (GET action=view)
 * - Update project (POST action=update, GET action=edit)
 * - Delete project (POST/GET action=delete)
 *
 * Enforces server-side RBAC:
 * - ADMIN: Full control (create, update, delete any project, manage members)
 * - PROJECT_MANAGER: Create projects, update and manage team members of managed projects
 * - TEAM_MEMBER: Read-only access to assigned projects
 */
@WebServlet(name = "ProjectServlet", urlPatterns = {"/projects"})
public class ProjectServlet extends HttpServlet {

    private ProjectService projectService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.projectService = ServiceFactory.getInstance().getProjectService();
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public ProjectServlet() {}

    public ProjectServlet(ProjectService projectService, UserService userService) {
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

        try {
            // 1. Delete Project via GET link
            if ("delete".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long projId = ValidationUtils.parseLongSafe(idParam, null);
                if (projId != null) {
                    Project target = projectService.getProjectById(projId);
                    SecurityUtils.requireCanDeleteProject(currentUser, target);
                    projectService.deleteProject(projId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Project deleted successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 2. View Project details
            if ("view".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long projId = ValidationUtils.parseLongSafe(idParam, null);
                if (projId != null) {
                    Project selected = projectService.getProjectById(projId);
                    SecurityUtils.requireCanViewProject(currentUser, selected, projectService.getProjectTeamMembers(projId));
                    request.setAttribute("selectedProject", selected);
                    request.setAttribute("projectTeam", projectService.getProjectTeamMembers(projId));
                }
            }

            // 3. Edit Project (prepare form data)
            if ("edit".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long projId = ValidationUtils.parseLongSafe(idParam, null);
                if (projId != null) {
                    Project editProj = projectService.getProjectById(projId);
                    SecurityUtils.requireCanUpdateProject(currentUser, editProj);
                    request.setAttribute("editProject", editProj);
                }
            }

            // 4. Add Member via GET (convenience)
            if ("addMember".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long projId = ValidationUtils.parseLongSafe(idParam, null);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (projId != null && userId != null) {
                    Project proj = projectService.getProjectById(projId);
                    SecurityUtils.requireCanManageProjectTeam(currentUser, proj);
                    projectService.addTeamMemberToProject(projId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Team member added successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 5. Remove Member via GET (convenience)
            if ("removeMember".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long projId = ValidationUtils.parseLongSafe(idParam, null);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (projId != null && userId != null) {
                    Project proj = projectService.getProjectById(projId);
                    SecurityUtils.requireCanManageProjectTeam(currentUser, proj);
                    projectService.removeTeamMemberFromProject(projId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Team member removed successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 6. List Projects (with combined filtering, searching, and sorting)
            String statusParam = request.getParameter("status");
            String priorityParam = request.getParameter("priority");
            String searchParam = request.getParameter("search");
            String sortParam = request.getParameter("sort");

            ProjectStatus status = null;
            if (ValidationUtils.isNotBlank(statusParam) && !"ALL".equalsIgnoreCase(statusParam)) {
                try {
                    status = ProjectStatus.valueOf(statusParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            Priority priority = null;
            if (ValidationUtils.isNotBlank(priorityParam) && !"ALL".equalsIgnoreCase(priorityParam)) {
                try {
                    priority = Priority.valueOf(priorityParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            List<Project> projects = projectService.filterProjects(searchParam, status, priority, sortParam);

            // RBAC Filtering: TEAM_MEMBER only views assigned projects
            if (currentUser != null && currentUser.getRole() == Role.TEAM_MEMBER) {
                projects = projects.stream()
                        .filter(p -> (p.getManagerId() != null && p.getManagerId().equals(currentUser.getId())) ||
                                projectService.getProjectTeamMembers(p.getId()).stream().anyMatch(tm -> tm.getId().equals(currentUser.getId())))
                        .collect(Collectors.toList());
            }

            request.setAttribute("projects", projects);
            request.setAttribute("users", userService.getAllUsers());
            request.setAttribute("selectedStatus", statusParam != null ? statusParam : "ALL");
            request.setAttribute("selectedPriority", priorityParam != null ? priorityParam : "ALL");
            request.setAttribute("selectedSort", sortParam != null ? sortParam : "");
            request.setAttribute("searchQuery", searchParam != null ? searchParam : "");
            request.setAttribute("projectStatuses", ProjectStatus.values());
            request.setAttribute("priorities", Priority.values());
            request.setAttribute("activeNav", "projects");
            request.setAttribute("pageTitle", "Project Portfolio");

            request.getRequestDispatcher("/WEB-INF/views/projects.jsp").forward(request, response);

        } catch (UnauthorizedAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (ResourceNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode("Project not found.", StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[ProjectServlet GET Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode("Unable to complete request. Please try again.", StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getAuthenticatedUser(request);
        String action = request.getParameter("action");

        try {
            // 1. Delete Project
            if ("delete".equalsIgnoreCase(action)) {
                Long projId = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (projId != null) {
                    Project target = projectService.getProjectById(projId);
                    SecurityUtils.requireCanDeleteProject(currentUser, target);
                    projectService.deleteProject(projId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Project deleted successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 2. Update Project
            if ("update".equalsIgnoreCase(action)) {
                Long projId = ValidationUtils.parseLongSafe(request.getParameter("id"), null);
                if (projId == null) {
                    throw new ValidationException("Project ID is required for update.");
                }

                Project proj = projectService.getProjectById(projId);
                SecurityUtils.requireCanUpdateProject(currentUser, proj);

                String updatedName = request.getParameter("name");
                String updatedCode = request.getParameter("code");
                String updatedDesc = request.getParameter("description");
                String updatedStatus = request.getParameter("status");
                String updatedPriority = request.getParameter("priority");
                String updatedBudget = request.getParameter("budget");
                String updatedEndDate = request.getParameter("endDate");

                if (ValidationUtils.isNotBlank(updatedName)) {
                    proj.setName(ValidationUtils.sanitize(updatedName));
                }
                if (ValidationUtils.isNotBlank(updatedCode)) {
                    proj.setCode(ValidationUtils.sanitize(updatedCode));
                }
                if (updatedDesc != null) {
                    proj.setDescription(ValidationUtils.sanitize(updatedDesc));
                }
                if (ValidationUtils.isNotBlank(updatedStatus)) {
                    proj.setStatus(ProjectStatus.valueOf(updatedStatus.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(updatedPriority)) {
                    proj.setPriority(Priority.valueOf(updatedPriority.toUpperCase()));
                }
                if (ValidationUtils.isNotBlank(updatedBudget)) {
                    proj.setBudget(ValidationUtils.parseDoubleSafe(updatedBudget, proj.getBudget()));
                }
                if (ValidationUtils.isNotBlank(updatedEndDate)) {
                    LocalDate parsedDate = ValidationUtils.parseLocalDateSafe(updatedEndDate, null);
                    if (parsedDate != null) {
                        proj.setEndDate(parsedDate);
                    }
                }

                projectService.updateProject(proj);
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Project updated successfully.", StandardCharsets.UTF_8));
                return;
            }

            // 3. Add Team Member
            if ("addMember".equalsIgnoreCase(action)) {
                Long projId = ValidationUtils.parseLongSafe(request.getParameter("projectId"), null);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (projId != null && userId != null) {
                    Project proj = projectService.getProjectById(projId);
                    SecurityUtils.requireCanManageProjectTeam(currentUser, proj);
                    projectService.addTeamMemberToProject(projId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Team member added to project.", StandardCharsets.UTF_8));
                return;
            }

            // 4. Remove Team Member
            if ("removeMember".equalsIgnoreCase(action)) {
                Long projId = ValidationUtils.parseLongSafe(request.getParameter("projectId"), null);
                Long userId = ValidationUtils.parseLongSafe(request.getParameter("userId"), null);
                if (projId != null && userId != null) {
                    Project proj = projectService.getProjectById(projId);
                    SecurityUtils.requireCanManageProjectTeam(currentUser, proj);
                    projectService.removeTeamMemberFromProject(projId, userId);
                }
                response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                        URLEncoder.encode("Team member removed from project.", StandardCharsets.UTF_8));
                return;
            }

            // 5. Create Project (default POST action)
            SecurityUtils.requireCanCreateProject(currentUser);

            String name = request.getParameter("name");
            String code = request.getParameter("code");
            String description = request.getParameter("description");
            String statusStr = request.getParameter("status");
            String priorityStr = request.getParameter("priority");
            String budgetStr = request.getParameter("budget");
            String endDateStr = request.getParameter("endDate");

            if (ValidationUtils.isBlank(name)) {
                throw new ValidationException("Project name is required.");
            }
            if (!ValidationUtils.isValidLength(name, 3, 150)) {
                throw new ValidationException("Project name must be between 3 and 150 characters.");
            }

            Project project = new Project();
            project.setName(ValidationUtils.sanitize(name));
            project.setCode(ValidationUtils.isNotBlank(code) 
                    ? ValidationUtils.sanitize(code) 
                    : "PRJ-" + (System.currentTimeMillis() % 10000));
            project.setDescription(ValidationUtils.sanitize(description));

            if (ValidationUtils.isNotBlank(statusStr)) {
                project.setStatus(ProjectStatus.valueOf(statusStr.toUpperCase()));
            } else {
                project.setStatus(ProjectStatus.PLANNING);
            }

            if (ValidationUtils.isNotBlank(priorityStr)) {
                project.setPriority(Priority.valueOf(priorityStr.toUpperCase()));
            } else {
                project.setPriority(Priority.MEDIUM);
            }

            project.setBudget(ValidationUtils.parseDoubleSafe(budgetStr, 0.0));
            project.setSpentAmount(0.0);
            project.setProgressPercentage(0);
            project.setTotalTasks(0);
            project.setCompletedTasks(0);
            project.setStartDate(LocalDate.now());

            if (ValidationUtils.isNotBlank(endDateStr)) {
                LocalDate parsedEnd = ValidationUtils.parseLocalDateSafe(endDateStr, null);
                project.setEndDate(parsedEnd != null ? parsedEnd : LocalDate.now().plusMonths(2));
            } else {
                project.setEndDate(LocalDate.now().plusMonths(2));
            }

            if (!ValidationUtils.isValidDateRange(project.getStartDate(), project.getEndDate())) {
                throw new ValidationException("Project end date cannot be earlier than start date.");
            }

            project.setManager(currentUser);
            project.addTeamMember(currentUser);

            projectService.createProject(project);

            response.sendRedirect(request.getContextPath() + "/projects?success=" + 
                    URLEncoder.encode("Project created successfully.", StandardCharsets.UTF_8));

        } catch (UnauthorizedAccessException e) {
            System.err.printf("[Security Warning] Unauthorized project action: %s%n", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (ValidationException | ProjectNotFoundException e) {
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[ProjectServlet POST Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/projects?error=" + 
                    URLEncoder.encode("An unexpected error occurred while saving the project.", StandardCharsets.UTF_8));
        }
    }
}
