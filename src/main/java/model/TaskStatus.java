package model;

/**
 * Enumeration representing the current progression status of a Task.
 */
public enum TaskStatus {
    TODO("To Do", "badge-secondary", "#64748b"),
    IN_PROGRESS("In Progress", "badge-primary", "#4f46e5"),
    IN_REVIEW("In Review", "badge-info", "#0284c7"),
    COMPLETED("Completed", "badge-success", "#10b981"),
    OVERDUE("Overdue", "badge-danger", "#dc2626"),
    BLOCKED("Blocked", "badge-danger", "#ef4444");

    private final String displayName;
    private final String badgeClass;
    private final String colorHex;

    TaskStatus(String displayName, String badgeClass, String colorHex) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getColorHex() {
        return colorHex;
    }
}
