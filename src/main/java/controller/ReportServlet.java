package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.DashboardStats;
import model.ReportResult;
import model.Task;
import service.DashboardService;
import service.ReportService;
import service.ServiceFactory;
import service.TaskService;

import java.io.IOException;
import java.util.List;

/**
 * [OOP Concept: Polymorphism]
 * [OOP Concept: Exception Handling]
 * Controller handling executive Reporting, metrics, and collection-based analytical reports:
 * - Project completion percentage
 * - Completed versus pending tasks
 * - Tasks grouped by priority
 * - Tasks grouped by status
 * - Team workload
 * - Overdue tasks
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/reports"})
public class ReportServlet extends HttpServlet {

    private DashboardService dashboardService;
    private ReportService reportService;
    private TaskService taskService;

    @Override
    public void init() throws ServletException {
        this.dashboardService = ServiceFactory.getInstance().getDashboardService();
        this.reportService = ServiceFactory.getInstance().getReportService();
        this.taskService = ServiceFactory.getInstance().getTaskService();
    }

    public ReportServlet() {}

    public ReportServlet(DashboardService dashboardService, ReportService reportService, TaskService taskService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
        this.taskService = taskService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            DashboardStats stats = dashboardService.getDashboardStatistics();

            // Polymorphic Report Generation
            List<ReportResult> generatedReports = reportService.generateConsolidatedReports();

            // Real JDBC-driven overdue task details for reports page
            List<Task> overdueTaskList = taskService.getOverdueTasks();

            request.setAttribute("stats", stats);
            request.setAttribute("reports", generatedReports);
            request.setAttribute("overdueTaskList", overdueTaskList);
            request.setAttribute("activeNav", "reports");
            request.setAttribute("pageTitle", "Project Reports & Analytics");

            request.getRequestDispatcher("/WEB-INF/views/reports.jsp").forward(request, response);
        } catch (Exception e) {
            System.err.printf("[Application Error] ReportServlet exception: %s%n", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Unable+to+compile+reports.+Please+retry.");
        }
    }
}
