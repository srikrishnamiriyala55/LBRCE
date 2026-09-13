package com.web.sms.entity;
import com.web.sms.enums.EntityStatus;
import jakarta.persistence.*;

@Entity
@Table(name="boarding_points")
public class BoardingPoints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="bus_id", nullable=false)
    private Bus bus;
    
    @ManyToOne
    @JoinColumn(name="route_id")
    private Route route;
    
    @Column(nullable=false)
    private String stationName;
    
    private Long feeAmount;
    
    @Column(columnDefinition = "int default 0")
    private Integer orderIndex = 0;
    
    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Long getFeeAmount() { return feeAmount; }
    public void setFeeAmount(Long feeAmount) { this.feeAmount = feeAmount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public EntityStatus getStatus() { return status; }
    public void setStatus(EntityStatus status) { this.status = status; }
}
