package dao;

import exception.DatabaseOperationException;
import model.Admin;
import model.ProjectManager;
import model.Role;
import model.TeamMember;
import model.User;
import util.DatabaseConnectionManager;

import java.sql.*;
import java.util.*;

/**
 * ==============================================================================
 * Database Operations: JDBC Implementation of UserDao (7 marks foundation)
 * ==============================================================================
 * 
 * Concrete JDBC implementation of the UserDao contract for MySQL 8.x.
 * 
 * Features:
 * 1. Executes parameterized SQL queries via PreparedStatement exclusively to prevent SQL injection.
 * 2. Employs try-with-resources to guarantee automatic closure of Connection, PreparedStatement, and ResultSet.
 * 3. Handles OOP Polymorphic instantiation: translates relational table rows into concrete
 *    subtypes (Admin, ProjectManager, TeamMember) based on the 'role' column.
 * 4. Catches technical java.sql.SQLException and converts into user-safe DatabaseOperationException.
 */
public class JdbcUserDao implements UserDao {

    private final DatabaseConnectionManager connectionManager;

    public JdbcUserDao() {
        this(DatabaseConnectionManager.getInstance());
    }

    public JdbcUserDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager != null ? connectionManager : DatabaseConnectionManager.getInstance();
    }

    private Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id ASC";
        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all users.", e);
        }
        return users;
    }

    @Override
    public Map<Long, User> findAllAsMap() {
        List<User> list = findAll();
        Map<Long, User> map = new LinkedHashMap<>();
        for (User user : list) {
            if (user.getId() != null) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    @Override
    public User save(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("User entity to save cannot be null.");
        }

        String sql = "INSERT INTO users (" +
                "username, full_name, email, role, department, avatar_initials, is_active, " +
                "password, admin_level, is_superuser, budget_approval_limit, certification_level, " +
                "technical_specialization, sprint_velocity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindUserParameters(ps, entity);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating user: " + entity.getUsername(), e);
        }
    }

    @Override
    public boolean update(User entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        String sql = "UPDATE users SET " +
                "username = ?, full_name = ?, email = ?, role = ?, department = ?, " +
                "avatar_initials = ?, is_active = ?, password = ?, admin_level = ?, is_superuser = ?, " +
                "budget_approval_limit = ?, certification_level = ?, technical_specialization = ?, " +
                "sprint_velocity = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindUserParameters(ps, entity);
            ps.setLong(15, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating user ID: " + entity.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting user by ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking user existence for ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting users.", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findByRole(Role role) {
        if (role == null) return Collections.emptyList();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name ASC";
        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding users by role: " + role, e);
        }
        return users;
    }

    @Override
    public List<User> findByDepartment(String department) {
        if (department == null || department.isBlank()) return Collections.emptyList();
        String sql = "SELECT * FROM users WHERE LOWER(department) = LOWER(?) ORDER BY full_name ASC";
        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, department.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding users by department: " + department, e);
        }
        return users;
    }

    @Override
    public Optional<User> login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM users WHERE (LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)) " +
                "AND password = ? AND is_active = TRUE";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String cleanIdentity = usernameOrEmail.trim();
            ps.setString(1, cleanIdentity);
            ps.setString(2, cleanIdentity);
            ps.setString(3, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error executing user login query for: " + usernameOrEmail, e);
        }

        // Secondary fallback for salted hashed passwords (e.g. SHA256$<salt>$<hash>)
        Optional<User> candidate = findByUsername(usernameOrEmail);
        if (candidate.isEmpty()) {
            candidate = findByEmail(usernameOrEmail);
        }
        if (candidate.isPresent()) {
            User user = candidate.get();
            if (user.isActive() && util.PasswordUtils.verifyPassword(password, user.getPassword())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    @Override
    public User register(User user, String password) {
        if (user == null) {
            throw new IllegalArgumentException("User entity to register cannot be null.");
        }
        user.setPassword(password);
        return save(user);
    }

    // =========================================================================
    // Helper Methods: Parameter Binding & Polymorphic ResultSet Mapping
    // =========================================================================

    private void bindUserParameters(PreparedStatement ps, User entity) throws SQLException {
        ps.setString(1, entity.getUsername());
        ps.setString(2, entity.getFullName());
        ps.setString(3, entity.getEmail());
        ps.setString(4, entity.getRole() != null ? entity.getRole().name() : Role.TEAM_MEMBER.name());
        ps.setString(5, entity.getDepartment());
        ps.setString(6, entity.getAvatarInitials());
        ps.setBoolean(7, entity.isActive());
        ps.setString(8, entity.getPassword() != null ? entity.getPassword() : "password123");

        // Polymorphic subtype state extraction
        if (entity instanceof Admin admin) {
            ps.setString(9, admin.getAdminLevel());
            ps.setBoolean(10, admin.isSuperUser());
            ps.setDouble(11, 0.0);
            ps.setString(12, "SYSTEM");
            ps.setString(13, "DevOps & Administration");
            ps.setInt(14, 30);
        } else if (entity instanceof ProjectManager pm) {
            ps.setString(9, "PM");
            ps.setBoolean(10, false);
            ps.setDouble(11, pm.getBudgetApprovalLimit());
            ps.setString(12, pm.getCertificationLevel());
            ps.setString(13, "Project Management");
            ps.setInt(14, 30);
        } else if (entity instanceof TeamMember tm) {
            ps.setString(9, "MEMBER");
            ps.setBoolean(10, false);
            ps.setDouble(11, 0.0);
            ps.setString(12, "NONE");
            ps.setString(13, tm.getTechnicalSpecialization());
            ps.setInt(14, tm.getSprintVelocity());
        } else {
            ps.setString(9, "MEMBER");
            ps.setBoolean(10, false);
            ps.setDouble(11, 0.0);
            ps.setString(12, "NONE");
            ps.setString(13, "General");
            ps.setInt(14, 30);
        }
    }

    /**
     * [OOP Concept: Polymorphism & Inheritance]
     * Factory mapping method: examines role column to instantiate the appropriate
     * concrete User subclass (Admin, ProjectManager, TeamMember).
     */
    public static User mapResultSetToUser(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String roleStr = rs.getString("role");
        String department = rs.getString("department");
        String avatarInitials = rs.getString("avatar_initials");
        boolean isActive = rs.getBoolean("is_active");

        Role role = Role.TEAM_MEMBER;
        if (roleStr != null) {
            try {
                role = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        User user;
        switch (role) {
            case ADMIN -> {
                String adminLevel = rs.getString("admin_level");
                boolean superUser = rs.getBoolean("is_superuser");
                user = new Admin(id, username, fullName, email, department, avatarInitials,
                        adminLevel != null ? adminLevel : "SYSTEM_ADMIN", superUser);
            }
            case PROJECT_MANAGER -> {
                double budgetLimit = rs.getDouble("budget_approval_limit");
                String certLevel = rs.getString("certification_level");
                user = new ProjectManager(id, username, fullName, email, department, avatarInitials,
                        budgetLimit, certLevel != null ? certLevel : "PMP Certified");
            }
            case TEAM_MEMBER -> {
                String specialization = rs.getString("technical_specialization");
                int velocity = rs.getInt("sprint_velocity");
                user = new TeamMember(id, username, fullName, email, department, avatarInitials,
                        specialization != null ? specialization : "Software Engineering",
                        velocity > 0 ? velocity : 30);
            }
            default -> user = new TeamMember(id, username, fullName, email, department, avatarInitials,
                    "General", 30);
        }

        try {
            user.setPassword(rs.getString("password"));
        } catch (SQLException ignored) {
        }

        user.setActive(isActive);
        return user;
    }
}
