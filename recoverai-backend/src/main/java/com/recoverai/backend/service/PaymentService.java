package com.recoverai.backend.service;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    @Transactional
    public void backfillFailureCategories() {

        List<Payment> payments =
                paymentRepository
                        .findByFailureCategoryIsNullAndFailureReasonIsNotNull();

        for (Payment payment : payments) {

            String failureReason =
                    payment.getFailureReason();

            if (failureReason == null
                    || failureReason.trim().isEmpty()) {
                continue;
            }

            payment.setFailureCategory(
                    determineFailureCategory(
                            failureReason
                    )
            );
        }

        if (!payments.isEmpty()) {
            paymentRepository.saveAll(payments);
        }
    }

    public Payment createPayment(Payment payment) {

        assignFailureCategory(payment);

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    private void assignFailureCategory(Payment payment) {

        String failureReason =
                payment.getFailureReason();

        if (failureReason == null
                || failureReason.trim().isEmpty()) {

            payment.setFailureCategory(null);
            return;
        }

        payment.setFailureCategory(
                determineFailureCategory(failureReason)
        );
    }

    private String determineFailureCategory(
            String failureReason
    ) {

        String normalized =
                failureReason
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replace('_', ' ')
                        .replaceAll("\\s+", " ");

        if (normalized.contains("INSUFFICIENT")
                && (normalized.contains("FUNDS")
                || normalized.contains("BALANCE"))) {
            return "INSUFFICIENT_FUNDS";
        }

        if (normalized.contains("SUSPECTED FRAUD")
                || normalized.contains("FRAUD")) {
            return "SUSPECTED_FRAUD";
        }

        if (normalized.contains("ACCOUNT")
                && normalized.contains("CLOSED")) {
            return "ACCOUNT_CLOSED";
        }

        if (normalized.contains("CARD")
                && (normalized.contains("EXPIRED")
                || normalized.contains("EXPIRY"))) {
            return "CARD_EXPIRED";
        }

        if (normalized.contains("CARD")
                && normalized.contains("DECLINED")) {
            return "CARD_DECLINED";
        }

        if (normalized.contains("PAYMENT")
                && normalized.contains("DECLINED")) {
            return "PAYMENT_DECLINED";
        }

        if (normalized.contains("LIMIT")
                && (normalized.contains("EXCEEDED")
                || normalized.contains("REACHED"))) {
            return "TRANSACTION_LIMIT_EXCEEDED";
        }

        if (normalized.contains("INVALID")
                && normalized.contains("ACCOUNT")) {
            return "INVALID_ACCOUNT";
        }

        if (normalized.contains("NETWORK")
                || normalized.contains("CONNECTION")
                || normalized.contains("TIMEOUT")) {
            return "NETWORK_ERROR";
        }

        return "OTHER";
    }
}