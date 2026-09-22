package com.web.sms.repository;
import com.web.sms.entity.BoardingPoints;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BoardingPointsRepository extends JpaRepository<BoardingPoints, Long> {
    List<BoardingPoints> findByBusId(Long busId);
    List<BoardingPoints> findByBusIdAndStatus(Long busId, EntityStatus status);
    boolean existsByBusIdAndStationNameIgnoreCase(Long busId, String stationName);
    Optional<BoardingPoints> findFirstByStationNameIgnoreCase(String stationName);
    List<BoardingPoints> findAllByOrderByBusBusNumberAscOrderIndexAscStationNameAsc();
}
