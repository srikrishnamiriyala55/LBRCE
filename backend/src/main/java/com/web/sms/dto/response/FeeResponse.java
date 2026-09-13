package com.web.sms.dto.response;

import com.web.sms.entity.Fee;

public class FeeResponse {
    private Long id;
    private String academicYear;
    private Long totalAmount;
    private Long paidAmount;
    private Long remainingAmount;
    private double paidPercentage;
    private String status;
    private boolean passEligible;
    private String rollNumber;
    private String studentName;

    public static FeeResponse fromFee(Fee f) {
        FeeResponse r = new FeeResponse();
        r.setId(f.getId());
        if(f.getAcademicYear() != null) r.setAcademicYear(f.getAcademicYear().getYearName());
        if(f.getStudent() != null) {
            r.setRollNumber(f.getStudent().getRollNumber());
            r.setStudentName(f.getStudent().getName());
        }
        r.setTotalAmount(f.getTotalAmount());
        r.setPaidAmount(f.getPaidAmount());
        r.setRemainingAmount(f.getTotalAmount() - (f.getPaidAmount() != null ? f.getPaidAmount() : 0L));
        r.setPaidPercentage(f.getPaidPercentage());
        r.setStatus(f.getStatus());
        r.setPassEligible(f.isPassEligible());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public Long getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Long remainingAmount) { this.remainingAmount = remainingAmount; }
    public double getPaidPercentage() { return paidPercentage; }
    public void setPaidPercentage(double paidPercentage) { this.paidPercentage = paidPercentage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPassEligible() { return passEligible; }
    public void setPassEligible(boolean passEligible) { this.passEligible = passEligible; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
