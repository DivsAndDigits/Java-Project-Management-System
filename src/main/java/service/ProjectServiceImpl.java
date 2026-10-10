package service;

import dao.ProjectDao;
import exception.ProjectNotFoundException;
import exception.UnauthorizedAccessException;
import exception.ValidationException;
import model.Priority;
import model.Project;
import model.ProjectStatus;
import model.User;
import util.GenericCollectionUtils;
import util.ValidationUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [OOP Concept: Polymorphism]
 * [OOP Concept: Exception Handling]
 * [Collections: List, Set, Map, Generics, Sorting, Filtering]
 * Implementation of ProjectService coordinating business validations, DAO access,
 * and collection-based domain algorithms.
 */
public class ProjectServiceImpl implements ProjectService {

    private final ProjectDao projectDao;
    private final dao.TeamMemberDao teamMemberDao;
    private final dao.TaskDao taskDao;
    private NotificationService notificationService;

    public ProjectServiceImpl(ProjectDao projectDao) {
        this(projectDao, null, null, null);
    }

    public ProjectServiceImpl(ProjectDao projectDao, dao.TeamMemberDao teamMemberDao) {
        this(projectDao, teamMemberDao, null, null);
    }

    public ProjectServiceImpl(ProjectDao projectDao, dao.TeamMemberDao teamMemberDao, dao.TaskDao taskDao) {
        this(projectDao, teamMemberDao, taskDao, null);
    }

