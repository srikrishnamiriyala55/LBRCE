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

    // Immutable values captured when the allocation is created. Historical
    // passes and reports must not change when master data is edited later.
    private String studentNameSnapshot;
    private String rollNumberSnapshot;
    private String branchSnapshot;
    private Integer studyYearSnapshot;
    private Integer semesterSnapshot;
    private String phoneNumberSnapshot;
    private String busNumberSnapshot;
    private String startingPointSnapshot;
    private String endingPointSnapshot;
    private String boardingPointSnapshot;
    private Long annualFeeSnapshot;
    private String inchargeNameSnapshot;
    private String inchargeTeacherIdSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (allocatedAt == null) allocatedAt = now;
        if (updatedAt == null) updatedAt = now;
    }
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
    public String getStudentNameSnapshot() { return studentNameSnapshot; }
    public void setStudentNameSnapshot(String value) { this.studentNameSnapshot = value; }
    public String getRollNumberSnapshot() { return rollNumberSnapshot; }
    public void setRollNumberSnapshot(String value) { this.rollNumberSnapshot = value; }
    public String getBranchSnapshot() { return branchSnapshot; }
    public void setBranchSnapshot(String value) { this.branchSnapshot = value; }
    public Integer getStudyYearSnapshot() { return studyYearSnapshot; }
    public void setStudyYearSnapshot(Integer value) { this.studyYearSnapshot = value; }
    public Integer getSemesterSnapshot() { return semesterSnapshot; }
    public void setSemesterSnapshot(Integer value) { this.semesterSnapshot = value; }
    public String getPhoneNumberSnapshot() { return phoneNumberSnapshot; }
    public void setPhoneNumberSnapshot(String value) { this.phoneNumberSnapshot = value; }
    public String getBusNumberSnapshot() { return busNumberSnapshot; }
    public void setBusNumberSnapshot(String value) { this.busNumberSnapshot = value; }
    public String getStartingPointSnapshot() { return startingPointSnapshot; }
    public void setStartingPointSnapshot(String value) { this.startingPointSnapshot = value; }
    public String getEndingPointSnapshot() { return endingPointSnapshot; }
    public void setEndingPointSnapshot(String value) { this.endingPointSnapshot = value; }
    public String getBoardingPointSnapshot() { return boardingPointSnapshot; }
    public void setBoardingPointSnapshot(String value) { this.boardingPointSnapshot = value; }
    public Long getAnnualFeeSnapshot() { return annualFeeSnapshot; }
    public void setAnnualFeeSnapshot(Long value) { this.annualFeeSnapshot = value; }
    public String getInchargeNameSnapshot() { return inchargeNameSnapshot; }
    public void setInchargeNameSnapshot(String value) { this.inchargeNameSnapshot = value; }
    public String getInchargeTeacherIdSnapshot() { return inchargeTeacherIdSnapshot; }
    public void setInchargeTeacherIdSnapshot(String value) { this.inchargeTeacherIdSnapshot = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
