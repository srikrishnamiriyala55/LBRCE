package com.web.sms.dto.response;

import com.web.sms.entity.Notification;
import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse fromNotification(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setType(n.getType());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
