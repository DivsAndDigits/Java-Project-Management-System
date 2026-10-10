package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Notification;
import model.User;
import service.NotificationService;
import service.ServiceFactory;
import service.UserService;
import util.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller handling Notifications center and status updates:
 * - List notifications (GET)
 * - Mark single notification read (GET/POST action=markRead)
 * - Mark all notifications read (GET/POST action=markAllRead)
 * - Trigger deadline reminder check (GET/POST action=checkDeadlines)
 *
 * Enforces architecture: Servlet -> Service -> DAO -> JDBC -> MySQL
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {"/notifications"})
public class NotificationServlet extends HttpServlet {

    private NotificationService notificationService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.notificationService = ServiceFactory.getInstance().getNotificationService();
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public NotificationServlet() {}

    public NotificationServlet(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null && session.getAttribute("currentUser") != null)
                ? (User) session.getAttribute("currentUser")
                : userService.getCurrentUser();

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=" +
                    URLEncoder.encode("Please sign in to view notifications.", StandardCharsets.UTF_8));
            return;
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        try {
            if ("markRead".equalsIgnoreCase(action) && ValidationUtils.isNotBlank(idParam)) {
                Long notificationId = ValidationUtils.parseLongSafe(idParam, null);
                if (notificationId != null) {
                    // Enforce notification recipient authorization (prevent IDOR)
                    List<Notification> userNotifs = notificationService.getUserNotifications(currentUser.getId());
                    boolean isRecipient = userNotifs.stream().anyMatch(n -> n.getId() != null && n.getId().equals(notificationId));
                    if (isRecipient || util.SecurityUtils.isAdmin(currentUser)) {
                        notificationService.markNotificationAsRead(notificationId);
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.sendRedirect(request.getContextPath() + "/notifications?error=" +
                                URLEncoder.encode("You are not authorized to modify this notification.", StandardCharsets.UTF_8));
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/notifications");
                return;
            } else if ("markAllRead".equalsIgnoreCase(action)) {
                notificationService.markAllNotificationsAsRead(currentUser.getId());
                response.sendRedirect(request.getContextPath() + "/notifications");
                return;
            } else if ("checkDeadlines".equalsIgnoreCase(action)) {
                // Restricted to Project Managers and Administrators
                if (!util.SecurityUtils.isAdmin(currentUser) && !util.SecurityUtils.isProjectManager(currentUser)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.sendRedirect(request.getContextPath() + "/notifications?error=" +
                            URLEncoder.encode("Triggering manual deadline sweeps requires Manager or Administrator role.", StandardCharsets.UTF_8));
                    return;
                }
                // Triggers immediate background sweep for approaching and overdue task deadlines
                service.concurrent.AppSchedulerManager.getInstance().triggerImmediateCheck();
                response.sendRedirect(request.getContextPath() + "/notifications?success=" + 
                        URLEncoder.encode("Deadline sweep completed successfully.", StandardCharsets.UTF_8));
                return;
            }

            List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());

            request.setAttribute("notifications", notifications);
            request.setAttribute("unreadCount", notificationService.getUnreadNotificationCount(currentUser.getId()));
            request.setAttribute("activeNav", "notifications");
            request.setAttribute("pageTitle", "Notifications");

            request.getRequestDispatcher("/WEB-INF/views/notifications.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.printf("[NotificationServlet Error] %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/dashboard?error=" + 
                    URLEncoder.encode("Failed to process notifications request.", StandardCharsets.UTF_8));
        }
    }
}
