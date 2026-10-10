package dao;

import model.*;
import org.junit.jupiter.api.*;
import service.ServiceFactory;
import service.ServiceFactory.DaoMode;
import util.DBConnection;
import util.DatabaseConnectionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ==============================================================================
 * Comprehensive End-to-End Verification Test for JDBC Persistence
 * ==============================================================================
 *
 * Verifies all 9 requirements specified in the prompt:
 * 1. Runtime dao.mode=jdbc is active by default.
 * 2. All application services wire concrete JDBC DAOs.
 * 3. No in-memory DAO fallback exists in JDBC mode.
 * 4. DBConnection connects to MySQL using db.properties.
 * 5. Real CRUD verification across Users, Projects, Tasks, Team Members, and Notifications.
 * 6. Persistence across application restart (ServiceFactory lifecycle reset).
 * 7. Frontend assets (JSP, HTML, JS) contain zero exposed database credentials.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndJdbcVerificationTest {

    private static DatabaseConnectionManager connManager;

    @BeforeAll
    static void init() {
        // Reset any leftover test overrides
        ServiceFactory.reset();
        System.clearProperty("dao.mode");

        connManager = DatabaseConnectionManager.getInstance();
        boolean available = connManager.isDatabaseAvailable();
        Assumptions.assumeTrue(available, "MySQL 8.x database is not reachable on localhost:3306. Skipping live E2E tests.");

        connManager.ensureSchemaUpdated();
    }

    @AfterEach
    void tearDown() {
        ServiceFactory.reset();
        System.clearProperty("dao.mode");
    }

    // =========================================================================
    // 1. Runtime Mode Verification
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Verify that dao.mode=jdbc is actually resolved and used at runtime")
    void testRuntimeDaoModeIsJdbc() {
        ServiceFactory.reset();
        ServiceFactory factory = ServiceFactory.getInstance();

        assertEquals(DaoMode.JDBC, factory.getDaoMode(),
                "Runtime DAO mode must be JDBC when db.properties specifies dao.mode=jdbc.");
    }

    // =========================================================================
    // 2. Concrete JDBC DAO Implementation Verification
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Verify that all application services use JDBC DAO implementations")
    void testServicesUseJdbcDaoImplementations() {
        ServiceFactory factory = ServiceFactory.getInstance();

        assertInstanceOf(JdbcUserDao.class, factory.getUserDao(),
                "UserDao must be an instance of JdbcUserDao in JDBC mode.");
        assertInstanceOf(JdbcProjectDao.class, factory.getProjectDao(),
                "ProjectDao must be an instance of JdbcProjectDao in JDBC mode.");
        assertInstanceOf(JdbcTaskDao.class, factory.getTaskDao(),
                "TaskDao must be an instance of JdbcTaskDao in JDBC mode.");
        assertInstanceOf(JdbcTeamMemberDao.class, factory.getTeamMemberDao(),
                "TeamMemberDao must be an instance of JdbcTeamMemberDao in JDBC mode.");
        assertInstanceOf(JdbcNotificationDao.class, factory.getNotificationDao(),
                "NotificationDao must be an instance of JdbcNotificationDao in JDBC mode.");
    }

    // =========================================================================
    // 3. No In-Memory DAO Fallback in JDBC Mode
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Verify that no in-memory DAO is used as fallback in JDBC mode")
    void testNoInMemoryDaoFallbackInJdbcMode() {
        ServiceFactory factory = ServiceFactory.getInstance();

        assertFalse(factory.getUserDao() instanceof InMemoryUserDao,
                "InMemoryUserDao must NOT be used when running in JDBC mode.");
        assertFalse(factory.getProjectDao() instanceof InMemoryProjectDao,
                "InMemoryProjectDao must NOT be used when running in JDBC mode.");
        assertFalse(factory.getTaskDao() instanceof InMemoryTaskDao,
                "InMemoryTaskDao must NOT be used when running in JDBC mode.");
        assertFalse(factory.getTeamMemberDao() instanceof InMemoryTeamMemberDao,
                "InMemoryTeamMemberDao must NOT be used when running in JDBC mode.");
        assertFalse(factory.getNotificationDao() instanceof InMemoryNotificationDao,
                "InMemoryNotificationDao must NOT be used when running in JDBC mode.");
    }

    // =========================================================================
    // 4. DBConnection Connectivity via db.properties
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Verify that DBConnection successfully connects to MySQL using db.properties")
    void testDBConnectionConnectsUsingDbProperties() throws Exception {
        assertTrue(DBConnection.isConnected(), "DBConnection.isConnected() must return true.");

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "DBConnection.getConnection() must return a non-null Connection.");
            assertFalse(conn.isClosed(), "Acquired database connection must be open and active.");
            assertTrue(conn.isValid(3), "Database connection must validate within 3 seconds.");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DATABASE(), CURRENT_USER()")) {
                assertTrue(rs.next(), "Test query on MySQL must return metadata row.");
                String dbName = rs.getString(1);
                assertNotNull(dbName);
                assertEquals("project_management", dbName.toLowerCase(),
                        "Connection must be established to the project_management schema.");
            }
        }
    }

    // =========================================================================
    // 5. End-to-End Real MySQL CRUD Verification
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Perform end-to-end CRUD verification for Users, Projects, Tasks, Team Members, and Notifications")
    void testFullRealMySqlCrudVerification() {
        ServiceFactory factory = ServiceFactory.getInstance();
        UserDao userDao = factory.getUserDao();
        ProjectDao projectDao = factory.getProjectDao();
        TaskDao taskDao = factory.getTaskDao();
        TeamMemberDao teamMemberDao = factory.getTeamMemberDao();
        NotificationDao notifDao = factory.getNotificationDao();

        long uniqueTs = System.currentTimeMillis();

        // ---------------------------------------------------------------------
        // A. USER CRUD
        // ---------------------------------------------------------------------
        String username = "e2e_user_" + uniqueTs;
        String email = "e2e_" + uniqueTs + "@company.com";
        TeamMember user = new TeamMember(null, username, "E2E Verified User", email,
                "Quality Engineering", "EU", "Java & Automation", 40);

        User registeredUser = userDao.register(user, "SecureE2EPass#2026");
        assertNotNull(registeredUser.getId(), "Registered user must receive auto-generated primary key ID.");

        // Read / Find by ID
        Optional<User> fetchedUser = userDao.findById(registeredUser.getId());
        assertTrue(fetchedUser.isPresent(), "User must be retrievable by ID from MySQL.");
        assertEquals(username, fetchedUser.get().getUsername());

        // Login check
        Optional<User> loggedInUser = userDao.login(username, "SecureE2EPass#2026");
        assertTrue(loggedInUser.isPresent(), "User must authenticate with correct password from MySQL.");

        // Update user
        registeredUser.setDepartment("Platform Core");
        assertTrue(userDao.update(registeredUser), "User update must succeed.");
        Optional<User> reFetchedUser = userDao.findById(registeredUser.getId());
        assertTrue(reFetchedUser.isPresent());
        assertEquals("Platform Core", reFetchedUser.get().getDepartment());

        // ---------------------------------------------------------------------
        // B. PROJECT CRUD
        // ---------------------------------------------------------------------
        String projectCode = "PRJ-E2E-" + (uniqueTs % 10000);
        Project project = new Project(null, projectCode, "E2E Integration Initiative",
                "Testing real database persistence end to end.", ProjectStatus.IN_PROGRESS,
                Priority.HIGH, registeredUser, LocalDate.now(), LocalDate.now().plusMonths(3),
                75000.0, 15000.0, 25, 10, 2);

        Project savedProject = projectDao.save(project);
        assertNotNull(savedProject.getId(), "Project must receive auto-generated primary key ID.");

        // Retrieve project
        Optional<Project> fetchedProject = projectDao.findById(savedProject.getId());
        assertTrue(fetchedProject.isPresent(), "Project must be retrievable by ID from MySQL.");
        assertEquals("E2E Integration Initiative", fetchedProject.get().getName());

        // Update project
        savedProject.setStatus(ProjectStatus.COMPLETED);
        savedProject.setProgressPercentage(100);
        assertTrue(projectDao.update(savedProject), "Project update must succeed.");

        Optional<Project> updatedProject = projectDao.findById(savedProject.getId());
        assertTrue(updatedProject.isPresent());
        assertEquals(ProjectStatus.COMPLETED, updatedProject.get().getStatus());

        // ---------------------------------------------------------------------
        // C. TEAM MEMBER CRUD
        // ---------------------------------------------------------------------
        // Add member
        boolean memberAdded = teamMemberDao.addTeamMember(savedProject.getId(), registeredUser.getId());
        assertTrue(memberAdded, "Team member must be successfully linked to project in team_members table.");

        // Duplicate rejection
        boolean duplicateAdded = teamMemberDao.addTeamMember(savedProject.getId(), registeredUser.getId());
        assertFalse(duplicateAdded, "Duplicate membership must be rejected by MySQL composite primary key.");

        // Retrieve project team
        Set<User> team = teamMemberDao.findProjectTeam(savedProject.getId());
        assertFalse(team.isEmpty(), "Team members for project must not be empty.");
        assertTrue(team.stream().anyMatch(u -> u.getId().equals(registeredUser.getId())),
                "Registered user must be present in project team set.");

        // ---------------------------------------------------------------------
        // D. TASK CRUD
        // ---------------------------------------------------------------------
        Task task = new Task(null, savedProject.getId(), savedProject.getName(),
                "Execute E2E DB Verification Suite", "Verify all MySQL tables and constraints.",
                TaskStatus.TODO, Priority.URGENT, registeredUser, registeredUser,
                LocalDate.now().plusDays(5), 16, 4);

        Task savedTask = taskDao.save(task);
        assertNotNull(savedTask.getId(), "Task must receive auto-generated primary key ID.");

        // Retrieve task
        Optional<Task> fetchedTask = taskDao.findById(savedTask.getId());
        assertTrue(fetchedTask.isPresent(), "Task must be retrievable by ID from MySQL.");
        assertEquals("Execute E2E DB Verification Suite", fetchedTask.get().getTitle());

        // Update task status
        savedTask.setStatus(TaskStatus.IN_PROGRESS);
        assertTrue(taskDao.update(savedTask), "Task status update must succeed.");

        // Assign task
        boolean assigned = taskDao.assignTask(savedTask.getId(), registeredUser.getId());
        assertTrue(assigned, "Task assignment to user must succeed.");

        // ---------------------------------------------------------------------
        // E. NOTIFICATIONS CRUD
        // ---------------------------------------------------------------------
        Notification notif = new Notification(null, registeredUser.getId(), "E2E Verification Completed",
                "All JDBC persistence operations validated against MySQL 8.x.",
                "success", false, LocalDateTime.now(), "Just now", "tasks");

        Notification savedNotif = notifDao.save(notif);
        assertNotNull(savedNotif.getId(), "Notification must receive auto-generated primary key ID.");

        // Retrieve notifications
        List<Notification> userNotifs = notifDao.findByUserId(registeredUser.getId());
        assertFalse(userNotifs.isEmpty(), "User notifications list must contain newly created notification.");

        // Mark as read
        assertTrue(notifDao.markAsRead(savedNotif.getId()), "Notification mark as read must succeed.");
        Optional<Notification> readNotif = notifDao.findById(savedNotif.getId());
        assertTrue(readNotif.isPresent());
        assertTrue(readNotif.get().isRead(), "Notification must be persisted as read=true in MySQL.");

        // ---------------------------------------------------------------------
        // CLEANUP
        // ---------------------------------------------------------------------
        assertTrue(notifDao.deleteById(savedNotif.getId()), "Notification cleanup deletion must succeed.");
        assertTrue(taskDao.deleteById(savedTask.getId()), "Task cleanup deletion must succeed.");
        assertTrue(teamMemberDao.removeTeamMember(savedProject.getId(), registeredUser.getId()), "Team member removal must succeed.");
        assertTrue(projectDao.deleteById(savedProject.getId()), "Project cleanup deletion must succeed.");
        assertTrue(userDao.deleteById(registeredUser.getId()), "User cleanup deletion must succeed.");
    }

    // =========================================================================
    // 6. Persistence Across Application Restart
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Verify that database changes persist in MySQL after application restart (ServiceFactory reset)")
    void testDatabaseChangesPersistAcrossApplicationRestart() {
        long uniqueTs = System.currentTimeMillis();
        String username = "restart_user_" + uniqueTs;
        String email = "restart_" + uniqueTs + "@company.com";

        // Step 1: Create user in Session 1
        ServiceFactory session1Factory = ServiceFactory.getInstance();
        UserDao session1UserDao = session1Factory.getUserDao();
        TeamMember testUser = new TeamMember(null, username, "Persistent User", email,
                "Architecture", "PU", "Distributed Systems", 50);
        User savedUser = session1UserDao.register(testUser, "PersistPassword#123");
        assertNotNull(savedUser.getId());

        Long savedUserId = savedUser.getId();

        // Step 2: Simulate application shutdown and restart by resetting ServiceFactory singleton
        ServiceFactory.reset();

        // Step 3: Re-instantiate application in Session 2
        ServiceFactory session2Factory = ServiceFactory.getInstance();
        assertEquals(DaoMode.JDBC, session2Factory.getDaoMode());
        UserDao session2UserDao = session2Factory.getUserDao();

        // Step 4: Verify that the user still exists in MySQL after restart
        Optional<User> reloadedUser = session2UserDao.findById(savedUserId);
        assertTrue(reloadedUser.isPresent(), "Entity must persist in MySQL database across application restarts.");
        assertEquals(username, reloadedUser.get().getUsername());
        assertEquals("Persistent User", reloadedUser.get().getFullName());
        assertEquals(email, reloadedUser.get().getEmail());
        assertEquals("Architecture", reloadedUser.get().getDepartment());

        // Cleanup
        session2UserDao.deleteById(savedUserId);
    }

    // =========================================================================
    // 7. Security: Verify No Database Credentials in Frontend
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Verify that no database credentials or passwords exist in frontend views or JS assets")
    void testFrontendContainsNoDatabaseCredentials() throws IOException {
        Path webappDir = Paths.get("src", "main", "webapp");
        assertTrue(Files.exists(webappDir), "Webapp directory must exist.");

        String[] sensitivePatterns = {
                "Project@1234",
                "jdbc:mysql://",
                "com.mysql.cj.jdbc.Driver"
        };

        try (Stream<Path> paths = Files.walk(webappDir)) {
            List<Path> frontendFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".jsp") || name.endsWith(".js") || name.endsWith(".html") || name.endsWith(".css");
                    })
                    .toList();

            assertFalse(frontendFiles.isEmpty(), "Frontend files must be present for inspection.");

            for (Path file : frontendFiles) {
                String content = Files.readString(file);
                for (String pattern : sensitivePatterns) {
                    assertFalse(content.contains(pattern),
                            "Sensitive pattern '" + pattern + "' must NEVER appear in frontend asset: " + file.getFileName());
                }
            }
        }
    }
}
