package com.recoverai.backend.recovery;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.strategy.DecisionResult;
import com.recoverai.backend.strategy.DecisionSource;
import com.recoverai.backend.strategy.DecisionType;
import com.recoverai.backend.strategy.RecoveryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryPolicyTest {

    private final RecoveryPolicy policy = new RecoveryPolicy();

    @Test
    void insufficientFundsIsAlwaysCustomerGuided() {
        Payment payment = failedPayment("INSUFFICIENT_FUNDS");
        DecisionResult approved = policy.apply(payment, 0, new DecisionResult(
                DecisionType.STOP, RecoveryAction.NO_ACTION, "Do nothing", 0.8, DecisionSource.GEMINI_AI));

        assertEquals(DecisionType.NOTIFY_CUSTOMER, approved.getDecision());
        assertEquals(RecoveryAction.CREATE_RECOVERY_ATTEMPT, approved.getRecoveryAction());
        assertTrue(policy.requiresCustomerAction(payment));
        assertFalse(policy.allowsAutomaticRecovery(payment));
    }

    @Test
    void nonRecoverableFailureCannotBeOverriddenByAi() {
        Payment payment = failedPayment("SUSPECTED_FRAUD");
        DecisionResult approved = policy.apply(payment, 0, retryRecommendation());

        assertEquals(DecisionType.STOP, approved.getDecision());
        assertEquals(RecoveryAction.NO_ACTION, approved.getRecoveryAction());
    }

    @Test
    void activeOrExhaustedRecoveryCannotCreateAnotherAttempt() {
        Payment payment = failedPayment("NETWORK_ERROR");

        assertTrue(policy.canCreateAttempt(payment, 0, false));
        assertFalse(policy.canCreateAttempt(payment, 0, true));
        assertFalse(policy.canCreateAttempt(payment, RecoveryPolicy.MAX_RECOVERY_ATTEMPTS, false));
    }

    private Payment failedPayment(String category) {
        Payment payment = new Payment();
        payment.setStatus("FAILED");
        payment.setFailureCategory(category);
        return payment;
    }

    private DecisionResult retryRecommendation() {
        return new DecisionResult(DecisionType.RETRY, RecoveryAction.CREATE_RECOVERY_ATTEMPT,
                "Retry recommended", 0.8, DecisionSource.GEMINI_AI);
    }
}
