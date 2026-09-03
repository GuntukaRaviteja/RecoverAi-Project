package com.recoverai.backend.strategy;

public interface AiDecisionStrategy {

    DecisionResult makeDecision(
            String failureCategory,
            String failureReason
    );
}