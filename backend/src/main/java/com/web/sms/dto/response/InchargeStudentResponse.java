package com.web.sms.dto.response;
import com.web.sms.entity.*;

public class InchargeStudentResponse {
 public Long allocationId; public String name; public String rollNumber; public String branch; public Integer year;
 public String boardingPoint; public String applicationStatus; public String transportationStatus="ACTIVE";
 public Long requiredFee=0L; public Long paidAmount=0L; public Long remainingFee=0L; public String feeStatus="PENDING";
 public String passStatus="INACTIVE"; public Integer seatNumber;
 public static InchargeStudentResponse from(TransportAllocation a,Fee f,BusPass p){InchargeStudentResponse r=new InchargeStudentResponse();r.allocationId=a.getId();r.name=a.getStudent().getName();r.rollNumber=a.getStudent().getRollNumber();r.branch=a.getStudent().getBranch();r.year=a.getStudent().getYear();r.boardingPoint=a.getBoardingPoint().getStationName();r.applicationStatus=a.getApplication()!=null?a.getApplication().getStatus().name():"APPROVED";r.seatNumber=a.getSeatNumber();if(f!=null){r.requiredFee=f.getTotalAmount();r.paidAmount=f.getPaidAmount()==null?0:f.getPaidAmount();r.remainingFee=Math.max(0,r.requiredFee-r.paidAmount);r.feeStatus=f.getStatus();}if(p!=null)r.passStatus=p.getStatus().name();return r;}
}
