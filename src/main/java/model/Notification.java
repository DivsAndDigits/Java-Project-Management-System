package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Domain entity representing in-app user notifications.
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type; // info, success, warning, danger
    private boolean read;
    private LocalDateTime createdAt;
    private String timeAgo;
    private String link;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
        this.type = "info";
    }

    public Notification(Long id, Long userId, String title, String message, String type,
                        boolean read, LocalDateTime createdAt, String timeAgo, String link) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.timeAgo = timeAgo != null ? timeAgo : "Just now";
        this.link = link;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
