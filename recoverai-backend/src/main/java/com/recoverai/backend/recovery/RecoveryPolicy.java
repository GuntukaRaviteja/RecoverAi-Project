package com.recoverai.backend.recovery;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.strategy.DecisionResult;
import com.recoverai.backend.strategy.DecisionType;
import com.recoverai.backend.strategy.RecoveryAction;
import org.springframework.stereotype.Component;

/**
 * Deterministic guardrails for recovery. AI can recommend a path, but it
 * cannot bypass customer protection or retry limits defined here.
 */
@Component
public class RecoveryPolicy {

    public static final int MAX_RECOVERY_ATTEMPTS = 5;

    public boolean requiresCustomerAction(Payment payment) {
        return "INSUFFICIENT_FUNDS".equalsIgnoreCase(payment.getFailureCategory());
    }

    public boolean allowsAutomaticRecovery(Payment payment) {
        return !requiresCustomerAction(payment)
                && !isNonRecoverable(payment.getFailureCategory());
    }

    public boolean canCreateAttempt(Payment payment, long attemptCount, boolean activeAttemptExists) {
        return "FAILED".equalsIgnoreCase(payment.getStatus())
                && attemptCount < MAX_RECOVERY_ATTEMPTS
                && !activeAttemptExists
                && !isNonRecoverable(payment.getFailureCategory());
    }

    public DecisionResult apply(Payment payment, long attemptCount, DecisionResult recommendation) {
        if (isNonRecoverable(payment.getFailureCategory())) {
            return new DecisionResult(DecisionType.STOP, RecoveryAction.NO_ACTION,
                    "Recovery policy stopped processing because this failure category is not safely recoverable.",
                    1.0, recommendation.getDecisionSource());
        }
        if (attemptCount >= MAX_RECOVERY_ATTEMPTS) {
            return new DecisionResult(DecisionType.STOP, RecoveryAction.NO_ACTION,
                    "Recovery policy stopped processing because the maximum of " + MAX_RECOVERY_ATTEMPTS + " attempts has been reached.",
                    1.0, recommendation.getDecisionSource());
        }
        if (requiresCustomerAction(payment)) {
            return new DecisionResult(DecisionType.NOTIFY_CUSTOMER, RecoveryAction.CREATE_RECOVERY_ATTEMPT,
                    "Recovery policy requires customer-guided recovery for insufficient funds.",
                    recommendation.getConfidenceScore(), recommendation.getDecisionSource());
        }
        return recommendation;
    }

    private boolean isNonRecoverable(String failureCategory) {
        return "SUSPECTED_FRAUD".equalsIgnoreCase(failureCategory)
                || "ACCOUNT_CLOSED".equalsIgnoreCase(failureCategory)
                || "INVALID_ACCOUNT".equalsIgnoreCase(failureCategory);
    }
}
