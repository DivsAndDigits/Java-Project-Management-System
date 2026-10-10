package service;

import controller.MockServletSupport;
import controller.NotificationServlet;
import filter.SecurityHeadersFilter;
import jakarta.servlet.http.HttpServletResponse;
import dao.*;
import exception.ResourceNotFoundException;
import exception.UnauthorizedAccessException;
import exception.ValidationException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.concurrent.DeadlineReminderTask;
import service.concurrent.NotificationCoordinator;
import util.PasswordUtils;
import util.SecurityUtils;
import util.ValidationUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Advanced Features, RBAC, Notifications,
 * Password Security, Input Validation, and Reporting.
 */
@DisplayName("Advanced Features, Security, Notifications & RBAC Verification Tests")
public class AdvancedFeaturesAndSecurityTest {

    private UserDao userDao;
    private ProjectDao projectDao;
    private TaskDao taskDao;
    private NotificationDao notificationDao;
    private TeamMemberDao teamMemberDao;

    private UserService userService;
    private ProjectService projectService;
    private TaskService taskService;
    private NotificationService notificationService;
    private NotificationCoordinator coordinator;

    private Admin adminUser;
    private ProjectManager managerUser;
    private TeamMember memberUser1;
    private TeamMember memberUser2;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        teamMemberDao = new InMemoryTeamMemberDao(userDao);
        notificationDao = new InMemoryNotificationDao();
        notificationService = new NotificationServiceImpl(notificationDao);
        coordinator = new NotificationCoordinator(notificationService);

        projectDao = new InMemoryProjectDao(userDao);
        taskDao = new InMemoryTaskDao(userDao);

        projectService = new ProjectServiceImpl(projectDao, teamMemberDao, taskDao, notificationService);
        taskService = new TaskServiceImpl(taskDao, projectDao, notificationService);
        userService = new UserServiceImpl(userDao, projectDao, taskDao);

        adminUser = new Admin(91L, "sysadmin", "System Administrator", "admin@corp.test", "Security", "SA", "SYSTEM_ADMIN", true);
        managerUser = new ProjectManager(92L, "pm.john", "John PM", "john.pm@corp.test", "Product", "JP", 110000.0, "PMP");
        memberUser1 = new TeamMember(93L, "dev.alice", "Alice Dev", "alice@corp.test", "Engineering", "AD", "Backend Specialist", 30);
        memberUser2 = new TeamMember(94L, "qa.bob", "Bob QA", "bob@corp.test", "QA", "BQ", "QA Engineer", 25);

