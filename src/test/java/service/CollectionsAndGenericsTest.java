package service;

import dao.*;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.GenericCollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Collections & Generics (Academic Requirement: 6 Marks) Test Suite")
public class CollectionsAndGenericsTest {

    private ProjectService projectService;
    private TaskService taskService;
    private UserService userService;
    private DashboardService dashboardService;
    private ProjectDao projectDao;
    private TaskDao taskDao;
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        projectDao = new InMemoryProjectDao(userDao);
        taskDao = new InMemoryTaskDao(userDao);

        projectService = new ProjectServiceImpl(projectDao);
        taskService = new TaskServiceImpl(taskDao);
        userService = new UserServiceImpl(userDao, projectDao, taskDao);
        dashboardService = new DashboardServiceImpl(projectDao, taskDao, userDao, userService);
    }

    // =========================================================================
    // 1. Generic DAO / Repository
    // =========================================================================
    @Test
    @DisplayName("1. Generic DAO: BaseDao<T, ID> and CrudOperations<T, ID> reusable methods")
    void testGenericDaoAndRepository() {
        CrudOperations<Project, Long> repo = projectDao;

        // findAll() returns List<T>
        List<Project> allProjects = repo.findAll();
        assertNotNull(allProjects);
        assertFalse(allProjects.isEmpty());

        // findById() returns Optional<T>
        Optional<Project> p1 = repo.findById(1L);
        assertTrue(p1.isPresent());
        assertEquals("PRJ-101", p1.get().getCode());

        // findAllAsMap() returns Map<ID, T>
        Map<Long, Project> map = repo.findAllAsMap();
        assertNotNull(map);
        assertEquals(allProjects.size(), map.size());
        assertTrue(map.containsKey(1L));

        // existsById() and count()
        assertTrue(repo.existsById(1L));
        assertFalse(repo.existsById(9999L));
        assertEquals(allProjects.size(), repo.count());
    }

    // =========================================================================
    // 2. List Collection Usage
    // =========================================================================
    @Test
    @DisplayName("2. List Collection: Project, Task, and User lists with project/assignee lookups")
    void testListCollectionUsage() {
        // List<Project>
        List<Project> projectList = projectService.getAllProjects();
        assertNotNull(projectList);
        assertTrue(projectList.size() >= 5);

        // List<Task>
        List<Task> taskList = taskService.getAllTasks();
        assertNotNull(taskList);
        assertTrue(taskList.size() >= 8);

        // List<Task> by project ID (both Long and primitive int overload)
        List<Task> prj1Tasks = taskService.getTasksByProjectId(1L);
        List<Task> prj1TasksInt = taskService.getTasksByProject(1);
        assertEquals(prj1Tasks.size(), prj1TasksInt.size());
        assertTrue(prj1Tasks.stream().allMatch(t -> t.getProjectId().equals(1L)));

        // List<Task> by assigned user ID
        List<Task> elenaTasks = taskService.getTasksAssignedToUser(4);
        assertNotNull(elenaTasks);
        assertFalse(elenaTasks.isEmpty());
        assertTrue(elenaTasks.stream().allMatch(t -> t.getAssignee().getId().equals(4L)));
    }

    // =========================================================================
    // 3. Set Collection & Duplicate Prevention
    // =========================================================================
    @Test
    @DisplayName("3. Set Collection: Prevent duplicate team members and enforce Set uniqueness")
    void testSetCollectionAndDuplicatePrevention() {
        Project project = projectService.getProjectById(1L);
        User david = userService.getUserById(3L);

        // David is already seeded into project 1
        int initialMemberCount = project.getTeamMembers().size();

        // Adding duplicate team member returns false and does NOT change Set size
        boolean addedDuplicate = projectService.addTeamMemberToProject(1L, david);
        assertFalse(addedDuplicate, "Duplicate team member must be rejected by Set semantics");
        assertEquals(initialMemberCount, projectService.getProjectTeamMembers(1L).size());

        // Add a new distinct team member
        User sarah = userService.getUserById(2L);
        boolean addedNew = projectService.addTeamMemberToProject(1L, sarah);
        assertTrue(addedNew, "New unique team member must be added to Set successfully");
        assertEquals(initialMemberCount + 1, projectService.getProjectTeamMembers(1L).size());

        // Remove team member
        boolean removed = projectService.removeTeamMemberFromProject(1L, 2L);
        assertTrue(removed, "Team member should be removed from Set");
        assertEquals(initialMemberCount, projectService.getProjectTeamMembers(1L).size());

        // Enterprise-wide unique contributors across all projects
        Set<User> allUniqueContributors = projectService.getAllUniqueProjectContributors();
        assertNotNull(allUniqueContributors);
        assertFalse(allUniqueContributors.isEmpty());

        // Verify Set equality contracts for User
        User userA1 = new TeamMember(99L, "coder", "Coder A", "coder@a.com", "Dev", "CA", "Junior", 40);
        User userA2 = new TeamMember(99L, "coder", "Coder A", "coder@a.com", "Dev", "CA", "Junior", 40);
        assertEquals(userA1, userA2);
        assertEquals(userA1.hashCode(), userA2.hashCode());

        Set<User> testSet = new HashSet<>();
        testSet.add(userA1);
        testSet.add(userA2);
        assertEquals(1, testSet.size(), "Set must contain exactly 1 user due to equals/hashCode");
    }

    // =========================================================================
    // 4. Map Collection Usage
    // =========================================================================
    @Test
    @DisplayName("4. Map Collection: Fast O(1) ID lookups and dynamic statistical groupings")
    void testMapCollectionOperations() {
        // Fast ID lookup via Map
        Map<Long, Project> projectMap = projectService.getProjectMap();
        assertNotNull(projectMap);
        assertTrue(projectMap.containsKey(1L));
        assertEquals("Cloud Infrastructure Modernization", projectMap.get(1L).getName());

        Map<Long, Task> taskMap = taskService.getTaskMap();
        assertNotNull(taskMap);
        assertTrue(taskMap.containsKey(1L));

        Map<Long, User> userMap = userService.getUserMap();
        assertNotNull(userMap);
        assertTrue(userMap.containsKey(1L));

        // Grouping counts: tasks by status
        Map<TaskStatus, Long> tasksByStatus = taskService.getTaskCountByStatus();
        assertNotNull(tasksByStatus);
        assertTrue(tasksByStatus.containsKey(TaskStatus.IN_PROGRESS));
        assertTrue(tasksByStatus.get(TaskStatus.IN_PROGRESS) > 0);

        // Grouping counts: tasks by priority
        Map<Priority, Long> tasksByPriority = taskService.getTaskCountByPriority();
        assertNotNull(tasksByPriority);
        assertTrue(tasksByPriority.containsKey(Priority.URGENT));

        // Projects by status map
        Map<ProjectStatus, Long> projectsByStatus = projectService.getProjectCountByStatus();
        assertNotNull(projectsByStatus);
        assertTrue(projectsByStatus.get(ProjectStatus.IN_PROGRESS) > 0);
    }

    // =========================================================================
    // 5. Sorting with Comparators
    // =========================================================================
    @Test
    @DisplayName("5. Sorting: Meaningful sorting using Java Comparators and thenComparing")
    void testSortingWithComparators() {
        // Sort tasks by deadline (due date)
        List<Task> sortedByDeadline = taskService.getTasksSortedBy("deadline");
        assertNotNull(sortedByDeadline);
        for (int i = 0; i < sortedByDeadline.size() - 1; i++) {
            LocalDate d1 = sortedByDeadline.get(i).getDueDate();
            LocalDate d2 = sortedByDeadline.get(i + 1).getDueDate();
            if (d1 != null && d2 != null) {
                assertTrue(d1.compareTo(d2) <= 0, "Tasks must be sorted chronologically by due date");
            }
        }

        // Sort tasks by priority (URGENT -> HIGH -> MEDIUM -> LOW)
        List<Task> sortedByPriority = taskService.getTasksSortedBy("priority");
        assertNotNull(sortedByPriority);
        for (int i = 0; i < sortedByPriority.size() - 1; i++) {
            Priority p1 = sortedByPriority.get(i).getPriority();
            Priority p2 = sortedByPriority.get(i + 1).getPriority();
            assertTrue(p1.compareTo(p2) >= 0, "Tasks must be sorted by descending priority");
        }

        // Sort tasks by estimated hours
        List<Task> sortedByHours = taskService.getTasksSortedBy("hours");
        assertNotNull(sortedByHours);
        for (int i = 0; i < sortedByHours.size() - 1; i++) {
            assertTrue(sortedByHours.get(i).getEstimatedHours() >= sortedByHours.get(i + 1).getEstimatedHours(),
                    "Tasks must be sorted descending by estimated hours");
        }

        // Sort projects by progress percentage
        List<Project> sortedProjects = projectService.getProjectsSortedBy("progress");
        assertNotNull(sortedProjects);
        for (int i = 0; i < sortedProjects.size() - 1; i++) {
            assertTrue(sortedProjects.get(i).getProgressPercentage() >= sortedProjects.get(i + 1).getProgressPercentage(),
                    "Projects must be sorted descending by progress percentage");
        }
    }

    // =========================================================================
    // 6. Filtering with Streams
    // =========================================================================
    @Test
    @DisplayName("6. Filtering: Stream-based filtering by status, priority, and overdue condition")
    void testFilteringWithStreams() {
        // Filter projects by status
        List<Project> completedProjects = projectService.filterProjectsByStatus(ProjectStatus.COMPLETED);
        assertNotNull(completedProjects);
        assertTrue(completedProjects.stream().allMatch(p -> p.getStatus() == ProjectStatus.COMPLETED));

        // Filter projects by priority
        List<Project> urgentProjects = projectService.filterProjectsByPriority(Priority.URGENT);
        assertNotNull(urgentProjects);
        assertTrue(urgentProjects.stream().allMatch(p -> p.getPriority() == Priority.URGENT));

        // Filter tasks by status
        List<Task> todoTasks = taskService.filterTasksByStatus(TaskStatus.TODO);
        assertNotNull(todoTasks);
        assertTrue(todoTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.TODO));

        // Filter overdue tasks
        List<Task> overdue = taskService.getOverdueTasks();
        assertNotNull(overdue);
        assertFalse(overdue.isEmpty());
        LocalDate today = LocalDate.now();
        assertTrue(overdue.stream().allMatch(t -> t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.COMPLETED),
                "Overdue tasks must have a due date before today and not be completed");
    }

    // =========================================================================
    // 7. Case-Insensitive Search
    // =========================================================================
    @Test
    @DisplayName("7. Search: Case-insensitive search across names, codes, and descriptions")
    void testCaseInsensitiveSearch() {
        // Search projects by keyword in title (case insensitive)
        List<Project> searchCloudLower = projectService.searchProjects("cloud");
        List<Project> searchCloudUpper = projectService.searchProjects("CLOUD");
        assertFalse(searchCloudLower.isEmpty());
        assertEquals(searchCloudLower.size(), searchCloudUpper.size(),
                "Search should be case-insensitive");

        // Search projects by code
        List<Project> searchCode = projectService.searchProjects("prj-102");
        assertEquals(1, searchCode.size());
        assertEquals("PRJ-102", searchCode.get(0).getCode());

        // Search tasks by title and description
        List<Task> searchTasks = taskService.searchTasks("kubernetes");
        assertFalse(searchTasks.isEmpty());
        assertTrue(searchTasks.get(0).getTitle().toLowerCase().contains("kubernetes") ||
                   searchTasks.get(0).getDescription().toLowerCase().contains("kubernetes"));
    }

    // =========================================================================
    // 8. Generic Collection Utilities (GenericCollectionUtils<T>)
    // =========================================================================
    @Test
    @DisplayName("8. Generic Utility: GenericCollectionUtils demonstrates PECS wildcards and generic algorithms")
    void testGenericCollectionUtils() {
        List<String> fruits = List.of("Apple", "Banana", "Apricot", "Blueberry", "Cherry");

        // Generic filter
        List<String> aFruits = GenericCollectionUtils.filter(fruits, s -> s.startsWith("A"));
        assertEquals(List.of("Apple", "Apricot"), aFruits);

        // Generic sort
        List<String> sortedFruits = GenericCollectionUtils.sort(fruits, Comparator.naturalOrder());
        assertEquals("Apple", sortedFruits.get(0));
        assertEquals("Cherry", sortedFruits.get(sortedFruits.size() - 1));

        // Generic countBy
        Map<Character, Long> countsByFirstLetter = GenericCollectionUtils.countBy(fruits, s -> s.charAt(0));
        assertEquals(2L, countsByFirstLetter.get('A'));
        assertEquals(2L, countsByFirstLetter.get('B'));
        assertEquals(1L, countsByFirstLetter.get('C'));

        // Generic indexBy
        Map<Integer, String> fruitsByIndex = GenericCollectionUtils.indexBy(fruits, String::length);
        assertNotNull(fruitsByIndex);

        // Generic toSet
        List<String> duplicates = List.of("X", "Y", "X", "Z");
        Set<String> uniqueSet = GenericCollectionUtils.toSet(duplicates);
        assertEquals(3, uniqueSet.size());
        assertTrue(uniqueSet.containsAll(List.of("X", "Y", "Z")));
    }

    // =========================================================================
    // 9. Thread-Safe Collections
    // =========================================================================
    @Test
    @DisplayName("9. Thread-Safe Collections: ConcurrentHashMap & CopyOnWriteArraySet under concurrent access")
    void testThreadSafeCollections() throws InterruptedException, ExecutionException {
        Project project = projectService.getProjectById(1L);

        // Concurrent additions to CopyOnWriteArraySet must not throw ConcurrentModificationException
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final long userId = 100 + i;
            tasks.add(() -> {
                User tempUser = new TeamMember(userId, "temp" + userId, "Temp " + userId,
                        "temp" + userId + "@corp.com", "Dev", "T" + userId, "QA", 20);
                return project.addTeamMember(tempUser);
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        for (Future<Boolean> f : futures) {
            assertTrue(f.get(), "Each unique thread-safe addition must succeed");
        }

        // Clean up added temp members
        for (int i = 0; i < threadCount; i++) {
            project.removeTeamMemberById(100L + i);
        }
    }

    // =========================================================================
    // 10. Dashboard & Report Statistics Integration
    // =========================================================================
    @Test
    @DisplayName("10. Dashboard Integration: Dynamic aggregation of Map, Set, and List metrics")
    void testDashboardAndReportIntegration() {
        DashboardStats stats = dashboardService.getDashboardStatistics();

        assertNotNull(stats);
        assertTrue(stats.getTotalProjects() >= 5);
        assertTrue(stats.getTotalTasks() >= 8);

        // Map metrics populated dynamically
        Map<TaskStatus, Long> tasksByStatus = stats.getTasksByStatus();
        assertNotNull(tasksByStatus, "tasksByStatus map must be populated");
        assertFalse(tasksByStatus.isEmpty());
        assertEquals(stats.getTotalTasks(), tasksByStatus.values().stream().mapToLong(Long::longValue).sum(),
                "Sum of tasksByStatus must match totalTasks count");

        Map<Priority, Long> tasksByPriority = stats.getTasksByPriority();
        assertNotNull(tasksByPriority, "tasksByPriority map must be populated");
        assertFalse(tasksByPriority.isEmpty());

        Map<ProjectStatus, Long> projectsByStatus = stats.getProjectsByStatus();
        assertNotNull(projectsByStatus, "projectsByStatus map must be populated");
        assertEquals(stats.getTotalProjects(), projectsByStatus.values().stream().mapToLong(Long::longValue).sum(),
                "Sum of projectsByStatus must match totalProjects count");

        // Set metrics populated dynamically
        Set<User> uniqueContributors = stats.getUniqueContributors();
        assertNotNull(uniqueContributors, "uniqueContributors set must be populated");
        assertFalse(uniqueContributors.isEmpty());
    }
}
