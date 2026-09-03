package com.recoverai.backend.service;

import com.recoverai.backend.dto.DashboardStats;
import com.recoverai.backend.dto.RecentActivityItem;
import com.recoverai.backend.dto.RecentActivityResponse;
import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.AuditLogRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(
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

    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        List<Payment> payments = paymentRepository.findAll();

        stats.setTotalPayments(payments.size());

        stats.setFailedPayments(
                payments.stream()
                        .filter(payment ->
                                "FAILED".equalsIgnoreCase(
                                        payment.getStatus()
                                )
                        )
                        .count()
        );

        stats.setRecoveredPayments(
                payments.stream()
                        .filter(payment ->
                                "RECOVERED".equalsIgnoreCase(
                                        payment.getStatus()
                                )
                        )
                        .count()
        );

        List<AiDecision> aiDecisions =
                aiDecisionRepository.findAll();

        stats.setTotalAiDecisions(aiDecisions.size());

        stats.setGeminiAiDecisions(
                aiDecisions.stream()
                        .filter(decision ->
                                "GEMINI_AI".equalsIgnoreCase(
                                        decision.getDecisionSource()
                                )
                        )
                        .count()
        );

        stats.setRetryDecisions(
                aiDecisions.stream()
                        .filter(decision ->
                                "RETRY".equalsIgnoreCase(
                                        decision.getDecision()
                                )
                        )
                        .count()
        );

        stats.setNotifyCustomerDecisions(
                aiDecisions.stream()
                        .filter(decision ->
                                "NOTIFY_CUSTOMER".equalsIgnoreCase(
                                        decision.getDecision()
                                )
                        )
                        .count()
        );

        stats.setStopDecisions(
                aiDecisions.stream()
                        .filter(decision ->
                                "STOP".equalsIgnoreCase(
                                        decision.getDecision()
                                )
                        )
                        .count()
        );

        List<RecoveryAttempt> recoveryAttempts =
                recoveryAttemptRepository.findAll();

        stats.setActiveRecoveryCases(
                recoveryAttempts.stream()
                        .filter(attempt -> List.of(
                                "PENDING",
                                "PROCESSING",
                                "WAITING_FOR_CUSTOMER",
                                "SCHEDULED",
                                "WAITING_FOR_PAYMENT_METHOD"
                        ).contains(attempt.getStatus()))
                        .count()
        );

        stats.setTotalRecoveryAttempts(
                recoveryAttempts.size()
        );

        stats.setSuccessfulRecoveryAttempts(
                recoveryAttempts.stream()
                        .filter(attempt ->
                                "SUCCESS".equalsIgnoreCase(
                                        attempt.getStatus()
                                )
                        )
                        .count()
        );

        stats.setFailedRecoveryAttempts(
                recoveryAttempts.stream()
                        .filter(attempt ->
                                "FAILED".equalsIgnoreCase(
                                        attempt.getStatus()
                                )
                        )
                        .count()
        );

        stats.setPendingRecoveryAttempts(
                recoveryAttempts.stream()
                        .filter(attempt -> List.of(
                                "PENDING",
                                "WAITING_FOR_CUSTOMER",
                                "WAITING_FOR_PAYMENT_METHOD",
                                "SCHEDULED",
                                "PROCESSING"
                        ).stream().anyMatch(status ->
                                status.equalsIgnoreCase(attempt.getStatus())))
                        .count()
        );

        long completedRecoveryAttempts =
                stats.getSuccessfulRecoveryAttempts()
                        + stats.getFailedRecoveryAttempts();

        if (completedRecoveryAttempts > 0) {

            double successRate =
                    (stats.getSuccessfulRecoveryAttempts() * 100.0)
                            / completedRecoveryAttempts;

            stats.setRecoverySuccessRate(
                    Math.round(successRate * 100.0) / 100.0
            );

        } else {
            stats.setRecoverySuccessRate(0.0);
        }

        return stats;
    }

    public RecentActivityResponse getRecentActivity() {

        List<RecentActivityItem> activities =
                new ArrayList<>();

        List<AiDecision> aiDecisions =
                aiDecisionRepository.findAll();

        for (AiDecision decision : aiDecisions) {
            activities.add(
                    new RecentActivityItem(
                            "AI_DECISION",
                            decision.getId(),
                            decision.getPaymentId(),
                            decision.getDecision(),
                            decision.getReason(),
                            decision.getCreatedAt()
                    )
            );
        }

        List<RecoveryAttempt> recoveryAttempts =
                recoveryAttemptRepository.findAll();

        for (RecoveryAttempt attempt : recoveryAttempts) {
            activities.add(
                    new RecentActivityItem(
                            "RECOVERY_ATTEMPT",
                            attempt.getId(),
                            attempt.getPaymentId(),
                            attempt.getStatus(),
                            attempt.getResponse(),
                            attempt.getAttemptedAt()
                    )
            );
        }

        List<AuditLog> auditLogs =
                auditLogRepository.findAll();

        for (AuditLog auditLog : auditLogs) {
            activities.add(
                    new RecentActivityItem(
                            "AUDIT_LOG",
                            auditLog.getId(),
                            auditLog.getPaymentId(),
                            auditLog.getAction(),
                            auditLog.getDetails(),
                            auditLog.getCreatedAt()
                    )
            );
        }

        activities.sort(
                Comparator.comparing(
                        RecentActivityItem::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        List<RecentActivityItem> recentActivities =
                activities.stream()
                        .limit(20)
                        .toList();

        return new RecentActivityResponse(
                recentActivities
        );
    }
}