    public ProjectServiceImpl(ProjectDao projectDao, dao.TeamMemberDao teamMemberDao, dao.TaskDao taskDao, NotificationService notificationService) {
        this.projectDao = projectDao;
        this.teamMemberDao = teamMemberDao;
        this.taskDao = taskDao;
        this.notificationService = notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // =========================================================================
    // [Collections: List]
    // =========================================================================

    @Override
    public List<Project> getAllProjects() {
        List<Project> list = projectDao.findAll();
        if (taskDao != null) {
            for (Project p : list) {
                syncMetrics(p);
            }
        }
        return list;
    }

    @Override
    public Project getProjectById(Long id) {
        if (id == null) {
            // [OOP Concept: Exception Handling]
            throw new ValidationException("Project ID cannot be null.");
        }
        Project project = projectDao.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        if (taskDao != null) {
            syncMetrics(project);
        }
        return project;
    }

    private void syncMetrics(Project p) {
        if (p != null && p.getId() != null && taskDao != null) {
            List<model.Task> tasks = taskDao.findByProjectId(p.getId());
            int total = tasks.size();
            int completed = (int) tasks.stream().filter(t -> t.getStatus() == model.TaskStatus.COMPLETED).count();
            p.updateTaskMetrics(total, completed);
        }
    }

    @Override
    public Project createProject(Project project) {
        if (project == null) {
            // [OOP Concept: Exception Handling]
            throw new ValidationException("Project cannot be null.");
        }
        if (ValidationUtils.isBlank(project.getName())) {
            throw new ValidationException("Project name is required.");
        }
        if (!ValidationUtils.isValidLength(project.getName(), 3, 150)) {
            throw new ValidationException("Project name must be between 3 and 150 characters.");
        }
        if (ValidationUtils.isBlank(project.getCode())) {
            project.setCode("PRJ-" + (System.currentTimeMillis() % 1000000));
        }
        if (project.getStartDate() == null) {
            project.setStartDate(java.time.LocalDate.now());
        }
        if (project.getEndDate() != null && !ValidationUtils.isValidDateRange(project.getStartDate(), project.getEndDate())) {
            throw new ValidationException("Project end date cannot be earlier than start date.");
        }
        if (project.getBudget() < 0) {
            throw new ValidationException("Project budget cannot be negative.");
        }

        // =====================================================================
        // [OOP Concept: Polymorphism]
        // Runtime polymorphic call to manager.canManageProjects().
        // Concrete subtype (Admin/ProjectManager/TeamMember) decides authorization dynamically.
        // =====================================================================
        if (project.getManager() != null && !project.getManager().canManageProjects()) {
            // [OOP Concept: Exception Handling]
            throw new UnauthorizedAccessException(project.getManager().getUsername(),
                    "Creating or managing projects is restricted to Project Managers and Administrators.");
        }

        Project saved = projectDao.save(project);

        // Notification: Project created event
        if (notificationService != null && saved != null) {
            Long recipientId = saved.getManagerId() != null ? saved.getManagerId() :
                    (saved.getManager() != null && saved.getManager().getId() != null ? saved.getManager().getId() : 1L);
            model.Notification notif = new model.Notification(
                    null,
                    recipientId,
                    "New Project Created: " + saved.getName(),
                    "Project '" + saved.getName() + "' (" + saved.getCode() + ") has been successfully created.",
                    "info",
                    false,
                    java.time.LocalDateTime.now(),
                    "Just now",
                    "project-details?id=" + saved.getId()
            );
            notificationService.sendNotification(notif);
        }

        return saved;
    }

    @Override
    public boolean updateProject(Project project) {
        if (project == null || project.getId() == null) {
            // [OOP Concept: Exception Handling]
            throw new ValidationException("Cannot update project without valid ID.");
        }
        if (ValidationUtils.isBlank(project.getName())) {
            throw new ValidationException("Project name is required.");
        }
        if (!ValidationUtils.isValidLength(project.getName(), 3, 150)) {
            throw new ValidationException("Project name must be between 3 and 150 characters.");
        }
        if (project.getStartDate() != null && project.getEndDate() != null &&
                !ValidationUtils.isValidDateRange(project.getStartDate(), project.getEndDate())) {
            throw new ValidationException("Project end date cannot be earlier than start date.");
        }

        // Verify project exists
        Project previous = getProjectById(project.getId());

        // [OOP Concept: Polymorphism]
        if (project.getManager() != null && !project.getManager().canManageProjects()) {
            throw new UnauthorizedAccessException(project.getManager().getUsername(), "Update project authorization denied.");
        }

        boolean updated = projectDao.update(project);

        // Notification: Project completed event
        if (updated && project.getStatus() == ProjectStatus.COMPLETED && notificationService != null) {
            Long recipientId = project.getManagerId() != null ? project.getManagerId() :
                    (project.getManager() != null && project.getManager().getId() != null ? project.getManager().getId() : 1L);
            model.Notification notif = new model.Notification(
                    null,
                    recipientId,
                    "Project Completed: " + project.getName(),
                    "Project '" + project.getName() + "' has reached 100% completion and is marked as COMPLETED.",
                    "success",
                    false,
                    java.time.LocalDateTime.now(),
                    "Just now",
                    "project-details?id=" + project.getId()
            );
            notificationService.sendNotification(notif);
        }

        return updated;
    }

    @Override
    public boolean deleteProject(Long id) {
        if (id == null) {
            throw new ValidationException("Project ID cannot be null.");
        }
        // Verify existence before deletion
        getProjectById(id);
        return projectDao.deleteById(id);
    }

    @Override
    public List<Project> getProjectsByStatus(ProjectStatus status) {
        if (status == null) {
            return getAllProjects();
        }
        return projectDao.findByStatus(status);
    }

    @Override
    public List<Project> searchProjects(String query) {
        if (ValidationUtils.isBlank(query)) {
            return getAllProjects();
        }
        // Case-insensitive search using generic utility
        String cleanQuery = query.trim().toLowerCase();
        return GenericCollectionUtils.filter(getAllProjects(), p ->
                (p.getName() != null && p.getName().toLowerCase().contains(cleanQuery)) ||
                (p.getCode() != null && p.getCode().toLowerCase().contains(cleanQuery)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(cleanQuery))
        );
    }

    @Override
    public List<Project> getRecentProjects(int limit) {
        return projectDao.findRecent(limit);
    }

    // =========================================================================
    // [Collections: Set]
    // Prevents duplicate team member membership using mathematical Set semantics.
    // =========================================================================

    @Override
    public boolean addTeamMemberToProject(Long projectId, User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null when assigning to project.");
        }
        Project project = getProjectById(projectId);
        // Set.add() returns false if user is already in the project's team Set
        boolean added = project.addTeamMember(user);
        if (teamMemberDao != null && user.getId() != null) {
            teamMemberDao.addTeamMember(projectId, user.getId());
        }
        if (added) {
            projectDao.update(project);
        }
        return added;
    }

