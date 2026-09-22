package com.web.sms.dto.response;

public interface TransportReportRow {
    Long getStudentDbId();
    String getRollNumber();
    String getStudentName();
    String getDepartment();
    Integer getStudyYear();
    Integer getSemester();
    String getAccountStatus();
    String getBusNumber();
    String getBoardingPoint();
    Long getTotalAmount();
    Long getPaidAmount();
    Long getRemainingAmount();
    String getPaymentStatus();
    String getApplicationStatus();
    String getTransferStatus();
}
