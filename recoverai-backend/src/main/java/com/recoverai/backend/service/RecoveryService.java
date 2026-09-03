package com.recoverai.backend.service;

import com.recoverai.backend.dto.AlternativePaymentMethodRequest;
import com.recoverai.backend.dto.PromiseToPayRequest;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.recovery.RecoveryPolicy;
import com.recoverai.backend.recovery.RecoveryStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecoveryService {


    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final PaymentRepository paymentRepository;
    private final RecoveryPolicy recoveryPolicy;
    private final AuditLogService auditLogService;
    private final OutreachService outreachService;

    public RecoveryService(
            RecoveryAttemptRepository recoveryAttemptRepository,
            PaymentRepository paymentRepository,
            RecoveryPolicy recoveryPolicy,
            AuditLogService auditLogService,
            OutreachService outreachService
    ) {
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.paymentRepository = paymentRepository;
        this.recoveryPolicy = recoveryPolicy;
        this.auditLogService = auditLogService;
        this.outreachService = outreachService;
    }

    @Transactional
    public RecoveryAttempt createRecoveryAttempt(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found with ID: " + paymentId
                ));

        long recoveryAttemptCount =
                recoveryAttemptRepository.countByPaymentId(paymentId);
        boolean activeAttemptExists = recoveryAttemptRepository.existsByPaymentIdAndStatusIn(
                paymentId, activeStatusNames());

        if (!recoveryPolicy.canCreateAttempt(payment, recoveryAttemptCount, activeAttemptExists)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    creationBlockedMessage(payment, recoveryAttemptCount, activeAttemptExists)
            );
        }

        RecoveryAttempt recoveryAttempt =
                new RecoveryAttempt();

        recoveryAttempt.setPaymentId(payment.getId());
        recoveryAttempt.setRecoveryMethod("RETRY");
        recoveryAttempt.setAttemptedAt(LocalDateTime.now());
        recoveryAttempt.setFailureReason(
                payment.getFailureCategory()
        );

        if (recoveryPolicy.requiresCustomerAction(payment)) {

            recoveryAttempt.setStatus(RecoveryStatus.WAITING_FOR_CUSTOMER.name());
            recoveryAttempt.setCustomerNotifiedAt(
                    LocalDateTime.now()
            );
            recoveryAttempt.setResponse(
                    "Payment recovery requires customer action. "
                            + "The customer has been notified about insufficient funds "
                            + "and can choose to retry later or use another payment method."
            );

        } else {

            recoveryAttempt.setStatus(RecoveryStatus.PENDING.name());
            recoveryAttempt.setResponse(
                    "Recovery attempt initiated. Attempt "
                            + (recoveryAttemptCount + 1)
                            + " of "
                            + RecoveryPolicy.MAX_RECOVERY_ATTEMPTS
            );
        }

        RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(recoveryAttempt);
        auditLogService.createAuditLog(paymentId, "RECOVERY_ATTEMPT_CREATED",
                "Created recovery attempt " + savedAttempt.getId() + " in state " + savedAttempt.getStatus());

        if (RecoveryStatus.WAITING_FOR_CUSTOMER.name().equals(savedAttempt.getStatus())) {
            outreachService.queueRecoveryOutreach(savedAttempt,
                    "Customer action is required before another recovery attempt.");
        }
        return savedAttempt;
    }

    @Transactional
    public RecoveryAttempt promiseToPay(Long recoveryAttemptId, PromiseToPayRequest request) {
        RecoveryAttempt recoveryAttempt = getRecoveryAttempt(recoveryAttemptId);
        if (RecoveryStatus.from(recoveryAttempt.getStatus()) != RecoveryStatus.WAITING_FOR_CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A promise to pay can only be recorded when customer action is required");
        }

        LocalDateTime deadline = request.getPromiseToPayDeadline();
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The promise-to-pay deadline must be in the future");
        }

        recoveryAttempt.setCustomerAction("PROMISE_TO_PAY");
        recoveryAttempt.setPromiseToPayDeadline(deadline);
        recoveryAttempt.setScheduledRetryAt(deadline);
        transition(recoveryAttempt, RecoveryStatus.SCHEDULED,
                "Customer promised to pay by " + deadline);
        recoveryAttempt.setResponse(
                "Customer promised to complete payment by " + deadline
                        + ". RecoverAI will not retry or send reminders before this deadline."
        );
        return recoveryAttemptRepository.save(recoveryAttempt);
    }

    @Transactional
    public RecoveryAttempt queueCustomerOutreach(Long recoveryAttemptId) {
        RecoveryAttempt recoveryAttempt = getRecoveryAttempt(recoveryAttemptId);
        outreachService.requestCustomerOutreach(recoveryAttempt);
        return recoveryAttempt;
    }

    @Transactional
    public RecoveryAttempt scheduleRetry(
            Long recoveryAttemptId,
            LocalDateTime scheduledRetryAt
    ) {

        if (scheduledRetryAt == null
                || !scheduledRetryAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Scheduled retry time must be in the future"
            );
        }

        RecoveryAttempt recoveryAttempt =
                getRecoveryAttempt(recoveryAttemptId);

        if (RecoveryStatus.from(recoveryAttempt.getStatus()) != RecoveryStatus.WAITING_FOR_CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Retry later can only be selected when the recovery attempt "
                            + "is waiting for customer action"
            );
        }

        recoveryAttempt.setCustomerAction("RETRY_LATER");
        recoveryAttempt.setScheduledRetryAt(scheduledRetryAt);
        transition(recoveryAttempt, RecoveryStatus.SCHEDULED, "Customer selected retry later");
        recoveryAttempt.setResponse(
                "Customer selected retry later. Recovery is scheduled for "
                        + scheduledRetryAt
        );

        return recoveryAttemptRepository.save(recoveryAttempt);
    }

    @Transactional
    public RecoveryAttempt chooseAnotherPaymentMethod(
            Long recoveryAttemptId
    ) {

        RecoveryAttempt recoveryAttempt =
                getRecoveryAttempt(recoveryAttemptId);

        if (RecoveryStatus.from(recoveryAttempt.getStatus()) != RecoveryStatus.WAITING_FOR_CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another payment method can only be selected when the recovery "
                            + "attempt is waiting for customer action"
            );
        }

        recoveryAttempt.setCustomerAction(
                "ANOTHER_PAYMENT_METHOD"
        );
        transition(recoveryAttempt, RecoveryStatus.WAITING_FOR_PAYMENT_METHOD,
                "Customer selected an alternative payment method");
        recoveryAttempt.setResponse(
                "Customer selected another payment method. "
                        + "Waiting for an alternative payment method or account."
        );

        return recoveryAttemptRepository.save(recoveryAttempt);
    }

    @Transactional
    public RecoveryAttempt submitAlternativePaymentMethod(
            Long recoveryAttemptId,
            AlternativePaymentMethodRequest request
    ) {

        RecoveryAttempt recoveryAttempt =
                getRecoveryAttempt(recoveryAttemptId);

        if (RecoveryStatus.from(recoveryAttempt.getStatus()) != RecoveryStatus.WAITING_FOR_PAYMENT_METHOD) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An alternative payment method can only be submitted when "
                            + "the recovery attempt is waiting for a payment method"
            );
        }

        recoveryAttempt.setAlternativePaymentMethod(
                request.getPaymentMethod()
        );
        recoveryAttempt.setAlternativePaymentMethodReference(
                request.getPaymentMethodReference()
        );
        recoveryAttempt.setRecoveryMethod(
                "ALTERNATIVE_PAYMENT_METHOD"
        );
        transition(recoveryAttempt, RecoveryStatus.PROCESSING,
                "Alternative payment method received");
        recoveryAttempt.setResponse(
                "Alternative payment method received. "
                        + "Recovery is being processed using the new payment method."
        );

        recoveryAttemptRepository.save(recoveryAttempt);

        // Simulated payment processing.
        // A real payment gateway integration would be used here.
        boolean recoverySuccessful = Math.random() < 0.8;

        if (recoverySuccessful) {
            return completeRecoveryAttempt(
                    recoveryAttempt,
                    "SUCCESS",
                    "Recovery completed successfully using the alternative payment method"
            );
        }

        /*
         * Alternative payment failed. Return to the customer-guided
         * recovery flow so the system does not retry automatically.
         */
        transition(recoveryAttempt, RecoveryStatus.WAITING_FOR_CUSTOMER,
                "Alternative payment method was unsuccessful");
        recoveryAttempt.setCustomerAction(null);
        recoveryAttempt.setScheduledRetryAt(null);
        recoveryAttempt.setCustomerNotifiedAt(
                LocalDateTime.now()
        );
        recoveryAttempt.setResponse(
                "The alternative payment method was unsuccessful. "
                        + "The customer has been notified and can choose "
                        + "another recovery option."
        );

        return recoveryAttemptRepository.save(recoveryAttempt);
    }

    public List<RecoveryAttempt> getRecoveryHistory(Long paymentId) {

        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Payment not found with ID: " + paymentId
            );
        }

        return recoveryAttemptRepository
                .findByPaymentIdOrderByAttemptedAtDesc(paymentId);
    }

    @Transactional
    public RecoveryAttempt executeRecoveryAttempt(
            Long recoveryAttemptId
    ) {

        RecoveryAttempt recoveryAttempt =
                getRecoveryAttempt(recoveryAttemptId);

        RecoveryStatus currentStatus = RecoveryStatus.from(recoveryAttempt.getStatus());
        if (currentStatus != RecoveryStatus.PENDING && currentStatus != RecoveryStatus.SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Recovery attempt can only be executed when its status is PENDING or SCHEDULED"
            );
        }

        if (currentStatus == RecoveryStatus.SCHEDULED
                && recoveryAttempt.getScheduledRetryAt() != null
                && recoveryAttempt.getScheduledRetryAt()
                .isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Scheduled recovery attempt cannot be executed before "
                            + recoveryAttempt.getScheduledRetryAt()
            );
        }

        transition(recoveryAttempt, RecoveryStatus.PROCESSING, "Recovery execution started");
        recoveryAttempt.setResponse(
                "Recovery attempt is being processed"
        );
        recoveryAttemptRepository.save(recoveryAttempt);

        // Simulated recovery behavior: 70% success probability
        boolean recoverySuccessful = Math.random() < 0.7;

        if (recoverySuccessful) {
            return completeRecoveryAttempt(
                    recoveryAttempt,
                    "SUCCESS",
                    "Recovery completed successfully"
            );
        }

        /*
         * Insufficient-funds recoveries require customer control.
         * If the scheduled retry fails, notify the customer again
         * instead of automatically creating another retry.
         */
        if ("INSUFFICIENT_FUNDS".equalsIgnoreCase(
                recoveryAttempt.getFailureReason()
        )) {

            transition(recoveryAttempt, RecoveryStatus.WAITING_FOR_CUSTOMER,
                    "Scheduled insufficient-funds retry was unsuccessful");
            recoveryAttempt.setCustomerAction(null);
            recoveryAttempt.setScheduledRetryAt(null);
            recoveryAttempt.setCustomerNotifiedAt(
                    LocalDateTime.now()
            );
            recoveryAttempt.setResponse(
                    "Scheduled recovery retry was unsuccessful. "
                            + "The customer has been notified again and can choose "
                            + "to retry later or use another payment method."
            );

            RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(recoveryAttempt);
            outreachService.queueRecoveryOutreach(savedAttempt,
                    "The scheduled recovery retry was unsuccessful and customer action is required again.");
            return savedAttempt;
        }

        long attemptCount =
                recoveryAttemptRepository.countByPaymentId(
                        recoveryAttempt.getPaymentId()
                );

        String failureMessage;

        if (attemptCount >= RecoveryPolicy.MAX_RECOVERY_ATTEMPTS) {
            failureMessage =
                    "Recovery attempt failed. Maximum of "
                            + RecoveryPolicy.MAX_RECOVERY_ATTEMPTS
                            + " attempts reached. No further retries will be created.";
        } else {
            failureMessage =
                    "Recovery attempt failed. Another retry may be scheduled.";
        }

        return completeRecoveryAttempt(
                recoveryAttempt,
                "FAILED",
                failureMessage
        );
    }

    @Transactional
    public RecoveryAttempt updateRecoveryStatus(
            Long recoveryAttemptId,
            String status,
            String response
    ) {

        RecoveryAttempt recoveryAttempt =
                getRecoveryAttempt(recoveryAttemptId);

        RecoveryStatus newStatus;
        try {
            newStatus = RecoveryStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        RecoveryStatus currentStatus = RecoveryStatus.from(recoveryAttempt.getStatus());
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }

        return completeRecoveryAttempt(
                recoveryAttempt,
                newStatus.name(),
                response
        );
    }

    private RecoveryAttempt completeRecoveryAttempt(
            RecoveryAttempt recoveryAttempt,
            String status,
            String response
    ) {

        RecoveryStatus targetStatus = RecoveryStatus.from(status);
        transition(recoveryAttempt, targetStatus, "Recovery processing completed");
        recoveryAttempt.setResponse(response);

        if (targetStatus == RecoveryStatus.SUCCESS) {

            Payment payment =
                    paymentRepository
                            .findById(
                                    recoveryAttempt.getPaymentId()
                            )
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Payment not found with id: "
                                                    + recoveryAttempt.getPaymentId()
                                    )
                            );

            payment.setStatus("RECOVERED");

            paymentRepository.save(payment);
        }

        return recoveryAttemptRepository.save(
                recoveryAttempt
        );
    }

    private RecoveryAttempt getRecoveryAttempt(
            Long recoveryAttemptId
    ) {
        return recoveryAttemptRepository
                .findById(recoveryAttemptId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Recovery attempt not found with id: "
                                        + recoveryAttemptId
                        )
                );
    }

    private List<String> activeStatusNames() {
        return List.of(
                RecoveryStatus.PENDING.name(), RecoveryStatus.PROCESSING.name(),
                RecoveryStatus.WAITING_FOR_CUSTOMER.name(), RecoveryStatus.SCHEDULED.name(),
                RecoveryStatus.WAITING_FOR_PAYMENT_METHOD.name()
        );
    }

    private String creationBlockedMessage(Payment payment, long attemptCount, boolean activeAttemptExists) {
        if (!"FAILED".equalsIgnoreCase(payment.getStatus())) {
            return "Recovery attempt can only be created for a payment with status FAILED";
        }
        if (attemptCount >= RecoveryPolicy.MAX_RECOVERY_ATTEMPTS) {
            return "Maximum of " + RecoveryPolicy.MAX_RECOVERY_ATTEMPTS
                    + " recovery attempts has been reached for this payment";
        }
        if (activeAttemptExists) {
            return "An active recovery attempt already exists for this payment";
        }
        return "Recovery policy does not allow another attempt for this payment";
    }

    private void transition(RecoveryAttempt recoveryAttempt, RecoveryStatus targetStatus, String reason) {
        RecoveryStatus sourceStatus = RecoveryStatus.from(recoveryAttempt.getStatus());
        if (!sourceStatus.canTransitionTo(targetStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid status transition from " + sourceStatus + " to " + targetStatus);
        }
        recoveryAttempt.setStatus(targetStatus.name());
        auditLogService.createAuditLog(recoveryAttempt.getPaymentId(), "RECOVERY_STATE_CHANGED",
                "Recovery attempt " + recoveryAttempt.getId() + " transitioned from " + sourceStatus
                        + " to " + targetStatus + ". " + reason);
    }


}