    @Override
    public boolean addTeamMemberToProject(Long projectId, Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null.");
        }
        Project project = getProjectById(projectId);
        if (teamMemberDao != null) {
            return teamMemberDao.addTeamMember(projectId, userId);
        }
        return project.getTeamMembers().stream().noneMatch(u -> userId.equals(u.getId()));
    }

    @Override
    public boolean removeTeamMemberFromProject(Long projectId, Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null.");
        }
        Project project = getProjectById(projectId);
        boolean removed = project.removeTeamMemberById(userId);
        if (teamMemberDao != null) {
            teamMemberDao.removeTeamMember(projectId, userId);
        }
        if (removed) {
            projectDao.update(project);
        }
        return removed;
    }

    @Override
    public Set<User> getProjectTeamMembers(Long projectId) {
        if (teamMemberDao != null) {
            Set<User> dbMembers = teamMemberDao.findProjectTeam(projectId);
            if (dbMembers != null && !dbMembers.isEmpty()) {
                return dbMembers;
            }
        }
        Project project = getProjectById(projectId);
        return project.getUniqueTeamMembers();
    }

    @Override
    public Set<User> getAllUniqueProjectContributors() {
        // [Collections: Set Uniqueness]
        // Flattens all project team sets into a single unified Set, eliminating duplicates
        Set<User> allContributors = new LinkedHashSet<>();
        for (Project project : getAllProjects()) {
            if (project.getManager() != null) {
                allContributors.add(project.getManager());
            }
            if (project.getTeamMembers() != null) {
                allContributors.addAll(project.getTeamMembers());
            }
        }
        return Collections.unmodifiableSet(allContributors);
    }

    // =========================================================================
    // [Collections: Map]
    // Indexed lookups and statistical aggregations via Map collections
    // =========================================================================

    @Override
    public Map<Long, Project> getProjectMap() {
        return projectDao.findAllAsMap();
    }

    @Override
    public Map<ProjectStatus, Long> getProjectCountByStatus() {
        return GenericCollectionUtils.countBy(getAllProjects(), Project::getStatus);
    }

    @Override
    public Map<Priority, Long> getProjectCountByPriority() {
        return GenericCollectionUtils.countBy(getAllProjects(), Project::getPriority);
    }

    @Override
    public Map<ProjectStatus, List<Project>> getProjectsGroupedByStatus() {
        return GenericCollectionUtils.groupBy(getAllProjects(), Project::getStatus);
    }

    // =========================================================================
    // [Collections: Sorting]
    // Type-safe sorting using Comparator.comparing() and thenComparing()
    // =========================================================================

    @Override
    public List<Project> getProjectsSortedBy(String sortBy) {
        List<Project> projects = getAllProjects();
        if (sortBy == null || sortBy.isBlank()) {
            return projects;
        }

        Comparator<Project> comparator;
        switch (sortBy.trim().toLowerCase()) {
            case "startdate":
            case "start_date":
                comparator = Comparator.comparing(Project::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Project::getId);
                break;
            case "enddate":
            case "end_date":
            case "deadline":
                comparator = Comparator.comparing(Project::getEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Project::getId);
                break;
            case "progress":
                comparator = Comparator.comparingInt(Project::getProgressPercentage).reversed()
                        .thenComparing(Project::getId);
                break;
            case "priority":
                comparator = Comparator.comparing(Project::getPriority).reversed()
                        .thenComparing(Project::getId);
                break;
            case "name":
                comparator = Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Project::getId);
                break;
            default:
                comparator = Comparator.comparing(Project::getId);
                break;
        }

        return GenericCollectionUtils.sort(projects, comparator);
    }

    // =========================================================================
    // [Collections: Filtering]
    // =========================================================================

    @Override
    public List<Project> filterProjectsByStatus(ProjectStatus status) {
        if (status == null) {
            return getAllProjects();
        }
        return GenericCollectionUtils.filter(getAllProjects(), p -> p.getStatus() == status);
    }

    @Override
    public List<Project> filterProjectsByPriority(Priority priority) {
        if (priority == null) {
            return getAllProjects();
        }
        return GenericCollectionUtils.filter(getAllProjects(), p -> p.getPriority() == priority);
    }

    @Override
    public List<Project> filterProjects(String search, ProjectStatus status, Priority priority, String sortBy) {
        List<Project> list = getAllProjects();

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            list = GenericCollectionUtils.filter(list, p ->
                    (p.getName() != null && p.getName().toLowerCase().contains(query)) ||
                    (p.getCode() != null && p.getCode().toLowerCase().contains(query)) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(query))
            );
        }

        if (status != null) {
            list = GenericCollectionUtils.filter(list, p -> p.getStatus() == status);
        }

        if (priority != null) {
            list = GenericCollectionUtils.filter(list, p -> p.getPriority() == priority);
        }

        if (sortBy != null && !sortBy.isBlank()) {
            Comparator<Project> comparator;
            switch (sortBy.trim().toLowerCase()) {
                case "startdate":
                case "start_date":
                    comparator = Comparator.comparing(Project::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Project::getId);
                    break;
                case "enddate":
                case "end_date":
                case "deadline":
                    comparator = Comparator.comparing(Project::getEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Project::getId);
                    break;
                case "progress":
                    comparator = Comparator.comparingInt(Project::getProgressPercentage).reversed()
                            .thenComparing(Project::getId);
                    break;
                case "priority":
                    comparator = Comparator.comparing(Project::getPriority).reversed()
                            .thenComparing(Project::getId);
                    break;
                case "name":
                    comparator = Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Project::getId);
                    break;
                default:
                    comparator = Comparator.comparing(Project::getId);
                    break;
            }
            list = GenericCollectionUtils.sort(list, comparator);
        }

        return list;
    }
}
