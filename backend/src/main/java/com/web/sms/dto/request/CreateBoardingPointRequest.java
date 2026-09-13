package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateBoardingPointRequest {
    @NotNull
    private Long busId;
    @NotBlank
    private String stationName;
    @NotNull
    @Positive
    private Long feeAmount;
    private Integer orderIndex;

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Long getFeeAmount() { return feeAmount; }
    public void setFeeAmount(Long feeAmount) { this.feeAmount = feeAmount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
