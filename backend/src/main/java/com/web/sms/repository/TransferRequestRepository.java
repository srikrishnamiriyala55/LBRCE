package com.web.sms.repository;
import com.web.sms.entity.TransferRequest;
import com.web.sms.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    @EntityGraph(attributePaths = {"student", "currentBus", "currentBus.incharge", "requestedBus",
            "requestedBus.incharge", "currentBoardingPoint", "requestedBoardingPoint", "oldIncharge",
            "newIncharge", "academicYear"})
    @Query(value = "select t from TransferRequest t", countQuery = "select count(t) from TransferRequest t")
    Page<TransferRequest> findAllWithDetails(Pageable pageable);

    /** List variant — no pagination overhead. Used by admin /transfers endpoint. */
    @EntityGraph(attributePaths = {"student", "currentBus", "currentBus.incharge", "requestedBus",
            "requestedBus.incharge", "currentBoardingPoint", "requestedBoardingPoint", "oldIncharge",
            "newIncharge", "academicYear"})
    @Query("select t from TransferRequest t order by t.id desc")
    List<TransferRequest> findAllWithDetailsList();

    /** List variant filtered by status. Used by admin /transfers?status=... endpoint. */
    @Query("select t from TransferRequest t where t.status = :status order by t.id desc")
    List<TransferRequest> findByStatusList(@org.springframework.data.repository.query.Param("status") TransferStatus status);
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
    Page<TransferRequest> findByStatus(TransferStatus status, Pageable pageable);
    /** Count-based method for incharge applicationSummary — avoids loading entities. */
    long countByBusApplication_BusId(Long busId);
}
