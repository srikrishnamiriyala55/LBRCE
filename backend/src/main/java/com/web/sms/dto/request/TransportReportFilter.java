package com.web.sms.dto.request;

public class TransportReportFilter {
    private String busNumber;
    private String name;
    private String rollNumber;
    private Integer year;
    private Integer semester;
    private String paymentStatus;
    private String accountStatus;
    private String applicationStatus;
    private String transferStatus;
    private String department;
    private Long minimumRemaining;

    public String getBusNumber(){return busNumber;} public void setBusNumber(String v){busNumber=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getRollNumber(){return rollNumber;} public void setRollNumber(String v){rollNumber=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
    public String getAccountStatus(){return accountStatus;} public void setAccountStatus(String v){accountStatus=v;}
    public String getApplicationStatus(){return applicationStatus;} public void setApplicationStatus(String v){applicationStatus=v;}
    public String getTransferStatus(){return transferStatus;} public void setTransferStatus(String v){transferStatus=v;}
    public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
    public Long getMinimumRemaining(){return minimumRemaining;} public void setMinimumRemaining(Long v){minimumRemaining=v;}
}
