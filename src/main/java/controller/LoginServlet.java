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
import service.ServiceFactory;
import service.UserService;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller handling user authentication (Login).
 * Enforces architecture: Servlet -> Service -> DAO -> JDBC -> MySQL
 *
 * Responsibilities:
 * - Accept login form
 * - Validate credentials using UserService
 * - Use JDBC through DAO layer
 * - Create HttpSession after successful login
 * - Redirect to dashboard
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public LoginServlet() {}

    public LoginServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // If user already holds an active session, forward directly to dashboard
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.setAttribute("pageTitle", "Sign In - ProjectFlow");
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String identity = request.getParameter("usernameOrEmail");
        String password = request.getParameter("password");

        try {
            if (ValidationUtils.isBlank(identity) || ValidationUtils.isBlank(password)) {
                throw new ValidationException("Username/email and password are required.");
            }

            // Authenticate through UserService -> UserDao -> JDBC
            User user = userService.loginUser(identity.trim(), password);

            // Mitigate Session Fixation: Invalidate pre-authentication session and issue a fresh session
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", java.util.UUID.randomUUID().toString());

            // Redirect to dashboard with personalized greeting
            response.sendRedirect(request.getContextPath() + "/dashboard?welcome=" + 
                    URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8));

        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/login?error=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.printf("[Login Error] Authentication failure: %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login?error=" + 
                    URLEncoder.encode("Invalid credentials or authentication service unavailable.", StandardCharsets.UTF_8));
        }
    }
}
