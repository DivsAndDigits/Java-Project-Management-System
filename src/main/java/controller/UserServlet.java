package controller;

import exception.UnauthorizedAccessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.ServiceFactory;
import service.UserService;
import util.SecurityUtils;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller handling User management operations:
 * - find user by ID
 * - update user
 * - delete user
 * - list all users
 *
 * RBAC: Strictly restricted to ADMINISTRATOR role.
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/users"})
public class UserServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public UserServlet() {}

    public UserServlet(UserService userService) {
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
        try {
            SecurityUtils.requireCanManageUsers(currentUser);
        } catch (UnauthorizedAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("errorCode", 403);
            request.setAttribute("errorMessage", "Access Denied: User administration requires Administrator privileges.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            return;
        }

        String idParam = request.getParameter("id");
        String actionParam = request.getParameter("action");

        if ("delete".equalsIgnoreCase(actionParam) && ValidationUtils.isNotBlank(idParam)) {
            Long userId = ValidationUtils.parseLongSafe(idParam, null);
            if (userId != null) {
                userService.deleteUser(userId);
            }
            response.sendRedirect(request.getContextPath() + "/team?success=User+deleted+successfully");
            return;
        }

        if (ValidationUtils.isNotBlank(idParam)) {
            Long userId = ValidationUtils.parseLongSafe(idParam, null);
            if (userId != null) {
                User user = userService.getUserById(userId);
                request.setAttribute("selectedUser", user);
            }
        }

        List<User> users = userService.getAllUsers();
        request.setAttribute("users", users);
        request.setAttribute("activeNav", "team");
        request.setAttribute("pageTitle", "User Directory");

        request.getRequestDispatcher("/WEB-INF/views/team.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getAuthenticatedUser(request);
        try {
            SecurityUtils.requireCanManageUsers(currentUser);
        } catch (UnauthorizedAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("errorCode", 403);
            request.setAttribute("errorMessage", "Access Denied: User administration requires Administrator privileges.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            return;
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        try {
            if ("delete".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long userId = ValidationUtils.parseLongSafe(idParam, null);
                if (userId != null) {
                    userService.deleteUser(userId);
                }
                response.sendRedirect(request.getContextPath() + "/team?success=User+deleted+successfully");
                return;
            } else if ("update".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long userId = ValidationUtils.parseLongSafe(idParam, null);
                if (userId != null) {
                    User user = userService.getUserById(userId);
                    String fullName = request.getParameter("fullName");
                    String department = request.getParameter("department");
                    String email = request.getParameter("email");
                    if (ValidationUtils.isNotBlank(fullName)) user.setFullName(ValidationUtils.sanitize(fullName));
                    if (ValidationUtils.isNotBlank(department)) user.setDepartment(ValidationUtils.sanitize(department));
                    if (ValidationUtils.isNotBlank(email)) user.setEmail(email.trim());
                    userService.updateUser(user);
                }
                response.sendRedirect(request.getContextPath() + "/team?success=User+updated+successfully");
                return;
            }
        } catch (Exception e) {
            System.err.printf("[UserServlet Error] Action %s failed: %s%n", action, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/team?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            return;
        }

        response.sendRedirect(request.getContextPath() + "/team");
    }
}
