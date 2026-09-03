package com.recoverai.backend.service;

import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.recovery.RecoveryPolicy;
import com.recoverai.backend.strategy.AiDecisionStrategy;
import com.recoverai.backend.strategy.DecisionResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiDecisionService {

    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryService recoveryService;
    private final AuditLogService auditLogService;
    private final AiDecisionStrategy aiDecisionStrategy;
    private final RecoveryPolicy recoveryPolicy;

    public AiDecisionService(
            PaymentRepository paymentRepository,
            AiDecisionRepository aiDecisionRepository,
            RecoveryAttemptRepository recoveryAttemptRepository,
            RecoveryService recoveryService,
            AuditLogService auditLogService,
            AiDecisionStrategy aiDecisionStrategy,
            RecoveryPolicy recoveryPolicy
    ) {
        this.paymentRepository = paymentRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryService = recoveryService;
        this.auditLogService = auditLogService;
        this.aiDecisionStrategy = aiDecisionStrategy;
        this.recoveryPolicy = recoveryPolicy;
    }

    public AiDecision analyzePayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found with ID: " + paymentId
                        )
                );

        if (!"FAILED".equalsIgnoreCase(payment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only failed payments can be analyzed for recovery"
            );
        }

        /*
         * Prevent duplicate AI analysis.
         *
         * This check is necessary even when no recovery attempt exists.
         * For example, NOTIFY_CUSTOMER and NO_ACTION decisions do not
         * create recovery attempts, so checking only recoveryAttemptCount
         * would allow duplicate AI decisions.
         */
        if (aiDecisionRepository.existsByPaymentId(paymentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment has already been analyzed. "
                            + "Further recovery actions are managed automatically."
            );
        }

        long recoveryAttemptCount =
                recoveryAttemptRepository.countByPaymentId(paymentId);

        if (recoveryAttemptCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment has already entered the recovery process. "
                            + "Further retries are managed automatically."
            );
        }

        DecisionResult recommendation =
                aiDecisionStrategy.makeDecision(
                        payment.getFailureCategory(),
                        payment.getFailureReason()
                );

        DecisionResult decisionResult =
                recoveryPolicy.apply(
                        payment,
                        recoveryAttemptCount,
                        recommendation
                );

        if (decisionResult != recommendation) {
            auditLogService.createAuditLog(
                    paymentId,
                    "RECOVERY_POLICY_APPLIED",
                    "AI recommendation was constrained by recovery policy: "
                            + decisionResult.getReason()
            );
        }

        AiDecision aiDecision = new AiDecision();

        aiDecision.setPaymentId(payment.getId());
        aiDecision.setCreatedAt(LocalDateTime.now());

        aiDecision.setDecision(
                decisionResult.getDecision().name()
        );

        aiDecision.setRecoveryAction(
                decisionResult.getRecoveryAction().name()
        );

        aiDecision.setReason(
                decisionResult.getReason()
        );

        aiDecision.setConfidenceScore(
                decisionResult.getConfidenceScore()
        );

        aiDecision.setDecisionSource(
                decisionResult.getDecisionSource().name()
        );

        AiDecision savedDecision =
                aiDecisionRepository.save(aiDecision);

        switch (decisionResult.getRecoveryAction()) {

            case CREATE_RECOVERY_ATTEMPT ->
                    recoveryService.createRecoveryAttempt(paymentId);

            case NOTIFY_CUSTOMER ->
                    auditLogService.createAuditLog(
                            paymentId,
                            "NOTIFY_CUSTOMER",
                            "Recovery decision: "
                                    + decisionResult.getReason()
                    );

            case NO_ACTION ->
                    auditLogService.createAuditLog(
                            paymentId,
                            "RECOVERY_STOPPED",
                            "Recovery stopped: "
                                    + decisionResult.getReason()
                    );
        }

        return savedDecision;
    }

    public List<AiDecision> getAllDecisions() {
        return aiDecisionRepository.findAll();
    }

    public List<AiDecision> getDecisionHistory(Long paymentId) {

        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Payment not found with ID: " + paymentId
            );
        }

        return aiDecisionRepository
                .findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }
}