package com.web.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Year-specific ownership used to authorize and reproduce historical data. */
@Entity
@Table(name="bus_incharge_assignment", uniqueConstraints =
        @UniqueConstraint(name="uk_bus_incharge_year", columnNames={"bus_id","incharge_id","academic_year_id"}))
public class BusInchargeAssignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="bus_id",nullable=false) private Bus bus;
    @ManyToOne(optional=false) @JoinColumn(name="incharge_id",nullable=false) private Incharge incharge;
    @ManyToOne(optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
    @Column(nullable=false) private LocalDateTime assignedAt;
    private LocalDateTime endedAt;
    @Column(nullable=false,length=20) private String status="ACTIVE";
    @Column(length=100) private String assignedBy;
    @PrePersist void create(){if(assignedAt==null)assignedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Bus getBus(){return bus;} public void setBus(Bus bus){this.bus=bus;}
    public Incharge getIncharge(){return incharge;} public void setIncharge(Incharge incharge){this.incharge=incharge;}
    public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear value){this.academicYear=value;}
    public LocalDateTime getAssignedAt(){return assignedAt;} public void setAssignedAt(LocalDateTime value){this.assignedAt=value;}
    public LocalDateTime getEndedAt(){return endedAt;} public void setEndedAt(LocalDateTime value){this.endedAt=value;}
    public String getStatus(){return status;} public void setStatus(String value){this.status=value;}
    public String getAssignedBy(){return assignedBy;} public void setAssignedBy(String value){this.assignedBy=value;}
}
