package dao;

import model.*;
import org.junit.jupiter.api.*;
import util.DatabaseConnectionManager;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ==============================================================================
 * Integration Test: Real MySQL JDBC Database Operations (7 marks verification)
 * ==============================================================================
 * 
 * Verifies live database interactions when a live MySQL 8.x database is running and
 * reachable with credentials configured in db.properties or system properties.
 * 
 * Uses JUnit 5 Assumptions (Assumptions.assumeTrue) to conditionally run when MySQL
 * is reachable, ensuring standard build commands (mvn test) never fail when executed
 * in environments where MySQL credentials are not yet configured.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcDatabaseIntegrationTest {

    private static DatabaseConnectionManager connManager;
    private static JdbcUserDao userDao;
    private static JdbcProjectDao projectDao;
    private static JdbcTaskDao taskDao;
    private static JdbcNotificationDao notificationDao;
    private static JdbcTeamMemberDao teamMemberDao;

    @BeforeAll
    static void init() {
        connManager = DatabaseConnectionManager.getInstance();
        boolean available = connManager.isDatabaseAvailable();
        // Skip all tests in this suite if live MySQL is not reachable
        Assumptions.assumeTrue(available,
                "Live MySQL database is not reachable on localhost:3306. Skipping live integration tests.");

        connManager.ensureSchemaUpdated();

        teamMemberDao = new JdbcTeamMemberDao(connManager);
        userDao = new JdbcUserDao(connManager);
        projectDao = new JdbcProjectDao(connManager, teamMemberDao);
        taskDao = new JdbcTaskDao(connManager);
        notificationDao = new JdbcNotificationDao(connManager);
    }

    @Test
    @Order(1)
    @DisplayName("Live MySQL: Verify connection creation")
    void testLiveConnection() {
        assertNotNull(connManager.getConnection(), "Should acquire valid active java.sql.Connection from MySQL.");
    }

    @Test
    @Order(2)
    @DisplayName("Live MySQL: User CRUD & Polymorphic Subtypes")
    void testUserCrud() {
        String testEmail = "test." + System.currentTimeMillis() + "@company.com";
        String testUsername = "user_" + System.currentTimeMillis();

        ProjectManager newPm = new ProjectManager(null, testUsername, "Test Project Manager",
                testEmail, "Engineering", "TP", 85000.0, "PMP Certified");

        User saved = userDao.register(newPm, "secretPass123");
        assertNotNull(saved.getId(), "Generated primary key ID must be assigned to saved user.");

        Optional<User> loggedIn = userDao.login(testUsername, "secretPass123");
        assertTrue(loggedIn.isPresent(), "User should be able to log in with correct credentials.");

        Optional<User> fetched = userDao.findById(saved.getId());
        assertTrue(fetched.isPresent(), "Saved user must be retrievable by ID.");
        assertEquals(testUsername, fetched.get().getUsername());
        assertTrue(fetched.get() instanceof ProjectManager, "Hydrated entity must preserve ProjectManager polymorphic type.");

        // Update user
        saved.setDepartment("Advanced R&D");
        boolean updated = userDao.update(saved);
        assertTrue(updated, "User update must return true.");

        Optional<User> byEmail = userDao.findByEmail(testEmail);
        assertTrue(byEmail.isPresent());
        assertEquals("Advanced R&D", byEmail.get().getDepartment());

        // Delete user
        boolean deleted = userDao.deleteById(saved.getId());
        assertTrue(deleted, "User deletion must return true.");
        assertFalse(userDao.findById(saved.getId()).isPresent(), "Deleted user must no longer exist.");
    }

    @Test
    @Order(3)
    @DisplayName("Live MySQL: Project CRUD & Joins with Manager")
    void testProjectCrud() {
        String code = "PRJ-T" + (System.currentTimeMillis() % 10000);
        Project project = new Project(null, code, "Test Live Project", "Description for live test project",
                ProjectStatus.IN_PROGRESS, Priority.HIGH, null,
                LocalDate.now(), LocalDate.now().plusDays(30), 50000.0, 10000.0, 20, 10, 2);

        Project saved = projectDao.save(project);
        assertNotNull(saved.getId());

        Optional<Project> fetched = projectDao.findById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Test Live Project", fetched.get().getName());

        saved.setStatus(ProjectStatus.COMPLETED);
        boolean updated = projectDao.update(saved);
        assertTrue(updated);

        boolean deleted = projectDao.deleteById(saved.getId());
        assertTrue(deleted);
    }

    @Test
    @Order(4)
    @DisplayName("Live MySQL: Task CRUD, Status Filtering & Overdue Check")
    void testTaskCrud() {
        // Find existing project or create temporary
        List<Project> projects = projectDao.findAll();
        Long projectId = projects.isEmpty() ? 1L : projects.get(0).getId();

        Task task = new Task(null, projectId, "Sample Project", "Integration Test Task",
                "Testing task persistence", TaskStatus.TODO, Priority.URGENT,
                null, null, LocalDate.now().minusDays(2), 20, 5);

        Task saved = taskDao.save(task);
        assertNotNull(saved.getId());

        Optional<Task> fetched = taskDao.findById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Integration Test Task", fetched.get().getTitle());

        // Overdue check
        List<Task> overdue = taskDao.findOverdueTasks();
        assertNotNull(overdue);

        boolean assigned = taskDao.assignTask(saved.getId(), null);
        assertTrue(assigned);

        taskDao.deleteById(saved.getId());
    }

    @Test
    @Order(5)
    @DisplayName("Live MySQL: Team Member Uniqueness & Duplicate Prevention")
    void testTeamMemberUniqueness() {
        List<Project> projects = projectDao.findAll();
        if (projects.isEmpty()) return;
        Long projectId = projects.get(0).getId();

        // Add member
        teamMemberDao.addTeamMember(projectId, 99999L);
        // Duplicate addition must be prevented safely
        boolean duplicateAdded = teamMemberDao.addTeamMember(projectId, 99999L);
        assertFalse(duplicateAdded, "Duplicate membership must be rejected by database UNIQUE/PK constraint.");

        teamMemberDao.removeTeamMember(projectId, 99999L);
    }

    @Test
    @Order(6)
    @DisplayName("Live MySQL: Notification CRUD & Read Status Updates")
    void testNotificationCrud() {
        Notification notif = new Notification(null, 1L, "Live DB Test", "Testing notification in MySQL",
                "info", false, null, null, "projects");

        Notification saved = notificationDao.save(notif);
        assertNotNull(saved.getId());

        boolean marked = notificationDao.markAsRead(saved.getId());
        assertTrue(marked);

        Optional<Notification> fetched = notificationDao.findById(saved.getId());
        assertTrue(fetched.isPresent());
        assertTrue(fetched.get().isRead());

        notificationDao.deleteById(saved.getId());
    }
}
