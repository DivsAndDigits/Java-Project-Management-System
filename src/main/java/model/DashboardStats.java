package model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated metrics and data container for the dashboard overview.
 * Demonstrates Collections (List, Set, Map) integration across reporting components.
 */
public class DashboardStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalProjects;
    private int activeProjects;
    private int completedProjects;
    private int totalTasks;
    private int inProgressTasks;
    private int completedTasks;
    private int pendingTasks;
    private int overdueTasks;
    private int teamMembersCount;
    private int overallProgressPercentage;
    private double totalBudget;
    private double totalSpent;
    private List<Project> recentProjects;
    private List<Task> priorityTasks;
    private List<TeamMemberSummary> teamSummary;

    // [Collections: Map & Set]
    private Map<TaskStatus, Long> tasksByStatus;
    private Map<Priority, Long> tasksByPriority;
    private Map<ProjectStatus, Long> projectsByStatus;
    private Map<Priority, Long> projectsByPriority;
    private Set<User> uniqueContributors;

    public DashboardStats() {}

    public int getTotalProjects() {
        return totalProjects;
    }

    public void setTotalProjects(int totalProjects) {
        this.totalProjects = totalProjects;
    }

    public int getActiveProjects() {
        return activeProjects;
    }

    public void setActiveProjects(int activeProjects) {
        this.activeProjects = activeProjects;
    }

    public int getCompletedProjects() {
        return completedProjects;
    }

    public void setCompletedProjects(int completedProjects) {
        this.completedProjects = completedProjects;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(int inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(int pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(int overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public int getTeamMembersCount() {
        return teamMembersCount;
    }

    public void setTeamMembersCount(int teamMembersCount) {
        this.teamMembersCount = teamMembersCount;
    }

    public int getOverallProgressPercentage() {
        return overallProgressPercentage;
    }

    public void setOverallProgressPercentage(int overallProgressPercentage) {
        this.overallProgressPercentage = overallProgressPercentage;
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public List<Project> getRecentProjects() {
        return recentProjects;
    }

    public void setRecentProjects(List<Project> recentProjects) {
        this.recentProjects = recentProjects;
    }

    public List<Task> getPriorityTasks() {
        return priorityTasks;
    }

    public void setPriorityTasks(List<Task> priorityTasks) {
        this.priorityTasks = priorityTasks;
    }

    public List<TeamMemberSummary> getTeamSummary() {
        return teamSummary;
    }

    public void setTeamSummary(List<TeamMemberSummary> teamSummary) {
        this.teamSummary = teamSummary;
    }

    public Map<TaskStatus, Long> getTasksByStatus() {
        return tasksByStatus;
    }

    public void setTasksByStatus(Map<TaskStatus, Long> tasksByStatus) {
        this.tasksByStatus = tasksByStatus;
    }

    public Map<Priority, Long> getTasksByPriority() {
        return tasksByPriority;
    }

    public void setTasksByPriority(Map<Priority, Long> tasksByPriority) {
        this.tasksByPriority = tasksByPriority;
    }

    public Map<ProjectStatus, Long> getProjectsByStatus() {
        return projectsByStatus;
    }

    public void setProjectsByStatus(Map<ProjectStatus, Long> projectsByStatus) {
        this.projectsByStatus = projectsByStatus;
    }

    public Map<Priority, Long> getProjectsByPriority() {
        return projectsByPriority;
    }

    public void setProjectsByPriority(Map<Priority, Long> projectsByPriority) {
        this.projectsByPriority = projectsByPriority;
    }

    public Set<User> getUniqueContributors() {
        return uniqueContributors;
    }

    public void setUniqueContributors(Set<User> uniqueContributors) {
        this.uniqueContributors = uniqueContributors;
    }
}
