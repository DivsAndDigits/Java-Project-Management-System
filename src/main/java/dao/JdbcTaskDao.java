package dao;

import exception.DatabaseOperationException;
import model.*;
import util.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * ==============================================================================
 * Database Operations: JDBC Implementation of TaskDao (7 marks foundation)
 * ==============================================================================
 * 
 * Concrete JDBC implementation of TaskDao communicating with MySQL 8.x.
 * 
 * Key Features:
 * 1. Uses SQL LEFT JOIN across 'projects' and 'users' (assignee and reporter)
 *    to fully hydrate Task associations in a single query.
 * 2. Parameterized queries via PreparedStatement preventing SQL injection.
 * 3. try-with-resources resource management closing Connection, PreparedStatement, and ResultSet.
 * 4. Implements overdue task detection, priority sorting, and status filtering in SQL.
 * 5. Exception Translation: converts SQLExceptions to DatabaseOperationException.
 */
public class JdbcTaskDao implements TaskDao {

    private final DatabaseConnectionManager connectionManager;

    public JdbcTaskDao() {
        this(DatabaseConnectionManager.getInstance());
    }

    public JdbcTaskDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager != null ? connectionManager : DatabaseConnectionManager.getInstance();
    }

    private Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    private static final String BASE_SELECT_JOIN =
            "SELECT t.id, t.project_id, t.title, t.description, t.status, t.priority, " +
            "       t.assignee_id, t.reporter_id, t.due_date, t.estimated_hours, t.logged_hours, t.created_at, " +
            "       pr.name AS pr_name, " +
            "       a.id AS a_id, a.username AS a_username, a.full_name AS a_fullname, " +
            "       a.email AS a_email, a.role AS a_role, a.department AS a_dept, a.avatar_initials AS a_avatar, " +
            "       r.id AS r_id, r.username AS r_username, r.full_name AS r_fullname, " +
            "       r.email AS r_email, r.role AS r_role, r.department AS r_dept, r.avatar_initials AS r_avatar " +
            "FROM tasks t " +
            "LEFT JOIN projects pr ON t.project_id = pr.id " +
            "LEFT JOIN users a ON t.assignee_id = a.id " +
            "LEFT JOIN users r ON t.reporter_id = r.id ";

    @Override
    public Optional<Task> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = BASE_SELECT_JOIN + "WHERE t.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding task by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        String sql = BASE_SELECT_JOIN + "ORDER BY t.id ASC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all tasks.", e);
        }
        return list;
    }

    @Override
    public Map<Long, Task> findAllAsMap() {
        List<Task> list = findAll();
        Map<Long, Task> map = new LinkedHashMap<>();
        for (Task task : list) {
            if (task.getId() != null) {
                map.put(task.getId(), task);
            }
        }
        return map;
    }

    @Override
    public Task save(Task entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Task entity cannot be null.");
        }

        String sql = "INSERT INTO tasks (" +
                "project_id, title, description, status, priority, assignee_id, " +
                "reporter_id, due_date, estimated_hours, logged_hours) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTaskParameters(ps, entity);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating task failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating task: " + entity.getTitle(), e);
        }
    }

    @Override
    public boolean update(Task entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        String sql = "UPDATE tasks SET " +
                "project_id = ?, title = ?, description = ?, status = ?, priority = ?, " +
                "assignee_id = ?, reporter_id = ?, due_date = ?, estimated_hours = ?, " +
                "logged_hours = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindTaskParameters(ps, entity);
            ps.setLong(11, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating task ID: " + entity.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting task ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM tasks WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking task existence for ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM tasks";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting tasks.", e);
        }
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE t.project_id = ? ORDER BY t.id ASC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding tasks by project ID: " + projectId, e);
        }
        return list;
    }

    @Override
    public List<Task> findByAssigneeId(Long assigneeId) {
        if (assigneeId == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE t.assignee_id = ? ORDER BY t.due_date ASC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, assigneeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding tasks by assignee ID: " + assigneeId, e);
        }
        return list;
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        if (status == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE t.status = ? ORDER BY t.id ASC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding tasks by status: " + status, e);
        }
        return list;
    }

    @Override
    public List<Task> findByPriority(Priority priority) {
        if (priority == null) return Collections.emptyList();
        String sql = BASE_SELECT_JOIN + "WHERE t.priority = ? ORDER BY t.id ASC";
        List<Task> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding tasks by priority: " + priority, e);
        }
        return list;
    }

    @Override
    public List<Task> findPriorityTasks(int limit) {
        String sql = BASE_SELECT_JOIN +
                "WHERE t.priority IN ('URGENT', 'HIGH') AND t.status NOT IN ('DONE', 'COMPLETED') " +
                "ORDER BY CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 ELSE 3 END, t.due_date ASC " +
                "LIMIT ?";

        List<Task> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving priority tasks.", e);
        }
        return list;
    }

    @Override
    public List<Task> findOverdueTasks() {
        String sql = BASE_SELECT_JOIN +
                "WHERE t.due_date < CURRENT_DATE AND t.status NOT IN ('DONE', 'COMPLETED') " +
                "ORDER BY t.due_date ASC";

        List<Task> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding overdue tasks.", e);
        }
        return list;
    }

    @Override
    public List<Task> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        String sql = BASE_SELECT_JOIN +
                "WHERE LOWER(t.title) LIKE ? OR LOWER(t.description) LIKE ? " +
                "ORDER BY t.id ASC";

        List<Task> list = new ArrayList<>();
        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error searching tasks by keyword: " + keyword, e);
        }
        return list;
    }

    @Override
    public int countByStatus(TaskStatus status) {
        if (status == null) return 0;
        String sql = "SELECT COUNT(*) FROM tasks WHERE status = ?";

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
            throw new DatabaseOperationException("Error counting tasks by status: " + status, e);
        }
    }

    @Override
    public boolean assignTask(Long taskId, Long assigneeId) {
        if (taskId == null) return false;
        String sql = "UPDATE tasks SET assignee_id = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (assigneeId != null) {
                ps.setLong(1, assigneeId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setLong(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error assigning task ID " + taskId + " to user ID " + assigneeId, e);
        }
    }

    // =========================================================================
    // Helper Methods: Parameter Binding & ResultSet Mapping
    // =========================================================================

    private void bindTaskParameters(PreparedStatement ps, Task entity) throws SQLException {
        ps.setLong(1, entity.getProjectId());
        ps.setString(2, entity.getTitle());
        ps.setString(3, entity.getDescription());
        ps.setString(4, entity.getStatus() != null ? entity.getStatus().name() : TaskStatus.TODO.name());
        ps.setString(5, entity.getPriority() != null ? entity.getPriority().name() : Priority.MEDIUM.name());

        if (entity.getAssignee() != null && entity.getAssignee().getId() != null) {
            ps.setLong(6, entity.getAssignee().getId());
        } else {
            ps.setNull(6, Types.BIGINT);
        }

        if (entity.getReporter() != null && entity.getReporter().getId() != null) {
            ps.setLong(7, entity.getReporter().getId());
        } else {
            ps.setNull(7, Types.BIGINT);
        }

        if (entity.getDueDate() != null) {
            ps.setDate(8, java.sql.Date.valueOf(entity.getDueDate()));
        } else {
            ps.setNull(8, Types.DATE);
        }

        ps.setInt(9, entity.getEstimatedHours());
        ps.setInt(10, entity.getLoggedHours());
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long projectId = rs.getLong("project_id");
        String projectName = rs.getString("pr_name");
        String title = rs.getString("title");
        String description = rs.getString("description");

        String statusStr = rs.getString("status");
        TaskStatus status = TaskStatus.TODO;
        if (statusStr != null) {
            try {
                status = TaskStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String priorityStr = rs.getString("priority");
        Priority priority = Priority.MEDIUM;
        if (priorityStr != null) {
            try {
                priority = Priority.valueOf(priorityStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // Hydrate assignee from join
        User assignee = null;
        Long assigneeId = rs.getObject("assignee_id", Long.class);
        if (assigneeId != null && rs.getString("a_username") != null) {
            assignee = new TeamMember(assigneeId, rs.getString("a_username"),
                    rs.getString("a_fullname"), rs.getString("a_email"),
                    rs.getString("a_dept"), rs.getString("a_avatar"), "Software Engineering", 30);
        }

        // Hydrate reporter from join
        User reporter = null;
        Long reporterId = rs.getObject("reporter_id", Long.class);
        if (reporterId != null && rs.getString("r_username") != null) {
            reporter = new ProjectManager(reporterId, rs.getString("r_username"),
                    rs.getString("r_fullname"), rs.getString("r_email"),
                    rs.getString("r_dept"), rs.getString("r_avatar"), 100000.0, "PMP");
        }

        java.sql.Date dueDateSql = rs.getDate("due_date");
        LocalDate dueDate = dueDateSql != null ? dueDateSql.toLocalDate() : null;

        int estimatedHours = rs.getInt("estimated_hours");
        int loggedHours = rs.getInt("logged_hours");

        Task task = new Task(id, projectId, projectName != null ? projectName : ("PRJ-" + projectId),
                title, description, status, priority, assignee, reporter, dueDate, estimatedHours, loggedHours);

        Timestamp createdAtSql = rs.getTimestamp("created_at");
        if (createdAtSql != null) {
            task.setCreatedAt(createdAtSql.toLocalDateTime());
        }

        return task;
    }
}
