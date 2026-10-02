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
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BusApplicationRepository extends JpaRepository<BusApplication, Long> {
    long countByBoardingPointId(Long boardingPointId);
    List<BusApplication> findByStudentId(Long studentId);
    void deleteByStudentId(Long studentId);
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
    @Query("select a from BusApplication a left join fetch a.student left join fetch a.bus left join fetch a.boardingPoint left join fetch a.academicYear order by a.appliedAt desc")
    List<BusApplication> findAllForAdminList();
    @Query(value = "select a from BusApplication a where (:status is null or a.status=:status) and (:search is null or :search='' or lower(a.student.name) like lower(concat('%',:search,'%')) or lower(a.student.rollNumber) like lower(concat('%',:search,'%')) or lower(a.bus.busNumber) like lower(concat('%',:search,'%')) or lower(a.boardingPoint.stationName) like lower(concat('%',:search,'%')) or lower(a.academicYear.yearName) like lower(concat('%',:search,'%')))",
           countQuery = "select count(a) from BusApplication a where (:status is null or a.status=:status) and (:search is null or :search='' or lower(a.student.name) like lower(concat('%',:search,'%')) or lower(a.student.rollNumber) like lower(concat('%',:search,'%')) or lower(a.bus.busNumber) like lower(concat('%',:search,'%')) or lower(a.boardingPoint.stationName) like lower(concat('%',:search,'%')) or lower(a.academicYear.yearName) like lower(concat('%',:search,'%')))")
    Page<BusApplication> searchForAdmin(@Param("search") String search, @Param("status") ApplicationStatus status, Pageable pageable);
    long countByBusIdAndStatusIn(Long busId, List<ApplicationStatus> statuses);
}
