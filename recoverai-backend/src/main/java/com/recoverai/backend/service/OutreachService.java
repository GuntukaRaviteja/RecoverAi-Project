package com.recoverai.backend.service;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.AuditLogRepository;
import com.recoverai.backend.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Provider-neutral outreach queue for the demo and audit trail. */
@Service
public class OutreachService {
    private static final List<String> CHANNELS = List.of("EMAIL", "WHATSAPP", "PHONE_CALL");
    private static final int OUTREACH_COOLDOWN_MINUTES = 30;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final PaymentRepository paymentRepository;
    private final java.util.Optional<EmailOutreachService> emailOutreachService;
    private final java.util.Optional<TwilioWhatsappOutreachService> whatsappOutreachService;
    private final String fallbackEmail;
    private final String fallbackWhatsappNumber;

    public OutreachService(
            AuditLogService auditLogService,
            AuditLogRepository auditLogRepository,
            PaymentRepository paymentRepository,
            java.util.Optional<EmailOutreachService> emailOutreachService,
            java.util.Optional<TwilioWhatsappOutreachService> whatsappOutreachService,
            @org.springframework.beans.factory.annotation.Value("${recoverai.outreach.fallback-email:}") String fallbackEmail,
            @org.springframework.beans.factory.annotation.Value("${recoverai.outreach.fallback-whatsapp-number:}") String fallbackWhatsappNumber
    ) {
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
        this.paymentRepository = paymentRepository;
        this.emailOutreachService = emailOutreachService;
        this.whatsappOutreachService = whatsappOutreachService;
        this.fallbackEmail = fallbackEmail;
        this.fallbackWhatsappNumber = fallbackWhatsappNumber;
    }

    public void queueRecoveryOutreach(RecoveryAttempt attempt, String reason) {
        for (String channel : CHANNELS) {
            auditLogService.createAuditLog(
                    attempt.getPaymentId(),
                    "OUTREACH_" + channel + "_QUEUED",
                    displayName(channel) + " recovery message queued for attempt "
                            + attempt.getId() + ". " + reason
            );

            deliver(channel, attempt, reason);
        }
    }

    /** Used for a user-triggered resend, where rate limits must be enforced. */
    public void requestCustomerOutreach(RecoveryAttempt attempt) {
        enforceCustomerProtection(attempt);
        queueRecoveryOutreach(attempt,
                "Recovery assistance is available: retry later, promise to pay, or use another payment method.");
    }

    private void enforceCustomerProtection(RecoveryAttempt attempt) {
        if ("PROMISE_TO_PAY".equals(attempt.getCustomerAction())
                && attempt.getPromiseToPayDeadline() != null
                && attempt.getPromiseToPayDeadline().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Customer outreach is paused until the promise-to-pay deadline");
        }

        auditLogRepository.findTopByPaymentIdAndActionOrderByCreatedAtDesc(
                attempt.getPaymentId(), "OUTREACH_EMAIL_QUEUED"
        ).ifPresent(lastOutreach -> {
            if (lastOutreach.getCreatedAt() != null
                    && lastOutreach.getCreatedAt().plusMinutes(OUTREACH_COOLDOWN_MINUTES)
                    .isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Customer outreach is rate-limited for 30 minutes to prevent spam");
            }
        });
    }

    private String displayName(String channel) {
        return switch (channel) {
            case "WHATSAPP" -> "WhatsApp";
            case "PHONE_CALL" -> "Phone call";
            default -> "Email";
        };
    }

    private void deliver(String channel, RecoveryAttempt attempt, String reason) {
        try {
            if ("EMAIL".equals(channel)) {
                String recipient = emailRecipient(attempt);
                if (recipient == null || recipient.isBlank() || emailOutreachService.isEmpty()) {
                    logDeliveryNotConfigured(attempt, channel);
                    return;
                }
                emailOutreachService.get().sendRecoveryMessage(recipient, attempt.getId(), reason);
            } else if ("WHATSAPP".equals(channel)) {
                String recipient = whatsappRecipient(attempt);
                if (recipient == null || recipient.isBlank() || whatsappOutreachService.isEmpty()) {
                    logDeliveryNotConfigured(attempt, channel);
                    return;
                }
                whatsappOutreachService.get().sendRecoveryMessage(recipient, attempt.getId(), reason);
            } else {
                logDeliveryNotConfigured(attempt, channel);
                return;
            }

            auditLogService.createAuditLog(attempt.getPaymentId(), "OUTREACH_" + channel + "_SENT",
                    displayName(channel) + " message sent for attempt " + attempt.getId());
        } catch (Exception exception) {
            auditLogService.createAuditLog(attempt.getPaymentId(), "OUTREACH_" + channel + "_FAILED",
                    displayName(channel) + " delivery failed for attempt " + attempt.getId()
                            + ": " + exception.getMessage());
        }
    }

    private void logDeliveryNotConfigured(RecoveryAttempt attempt, String channel) {
        auditLogService.createAuditLog(attempt.getPaymentId(), "OUTREACH_" + channel + "_NOT_CONFIGURED",
                displayName(channel) + " delivery is not configured; the outreach remains in the audit trail only.");
    }

    private String emailRecipient(RecoveryAttempt attempt) {
        return paymentRepository.findById(attempt.getPaymentId())
                .map(payment -> nonBlankOrFallback(payment.getCustomerEmail(), fallbackEmail))
                .orElse(fallbackEmail);
    }

    private String whatsappRecipient(RecoveryAttempt attempt) {
        return paymentRepository.findById(attempt.getPaymentId())
                .map(payment -> nonBlankOrFallback(payment.getCustomerWhatsappNumber(), fallbackWhatsappNumber))
                .orElse(fallbackWhatsappNumber);
    }

    private String nonBlankOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
