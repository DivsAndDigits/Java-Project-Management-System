package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.DashboardStats;
import service.DashboardService;
import service.ProjectService;
import service.ServiceFactory;
import service.UserService;
import java.io.IOException;

/**
 * Controller handling the main executive Dashboard view.
 * Enforces architecture: Servlet -> Service -> DAO -> JDBC -> MySQL
 *
 * Responsibilities:
 * - Retrieve project/task statistics via DashboardService
 * - Send aggregated data and entity lists to dashboard JSP
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", ""})
public class DashboardServlet extends HttpServlet {

    private DashboardService dashboardService;
    private ProjectService projectService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.dashboardService = ServiceFactory.getInstance().getDashboardService();
        this.projectService = ServiceFactory.getInstance().getProjectService();
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public DashboardServlet() {}

    public DashboardServlet(DashboardService dashboardService, ProjectService projectService, UserService userService) {
        this.dashboardService = dashboardService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Retrieve project/task KPI statistics
        DashboardStats stats = dashboardService.getDashboardStatistics();

        // Pass statistics and entity collections to JSP via request attributes
        request.setAttribute("stats", stats);
        request.setAttribute("projects", projectService.getAllProjects());
        request.setAttribute("users", userService.getAllUsers());
        request.setAttribute("activeNav", "dashboard");
        request.setAttribute("pageTitle", "Executive Dashboard");

        request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
    }
}
