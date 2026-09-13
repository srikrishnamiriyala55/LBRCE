package com.web.sms.repository;
import com.web.sms.entity.Bus;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByBusNumber(String busNumber);
    List<Bus> findByStatus(EntityStatus status);
    List<Bus> findByInchargeId(Long inchargeId);
    List<Bus> findByRouteId(Long routeId);
    boolean existsByBusNumber(String busNumber);
    long countByStatus(EntityStatus status);
    @Query("select coalesce(sum(b.totalSeats),0) from Bus b where b.status=com.web.sms.enums.EntityStatus.ACTIVE") Long sumActiveCapacity();
    @Query("select count(b) from Bus b where b.status=com.web.sms.enums.EntityStatus.ACTIVE and b.availableSeats<=0") long countFullBuses();
    long countByRouteIdAndStatus(Long routeId,EntityStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bus b where b.id=:id") Optional<Bus> findByIdForUpdate(Long id);
}
