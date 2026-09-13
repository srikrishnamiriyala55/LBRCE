package com.web.sms.repository;
import com.web.sms.entity.Complaint;
import com.web.sms.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Page<Complaint> findByStudentId(Long studentId, Pageable pageable);
    Page<Complaint> findByBusId(Long busId, Pageable pageable);
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    long countByStatus(ComplaintStatus status);
    boolean existsByStudentIdAndStatusInAndSubjectIgnoreCase(Long studentId, List<ComplaintStatus> statuses, String subject);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Complaint c where c.id=:id") Optional<Complaint> findByIdForUpdate(Long id);
}
