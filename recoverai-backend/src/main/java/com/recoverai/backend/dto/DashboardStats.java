package com.recoverai.backend.dto;

public class DashboardStats {

    private long totalPayments;
    private long failedPayments;
    private long recoveredPayments;
    private long activeRecoveryCases;

    private long totalAiDecisions;
    private long geminiAiDecisions;

    private long retryDecisions;
    private long notifyCustomerDecisions;
    private long stopDecisions;

    private long totalRecoveryAttempts;
    private long successfulRecoveryAttempts;
    private long failedRecoveryAttempts;
    private long pendingRecoveryAttempts;

    private double recoverySuccessRate;

    public DashboardStats() {
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(long failedPayments) {
        this.failedPayments = failedPayments;
    }

    public long getRecoveredPayments() {
        return recoveredPayments;
    }

    public void setRecoveredPayments(long recoveredPayments) {
        this.recoveredPayments = recoveredPayments;
    }

    public long getActiveRecoveryCases() {
        return activeRecoveryCases;
    }

    public void setActiveRecoveryCases(long activeRecoveryCases) {
        this.activeRecoveryCases = activeRecoveryCases;
    }

    public long getTotalAiDecisions() {
        return totalAiDecisions;
    }

    public void setTotalAiDecisions(long totalAiDecisions) {
        this.totalAiDecisions = totalAiDecisions;
    }

    public long getGeminiAiDecisions() {
        return geminiAiDecisions;
    }

    public void setGeminiAiDecisions(long geminiAiDecisions) {
        this.geminiAiDecisions = geminiAiDecisions;
    }

    public long getRetryDecisions() {
        return retryDecisions;
    }

    public void setRetryDecisions(long retryDecisions) {
        this.retryDecisions = retryDecisions;
    }

    public long getNotifyCustomerDecisions() {
        return notifyCustomerDecisions;
    }

    public void setNotifyCustomerDecisions(long notifyCustomerDecisions) {
        this.notifyCustomerDecisions = notifyCustomerDecisions;
    }

    public long getStopDecisions() {
        return stopDecisions;
    }

    public void setStopDecisions(long stopDecisions) {
        this.stopDecisions = stopDecisions;
    }

    public long getTotalRecoveryAttempts() {
        return totalRecoveryAttempts;
    }

    public void setTotalRecoveryAttempts(long totalRecoveryAttempts) {
        this.totalRecoveryAttempts = totalRecoveryAttempts;
    }

    public long getSuccessfulRecoveryAttempts() {
        return successfulRecoveryAttempts;
    }

    public void setSuccessfulRecoveryAttempts(
            long successfulRecoveryAttempts
    ) {
        this.successfulRecoveryAttempts =
                successfulRecoveryAttempts;
    }

    public long getFailedRecoveryAttempts() {
        return failedRecoveryAttempts;
    }

    public void setFailedRecoveryAttempts(
            long failedRecoveryAttempts
    ) {
        this.failedRecoveryAttempts =
                failedRecoveryAttempts;
    }

    public long getPendingRecoveryAttempts() {
        return pendingRecoveryAttempts;
    }

    public void setPendingRecoveryAttempts(
            long pendingRecoveryAttempts
    ) {
        this.pendingRecoveryAttempts =
                pendingRecoveryAttempts;
    }

    public double getRecoverySuccessRate() {
        return recoverySuccessRate;
    }

    public void setRecoverySuccessRate(
            double recoverySuccessRate
    ) {
        this.recoverySuccessRate = recoverySuccessRate;
    }
}
