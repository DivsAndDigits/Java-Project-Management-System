package model;

/**
 * Enumeration representing distinct user roles within the Online Project Management Tool.
 */
public enum Role {
    ADMIN("Administrator", "Full system access and settings management"),
    PROJECT_MANAGER("Project Manager", "Project creation, resource allocation, and reporting"),
    TEAM_MEMBER("Team Member", "Task execution, progress tracking, and collaboration"),
    CLIENT("Client", "Read-only milestone and project progress viewing");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
