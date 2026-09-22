package com.web.sms.repository;
import com.web.sms.entity.BoardingPoints;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BoardingPointsRepository extends JpaRepository<BoardingPoints, Long> {
    List<BoardingPoints> findByBusId(Long busId);
    List<BoardingPoints> findByBusIdAndStatus(Long busId, EntityStatus status);
    List<BoardingPoints> findByBusIdOrderByOrderIndexAscStationNameAsc(Long busId);
    List<BoardingPoints> findByBusIdAndStatusOrderByOrderIndexAscStationNameAsc(Long busId, EntityStatus status);
    boolean existsByBusIdAndStationNameIgnoreCase(Long busId, String stationName);
    Optional<BoardingPoints> findFirstByStationNameIgnoreCase(String stationName);
    List<BoardingPoints> findAllByOrderByBusBusNumberAscOrderIndexAscStationNameAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BoardingPoints p where p.bus.id=:busId order by p.orderIndex asc, p.stationName asc")
    List<BoardingPoints> findByBusIdForUpdate(@Param("busId") Long busId);
}
