package controller;

import exception.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Admin;
import model.ProjectManager;
import model.Role;
import model.TeamMember;
import model.User;
import service.ServiceFactory;
import service.UserService;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller handling user registration.
 * Enforces architecture: Servlet -> Service -> DAO -> JDBC -> MySQL
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public RegisterServlet() {}

    public RegisterServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.setAttribute("pageTitle", "Register - ProjectFlow");
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String roleStr = request.getParameter("role");
        String department = request.getParameter("department");

        try {
            if (ValidationUtils.isBlank(username) || !ValidationUtils.isValidLength(username, 3, 50)) {
                throw new ValidationException("Username must be between 3 and 50 characters.");
            }
            if (ValidationUtils.isBlank(fullName) || !ValidationUtils.isValidLength(fullName, 2, 100)) {
                throw new ValidationException("Full name is required (2 to 100 characters).");
            }
            if (!ValidationUtils.isValidEmail(email)) {
                throw new ValidationException("A valid email address is required.");
            }
            if (ValidationUtils.isBlank(password) || !ValidationUtils.isValidLength(password, 6, 100)) {
                throw new ValidationException("Password must be at least 6 characters long.");
            }

            // Role assignment: public self-registration always assigns TEAM_MEMBER to prevent privilege escalation.
            // Only authenticated Administrators can assign elevated roles (ADMIN / PROJECT_MANAGER).
            HttpSession existingSession = request.getSession(false);
            User sessionUser = (existingSession != null) ? (User) existingSession.getAttribute("currentUser") : null;
            boolean isAdminCreating = sessionUser != null && sessionUser.getRole() == Role.ADMIN;

            Role role = Role.TEAM_MEMBER;
            if (isAdminCreating && ValidationUtils.isNotBlank(roleStr)) {
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            User newUser;
            switch (role) {
                case ADMIN:
                    newUser = new Admin();
                    break;
                case PROJECT_MANAGER:
                    newUser = new ProjectManager();
                    break;
                case TEAM_MEMBER:
                default:
                    newUser = new TeamMember();
                    break;
            }

            newUser.setUsername(ValidationUtils.sanitize(username));
            newUser.setFullName(ValidationUtils.sanitize(fullName));
            newUser.setEmail(ValidationUtils.sanitize(email));
            newUser.setRole(role);
            newUser.setDepartment(ValidationUtils.isNotBlank(department) ? ValidationUtils.sanitize(department) : "Engineering");
            newUser.setActive(true);

            User registered = userService.registerUser(newUser, password);

            if (!isAdminCreating) {
                HttpSession session = request.getSession(true);
                session.setAttribute("currentUser", registered);
                session.setAttribute("user", registered);
                session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());
                response.sendRedirect(request.getContextPath() + "/dashboard?welcome=" + 
                        URLEncoder.encode(registered.getFullName(), StandardCharsets.UTF_8));
            } else {
                response.sendRedirect(request.getContextPath() + "/team?success=" + 
                        URLEncoder.encode("User account created successfully.", StandardCharsets.UTF_8));
            }
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/register?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[Register Error] Registration failure: %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/register?error=" + 
                    URLEncoder.encode("Could not register user. Username or email may already be in use.", StandardCharsets.UTF_8));
        }
    }
}
