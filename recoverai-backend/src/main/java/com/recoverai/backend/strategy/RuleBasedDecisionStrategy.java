package com.recoverai.backend.strategy;

import org.springframework.stereotype.Component;

@Component("ruleBasedDecisionStrategy")
public class RuleBasedDecisionStrategy implements AiDecisionStrategy {

    @Override
    public DecisionResult makeDecision(
            String failureCategory,
            String failureReason
    ) {

        if (failureCategory == null
                || failureCategory.trim().isEmpty()) {

            return new DecisionResult(
                    DecisionType.STOP,
                    RecoveryAction.NO_ACTION,
                    "No failure category is available for this payment",
                    0.60,
                    DecisionSource.RULE_BASED_FALLBACK
            );
        }

        return switch (failureCategory) {

            case "INSUFFICIENT_FUNDS" ->
                    new DecisionResult(
                            DecisionType.RETRY,
                            RecoveryAction.CREATE_RECOVERY_ATTEMPT,
                            "Customer may have sufficient funds later",
                            0.90,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "NETWORK_ERROR" ->
                    new DecisionResult(
                            DecisionType.RETRY,
                            RecoveryAction.CREATE_RECOVERY_ATTEMPT,
                            "Temporary network issues may be resolved by retrying",
                            0.95,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "CARD_DECLINED" ->
                    new DecisionResult(
                            DecisionType.RETRY,
                            RecoveryAction.CREATE_RECOVERY_ATTEMPT,
                            "The payment may succeed after retrying",
                            0.75,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "CARD_EXPIRED" ->
                    new DecisionResult(
                            DecisionType.NOTIFY_CUSTOMER,
                            RecoveryAction.NOTIFY_CUSTOMER,
                            "Customer needs to update the expired payment method",
                            0.95,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "ACCOUNT_CLOSED" ->
                    new DecisionResult(
                            DecisionType.STOP,
                            RecoveryAction.NO_ACTION,
                            "The payment account is closed and cannot be recovered",
                            0.99,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "SUSPECTED_FRAUD" ->
                    new DecisionResult(
                            DecisionType.STOP,
                            RecoveryAction.NO_ACTION,
                            "Recovery should not continue because the payment is suspected of fraud",
                            0.99,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "INVALID_ACCOUNT" ->
                    new DecisionResult(
                            DecisionType.STOP,
                            RecoveryAction.NO_ACTION,
                            "The payment account details are invalid",
                            0.98,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "PAYMENT_DECLINED" ->
                    new DecisionResult(
                            DecisionType.NOTIFY_CUSTOMER,
                            RecoveryAction.NOTIFY_CUSTOMER,
                            "Customer should be notified that the payment was declined",
                            0.80,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "TRANSACTION_LIMIT_EXCEEDED" ->
                    new DecisionResult(
                            DecisionType.NOTIFY_CUSTOMER,
                            RecoveryAction.NOTIFY_CUSTOMER,
                            "Customer should be notified about the transaction limit",
                            0.90,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            case "OTHER" ->
                    new DecisionResult(
                            DecisionType.NOTIFY_CUSTOMER,
                            RecoveryAction.NOTIFY_CUSTOMER,
                            "The failure requires customer attention",
                            0.65,
                            DecisionSource.RULE_BASED_FALLBACK
                    );

            default ->
                    new DecisionResult(
                            DecisionType.STOP,
                            RecoveryAction.NO_ACTION,
                            "No suitable recovery strategy identified",
                            0.60,
                            DecisionSource.RULE_BASED_FALLBACK
                    );
        };
    }
}