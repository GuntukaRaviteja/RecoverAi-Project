package com.recoverai.backend.controller;

import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.repository.AuditLogRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@Validated
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    // Get all audit logs with clean pagination response
    @GetMapping
    public PageResponse<AuditLog> getAllAuditLogs(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not be greater than 100")
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<AuditLog> auditLogPage =
                auditLogRepository.findAll(pageable);

        return new PageResponse<>(auditLogPage);
    }

    // Get audit logs for a specific payment with clean pagination response
    @GetMapping("/payment/{paymentId}")
    public PageResponse<AuditLog> getAuditLogsByPaymentId(
            @PathVariable Long paymentId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not be greater than 100")
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<AuditLog> auditLogPage =
                auditLogRepository.findByPaymentId(
                        paymentId,
                        pageable
                );

        return new PageResponse<>(auditLogPage);
    }
}