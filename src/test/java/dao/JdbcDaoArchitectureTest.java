package dao;

import exception.DatabaseOperationException;
import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ServiceFactory;
import service.ServiceFactory.DaoMode;
import util.DatabaseConnectionManager;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ==============================================================================
 * Test Suite: JDBC DAO Architecture, Resource Management & Transactions
 * ==============================================================================
 * 
 * Verifies:
 * 1. DatabaseConnectionManager configuration, connection lifecycle, and resource closure.
 * 2. ServiceFactory dynamic switching between In-Memory DAOs and JDBC DAOs.
 * 3. PreparedStatement SQL safety and parameter binding.
 * 4. ACID Transaction management (setAutoCommit(false), commit, and rollback).
 * 5. Exception translation: technical SQLExceptions wrapped in DatabaseOperationException.
 */
class JdbcDaoArchitectureTest {

    @BeforeEach
    void setUp() {
        ServiceFactory.setDaoMode(DaoMode.MEMORY);
    }

    @AfterEach
    void tearDown() {
        ServiceFactory.setDaoMode(DaoMode.MEMORY);
    }

    // =========================================================================
    // 1. DatabaseConnectionManager Tests
    // =========================================================================

    @Test
    @DisplayName("DatabaseConnectionManager should load configuration and provide singleton instance")
    void testConnectionManagerConfiguration() {
        DatabaseConnectionManager mgr = DatabaseConnectionManager.getInstance();
        assertNotNull(mgr, "DatabaseConnectionManager instance must not be null.");

        assertNotNull(mgr.getUrl(), "JDBC URL must be configured.");
        assertTrue(mgr.getUrl().contains("project_management"), "URL should target project_management database.");
        assertNotNull(mgr.getUsername(), "Username must be configured.");
        assertNotNull(mgr.getPassword(), "Password must be configured.");
        assertEquals("com.mysql.cj.jdbc.Driver", mgr.getDriverClass(), "Driver should be MySQL connector driver.");
    }

    @Test
    @DisplayName("DatabaseConnectionManager.closeQuietly should safely close resources without throwing")
    void testCloseQuietly() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable dummyResource = () -> closed.set(true);

        assertDoesNotThrow(() -> DatabaseConnectionManager.closeQuietly(dummyResource, null));
        assertTrue(closed.get(), "Resource should be closed by closeQuietly.");

