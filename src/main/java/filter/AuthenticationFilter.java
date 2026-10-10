package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.ServiceFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter enforcing session-based authentication across all protected application endpoints.
 * Unauthenticated users attempting to access secured routes are intercepted and redirected
 * to the login page.
 *
 * Architecture:
 * - Public routes: /login, /register, /logout, static assets (/css/*, /js/*, /images/*)
 * - Protected routes: /dashboard, /projects, /tasks, /team, /reports, /notifications, /profile, /users, etc.
 */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/*"})
public class AuthenticationFilter implements Filter {

    public static final String SESSION_USER_KEY = "currentUser";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = uri.substring(contextPath.length());

        // 1. Allow static resources
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Allow public authentication endpoints
        if (isPublicAuthPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Inspect active session for authenticated user
        HttpSession session = httpRequest.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute(SESSION_USER_KEY);
            if (currentUser == null) {
                currentUser = (User) session.getAttribute("user");
            }
        }

        if (currentUser == null) {
            // Unauthenticated user -> redirect to login page with notice
            String loginUrl = contextPath + "/login?error=" + 
                    URLEncoder.encode("Please sign in to access this page.", StandardCharsets.UTF_8);
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        // 4. Authenticated user -> attach user & unread notification count to request scope
        try {
            int unreadCount = ServiceFactory.getInstance()
                    .getNotificationService()
                    .getUnreadNotificationCount(currentUser.getId());
            httpRequest.setAttribute("unreadNotificationCount", unreadCount);
        } catch (Exception e) {
            httpRequest.setAttribute("unreadNotificationCount", 0);
        }
        httpRequest.setAttribute("currentUser", currentUser);

        chain.doFilter(request, response);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".svg")
                || path.endsWith(".ico")
                || path.equals("/favicon.ico");
    }

    private boolean isPublicAuthPath(String path) {
        return path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.startsWith("/login?")
                || path.startsWith("/register?")
                || path.startsWith("/logout?");
    }

    @Override
    public void destroy() {}
}
