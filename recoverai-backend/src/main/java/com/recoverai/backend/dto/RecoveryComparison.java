package com.recoverai.backend.dto;

public record RecoveryComparison(
        long paymentCohort,
        long aiAnalyzed,
        long policyApproved,
        long recoveryAttempted,
        double revenueAtRisk,
        long recoverAiRecoveredPayments,
        double recoverAiRevenueRecovered,
        double recoverAiRecoveryRate,
        long recoverAiAttempts,
        long blindRetryRecoveredPayments,
        double blindRetryRevenueRecovered,
        double blindRetryRecoveryRate,
        long blindRetryAttempts,
        double recoveryRateImprovement,
        double additionalRevenueRecovered,
        double recoveryLiftPercentage,
        long unnecessaryBlindRetries,
        long customerActionCases,
        long policyStoppedCases,
        long maxAttemptsReached
) {
}
