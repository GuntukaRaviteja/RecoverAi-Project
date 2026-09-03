package com.recoverai.backend.scheduler;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.AuditLogRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.recovery.RecoveryPolicy;
import com.recoverai.backend.recovery.RecoveryStatus;
import com.recoverai.backend.service.AuditLogService;
import com.recoverai.backend.service.RecoveryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RecoveryScheduler {


    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final RecoveryService recoveryService;
    private final AuditLogService auditLogService;
    private final RecoveryPolicy recoveryPolicy;

    public RecoveryScheduler(
            RecoveryAttemptRepository recoveryAttemptRepository,
            PaymentRepository paymentRepository,
            AuditLogRepository auditLogRepository,
            RecoveryService recoveryService,
            AuditLogService auditLogService,
            RecoveryPolicy recoveryPolicy
    ) {
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
        this.recoveryService = recoveryService;
        this.auditLogService = auditLogService;
        this.recoveryPolicy = recoveryPolicy;
    }

    // Runs every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void processRecoveryAttempts() {

        executePendingRecoveryAttempts();

        executeScheduledRecoveryAttempts();

        createRetryAttemptsForFailedPayments();
    }

    private void executePendingRecoveryAttempts() {

        List<RecoveryAttempt> pendingAttempts =
                recoveryAttemptRepository
                        .findByStatusOrderByAttemptedAtAsc(RecoveryStatus.PENDING.name());

        for (RecoveryAttempt attempt : pendingAttempts) {
            executeAttemptAutomatically(
                    attempt,
                    "AUTOMATIC_RECOVERY_STARTED"
            );
        }
    }

    private void executeScheduledRecoveryAttempts() {

        List<RecoveryAttempt> scheduledAttempts =
                recoveryAttemptRepository
                        .findByStatusAndScheduledRetryAtLessThanEqualOrderByScheduledRetryAtAsc(
                                RecoveryStatus.SCHEDULED.name(),
                                LocalDateTime.now()
                        );

        for (RecoveryAttempt attempt : scheduledAttempts) {

            executeAttemptAutomatically(
                    attempt,
                    "SCHEDULED_RECOVERY_STARTED"
            );
        }
    }

    private void executeAttemptAutomatically(
            RecoveryAttempt attempt,
            String startAuditAction
    ) {

        try {

            auditLogService.createAuditLog(
                    attempt.getPaymentId(),
                    startAuditAction,
                    "Automatic scheduler started recovery attempt "
                            + attempt.getId()
            );

            RecoveryAttempt completedAttempt =
                    recoveryService.executeRecoveryAttempt(
                            attempt.getId()
                    );

            if ("SUCCESS".equalsIgnoreCase(
                    completedAttempt.getStatus()
            )) {

                auditLogService.createAuditLog(
                        completedAttempt.getPaymentId(),
                        "AUTOMATIC_RECOVERY_SUCCESS",
                        "Recovery attempt "
                                + completedAttempt.getId()
                                + " completed successfully. "
                                + "Payment was marked as RECOVERED."
                );

            } else if ("FAILED".equalsIgnoreCase(
                    completedAttempt.getStatus()
            )) {

                auditLogService.createAuditLog(
                        completedAttempt.getPaymentId(),
                        "AUTOMATIC_RECOVERY_FAILED",
                        completedAttempt.getResponse()
                );
            }

        } catch (Exception exception) {

            auditLogService.createAuditLog(
                    attempt.getPaymentId(),
                    "AUTOMATIC_RECOVERY_ERROR",
                    "Recovery attempt "
                            + attempt.getId()
                            + " encountered an error: "
                            + exception.getMessage()
            );

            System.err.println(
                    "Failed to automatically execute recovery attempt "
                            + attempt.getId()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void createRetryAttemptsForFailedPayments() {

        List<Payment> failedPayments =
                paymentRepository.findByStatus("FAILED");

        for (Payment payment : failedPayments) {

            if (!recoveryPolicy.allowsAutomaticRecovery(payment)) {
                continue;
            }

            long attemptCount =
                    recoveryAttemptRepository.countByPaymentId(
                            payment.getId()
                    );

            // The first attempt is created by the AI decision.
            // Scheduler only creates subsequent retries.
            if (attemptCount == 0) {
                continue;
            }

            // Stop after 5 total attempts
            if (attemptCount >= RecoveryPolicy.MAX_RECOVERY_ATTEMPTS) {

                boolean maximumAttemptsAlreadyLogged =
                        auditLogRepository
                                .existsByPaymentIdAndAction(
                                        payment.getId(),
                                        "MAX_RECOVERY_ATTEMPTS_REACHED"
                                );

                if (!maximumAttemptsAlreadyLogged) {

                    auditLogService.createAuditLog(
                            payment.getId(),
                            "MAX_RECOVERY_ATTEMPTS_REACHED",
                            "Automatic scheduler detected that the maximum of "
                                    + RecoveryPolicy.MAX_RECOVERY_ATTEMPTS
                                    + " recovery attempts has been reached. "
                                    + "No further recovery retries will be created."
                    );
                }

                continue;
            }

            boolean activeAttemptExists =
                    recoveryAttemptRepository
                            .existsByPaymentIdAndStatusIn(
                                    payment.getId(),
                                    List.of(
                                            RecoveryStatus.PENDING.name(),
                                            RecoveryStatus.PROCESSING.name(),
                                            RecoveryStatus.WAITING_FOR_CUSTOMER.name(),
                                            RecoveryStatus.SCHEDULED.name(),
                                            RecoveryStatus.WAITING_FOR_PAYMENT_METHOD.name()
                                    )
                            );

            // Don't create another attempt while one is active
            if (activeAttemptExists) {
                continue;
            }

            try {

                RecoveryAttempt newAttempt =
                        recoveryService.createRecoveryAttempt(
                                payment.getId()
                        );

                auditLogService.createAuditLog(
                        payment.getId(),
                        "AUTOMATIC_RECOVERY_RETRY_CREATED",
                        "Automatic scheduler created recovery attempt "
                                + newAttempt.getId()
                                + ". Attempt "
                                + (attemptCount + 1)
                                + " of "
                                + RecoveryPolicy.MAX_RECOVERY_ATTEMPTS
                );

            } catch (Exception exception) {

                auditLogService.createAuditLog(
                        payment.getId(),
                        "AUTOMATIC_RECOVERY_RETRY_ERROR",
                        "Failed to automatically create a recovery retry: "
                                + exception.getMessage()
                );

                System.err.println(
                        "Failed to create automatic retry for payment "
                                + payment.getId()
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }

}
