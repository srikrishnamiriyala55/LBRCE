package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateApplicationRequest {
    @NotNull private Long busId;
    @NotNull private Long boardingPointId;
    @Size(max=500) private String remarks;
    public Long getBusId(){return busId;} public void setBusId(Long v){busId=v;}
    public Long getBoardingPointId(){return boardingPointId;} public void setBoardingPointId(Long v){boardingPointId=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
}
