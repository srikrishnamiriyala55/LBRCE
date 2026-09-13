package com.web.sms.dto.response;

import com.web.sms.entity.BoardingPoints;

public class BoardingPointResponse {
    private Long id;
    private String stationName;
    private Long feeAmount;
    private Integer orderIndex;
    private String busNumber;
    private Long busId;
    private String routeName;
    private String status;

    public static BoardingPointResponse fromBoardingPoint(BoardingPoints bp) {
        BoardingPointResponse r = new BoardingPointResponse();
        r.setId(bp.getId());
        r.setStationName(bp.getStationName());
        r.setFeeAmount(bp.getFeeAmount());
        r.setOrderIndex(bp.getOrderIndex());
        if (bp.getBus() != null) {
            r.setBusId(bp.getBus().getId());
            r.setBusNumber(bp.getBus().getBusNumber());
        }
        if (bp.getRoute() != null) r.setRouteName(bp.getRoute().getRouteName());
        r.setStatus(bp.getStatus() == null ? null : bp.getStatus().name());
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
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
