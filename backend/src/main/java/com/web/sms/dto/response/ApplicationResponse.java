package com.web.sms.dto.response;

import com.web.sms.enums.ApplicationStatus;
import com.web.sms.entity.BusApplication;
import java.time.LocalDateTime;

public class ApplicationResponse {
    private Long id;
    private Long busId;
    private Long boardingPointId;
    private Long studentId;
    private String busNumber;
    private String startingPoint;
    private String endingPoint;
    private String boardingPointName;
    private String academicYear;
    private ApplicationStatus status;
    private String remarks;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String studentName;
    private String rollNumber;

    public static ApplicationResponse fromApplication(BusApplication app) {
        ApplicationResponse r = new ApplicationResponse();
        r.setId(app.getId());
        if(app.getBus() != null) { r.setBusId(app.getBus().getId()); r.setBusNumber(app.getBus().getBusNumber()); }
        if(app.getBus() != null) {
            r.setStartingPoint(app.getBus().getStartingPoint());
            r.setEndingPoint(app.getBus().getEndingPoint());
        }
        if(app.getBoardingPoint() != null) { r.setBoardingPointId(app.getBoardingPoint().getId()); r.setBoardingPointName(app.getBoardingPoint().getStationName()); }
        if(app.getAcademicYear() != null) r.setAcademicYear(app.getAcademicYear().getYearName());
        r.setStatus(app.getStatus());
        r.setRemarks(app.getRemarks());
        r.setAppliedAt(app.getAppliedAt());
        r.setReviewedAt(app.getReviewedAt());
        if (app.getStudent() != null) { r.setStudentId(app.getStudent().getId()); r.setStudentName(app.getStudent().getName()); r.setRollNumber(app.getStudent().getRollNumber()); }
        return r;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBusId() { return busId; }
    public void setBusId(Long v) { busId=v; }
    public Long getBoardingPointId() { return boardingPointId; }
    public void setBoardingPointId(Long v) { boardingPointId=v; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long v) { studentId=v; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getStartingPoint() { return startingPoint; }
    public void setStartingPoint(String startingPoint) { this.startingPoint = startingPoint; }
    public String getEndingPoint() { return endingPoint; }
    public void setEndingPoint(String endingPoint) { this.endingPoint = endingPoint; }
    public String getBoardingPointName() { return boardingPointName; }
    public void setBoardingPointName(String boardingPointName) { this.boardingPointName = boardingPointName; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String v) { studentName=v; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String v) { rollNumber=v; }
}
