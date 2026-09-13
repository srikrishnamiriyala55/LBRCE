package com.web.sms.repository;
import com.web.sms.entity.Fee;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface FeeRepository extends JpaRepository<Fee, Long> {
    Optional<Fee> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<Fee> findByStudentId(Long studentId);
    Page<Fee> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(f.paidAmount), 0) FROM Fee f")
    Long sumTotalPaidAmount();
    Page<Fee> findByAllocationBusIdAndAllocationStatus(Long busId, EntityStatus status, Pageable pageable);
    @Query("select coalesce(sum(f.totalAmount),0) from Fee f where f.allocation.bus.id=:busId and f.allocation.status=com.web.sms.enums.EntityStatus.ACTIVE")
    Long sumRequiredForActiveBus(Long busId);
    @Query("select coalesce(sum(f.paidAmount),0) from Fee f where f.allocation.bus.id=:busId and f.allocation.status=com.web.sms.enums.EntityStatus.ACTIVE")
    Long sumPaidForActiveBus(Long busId);
    @Query("select coalesce(sum(f.totalAmount),0) from Fee f") Long sumTotalExpected();
    long countByPassEligibleTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fee f where f.id=:id") Optional<Fee> findByIdForUpdate(Long id);
}
