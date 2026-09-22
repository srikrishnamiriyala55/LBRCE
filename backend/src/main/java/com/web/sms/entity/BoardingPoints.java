package com.web.sms.entity;
import com.web.sms.enums.EntityStatus;
import jakarta.persistence.*;

@Entity
@Table(name="boarding_points", uniqueConstraints = {
        @UniqueConstraint(name="uk_boarding_point_bus_station", columnNames={"bus_id", "station_name"})
})
public class BoardingPoints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="bus_id", nullable=false)
    private Bus bus;
    
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
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Long getFeeAmount() { return feeAmount; }
    public void setFeeAmount(Long feeAmount) { this.feeAmount = feeAmount; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public EntityStatus getStatus() { return status; }
    public void setStatus(EntityStatus status) { this.status = status; }
}
