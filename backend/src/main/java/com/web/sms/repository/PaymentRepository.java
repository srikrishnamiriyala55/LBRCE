package com.web.sms.repository;
import com.web.sms.entity.Payment;
import com.web.sms.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByFeeId(Long feeId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    @Query("select p from Payment p join fetch p.student join fetch p.fee f join fetch f.academicYear join fetch f.allocation a where a.bus.id=:busId and (:academicYearId is null or f.academicYear.id=:academicYearId) order by p.createdAt desc")
    List<Payment> findForIncharge(Long busId, Long academicYearId);
    void deleteByStudentId(Long studentId);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);
    boolean existsByFeeIdAndStatus(Long feeId, PaymentStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId=:orderId") Optional<Payment> findByOrderIdForUpdate(String orderId);
}
