package service;

import dao.NotificationDao;
import model.Notification;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Channel delivering in-app banner/bell notifications by persisting to NotificationDao.
 */
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationDao notificationDao;

    public InAppNotificationChannel(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    public boolean deliver(Notification notification) {
        if (notification == null) return false;
        notificationDao.save(notification);
        return true;
    }
}
