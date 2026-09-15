package com.web.sms.entity;
import com.web.sms.enums.ComplaintCategory;
import com.web.sms.enums.ComplaintStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="complaint")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    
    @ManyToOne
    @JoinColumn(name="bus_id", nullable=false)
    private Bus bus;
    
    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;
    
    @Column(nullable=false)
    private String subject;
    
    @Column(length=1000, nullable=false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status = ComplaintStatus.OPEN;
    
    @Column(length=1000)
    private String response;
    
    private String respondedBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getRespondedBy() { return respondedBy; }
    public void setRespondedBy(String respondedBy) { this.respondedBy = respondedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
