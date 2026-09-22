package com.web.sms.dto.response;

import com.web.sms.entity.Bus;
import com.web.sms.enums.EntityStatus;

public class BusResponse {
    private Long id;
    private String busNumber;
    private String startingPoint;
    private String endingPoint;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer occupiedSeats;
    private EntityStatus status;
    private Long inchargeId;
    private String inchargeTeacherId;
    private String inchargeName;

    public static BusResponse fromBus(Bus b, int occupied) {
        BusResponse r = new BusResponse();
        r.setId(b.getId());
        r.setBusNumber(b.getBusNumber());
        r.setStartingPoint(b.getStartingPoint());
        r.setEndingPoint(b.getEndingPoint());
        r.setTotalSeats(b.getTotalSeats());
        r.setAvailableSeats(b.getAvailableSeats());
        r.setOccupiedSeats(occupied);
        r.setStatus(b.getStatus());
        if(b.getIncharge() != null) {
            r.setInchargeId(b.getIncharge().getId());
            r.setInchargeTeacherId(b.getIncharge().getTeacherId());
            r.setInchargeName(b.getIncharge().getName());
        }
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
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
    public Long getInchargeId() { return inchargeId; }
    public void setInchargeId(Long inchargeId) { this.inchargeId = inchargeId; }
    public String getInchargeTeacherId() { return inchargeTeacherId; }
    public void setInchargeTeacherId(String inchargeTeacherId) { this.inchargeTeacherId = inchargeTeacherId; }
    public String getInchargeName() { return inchargeName; }
    public void setInchargeName(String inchargeName) { this.inchargeName = inchargeName; }
}
