package model;

import exception.InvalidUserException;
import util.ValidationUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * [OOP Concept: Abstraction]
 * [OOP Concept: Encapsulation]
 * Abstract base class representing a system user within the Online Project Management Tool.
 * Encapsulates core state and identity, providing template abstractions that concrete
 * user subclasses (Admin, ProjectManager, TeamMember) specialize.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // Encapsulated private state
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private String department;
    private String avatarInitials;
    private boolean active;
    private String password;
    private LocalDateTime createdAt;

    /**
     * Protected constructor for subclass instantiation.
     */
    protected User() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * [OOP Concept: Encapsulation]
     * Parameterized constructor validating invariant state before initialization.
     */
    protected User(Long id, String username, String fullName, String email, Role role, String department, String avatarInitials) {
        this.id = id;
        setUsername(username);
        setFullName(fullName);
        setEmail(email);
        this.role = role != null ? role : Role.TEAM_MEMBER;
        this.department = department != null ? department.trim() : "General";
        this.avatarInitials = avatarInitials;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // =========================================================================
    // [OOP Concept: Abstraction]
    // Abstract Methods: Must be implemented by concrete subclasses
    // =========================================================================

    /**
     * Returns the human-readable professional title for this user type.
     */
    public abstract String getRoleTitle();

    /**
     * Indicates whether this user has root administrative privileges.
     */
    public abstract boolean hasAdministrativePrivileges();

    /**
     * Indicates whether this user is authorized to create, scope, and edit projects.
     */
    public abstract boolean canManageProjects();

    /**
     * Indicates whether this user is authorized to assign sprint tasks to others.
     */
    public abstract boolean canAssignTasks();

    /**
     * Returns the maximum concurrent tasks this user type can manage/execute simultaneously.
     */
    public abstract int getMaxConcurrentTasks();

    /**
     * Provides a summary description of permissions granted to this user.
     */
    public abstract String getPermissionsSummary();

    // =========================================================================
    // [OOP Concept: Encapsulation]
    // Getter and Setter Methods with Invariant Validation
    // =========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (ValidationUtils.isBlank(username)) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("username", "Username cannot be null or empty.");
        }
        this.username = username.trim().toLowerCase();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (ValidationUtils.isBlank(fullName)) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("fullName", "Full name cannot be null or empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!ValidationUtils.isValidEmail(email)) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("email", "A valid email format is required (e.g., user@domain.com).");
        }
        this.email = email.trim().toLowerCase();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.TEAM_MEMBER;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : "General";
    }

    public String getAvatarInitials() {
        if (avatarInitials != null && !avatarInitials.isBlank()) {
            return avatarInitials;
        }
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() > 0) {
                return ("" + parts[0].charAt(0)).toUpperCase();
            }
        }
        return "U";
    }

    public void setAvatarInitials(String avatarInitials) {
        this.avatarInitials = avatarInitials;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // =========================================================================
    // [OOP Concept: Polymorphism]
    // Factory method polymorphically creating the correct subclass based on Role
    // =========================================================================
    public static User create(Long id, String username, String fullName, String email, Role role, String department, String avatarInitials) {
        if (role == null) {
            role = Role.TEAM_MEMBER;
        }
        switch (role) {
            case ADMIN:
                return new Admin(id, username, fullName, email, department, avatarInitials, "SYSTEM_ADMIN", true);
            case PROJECT_MANAGER:
                return new ProjectManager(id, username, fullName, email, department, avatarInitials, 150000.0, "PMP / Agile Scrum Master");
            case TEAM_MEMBER:
            case CLIENT:
            default:
                return new TeamMember(id, username, fullName, email, department, avatarInitials, department + " Specialist", 40);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        if (id != null && user.id != null) {
            return id.equals(user.id);
        }
        return username != null && username.equalsIgnoreCase(user.username);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return username != null ? username.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("%s [id=%d, username='%s', fullName='%s', role=%s, department='%s']",
                getClass().getSimpleName(), id, username, fullName, role, department);
    }
}
