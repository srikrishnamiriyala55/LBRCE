package com.web.sms.repository;
import com.web.sms.entity.Payment;
import com.web.sms.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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
    Optional<Payment> findFirstByFeeIdAndStatusOrderByCreatedAtDesc(Long feeId, PaymentStatus status);
    @Query("""
        select p from Payment p
        join fetch p.student s
        join fetch p.fee f
        join fetch f.academicYear ay
        where (:academicYearId is null or ay.id = :academicYearId)
          and (:fromDate is null or coalesce(p.paidAt, p.createdAt) >= :fromDate)
          and (:toDateExclusive is null or coalesce(p.paidAt, p.createdAt) < :toDateExclusive)
          and (:status is null or p.status = :status)
          and (:mode is null or upper(p.gatewayName) = :mode)
          and (:search is null or lower(s.name) like lower(concat('%', :search, '%'))
               or lower(s.rollNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(s.phoneNumber, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.transactionId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.orderId, '')) like lower(concat('%', :search, '%')))
        order by coalesce(p.paidAt, p.createdAt) desc, p.id desc
        """)
    List<Payment> findForAdminReport(Long academicYearId, LocalDateTime fromDate, LocalDateTime toDateExclusive,
                                     PaymentStatus status, String mode, String search);
    @Query("""
        select p from Payment p
        join fetch p.student s
        join fetch p.fee f
        join fetch f.academicYear ay
        join fetch f.allocation a
        where a.bus.id = :busId
          and (:academicYearId is null or ay.id = :academicYearId)
          and (:fromDate is null or coalesce(p.paidAt, p.createdAt) >= :fromDate)
          and (:toDateExclusive is null or coalesce(p.paidAt, p.createdAt) < :toDateExclusive)
          and (:status is null or p.status = :status)
          and (:mode is null or upper(p.gatewayName) = :mode)
          and (:search is null or lower(s.name) like lower(concat('%', :search, '%'))
               or lower(s.rollNumber) like lower(concat('%', :search, '%'))
               or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(s.phoneNumber, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.transactionId, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(p.orderId, '')) like lower(concat('%', :search, '%')))
        order by coalesce(p.paidAt, p.createdAt) desc, p.id desc
        """)
    List<Payment> findForInchargeReport(Long busId, Long academicYearId, LocalDateTime fromDate,
                                        LocalDateTime toDateExclusive, PaymentStatus status, String mode,
                                        String search);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId=:orderId") Optional<Payment> findByOrderIdForUpdate(String orderId);
}
