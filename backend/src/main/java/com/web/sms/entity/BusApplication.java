package com.web.sms.entity;
import com.web.sms.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="bus_application")
public class BusApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    
    @ManyToOne
    @JoinColumn(name="bus_id", nullable=false)
    private Bus bus;
    
    @ManyToOne
    @JoinColumn(name="boarding_point_id", nullable=false)
    private BoardingPoints boardingPoint;
    
    @ManyToOne
    @JoinColumn(name="academic_year_id", nullable=false)
    private AcademicYear academicYear;
    
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    @Column(length=500)
    private String remarks;
    
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        appliedAt = now;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public BoardingPoints getBoardingPoint() { return boardingPoint; }
    public void setBoardingPoint(BoardingPoints boardingPoint) { this.boardingPoint = boardingPoint; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
