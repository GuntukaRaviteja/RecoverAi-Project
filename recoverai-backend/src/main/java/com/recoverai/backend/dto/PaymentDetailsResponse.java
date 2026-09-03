package com.recoverai.backend.dto;

import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;

import java.util.List;

public class PaymentDetailsResponse {

    private Payment payment;

    private List<AiDecision> aiDecisions;

    private List<RecoveryAttempt> recoveryAttempts;

    private List<AuditLog> auditLogs;

    public PaymentDetailsResponse() {
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<AiDecision> getAiDecisions() {
        return aiDecisions;
    }

    public void setAiDecisions(List<AiDecision> aiDecisions) {
        this.aiDecisions = aiDecisions;
    }

    public List<RecoveryAttempt> getRecoveryAttempts() {
        return recoveryAttempts;
    }

    public void setRecoveryAttempts(
            List<RecoveryAttempt> recoveryAttempts
    ) {
        this.recoveryAttempts = recoveryAttempts;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }
}