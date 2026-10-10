package dao;

import exception.DatabaseOperationException;
import model.Notification;
import util.DatabaseConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ==============================================================================
 * Database Operations: JDBC Implementation of NotificationDao (7 marks foundation)
 * ==============================================================================
 * 
 * Concrete JDBC implementation of NotificationDao communicating with MySQL 8.x.
 * 
 * Key Features:
 * 1. Parameterized queries via PreparedStatement preventing SQL injection.
 * 2. try-with-resources resource management closing Connection, PreparedStatement, and ResultSet.
 * 3. Supports markAsRead and markAllAsRead status updates.
 * 4. Exception Translation: converts SQLExceptions to DatabaseOperationException.
 */
public class JdbcNotificationDao implements NotificationDao {

    private final DatabaseConnectionManager connectionManager;

    public JdbcNotificationDao() {
        this(DatabaseConnectionManager.getInstance());
    }

    public JdbcNotificationDao(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager != null ? connectionManager : DatabaseConnectionManager.getInstance();
    }

    private Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    @Override
    public Optional<Notification> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM notifications WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding notification by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Notification> findAll() {
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all notifications.", e);
        }
        return list;
    }

    @Override
    public Map<Long, Notification> findAllAsMap() {
        List<Notification> list = findAll();
        Map<Long, Notification> map = new LinkedHashMap<>();
        for (Notification n : list) {
            if (n.getId() != null) {
                map.put(n.getId(), n);
            }
        }
        return map;
    }

    @Override
    public Notification save(Notification entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Notification entity cannot be null.");
        }

        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, action_link, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, entity.getUserId());
            ps.setString(2, entity.getTitle());
            ps.setString(3, entity.getMessage());
            ps.setString(4, entity.getType() != null ? entity.getType() : "info");
            ps.setBoolean(5, entity.isRead());
            ps.setString(6, entity.getLink());

            LocalDateTime created = entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(created));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving notification: " + entity.getTitle(), e);
        }
    }

    @Override
    public boolean update(Notification entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        String sql = "UPDATE notifications SET user_id = ?, title = ?, message = ?, type = ?, " +
                "is_read = ?, action_link = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, entity.getUserId());
            ps.setString(2, entity.getTitle());
            ps.setString(3, entity.getMessage());
            ps.setString(4, entity.getType());
            ps.setBoolean(5, entity.isRead());
            ps.setString(6, entity.getLink());
            ps.setLong(7, entity.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating notification ID: " + entity.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM notifications WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting notification ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM notifications WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking notification existence for ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM notifications";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting notifications.", e);
        }
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding notifications for user ID: " + userId, e);
        }
        return list;
    }

    @Override
    public int countUnreadByUserId(Long userId) {
        if (userId == null) return 0;
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error counting unread notifications for user ID: " + userId, e);
        }
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        if (notificationId == null) return false;
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error marking notification " + notificationId + " as read", e);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        if (userId == null) return;
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error marking all notifications as read for user: " + userId, e);
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getLong("user_id");
        String title = rs.getString("title");
        String message = rs.getString("message");
        String type = rs.getString("type");
        boolean isRead = rs.getBoolean("is_read");
        String link = rs.getString("action_link");

        Timestamp createdAtSql = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtSql != null ? createdAtSql.toLocalDateTime() : LocalDateTime.now();

        return new Notification(id, userId, title, message, type != null ? type : "info",
                isRead, createdAt, "Just now", link);
    }
}
