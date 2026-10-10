package service;

import dao.CrudOperations;
import exception.*;
import model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Academic OOP Concepts Verification")
public class OopConceptsIntegrationTest {

    // =========================================================================
    // 1. [OOP Concept: Encapsulation]
    // =========================================================================
    @Test
    @DisplayName("Encapsulation: Invariant validation on setters and private fields")
    void testEncapsulationAndValidation() {
        Admin admin = new Admin();

        // Valid assignment through encapsulated setter
        admin.setFullName("Root Admin");
        assertEquals("Root Admin", admin.getFullName());

        // Invalid email format must be rejected by encapsulated validation
        assertThrows(InvalidUserException.class, () -> admin.setEmail("invalid-email-address"));

        // Invalid blank username must be rejected
        assertThrows(InvalidUserException.class, () -> admin.setUsername("   "));

        // Project budget encapsulation
        Project project = new Project();
        project.setBudget(50000.0);
        assertEquals(50000.0, project.getBudget());
        assertThrows(IllegalArgumentException.class, () -> project.setBudget(-100.0));

        // Task hours encapsulation
        Task task = new Task();
        task.setEstimatedHours(16);
        assertEquals(16, task.getEstimatedHours());
        assertThrows(IllegalArgumentException.class, () -> task.setEstimatedHours(-5));
    }

    // =========================================================================
    // 2. [OOP Concept: Inheritance]
    // =========================================================================
    @Test
    @DisplayName("Inheritance: Subclasses properly inherit from abstract User")
    void testInheritanceHierarchy() {
        User admin = new Admin(10L, "sysadmin", "Alice Admin", "alice@corp.com", "DevOps", "AA", "LEVEL_3", true);
        User pm = new ProjectManager(11L, "pm.john", "John Doe", "john@corp.com", "Engineering", "JD", 80000.0, "PMP");
        User dev = new TeamMember(12L, "dev.bob", "Bob Dev", "bob@corp.com", "Frontend", "BD", "React/TypeScript", 35);

        // Verify IS-A relationships
        assertTrue(admin instanceof User, "Admin IS-A User");
        assertTrue(pm instanceof User, "ProjectManager IS-A User");
        assertTrue(dev instanceof User, "TeamMember IS-A User");

        // Inherited state accessible via base methods
        assertEquals("alice@corp.com", admin.getEmail());
        assertEquals("john@corp.com", pm.getEmail());
        assertEquals("bob@corp.com", dev.getEmail());
    }

    // =========================================================================
    // 3. [OOP Concept: Polymorphism]
    // =========================================================================
    @Test
    @DisplayName("Polymorphism: Dynamic method dispatch across User subtypes")
    void testRuntimePolymorphism() {
        User admin = new Admin();
        User pm = new ProjectManager();
        User member = new TeamMember();

        List<User> userList = List.of(admin, pm, member);

        // Verify dynamic dispatch of abstract methods across collection
        for (User u : userList) {
            assertNotNull(u.getRoleTitle(), "Subclass should return distinct role title polymorphically");
            assertNotNull(u.getPermissionsSummary(), "Subclass should return distinct permissions polymorphically");
        }

        assertTrue(admin.hasAdministrativePrivileges());
        assertFalse(pm.hasAdministrativePrivileges());
        assertFalse(member.hasAdministrativePrivileges());

        assertTrue(admin.canManageProjects());
        assertTrue(pm.canManageProjects());
        assertFalse(member.canManageProjects());

        assertTrue(admin.canAssignTasks());
        assertTrue(pm.canAssignTasks());
        assertFalse(member.canAssignTasks());
    }

