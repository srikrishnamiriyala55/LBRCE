package com.web.sms.repository;
import com.web.sms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    /** List variant — used by admin audit-logs endpoint. */
    List<AuditLog> findAllByOrderByTimestampDesc();
}
