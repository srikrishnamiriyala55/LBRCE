package com.web.sms.dto.response;

import com.web.sms.entity.TransportAllocation;
import java.time.LocalDateTime;

public class AllocationHistoryResponse {
    private Long id; private String busNumber; private String startingPoint; private String endingPoint;
    private String academicYear; private String boardingPoint; private String status; private LocalDateTime allocatedAt; private LocalDateTime deactivatedAt;
    private String inchargeName; private String inchargeTeacherId;
    public static AllocationHistoryResponse from(TransportAllocation a){
        AllocationHistoryResponse r=new AllocationHistoryResponse();r.id=a.getId();
        r.academicYear=a.getAcademicYear()==null?null:a.getAcademicYear().getYearName();
        r.busNumber=value(a.getBusNumberSnapshot(),a.getBus()==null?null:a.getBus().getBusNumber());
        r.startingPoint=value(a.getStartingPointSnapshot(),a.getBus()==null?null:a.getBus().getStartingPoint());
        r.endingPoint=value(a.getEndingPointSnapshot(),a.getBus()==null?null:a.getBus().getEndingPoint());
        r.boardingPoint=value(a.getBoardingPointSnapshot(),a.getBoardingPoint()==null?null:a.getBoardingPoint().getStationName());
        r.inchargeName=value(a.getInchargeNameSnapshot(),a.getBus()!=null&&a.getBus().getIncharge()!=null?a.getBus().getIncharge().getName():null);
        r.inchargeTeacherId=value(a.getInchargeTeacherIdSnapshot(),a.getBus()!=null&&a.getBus().getIncharge()!=null?a.getBus().getIncharge().getTeacherId():null);
        r.status=a.getStatus()==null?null:a.getStatus().name();r.allocatedAt=a.getAllocatedAt();r.deactivatedAt=a.getDeactivatedAt();return r;
    }
    private static String value(String captured,String current){return captured==null||captured.isBlank()?current:captured;}
    public Long getId(){return id;} public String getAcademicYear(){return academicYear;} public String getBusNumber(){return busNumber;} public String getStartingPoint(){return startingPoint;} public String getEndingPoint(){return endingPoint;} public String getBoardingPoint(){return boardingPoint;} public String getStatus(){return status;} public LocalDateTime getAllocatedAt(){return allocatedAt;} public LocalDateTime getDeactivatedAt(){return deactivatedAt;} public String getInchargeName(){return inchargeName;} public String getInchargeTeacherId(){return inchargeTeacherId;}
}
