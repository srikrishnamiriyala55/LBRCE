package com.web.sms.repository;
import com.web.sms.entity.BusPass;
import com.web.sms.enums.PassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.web.sms.enums.EntityStatus;
import java.util.Optional;

public interface BusPassRepository extends JpaRepository<BusPass, Long> {
    Optional<BusPass> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, PassStatus status);
    Optional<BusPass> findByPassNumber(String passNumber);
    Optional<BusPass> findByVerificationToken(String verificationToken);
    List<BusPass> findByStudentId(Long studentId);
    Page<BusPass> findByAllocationBusIdAndAllocationStatus(Long busId, EntityStatus status, Pageable pageable);
    long countByAllocationBusIdAndAllocationStatusAndStatus(Long busId, EntityStatus allocationStatus, PassStatus status);
    long countByStatus(PassStatus status);
}
