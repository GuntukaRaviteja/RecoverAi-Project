package com.recoverai.backend.service;

import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog createAuditLog(
            Long paymentId,
            String action,
            String details
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.setPaymentId(paymentId);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(LocalDateTime.now());

        return auditLogRepository.save(auditLog);
    }
}