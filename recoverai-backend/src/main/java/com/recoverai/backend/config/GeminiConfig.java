package com.recoverai.backend.config;

import com.google.genai.Client;
import com.recoverai.backend.strategy.AiDecisionStrategy;
import com.recoverai.backend.strategy.FallbackAiDecisionStrategy;
import com.recoverai.backend.strategy.LlmDecisionStrategy;
import com.recoverai.backend.strategy.RuleBasedDecisionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GeminiConfig {

    @Bean
    @Primary
    public AiDecisionStrategy aiDecisionStrategy(
            RuleBasedDecisionStrategy ruleBasedDecisionStrategy) {

        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        // A missing provider key is an expected deployment configuration, not
        // an application failure. Keep recovery available through deterministic rules.
        if (apiKey == null || apiKey.isBlank()) {
            return ruleBasedDecisionStrategy;
        }

        return new FallbackAiDecisionStrategy(
                new LlmDecisionStrategy(new Client()),
                ruleBasedDecisionStrategy
        );
    }
}
