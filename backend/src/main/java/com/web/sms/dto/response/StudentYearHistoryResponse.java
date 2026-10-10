package com.web.sms.dto.response;

import java.util.List;

public class StudentYearHistoryResponse {
    private Long academicYearId; private String academicYear; private boolean active;
    private String branch; private Integer year; private Integer semester; private String academicStatus;
    private List<ApplicationResponse> applications=List.of(); private List<AllocationHistoryResponse> allocations=List.of();
    private List<FeeResponse> fees=List.of(); private List<PaymentResponse> payments=List.of();
    private List<PassResponse> passes=List.of(); private List<TransferResponse> transfers=List.of();
    public Long getAcademicYearId(){return academicYearId;} public void setAcademicYearId(Long v){academicYearId=v;}
    public String getAcademicYear(){return academicYear;} public void setAcademicYear(String v){academicYear=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public String getBranch(){return branch;} public void setBranch(String v){branch=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
    public String getAcademicStatus(){return academicStatus;} public void setAcademicStatus(String v){academicStatus=v;}
    public List<ApplicationResponse> getApplications(){return applications;} public void setApplications(List<ApplicationResponse> v){applications=v;}
    public List<AllocationHistoryResponse> getAllocations(){return allocations;} public void setAllocations(List<AllocationHistoryResponse> v){allocations=v;}
    public List<FeeResponse> getFees(){return fees;} public void setFees(List<FeeResponse> v){fees=v;}
    public List<PaymentResponse> getPayments(){return payments;} public void setPayments(List<PaymentResponse> v){payments=v;}
    public List<PassResponse> getPasses(){return passes;} public void setPasses(List<PassResponse> v){passes=v;}
    public List<TransferResponse> getTransfers(){return transfers;} public void setTransfers(List<TransferResponse> v){transfers=v;}
}
