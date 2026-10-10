package model;

/**
 * Enumeration representing the current lifecycle status of a Project.
 */
public enum ProjectStatus {
    PLANNING("Planning", "badge-secondary", "#64748b"),
    IN_PROGRESS("In Progress", "badge-primary", "#4f46e5"),
    ON_HOLD("On Hold", "badge-warning", "#f59e0b"),
    COMPLETED("Completed", "badge-success", "#10b981"),
    CANCELLED("Cancelled", "badge-danger", "#ef4444");

    private final String displayName;
    private final String badgeClass;
    private final String colorHex;

    ProjectStatus(String displayName, String badgeClass, String colorHex) {
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
