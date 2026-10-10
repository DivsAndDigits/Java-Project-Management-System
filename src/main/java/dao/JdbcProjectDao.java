package dao;

import exception.DatabaseOperationException;
import model.*;
import util.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ==============================================================================
 * Database Operations: JDBC Implementation of ProjectDao (7 marks foundation)
 * ==============================================================================
 * 
 * Concrete JDBC implementation of ProjectDao communicating with MySQL 8.x.
 * 
 * Key Features:
 * 1. Uses SQL LEFT JOIN on 'users' to eagerly hydrate the Project Manager entity.
 * 2. Eagerly populates unique Project Team Members using TeamMemberDao / 'team_members' table.
 * 3. Parameterized queries via PreparedStatement preventing SQL injection.
 * 4. ACID Transaction Management: implements createProjectWithTeam() using
 *    setAutoCommit(false), commit(), and rollback() upon failure.
 * 5. Exception Translation: converts SQLExceptions to DatabaseOperationException.
 */
public class JdbcProjectDao implements ProjectDao {

    private final DatabaseConnectionManager connectionManager;
    private final TeamMemberDao teamMemberDao;

    public JdbcProjectDao() {
        this(DatabaseConnectionManager.getInstance(), new JdbcTeamMemberDao());
    }

    public JdbcProjectDao(DatabaseConnectionManager connectionManager) {
        this(connectionManager, new JdbcTeamMemberDao(connectionManager));
    }

    public JdbcProjectDao(DatabaseConnectionManager connectionManager, TeamMemberDao teamMemberDao) {
        this.connectionManager = connectionManager != null ? connectionManager : DatabaseConnectionManager.getInstance();
        this.teamMemberDao = teamMemberDao != null ? teamMemberDao : new JdbcTeamMemberDao(this.connectionManager);
    }

    private Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    private static final String BASE_SELECT_JOIN =
            "SELECT p.id, p.code, p.name, p.description, p.status, p.priority, " +
            "       p.manager_id, p.start_date, p.end_date, p.budget, p.spent_amount, " +
            "       p.progress_percentage, p.total_tasks, p.completed_tasks, p.created_at, " +
            "       u.id AS u_id, u.username AS u_username, u.full_name AS u_full_name, " +
            "       u.email AS u_email, u.role AS u_role, u.department AS u_department, " +
            "       u.avatar_initials AS u_avatar, u.admin_level AS u_admin_level, " +
            "       u.is_superuser AS u_is_superuser, u.budget_approval_limit AS u_budget_limit, " +
            "       u.certification_level AS u_cert_level, u.technical_specialization AS u_tech_spec, " +
            "       u.sprint_velocity AS u_velocity, u.is_active AS u_active " +
            "FROM projects p " +
            "LEFT JOIN users u ON p.manager_id = u.id ";

