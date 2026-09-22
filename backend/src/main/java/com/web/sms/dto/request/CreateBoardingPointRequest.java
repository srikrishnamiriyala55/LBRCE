package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;

public class CreateBoardingPointRequest {
    @NotNull
    private Long busId;
    private String stationName;
    private Long existingPointId;
    @NotNull
    @Positive
    private Long feeAmount;
    @Min(0)
    private Integer orderIndex;

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Long getExistingPointId() { return existingPointId; }
    public void setExistingPointId(Long existingPointId) { this.existingPointId = existingPointId; }
    public Long getFeeAmount() { return feeAmount; }
    public void setFeeAmount(Long feeAmount) { this.feeAmount = feeAmount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
