package com.recoverai.backend.dto;

public class DashboardSummary {

    private long totalPayments;
    private long failedPayments;
    private long recoveredPayments;
    private long pendingRecoveries;
    private long totalRecoveryAttempts;
    private double recoverySuccessRate;

    public DashboardSummary(
            long totalPayments,
            long failedPayments,
            long recoveredPayments,
            long pendingRecoveries,
            long totalRecoveryAttempts,
            double recoverySuccessRate
    ) {
        this.totalPayments = totalPayments;
        this.failedPayments = failedPayments;
        this.recoveredPayments = recoveredPayments;
        this.pendingRecoveries = pendingRecoveries;
        this.totalRecoveryAttempts = totalRecoveryAttempts;
        this.recoverySuccessRate = recoverySuccessRate;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public long getRecoveredPayments() {
        return recoveredPayments;
    }

    public long getPendingRecoveries() {
        return pendingRecoveries;
    }

    public long getTotalRecoveryAttempts() {
        return totalRecoveryAttempts;
    }

    public double getRecoverySuccessRate() {
        return recoverySuccessRate;
    }
}