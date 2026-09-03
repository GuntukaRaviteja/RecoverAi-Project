package com.recoverai.backend.dto;

public class DashboardAnalytics {

    private long successfulPayments;
    private long recoveredPayments;
    private long failedPayments;

    private long pendingRecoveryAttempts;
    private long successfulRecoveryAttempts;
    private long failedRecoveryAttempts;

    public DashboardAnalytics(
            long successfulPayments,
            long recoveredPayments,
            long failedPayments,
            long pendingRecoveryAttempts,
            long successfulRecoveryAttempts,
            long failedRecoveryAttempts
    ) {
        this.successfulPayments = successfulPayments;
        this.recoveredPayments = recoveredPayments;
        this.failedPayments = failedPayments;
        this.pendingRecoveryAttempts = pendingRecoveryAttempts;
        this.successfulRecoveryAttempts = successfulRecoveryAttempts;
        this.failedRecoveryAttempts = failedRecoveryAttempts;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public long getRecoveredPayments() {
        return recoveredPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public long getPendingRecoveryAttempts() {
        return pendingRecoveryAttempts;
    }

    public long getSuccessfulRecoveryAttempts() {
        return successfulRecoveryAttempts;
    }

    public long getFailedRecoveryAttempts() {
        return failedRecoveryAttempts;
    }
}