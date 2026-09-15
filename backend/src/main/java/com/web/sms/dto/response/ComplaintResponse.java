package com.web.sms.dto.response;

import com.web.sms.entity.Complaint;
import com.web.sms.enums.ComplaintCategory;
import com.web.sms.enums.ComplaintStatus;
import java.time.LocalDateTime;

public class ComplaintResponse {
    private Long id;
    private String busNumber;
    private String studentName;
    private String rollNumber;
    private ComplaintCategory category;
    private String subject;
    private String description;
    private ComplaintStatus status;
    private String response;
    private String respondedBy;
    private LocalDateTime createdAt;

    public static ComplaintResponse fromComplaint(Complaint c) {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(c.getId());
        if(c.getStudent() != null) {
            r.setStudentName(c.getStudent().getName());
            r.setRollNumber(c.getStudent().getRollNumber());
        }
        if(c.getBus() != null) r.setBusNumber(c.getBus().getBusNumber());
        r.setCategory(c.getCategory());
        r.setSubject(c.getSubject());
        r.setDescription(c.getDescription());
        r.setStatus(c.getStatus());
        r.setResponse(c.getResponse());
        r.setRespondedBy(c.getRespondedBy());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getRespondedBy() { return respondedBy; }
    public void setRespondedBy(String respondedBy) { this.respondedBy = respondedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
