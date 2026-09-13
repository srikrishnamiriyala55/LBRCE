package com.web.sms.entity;
import com.web.sms.enums.EntityStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="route")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique=true, nullable=false)
    private String routeName;
    
    private String startingPoint;
    private String endingPoint;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getStartingPoint() { return startingPoint; }
    public void setStartingPoint(String startingPoint) { this.startingPoint = startingPoint; }
    public String getEndingPoint() { return endingPoint; }
    public void setEndingPoint(String endingPoint) { this.endingPoint = endingPoint; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EntityStatus getStatus() { return status; }
    public void setStatus(EntityStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
