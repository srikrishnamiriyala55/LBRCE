package com.web.sms.entity;
import com.web.sms.enums.TransferStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="transfer_request")
public class TransferRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    
    @ManyToOne
    @JoinColumn(name="current_bus_id", nullable=false)
    private Bus currentBus;
    
    @ManyToOne
    @JoinColumn(name="current_boarding_point_id")
    private BoardingPoints currentBoardingPoint;
    
    @ManyToOne
    @JoinColumn(name="requested_bus_id", nullable=false)
    private Bus requestedBus;
    
    @ManyToOne
    @JoinColumn(name="requested_boarding_point_id", nullable=false)
    private BoardingPoints requestedBoardingPoint;
    
    @ManyToOne
    @JoinColumn(name="academic_year_id", nullable=false)
    private AcademicYear academicYear;
    
    @Enumerated(EnumType.STRING)
    private TransferStatus status = TransferStatus.TRANSFER_REQUESTED;
    
    @Column(length=500)
    private String reason;
    
    @Column(length=500)
    private String remarks;
    
    private LocalDateTime requestedAt;
    @ManyToOne
    @JoinColumn(name="old_incharge_id")
    private Incharge oldIncharge;
    @ManyToOne
    @JoinColumn(name="new_incharge_id")
    private Incharge newIncharge;
    private LocalDateTime oldInchargeDecisionAt;
    private LocalDateTime newInchargeDecisionAt;
    private String oldInchargeRemarks;
    private String newInchargeRemarks;
    private LocalDateTime completedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Bus getCurrentBus() { return currentBus; }
    public void setCurrentBus(Bus currentBus) { this.currentBus = currentBus; }
    public BoardingPoints getCurrentBoardingPoint() { return currentBoardingPoint; }
    public void setCurrentBoardingPoint(BoardingPoints currentBoardingPoint) { this.currentBoardingPoint = currentBoardingPoint; }
    public Bus getRequestedBus() { return requestedBus; }
    public void setRequestedBus(Bus requestedBus) { this.requestedBus = requestedBus; }
    public BoardingPoints getRequestedBoardingPoint() { return requestedBoardingPoint; }
    public void setRequestedBoardingPoint(BoardingPoints requestedBoardingPoint) { this.requestedBoardingPoint = requestedBoardingPoint; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public Incharge getOldIncharge() { return oldIncharge; }
    public void setOldIncharge(Incharge oldIncharge) { this.oldIncharge = oldIncharge; }
    public Incharge getNewIncharge() { return newIncharge; }
    public void setNewIncharge(Incharge newIncharge) { this.newIncharge = newIncharge; }
    public LocalDateTime getOldInchargeDecisionAt() { return oldInchargeDecisionAt; }
    public void setOldInchargeDecisionAt(LocalDateTime value) { this.oldInchargeDecisionAt = value; }
    public LocalDateTime getNewInchargeDecisionAt() { return newInchargeDecisionAt; }
    public void setNewInchargeDecisionAt(LocalDateTime value) { this.newInchargeDecisionAt = value; }
    public String getOldInchargeRemarks() { return oldInchargeRemarks; }
    public void setOldInchargeRemarks(String value) { this.oldInchargeRemarks = value; }
    public String getNewInchargeRemarks() { return newInchargeRemarks; }
    public void setNewInchargeRemarks(String value) { this.newInchargeRemarks = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
