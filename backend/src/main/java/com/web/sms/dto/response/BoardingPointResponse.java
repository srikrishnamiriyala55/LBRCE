package com.web.sms.dto.response;

import com.web.sms.entity.BoardingPoints;

public class BoardingPointResponse {
    private Long id;
    private String stationName;
    private Long feeAmount;
    private Integer orderIndex;

    public static BoardingPointResponse fromBoardingPoint(BoardingPoints bp) {
        BoardingPointResponse r = new BoardingPointResponse();
        r.setId(bp.getId());
        r.setStationName(bp.getStationName());
        r.setFeeAmount(bp.getFeeAmount());
        r.setOrderIndex(bp.getOrderIndex());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Long getFeeAmount() { return feeAmount; }
    public void setFeeAmount(Long feeAmount) { this.feeAmount = feeAmount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
