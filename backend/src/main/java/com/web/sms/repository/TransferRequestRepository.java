package com.web.sms.repository;
import com.web.sms.entity.TransferRequest;
import com.web.sms.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    long countByCurrentBoardingPointIdOrRequestedBoardingPointId(Long currentBoardingPointId, Long requestedBoardingPointId);
    List<TransferRequest> findByStudentId(Long studentId);
    void deleteByStudentId(Long studentId);
    Page<TransferRequest> findByCurrentBusIdOrRequestedBusId(Long currentBusId, Long requestedBusId, Pageable pageable);
    List<TransferRequest> findByStudentIdAndAcademicYearIdAndStatusIn(Long studentId, Long academicYearId, List<TransferStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransferRequest t where t.id = :id")
    Optional<TransferRequest> findByIdForUpdate(Long id);
    Page<TransferRequest> findByCurrentBusIdAndStatus(Long busId, TransferStatus status, Pageable pageable);
    Page<TransferRequest> findByRequestedBusIdAndStatus(Long busId, TransferStatus status, Pageable pageable);
    long countByStatus(TransferStatus status);
    long countByCurrentBusIdAndStatusIn(Long busId, List<TransferStatus> statuses);
    long countByRequestedBusIdAndStatusIn(Long busId, List<TransferStatus> statuses);
}
