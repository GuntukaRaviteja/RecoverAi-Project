package com.recoverai.backend.controller;

import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.dto.RecoveryOperation;
import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.recovery.RecoveryPolicy;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recovery-attempts")
@Validated
public class RecoveryAttemptController {

    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;

    public RecoveryAttemptController(
            RecoveryAttemptRepository recoveryAttemptRepository,
            PaymentRepository paymentRepository,
            AiDecisionRepository aiDecisionRepository
    ) {
        this.recoveryAttemptRepository =
                recoveryAttemptRepository;
        this.paymentRepository = paymentRepository;
        this.aiDecisionRepository = aiDecisionRepository;
    }

    @GetMapping
    public List<RecoveryOperation> getRecoveryOperations() {
        Map<Long, List<RecoveryAttempt>> attemptsByPayment = new HashMap<>();
        recoveryAttemptRepository.findAll().forEach(attempt ->
                attemptsByPayment
                        .computeIfAbsent(attempt.getPaymentId(), ignored -> new ArrayList<>())
                        .add(attempt)
        );

        return paymentRepository.findAll().stream()
                .filter(payment -> attemptsByPayment.containsKey(payment.getId())
                        || aiDecisionRepository.existsByPaymentId(payment.getId()))
                .map(payment -> toRecoveryOperation(
                        payment,
                        attemptsByPayment.getOrDefault(payment.getId(), List.of())
                ))
                .sorted(Comparator.comparing(
                        RecoveryOperation::paymentId,
                        Comparator.reverseOrder()
                ))
                .toList();
    }

    private RecoveryOperation toRecoveryOperation(
            Payment payment,
            List<RecoveryAttempt> attempts
    ) {
        RecoveryAttempt latestAttempt = attempts.stream()
                .max(Comparator.comparing(
                        RecoveryAttempt::getAttemptedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(null);
        AiDecision aiDecision = aiDecisionRepository
                .findByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .stream()
                .findFirst()
                .orElse(null);
        String status = latestAttempt != null
                ? latestAttempt.getStatus()
                : ("STOP".equalsIgnoreCase(aiDecision == null ? null : aiDecision.getDecision())
                || "NO_ACTION".equalsIgnoreCase(aiDecision == null ? null : aiDecision.getRecoveryAction())
                ? "STOPPED"
                : ("RECOVERED".equalsIgnoreCase(payment.getStatus())
                ? "SUCCESS"
                : payment.getStatus()));
        String customerAction = latestAttempt == null
                ? null
                : latestAttempt.getCustomerAction();
        boolean customerActionRequired = List.of(
                "WAITING_FOR_CUSTOMER",
                "WAITING_FOR_PAYMENT_METHOD"
        ).contains(status);

        return new RecoveryOperation(
                payment.getId(),
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getFailureCategory(),
                aiDecision == null ? null : aiDecision.getDecision(),
                aiDecision == null ? null : aiDecision.getRecoveryAction(),
                latestAttempt == null ? null : latestAttempt.getRecoveryMethod(),
                attempts.size(),
                RecoveryPolicy.MAX_RECOVERY_ATTEMPTS,
                status,
                customerAction,
                customerActionRequired
        );
    }

    // Get recovery history for a payment with clean pagination response
    @GetMapping("/payment/{paymentId}")
    public PageResponse<RecoveryAttempt> getRecoveryHistory(
            @PathVariable Long paymentId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not be greater than 100")
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("attemptedAt").descending()
        );

        Page<RecoveryAttempt> recoveryAttemptPage =
                recoveryAttemptRepository.findByPaymentId(
                        paymentId,
                        pageable
                );

        return new PageResponse<>(recoveryAttemptPage);
    }
}