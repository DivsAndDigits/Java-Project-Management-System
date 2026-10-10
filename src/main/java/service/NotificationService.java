package service;

import model.Notification;
import java.util.List;

/**
 * [OOP Concept: Interface]
 * Service contract for User notifications, system alerts, and multi-channel dispatch.
 */
public interface NotificationService {

    List<Notification> getUserNotifications(Long userId);

    List<Notification> getAllNotifications();

    Notification createNotification(Notification notification);

    int getUnreadNotificationCount(Long userId);

    boolean markNotificationAsRead(Long notificationId);

    void markAllNotificationsAsRead(Long userId);

    /**
     * [OOP Concept: Polymorphism]
     * Dispatches notification polymorphically across registered notification channels.
     *
     * @param notification notification to dispatch
     */
    void sendNotification(Notification notification);
}
