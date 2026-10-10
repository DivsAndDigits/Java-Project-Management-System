package service;

import model.Notification;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Channel recording notification broadcast events into system audit trails.
 */
public class AuditLogNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "AUDIT_LOG";
    }

    @Override
    public boolean deliver(Notification notification) {
        if (notification == null) return false;
        System.out.printf("[AuditLogNotificationChannel] AUDIT: Notification #%d issued for User #%d at %s%n",
                notification.getId() != null ? notification.getId() : 0, notification.getUserId(), notification.getCreatedAt());
        return true;
    }
}
