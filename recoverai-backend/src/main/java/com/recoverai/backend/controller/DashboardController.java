package com.recoverai.backend.controller;

import com.recoverai.backend.dto.DashboardAnalytics;
import com.recoverai.backend.dto.DashboardSummary;
import com.recoverai.backend.dto.FailureReasonAnalytics;
import com.recoverai.backend.dto.DashboardStats;
import com.recoverai.backend.dto.RecentActivityResponse;
import com.recoverai.backend.dto.RecoveryTrend;
import com.recoverai.backend.dto.RecoveryComparison;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final DashboardService dashboardService;

    public DashboardController(
            PaymentRepository paymentRepository,
            RecoveryAttemptRepository recoveryAttemptRepository,
            AiDecisionRepository aiDecisionRepository,
            DashboardService dashboardService
    ) {
        this.paymentRepository = paymentRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStats getDashboardStats() {
        return dashboardService.getDashboardStats();
    }

    @GetMapping("/recent-activity")
    public RecentActivityResponse getRecentActivity() {
        return dashboardService.getRecentActivity();
    }

    @GetMapping("/summary")
    public DashboardSummary getDashboardSummary() {

        long totalPayments = paymentRepository.count();

        long failedPayments =
                paymentRepository.countByStatus("FAILED");

        long recoveredPayments =
                paymentRepository.countByStatus("RECOVERED");

        long pendingRecoveries =
                recoveryAttemptRepository.countByStatus("PENDING");

        long totalRecoveryAttempts =
                recoveryAttemptRepository.count();

        long successfulRecoveryAttempts =
                recoveryAttemptRepository.countByStatus("SUCCESS");

        long failedRecoveryAttempts =
                recoveryAttemptRepository.countByStatus("FAILED");

        long completedRecoveryAttempts =
                successfulRecoveryAttempts + failedRecoveryAttempts;

        double recoverySuccessRate =
                completedRecoveryAttempts > 0
                        ? (successfulRecoveryAttempts * 100.0)
                        / completedRecoveryAttempts
                        : 0.0;

        recoverySuccessRate =
                Math.round(recoverySuccessRate * 100.0) / 100.0;

        return new DashboardSummary(
                totalPayments,
                failedPayments,
                recoveredPayments,
                pendingRecoveries,
                totalRecoveryAttempts,
                recoverySuccessRate
        );
    }

    @GetMapping("/analytics")
    public DashboardAnalytics getDashboardAnalytics() {

        long successfulPayments =
                paymentRepository.countByStatus("SUCCESS");

        long recoveredPayments =
                paymentRepository.countByStatus("RECOVERED");

        long failedPayments =
                paymentRepository.countByStatus("FAILED");

        long pendingRecoveryAttempts =
                recoveryAttemptRepository.findAll().stream()
                        .filter(attempt -> List.of(
                                "PENDING",
                                "WAITING_FOR_CUSTOMER",
                                "WAITING_FOR_PAYMENT_METHOD",
                                "SCHEDULED",
                                "PROCESSING"
                        ).stream().anyMatch(status ->
                                status.equalsIgnoreCase(attempt.getStatus())))
                        .count();

        long successfulRecoveryAttempts =
                recoveryAttemptRepository.countByStatus("SUCCESS");

        long failedRecoveryAttempts =
                recoveryAttemptRepository.countByStatus("FAILED");

        return new DashboardAnalytics(
                successfulPayments,
                recoveredPayments,
                failedPayments,
                pendingRecoveryAttempts,
                successfulRecoveryAttempts,
                failedRecoveryAttempts
        );
    }

    @GetMapping("/recovery-trends")
    public List<RecoveryTrend> getRecoveryTrends() {

        List<RecoveryAttempt> recoveryAttempts =
                recoveryAttemptRepository.findAllByOrderByAttemptedAtAsc();

        Map<LocalDate, long[]> dailyStats =
                new TreeMap<>();

        for (RecoveryAttempt attempt : recoveryAttempts) {

            if (attempt.getAttemptedAt() == null) {
                continue;
            }

            LocalDate date =
                    attempt.getAttemptedAt().toLocalDate();

            long[] stats =
                    dailyStats.computeIfAbsent(
                            date,
                            key -> new long[3]
                    );

            stats[0]++;

            if ("SUCCESS".equals(attempt.getStatus())) {
                stats[1]++;
            }

            if ("FAILED".equals(attempt.getStatus())) {
                stats[2]++;
            }
        }

        return dailyStats.entrySet()
                .stream()
                .map(entry -> new RecoveryTrend(
                        entry.getKey().toString(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2]
                ))
                .toList();
    }

    @GetMapping("/failure-reasons")
    public List<FailureReasonAnalytics> getFailureReasonAnalytics() {

        List<Payment> failedPayments =
                paymentRepository.findByStatus("FAILED");

        Map<String, Long> failureReasonCounts =
                new TreeMap<>();

        for (Payment payment : failedPayments) {

            String category =
                    payment.getFailureCategory();

            if (category == null
                    || category.trim().isEmpty()) {
                category = "UNKNOWN";
            }

            failureReasonCounts.merge(
                    category,
                    1L,
                    Long::sum
            );
        }

        return failureReasonCounts.entrySet()
                .stream()
                .map(entry -> new FailureReasonAnalytics(
                        getFailureCategoryDisplayName(
                                entry.getKey()
                        ),
                        entry.getValue()
                ))
                .sorted(
                        Comparator.comparing(
                                FailureReasonAnalytics::getCount,
                                Comparator.reverseOrder()
                        )
                )
                .toList();
    }

    @GetMapping("/recovery-comparison")
    public RecoveryComparison getRecoveryComparison() {
                List<Payment> failedPaymentCohort = paymentRepository.findAll()
                        .stream()
                        .filter(payment -> "FAILED".equalsIgnoreCase(payment.getStatus())
                                || "RECOVERED".equalsIgnoreCase(payment.getStatus()))
                        .toList();

                Map<Long, Payment> cohortById = new HashMap<>();
                failedPaymentCohort.forEach(payment -> cohortById.put(payment.getId(), payment));
                Map<Long, AiDecision> decisionsByPayment = aiDecisionRepository.findAll().stream()
                        .filter(decision -> cohortById.containsKey(decision.getPaymentId()))
                        .collect(java.util.stream.Collectors.toMap(
                                AiDecision::getPaymentId,
                                decision -> decision,
                                (first, ignored) -> first
                        ));

                List<RecoveryAttempt> cohortAttempts = recoveryAttemptRepository.findAll()
                        .stream()
                        .filter(attempt -> cohortById.containsKey(attempt.getPaymentId()))
                        .toList();

                Map<Long, RecoveryAttempt> firstAttemptByPayment = new HashMap<>();
                cohortAttempts.stream()
                        .sorted(Comparator.comparing(
                                RecoveryAttempt::getAttemptedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                        .forEach(attempt -> firstAttemptByPayment.putIfAbsent(
                                attempt.getPaymentId(),
                                attempt
                        ));

                long recoverAiRecoveredPayments = failedPaymentCohort.stream()
                        .filter(payment -> "RECOVERED".equalsIgnoreCase(payment.getStatus()))
                        .count();
                double revenueAtRisk = failedPaymentCohort.stream()
                        .mapToDouble(payment -> payment.getAmount() == null ? 0 : payment.getAmount())
                        .sum();
                double recoverAiRevenueRecovered = failedPaymentCohort.stream()
                        .filter(payment -> "RECOVERED".equalsIgnoreCase(payment.getStatus()))
                        .mapToDouble(payment -> payment.getAmount() == null ? 0 : payment.getAmount())
                        .sum();
                long blindRetryRecoveredPayments = firstAttemptByPayment.values().stream()
                        .filter(attempt -> "SUCCESS".equalsIgnoreCase(attempt.getStatus())
                                || "RECOVERED".equalsIgnoreCase(attempt.getStatus()))
                        .map(RecoveryAttempt::getPaymentId)
                        .distinct()
                        .count();
                double blindRetryRevenueRecovered = firstAttemptByPayment.values().stream()
                        .filter(attempt -> "SUCCESS".equalsIgnoreCase(attempt.getStatus())
                                || "RECOVERED".equalsIgnoreCase(attempt.getStatus()))
                        .map(RecoveryAttempt::getPaymentId)
                        .map(cohortById::get)
                        .mapToDouble(payment -> payment.getAmount() == null ? 0 : payment.getAmount())
                        .sum();

                double recoverAiRate = percentage(recoverAiRecoveredPayments, failedPaymentCohort.size());
                double blindRetryRate = percentage(blindRetryRecoveredPayments, failedPaymentCohort.size());
                long policyStoppedCases = decisionsByPayment.values().stream()
                        .filter(decision -> "STOP".equalsIgnoreCase(decision.getDecision())
                                || "NO_ACTION".equalsIgnoreCase(decision.getRecoveryAction()))
                        .count();
                long customerActionCases = decisionsByPayment.values().stream()
                        .filter(decision -> "NOTIFY_CUSTOMER".equalsIgnoreCase(decision.getDecision())
                                || "NOTIFY_CUSTOMER".equalsIgnoreCase(decision.getRecoveryAction())
                                || "CUSTOMER_ACTION".equalsIgnoreCase(decision.getDecision()))
                        .count();
                long maxAttemptsReached = cohortAttempts.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                RecoveryAttempt::getPaymentId,
                                java.util.stream.Collectors.counting()
                        ))
                        .values().stream()
                        .filter(count -> count >= com.recoverai.backend.recovery.RecoveryPolicy.MAX_RECOVERY_ATTEMPTS)
                        .count();
                long policyApproved = decisionsByPayment.values().stream()
                        .filter(decision -> !"STOP".equalsIgnoreCase(decision.getDecision())
                                && !"NO_ACTION".equalsIgnoreCase(decision.getRecoveryAction()))
                        .count();
                double additionalRevenueRecovered = recoverAiRevenueRecovered - blindRetryRevenueRecovered;
                double recoveryLiftPercentage = blindRetryRevenueRecovered == 0
                        ? 0
                        : round(additionalRevenueRecovered / blindRetryRevenueRecovered * 100.0);
                long unnecessaryBlindRetries =
                        customerActionCases + policyStoppedCases;

                return new RecoveryComparison(
                        failedPaymentCohort.size(),
                        decisionsByPayment.size(),
                        policyApproved,
                        firstAttemptByPayment.size(),
                        round(revenueAtRisk),
                        recoverAiRecoveredPayments,
                        round(recoverAiRevenueRecovered),
                        recoverAiRate,
                        cohortAttempts.size(),
                        blindRetryRecoveredPayments,
                        round(blindRetryRevenueRecovered),
                        blindRetryRate,
                        firstAttemptByPayment.size(),
                        round(recoverAiRate - blindRetryRate),
                        round(additionalRevenueRecovered),
                        recoveryLiftPercentage,
                        unnecessaryBlindRetries,
                        customerActionCases,
                        policyStoppedCases,
                        maxAttemptsReached
                );
    }

    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String getFailureCategoryDisplayName(
            String category
    ) {

        return switch (category) {
            case "INSUFFICIENT_FUNDS" ->
                    "Insufficient Funds";

            case "SUSPECTED_FRAUD" ->
                    "Suspected Fraud";

            case "ACCOUNT_CLOSED" ->
                    "Account Closed";

            case "CARD_EXPIRED", "EXPIRED_CARD" ->
                    "Card Expired";

            case "CARD_DECLINED" ->
                    "Card Declined";

            case "PAYMENT_DECLINED" ->
                    "Payment Declined";

            case "TRANSACTION_LIMIT_EXCEEDED" ->
                    "Transaction Limit Exceeded";

            case "INVALID_ACCOUNT" ->
                    "Invalid Account";

            case "NETWORK_ERROR", "NETWORK_TECHNICAL_FAILURE" ->
                    "Temporary Network Error";

            case "OTHER" ->
                    "Other";

            default ->
                    "Unknown";
        };
    }
}