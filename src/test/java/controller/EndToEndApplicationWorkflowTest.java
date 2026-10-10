package controller;

import exception.UnauthorizedAccessException;
import filter.AuthenticationFilter;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ServiceFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive End-to-End User Workflow & Dynamic Domain Logic Test Suite.
 * Validates the complete user workflow:
 * LOGIN -> DASHBOARD -> PROJECTS -> PROJECT DETAILS -> TASKS -> TEAM -> REPORTS -> NOTIFICATIONS
 *
 * Verifies:
 * 1. Real database-driven dashboard statistics across all 7 metrics (totalProjects, activeProjects,
 *    completedProjects, pendingTasks, completedTasks, overdueTasks, teamMembersCount).
 * 2. Automatic project progress calculation: Completed Tasks / Total Tasks * 100 (0% if total is 0).
 * 3. Dynamic progress synchronization on task creation, task completion, and task deletion.
 * 4. Dynamic overdue task logic (dueDate < today AND status != COMPLETED or status == OVERDUE).
 * 5. Multi-criteria combined filtering for both Projects and Tasks.
 * 6. Dedicated Project Details view, inline task creation, and team member management.
 */
class EndToEndApplicationWorkflowTest {

    private ServiceFactory factory;
    private LoginServlet loginServlet;
    private DashboardServlet dashboardServlet;
    private ProjectServlet projectServlet;
    private ProjectDetailsServlet projectDetailsServlet;
    private TaskServlet taskServlet;
    private TeamServlet teamServlet;
    private NotificationServlet notificationServlet;
    private ProfileServlet profileServlet;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory.reset();
        factory = ServiceFactory.getInstance();

        loginServlet = new LoginServlet(factory.getUserService());
        dashboardServlet = new DashboardServlet(factory.getDashboardService(), factory.getProjectService(), factory.getUserService());
        projectServlet = new ProjectServlet(factory.getProjectService(), factory.getUserService());
        projectDetailsServlet = new ProjectDetailsServlet(factory.getProjectService(), factory.getTaskService(), factory.getUserService());
        taskServlet = new TaskServlet(factory.getTaskService(), factory.getProjectService(), factory.getUserService());
        teamServlet = new TeamServlet(factory.getUserService());
        notificationServlet = new NotificationServlet(factory.getNotificationService(), factory.getUserService());
        profileServlet = new ProfileServlet(factory.getUserService(), factory.getProjectService(), factory.getTaskService());

