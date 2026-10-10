package model;

import exception.InvalidUserException;

/**
 * [OOP Concept: Inheritance]
 * Concrete subclass representing a Project Manager user inheriting from User.
 * Specializes project planning, budget approval thresholds, and task assignment authorization.
 */
public class ProjectManager extends User {
    private static final long serialVersionUID = 1L;

    // [OOP Concept: Encapsulation]
    private double budgetApprovalLimit;
    private String certificationLevel;

    public ProjectManager() {
        super();
        setRole(Role.PROJECT_MANAGER);
        this.budgetApprovalLimit = 100000.0;
        this.certificationLevel = "PMP";
    }

    public ProjectManager(Long id, String username, String fullName, String email, String department,
                          String avatarInitials, double budgetApprovalLimit, String certificationLevel) {
        super(id, username, fullName, email, Role.PROJECT_MANAGER, department, avatarInitials);
        setBudgetApprovalLimit(budgetApprovalLimit);
        setCertificationLevel(certificationLevel);
    }

    // =========================================================================
    // [OOP Concept: Polymorphism]
    // Concrete implementations of User's abstract methods
    // =========================================================================

    @Override
    public String getRoleTitle() {
        return "Project Manager (" + certificationLevel + ")";
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return false;
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
        return 25;
    }

    @Override
    public String getPermissionsSummary() {
        return String.format("Project portfolio creation, sprint milestone planning, resource assignment, and budget approval up to $%,.2f.", budgetApprovalLimit);
    }

    // =========================================================================
    // [OOP Concept: Encapsulation]
    // Getters and Setters with validation
    // =========================================================================

    public double getBudgetApprovalLimit() {
        return budgetApprovalLimit;
    }

    public void setBudgetApprovalLimit(double budgetApprovalLimit) {
        if (budgetApprovalLimit < 0) {
            throw new InvalidUserException("budgetApprovalLimit", "Budget approval limit cannot be negative.");
        }
        this.budgetApprovalLimit = budgetApprovalLimit;
    }

    public String getCertificationLevel() {
        return certificationLevel;
    }

    public void setCertificationLevel(String certificationLevel) {
        if (certificationLevel == null || certificationLevel.isBlank()) {
            this.certificationLevel = "Certified Scrum Master";
        } else {
            this.certificationLevel = certificationLevel.trim();
        }
    }
}