    @Override
    public Optional<Project> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = BASE_SELECT_JOIN + "WHERE p.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Project project = mapResultSetToProject(rs);
                    loadTeamMembers(project);
                    return Optional.of(project);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding project by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Project> findAll() {
        String sql = BASE_SELECT_JOIN + "ORDER BY p.id ASC";
        List<Project> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Project project = mapResultSetToProject(rs);
                loadTeamMembers(project);
                list.add(project);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all projects.", e);
        }
        return list;
    }

    @Override
    public Map<Long, Project> findAllAsMap() {
        List<Project> list = findAll();
        Map<Long, Project> map = new LinkedHashMap<>();
        for (Project project : list) {
            if (project.getId() != null) {
                map.put(project.getId(), project);
            }
        }
        return map;
    }

    @Override
    public Project save(Project entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Project entity cannot be null.");
        }

        String sql = "INSERT INTO projects (" +
                "code, name, description, status, priority, manager_id, " +
                "start_date, end_date, budget, spent_amount, progress_percentage, " +
                "total_tasks, completed_tasks) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindProjectParameters(ps, entity);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating project failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long generatedId = generatedKeys.getLong(1);
                    entity.setId(generatedId);
                    if (entity.getCode() == null || entity.getCode().isBlank()) {
                        entity.setCode("PRJ-" + generatedId);
                        updateProjectCode(conn, generatedId, entity.getCode());
                    }
                }
            }

            // Persist associated team members if present
            if (entity.getTeamMembers() != null && !entity.getTeamMembers().isEmpty()) {
                for (User member : entity.getTeamMembers()) {
                    if (member != null && member.getId() != null) {
                        teamMemberDao.addTeamMember(entity.getId(), member.getId());
                    }
                }
            }

            return entity;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating project: " + entity.getName(), e);
        }
    }

    /**
     * [Database Operations: Transactions]
     * Executes atomic multi-table mutation using manual transaction control:
     * 1. conn.setAutoCommit(false)
     * 2. Insert project row and retrieve generated primary key
     * 3. Insert initial team member relationships into 'team_members' join table
     * 4. conn.commit() upon complete success
     * 5. conn.rollback() on any error to maintain referential integrity
     */
    @Override
    public Project createProjectWithTeam(Project project, Set<Long> memberUserIds) {
        if (project == null) {
            throw new IllegalArgumentException("Project entity cannot be null.");
        }

        Connection conn = null;
        try {
            conn = getConnection();
            // Start transaction
            conn.setAutoCommit(false);

            String insertProjectSql = "INSERT INTO projects (" +
                    "code, name, description, status, priority, manager_id, " +
                    "start_date, end_date, budget, spent_amount, progress_percentage, " +
                    "total_tasks, completed_tasks) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertProjectSql, Statement.RETURN_GENERATED_KEYS)) {
                bindProjectParameters(ps, project);
                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        project.setId(generatedId);
                        if (project.getCode() == null || project.getCode().isBlank()) {
                            project.setCode("PRJ-" + generatedId);
                            updateProjectCode(conn, generatedId, project.getCode());
                        }
                    }
                }
            }

            // Assign initial team members inside the same transaction
            if (memberUserIds != null && !memberUserIds.isEmpty()) {
                String insertMemberSql = "INSERT INTO team_members (project_id, user_id) VALUES (?, ?)";
                try (PreparedStatement psMember = conn.prepareStatement(insertMemberSql)) {
                    for (Long userId : memberUserIds) {
                        if (userId != null) {
                            psMember.setLong(1, project.getId());
                            psMember.setLong(2, userId);
                            psMember.addBatch();
                        }
                    }
                    psMember.executeBatch();
                }
            }

            // Commit transaction
            conn.commit();

            // Load populated team members for the returned project
            loadTeamMembers(project);
            return project;

        } catch (SQLException e) {
            // Rollback on any failure to guarantee atomicity
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[JdbcProjectDao] Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseOperationException("Transaction failed: rolled back project creation for " + project.getName(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void updateProjectCode(Connection conn, long id, String code) throws SQLException {
        String sql = "UPDATE projects SET code = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean update(Project entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        String sql = "UPDATE projects SET " +
                "code = ?, name = ?, description = ?, status = ?, priority = ?, " +
                "manager_id = ?, start_date = ?, end_date = ?, budget = ?, " +
                "spent_amount = ?, progress_percentage = ?, total_tasks = ?, " +
                "completed_tasks = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindProjectParameters(ps, entity);
            ps.setLong(14, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating project ID: " + entity.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM projects WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting project ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM projects WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking project existence for ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM projects";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting projects.", e);
        }
    }

    @Override
    public List<Project> findByStatus(ProjectStatus status) {
        if (status == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE p.status = ? ORDER BY p.id ASC";
        return executeQueryWithParam(sql, status.name());
    }

    @Override
    public List<Project> findByPriority(Priority priority) {
        if (priority == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE p.priority = ? ORDER BY p.id ASC";
        return executeQueryWithParam(sql, priority.name());
    }

    @Override
    public List<Project> findRecent(int limit) {
        String sql = BASE_SELECT_JOIN + "ORDER BY p.id DESC LIMIT ?";
        List<Project> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project project = mapResultSetToProject(rs);
                    loadTeamMembers(project);
                    list.add(project);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving recent projects.", e);
        }
        return list;
    }

    @Override
    public List<Project> findByManagerId(Long managerId) {
        if (managerId == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE p.manager_id = ? ORDER BY p.id ASC";
        List<Project> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, managerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project project = mapResultSetToProject(rs);
                    loadTeamMembers(project);
                    list.add(project);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding projects by manager ID: " + managerId, e);
        }
        return list;
    }

    @Override
    public List<Project> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        String sql = BASE_SELECT_JOIN +
                "WHERE LOWER(p.name) LIKE ? OR LOWER(p.code) LIKE ? OR LOWER(p.description) LIKE ? " +
                "ORDER BY p.id ASC";

        List<Project> list = new ArrayList<>();
        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project project = mapResultSetToProject(rs);
                    loadTeamMembers(project);
                    list.add(project);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error searching projects by keyword: " + keyword, e);
        }
        return list;
    }

    @Override
    public int countByStatus(ProjectStatus status) {
        if (status == null) return 0;
        String sql = "SELECT COUNT(*) FROM projects WHERE status = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting projects by status: " + status, e);
        }
    }

    // =========================================================================
    // Helper Methods: Parameter Binding & ResultSet Mapping
    // =========================================================================

    private void bindProjectParameters(PreparedStatement ps, Project entity) throws SQLException {
        ps.setString(1, entity.getCode());
        ps.setString(2, entity.getName());
        ps.setString(3, entity.getDescription());
        ps.setString(4, entity.getStatus() != null ? entity.getStatus().name() : ProjectStatus.PLANNING.name());
        ps.setString(5, entity.getPriority() != null ? entity.getPriority().name() : Priority.MEDIUM.name());

        if (entity.getManager() != null && entity.getManager().getId() != null) {
            ps.setLong(6, entity.getManager().getId());
        } else {
            ps.setNull(6, Types.BIGINT);
        }

        if (entity.getStartDate() != null) {
            ps.setDate(7, java.sql.Date.valueOf(entity.getStartDate()));
        } else {
            ps.setNull(7, Types.DATE);
        }

        if (entity.getEndDate() != null) {
            ps.setDate(8, java.sql.Date.valueOf(entity.getEndDate()));
        } else {
            ps.setNull(8, Types.DATE);
        }

        ps.setDouble(9, entity.getBudget());
        ps.setDouble(10, entity.getSpentAmount());
        ps.setInt(11, entity.getProgressPercentage());
        ps.setInt(12, entity.getTotalTasks());
        ps.setInt(13, entity.getCompletedTasks());
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String code = rs.getString("code");
        String name = rs.getString("name");
        String description = rs.getString("description");

        String statusStr = rs.getString("status");
        ProjectStatus status = ProjectStatus.PLANNING;
        if (statusStr != null) {
            try {
                status = ProjectStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String priorityStr = rs.getString("priority");
        Priority priority = Priority.MEDIUM;
        if (priorityStr != null) {
            try {
                priority = Priority.valueOf(priorityStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // Hydrate manager via JOIN columns
        User manager = null;
        Long managerId = rs.getObject("manager_id", Long.class);
        if (managerId != null && rs.getString("u_username") != null) {
            manager = mapJoinedManager(rs);
        }

        java.sql.Date startDateSql = rs.getDate("start_date");
        LocalDate startDate = startDateSql != null ? startDateSql.toLocalDate() : null;

        java.sql.Date endDateSql = rs.getDate("end_date");
        LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;

        double budget = rs.getDouble("budget");
        double spent = rs.getDouble("spent_amount");
        int progress = rs.getInt("progress_percentage");
        int totalTasks = rs.getInt("total_tasks");
        int completedTasks = rs.getInt("completed_tasks");

        Project project = new Project(id, code, name, description, status, priority, manager,
                startDate, endDate, budget, spent, progress, totalTasks, completedTasks);

        Timestamp createdAtSql = rs.getTimestamp("created_at");
        if (createdAtSql != null) {
            project.setCreatedAt(createdAtSql.toLocalDateTime());
        }

        return project;
    }

    private User mapJoinedManager(ResultSet rs) throws SQLException {
        Long uId = rs.getLong("u_id");
        String uUsername = rs.getString("u_username");
        String uFullName = rs.getString("u_full_name");
        String uEmail = rs.getString("u_email");
        String uRoleStr = rs.getString("u_role");
        String uDept = rs.getString("u_department");
        String uAvatar = rs.getString("u_avatar");
        boolean uActive = rs.getBoolean("u_active");

        Role role = Role.PROJECT_MANAGER;
        if (uRoleStr != null) {
            try {
                role = Role.valueOf(uRoleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        User user;
        if (role == Role.PROJECT_MANAGER) {
            double budgetLimit = rs.getDouble("u_budget_limit");
            String cert = rs.getString("u_cert_level");
            user = new ProjectManager(uId, uUsername, uFullName, uEmail, uDept, uAvatar, budgetLimit, cert != null ? cert : "PMP");
        } else if (role == Role.ADMIN) {
            String adminLevel = rs.getString("u_admin_level");
            boolean superUser = rs.getBoolean("u_is_superuser");
            user = new Admin(uId, uUsername, uFullName, uEmail, uDept, uAvatar, adminLevel != null ? adminLevel : "SYSTEM_ADMIN", superUser);
        } else {
            String techSpec = rs.getString("u_tech_spec");
            int velocity = rs.getInt("u_velocity");
            user = new TeamMember(uId, uUsername, uFullName, uEmail, uDept, uAvatar, techSpec != null ? techSpec : "General", velocity > 0 ? velocity : 30);
        }
        user.setActive(uActive);
        return user;
    }

    private void loadTeamMembers(Project project) {
        if (project != null && project.getId() != null) {
            Set<User> team = teamMemberDao.findProjectTeam(project.getId());
            project.setTeamMembers(team);
        }
    }

    private List<Project> executeQueryWithParam(String sql, String param) {
        List<Project> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project project = mapResultSetToProject(rs);
                    loadTeamMembers(project);
                    list.add(project);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error executing project query with param: " + param, e);
        }
        return list;
    }
}
