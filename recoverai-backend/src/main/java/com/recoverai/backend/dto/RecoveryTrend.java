package com.recoverai.backend.dto;

public class RecoveryTrend {

    private String date;
    private long totalAttempts;
    private long successfulAttempts;
    private long failedAttempts;

    public RecoveryTrend(
            String date,
            long totalAttempts,
            long successfulAttempts,
            long failedAttempts
    ) {
        this.date = date;
        this.totalAttempts = totalAttempts;
        this.successfulAttempts = successfulAttempts;
        this.failedAttempts = failedAttempts;
    }

    public String getDate() {
        return date;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public long getSuccessfulAttempts() {
        return successfulAttempts;
    }

    public long getFailedAttempts() {
        return failedAttempts;
    }
}