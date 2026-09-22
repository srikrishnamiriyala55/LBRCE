package com.web.sms.entity;
import com.web.sms.enums.EntityStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="transport_allocation")
public class TransportAllocation {
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
    
    @OneToOne
    @JoinColumn(name="application_id")
    private BusApplication application;
    
    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;
    
    private LocalDateTime allocatedAt;
    private LocalDateTime deactivatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer seatNumber;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

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
    public BusApplication getApplication() { return application; }
    public void setApplication(BusApplication application) { this.application = application; }
    public EntityStatus getStatus() { return status; }
    public void setStatus(EntityStatus status) { this.status = status; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
    public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
