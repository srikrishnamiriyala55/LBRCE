package com.web.sms.repository;
import com.web.sms.entity.BusApplication;
import com.web.sms.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface BusApplicationRepository extends JpaRepository<BusApplication, Long> {
    List<BusApplication> findByStudentId(Long studentId);
    List<BusApplication> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<BusApplication> findByBusId(Long busId);
    Page<BusApplication> findByBusId(Long busId, Pageable pageable);
    List<BusApplication> findByBusIdAndStatus(Long busId, ApplicationStatus status);
    Page<BusApplication> findByBusIdAndStatus(Long busId, ApplicationStatus status, Pageable pageable);
    Page<BusApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    Page<BusApplication> findByStudentId(Long studentId, Pageable pageable);
    List<BusApplication> findByStudentIdAndAcademicYearIdAndStatusIn(Long studentId, Long academicYearId, List<ApplicationStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BusApplication a where a.id=:id")
    Optional<BusApplication> findByIdForUpdate(Long id);
    long countByStatus(ApplicationStatus status);
    long countByBusIdAndStatusIn(Long busId, List<ApplicationStatus> statuses);
}
