package com.recoverai.backend.dto;

import java.time.LocalDateTime;

public class RecentActivityItem {

    private String type;
    private Long referenceId;
    private Long paymentId;
    private String action;
    private String details;
    private LocalDateTime createdAt;

    public RecentActivityItem() {
    }

    public RecentActivityItem(
            String type,
            Long referenceId,
            Long paymentId,
            String action,
            String details,
            LocalDateTime createdAt
    ) {
        this.type = type;
        this.referenceId = referenceId;
        this.paymentId = paymentId;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}