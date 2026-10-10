package controller;

import exception.UnauthorizedAccessException;
import filter.AuthenticationFilter;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ServiceFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration and unit tests for the Servlet web layer.
 * Verifies Servlets, Filters, session management, and HTTP method contracts:
 * - LoginServlet (/login)
 * - LogoutServlet (/logout)
 * - DashboardServlet (/dashboard, /)
 * - ProjectServlet (/projects)
 * - TaskServlet (/tasks)
 * - TeamServlet (/team)
 * - NotificationServlet (/notifications)
 * - ProfileServlet (/profile)
 * - AuthenticationFilter (/*)
 */
class ServletWebLayerIntegrationTest {

    private ServiceFactory factory;
    private LoginServlet loginServlet;
    private LogoutServlet logoutServlet;
    private DashboardServlet dashboardServlet;
    private ProjectServlet projectServlet;
    private TaskServlet taskServlet;
    private TeamServlet teamServlet;
    private NotificationServlet notificationServlet;
    private ProfileServlet profileServlet;
    private AuthenticationFilter authFilter;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory.reset();
        factory = ServiceFactory.getInstance();

        loginServlet = new LoginServlet(factory.getUserService());
        logoutServlet = new LogoutServlet();
        dashboardServlet = new DashboardServlet(
                factory.getDashboardService(),
                factory.getProjectService(),
                factory.getUserService()
        );
        projectServlet = new ProjectServlet(factory.getProjectService(), factory.getUserService());
        taskServlet = new TaskServlet(
                factory.getTaskService(),
                factory.getProjectService(),
                factory.getUserService()
        );
        teamServlet = new TeamServlet(factory.getUserService());
        notificationServlet = new NotificationServlet(factory.getNotificationService(), factory.getUserService());
        profileServlet = new ProfileServlet(
                factory.getUserService(),
                factory.getProjectService(),
                factory.getTaskService()
        );
        authFilter = new AuthenticationFilter();
    }

    @Test
    @DisplayName("LoginServlet: Success POST validates credentials, creates HttpSession, and redirects to dashboard")
    void testLoginServlet_Success_CreatesHttpSession_RedirectsDashboard() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/login");
        req.setParameter("usernameOrEmail", "alex.morgan");
        req.setParameter("password", "password123");

        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        loginServlet.doPost(req.asProxy(), resp.asProxy());

        assertNotNull(req.session, "HttpSession must be created on successful login");
        User userInSession = (User) req.session.getAttribute("currentUser");
        assertNotNull(userInSession, "currentUser must be stored in HttpSession");
        assertEquals("alex.morgan", userInSession.getUsername());
        assertNotNull(resp.redirectedUrl, "Response must redirect");
        assertTrue(resp.redirectedUrl.contains("/dashboard"), "Must redirect to dashboard");
    }

    @Test
    @DisplayName("LoginServlet: Failure POST with wrong password redirects to /login?error=")
    void testLoginServlet_InvalidCredentials_RedirectsLoginWithError() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/login");
        req.setParameter("usernameOrEmail", "alex.morgan");
        req.setParameter("password", "wrong_password");

        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        loginServlet.doPost(req.asProxy(), resp.asProxy());

        assertNull(req.session, "HttpSession must NOT be created on failed login");
        assertNotNull(resp.redirectedUrl);
        assertTrue(resp.redirectedUrl.contains("/login?error="), "Must redirect to login with error parameter");
    }

    @Test
    @DisplayName("LoginServlet: GET forwards to login.jsp when unauthenticated, or redirects to dashboard when logged in")
    void testLoginServlet_GetRouting() throws Exception {
        // 1. Unauthenticated GET -> forward to /WEB-INF/views/login.jsp
        MockServletSupport.TestRequest unauthReq = new MockServletSupport.TestRequest("/login");
        MockServletSupport.TestResponse unauthResp = new MockServletSupport.TestResponse();
        loginServlet.doGet(unauthReq.asProxy(), unauthResp.asProxy());
        assertEquals("/WEB-INF/views/login.jsp", unauthReq.forwardPath);

        // 2. Authenticated GET -> redirect to /dashboard
        MockServletSupport.TestRequest authReq = new MockServletSupport.TestRequest("/login");
        authReq.session = new MockServletSupport.TestSession();
        authReq.session.setAttribute("currentUser", factory.getUserService().getCurrentUser());
        MockServletSupport.TestResponse authResp = new MockServletSupport.TestResponse();
        loginServlet.doGet(authReq.asProxy(), authResp.asProxy());
        assertTrue(authResp.redirectedUrl.contains("/dashboard"));
    }

    @Test
    @DisplayName("LogoutServlet: Invalidates session and redirects to /login?loggedOut=true")
    void testLogoutServlet_InvalidatesSession_RedirectsLoginPage() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/logout");
        req.session = new MockServletSupport.TestSession();
        req.session.setAttribute("currentUser", factory.getUserService().getCurrentUser());

        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        logoutServlet.doGet(req.asProxy(), resp.asProxy());

        assertTrue(req.session.isInvalidated(), "HttpSession must be invalidated");
        assertNotNull(resp.redirectedUrl);
        assertTrue(resp.redirectedUrl.contains("/login?loggedOut=true"), "Must redirect to login page with loggedOut flag");
    }

    @Test
    @DisplayName("AuthenticationFilter: Unauthenticated access to protected pages is intercepted and redirected to /login")
    void testAuthenticationFilter_UnauthenticatedProtected_RedirectsLogin() throws Exception {
        String[] protectedPaths = {"/dashboard", "/projects", "/tasks", "/team", "/reports", "/notifications", "/profile"};

        for (String path : protectedPaths) {
            MockServletSupport.TestRequest req = new MockServletSupport.TestRequest(path);
            MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();
            MockServletSupport.TestFilterChain chain = new MockServletSupport.TestFilterChain();

            authFilter.doFilter(req.asProxy(), resp.asProxy(), chain);

            assertFalse(chain.invoked, "Filter chain must NOT proceed for unauthenticated route: " + path);
            assertNotNull(resp.redirectedUrl, "Must redirect unauthenticated request: " + path);
            assertTrue(resp.redirectedUrl.contains("/login?error="), "Must redirect to /login?error=");
        }
    }

    @Test
    @DisplayName("AuthenticationFilter: Whitelisted public routes bypass session check")
    void testAuthenticationFilter_PublicRoutes_BypassAuth() throws Exception {
        String[] publicPaths = {"/login", "/register", "/logout", "/css/main.css", "/js/main.js", "/images/logo.png"};

        for (String path : publicPaths) {
            MockServletSupport.TestRequest req = new MockServletSupport.TestRequest(path);
            MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();
            MockServletSupport.TestFilterChain chain = new MockServletSupport.TestFilterChain();

            authFilter.doFilter(req.asProxy(), resp.asProxy(), chain);

            assertTrue(chain.invoked, "Filter chain MUST proceed for public path: " + path);
            assertNull(resp.redirectedUrl, "Public path should not redirect: " + path);
        }
    }

    @Test
    @DisplayName("AuthenticationFilter: Authenticated request with session user passes through and sets unread badge")
    void testAuthenticationFilter_AuthenticatedUser_PassesThrough() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/dashboard");
        req.session = new MockServletSupport.TestSession();
        User user = factory.getUserService().getCurrentUser();
        req.session.setAttribute("currentUser", user);

        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();
        MockServletSupport.TestFilterChain chain = new MockServletSupport.TestFilterChain();

        authFilter.doFilter(req.asProxy(), resp.asProxy(), chain);

        assertTrue(chain.invoked, "Filter chain must proceed for authenticated user");
        assertNotNull(req.attributes.get("unreadNotificationCount"), "Unread notification count must be bound to request");
        assertEquals(user, req.attributes.get("currentUser"));
    }

    @Test
    @DisplayName("DashboardServlet: Retrieves statistics and sets attributes for dashboard.jsp")
    void testDashboardServlet_RetrievesStatsAndForwards() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/dashboard");
        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        dashboardServlet.doGet(req.asProxy(), resp.asProxy());

        assertNotNull(req.attributes.get("stats"), "stats attribute must be set");
        assertTrue(req.attributes.get("stats") instanceof DashboardStats);
        assertNotNull(req.attributes.get("projects"), "projects collection must be set");
        assertNotNull(req.attributes.get("users"), "users collection must be set");
        assertEquals("dashboard", req.attributes.get("activeNav"));
        assertEquals("/WEB-INF/views/dashboard.jsp", req.forwardPath);
    }

    @Test
    @DisplayName("ProjectServlet: Handles full lifecycle - Create, List, View, Update, Delete")
    void testProjectServlet_FullCrudLifecycle() throws Exception {
        User manager = factory.getUserService().getCurrentUser();

        // Clean up prior test run leftovers if present in database
        factory.getProjectService().getAllProjects().stream()
                .filter(p -> "PRJ-999".equals(p.getCode()))
                .forEach(p -> factory.getProjectService().deleteProject(p.getId()));

        // 1. Create project (POST)
        MockServletSupport.TestRequest createReq = new MockServletSupport.TestRequest("/projects");
        createReq.session = new MockServletSupport.TestSession();
        createReq.session.setAttribute("currentUser", manager);
        createReq.setParameter("name", "Distributed AI Inference Engine");
        createReq.setParameter("code", "PRJ-999");
        createReq.setParameter("description", "High performance distributed model server");
        createReq.setParameter("status", "IN_PROGRESS");
        createReq.setParameter("priority", "HIGH");
        createReq.setParameter("budget", "150000");
        createReq.setParameter("endDate", "2027-01-15");

        MockServletSupport.TestResponse createResp = new MockServletSupport.TestResponse();
        projectServlet.doPost(createReq.asProxy(), createResp.asProxy());

        assertTrue(createResp.redirectedUrl.contains("/projects?success="));

        // 2. List projects (GET)
        MockServletSupport.TestRequest listReq = new MockServletSupport.TestRequest("/projects");
        MockServletSupport.TestResponse listResp = new MockServletSupport.TestResponse();
        projectServlet.doGet(listReq.asProxy(), listResp.asProxy());

        @SuppressWarnings("unchecked")
        List<Project> projects = (List<Project>) listReq.attributes.get("projects");
        assertNotNull(projects);
        Project createdProj = projects.stream()
                .filter(p -> "Distributed AI Inference Engine".equals(p.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(createdProj, "Created project must appear in projects listing");
        assertEquals("PRJ-999", createdProj.getCode());

        // 3. View project (GET action=view)
        MockServletSupport.TestRequest viewReq = new MockServletSupport.TestRequest("/projects");
        viewReq.setParameter("action", "view");
        viewReq.setParameter("id", String.valueOf(createdProj.getId()));
        MockServletSupport.TestResponse viewResp = new MockServletSupport.TestResponse();
        projectServlet.doGet(viewReq.asProxy(), viewResp.asProxy());

        Project viewed = (Project) viewReq.attributes.get("selectedProject");
        assertNotNull(viewed);
        assertEquals(createdProj.getId(), viewed.getId());

        // 4. Update project (POST action=update)
        MockServletSupport.TestRequest updateReq = new MockServletSupport.TestRequest("/projects");
        updateReq.setParameter("action", "update");
        updateReq.setParameter("id", String.valueOf(createdProj.getId()));
        updateReq.setParameter("name", "Distributed AI Inference Engine v2");
        updateReq.setParameter("status", "COMPLETED");
        MockServletSupport.TestResponse updateResp = new MockServletSupport.TestResponse();
        projectServlet.doPost(updateReq.asProxy(), updateResp.asProxy());

        assertTrue(updateResp.redirectedUrl.contains("/projects?success="));
        Project updatedProj = factory.getProjectService().getProjectById(createdProj.getId());
        assertEquals("Distributed AI Inference Engine v2", updatedProj.getName());
        assertEquals(ProjectStatus.COMPLETED, updatedProj.getStatus());

        // 5. Delete project (POST action=delete)
        MockServletSupport.TestRequest deleteReq = new MockServletSupport.TestRequest("/projects");
        deleteReq.setParameter("action", "delete");
        deleteReq.setParameter("id", String.valueOf(createdProj.getId()));
        MockServletSupport.TestResponse deleteResp = new MockServletSupport.TestResponse();
        projectServlet.doPost(deleteReq.asProxy(), deleteResp.asProxy());

        assertTrue(deleteResp.redirectedUrl.contains("/projects?success="));
        assertThrows(Exception.class, () -> factory.getProjectService().getProjectById(createdProj.getId()));
    }

    @Test
    @DisplayName("TaskServlet: Handles full lifecycle - Create, List, Assign, Status Update, Delete")
    void testTaskServlet_FullLifecycle() throws Exception {
        User currentUser = factory.getUserService().getCurrentUser();
        Project project = factory.getProjectService().getAllProjects().get(0);

        // 1. Create task (POST)
        MockServletSupport.TestRequest createReq = new MockServletSupport.TestRequest("/tasks");
        createReq.session = new MockServletSupport.TestSession();
        createReq.session.setAttribute("currentUser", currentUser);
        createReq.setParameter("title", "Implement TLS 1.3 Mutual Handshake");
        createReq.setParameter("description", "Zero-trust service mesh mTLS connection configuration");
        createReq.setParameter("projectId", String.valueOf(project.getId()));
        createReq.setParameter("priority", "URGENT");
        createReq.setParameter("estimatedHours", "16");
        createReq.setParameter("dueDate", "2026-12-31");

        MockServletSupport.TestResponse createResp = new MockServletSupport.TestResponse();
        taskServlet.doPost(createReq.asProxy(), createResp.asProxy());

        assertTrue(createResp.redirectedUrl.contains("/tasks?success="));

        // 2. List tasks (GET)
        MockServletSupport.TestRequest listReq = new MockServletSupport.TestRequest("/tasks");
        MockServletSupport.TestResponse listResp = new MockServletSupport.TestResponse();
        taskServlet.doGet(listReq.asProxy(), listResp.asProxy());

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) listReq.attributes.get("tasks");
        assertNotNull(tasks);
        Task createdTask = tasks.stream()
                .filter(t -> "Implement TLS 1.3 Mutual Handshake".equals(t.getTitle()))
                .findFirst()
                .orElse(null);
        assertNotNull(createdTask, "Created task must be found in task listing");
        assertEquals(TaskStatus.TODO, createdTask.getStatus());

        // 3. Update task status (POST action=status)
        MockServletSupport.TestRequest statusReq = new MockServletSupport.TestRequest("/tasks");
        statusReq.setParameter("action", "status");
        statusReq.setParameter("id", String.valueOf(createdTask.getId()));
        statusReq.setParameter("newStatus", "IN_PROGRESS");
        MockServletSupport.TestResponse statusResp = new MockServletSupport.TestResponse();
        taskServlet.doPost(statusReq.asProxy(), statusResp.asProxy());

        assertTrue(statusResp.redirectedUrl.contains("/tasks?success="));
        Task inProg = factory.getTaskService().getTaskById(createdTask.getId());
        assertEquals(TaskStatus.IN_PROGRESS, inProg.getStatus());

        // 4. Assign task (POST action=assign)
        List<User> users = factory.getUserService().getAllUsers();
        User assignee = users.get(users.size() - 1);
        MockServletSupport.TestRequest assignReq = new MockServletSupport.TestRequest("/tasks");
        assignReq.setParameter("action", "assign");
        assignReq.setParameter("id", String.valueOf(createdTask.getId()));
        assignReq.setParameter("assigneeId", String.valueOf(assignee.getId()));
        MockServletSupport.TestResponse assignResp = new MockServletSupport.TestResponse();
        taskServlet.doPost(assignReq.asProxy(), assignResp.asProxy());

        assertTrue(assignResp.redirectedUrl.contains("/tasks?success="));
        Task assigned = factory.getTaskService().getTaskById(createdTask.getId());
        assertNotNull(assigned.getAssignee());
        assertEquals(assignee.getId(), assigned.getAssignee().getId());

        // 5. Delete task (POST action=delete)
        MockServletSupport.TestRequest deleteReq = new MockServletSupport.TestRequest("/tasks");
        deleteReq.setParameter("action", "delete");
        deleteReq.setParameter("id", String.valueOf(createdTask.getId()));
        MockServletSupport.TestResponse deleteResp = new MockServletSupport.TestResponse();
        taskServlet.doPost(deleteReq.asProxy(), deleteResp.asProxy());

        assertTrue(deleteResp.redirectedUrl.contains("/tasks?success="));
        assertThrows(Exception.class, () -> factory.getTaskService().getTaskById(createdTask.getId()));
    }

    @Test
    @DisplayName("TeamServlet: Sets team member summaries and forwards to team.jsp")
    void testTeamServlet() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/team");
        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        teamServlet.doGet(req.asProxy(), resp.asProxy());

        assertNotNull(req.attributes.get("teamSummaries"));
        assertEquals("team", req.attributes.get("activeNav"));
        assertEquals("/WEB-INF/views/team.jsp", req.forwardPath);
    }

    @Test
    @DisplayName("NotificationServlet: Handles markRead, markAllRead, checkDeadlines and lists notifications")
    void testNotificationServlet() throws Exception {
        User currentUser = factory.getUserService().getCurrentUser();

        // 1. Trigger deadline sweep
        MockServletSupport.TestRequest sweepReq = new MockServletSupport.TestRequest("/notifications");
        sweepReq.setParameter("action", "checkDeadlines");
        MockServletSupport.TestResponse sweepResp = new MockServletSupport.TestResponse();
        notificationServlet.doGet(sweepReq.asProxy(), sweepResp.asProxy());
        assertTrue(sweepResp.redirectedUrl.contains("/notifications?success="));

        // 2. Mark all read
        MockServletSupport.TestRequest markAllReq = new MockServletSupport.TestRequest("/notifications");
        markAllReq.setParameter("action", "markAllRead");
        MockServletSupport.TestResponse markAllResp = new MockServletSupport.TestResponse();
        notificationServlet.doGet(markAllReq.asProxy(), markAllResp.asProxy());
        assertTrue(markAllResp.redirectedUrl.contains("/notifications"));

        // 3. Listing notifications
        MockServletSupport.TestRequest listReq = new MockServletSupport.TestRequest("/notifications");
        MockServletSupport.TestResponse listResp = new MockServletSupport.TestResponse();
        notificationServlet.doGet(listReq.asProxy(), listResp.asProxy());
        assertNotNull(listReq.attributes.get("notifications"));
        assertEquals("/WEB-INF/views/notifications.jsp", listReq.forwardPath);
    }

    @Test
    @DisplayName("ProfileServlet: View and update user profile preferences")
    void testProfileServlet() throws Exception {
        User currentUser = factory.getUserService().getCurrentUser();

        // 1. GET profile view
        MockServletSupport.TestRequest getReq = new MockServletSupport.TestRequest("/profile");
        getReq.session = new MockServletSupport.TestSession();
        getReq.session.setAttribute("currentUser", currentUser);
        MockServletSupport.TestResponse getResp = new MockServletSupport.TestResponse();
        profileServlet.doGet(getReq.asProxy(), getResp.asProxy());

        assertEquals(currentUser, getReq.attributes.get("user"));
        assertEquals("/WEB-INF/views/profile.jsp", getReq.forwardPath);

        // 2. POST profile update
        MockServletSupport.TestRequest postReq = new MockServletSupport.TestRequest("/profile");
        postReq.session = new MockServletSupport.TestSession();
        postReq.session.setAttribute("currentUser", currentUser);
        postReq.setParameter("fullName", "Alex Updated Morgan");
        postReq.setParameter("department", "Principal Engineering");
        MockServletSupport.TestResponse postResp = new MockServletSupport.TestResponse();
        profileServlet.doPost(postReq.asProxy(), postResp.asProxy());

        assertTrue(postResp.redirectedUrl.contains("/profile?success="));
        assertEquals("Alex Updated Morgan", currentUser.getFullName());
    }
}
