package dao;

import exception.DatabaseOperationException;
import model.User;
import util.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ==============================================================================
 * Database Operations: JDBC Implementation of TeamMemberDao (7 marks foundation)
 * ==============================================================================
 * 
 * Concrete JDBC implementation of TeamMemberDao managing project membership in MySQL.
 * 
 * Key Features:
 * 1. Leverages MySQL PRIMARY KEY (project_id, user_id) to strictly prevent duplicate memberships.
 * 2. Uses PreparedStatement for all parameter binding.
 * 3. Performs SQL INNER JOIN between 'team_members' and 'users' to efficiently load team members.
 * 4. Catches duplicate key violations cleanly and returns false rather than crashing.
 */
public class JdbcTeamMemberDao implements TeamMemberDao {

    private final DatabaseConnectionManager connectionManager;

    public JdbcTeamMemberDao() {
        this(DatabaseConnectionManager.getInstance());
    }

    public JdbcTeamMemberDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager != null ? connectionManager : DatabaseConnectionManager.getInstance();
    }

    private Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    @Override
    public boolean addTeamMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        String sql = "INSERT INTO team_members (project_id, user_id) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            ps.setLong(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // MySQL error code 1062 = Duplicate entry, SQLState 23000 = Integrity constraint violation
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                // Duplicate membership rejected safely by database constraint
                return false;
            }
            throw new DatabaseOperationException("Error adding team member user " + userId + " to project " + projectId, e);
        }
    }

    @Override
    public boolean removeTeamMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        String sql = "DELETE FROM team_members WHERE project_id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            ps.setLong(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error removing team member user " + userId + " from project " + projectId, e);
        }
    }

    @Override
    public Set<User> findProjectTeam(Long projectId) {
        if (projectId == null) return new LinkedHashSet<>();
        String sql = "SELECT u.* FROM users u " +
                "INNER JOIN team_members tm ON u.id = tm.user_id " +
                "WHERE tm.project_id = ? " +
                "ORDER BY u.full_name ASC";

        Set<User> team = new LinkedHashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    team.add(JdbcUserDao.mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving team members for project: " + projectId, e);
        }
        return team;
    }

    @Override
    public boolean isMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        String sql = "SELECT 1 FROM team_members WHERE project_id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking membership for user " + userId + " in project " + projectId, e);
        }
    }

    @Override
    public int countByProjectId(Long projectId) {
        if (projectId == null) return 0;
        String sql = "SELECT COUNT(*) FROM team_members WHERE project_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting members for project: " + projectId, e);
        }
    }

    @Override
    public void removeAllForProject(Long projectId) {
        if (projectId == null) return;
        String sql = "DELETE FROM team_members WHERE project_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, projectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error removing all members for project: " + projectId, e);
        }
    }
}
