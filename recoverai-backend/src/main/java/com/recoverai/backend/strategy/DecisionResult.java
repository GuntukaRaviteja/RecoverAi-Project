package com.recoverai.backend.strategy;

public class DecisionResult {

    private final DecisionType decision;
    private final RecoveryAction recoveryAction;
    private final String reason;
    private final double confidenceScore;
    private final DecisionSource decisionSource;

    public DecisionResult(
            DecisionType decision,
            RecoveryAction recoveryAction,
            String reason,
            double confidenceScore,
            DecisionSource decisionSource) {

        this.decision = decision;
        this.recoveryAction = recoveryAction;
        this.reason = reason;
        this.confidenceScore = confidenceScore;
        this.decisionSource = decisionSource;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public RecoveryAction getRecoveryAction() {
        return recoveryAction;
    }

    public String getReason() {
        return reason;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public DecisionSource getDecisionSource() {
        return decisionSource;
    }
}