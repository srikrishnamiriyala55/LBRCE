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
    private String email;
    private String phoneNumber;
    private String branch;
    private Integer year;
    private Integer semester;
    private Long totalFee;
    private Long paidTillNow;
    private Long pendingAmount;

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
        if(p.getStudent()!=null){
            r.setStudentName(p.getStudent().getName());r.setRollNumber(p.getStudent().getRollNumber());
            r.setEmail(p.getStudent().getEmail());r.setPhoneNumber(p.getStudent().getPhoneNumber());
            r.setBranch(p.getStudent().getBranch());r.setYear(p.getStudent().getYear());r.setSemester(p.getStudent().getSemester());
        }
        if(p.getFee()!=null){
            if(p.getFee().getAcademicYear()!=null)r.setAcademicYear(p.getFee().getAcademicYear().getYearName());
            long total=p.getFee().getTotalAmount()==null?0L:p.getFee().getTotalAmount();
            long paid=p.getFee().getPaidAmount()==null?0L:p.getFee().getPaidAmount();
            r.setTotalFee(total);r.setPaidTillNow(paid);r.setPendingAmount(Math.max(0L,total-paid));
        }
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
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
    public String getBranch(){return branch;} public void setBranch(String v){branch=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
    public Long getTotalFee(){return totalFee;} public void setTotalFee(Long v){totalFee=v;}
    public Long getPaidTillNow(){return paidTillNow;} public void setPaidTillNow(Long v){paidTillNow=v;}
    public Long getPendingAmount(){return pendingAmount;} public void setPendingAmount(Long v){pendingAmount=v;}
}
