package model;

import exception.InvalidUserException;

/**
 * [OOP Concept: Inheritance]
 * Concrete subclass representing an Administrator user inheriting from User.
 * Specializes administrative permissions, system security levels, and root operations.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    // [OOP Concept: Encapsulation]
    private String adminLevel;
    private boolean superUser;

    public Admin() {
        super();
        setRole(Role.ADMIN);
        this.adminLevel = "SYSTEM_ADMIN";
        this.superUser = true;
    }

    public Admin(Long id, String username, String fullName, String email, String department,
                 String avatarInitials, String adminLevel, boolean superUser) {
        super(id, username, fullName, email, Role.ADMIN, department, avatarInitials);
        setAdminLevel(adminLevel);
        this.superUser = superUser;
    }

    // =========================================================================
    // [OOP Concept: Polymorphism]
    // Concrete implementations of User's abstract methods
    // =========================================================================

    @Override
    public String getRoleTitle() {
        return "System Administrator (" + adminLevel + ")";
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return true;
    }

    @Override
    public boolean canManageProjects() {
        return true;
    }

    @Override
    public boolean canAssignTasks() {
        return true;
    }

    @Override
    public int getMaxConcurrentTasks() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getPermissionsSummary() {
        return "Full root administrative system access, security audit logging, and global configuration control.";
    }

    // =========================================================================
    // [OOP Concept: Encapsulation]
    // Getters and Setters with validation
    // =========================================================================

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        if (adminLevel == null || adminLevel.isBlank()) {
            throw new InvalidUserException("adminLevel", "Admin level cannot be blank.");
        }
        this.adminLevel = adminLevel.trim().toUpperCase();
    }

    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }
}
