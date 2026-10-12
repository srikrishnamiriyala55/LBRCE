package com.web.sms.dto.response;

import com.web.sms.entity.Payment;
import com.web.sms.enums.PaymentStatus;
import java.time.LocalDateTime;

public class PaymentResponse {
    private Long id;
    private String orderId;
    private Long amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String studentName;
    private String rollNumber;
    private String academicYear;
    private String transactionId;
    private String gatewayName;

    public static PaymentResponse fromPayment(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setAmount(p.getAmount());
        r.setStatus(p.getStatus());
        r.setPaidAt(p.getPaidAt());
        r.setCreatedAt(p.getCreatedAt());
        r.setTransactionId(p.getTransactionId());
        r.setGatewayName(p.getGatewayName());
        if(p.getStudent()!=null){r.setStudentName(p.getStudent().getName());r.setRollNumber(p.getStudent().getRollNumber());}
        if(p.getFee()!=null && p.getFee().getAcademicYear()!=null){r.setAcademicYear(p.getFee().getAcademicYear().getYearName());}
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStudentName(){return studentName;} public void setStudentName(String v){studentName=v;}
    public String getRollNumber(){return rollNumber;} public void setRollNumber(String v){rollNumber=v;}
    public String getAcademicYear(){return academicYear;} public void setAcademicYear(String v){academicYear=v;}
    public String getTransactionId(){return transactionId;} public void setTransactionId(String v){transactionId=v;}
    public String getGatewayName(){return gatewayName;} public void setGatewayName(String v){gatewayName=v;}
}
