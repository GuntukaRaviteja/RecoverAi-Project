package com.recoverai.backend.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackAiDecisionStrategy implements AiDecisionStrategy {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    FallbackAiDecisionStrategy.class
            );

    private final AiDecisionStrategy primaryStrategy;
    private final AiDecisionStrategy fallbackStrategy;

    public FallbackAiDecisionStrategy(
            AiDecisionStrategy primaryStrategy,
            AiDecisionStrategy fallbackStrategy
    ) {
        this.primaryStrategy = primaryStrategy;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public DecisionResult makeDecision(
            String failureCategory,
            String failureReason
    ) {

        try {
            return primaryStrategy.makeDecision(
                    failureCategory,
                    failureReason
            );

        } catch (Exception exception) {

            logger.error(
                    "Gemini AI decision failed. Using rule-based fallback.",
                    exception
            );

            return fallbackStrategy.makeDecision(
                    failureCategory,
                    failureReason
            );
        }
    }
}