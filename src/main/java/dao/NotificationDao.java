package dao;

import model.Notification;
import java.util.List;

/**
 * Data Access Object contract for Notification entities.
 */
public interface NotificationDao extends BaseDao<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    int countUnreadByUserId(Long userId);

    boolean markAsRead(Long notificationId);

    void markAllAsRead(Long userId);
}
