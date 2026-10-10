package controller;

import exception.InvalidUserException;
import exception.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.ProjectService;
import service.ServiceFactory;
import service.TaskService;
import service.UserService;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller handling User Profile details and profile preferences:
 * - View profile (GET)
 * - Update profile (POST)
 *
 * Enforces architecture: Servlet -> Service -> DAO -> JDBC -> MySQL
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    private UserService userService;
    private ProjectService projectService;
    private TaskService taskService;

    @Override
    public void init() throws ServletException {
        this.userService = ServiceFactory.getInstance().getUserService();
        this.projectService = ServiceFactory.getInstance().getProjectService();
        this.taskService = ServiceFactory.getInstance().getTaskService();
    }

    public ProfileServlet() {}

    public ProfileServlet(UserService userService, ProjectService projectService, TaskService taskService) {
        this.userService = userService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null && session.getAttribute("currentUser") != null)
                ? (User) session.getAttribute("currentUser")
                : userService.getCurrentUser();

        request.setAttribute("user", currentUser);
        request.setAttribute("managedProjects", projectService.getAllProjects());
        request.setAttribute("assignedTasks", taskService.getTasksByAssigneeId(currentUser.getId()));
        request.setAttribute("activeNav", "profile");
        request.setAttribute("pageTitle", "User Profile");

        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null && session.getAttribute("currentUser") != null)
                ? (User) session.getAttribute("currentUser")
                : userService.getCurrentUser();

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String department = request.getParameter("department");

        try {
            if (ValidationUtils.isNotBlank(fullName)) {
                currentUser.setFullName(ValidationUtils.sanitize(fullName));
            }
            if (ValidationUtils.isNotBlank(email)) {
                currentUser.setEmail(email.trim());
            }
            if (ValidationUtils.isNotBlank(department)) {
                currentUser.setDepartment(ValidationUtils.sanitize(department));
            }

            userService.updateUser(currentUser);
            if (session != null) {
                session.setAttribute("currentUser", currentUser);
                session.setAttribute("user", currentUser);
            }

            response.sendRedirect(request.getContextPath() + "/profile?success=" + 
                    URLEncoder.encode("Profile updated successfully.", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/profile?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[Application Error] Profile update failed: %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/profile?error=" + 
                    URLEncoder.encode("An unexpected error occurred while updating profile.", StandardCharsets.UTF_8));
        }
    }
}
