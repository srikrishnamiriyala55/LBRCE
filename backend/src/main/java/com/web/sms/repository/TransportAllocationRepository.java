package com.web.sms.repository;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransportAllocationRepository extends JpaRepository<TransportAllocation, Long> {
    Optional<TransportAllocation> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, EntityStatus status);
    List<TransportAllocation> findByBusIdAndStatus(Long busId, EntityStatus status);
    long countByBusIdAndStatus(Long busId, EntityStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TransportAllocation a where a.student.id = :studentId and a.academicYear.id = :yearId and a.status = com.web.sms.enums.EntityStatus.ACTIVE")
    Optional<TransportAllocation> findActiveForUpdate(Long studentId, Long yearId);
    @Query("select a from TransportAllocation a left join Fee f on f.allocation.id=a.id " +
           "where a.bus.id=:busId " +
           "and (:academicYearId is null or a.academicYear.id=:academicYearId) " +
           "and (:search is null or lower(a.student.name) like lower(concat('%',:search,'%')) or lower(a.student.rollNumber) like lower(concat('%',:search,'%'))) " +
           "and (:branch is null or exists (select 1 from StudentAcademicDetails sad where sad.student.id=a.student.id and sad.academicYear.id=a.academicYear.id and sad.branch=:branch)) " +
           "and (:year is null or exists (select 1 from StudentAcademicDetails sad where sad.student.id=a.student.id and sad.academicYear.id=a.academicYear.id and sad.year=:year)) " +
           "and (:boardingPointId is null or a.boardingPoint.id=:boardingPointId) " +
           "and (:paymentStatus is null or f.status=:paymentStatus) " +
           "and (:passStatus is null " +
           " or (:passStatus='ACTIVE' and exists (select 1 from BusPass bp where bp.allocation.id=a.id and bp.status=com.web.sms.enums.PassStatus.ACTIVE)) " +
           " or (:passStatus='INACTIVE' and not exists (select 1 from BusPass bp where bp.allocation.id=a.id and bp.status=com.web.sms.enums.PassStatus.ACTIVE))) " +
           "order by a.academicYear.startDate desc, a.student.rollNumber asc")
    Page<TransportAllocation> searchForIncharge(@Param("busId") Long busId,@Param("academicYearId") Long academicYearId,@Param("search") String search,@Param("branch") String branch,@Param("year") Integer year,@Param("boardingPointId") Long boardingPointId,@Param("paymentStatus") String paymentStatus,@Param("passStatus") String passStatus,Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TransportAllocation a where a.id=:id")
    Optional<TransportAllocation> findByIdForUpdate(Long id);
    long countByStatus(EntityStatus status);
    List<TransportAllocation> findByStudentId(Long studentId);
    List<TransportAllocation> findByAcademicYearIdAndStatus(Long academicYearId, EntityStatus status);
    void deleteByStudentId(Long studentId);
    Optional<TransportAllocation> findByStudentIdAndBusIdAndStatus(Long studentId,Long busId,EntityStatus status);
    boolean existsByApplicationId(Long applicationId);
    long countByBoardingPointIdAndStatus(Long boardingPointId,EntityStatus status);
    long countByBoardingPointId(Long boardingPointId);
}