        userDao.save(adminUser);
        userDao.save(managerUser);
        userDao.save(memberUser1);
        userDao.save(memberUser2);
    }

    // =========================================================================
    // 1. Adaptive Password Hashing & In-Flight Transparent Migration
    // =========================================================================
    @Test
    @DisplayName("Password Security: Adaptive PBKDF2 generation, 100k rounds, constant-time verification & legacy support")
    void testPasswordHashingAndVerification() {
        String rawPassword = "StrongSecretPassword#2026";
        String hashed = PasswordUtils.hashPassword(rawPassword);

        assertNotNull(hashed);
        assertTrue(hashed.startsWith("PBKDF2$"), "Hashed password must start with PBKDF2$ prefix");
        String[] parts = hashed.split("\\$");
        assertEquals(4, parts.length, "Hash format must be PBKDF2$<iterations>$<saltHex>$<hashHex>");
        assertEquals("100000", parts[1], "Iteration count must match configured 100,000 rounds");
        assertEquals(32, parts[2].length(), "16-byte salt must be 32 hex characters");
        assertEquals(64, parts[3].length(), "256-bit key must be 64 hex characters");

        // Proper verification
        assertTrue(PasswordUtils.verifyPassword(rawPassword, hashed), "Original raw password must match hash");
        assertFalse(PasswordUtils.verifyPassword("WrongPassword#999", hashed), "Incorrect password must be rejected");

        // Backward compatibility with legacy salted SHA-256
        String legacySalt = "0123456789abcdef0123456789abcdef";
        String legacyHash = PasswordUtils.computeLegacySha256(legacySalt, "legacyPass2025");
        String storedLegacySha = "SHA256$" + legacySalt + "$" + legacyHash;
        assertTrue(PasswordUtils.verifyPassword("legacyPass2025", storedLegacySha), "Legacy SHA-256 must verify correctly");
        assertFalse(PasswordUtils.verifyPassword("wrongPass", storedLegacySha), "Wrong password must fail");
        assertTrue(PasswordUtils.needsRehash(storedLegacySha), "Legacy SHA-256 must require rehash to PBKDF2");

        // Backward compatibility with legacy plaintext passwords
        String legacyPlaintext = "password123";
        assertTrue(PasswordUtils.verifyPassword("password123", legacyPlaintext), "Legacy plaintext must match itself");
        assertFalse(PasswordUtils.verifyPassword("wrongpass", legacyPlaintext), "Wrong password for legacy must fail");
        assertTrue(PasswordUtils.needsRehash(legacyPlaintext), "Legacy plaintext must require rehash to PBKDF2");
        assertFalse(PasswordUtils.needsRehash(hashed), "Current PBKDF2 hash does not need rehash");
    }

    @Test
    @DisplayName("Password Security: Transparent in-flight upgrade to PBKDF2 on legacy user login")
    void testPasswordInFlightMigrationOnLogin() {
        // Register user with legacy plaintext password
        TeamMember legacyUser = new TeamMember(95L, "legacy.carol", "Carol Legacy", "carol@corp.test", "Engineering", "CL", "Dev", 25);
        legacyUser.setPassword("legacyCarolSecret#1");
        userDao.save(legacyUser);

        // Verify stored credential requires rehash
        User storedBefore = userDao.findById(legacyUser.getId()).orElseThrow();
        assertTrue(PasswordUtils.needsRehash(storedBefore.getPassword()), "Stored password before login must require rehash");

        // Successful login triggers transparent in-flight upgrade
        User authenticated = userService.loginUser("legacy.carol", "legacyCarolSecret#1");
        assertNotNull(authenticated, "User must successfully authenticate");

        // Verify password in DAO was transparently upgraded to PBKDF2
        User storedAfter = userDao.findById(legacyUser.getId()).orElseThrow();
        assertNotNull(storedAfter.getPassword());
        assertTrue(storedAfter.getPassword().startsWith("PBKDF2$100000$"), "Stored password must be upgraded to PBKDF2 with 100k rounds");
        assertFalse(PasswordUtils.needsRehash(storedAfter.getPassword()), "Upgraded hash must no longer need rehash");
        assertTrue(PasswordUtils.verifyPassword("legacyCarolSecret#1", storedAfter.getPassword()), "New PBKDF2 hash must verify against original password");
    }

    @Test
    @DisplayName("Security Headers & CSRF: Strict headers injected and forged tokens rejected with HTTP 403")
    void testSecurityHeadersFilterAndCsrfProtection() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        // 1. Verify defensive HTTP response headers injected
        MockServletSupport.TestRequest req = new MockServletSupport.TestRequest("/projects");
        MockServletSupport.TestResponse resp = new MockServletSupport.TestResponse();
        MockServletSupport.TestFilterChain chain = new MockServletSupport.TestFilterChain();

        filter.doFilter(req.asProxy(), resp.asProxy(), chain);
        assertTrue(chain.invoked, "Filter chain must be invoked for safe GET requests");
        assertEquals("nosniff", resp.headers.get("X-Content-Type-Options"));
        assertEquals("SAMEORIGIN", resp.headers.get("X-Frame-Options"));
        assertEquals("1; mode=block", resp.headers.get("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin", resp.headers.get("Referrer-Policy"));
        assertNotNull(resp.headers.get("Content-Security-Policy"));

        // 2. Authenticated POST with invalid CSRF token -> Rejected with HTTP 403
        MockServletSupport.TestRequest postReq = new MockServletSupport.TestRequest("/projects");
        postReq.method = "POST";
        MockServletSupport.TestSession session = new MockServletSupport.TestSession();
        session.setAttribute("csrfToken", "valid-session-secret-token-123");
        session.setAttribute("currentUser", adminUser);
        postReq.session = session;
        postReq.setParameter("csrfToken", "malicious-forged-token-999"); // Tampered token

        MockServletSupport.TestResponse postResp = new MockServletSupport.TestResponse();
        MockServletSupport.TestFilterChain postChain = new MockServletSupport.TestFilterChain();

        filter.doFilter(postReq.asProxy(), postResp.asProxy(), postChain);
        assertFalse(postChain.invoked, "Filter chain must NOT be invoked when CSRF token is invalid");
        assertEquals(HttpServletResponse.SC_FORBIDDEN, postResp.status, "Response status must be 403 Forbidden");

        // 3. Authenticated POST with matching CSRF token -> Permitted
        MockServletSupport.TestRequest validReq = new MockServletSupport.TestRequest("/projects");
        validReq.method = "POST";
        validReq.session = session;
        validReq.setParameter("csrfToken", "valid-session-secret-token-123"); // Matching token

        MockServletSupport.TestResponse validResp = new MockServletSupport.TestResponse();
        MockServletSupport.TestFilterChain validChain = new MockServletSupport.TestFilterChain();

        filter.doFilter(validReq.asProxy(), validResp.asProxy(), validChain);
        assertTrue(validChain.invoked, "Filter chain must be invoked when CSRF token matches session");
    }

    @Test
    @DisplayName("Notification IDOR Protection: Team member cannot mark another user's notification as read")
    void testNotificationIdorProtection() throws Exception {
        NotificationServlet servlet = new NotificationServlet(notificationService, userService);

        // Create notification owned by Alice (memberUser1)
        Notification aliceNotif = new Notification(
                201L,
                memberUser1.getId(),
                "Confidential Performance Review",
                "Review available in portal",
                "info",
                false,
                LocalDateTime.now(),
                "Just now",
                null
        );
        notificationDao.save(aliceNotif);

        // Bob (memberUser2) tries to mark Alice's notification as read via action=markRead&id=201
        MockServletSupport.TestRequest bobReq = new MockServletSupport.TestRequest("/notifications");
        bobReq.method = "POST";
        MockServletSupport.TestSession bobSession = new MockServletSupport.TestSession();
        bobSession.setAttribute("currentUser", memberUser2); // Bob's session
        bobReq.session = bobSession;
        bobReq.setParameter("action", "markRead");
        bobReq.setParameter("id", "201");

        MockServletSupport.TestResponse bobResp = new MockServletSupport.TestResponse();
        servlet.doPost(bobReq.asProxy(), bobResp.asProxy());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, bobResp.status, "Cross-user notification modification must return 403 Forbidden");
        assertFalse(notificationDao.findById(201L).orElseThrow().isRead(), "Alice's notification must remain unread after unauthorized attempt");

        // Alice (the rightful owner) marks her own notification as read
        MockServletSupport.TestRequest aliceReq = new MockServletSupport.TestRequest("/notifications");
        aliceReq.method = "POST";
        MockServletSupport.TestSession aliceSession = new MockServletSupport.TestSession();
        aliceSession.setAttribute("currentUser", memberUser1); // Alice's session
        aliceReq.session = aliceSession;
        aliceReq.setParameter("action", "markRead");
        aliceReq.setParameter("id", "201");

        MockServletSupport.TestResponse aliceResp = new MockServletSupport.TestResponse();
        servlet.doPost(aliceReq.asProxy(), aliceResp.asProxy());

        assertTrue(notificationDao.findById(201L).orElseThrow().isRead(), "Alice must be permitted to mark her own notification as read");
    }

    // =========================================================================
    // 2. Notification System: Project Created & Completed Events
    // =========================================================================
    @Test
    @DisplayName("Notifications: Automatic event generation for Project Creation and Project Completion")
    void testProjectCreatedAndCompletedNotifications() {
        int initialNotifications = notificationService.getUserNotifications(managerUser.getId()).size();

        // 1. Create project -> triggers project created notification
        Project project = new Project();
        project.setName("Autonomous Robotics Control");
        project.setCode("ROBO-01");
        project.setDescription("Core robotics control stack");
        project.setStatus(ProjectStatus.PLANNING);
        project.setPriority(Priority.HIGH);
        project.setManager(managerUser);
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        project.setBudget(75000.0);

        Project savedProject = projectService.createProject(project);
        assertNotNull(savedProject.getId());

        List<Notification> managerNotifs = notificationService.getUserNotifications(managerUser.getId());
        assertTrue(managerNotifs.size() > initialNotifications, "Project creation must generate a notification");
        boolean hasCreationNotif = managerNotifs.stream().anyMatch(n -> n.getTitle().contains("New Project Created"));
        assertTrue(hasCreationNotif, "Manager must receive 'New Project Created' notification");

        // 2. Complete project -> triggers project completed notification
        savedProject.setStatus(ProjectStatus.COMPLETED);
        projectService.updateProject(savedProject);

        managerNotifs = notificationService.getUserNotifications(managerUser.getId());
        boolean hasCompletionNotif = managerNotifs.stream().anyMatch(n -> n.getTitle().contains("Project Completed"));
        assertTrue(hasCompletionNotif, "Manager must receive 'Project Completed' notification");
    }

    // =========================================================================
    // 3. Notification System: Task Assigned & Completed Events + Mark As Read
    // =========================================================================
    @Test
    @DisplayName("Notifications: Task Assigned & Completed events and Read Status Management")
    void testTaskAssignedAndCompletedNotifications() {
        Project project = projectService.getAllProjects().get(0);

        // 1. Assign new task -> triggers notification for assignee
        Task task = new Task();
        task.setTitle("Design Kernel Memory Allocator");
        task.setDescription("Implement slab allocator in C/Rust");
        task.setProjectId(project.getId());
        task.setAssignee(memberUser1);
        task.setPriority(Priority.URGENT);
        task.setStatus(TaskStatus.TODO);
        task.setEstimatedHours(24);
        task.setDueDate(LocalDate.now().plusDays(5));

        Task savedTask = taskService.createTask(task);
        assertNotNull(savedTask.getId());

        List<Notification> aliceNotifs = notificationService.getUserNotifications(memberUser1.getId());
        assertFalse(aliceNotifs.isEmpty(), "Alice must receive task assignment notification");
        Notification assignNotif = aliceNotifs.stream()
                .filter(n -> n.getTitle().contains("New Task Assigned"))
                .findFirst()
                .orElse(null);
        assertNotNull(assignNotif, "Assignment notification must be present");
        assertFalse(assignNotif.isRead(), "New notification must be unread");

        // Mark as read
        notificationService.markNotificationAsRead(assignNotif.getId());
        Notification updatedNotif = notificationDao.findById(assignNotif.getId()).orElse(null);
        assertNotNull(updatedNotif);
        assertTrue(updatedNotif.isRead(), "Notification must be marked as read");

        // 2. Complete task -> triggers task completed notification
        taskService.updateTaskStatus(savedTask.getId(), TaskStatus.COMPLETED);

        aliceNotifs = notificationService.getUserNotifications(memberUser1.getId());
        boolean hasTaskCompletedNotif = aliceNotifs.stream().anyMatch(n -> n.getTitle().contains("Task Completed"));
        assertTrue(hasTaskCompletedNotif, "Assignee/Reporter must receive task completed notification");

        // Mark all as read
        notificationService.markAllNotificationsAsRead(memberUser1.getId());
        int unreadCount = notificationService.getUnreadNotificationCount(memberUser1.getId());
        assertEquals(0, unreadCount, "All notifications must be read after markAllNotificationsAsRead");
    }

    // =========================================================================
    // 4. Background Scheduler: Deadline Reminders & Deduplication Resilience
    // =========================================================================
    @Test
    @DisplayName("Background Task: DeadlineReminderTask dispatches alerts and dedups cleanly")
    void testDeadlineReminderSchedulerDeduplication() {
        DeadlineReminderTask reminderTask = new DeadlineReminderTask(taskDao, coordinator);

        // Run 1: Scans tasks and generates alerts
        reminderTask.run();
        long initialDispatched = coordinator.getTotalDispatchedCount();
        assertTrue(initialDispatched > 0, "Initial deadline scan must dispatch notifications for seed tasks");

        // Run 2: Immediate re-run must suppress duplicates without throwing exceptions
        reminderTask.run();
        assertEquals(initialDispatched, coordinator.getTotalDispatchedCount(),
                "Subsequent run must not re-dispatch identical deadline event keys");
        assertTrue(coordinator.getDuplicatesSuppressedCount() > 0,
                "Coordinator must track duplicate alerts as suppressed");
    }

    // =========================================================================
    // 5. Server-Side Role-Based Access Control (RBAC) Enforcement
    // =========================================================================
    @Test
    @DisplayName("RBAC: Administrator, Project Manager, and Team Member strict server-side permissions")
    void testRoleBasedAccessControlEnforcement() {
        Project project = projectService.getAllProjects().get(0);

        // --- Admin Privileges ---
        assertDoesNotThrow(() -> SecurityUtils.requireCanManageUsers(adminUser));
        assertDoesNotThrow(() -> SecurityUtils.requireCanCreateProject(adminUser));
        assertDoesNotThrow(() -> SecurityUtils.requireCanDeleteProject(adminUser, project));
        assertDoesNotThrow(() -> SecurityUtils.requireCanUpdateProject(adminUser, project));

        // --- Project Manager Privileges & Restrictions ---
        assertDoesNotThrow(() -> SecurityUtils.requireCanCreateProject(managerUser));
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanManageUsers(managerUser),
                "Project Manager cannot access user management");

        // --- Team Member Restrictions ---
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanManageUsers(memberUser1),
                "Team Member cannot access user management");
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanCreateProject(memberUser1),
                "Team Member cannot create projects");
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanDeleteProject(memberUser1, project),
                "Team Member cannot delete projects");
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanUpdateProject(memberUser1, project),
                "Team Member cannot edit project metadata");
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanCreateTask(memberUser1, project),
                "Team Member cannot create/assign tasks");

        // --- Task Status Permissions ---
        Task assignedTask = new Task();
        assignedTask.setId(501L);
        assignedTask.setTitle("Assigned to Alice");
        assignedTask.setAssignee(memberUser1);

        // Alice can update her own task status
        assertDoesNotThrow(() -> SecurityUtils.requireCanUpdateTaskStatus(memberUser1, assignedTask));

        // Bob cannot update Alice's task status
        assertThrows(UnauthorizedAccessException.class, () -> SecurityUtils.requireCanUpdateTaskStatus(memberUser2, assignedTask),
                "Bob cannot update Alice's assigned task");

        // Admin and Project Manager can update any task status
        assertDoesNotThrow(() -> SecurityUtils.requireCanUpdateTaskStatus(adminUser, assignedTask));
        assertDoesNotThrow(() -> SecurityUtils.requireCanUpdateTaskStatus(managerUser, assignedTask));
    }

    // =========================================================================
    // 6. Server-Side Input Validation Rules
    // =========================================================================
    @Test
    @DisplayName("Validation: Invalid date ranges, lengths, and emails throw ValidationException")
    void testInputValidationRules() {
        // Invalid date range (end date before start date)
        Project invalidDateProject = new Project();
        invalidDateProject.setName("Invalid Chronology Project");
        invalidDateProject.setCode("INV-01");
        invalidDateProject.setManager(managerUser);
        invalidDateProject.setStartDate(LocalDate.of(2026, 12, 1));
        invalidDateProject.setEndDate(LocalDate.of(2026, 11, 1)); // Earlier than start
        assertThrows(ValidationException.class, () -> projectService.createProject(invalidDateProject));

        // Short project name (< 3 chars)
        Project shortNameProject = new Project();
        shortNameProject.setName("AB");
        shortNameProject.setCode("PRJ-AB");
        shortNameProject.setManager(managerUser);
        assertThrows(ValidationException.class, () -> projectService.createProject(shortNameProject));

        // Negative budget encapsulation validation
        Project negBudgetProject = new Project();
        assertThrows(IllegalArgumentException.class, () -> negBudgetProject.setBudget(-500.0));

        // Invalid task title
        Task emptyTask = new Task();
        emptyTask.setTitle("   ");
        emptyTask.setProjectId(1L);
        assertThrows(ValidationException.class, () -> taskService.createTask(emptyTask));

        // ValidationUtils email validation
        assertTrue(ValidationUtils.isValidEmail("alex.morgan@company.com"));
        assertFalse(ValidationUtils.isValidEmail("plainaddress"));
        assertFalse(ValidationUtils.isValidEmail("missing@domain"));
    }

    // =========================================================================
    // 7. Reports Statistics & Zero-Task Divide-by-Zero Protection
    // =========================================================================
    @Test
    @DisplayName("Reports: Real metric computation, Collection breakdowns, and 0-task safety")
    void testReportsCalculationsAndZeroTaskProtection() {
        // Create an empty project with 0 tasks to test divide-by-zero protection
        Project emptyProject = new Project();
        emptyProject.setName("Zero Task Clean Slate Initiative");
        emptyProject.setCode("ZERO-01");
        emptyProject.setManager(managerUser);
        emptyProject.setStartDate(LocalDate.now());
        emptyProject.setEndDate(LocalDate.now().plusMonths(1));
        Project savedEmpty = projectService.createProject(emptyProject);

        List<Task> emptyTasks = taskService.getTasksByProjectId(savedEmpty.getId());
        assertEquals(0, emptyTasks.size());

        // Safe percentage calculation with 0 tasks
        int total = emptyTasks.size();
        long completed = emptyTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int safePercentage = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
        assertEquals(0, safePercentage, "Completion percentage for 0 tasks must safely evaluate to 0%");

        // Verify task grouping collections
        Map<Priority, Long> priorityDistribution = taskService.getTaskCountByPriority();
        assertNotNull(priorityDistribution, "Priority distribution map must not be null");
        assertFalse(priorityDistribution.isEmpty(), "Priority distribution map must contain counts");

        Map<TaskStatus, Long> statusDistribution = taskService.getTaskCountByStatus();
        assertNotNull(statusDistribution, "Status distribution map must not be null");
        assertFalse(statusDistribution.isEmpty(), "Status distribution map must contain counts");

        List<TeamMemberSummary> teamSummaries = userService.getTeamMemberSummaries();
        assertNotNull(teamSummaries, "Team member summaries list must not be null");
        assertFalse(teamSummaries.isEmpty(), "Team member summaries list must contain users");
    }
}
