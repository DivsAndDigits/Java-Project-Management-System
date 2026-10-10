package model;

/**
 * Enumeration representing the priority level of Projects and Tasks.
 */
public enum Priority {
    LOW("Low", "priority-low", "#10b981"),
    MEDIUM("Medium", "priority-medium", "#0ea5e9"),
    HIGH("High", "priority-high", "#f59e0b"),
    URGENT("Urgent", "priority-urgent", "#ef4444");

    private final String displayName;
    private final String badgeClass;
    private final String colorHex;

    Priority(String displayName, String badgeClass, String colorHex) {
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
