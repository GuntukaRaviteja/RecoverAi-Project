package com.recoverai.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_attempts")
public class RecoveryAttempt {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentId;

    private String recoveryMethod;

    private String status;

    private LocalDateTime attemptedAt;

    private String response;

    // Reason why the original payment or recovery attempt failed
    private String failureReason;

    // Action selected by the customer:
// RETRY_LATER or ANOTHER_PAYMENT_METHOD
    private String customerAction;

    // Time at which RecoverAI should automatically retry the payment
    private LocalDateTime scheduledRetryAt;

    // A customer promise-to-pay pauses recovery and outreach until this deadline.
    private LocalDateTime promiseToPayDeadline;

    // Time when the customer was notified about the failed payment
    private LocalDateTime customerNotifiedAt;

    // Alternative payment method selected by the customer.
// Example: CARD, UPI, BANK_ACCOUNT
    private String alternativePaymentMethod;

    // Safe, masked reference for the alternative payment method.
// Example: CARD_****1234 or customer@bank
    private String alternativePaymentMethodReference;

    public RecoveryAttempt() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getRecoveryMethod() {
        return recoveryMethod;
    }

    public void setRecoveryMethod(String recoveryMethod) {
        this.recoveryMethod = recoveryMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getCustomerAction() {
        return customerAction;
    }

    public void setCustomerAction(String customerAction) {
        this.customerAction = customerAction;
    }

    public LocalDateTime getScheduledRetryAt() {
        return scheduledRetryAt;
    }

    public void setScheduledRetryAt(LocalDateTime scheduledRetryAt) {
        this.scheduledRetryAt = scheduledRetryAt;
    }

    public LocalDateTime getPromiseToPayDeadline() {
        return promiseToPayDeadline;
    }

    public void setPromiseToPayDeadline(LocalDateTime promiseToPayDeadline) {
        this.promiseToPayDeadline = promiseToPayDeadline;
    }

    public LocalDateTime getCustomerNotifiedAt() {
        return customerNotifiedAt;
    }

    public void setCustomerNotifiedAt(
            LocalDateTime customerNotifiedAt
    ) {
        this.customerNotifiedAt = customerNotifiedAt;
    }

    public String getAlternativePaymentMethod() {
        return alternativePaymentMethod;
    }

    public void setAlternativePaymentMethod(
            String alternativePaymentMethod
    ) {
        this.alternativePaymentMethod =
                alternativePaymentMethod;
    }

    public String getAlternativePaymentMethodReference() {
        return alternativePaymentMethodReference;
    }

    public void setAlternativePaymentMethodReference(
            String alternativePaymentMethodReference
    ) {
        this.alternativePaymentMethodReference =
                alternativePaymentMethodReference;
    }


}
