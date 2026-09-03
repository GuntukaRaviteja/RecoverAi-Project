package com.recoverai.backend.dto;

public record RecoveryOperation(
        Long paymentId,
        String paymentReference,
        Double amount,
        String failureCategory,
        String aiDecision,
        String recoveryAction,
        String recoveryMethod,
        long attempts,
        int maxAttempts,
        String status,
        String customerAction,
        boolean customerActionRequired
) {
}
