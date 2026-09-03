

package com.recoverai.backend.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;

import java.util.List;
import java.util.Map;

public class LlmDecisionStrategy implements AiDecisionStrategy {

    private static final String MODEL_NAME =
            "gemini-3.6-flash";

    private final Client geminiClient;
    private final ObjectMapper objectMapper;

    public LlmDecisionStrategy(Client geminiClient) {
        this.geminiClient = geminiClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DecisionResult makeDecision(
            String failureCategory,
            String failureReason
    ) {

        String prompt = """
                You are an AI system for a payment recovery application.

                Analyze the payment failure category and the original
                payment failure reason. Choose the most appropriate
                decision and recovery action.

                Allowed decisions:
                RETRY
                NOTIFY_CUSTOMER
                STOP

                Allowed recovery actions:
                CREATE_RECOVERY_ATTEMPT
                NOTIFY_CUSTOMER
                NO_ACTION

                The decision and recovery action must be logically matched:

                RETRY -> CREATE_RECOVERY_ATTEMPT
                NOTIFY_CUSTOMER -> NOTIFY_CUSTOMER
                STOP -> NO_ACTION

                Payment failure category:
                %s

                Original payment failure reason:
                %s
                """.formatted(
                failureCategory != null
                        ? failureCategory
                        : "UNKNOWN",
                failureReason != null
                        ? failureReason
                        : "No failure reason provided"
        );

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .responseSchema(createResponseSchema())
                        .build();

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        MODEL_NAME,
                        prompt,
                        config
                );

        String aiResponse = response.text();

        if (aiResponse == null || aiResponse.isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned an empty decision response"
            );
        }

        return parseDecision(aiResponse);
    }

    private Schema createResponseSchema() {

        Schema decisionSchema = Schema.builder()
                .type("STRING")
                .enum_(List.of(
                        "RETRY",
                        "NOTIFY_CUSTOMER",
                        "STOP"
                ))
                .build();

        Schema actionSchema = Schema.builder()
                .type("STRING")
                .enum_(List.of(
                        "CREATE_RECOVERY_ATTEMPT",
                        "NOTIFY_CUSTOMER",
                        "NO_ACTION"
                ))
                .build();

        Schema confidenceSchema = Schema.builder()
                .type("NUMBER")
                .build();

        Schema reasonSchema = Schema.builder()
                .type("STRING")
                .build();

        return Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "decision", decisionSchema,
                        "action", actionSchema,
                        "confidence", confidenceSchema,
                        "reason", reasonSchema
                ))
                .required(List.of(
                        "decision",
                        "action",
                        "confidence",
                        "reason"
                ))
                .build();
    }

    private DecisionResult parseDecision(String aiResponse) {

        try {
            JsonNode root =
                    objectMapper.readTree(aiResponse);

            String decisionValue =
                    root.get("decision")
                            .asText()
                            .trim();

            String actionValue =
                    root.get("action")
                            .asText()
                            .trim();

            double confidenceScore =
                    root.get("confidence")
                            .asDouble();

            String reason =
                    root.get("reason")
                            .asText()
                            .trim();

            DecisionType decision =
                    DecisionType.valueOf(
                            decisionValue
                    );

            RecoveryAction recoveryAction =
                    RecoveryAction.valueOf(
                            actionValue
                    );

            if (confidenceScore < 0
                    || confidenceScore > 1) {

                throw new IllegalStateException(
                        "Gemini returned an invalid confidence score"
                );
            }

            return new DecisionResult(
                    decision,
                    recoveryAction,
                    reason,
                    confidenceScore,
                    DecisionSource.GEMINI_AI
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to parse Gemini structured response: "
                            + aiResponse,
                    exception
            );
        }
    }
}