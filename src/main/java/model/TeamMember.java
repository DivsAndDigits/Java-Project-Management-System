package model;

import exception.InvalidUserException;

/**
 * [OOP Concept: Inheritance]
 * Concrete subclass representing an Engineering/Design Team Member inheriting from User.
 * Specializes task execution, technical discipline specialization, and sprint capacity.
 */
public class TeamMember extends User {
    private static final long serialVersionUID = 1L;

    // [OOP Concept: Encapsulation]
    private String technicalSpecialization;
    private int sprintVelocity;

    public TeamMember() {
        super();
        setRole(Role.TEAM_MEMBER);
        this.technicalSpecialization = "Software Engineering";
        this.sprintVelocity = 30;
    }

    public TeamMember(Long id, String username, String fullName, String email, String department,
                      String avatarInitials, String technicalSpecialization, int sprintVelocity) {
        super(id, username, fullName, email, Role.TEAM_MEMBER, department, avatarInitials);
        setTechnicalSpecialization(technicalSpecialization);
        setSprintVelocity(sprintVelocity);
    }

    // =========================================================================
    // [OOP Concept: Polymorphism]
    // Concrete implementations of User's abstract methods
    // =========================================================================

    @Override
    public String getRoleTitle() {
        return "Team Member • " + technicalSpecialization;
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return false;
    }

    @Override
    public boolean canManageProjects() {
        return false;
    }

    @Override
    public boolean canAssignTasks() {
        return false;
    }

    @Override
    public int getMaxConcurrentTasks() {
        return 8;
    }

    @Override
    public String getPermissionsSummary() {
        return "Task work item execution, pull request submissions, hours logging, and sprint progress tracking.";
    }

    // =========================================================================
    // [OOP Concept: Encapsulation]
    // Getters and Setters with validation
    // =========================================================================

    public String getTechnicalSpecialization() {
        return technicalSpecialization;
    }

    public void setTechnicalSpecialization(String technicalSpecialization) {
        if (technicalSpecialization == null || technicalSpecialization.isBlank()) {
            this.technicalSpecialization = "Software Engineering";
        } else {
            this.technicalSpecialization = technicalSpecialization.trim();
        }
    }

    public int getSprintVelocity() {
        return sprintVelocity;
    }

    public void setSprintVelocity(int sprintVelocity) {
        if (sprintVelocity < 0) {
            throw new InvalidUserException("sprintVelocity", "Sprint velocity cannot be negative.");
        }
        this.sprintVelocity = sprintVelocity;
    }
}