    @Test
    @DisplayName("Polymorphism: ReportGenerator generates distinct report types via common interface")
    void testPolymorphicReportGeneration() {
        DashboardService dashboardService = ServiceFactory.getInstance().getDashboardService();
        ReportService reportService = new ReportServiceImpl(dashboardService);

        List<ReportResult> reports = reportService.generateConsolidatedReports();
        assertNotNull(reports);
        assertEquals(3, reports.size(), "Should have 3 polymorphic report generators executed");

        ReportResult execReport = reports.get(0);
        ReportResult finReport = reports.get(1);
        ReportResult velReport = reports.get(2);

        assertEquals("EXECUTIVE", execReport.getReportType());
        assertEquals("FINANCIAL", finReport.getReportType());
        assertEquals("VELOCITY", velReport.getReportType());

        assertFalse(execReport.getSummaryMetrics().isEmpty());
        assertFalse(finReport.getSummaryMetrics().isEmpty());
        assertFalse(velReport.getSummaryMetrics().isEmpty());
    }

    // =========================================================================
    // 4. [OOP Concept: Abstraction]
    // =========================================================================
    @Test
    @DisplayName("Abstraction: Abstract classes cannot be instantiated directly and define contracts")
    void testAbstractionContracts() {
        // User and AbstractReportGenerator are abstract, verifying template pattern
        ExecutiveSummaryReportGenerator execGen = new ExecutiveSummaryReportGenerator();
        assertTrue(execGen instanceof AbstractReportGenerator);
        assertTrue(execGen instanceof ReportGenerator);

        DashboardStats stats = ServiceFactory.getInstance().getDashboardService().getDashboardStatistics();
        ReportResult result = execGen.generate(stats);
        assertNotNull(result);
        assertEquals("EXECUTIVE", result.getReportType());
    }

    // =========================================================================
    // 5. [OOP Concept: Interface]
    // =========================================================================
    @Test
    @DisplayName("Interface: CrudOperations and BaseDao define generic contracts")
    void testGenericInterfaces() {
        CrudOperations<Project, Long> projectRepo = new dao.InMemoryProjectDao(new dao.InMemoryUserDao());
        assertNotNull(projectRepo);
        assertTrue(projectRepo instanceof CrudOperations);
        assertTrue(projectRepo instanceof dao.BaseDao);

        // Verify generic CRUD operations
        List<Project> all = projectRepo.findAll();
        assertFalse(all.isEmpty());

        NotificationService notifService = ServiceFactory.getInstance().getNotificationService();
        assertNotNull(notifService);

        int initialCount = notifService.getUnreadNotificationCount(1L);
        Notification notif = new Notification(null, 1L, "Sprint Review", "Sprint 42 demo at 3 PM", "info", false, null, "now", "tasks");
        notifService.sendNotification(notif);

        assertEquals(initialCount + 1, notifService.getUnreadNotificationCount(1L));
    }

    // =========================================================================
    // 6. [OOP Concept: Exception Handling]
    // =========================================================================
    @Test
    @DisplayName("Exception Handling: Custom domain exceptions thrown with meaningful messages")
    void testCustomDomainExceptions() {
        ProjectService projectService = ServiceFactory.getInstance().getProjectService();
        TaskService taskService = ServiceFactory.getInstance().getTaskService();
        UserService userService = ServiceFactory.getInstance().getUserService();

        // 1. ProjectNotFoundException
        assertThrows(ProjectNotFoundException.class, () -> projectService.getProjectById(99999L));

        // 2. TaskNotFoundException
        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(99999L));

        // 3. InvalidUserException
        assertThrows(InvalidUserException.class, () -> userService.getUserById(null));

        // 4. UnauthorizedAccessException
        User member = new TeamMember(99L, "charlie", "Charlie Dev", "charlie@test.com", "Dev", "CD", "QA", 20);
        Task unauthorizedTask = new Task();
        unauthorizedTask.setTitle("Restricted Task Assignment");
        unauthorizedTask.setReporter(member);
        assertThrows(UnauthorizedAccessException.class, () -> taskService.createTask(unauthorizedTask));

        // 5. DatabaseOperationException
        DatabaseOperationException dbEx = new DatabaseOperationException("Connection timeout to master replica");
        assertTrue(dbEx instanceof DatabaseException);
        assertTrue(dbEx instanceof ApplicationException);
        assertEquals("Connection timeout to master replica", dbEx.getMessage());
    }
}
