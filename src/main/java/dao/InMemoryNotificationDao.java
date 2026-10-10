package dao;

import model.Notification;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [OOP Concept: Inheritance]
 * Thread-safe In-Memory implementation of NotificationDao extending AbstractInMemoryDao<Notification, Long>.
 */
public class InMemoryNotificationDao extends AbstractInMemoryDao<Notification, Long> implements NotificationDao {

    public InMemoryNotificationDao() {
        seedInitialData();
    }

    private void seedInitialData() {
        storage.put(1L, new Notification(1L, 1L, "Critical Task Assigned",
                "Elena assigned you to review the Disaster Recovery failover drill for PRJ-101.",
                "danger", false, LocalDateTime.now().minusMinutes(25), "25m ago", "tasks"));

        storage.put(2L, new Notification(2L, 1L, "Sprint Milestone Reached",
                "Project 'ISO-27001 Security Audit' has been marked as 100% Completed.",
                "success", false, LocalDateTime.now().minusHours(2), "2h ago", "projects"));

        storage.put(3L, new Notification(3L, 1L, "New Code Review Requested",
                "Priya Sharma submitted automated regression test report for review.",
                "info", true, LocalDateTime.now().minusHours(5), "5h ago", "tasks"));

        storage.put(4L, new Notification(4L, 1L, "Budget Alert",
                "PRJ-101 (Cloud Infrastructure Modernization) reached 61% of total budget allocation.",
                "warning", false, LocalDateTime.now().minusDays(1), "1d ago", "projects"));
    }

    @Override
    protected Long getEntityId(Notification entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(Notification entity, Long id) {
        entity.setId(id);
    }

    @Override
    public List<Notification> findAll() {
        return storage.values().stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(n -> userId == null || userId.equals(n.getUserId()))
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public int countUnreadByUserId(Long userId) {
        return (int) storage.values().stream()
                .filter(n -> (userId == null || userId.equals(n.getUserId())) && !n.isRead())
                .count();
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        Notification notification = storage.get(notificationId);
        if (notification != null) {
            notification.setRead(true);
            return true;
        }
        return false;
    }

    @Override
    public void markAllAsRead(Long userId) {
        storage.values().stream()
                .filter(n -> userId == null || userId.equals(n.getUserId()))
                .forEach(n -> n.setRead(true));
    }
}