        // Throwing resource should not bubble up exceptions
        AutoCloseable throwingResource = () -> {
            throw new SQLException("Test closing error");
        };
        assertDoesNotThrow(() -> DatabaseConnectionManager.closeQuietly(throwingResource));
    }

    // =========================================================================
    // 2. ServiceFactory Switching & In-Memory Compatibility Tests
    // =========================================================================

    @Test
    @DisplayName("ServiceFactory should seamlessly toggle between In-Memory and JDBC DAO modes")
    void testServiceFactoryModeSwitching() {
        // Initial in-memory mode
        ServiceFactory.setDaoMode(DaoMode.MEMORY);
        ServiceFactory factory = ServiceFactory.getInstance();
        assertEquals(DaoMode.MEMORY, factory.getDaoMode());
        assertTrue(factory.getUserDao() instanceof InMemoryUserDao);
        assertTrue(factory.getProjectDao() instanceof InMemoryProjectDao);
        assertTrue(factory.getTaskDao() instanceof InMemoryTaskDao);
        assertTrue(factory.getNotificationDao() instanceof InMemoryNotificationDao);
        assertTrue(factory.getTeamMemberDao() instanceof InMemoryTeamMemberDao);

        // Switch to JDBC mode
        ServiceFactory.setDaoMode(DaoMode.JDBC);
        ServiceFactory jdbcFactory = ServiceFactory.getInstance();
        assertEquals(DaoMode.JDBC, jdbcFactory.getDaoMode());
        assertTrue(jdbcFactory.getUserDao() instanceof JdbcUserDao);
        assertTrue(jdbcFactory.getProjectDao() instanceof JdbcProjectDao);
        assertTrue(jdbcFactory.getTaskDao() instanceof JdbcTaskDao);
        assertTrue(jdbcFactory.getNotificationDao() instanceof JdbcNotificationDao);
        assertTrue(jdbcFactory.getTeamMemberDao() instanceof JdbcTeamMemberDao);

        // Switch back to in-memory mode
        ServiceFactory.setDaoMode(DaoMode.MEMORY);
        assertEquals(DaoMode.MEMORY, ServiceFactory.getInstance().getDaoMode());
    }

    // =========================================================================
    // 3. PreparedStatement SQL Safety & Parameter Binding Tests
    // =========================================================================

    @Test
    @DisplayName("JdbcUserDao should prepare statement and bind parameters safely without SQL concatenation")
    void testJdbcUserDaoPreparedStatementSafety() throws Exception {
        List<String> executedSql = new ArrayList<>();
        Map<Integer, Object> boundParams = new HashMap<>();

        Connection mockConn = createMockConnection(executedSql, boundParams, 1L);
        DatabaseConnectionManager customMgr = createTestConnectionManager(mockConn);
        JdbcUserDao userDao = new JdbcUserDao(customMgr);

        Admin testAdmin = new Admin(null, "admin.test", "Admin Tester", "admin.test@example.com",
                "Infrastructure", "AT", "SYSTEM_ADMIN", true);

        User saved = userDao.save(testAdmin);
        assertNotNull(saved);
        assertEquals(1L, saved.getId());

        assertFalse(executedSql.isEmpty(), "At least one SQL query should be executed.");
        String insertSql = executedSql.get(0);
        assertTrue(insertSql.startsWith("INSERT INTO users"), "Should execute INSERT query.");
        assertTrue(insertSql.contains("?"), "SQL must use parameterized placeholders (?) rather than string concatenation.");

        // Verify bound parameters
        assertEquals("admin.test", boundParams.get(1));
        assertEquals("Admin Tester", boundParams.get(2));
        assertEquals("admin.test@example.com", boundParams.get(3));
        assertEquals("ADMIN", boundParams.get(4));
    }

    // =========================================================================
    // 4. ACID Transaction & Rollback Tests
    // =========================================================================

    @Test
    @DisplayName("JdbcProjectDao.createProjectWithTeam should execute in a transaction and commit on success")
    void testTransactionCommitOnSuccess() throws Exception {
        AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
        AtomicBoolean committed = new AtomicBoolean(false);
        AtomicBoolean rolledBack = new AtomicBoolean(false);
        List<String> executedSql = new ArrayList<>();

        Connection mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setAutoCommit".equals(name)) {
                        boolean ac = (Boolean) args[0];
                        if (!ac) autoCommitDisabled.set(true);
                        return null;
                    }
                    if ("commit".equals(name)) {
                        committed.set(true);
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        rolledBack.set(true);
                        return null;
                    }
                    if ("close".equals(name) || "isClosed".equals(name)) {
                        return null;
                    }
                    if ("prepareStatement".equals(name)) {
                        executedSql.add((String) args[0]);
                        return createMockPreparedStatement(new HashMap<>(), 99L);
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        DatabaseConnectionManager customMgr = createTestConnectionManager(mockConn);
        JdbcTeamMemberDao teamDao = new JdbcTeamMemberDao(customMgr);
        JdbcProjectDao projectDao = new JdbcProjectDao(customMgr, teamDao);

        Project project = new Project();
        project.setName("Mission Critical Initiative");
        project.setCode("PRJ-MC");

        Set<Long> memberIds = new HashSet<>(Arrays.asList(101L, 102L));
        Project result = projectDao.createProjectWithTeam(project, memberIds);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertTrue(autoCommitDisabled.get(), "Transaction must disable auto-commit (setAutoCommit(false)).");
        assertTrue(committed.get(), "Transaction must call commit() upon successful completion.");
        assertFalse(rolledBack.get(), "Transaction must NOT call rollback() on success.");
    }

    @Test
    @DisplayName("JdbcProjectDao.createProjectWithTeam should roll back when a database failure occurs")
    void testTransactionRollbackOnFailure() throws Exception {
        AtomicBoolean autoCommitDisabled = new AtomicBoolean(false);
        AtomicBoolean committed = new AtomicBoolean(false);
        AtomicBoolean rolledBack = new AtomicBoolean(false);

        Connection mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setAutoCommit".equals(name)) {
                        autoCommitDisabled.set(true);
                        return null;
                    }
                    if ("commit".equals(name)) {
                        committed.set(true);
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        rolledBack.set(true);
                        return null;
                    }
                    if ("close".equals(name) || "isClosed".equals(name)) {
                        return null;
                    }
                    if ("prepareStatement".equals(name)) {
                        String sql = (String) args[0];
                        if (sql.contains("team_members")) {
                            // Simulate fatal DB failure when inserting team members
                            throw new SQLException("Simulated disk I/O failure during batch insert", "40001");
                        }
                        return createMockPreparedStatement(new HashMap<>(), 88L);
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        DatabaseConnectionManager customMgr = createTestConnectionManager(mockConn);
        JdbcProjectDao projectDao = new JdbcProjectDao(customMgr, new JdbcTeamMemberDao(customMgr));

        Project project = new Project();
        project.setName("Failing Project");

        DatabaseOperationException ex = assertThrows(DatabaseOperationException.class, () ->
                projectDao.createProjectWithTeam(project, Collections.singleton(55L))
        );

        assertTrue(ex.getMessage().contains("Transaction failed"), "Exception should communicate transaction failure.");
        assertTrue(autoCommitDisabled.get(), "Auto-commit should have been disabled.");
        assertTrue(rolledBack.get(), "Transaction rollback() must be executed upon failure.");
        assertFalse(committed.get(), "Commit must NOT be called after failure.");
    }

    // =========================================================================
    // 5. Exception Translation Tests
    // =========================================================================

    @Test
    @DisplayName("JDBC DAOs must translate raw SQLExceptions into application DatabaseOperationException")
    void testExceptionTranslation() throws Exception {
        Connection throwingConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        throw new SQLException("Simulated connection timeout", "08001");
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        DatabaseConnectionManager customMgr = createTestConnectionManager(throwingConn);

        JdbcUserDao userDao = new JdbcUserDao(customMgr);
        JdbcProjectDao projectDao = new JdbcProjectDao(customMgr);
        JdbcTaskDao taskDao = new JdbcTaskDao(customMgr);
        JdbcNotificationDao notifDao = new JdbcNotificationDao(customMgr);

        assertThrows(DatabaseOperationException.class, () -> userDao.findById(1L));
        assertThrows(DatabaseOperationException.class, () -> projectDao.findAll());
        assertThrows(DatabaseOperationException.class, () -> taskDao.findByProjectId(1L));
        assertThrows(DatabaseOperationException.class, () -> notifDao.findByUserId(1L));
    }

    // =========================================================================
    // 6. Centralized DBConnection & Full JDBC Operation Tests
    // =========================================================================

    @Test
    @DisplayName("DBConnection utility should provide safe closing and connection probing")
    void testDBConnectionUtility() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable resource = () -> closed.set(true);

        assertDoesNotThrow(() -> util.DBConnection.close(resource, null));
        assertTrue(closed.get(), "DBConnection.close should close the provided AutoCloseable resource.");

        // isConnected returns boolean without throwing
        assertDoesNotThrow(util.DBConnection::isConnected);
    }

    @Test
    @DisplayName("JdbcUserDao login and register should bind parameters with PreparedStatement")
    void testJdbcUserDaoLoginAndRegister() throws Exception {
        List<String> executedSql = new ArrayList<>();
        Map<Integer, Object> boundParams = new HashMap<>();

        Connection mockConn = createMockConnection(executedSql, boundParams, 55L);
        DatabaseConnectionManager customMgr = createTestConnectionManager(mockConn);
        JdbcUserDao userDao = new JdbcUserDao(customMgr);

        // Test login parameter binding
        userDao.login("admin.test", "secretPwd123");
        assertFalse(executedSql.isEmpty());
        String loginSql = executedSql.get(0);
        assertTrue(loginSql.contains("SELECT * FROM users"));
        assertTrue(loginSql.contains("password = ?"));
        assertEquals("admin.test", boundParams.get(1));
        assertEquals("admin.test", boundParams.get(2));
        assertEquals("secretPwd123", boundParams.get(3));

        // Test register
        executedSql.clear();
        boundParams.clear();
        TeamMember newMember = new TeamMember(null, "new.user", "New User", "new.user@company.com",
                "Frontend", "NU", "React", 30);
        User registered = userDao.register(newMember, "newPassword123");
        assertNotNull(registered);
        assertEquals(55L, registered.getId());
        assertEquals("newPassword123", registered.getPassword());
        assertFalse(executedSql.isEmpty());
        String regSql = executedSql.get(0);
        assertTrue(regSql.contains("INSERT INTO users"));
        assertEquals("newPassword123", boundParams.get(8));
    }

    @Test
    @DisplayName("JdbcTaskDao assignTask should bind taskId and assigneeId via PreparedStatement")
    void testJdbcTaskDaoAssignTask() throws Exception {
        List<String> executedSql = new ArrayList<>();
        Map<Integer, Object> boundParams = new HashMap<>();

        Connection mockConn = createMockConnection(executedSql, boundParams, 1L);
        DatabaseConnectionManager customMgr = createTestConnectionManager(mockConn);
        JdbcTaskDao taskDao = new JdbcTaskDao(customMgr);

        boolean assigned = taskDao.assignTask(10L, 4L);
        assertTrue(assigned);
        assertFalse(executedSql.isEmpty());
        String assignSql = executedSql.get(0);
        assertTrue(assignSql.contains("UPDATE tasks SET assignee_id = ? WHERE id = ?"));
        assertEquals(4L, boundParams.get(1));
        assertEquals(10L, boundParams.get(2));
    }

    @Test
    @DisplayName("Service layer operations should coordinate cleanly with DAO contracts")
    void testServiceLayerOperations() {
        ServiceFactory factory = ServiceFactory.getInstance();
        service.UserService userService = factory.getUserService();
        service.TaskService taskService = factory.getTaskService();
        service.ProjectService projectService = factory.getProjectService();
        service.NotificationService notifService = factory.getNotificationService();

        // 1. User Service: register, login, find, update, delete
        User registered = userService.registerUser(new TeamMember(null, "svc.user", "Service User",
                "svc.user@company.com", "QA", "SU", "Selenium", 35), "mySecurePass");
        assertNotNull(registered.getId());

        User loggedIn = userService.loginUser("svc.user", "mySecurePass");
        assertNotNull(loggedIn);
        assertEquals("svc.user", loggedIn.getUsername());

        User fetched = userService.getUserById(registered.getId());
        assertEquals("Service User", fetched.getFullName());

        registered.setDepartment("Security");
        boolean updated = userService.updateUser(registered);
        assertTrue(updated);

        boolean deleted = userService.deleteUser(registered.getId());
        assertTrue(deleted);

        // 2. Task Service: assignTask
        List<Task> allTasks = taskService.getAllTasks();
        if (!allTasks.isEmpty()) {
            Task t = allTasks.get(0);
            boolean assigned = taskService.assignTask(t.getId(), 2L);
            assertTrue(assigned);
        }

        // 3. Project Service: add and remove team member
        List<Project> projects = projectService.getAllProjects();
        if (!projects.isEmpty()) {
            Project p = projects.get(0);
            boolean memberAdded = projectService.addTeamMemberToProject(p.getId(), 2L);
            assertNotNull(projectService.getProjectTeamMembers(p.getId()));
        }

        // 4. Notification Service: create and getAll
        Notification notif = new Notification(null, 1L, "System Test", "Service operation test",
                "info", false, java.time.LocalDateTime.now(), "Just now", "projects");
        Notification savedNotif = notifService.createNotification(notif);
        assertNotNull(savedNotif.getId());

        List<Notification> allNotifs = notifService.getAllNotifications();
        assertFalse(allNotifs.isEmpty());
    }

    // =========================================================================
    // Test Harness Helper Methods (Dynamic Proxies for pure JDBC simulation)
    // =========================================================================

    private Connection createMockConnection(List<String> sqlSink, Map<Integer, Object> paramSink, long generatedKeyId) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName())) {
                        String sql = (String) args[0];
                        sqlSink.add(sql);
                        return createMockPreparedStatement(paramSink, generatedKeyId);
                    }
                    if ("close".equals(method.getName()) || "isClosed".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private PreparedStatement createMockPreparedStatement(Map<Integer, Object> paramSink, long generatedKeyId) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("set") && args != null && args.length >= 2) {
                        int index = (Integer) args[0];
                        Object val = args[1];
                        paramSink.put(index, val);
                        return null;
                    }
                    if ("executeUpdate".equals(name)) {
                        return 1;
                    }
                    if ("executeBatch".equals(name)) {
                        return new int[]{1};
                    }
                    if ("getGeneratedKeys".equals(name)) {
                        return createMockGeneratedKeysResultSet(generatedKeyId);
                    }
                    if ("executeQuery".equals(name)) {
                        return createMockEmptyResultSet();
                    }
                    if ("close".equals(name) || "isClosed".equals(name)) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ResultSet createMockGeneratedKeysResultSet(long generatedKeyId) {
        AtomicInteger cursor = new AtomicInteger(0);
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        return cursor.getAndIncrement() == 0;
                    }
                    if ("getLong".equals(name)) {
                        return generatedKeyId;
                    }
                    if ("getObject".equals(name)) {
                        return generatedKeyId;
                    }
                    if ("close".equals(name)) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ResultSet createMockEmptyResultSet() {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("next".equals(method.getName())) {
                        return false;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private DatabaseConnectionManager createTestConnectionManager(Connection connection) {
        return new DatabaseConnectionManager() {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public boolean isDatabaseAvailable() {
                return true;
            }
        };
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == double.class) return 0.0;
        if (returnType == float.class) return 0.0f;
        return null;
    }
}
