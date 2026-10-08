package com.web.sms.repository;
import com.web.sms.entity.BusPass;
import com.web.sms.enums.PassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.web.sms.enums.EntityStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusPassRepository extends JpaRepository<BusPass, Long> {
    Optional<BusPass> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, PassStatus status);
    Optional<BusPass> findByPassNumber(String passNumber);
    Optional<BusPass> findByVerificationToken(String verificationToken);
    List<BusPass> findByStudentId(Long studentId);
    List<BusPass> findByAcademicYearIdAndStatus(Long academicYearId, PassStatus status);
    Optional<BusPass> findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(Long studentId, Long academicYearId);
    Page<BusPass> findByAllocationBusIdAndAllocationStatus(Long busId, EntityStatus status, Pageable pageable);
    long countByAllocationBusIdAndAllocationStatusAndStatus(Long busId, EntityStatus allocationStatus, PassStatus status);
    long countByStatus(PassStatus status);
    void deleteByStudentId(Long studentId);
    @Query("select p from BusPass p where :search is null or :search='' or lower(p.passNumber) like lower(concat('%',:search,'%')) or lower(p.student.name) like lower(concat('%',:search,'%')) or lower(p.student.rollNumber) like lower(concat('%',:search,'%')) or lower(p.allocation.bus.busNumber) like lower(concat('%',:search,'%'))")
    Page<BusPass> search(@Param("search") String search, Pageable pageable);

    /** List variant for admin passes endpoint (no pagination overhead). */
    @Query("select p from BusPass p where :search is null or :search='' or lower(p.passNumber) like lower(concat('%',:search,'%')) or lower(p.student.name) like lower(concat('%',:search,'%')) or lower(p.student.rollNumber) like lower(concat('%',:search,'%')) or lower(p.allocation.bus.busNumber) like lower(concat('%',:search,'%')) order by p.student.rollNumber asc")
    List<BusPass> searchList(@Param("search") String search);

    /** Historical and current passes for students allocated to the incharge's bus. */
    @Query("select p from BusPass p where p.allocation.bus.id=:busId " +
           "and (:academicYearId is null or p.academicYear.id=:academicYearId) " +
           "order by p.academicYear.startDate desc, p.student.rollNumber asc")
    List<BusPass> findForIncharge(@Param("busId") Long busId,
                                  @Param("academicYearId") Long academicYearId);
}
