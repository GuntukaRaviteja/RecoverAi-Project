package com.recoverai.backend.controller;

import com.recoverai.backend.dto.AlternativePaymentMethodRequest;
import com.recoverai.backend.dto.PromiseToPayRequest;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.service.RecoveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {


    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    // Create a recovery attempt
    @PostMapping("/{paymentId}")
    public RecoveryAttempt createRecoveryAttempt(
            @PathVariable Long paymentId
    ) {
        return recoveryService.createRecoveryAttempt(paymentId);
    }

    // Get recovery history for a payment
    @GetMapping("/payment/{paymentId}")
    public List<RecoveryAttempt> getRecoveryHistory(
            @PathVariable Long paymentId
    ) {
        return recoveryService.getRecoveryHistory(paymentId);
    }

    // Customer chooses to retry the payment later
    @PostMapping("/attempts/{recoveryAttemptId}/schedule")
    public RecoveryAttempt scheduleRetry(
            @PathVariable Long recoveryAttemptId,
            @RequestParam LocalDateTime scheduledRetryAt
    ) {
        return recoveryService.scheduleRetry(
                recoveryAttemptId,
                scheduledRetryAt
        );
    }

    // Alias used by the customer recovery experience.
    @PostMapping("/attempts/{recoveryAttemptId}/retry-later")
    public RecoveryAttempt retryLater(
            @PathVariable Long recoveryAttemptId,
            @RequestParam LocalDateTime scheduledRetryAt
    ) {
        return recoveryService.scheduleRetry(recoveryAttemptId, scheduledRetryAt);
    }

    @PostMapping("/attempts/{recoveryAttemptId}/promise-to-pay")
    public RecoveryAttempt promiseToPay(
            @PathVariable Long recoveryAttemptId,
            @Valid @RequestBody PromiseToPayRequest request
    ) {
        return recoveryService.promiseToPay(recoveryAttemptId, request);
    }

    @PostMapping("/attempts/{recoveryAttemptId}/notify")
    public RecoveryAttempt queueCustomerOutreach(
            @PathVariable Long recoveryAttemptId
    ) {
        return recoveryService.queueCustomerOutreach(recoveryAttemptId);
    }

    // Customer chooses another payment method or account
    @PostMapping("/attempts/{recoveryAttemptId}/choose-payment-method")
    public RecoveryAttempt chooseAnotherPaymentMethod(
            @PathVariable Long recoveryAttemptId
    ) {
        return recoveryService.chooseAnotherPaymentMethod(
                recoveryAttemptId
        );
    }

    // Customer submits the alternative payment method
    @PostMapping("/attempts/{recoveryAttemptId}/alternative-payment-method")
    public RecoveryAttempt submitAlternativePaymentMethod(
            @PathVariable Long recoveryAttemptId,
            @Valid @RequestBody AlternativePaymentMethodRequest request
    ) {
        return recoveryService.submitAlternativePaymentMethod(
                recoveryAttemptId,
                request
        );
    }

    // Update recovery attempt status manually
    @PutMapping("/{recoveryAttemptId}/status")
    public RecoveryAttempt updateRecoveryStatus(
            @PathVariable Long recoveryAttemptId,
            @RequestParam String status,
            @RequestParam String response
    ) {
        return recoveryService.updateRecoveryStatus(
                recoveryAttemptId,
                status,
                response
        );
    }

    // Execute a recovery attempt
    @PostMapping("/attempts/{recoveryAttemptId}/execute")
    public RecoveryAttempt executeRecoveryAttempt(
            @PathVariable Long recoveryAttemptId
    ) {
        return recoveryService.executeRecoveryAttempt(recoveryAttemptId);
    }

}