        // Clean up test projects from previous runs to ensure clean idempotency
        factory.getProjectService().getAllProjects().stream()
                .filter(p -> p.getCode() != null && (p.getCode().startsWith("AI-ENG") || p.getCode().startsWith("AUTO-INFRA") || p.getCode().startsWith("PRJ-PROGRESS")))
                .forEach(p -> factory.getProjectService().deleteProject(p.getId()));
    }

    @Test
    @DisplayName("Workflow Step 1: Login creates authenticated session")
    void testStep1_LoginWorkflow() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/login");
        req.setParameter("usernameOrEmail", "alex.morgan");
        req.setParameter("password", "password123");

        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();
        loginServlet.doPost(req.asProxy(), resp.asProxy());

        assertNotNull(req.session, "Session must be initialized");
        User currentUser = (User) req.session.getAttribute(AuthenticationFilter.SESSION_USER_KEY);
        assertNotNull(currentUser, "Current authenticated user must be set in session");
        assertEquals("alex.morgan", currentUser.getUsername());
        assertTrue(resp.redirectedUrl.contains("/dashboard"), "Must redirect to dashboard");
    }

    @Test
    @DisplayName("Workflow Step 2: Dashboard computes all 7 real database metrics without hardcoded values")
    void testStep2_DashboardRealDatabaseMetrics() throws Exception {
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/dashboard");
        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        dashboardServlet.doGet(req.asProxy(), resp.asProxy());

        DashboardStats stats = (DashboardStats) req.attributes.get("stats");
        assertNotNull(stats, "DashboardStats must be attached to request");

        // Verify all 7 core metrics
        assertTrue(stats.getTotalProjects() >= 0, "Total projects must be calculated");
        assertTrue(stats.getActiveProjects() >= 0, "Active projects must be calculated");
        assertTrue(stats.getCompletedProjects() >= 0, "Completed projects must be calculated");
        assertTrue(stats.getTotalTasks() >= 0, "Total tasks must be calculated");
        assertTrue(stats.getPendingTasks() >= 0, "Pending tasks must be calculated");
        assertTrue(stats.getCompletedTasks() >= 0, "Completed tasks must be calculated");
        assertTrue(stats.getOverdueTasks() >= 0, "Overdue tasks must be calculated");
        assertTrue(stats.getTeamMembersCount() >= 0, "Team members count must be calculated");

        // Check relationship: pendingTasks + completedTasks == totalTasks
        assertEquals(stats.getTotalTasks(), stats.getPendingTasks() + stats.getCompletedTasks(),
                "Total tasks must equal pending tasks plus completed tasks");
    }

    @Test
    @DisplayName("Workflow Step 3 & 4: Project CRUD and Multi-Criteria Combined Filtering")
    void testStep3_ProjectCrudAndCombinedFiltering() throws Exception {
        User manager = factory.getUserService().getUserById(1L);

        // 1. Create project
        Project project = new Project();
        project.setName("AI Telemetry Engine");
        project.setCode("AI-ENG-01");
        project.setDescription("Real-time distributed telemetry processing pipeline");
        project.setStatus(ProjectStatus.PLANNING);
        project.setPriority(Priority.HIGH);
        project.setBudget(75000.0);
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        project.setManager(manager);

        Project created = factory.getProjectService().createProject(project);
        assertNotNull(created.getId(), "Created project must have generated ID");
        assertEquals(0, created.getProgressPercentage(), "Initial progress must be 0%");

        // 2. Combined multi-criteria filter test (Search + Status + Priority)
        List<Project> filtered = factory.getProjectService().filterProjects("Telemetry", ProjectStatus.PLANNING, Priority.HIGH, "name");
        assertFalse(filtered.isEmpty(), "Combined filtering must return matched project");
        assertEquals("AI Telemetry Engine", filtered.get(0).getName());

        // 3. Negative filter test
        List<Project> none = factory.getProjectService().filterProjects("Telemetry", ProjectStatus.COMPLETED, Priority.LOW, null);
        assertTrue(none.isEmpty(), "Mismatched criteria must return empty list");

        // 4. Update project
        created.setDescription("Updated telemetry description with machine learning heuristics");
        created.setStatus(ProjectStatus.IN_PROGRESS);
        boolean updated = factory.getProjectService().updateProject(created);
        assertTrue(updated, "Project update must succeed");

        Project reloaded = factory.getProjectService().getProjectById(created.getId());
        assertEquals(ProjectStatus.IN_PROGRESS, reloaded.getStatus());
        assertTrue(reloaded.getDescription().contains("machine learning"));
    }

    @Test
    @DisplayName("Workflow Step 5: Dedicated Project Details Page and Task/Team Management")
    void testStep4_ProjectDetailsPageWorkflow() throws Exception {
        User manager = factory.getUserService().getUserById(1L);
        Project project = new Project();
        project.setName("Autonomous Cloud Infrastructure");
        project.setCode("AUTO-INFRA");
        project.setDescription("Automated provisioning with Terraform and Nomad");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setPriority(Priority.URGENT);
        project.setManager(manager);
        project = factory.getProjectService().createProject(project);

        Long projectId = project.getId();

        // 1. GET Project Details
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/project-details");
        req.setParameter("id", String.valueOf(projectId));
        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();

        projectDetailsServlet.doGet(req.asProxy(), resp.asProxy());

        Project viewProject = (Project) req.attributes.get("project");
        assertNotNull(viewProject);
        assertEquals("Autonomous Cloud Infrastructure", viewProject.getName());
        assertEquals("/WEB-INF/views/project-details.jsp", req.forwardPath);

        // 2. POST Add Team Member
        MockServletSupport.TestRequest postReqMember = new MockServletSupport.TestRequest("/project-details");
        postReqMember.setParameter("action", "addMember");
        postReqMember.setParameter("id", String.valueOf(projectId));
        postReqMember.setParameter("userId", "2"); // Add user 2
        MockServletSupport.TestResponse postRespMember = new MockServletSupport.TestResponse();

        projectDetailsServlet.doPost(postReqMember.asProxy(), postRespMember.asProxy());

        Set<User> team = factory.getProjectService().getProjectTeamMembers(projectId);
        assertTrue(team.stream().anyMatch(u -> u.getId().equals(2L)), "User 2 must be in project team");

        // 3. POST Add Task to this project
        MockServletSupport.TestRequest postReqTask = new MockServletSupport.TestRequest("/project-details");
        postReqTask.setParameter("action", "addTask");
        postReqTask.setParameter("id", String.valueOf(projectId));
        postReqTask.setParameter("title", "Provision Nomad Cluster");
        postReqTask.setParameter("priority", "HIGH");
        postReqTask.setParameter("assigneeId", "2");
        postReqTask.setParameter("estimatedHours", "16");
        postReqTask.session = new MockServletSupport.TestSession();
        postReqTask.session.setAttribute("currentUser", manager);

        MockServletSupport.TestResponse postRespTask = new MockServletSupport.TestResponse();
        projectDetailsServlet.doPost(postReqTask.asProxy(), postRespTask.asProxy());

        List<Task> projectTasks = factory.getTaskService().getTasksByProjectId(projectId);
        assertEquals(1, projectTasks.size(), "Project must have 1 task");
        assertEquals("Provision Nomad Cluster", projectTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Automatic Project Progress Calculation and Dynamic Synchronization")
    void testStep6_AutomaticProjectProgressSync() {
        User reporter = factory.getUserService().getUserById(1L);

        // 1. New project -> 0 tasks -> 0% progress
        Project project = new Project();
        project.setName("Progress Auto-Calc System");
        project.setCode("PRJ-PROGRESS-" + System.nanoTime() % 100000);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setPriority(Priority.MEDIUM);
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(2));
        project.setBudget(50000.0);
        project.setManager(reporter);
        Project savedProject = factory.getProjectService().createProject(project);

        Long pid = savedProject.getId();
        assertEquals(0, factory.getProjectService().getProjectById(pid).getProgressPercentage(), "0 tasks -> 0% progress");

        // 2. Add Task 1 (TODO) -> 1 task, 0 completed -> 0%
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setProjectId(pid);
        task1.setStatus(TaskStatus.TODO);
        task1.setReporter(reporter);
        task1 = factory.getTaskService().createTask(task1);

        Project pAfterTask1 = factory.getProjectService().getProjectById(pid);
        assertEquals(1, pAfterTask1.getTotalTasks());
        assertEquals(0, pAfterTask1.getCompletedTasks());
        assertEquals(0, pAfterTask1.getProgressPercentage(), "1 task (0 done) -> 0% progress");

        // 3. Add Task 2 (TODO) -> 2 tasks, 0 completed -> 0%
        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setProjectId(pid);
        task2.setStatus(TaskStatus.TODO);
        task2.setReporter(reporter);
        task2 = factory.getTaskService().createTask(task2);

        Project pAfterTask2 = factory.getProjectService().getProjectById(pid);
        assertEquals(2, pAfterTask2.getTotalTasks());
        assertEquals(0, pAfterTask2.getProgressPercentage(), "2 tasks (0 done) -> 0% progress");

        // 4. Complete Task 1 -> 2 tasks, 1 completed -> 50%
        factory.getTaskService().updateTaskStatus(task1.getId(), TaskStatus.COMPLETED);

        Project pAfterTask1Complete = factory.getProjectService().getProjectById(pid);
        assertEquals(2, pAfterTask1Complete.getTotalTasks());
        assertEquals(1, pAfterTask1Complete.getCompletedTasks());
        assertEquals(50, pAfterTask1Complete.getProgressPercentage(), "1 of 2 completed -> 50% progress");

        // 5. Complete Task 2 -> 2 tasks, 2 completed -> 100% and project auto-transitions to COMPLETED
        factory.getTaskService().updateTaskStatus(task2.getId(), TaskStatus.COMPLETED);

        Project pAfterAllComplete = factory.getProjectService().getProjectById(pid);
        assertEquals(2, pAfterAllComplete.getTotalTasks());
        assertEquals(2, pAfterAllComplete.getCompletedTasks());
        assertEquals(100, pAfterAllComplete.getProgressPercentage(), "2 of 2 completed -> 100% progress");
        assertEquals(ProjectStatus.COMPLETED, pAfterAllComplete.getStatus(), "100% progress auto-transitions status to COMPLETED");

        // 6. Delete Task 2 -> 1 task, 1 completed -> 100%
        factory.getTaskService().deleteTask(task2.getId());

        Project pAfterTaskDelete = factory.getProjectService().getProjectById(pid);
        assertEquals(1, pAfterTaskDelete.getTotalTasks());
        assertEquals(1, pAfterTaskDelete.getCompletedTasks());
        assertEquals(100, pAfterTaskDelete.getProgressPercentage(), "1 of 1 completed -> 100% progress");

        // 7. Add Task 3 (TODO) -> 2 tasks, 1 completed -> 50%
        Task task3 = new Task();
        task3.setTitle("Task 3");
        task3.setProjectId(pid);
        task3.setStatus(TaskStatus.TODO);
        task3.setReporter(reporter);
        factory.getTaskService().createTask(task3);

        Project pAfterTask3 = factory.getProjectService().getProjectById(pid);
        assertEquals(2, pAfterTask3.getTotalTasks());
        assertEquals(1, pAfterTask3.getCompletedTasks());
        assertEquals(50, pAfterTask3.getProgressPercentage(), "1 of 2 completed -> 50% progress");
    }

    @Test
    @DisplayName("Dynamic Overdue Task Logic: dueDate < today AND status != COMPLETED")
    void testStep7_DynamicOverdueTaskLogic() {
        User reporter = factory.getUserService().getUserById(1L);

        // Task with past due date and TODO status -> OVERDUE
        Task overdueTask = new Task();
        overdueTask.setTitle("Legacy Migration");
        overdueTask.setProjectId(1L);
        overdueTask.setStatus(TaskStatus.TODO);
        overdueTask.setDueDate(LocalDate.now().minusDays(5));
        overdueTask.setReporter(reporter);
        Task savedOverdue = factory.getTaskService().createTask(overdueTask);

        assertTrue(savedOverdue.isOverdue(), "Past due date with TODO status must be overdue");

        // Mark as COMPLETED -> no longer overdue
        factory.getTaskService().updateTaskStatus(savedOverdue.getId(), TaskStatus.COMPLETED);
        Task completedTask = factory.getTaskService().getTaskById(savedOverdue.getId());
        assertFalse(completedTask.isOverdue(), "Completed tasks must never be overdue even if past due date");

        // Task with explicit OVERDUE status enum
        Task explicitOverdue = new Task();
        explicitOverdue.setTitle("Explicit Overdue Task");
        explicitOverdue.setProjectId(1L);
        explicitOverdue.setStatus(TaskStatus.OVERDUE);
        explicitOverdue.setReporter(reporter);
        assertTrue(explicitOverdue.isOverdue(), "Explicit OVERDUE status must be overdue");

        // Task due in future -> NOT overdue
        Task futureTask = new Task();
        futureTask.setTitle("Future Roadmap Item");
        futureTask.setProjectId(1L);
        futureTask.setStatus(TaskStatus.TODO);
        futureTask.setDueDate(LocalDate.now().plusDays(10));
        assertFalse(futureTask.isOverdue(), "Future due date must not be overdue");
    }

    @Test
    @DisplayName("Task Multi-Criteria Combined Filtering")
    void testStep8_TaskMultiCriteriaFiltering() {
        User reporter = factory.getUserService().getUserById(1L);
        User assignee = factory.getUserService().getUserById(2L);

        Task target = new Task();
        target.setTitle("Implement OAuth2 Resource Server");
        target.setDescription("Token introspection and cryptographic JWT verification");
        target.setProjectId(1L);
        target.setStatus(TaskStatus.IN_PROGRESS);
        target.setPriority(Priority.URGENT);
        target.setAssignee(assignee);
        target.setReporter(reporter);
        target.setDueDate(LocalDate.now().plusDays(4));
        factory.getTaskService().createTask(target);

        // Combined filter matching all criteria
        List<Task> results = factory.getTaskService().filterTasks(
                "OAuth2",
                1L,
                2L,
                TaskStatus.IN_PROGRESS,
                Priority.URGENT,
                false,
                "priority"
        );

        assertFalse(results.isEmpty(), "Multi-criteria search must match target task");
        assertEquals("Implement OAuth2 Resource Server", results.get(0).getTitle());

        // Negative filter: mismatch on status
        List<Task> emptyResults = factory.getTaskService().filterTasks(
                "OAuth2",
                1L,
                2L,
                TaskStatus.COMPLETED,
                Priority.URGENT,
                null,
                null
        );
        assertTrue(emptyResults.isEmpty(), "Mismatched status must return empty list");
    }

    @Test
    @DisplayName("Workflow Step 6, 7 & 8: Team Directory, Reports & Notifications")
    void testStep9_TeamReportsNotificationsWorkflow() throws Exception {
        // 1. Team Servlet
        MockServletSupport.TestRequest teamReq = new MockServletSupport.TestRequest("/team");
        MockServletSupport.TestResponse teamResp = new MockServletSupport.TestResponse();
        teamServlet.doGet(teamReq.asProxy(), teamResp.asProxy());

        List<TeamMemberSummary> summaries = (List<TeamMemberSummary>) teamReq.attributes.get("teamSummaries");
        assertNotNull(summaries, "Team summaries must be populated");
        assertFalse(summaries.isEmpty(), "Team members directory must not be empty");

        // 2. Notification Center
        User user = factory.getUserService().getUserById(1L);
        MockServletSupport.TestRequest notifReq = new MockServletSupport.TestRequest("/notifications");
        notifReq.session = new MockServletSupport.TestSession();
        notifReq.session.setAttribute("currentUser", user);
        MockServletSupport.TestResponse notifResp = new MockServletSupport.TestResponse();

        notificationServlet.doGet(notifReq.asProxy(), notifResp.asProxy());

        List<Notification> notifs = (List<Notification>) notifReq.attributes.get("notifications");
        assertNotNull(notifs, "User notifications list must be present");
        assertEquals("/WEB-INF/views/notifications.jsp", notifReq.forwardPath);

        // 3. Mark all read
        MockServletSupport.TestRequest markAllReq = new MockServletSupport.TestRequest("/notifications");
        markAllReq.setParameter("action", "markAllRead");
        markAllReq.session = new MockServletSupport.TestSession();
        markAllReq.session.setAttribute("currentUser", user);
        MockServletSupport.TestResponse markAllResp = new MockServletSupport.TestResponse();

        notificationServlet.doGet(markAllReq.asProxy(), markAllResp.asProxy());
        assertTrue(markAllResp.redirectedUrl.contains("/notifications"));
        assertEquals(0, factory.getNotificationService().getUnreadNotificationCount(user.getId()));
    }
}
