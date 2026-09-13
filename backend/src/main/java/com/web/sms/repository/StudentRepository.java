package com.web.sms.repository;
import com.web.sms.entity.Student;
import com.web.sms.dto.response.TransportReportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByPhoneNumber(String phoneNumber);
    Page<Student> findByBranch(String branch, Pageable pageable);
    Page<Student> findByStatus(String status, Pageable pageable);
    Page<Student> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    @Query("SELECT s FROM Student s WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:branch IS NULL OR :branch = '' OR s.branch = :branch)")
    Page<Student> searchStudents(@Param("search") String search, @Param("branch") String branch, Pageable pageable);

    boolean existsByRollNumber(String rollNumber);
    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s where s.id=:id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);

    @Query(value="""
        SELECT s.id AS studentDbId, s.student_id AS rollNumber, s.name AS studentName,
          s.branch AS department, s.year AS studyYear, s.semester AS semester, s.status AS accountStatus,
          b.bus_number AS busNumber, bp.station_name AS boardingPoint, ta.seat_number AS seatNumber,
          COALESCE(f.total_amount,0) AS totalAmount, COALESCE(f.paid_amount,0) AS paidAmount,
          GREATEST(COALESCE(f.total_amount,0)-COALESCE(f.paid_amount,0),0) AS remainingAmount,
          CASE WHEN f.id IS NULL THEN 'NOT_ASSIGNED' WHEN COALESCE(f.paid_amount,0)=0 THEN 'UNPAID'
               WHEN COALESCE(f.paid_amount,0)>=f.total_amount THEN 'PAID' ELSE 'PARTIALLY_PAID' END AS paymentStatus,
          ba.status AS applicationStatus, tr.status AS transferStatus
        FROM student s
        LEFT JOIN academic_year ay ON ay.active=1
        LEFT JOIN transport_allocation ta ON ta.student_id=s.id AND ta.academic_year_id=ay.id AND ta.status='ACTIVE'
        LEFT JOIN bus b ON b.id=ta.bus_id
        LEFT JOIN boarding_points bp ON bp.id=ta.boarding_point_id
        LEFT JOIN fee f ON f.student_id=s.id AND f.academic_year_id=ay.id
        LEFT JOIN bus_application ba ON ba.id=(SELECT MAX(ba2.id) FROM bus_application ba2 WHERE ba2.student_id=s.id AND ba2.academic_year_id=ay.id)
        LEFT JOIN transfer_request tr ON tr.id=(SELECT MAX(tr2.id) FROM transfer_request tr2 WHERE tr2.student_id=s.id AND tr2.academic_year_id=ay.id)
        WHERE (:scopeBusId IS NULL OR b.id=:scopeBusId)
          AND (:busNumber IS NULL OR LOWER(b.bus_number) LIKE LOWER(CONCAT('%',:busNumber,'%')))
          AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%',:name,'%')))
          AND (:rollNumber IS NULL OR LOWER(s.student_id) LIKE LOWER(CONCAT('%',:rollNumber,'%')))
          AND (:studyYear IS NULL OR s.year=:studyYear) AND (:semester IS NULL OR s.semester=:semester)
          AND (:department IS NULL OR LOWER(s.branch)=LOWER(:department))
          AND (:accountStatus IS NULL OR UPPER(s.status)=UPPER(:accountStatus))
          AND (:applicationStatus IS NULL OR ba.status=:applicationStatus)
          AND (:transferStatus IS NULL OR tr.status=:transferStatus)
          AND (:paymentStatus IS NULL OR
              (:paymentStatus='PAID' AND f.id IS NOT NULL AND COALESCE(f.paid_amount,0)>=f.total_amount) OR
              (:paymentStatus='UNPAID' AND (f.id IS NULL OR COALESCE(f.paid_amount,0)=0)) OR
              (:paymentStatus='PARTIALLY_PAID' AND COALESCE(f.paid_amount,0)>0 AND COALESCE(f.paid_amount,0)<f.total_amount) OR
              (:paymentStatus='PENDING' AND f.id IS NOT NULL AND COALESCE(f.paid_amount,0)<f.total_amount))
          AND (:minimumRemaining IS NULL OR GREATEST(COALESCE(f.total_amount,0)-COALESCE(f.paid_amount,0),0)>=:minimumRemaining)
        """, countQuery="""
        SELECT COUNT(*) FROM student s
        LEFT JOIN academic_year ay ON ay.active=1
        LEFT JOIN transport_allocation ta ON ta.student_id=s.id AND ta.academic_year_id=ay.id AND ta.status='ACTIVE'
        LEFT JOIN bus b ON b.id=ta.bus_id LEFT JOIN fee f ON f.student_id=s.id AND f.academic_year_id=ay.id
        LEFT JOIN bus_application ba ON ba.id=(SELECT MAX(ba2.id) FROM bus_application ba2 WHERE ba2.student_id=s.id AND ba2.academic_year_id=ay.id)
        LEFT JOIN transfer_request tr ON tr.id=(SELECT MAX(tr2.id) FROM transfer_request tr2 WHERE tr2.student_id=s.id AND tr2.academic_year_id=ay.id)
        WHERE (:scopeBusId IS NULL OR b.id=:scopeBusId)
          AND (:busNumber IS NULL OR LOWER(b.bus_number) LIKE LOWER(CONCAT('%',:busNumber,'%')))
          AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%',:name,'%')))
          AND (:rollNumber IS NULL OR LOWER(s.student_id) LIKE LOWER(CONCAT('%',:rollNumber,'%')))
          AND (:studyYear IS NULL OR s.year=:studyYear) AND (:semester IS NULL OR s.semester=:semester)
          AND (:department IS NULL OR LOWER(s.branch)=LOWER(:department)) AND (:accountStatus IS NULL OR UPPER(s.status)=UPPER(:accountStatus))
          AND (:applicationStatus IS NULL OR ba.status=:applicationStatus) AND (:transferStatus IS NULL OR tr.status=:transferStatus)
          AND (:paymentStatus IS NULL OR (:paymentStatus='PAID' AND f.id IS NOT NULL AND COALESCE(f.paid_amount,0)>=f.total_amount) OR (:paymentStatus='UNPAID' AND (f.id IS NULL OR COALESCE(f.paid_amount,0)=0)) OR (:paymentStatus='PARTIALLY_PAID' AND COALESCE(f.paid_amount,0)>0 AND COALESCE(f.paid_amount,0)<f.total_amount) OR (:paymentStatus='PENDING' AND f.id IS NOT NULL AND COALESCE(f.paid_amount,0)<f.total_amount))
          AND (:minimumRemaining IS NULL OR GREATEST(COALESCE(f.total_amount,0)-COALESCE(f.paid_amount,0),0)>=:minimumRemaining)
        """, nativeQuery=true)
    Page<TransportReportRow> transportReport(@Param("scopeBusId") Long scopeBusId,@Param("busNumber") String busNumber,
      @Param("name") String name,@Param("rollNumber") String rollNumber,@Param("studyYear") Integer studyYear,
      @Param("semester") Integer semester,@Param("paymentStatus") String paymentStatus,@Param("accountStatus") String accountStatus,
      @Param("applicationStatus") String applicationStatus,@Param("transferStatus") String transferStatus,
      @Param("department") String department,@Param("minimumRemaining") Long minimumRemaining,Pageable pageable);
}
