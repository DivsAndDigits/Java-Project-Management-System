package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Domain entity representing a Project in the project management application.
 */
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String description;
    private ProjectStatus status;
    private Priority priority;
    private User manager;
    // [Collections: Set & Thread-Safe Collections]
    // CopyOnWriteArraySet enforces Set uniqueness (preventing duplicate team members)
    // while providing thread-safety for concurrent reads across servlet requests.
    private Set<User> teamMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private double budget;
    private double spentAmount;
    private int progressPercentage;
    private int totalTasks;
    private int completedTasks;
    private LocalDateTime createdAt;

    public Project() {
        this.teamMembers = new CopyOnWriteArraySet<>();
        this.status = ProjectStatus.PLANNING;
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
    }

    public Project(Long id, String code, String name, String description, ProjectStatus status,
                   Priority priority, User manager, LocalDate startDate, LocalDate endDate,
                   double budget, double spentAmount, int progressPercentage, int totalTasks, int completedTasks) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.manager = manager;
        this.teamMembers = new CopyOnWriteArraySet<>();
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.spentAmount = spentAmount;
        this.progressPercentage = progressPercentage;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public User getManager() {
        return manager;
    }

    public Long getManagerId() {
        return manager != null ? manager.getId() : null;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    // [Collections: Set]
    // Returns the Set of team members, guaranteeing uniqueness
    public Set<User> getTeamMembers() {
        return teamMembers;
    }

    public Set<User> getUniqueTeamMembers() {
        return Collections.unmodifiableSet(teamMembers);
    }

    public List<User> getTeamMembersAsList() {
        return new ArrayList<>(teamMembers);
    }

    public void setTeamMembers(Collection<User> teamMembers) {
        this.teamMembers = new CopyOnWriteArraySet<>();
        if (teamMembers != null) {
            this.teamMembers.addAll(teamMembers);
        }
    }

    /**
     * [Collections: Set Uniqueness]
     * Adds a unique team member to the project.
     * Returns true if the user was added, false if already present (duplicate prevented).
     */
    public boolean addTeamMember(User member) {
        if (this.teamMembers == null) {
            this.teamMembers = new CopyOnWriteArraySet<>();
        }
        if (member == null) {
            return false;
        }
        return this.teamMembers.add(member);
    }

    /**
     * [Collections: Set]
     * Removes a team member from the project.
     */
    public boolean removeTeamMember(User member) {
        if (this.teamMembers == null || member == null) {
            return false;
        }
        return this.teamMembers.remove(member);
    }

    public boolean removeTeamMemberById(Long userId) {
        if (this.teamMembers == null || userId == null) {
            return false;
        }
        return this.teamMembers.removeIf(u -> userId.equals(u.getId()));
    }

    public boolean hasTeamMember(User member) {
        return this.teamMembers != null && this.teamMembers.contains(member);
    }

    public boolean hasTeamMemberId(Long userId) {
        return this.teamMembers != null && this.teamMembers.stream().anyMatch(u -> userId.equals(u.getId()));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    // [OOP Concept: Encapsulation]
    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        if (budget < 0) {
            throw new IllegalArgumentException("Project budget cannot be negative.");
        }
        this.budget = budget;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        if (spentAmount < 0) {
            throw new IllegalArgumentException("Spent amount cannot be negative.");
        }
        this.spentAmount = spentAmount;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    // =========================================================================
    // [Multithreading Concept: Synchronization on Shared Mutable Resource]
    // Synchronized methods ensure thread-safe atomic updates to project progress
    // metrics when multiple concurrent threads complete or alter tasks simultaneously.
    // Prevents check-then-act race conditions and lost updates.
    // =========================================================================

    /**
     * Atomically records a task status change and recalculates progress percentage.
     * Synchronized on the Project instance monitor lock to prevent lost updates
     * when multiple threads transition tasks concurrently.
     *
     * @param newlyCompleted true if the task was moved into COMPLETED status
     * @param wasCompleted   true if the task was previously in COMPLETED status
     */
    public synchronized void recordTaskCompletion(boolean newlyCompleted, boolean wasCompleted) {
        if (newlyCompleted && !wasCompleted) {
            this.completedTasks++;
        } else if (!newlyCompleted && wasCompleted && this.completedTasks > 0) {
            this.completedTasks--;
        }
        recalculateProgressInternal();
    }

    /**
     * Atomically recalculates progress percentage based on total and completed tasks.
     * Synchronized to guarantee atomic read-modify-write visibility across threads.
     */
    public synchronized void updateTaskMetrics(int total, int completed) {
        this.totalTasks = Math.max(0, total);
        this.completedTasks = Math.max(0, Math.min(this.totalTasks, completed));
        recalculateProgressInternal();
    }

    private void recalculateProgressInternal() {
        if (this.totalTasks > 0) {
            this.progressPercentage = Math.min(100, Math.max(0, (int) Math.round((this.completedTasks * 100.0) / this.totalTasks)));
            if (this.progressPercentage == 100 && this.status == ProjectStatus.IN_PROGRESS) {
                this.status = ProjectStatus.COMPLETED;
            }
        } else {
            this.progressPercentage = 0;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOverdue() {
        return endDate != null && LocalDate.now().isAfter(endDate) && status != ProjectStatus.COMPLETED;
    }

    public double getRemainingBudget() {
        return Math.max(0.0, budget - spentAmount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        if (id != null && project.id != null) {
            return id.equals(project.id);
        }
        return code != null && code.equalsIgnoreCase(project.code);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return code != null ? code.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", progress=" + progressPercentage + "%" +
                '}';
    }
}
