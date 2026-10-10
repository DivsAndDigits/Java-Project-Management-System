package service;

import dao.ProjectDao;
import dao.TaskDao;
import dao.UserDao;
import model.*;
import java.util.List;

/**
 * Implementation of DashboardService aggregating domain statistics from DAOs and services.
 */
public class DashboardServiceImpl implements DashboardService {

    private final ProjectDao projectDao;
    private final TaskDao taskDao;
    private final UserDao userDao;
    private final UserService userService;

    public DashboardServiceImpl(ProjectDao projectDao, TaskDao taskDao, UserDao userDao, UserService userService) {
        this.projectDao = projectDao;
        this.taskDao = taskDao;
        this.userDao = userDao;
        this.userService = userService;
    }

    @Override
    public DashboardStats getDashboardStatistics() {
        DashboardStats stats = new DashboardStats();

        List<Project> allProjects = projectDao.findAll();
        List<Task> allTasks = taskDao.findAll();

        int totalProjects = allProjects.size();
        int activeProjects = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS).count();
        int completedProjects = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();

        int totalTasks = allTasks.size();
        int inProgressTasks = (int) allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int completedTasks = (int) allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int pendingTasks = (int) allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
        int overdueTasks = (int) allTasks.stream().filter(Task::isOverdue).count();

        double totalBudget = allProjects.stream().mapToDouble(Project::getBudget).sum();
        double totalSpent = allProjects.stream().mapToDouble(Project::getSpentAmount).sum();

        int avgProgress = 0;
        if (!allProjects.isEmpty()) {
            avgProgress = (int) allProjects.stream().mapToInt(Project::getProgressPercentage).average().orElse(0.0);
        }

        stats.setTotalProjects(totalProjects);
        stats.setActiveProjects(activeProjects);
        stats.setCompletedProjects(completedProjects);
        stats.setTotalTasks(totalTasks);
        stats.setInProgressTasks(inProgressTasks);
        stats.setCompletedTasks(completedTasks);
        stats.setPendingTasks(pendingTasks);
        stats.setOverdueTasks(overdueTasks);
        stats.setTeamMembersCount(userDao.findAll().size());
        stats.setOverallProgressPercentage(avgProgress);
        stats.setTotalBudget(totalBudget);
        stats.setTotalSpent(totalSpent);

        // [Collections: Map & Set Integration]
        // Grouping and counting dynamically using GenericCollectionUtils
        java.util.Map<TaskStatus, Long> tasksByStatus = util.GenericCollectionUtils.countBy(allTasks, Task::getStatus);
        java.util.Map<Priority, Long> tasksByPriority = util.GenericCollectionUtils.countBy(allTasks, Task::getPriority);
        java.util.Map<ProjectStatus, Long> projectsByStatus = util.GenericCollectionUtils.countBy(allProjects, Project::getStatus);
        java.util.Map<Priority, Long> projectsByPriority = util.GenericCollectionUtils.countBy(allProjects, Project::getPriority);

        java.util.Set<User> uniqueContributors = new java.util.LinkedHashSet<>();
        for (Project p : allProjects) {
            if (p.getManager() != null) uniqueContributors.add(p.getManager());
            if (p.getTeamMembers() != null) uniqueContributors.addAll(p.getTeamMembers());
        }

        stats.setTasksByStatus(tasksByStatus);
        stats.setTasksByPriority(tasksByPriority);
        stats.setProjectsByStatus(projectsByStatus);
        stats.setProjectsByPriority(projectsByPriority);
        stats.setUniqueContributors(uniqueContributors);

        // Populate recent projects and high priority tasks
        stats.setRecentProjects(projectDao.findRecent(5));
        stats.setPriorityTasks(taskDao.findPriorityTasks(6));
        stats.setTeamSummary(userService.getTeamMemberSummaries());

        return stats;
    }
}
