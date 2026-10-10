package service;

import model.Notification;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Channel simulating asynchronous email dispatch for high-priority alerts.
 */
public class EmailNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public boolean deliver(Notification notification) {
        if (notification == null) return false;
        // Simulates external SMTP dispatch for urgent notifications
        if ("danger".equalsIgnoreCase(notification.getType()) || "warning".equalsIgnoreCase(notification.getType())) {
            System.out.printf("[EmailNotificationChannel] Sending Email to User #%d | Subject: %s%n",
                    notification.getUserId(), notification.getTitle());
        }
        return true;
    }
}
