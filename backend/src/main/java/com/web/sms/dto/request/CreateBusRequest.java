package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class CreateBusRequest {
    @NotBlank
    private String busNumber;
    private Long routeId;
    @NotNull
    @Min(1)
    private Integer totalSeats;
    private String startingPoint;
    private String endingPoint;

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public String getStartingPoint() { return startingPoint; }
    public void setStartingPoint(String startingPoint) { this.startingPoint = startingPoint; }
    public String getEndingPoint() { return endingPoint; }
    public void setEndingPoint(String endingPoint) { this.endingPoint = endingPoint; }
}
