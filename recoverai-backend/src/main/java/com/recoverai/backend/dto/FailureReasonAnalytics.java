package com.recoverai.backend.dto;

public class FailureReasonAnalytics {

    private String reason;
    private long count;

    public FailureReasonAnalytics(
            String reason,
            long count
    ) {
        this.reason = reason;
        this.count = count;
    }

    public String getReason() {
        return reason;
    }

    public long getCount() {
        return count;
    }
}