package com.web.sms.dto.response;

import com.web.sms.entity.TransferRequest;
import com.web.sms.enums.TransferStatus;
import java.time.LocalDateTime;

public class TransferResponse {
    private Long id;
    private String currentBusNumber;
    private String requestedBusNumber;
    private String currentBoardingPoint;
    private String requestedBoardingPoint;
    private TransferStatus status;
    private String reason;
    private String remarks;
    private String studentName;
    private String studentRollNumber;
    private String currentStartingPoint;
    private String currentEndingPoint;
    private String requestedStartingPoint;
    private String requestedEndingPoint;
    private String oldInchargeName;
    private String newInchargeName;
    private String academicYear;
    private Integer requestedAvailableSeats;
    private LocalDateTime oldInchargeDecisionAt;
    private LocalDateTime newInchargeDecisionAt;
    private LocalDateTime completedAt;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static TransferResponse fromTransfer(TransferRequest t) {
        TransferResponse r = new TransferResponse();
        r.setId(t.getId());
        if(t.getCurrentBus() != null) r.setCurrentBusNumber(t.getCurrentBus().getBusNumber());
        if(t.getRequestedBus() != null) r.setRequestedBusNumber(t.getRequestedBus().getBusNumber());
        if(t.getCurrentBoardingPoint() != null) r.setCurrentBoardingPoint(t.getCurrentBoardingPoint().getStationName());
        if(t.getRequestedBoardingPoint() != null) r.setRequestedBoardingPoint(t.getRequestedBoardingPoint().getStationName());
        r.setStatus(t.getStatus());
        r.setReason(t.getReason());
        r.setRemarks(t.getRemarks());
        r.setRequestedAt(t.getRequestedAt());
        r.setProcessedAt(t.getProcessedAt());
        if(t.getStudent()!=null){r.setStudentName(t.getStudent().getName());r.setStudentRollNumber(t.getStudent().getRollNumber());}
        if (t.getCurrentBus() != null) {
            r.setCurrentStartingPoint(t.getCurrentBus().getStartingPoint());
            r.setCurrentEndingPoint(t.getCurrentBus().getEndingPoint());
        }
        if (t.getRequestedBus() != null) {
            r.setRequestedStartingPoint(t.getRequestedBus().getStartingPoint());
            r.setRequestedEndingPoint(t.getRequestedBus().getEndingPoint());
            r.setRequestedAvailableSeats(t.getRequestedBus().getAvailableSeats());
        }
        if (t.getOldIncharge() != null) r.setOldInchargeName(t.getOldIncharge().getName());
        else if(t.getCurrentBus()!=null&&t.getCurrentBus().getIncharge()!=null)r.setOldInchargeName(t.getCurrentBus().getIncharge().getName());
        if (t.getNewIncharge() != null) r.setNewInchargeName(t.getNewIncharge().getName());
        else if(t.getRequestedBus()!=null&&t.getRequestedBus().getIncharge()!=null)r.setNewInchargeName(t.getRequestedBus().getIncharge().getName());
        if(t.getAcademicYear()!=null)r.setAcademicYear(t.getAcademicYear().getYearName());
        r.setOldInchargeDecisionAt(t.getOldInchargeDecisionAt());
        r.setNewInchargeDecisionAt(t.getNewInchargeDecisionAt());
        r.setCompletedAt(t.getCompletedAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCurrentBusNumber() { return currentBusNumber; }
    public void setCurrentBusNumber(String currentBusNumber) { this.currentBusNumber = currentBusNumber; }
    public String getRequestedBusNumber() { return requestedBusNumber; }
    public void setRequestedBusNumber(String requestedBusNumber) { this.requestedBusNumber = requestedBusNumber; }
    public String getCurrentBoardingPoint() { return currentBoardingPoint; }
    public void setCurrentBoardingPoint(String currentBoardingPoint) { this.currentBoardingPoint = currentBoardingPoint; }
    public String getRequestedBoardingPoint() { return requestedBoardingPoint; }
    public void setRequestedBoardingPoint(String requestedBoardingPoint) { this.requestedBoardingPoint = requestedBoardingPoint; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String v) { studentName = v; }
    public String getStudentRollNumber() { return studentRollNumber; }
    public void setStudentRollNumber(String v) { studentRollNumber = v; }
    public String getCurrentStartingPoint() { return currentStartingPoint; }
    public void setCurrentStartingPoint(String v) { currentStartingPoint = v; }
    public String getCurrentEndingPoint() { return currentEndingPoint; }
    public void setCurrentEndingPoint(String v) { currentEndingPoint = v; }
    public String getRequestedStartingPoint() { return requestedStartingPoint; }
    public void setRequestedStartingPoint(String v) { requestedStartingPoint = v; }
    public String getRequestedEndingPoint() { return requestedEndingPoint; }
    public void setRequestedEndingPoint(String v) { requestedEndingPoint = v; }
    public String getOldInchargeName() { return oldInchargeName; }
    public void setOldInchargeName(String v) { oldInchargeName = v; }
    public String getNewInchargeName() { return newInchargeName; }
    public void setNewInchargeName(String v) { newInchargeName = v; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String v) { academicYear=v; }
    public Integer getRequestedAvailableSeats() { return requestedAvailableSeats; }
    public void setRequestedAvailableSeats(Integer v) { requestedAvailableSeats=v; }
    public LocalDateTime getOldInchargeDecisionAt() { return oldInchargeDecisionAt; }
    public void setOldInchargeDecisionAt(LocalDateTime v) { oldInchargeDecisionAt = v; }
    public LocalDateTime getNewInchargeDecisionAt() { return newInchargeDecisionAt; }
    public void setNewInchargeDecisionAt(LocalDateTime v) { newInchargeDecisionAt = v; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime v) { completedAt = v; }
}
