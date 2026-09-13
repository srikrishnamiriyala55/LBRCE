package com.web.sms.service;

import com.web.sms.entity.AuditLog;
import com.web.sms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepo;

    public AuditService(AuditLogRepository auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    @Transactional
    public void log(String userId, String userRole, String action, String entityType, String entityId, String oldValue, String newValue, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUserRole(userRole);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepo.save(log);
    }

    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepo.findAllByOrderByTimestampDesc(pageable);
    }
}
