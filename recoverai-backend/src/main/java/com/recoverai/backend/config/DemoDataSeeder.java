package com.recoverai.backend.config;

import com.recoverai.backend.entity.AiDecision;
import com.recoverai.backend.entity.AuditLog;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.AuditLogRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final PaymentRepository paymentRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final AuditLogRepository auditLogRepository;

    private final boolean seedEnabled;
    private final boolean resetEnabled;

    public DemoDataSeeder(
            PaymentRepository paymentRepository,
            RecoveryAttemptRepository recoveryAttemptRepository,
            AiDecisionRepository aiDecisionRepository,
            AuditLogRepository auditLogRepository,
            @Value("${recoverai.demo.seed:false}") boolean seedEnabled,
            @Value("${recoverai.demo.reset:false}") boolean resetEnabled
    ) {
        this.paymentRepository = paymentRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.auditLogRepository = auditLogRepository;
        this.seedEnabled = seedEnabled;
        this.resetEnabled = resetEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (!seedEnabled) {
            return;
        }

        if (!resetEnabled && paymentRepository.count() > 0) {
            return;
        }

        System.out.println();
        System.out.println("==============================================");
        System.out.println("RecoverAI DEMO DATA RESET STARTED");
        System.out.println("==============================================");

        deleteExistingData();

        List<Payment> payments = createPayments();

        List<Payment> savedPayments =
                paymentRepository.saveAll(payments);

        paymentRepository.flush();

        createAiDecisions(savedPayments);
        createRecoveryOutcomes(savedPayments);
        createAuditLogs(savedPayments);

        System.out.println();
        System.out.println("==============================================");
        System.out.println("RecoverAI DEMO DATA RESET COMPLETED");
        System.out.println("==============================================");
        System.out.println("Payments created     : " + savedPayments.size());
        System.out.println("AI decisions created : " + savedPayments.size());
        System.out.println("Audit logs created   : " + (savedPayments.size() * 2));
        System.out.println("Recovery attempts     : " + countSeededRecoveryAttempts(savedPayments));
        System.out.println("==============================================");
        System.out.println();
    }

    private void deleteExistingData() {

        System.out.println("Deleting old audit logs...");
        auditLogRepository.deleteAllInBatch();
        auditLogRepository.flush();

        System.out.println("Deleting old AI decisions...");
        aiDecisionRepository.deleteAllInBatch();
        aiDecisionRepository.flush();

        System.out.println("Deleting old recovery attempts...");
        recoveryAttemptRepository.deleteAllInBatch();
        recoveryAttemptRepository.flush();

        System.out.println("Deleting old payments...");
        paymentRepository.deleteAllInBatch();
        paymentRepository.flush();

        System.out.println("Old transaction data deleted.");
    }

    private List<Payment> createPayments() {

        List<Payment> payments = new ArrayList<>();

        addCategory(
                payments,
                "INSUFFICIENT_FUNDS",
                "Insufficient funds in the customer's bank account.",
                25
        );

        addCategory(
                payments,
                "EXPIRED_CARD",
                "The payment card has expired.",
                5
        );

        addCategory(
                payments,
                "INVALID_CARD",
                "The card details are invalid or could not be verified.",
                5
        );

        addCategory(
                payments,
                "AUTHENTICATION_FAILED",
                "Card authentication or 3-D Secure verification failed.",
                20
        );

        addCategory(
                payments,
                "NETWORK_TECHNICAL_FAILURE",
                "A temporary network or payment-processing error occurred.",
                30
        );

        addCategory(
                payments,
                "SUSPECTED_FRAUD",
                "The payment was blocked by fraud or security controls.",
                3
        );

        addCategory(
                payments,
                "PAYMENT_TIMEOUT",
                "The payment processor did not respond within the allowed time.",
                20
        );

        addCategory(
                payments,
                "LIMIT_EXCEEDED",
                "The transaction exceeded the customer's available payment limit.",
                10
        );

        addCategory(
                payments,
                "BANK_ISSUER_UNAVAILABLE",
                "The customer's bank or card issuer was temporarily unavailable.",
                20
        );

        addCategory(
                payments,
                "CARD_DECLINED",
                "The card issuer declined the payment.",
                18
        );

        return payments;
    }

    private void addCategory(
            List<Payment> payments,
            String category,
            String reason,
            int count
    ) {

        for (int i = 1; i <= count; i++) {

            int sequence = payments.size() + 1;

            Payment payment = new Payment();

            payment.setPaymentId(
                    String.format("pay_demo_%04d", sequence)
            );

            payment.setAmount(
                    calculateAmount(sequence)
            );

            payment.setCurrency("INR");

            payment.setCustomerId(
                    String.format("cust_demo_%04d", sequence)
            );

            payment.setCustomerEmail(
                    String.format(
                            "customer%04d@recoverai.demo",
                            sequence
                    )
            );

            payment.setCustomerWhatsappNumber(
                    String.format(
                            "+919900%06d",
                            sequence
                    )
            );

            payment.setStatus("FAILED");

            payment.setFailureCategory(category);

            payment.setFailureReason(reason);

            payment.setPaymentMethod(
                    determinePaymentMethod(sequence)
            );

            payment.setPaymentMethodReference(
                    determinePaymentReference(
                            sequence,
                            determinePaymentMethod(sequence)
                    )
            );

            /*
             * Spread the demo payments across recent dates so that
             * dashboard and analytics views have realistic timestamps.
             */
            payment.setCreatedAt(
                    LocalDateTime.now()
                            .minusDays(sequence % 14)
                            .minusHours(sequence % 12)
                            .minusMinutes(sequence % 50)
            );

            payments.add(payment);
        }
    }

    private double calculateAmount(int sequence) {

        /*
         * Deterministic high-value INR amounts for the buildathon demo.
         * Every individual payment is strictly greater than INR 50,000.
         */
        double[] amounts = {
                65490.0,
                72850.0,
                91250.0,
                105600.0,
                58750.0,
                124900.0,
                68300.0,
                79990.0,
                113750.0,
                146500.0,
                55200.0,
                87500.0,
                102450.0,
                139900.0,
                61500.0,
                94750.0,
                121300.0,
                73400.0,
                108900.0,
                149500.0
        };

        return amounts[(sequence - 1) % amounts.length];
    }

    private String determinePaymentMethod(int sequence) {

        int selector = sequence % 3;

        if (selector == 0) {
            return "UPI";
        }

        if (selector == 1) {
            return "CARD";
        }

        return "BANK_ACCOUNT";
    }

    private String determinePaymentReference(
            int sequence,
            String paymentMethod
    ) {

        if ("UPI".equals(paymentMethod)) {
            return String.format(
                    "customer%04d@bank",
                    sequence
            );
        }

        if ("CARD".equals(paymentMethod)) {
            int lastFour = 1000 + (sequence % 9000);

            return String.format(
                    "CARD_****%04d",
                    lastFour
            );
        }

        return String.format(
                "BANK_****%04d",
                1000 + (sequence % 9000)
        );
    }

    private void createAiDecisions(
            List<Payment> payments
    ) {

        List<AiDecision> decisions = new ArrayList<>();

        for (Payment payment : payments) {

            AiDecision decision =
                    buildAiDecision(payment);

            decisions.add(decision);
        }

        aiDecisionRepository.saveAll(decisions);
        aiDecisionRepository.flush();
    }

    private AiDecision buildAiDecision(
            Payment payment
    ) {

        String category =
                payment.getFailureCategory();

        AiDecision decision =
                new AiDecision();

        decision.setPaymentId(
                payment.getId()
        );

        decision.setDecision(
                getDecision(category)
        );

        decision.setRecoveryAction(
                getRecoveryAction(category)
        );

        decision.setReason(
                getAiReason(category)
        );

        decision.setConfidenceScore(
                getConfidence(category)
        );

        /*
         * This identifies these records as deterministic demo
         * decisions while keeping the actual recovery engine untouched.
         */
        decision.setDecisionSource(
                "RECOVERAI_DEMO_AI"
        );

        decision.setCreatedAt(
                payment.getCreatedAt().plusSeconds(10)
        );

        return decision;
    }

    private String getDecision(String category) {

        return switch (category) {

            case "INSUFFICIENT_FUNDS" ->
                    "CUSTOMER_ACTION_REQUIRED";

            case "EXPIRED_CARD" ->
                    "UPDATE_PAYMENT_METHOD";

            case "INVALID_CARD" ->
                    "ALTERNATIVE_PAYMENT_METHOD";

            case "AUTHENTICATION_FAILED" ->
                    "CUSTOMER_AUTHENTICATION_REQUIRED";

            case "NETWORK_TECHNICAL_FAILURE" ->
                    "SCHEDULE_RETRY";

            case "SUSPECTED_FRAUD" ->
                    "STOP";

            case "PAYMENT_TIMEOUT" ->
                    "SCHEDULE_RETRY";

            case "LIMIT_EXCEEDED" ->
                    "ALTERNATIVE_PAYMENT_METHOD";

            case "BANK_ISSUER_UNAVAILABLE" ->
                    "SCHEDULE_RETRY";

            case "CARD_DECLINED" ->
                    "ALTERNATIVE_PAYMENT_METHOD";

            default ->
                    "MANUAL_REVIEW";
        };
    }

    private String getRecoveryAction(String category) {

        return switch (category) {

            case "INSUFFICIENT_FUNDS" ->
                    "NOTIFY_CUSTOMER";

            case "EXPIRED_CARD" ->
                    "REQUEST_UPDATED_CARD";

            case "INVALID_CARD" ->
                    "REQUEST_ALTERNATIVE_PAYMENT_METHOD";

            case "AUTHENTICATION_FAILED" ->
                    "REQUEST_CUSTOMER_AUTHENTICATION";

            case "NETWORK_TECHNICAL_FAILURE" ->
                    "RETRY_LATER";

            case "SUSPECTED_FRAUD" ->
                    "STOP_RECOVERY";

            case "PAYMENT_TIMEOUT" ->
                    "RETRY_LATER";

            case "LIMIT_EXCEEDED" ->
                    "USE_ALTERNATIVE_PAYMENT_METHOD";

            case "BANK_ISSUER_UNAVAILABLE" ->
                    "RETRY_LATER";

            case "CARD_DECLINED" ->
                    "USE_ALTERNATIVE_PAYMENT_METHOD";

            default ->
                    "MANUAL_REVIEW";
        };
    }

    private String getAiReason(String category) {

        return switch (category) {

            case "INSUFFICIENT_FUNDS" ->
                    "The failure indicates insufficient available funds. "
                            + "Immediate repeated retries may frustrate the customer, "
                            + "so RecoverAI should request customer action.";

            case "EXPIRED_CARD" ->
                    "The payment method has expired. "
                            + "Retrying the same card is unlikely to succeed, "
                            + "so RecoverAI should request an updated payment method.";

            case "INVALID_CARD" ->
                    "The payment method could not be validated. "
                            + "RecoverAI should avoid repeated retries and request "
                            + "a valid alternative payment method.";

            case "AUTHENTICATION_FAILED" ->
                    "Authentication was unsuccessful. "
                            + "The customer should complete authentication before "
                            + "another payment attempt.";

            case "NETWORK_TECHNICAL_FAILURE" ->
                    "The failure appears temporary and technical. "
                            + "A controlled retry is more appropriate than immediate "
                            + "customer escalation.";

            case "SUSPECTED_FRAUD" ->
                    "Security controls identified a potentially fraudulent payment. "
                            + "Automatic recovery is blocked and the case should be "
                            + "stopped or escalated.";

            case "PAYMENT_TIMEOUT" ->
                    "The payment processor timed out. "
                            + "Because the failure may be transient, RecoverAI can "
                            + "schedule a controlled retry.";

            case "LIMIT_EXCEEDED" ->
                    "The transaction exceeded an applicable payment limit. "
                            + "A different payment method is more appropriate than "
                            + "repeating the same request.";

            case "BANK_ISSUER_UNAVAILABLE" ->
                    "The bank or card issuer is temporarily unavailable. "
                            + "RecoverAI should wait and use a controlled retry rather "
                            + "than repeatedly contacting the issuer.";

            case "CARD_DECLINED" ->
                    "The card issuer declined the transaction. "
                            + "RecoverAI should avoid uncontrolled retries and offer "
                            + "an alternative payment method.";

            default ->
                    "The failure requires manual review before recovery.";
        };
    }

    private double getConfidence(String category) {

        return switch (category) {

            case "SUSPECTED_FRAUD" ->
                    0.98;

            case "EXPIRED_CARD",
                 "INVALID_CARD" ->
                    0.97;

            case "INSUFFICIENT_FUNDS" ->
                    0.95;

            case "AUTHENTICATION_FAILED" ->
                    0.94;

            case "LIMIT_EXCEEDED" ->
                    0.93;

            case "CARD_DECLINED" ->
                    0.91;

            case "PAYMENT_TIMEOUT",
                 "BANK_ISSUER_UNAVAILABLE" ->
                    0.90;

            case "NETWORK_TECHNICAL_FAILURE" ->
                    0.89;

            default ->
                    0.85;
        };
    }

    private void createRecoveryOutcomes(List<Payment> payments) {

        List<RecoveryAttempt> attempts = new ArrayList<>();

        for (Payment payment : payments) {

            int sequence = Integer.parseInt(
                    payment.getPaymentId().substring(
                            payment.getPaymentId().lastIndexOf('_') + 1
                    )
            );

            RecoveryAttempt attempt = buildRecoveryOutcome(payment, sequence);

            if (attempt != null) {
                attempts.add(attempt);
            }
        }

        if (!attempts.isEmpty()) {
            recoveryAttemptRepository.saveAll(attempts);
            recoveryAttemptRepository.flush();
        }
    }

    private RecoveryAttempt buildRecoveryOutcome(
            Payment payment,
            int sequence
    ) {

        String category = payment.getFailureCategory();

        // Customer-first cases: no automatic retry is created.
        if ("INSUFFICIENT_FUNDS".equals(category)) {
            return buildAttempt(
                    payment,
                    "RETRY",
                    "WAITING_FOR_CUSTOMER",
                    "Payment recovery requires customer action. Customer notified about insufficient funds.",
                    "INSUFFICIENT_FUNDS",
                    payment.getCreatedAt().plusSeconds(30),
                    true
            );
        }

        // Fraud/security cases are stopped and escalated.
        if ("SUSPECTED_FRAUD".equals(category)) {
            return null;
        }

        // Deterministic demo distribution for the remaining 128 payments:
        // 80 recovered, 32 failed, 16 scheduled.
        boolean recovered = isRecoveredDemoCase(sequence);
        boolean scheduled = isScheduledDemoCase(sequence);

        if (scheduled) {
            return buildAttempt(
                    payment,
                    "RETRY",
                    "SCHEDULED",
                    "Controlled recovery retry scheduled by RecoverAI policy.",
                    null,
                    payment.getCreatedAt().plusDays(1),
                    false
            );
        }

        if (recovered) {
            payment.setStatus("RECOVERED");

            return buildAttempt(
                    payment,
                    "RETRY",
                    "SUCCESS",
                    "Recovery completed successfully. Revenue recovered by RecoverAI.",
                    null,
                    payment.getCreatedAt().plusSeconds(35),
                    false
            );
        }

        return buildAttempt(
                payment,
                "RETRY",
                "FAILED",
                "Recovery attempt failed. RecoverAI retained the case for controlled follow-up.",
                category,
                payment.getCreatedAt().plusSeconds(35),
                false
        );
    }

    private boolean isRecoveredDemoCase(int sequence) {

        return (sequence >= 36 && sequence <= 45)
                || (sequence >= 56 && sequence <= 80)
                || (sequence >= 89 && sequence <= 103)
                || (sequence >= 109 && sequence <= 112)
                || (sequence >= 119 && sequence <= 132)
                || (sequence >= 139 && sequence <= 150);
    }

    private boolean isScheduledDemoCase(int sequence) {

        return (sequence >= 26 && sequence <= 27)
                || (sequence >= 48 && sequence <= 49)
                || (sequence >= 83 && sequence <= 84)
                || (sequence >= 104 && sequence <= 106)
                || (sequence >= 113 && sequence <= 114)
                || (sequence >= 133 && sequence <= 134)
                || (sequence >= 151 && sequence <= 153);
    }

    private RecoveryAttempt buildAttempt(
            Payment payment,
            String recoveryMethod,
            String status,
            String response,
            String failureReason,
            LocalDateTime attemptedAt,
            boolean customerNotified
    ) {

        RecoveryAttempt attempt = new RecoveryAttempt();
        attempt.setPaymentId(payment.getId());
        attempt.setRecoveryMethod(recoveryMethod);
        attempt.setStatus(status);
        attempt.setAttemptedAt(attemptedAt);
        attempt.setResponse(response);
        attempt.setFailureReason(failureReason);

        if (customerNotified) {
            attempt.setCustomerNotifiedAt(attemptedAt);
        }

        if ("SCHEDULED".equals(status)) {
            attempt.setScheduledRetryAt(payment.getCreatedAt().plusDays(1));
        }

        return attempt;
    }

    private long countSeededRecoveryAttempts(List<Payment> payments) {

        return payments.stream()
                .filter(payment -> !"SUSPECTED_FRAUD".equals(payment.getFailureCategory()))
                .count();
    }

    private void createAuditLogs(
            List<Payment> payments
    ) {

        List<AuditLog> logs = new ArrayList<>();

        for (Payment payment : payments) {

            AuditLog log = new AuditLog();

            log.setPaymentId(
                    payment.getId()
            );

            log.setAction(
                    "AI_DECISION_CREATED"
            );

            log.setDetails(
                    String.format(
                            "RecoverAI analyzed failed payment %s. "
                                    + "Failure category: %s. "
                                    + "Initial AI recovery decision was generated "
                                    + "and deterministic recovery policy controls "
                                    + "are ready to govern the next action.",
                            payment.getPaymentId(),
                            payment.getFailureCategory()
                    )
            );

            log.setCreatedAt(
                    payment.getCreatedAt().plusSeconds(20)
            );

            logs.add(log);

            String outcome = getDemoOutcome(payment);
            AuditLog outcomeLog = new AuditLog();
            outcomeLog.setPaymentId(payment.getId());
            outcomeLog.setAction("RECOVERY_OUTCOME_SEEDED");
            outcomeLog.setDetails(
                    "RecoverAI demo recovery outcome: " + outcome
                            + ". The outcome is governed by deterministic demo policy data "
                            + "for repeatable buildathon presentation and testing."
            );
            outcomeLog.setCreatedAt(payment.getCreatedAt().plusSeconds(40));
            logs.add(outcomeLog);
        }

        auditLogRepository.saveAll(logs);
        auditLogRepository.flush();
    }
    private String getDemoOutcome(Payment payment) {

        String category = payment.getFailureCategory();

        if ("INSUFFICIENT_FUNDS".equals(category)) {
            return "CUSTOMER_ACTION_REQUIRED";
        }

        if ("SUSPECTED_FRAUD".equals(category)) {
            return "STOPPED_AND_ESCALATED";
        }

        if ("RECOVERED".equals(payment.getStatus())) {
            return "RECOVERED";
        }

        int sequence = Integer.parseInt(
                payment.getPaymentId().substring(
                        payment.getPaymentId().lastIndexOf('_') + 1
                )
        );

        if (isScheduledDemoCase(sequence)) {
            return "SCHEDULED_RETRY";
        }

        return "RECOVERY_FAILED";
    }


}
