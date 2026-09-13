package com.web.sms.dto.response;

import com.web.sms.entity.Bus;
import com.web.sms.enums.EntityStatus;

public class BusResponse {
    private Long id;
    private String busNumber;
    private String routeName;
    private String startingPoint;
    private String endingPoint;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer occupiedSeats;
    private EntityStatus status;
    private String inchargeName;

    public static BusResponse fromBus(Bus b, int occupied) {
        BusResponse r = new BusResponse();
        r.setId(b.getId());
        r.setBusNumber(b.getBusNumber());
        if(b.getRoute() != null) r.setRouteName(b.getRoute().getRouteName());
        r.setStartingPoint(b.getStartingPoint());
        r.setEndingPoint(b.getEndingPoint());
        r.setTotalSeats(b.getTotalSeats());
        r.setAvailableSeats(b.getAvailableSeats());
        r.setOccupiedSeats(occupied);
        r.setStatus(b.getStatus());
        if(b.getIncharge() != null) r.setInchargeName(b.getIncharge().getName());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getStartingPoint() { return startingPoint; }
    public void setStartingPoint(String startingPoint) { this.startingPoint = startingPoint; }
    public String getEndingPoint() { return endingPoint; }
    public void setEndingPoint(String endingPoint) { this.endingPoint = endingPoint; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    public Integer getOccupiedSeats() { return occupiedSeats; }
    public void setOccupiedSeats(Integer occupiedSeats) { this.occupiedSeats = occupiedSeats; }
    public EntityStatus getStatus() { return status; }
    public void setStatus(EntityStatus status) { this.status = status; }
    public String getInchargeName() { return inchargeName; }
    public void setInchargeName(String inchargeName) { this.inchargeName = inchargeName; }
}
