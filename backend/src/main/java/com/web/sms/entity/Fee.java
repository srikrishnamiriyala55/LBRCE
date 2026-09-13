package com.web.sms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="fee")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="student_id", nullable=false)
    private Student student;
    
    @ManyToOne
    @JoinColumn(name="academic_year_id", nullable=false)
    private AcademicYear academicYear;
    
    @OneToOne
    @JoinColumn(name="allocation_id")
    private TransportAllocation allocation;
    
    @Column(nullable=false)
    private Long totalAmount;
    
    @Column(columnDefinition = "bigint default 0")
    private Long paidAmount = 0L;
    
    @Column(columnDefinition = "varchar(255) default 'PENDING'")
    private String status = "PENDING";
    
    @Column(columnDefinition = "boolean default false")
    private boolean passEligible = false;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public double getPaidPercentage() {
        if (totalAmount != null && totalAmount > 0) {
            return (paidAmount * 100.0) / totalAmount;
        }
        return 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public TransportAllocation getAllocation() { return allocation; }
    public void setAllocation(TransportAllocation allocation) { this.allocation = allocation; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPassEligible() { return passEligible; }
    public void setPassEligible(boolean passEligible) { this.passEligible = passEligible; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
