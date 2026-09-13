package com.web.sms.dto.response;

import com.web.sms.enums.ApplicationStatus;
import com.web.sms.entity.BusApplication;
import java.time.LocalDateTime;

public class ApplicationResponse {
    private Long id;
    private String busNumber;
    private String routeName;
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
        if(app.getBus() != null) r.setBusNumber(app.getBus().getBusNumber());
        if(app.getBus() != null && app.getBus().getRoute() != null) r.setRouteName(app.getBus().getRoute().getRouteName());
        if(app.getBoardingPoint() != null) r.setBoardingPointName(app.getBoardingPoint().getStationName());
        if(app.getAcademicYear() != null) r.setAcademicYear(app.getAcademicYear().getYearName());
        r.setStatus(app.getStatus());
        r.setRemarks(app.getRemarks());
        r.setAppliedAt(app.getAppliedAt());
        r.setReviewedAt(app.getReviewedAt());
        if (app.getStudent() != null) { r.setStudentName(app.getStudent().getName()); r.setRollNumber(app.getStudent().getRollNumber()); }
        return r;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
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
