package com.web.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Academic progression for one student in one academic year. */
@Entity
@Table(name = "student_academic_details", uniqueConstraints = @UniqueConstraint(name = "uk_student_academic_year", columnNames = {"student_id", "academic_year_id"}))
public class StudentAcademicDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false) private AcademicYear academicYear;
    @Column(nullable = false, length = 20) private String branch;
    @Column(name = "study_year", nullable = false) private Integer year;
    @Column(nullable = false) private Integer semester;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; } public void setStudent(Student student) { this.student = student; }
    public AcademicYear getAcademicYear() { return academicYear; } public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public String getBranch() { return branch; } public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; } public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; } public void setSemester(Integer semester) { this.semester = semester; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
