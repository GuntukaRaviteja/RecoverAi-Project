package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.repository.AiDecisionRepository;
import com.recoverai.backend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final AiDecisionService aiDecisionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookService(
            PaymentRepository paymentRepository,
            AiDecisionRepository aiDecisionRepository,
            AuditLogService auditLogService,
            AiDecisionService aiDecisionService,
            @Value("${recoverai.razorpay.webhook.secret:}") String webhookSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.auditLogService = auditLogService;
        this.aiDecisionService = aiDecisionService;
        this.objectMapper = new ObjectMapper();
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void process(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay webhook secret is not configured"
            );
        }

        if (!isValidSignature(payload, signature)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Razorpay webhook signature"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            JsonNode entity = root.path("payload").path("payment").path("entity");
            String razorpayPaymentId = entity.path("id").asText(null);

            if (razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Webhook does not contain a payment entity"
                );
            }

            if ("payment.failed".equals(event)) {
                upsertFailedPayment(entity, razorpayPaymentId);
            } else if ("payment.captured".equals(event)
                    || "payment.authorized".equals(event)) {
                markPaymentSuccessful(razorpayPaymentId);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to parse Razorpay webhook payload",
                    exception
            );
        }
    }

    private void upsertFailedPayment(
            JsonNode entity,
            String razorpayPaymentId
    ) {
        Payment payment = paymentRepository.findByPaymentId(razorpayPaymentId)
                .orElseGet(Payment::new);

        payment.setPaymentId(razorpayPaymentId);
        payment.setAmount(entity.path("amount").asDouble(0) / 100.0);
        payment.setCurrency(entity.path("currency").asText("INR"));
        payment.setCustomerEmail(entity.path("email").asText(null));
        payment.setCustomerWhatsappNumber(entity.path("contact").asText(null));
        payment.setPaymentMethod(entity.path("method").asText(null));
        payment.setPaymentMethodReference(maskReference(entity));
        payment.setFailureReason(entity.path("error_description")
                .asText("Razorpay payment failed"));
        payment.setStatus("FAILED");
        payment.setCreatedAt(payment.getCreatedAt() == null
                ? LocalDateTime.now()
                : payment.getCreatedAt());

        Payment saved = paymentRepository.save(payment);
        auditLogService.createAuditLog(
                saved.getId(),
                "RAZORPAY_PAYMENT_FAILED_RECEIVED",
                "Verified Razorpay payment.failed webhook for "
                        + razorpayPaymentId
        );

        if (!aiDecisionRepository.existsByPaymentId(saved.getId())) {
            aiDecisionService.analyzePayment(saved.getId());
        }
    }

    private void markPaymentSuccessful(String razorpayPaymentId) {
        paymentRepository.findByPaymentId(razorpayPaymentId)
                .ifPresent(payment -> {
                    payment.setStatus("RECOVERED");
                    paymentRepository.save(payment);
                    auditLogService.createAuditLog(
                            payment.getId(),
                            "RAZORPAY_PAYMENT_RECOVERED",
                            "Verified Razorpay payment success webhook for "
                                    + razorpayPaymentId
                    );
                });
    }

    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            String expected = HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to verify Razorpay webhook signature",
                    exception
            );
        }
    }

    private String maskReference(JsonNode entity) {
        String id = entity.path("card").path("last4").asText(null);
        return id == null ? null : "CARD_****" + id;
    }
}
