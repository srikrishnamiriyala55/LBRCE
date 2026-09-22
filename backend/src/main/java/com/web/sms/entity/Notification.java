package com.web.sms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable=false)
    private String recipientRollNumber;
    
    @Column(nullable=false)
    private String title;
    
    @Column(length=1000, nullable=false)
    private String message;
    
    private String type;
    
    @Column(name="is_read", columnDefinition = "boolean default false")
    private boolean isRead = false;
    
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecipientRollNumber() { return recipientRollNumber; }
    public void setRecipientRollNumber(String recipientRollNumber) { this.recipientRollNumber = recipientRollNumber; }
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
