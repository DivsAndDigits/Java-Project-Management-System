package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain entity representing an actionable task within a project.
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private User assignee;
    private User reporter;
    private LocalDate dueDate;
    private int estimatedHours;
    private int loggedHours;
    private LocalDateTime createdAt;

    public Task() {
        this.status = TaskStatus.TODO;
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
    }

    public Task(Long id, Long projectId, String projectName, String title, String description,
                TaskStatus status, Priority priority, User assignee, User reporter,
                LocalDate dueDate, int estimatedHours, int loggedHours) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
        this.reporter = reporter;
        this.dueDate = dueDate;
        this.estimatedHours = estimatedHours;
        this.loggedHours = loggedHours;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Long getAssigneeId() {
        return assignee != null ? assignee.getId() : null;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public Long getReporterId() {
        return reporter != null ? reporter.getId() : null;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // [OOP Concept: Encapsulation]
    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        if (estimatedHours < 0) {
            throw new IllegalArgumentException("Estimated hours cannot be negative.");
        }
        this.estimatedHours = estimatedHours;
    }

    public int getLoggedHours() {
        return loggedHours;
    }

    public void setLoggedHours(int loggedHours) {
        if (loggedHours < 0) {
            throw new IllegalArgumentException("Logged hours cannot be negative.");
        }
        this.loggedHours = loggedHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOverdue() {
        return status == TaskStatus.OVERDUE || 
               (dueDate != null && LocalDate.now().isAfter(dueDate) && status != TaskStatus.COMPLETED);
    }

    public TaskStatus getEffectiveStatus() {
        return isOverdue() ? TaskStatus.OVERDUE : status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        if (id != null && task.id != null) {
            return id.equals(task.id);
        }
        return title != null && title.equalsIgnoreCase(task.title);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return title != null ? title.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", dueDate=" + dueDate +
                '}';
    }
}
