package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.service.AiDecisionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-decisions")
public class AiDecisionController {

    private final AiDecisionService aiDecisionService;

    public AiDecisionController(AiDecisionService aiDecisionService) {
        this.aiDecisionService = aiDecisionService;
    }

    // Analyze a payment using the AI decision strategy
    @PostMapping("/{paymentId}")
    public AiDecision analyzePayment(@PathVariable Long paymentId) {
        return aiDecisionService.analyzePayment(paymentId);
    }

    // Get all AI decisions
    @GetMapping("/all")
    public List<AiDecision> getAllDecisions() {
        return aiDecisionService.getAllDecisions();
    }

    // Get AI decision history for a specific payment
    @GetMapping("/payment/{paymentId}")
    public List<AiDecision> getDecisionHistory(
            @PathVariable Long paymentId
    ) {
        return aiDecisionService.getDecisionHistory(paymentId);
    }
}