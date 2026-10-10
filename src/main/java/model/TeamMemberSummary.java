package model;

import java.io.Serializable;

/**
 * Summary DTO representing a team member's workload and project assignments.
 */
public class TeamMemberSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private User user;
    private int activeProjects;
    private int assignedTasks;
    private int completedTasks;
    private int workloadPercentage;
    private String currentFocus;

    public TeamMemberSummary() {}

    public TeamMemberSummary(User user, int activeProjects, int assignedTasks, int completedTasks,
                             int workloadPercentage, String currentFocus) {
        this.user = user;
        this.activeProjects = activeProjects;
        this.assignedTasks = assignedTasks;
        this.completedTasks = completedTasks;
        this.workloadPercentage = workloadPercentage;
        this.currentFocus = currentFocus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getActiveProjects() {
        return activeProjects;
    }

    public void setActiveProjects(int activeProjects) {
        this.activeProjects = activeProjects;
    }

    public int getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(int assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getWorkloadPercentage() {
        return workloadPercentage;
    }

    public void setWorkloadPercentage(int workloadPercentage) {
        this.workloadPercentage = workloadPercentage;
    }

    public String getCurrentFocus() {
        return currentFocus;
    }

    public void setCurrentFocus(String currentFocus) {
        this.currentFocus = currentFocus;
    }
}
