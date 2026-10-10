package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.TeamMemberSummary;
import service.ServiceFactory;
import service.UserService;
import java.io.IOException;
import java.util.List;

/**
 * Controller handling Team directory and workload analysis view.
 * Servlet -> Service -> DAO -> JDBC -> MySQL
 */
@WebServlet(name = "TeamServlet", urlPatterns = {"/team"})
public class TeamServlet extends HttpServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.userService = ServiceFactory.getInstance().getUserService();
    }

    public TeamServlet() {}

    public TeamServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<TeamMemberSummary> teamSummaries = userService.getTeamMemberSummaries();

        request.setAttribute("teamSummaries", teamSummaries);
        request.setAttribute("activeNav", "team");
        request.setAttribute("pageTitle", "Team Directory & Workload");

        request.getRequestDispatcher("/WEB-INF/views/team.jsp").forward(request, response);
    }
}
