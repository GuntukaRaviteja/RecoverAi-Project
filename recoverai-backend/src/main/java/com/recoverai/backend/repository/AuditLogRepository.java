package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    boolean existsByPaymentIdAndAction(
            Long paymentId,
            String action
    );

    List<AuditLog> findByPaymentIdOrderByCreatedAtDesc(
            Long paymentId
    );

    Page<AuditLog> findByPaymentId(
            Long paymentId,
            Pageable pageable
    );

    Optional<AuditLog> findTopByPaymentIdAndActionOrderByCreatedAtDesc(
            Long paymentId,
            String action
    );
} 
