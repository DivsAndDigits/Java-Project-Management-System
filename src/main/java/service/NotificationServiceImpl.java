package service;

import dao.NotificationDao;
import exception.ValidationException;
import model.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * [OOP Concept: Polymorphism]
 * Implementation of NotificationService coordinating notification persistence and multi-channel dispatch.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    // [OOP Concept: Polymorphism]
    // List of polymorphic NotificationChannel implementations
    private final List<NotificationChannel> channels;

    public NotificationServiceImpl(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
        this.channels = new ArrayList<>();

        // Register default delivery channels
        this.channels.add(new InAppNotificationChannel(notificationDao));
        this.channels.add(new EmailNotificationChannel());
        this.channels.add(new AuditLogNotificationChannel());
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationDao.findByUserId(userId);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationDao.findAll();
    }

    @Override
    public Notification createNotification(Notification notification) {
        if (notification == null) {
            throw new ValidationException("Notification cannot be null.");
        }
        return notificationDao.save(notification);
    }

    @Override
    public int getUnreadNotificationCount(Long userId) {
        return notificationDao.countUnreadByUserId(userId);
    }

    @Override
    public boolean markNotificationAsRead(Long notificationId) {
        if (notificationId == null) {
            throw new ValidationException("Notification ID cannot be null.");
        }
        return notificationDao.markAsRead(notificationId);
    }

    @Override
    public void markAllNotificationsAsRead(Long userId) {
        notificationDao.markAllAsRead(userId);
    }

    @Override
    public void sendNotification(Notification notification) {
        if (notification == null) {
            throw new ValidationException("Notification cannot be null.");
        }

        // [OOP Concept: Polymorphism]
        // Dispatches to InAppNotificationChannel, EmailNotificationChannel, AuditLogNotificationChannel
        for (NotificationChannel channel : channels) {
            channel.deliver(notification);
        }
    }
}
