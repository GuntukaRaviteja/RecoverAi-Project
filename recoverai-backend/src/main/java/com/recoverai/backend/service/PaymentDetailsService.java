package com.recoverai.backend.service;

import com.recoverai.backend.dto.PaymentDetailsResponse;
import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.AuditLogRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PaymentDetailsService {

    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentDetailsService(
            PaymentRepository paymentRepository,
            AiDecisionRepository aiDecisionRepository,
            RecoveryAttemptRepository recoveryAttemptRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public PaymentDetailsResponse getPaymentDetails(
            Long paymentId
    ) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Payment not found with ID: "
                                                + paymentId
                                )
                        );

        List<AiDecision> aiDecisions =
                aiDecisionRepository
                        .findByPaymentIdOrderByCreatedAtDesc(
                                paymentId
                        );

        List<RecoveryAttempt> recoveryAttempts =
                recoveryAttemptRepository
                        .findByPaymentIdOrderByAttemptedAtDesc(
                                paymentId
                        );

        List<AuditLog> auditLogs =
                auditLogRepository
                        .findByPaymentIdOrderByCreatedAtDesc(
                                paymentId
                        );

        PaymentDetailsResponse response =
                new PaymentDetailsResponse();

        response.setPayment(payment);
        response.setAiDecisions(aiDecisions);
        response.setRecoveryAttempts(recoveryAttempts);
        response.setAuditLogs(auditLogs);

        return response;
    }
}