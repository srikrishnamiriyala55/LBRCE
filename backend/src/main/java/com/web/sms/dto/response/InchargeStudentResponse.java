package com.web.sms.dto.response;
import com.web.sms.entity.*;

public class InchargeStudentResponse {
 public Long allocationId; public Long studentId; public String name; public String rollNumber; public String email; public String phoneNumber; public String dob; public String gender; public String address; public String branch; public Integer year; public Integer semester; public String bloodGroup; public String parentName; public String parentPhoneNumber;
 public String boardingPoint; public String applicationStatus; public String transportationStatus="ACTIVE";
 public Long requiredFee=0L; public Long paidAmount=0L; public Long remainingFee=0L; public String feeStatus="PENDING";
 public String passStatus="INACTIVE";
 public static InchargeStudentResponse from(TransportAllocation a,Fee f,BusPass p){InchargeStudentResponse r=new InchargeStudentResponse();Student s=a.getStudent();r.allocationId=a.getId();r.studentId=s.getId();r.name=s.getName();r.rollNumber=s.getRollNumber();r.email=s.getEmail();r.phoneNumber=s.getPhoneNumber();r.dob=s.getDob()==null?null:s.getDob().toString();r.gender=s.getGender();r.address=s.getAddress();r.branch=s.getBranch();r.year=s.getYear();r.semester=s.getSemester();r.bloodGroup=s.getBloodGroup();r.parentName=s.getParentName();r.parentPhoneNumber=s.getParentPhoneNumber();r.boardingPoint=a.getBoardingPoint().getStationName();r.applicationStatus=a.getApplication()!=null?a.getApplication().getStatus().name():"APPROVED";if(f!=null){r.requiredFee=f.getTotalAmount();r.paidAmount=f.getPaidAmount()==null?0:f.getPaidAmount();r.remainingFee=Math.max(0,r.requiredFee-r.paidAmount);r.feeStatus=f.getStatus();}if(p!=null)r.passStatus=p.getStatus().name();return r;}
}
