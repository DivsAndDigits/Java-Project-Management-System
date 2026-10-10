package service;

import model.Notification;

/**
 * [OOP Concept: Interface]
 * Abstraction defining notification dispatch channels (In-App, Email, Audit Log).
 * Demonstrates runtime polymorphism when notifications are delivered across channels.
 */
public interface NotificationChannel {

    /**
     * Unique identifier for the notification delivery channel.
     */
    String getChannelName();

    /**
     * [OOP Concept: Polymorphism]
     * Delivers notification payload polymorphically according to the channel mechanics.
     *
     * @param notification the alert to send
     * @return true if delivery was acknowledged
     */
    boolean deliver(Notification notification);
}
