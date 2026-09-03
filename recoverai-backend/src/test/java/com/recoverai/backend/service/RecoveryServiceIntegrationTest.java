package com.recoverai.backend.service;

import com.recoverai.backend.dto.PromiseToPayRequest;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.recovery.RecoveryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RecoveryServiceIntegrationTest {

    @Autowired
    private RecoveryService recoveryService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void insufficientFundsUsesCustomerGuidedPromiseToPayFlow() {
        Payment payment = paymentRepository.save(failedPayment("INSUFFICIENT_FUNDS"));

        RecoveryAttempt attempt = recoveryService.createRecoveryAttempt(payment.getId());
        assertEquals(RecoveryStatus.WAITING_FOR_CUSTOMER.name(), attempt.getStatus());

        PromiseToPayRequest request = new PromiseToPayRequest();
        request.setPromiseToPayDeadline(LocalDateTime.now().plusDays(1));
        RecoveryAttempt scheduled = recoveryService.promiseToPay(attempt.getId(), request);

        assertEquals(RecoveryStatus.SCHEDULED.name(), scheduled.getStatus());
        assertEquals("PROMISE_TO_PAY", scheduled.getCustomerAction());
        assertNotNull(scheduled.getScheduledRetryAt());
    }

    @Test
    void blocksDuplicateActiveRecoveryAttempts() {
        Payment payment = paymentRepository.save(failedPayment("NETWORK_ERROR"));
        recoveryService.createRecoveryAttempt(payment.getId());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recoveryService.createRecoveryAttempt(payment.getId())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void blocksRecoveryForNonRecoverableFailuresEvenWhenCalledDirectly() {
        Payment payment = paymentRepository.save(failedPayment("SUSPECTED_FRAUD"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recoveryService.createRecoveryAttempt(payment.getId())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void customerCanSelectAnAlternativePaymentMethod() {
        Payment payment = paymentRepository.save(failedPayment("INSUFFICIENT_FUNDS"));
        RecoveryAttempt attempt = recoveryService.createRecoveryAttempt(payment.getId());

        RecoveryAttempt updated = recoveryService.chooseAnotherPaymentMethod(attempt.getId());

        assertEquals(RecoveryStatus.WAITING_FOR_PAYMENT_METHOD.name(), updated.getStatus());
        assertEquals("ANOTHER_PAYMENT_METHOD", updated.getCustomerAction());
    }

    private Payment failedPayment(String failureCategory) {
        Payment payment = new Payment();
        payment.setPaymentId("pay-test-" + System.nanoTime());
        payment.setAmount(2500.0);
        payment.setCurrency("INR");
        payment.setCustomerId("customer-test");
        payment.setStatus("FAILED");
        payment.setFailureCategory(failureCategory);
        payment.setFailureReason("Payment could not be completed");
        payment.setPaymentMethod("CARD");
        payment.setPaymentMethodReference("CARD_****1234");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
}
