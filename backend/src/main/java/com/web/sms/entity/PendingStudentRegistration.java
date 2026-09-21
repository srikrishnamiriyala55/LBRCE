package com.web.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_student_registration")
public class PendingStudentRegistration {
    @Id
    @Column(length = 36)
    private String challengeId;
    @Column(nullable = false, unique = true, length = 30)
    private String rollNumber;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    @Column(nullable = false, length = 15)
    private String phoneNumber;
    @Column(nullable = false, length = 50)
    private String branch;
    @Column(nullable = false)
    private Integer year;
    @Column(nullable = false)
    private Integer semester;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
