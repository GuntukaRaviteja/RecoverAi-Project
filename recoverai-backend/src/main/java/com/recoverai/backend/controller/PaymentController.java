package com.recoverai.backend.controller;

import com.recoverai.backend.dto.PaymentDetailsResponse;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.service.PaymentDetailsService;
import com.recoverai.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentDetailsService paymentDetailsService;

    public PaymentController(
            PaymentService paymentService,
            PaymentDetailsService paymentDetailsService
    ) {
        this.paymentService = paymentService;
        this.paymentDetailsService = paymentDetailsService;
    }

    // Create a new payment
    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {
        return paymentService.createPayment(payment);
    }

    // Get all payments
    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    // Get complete details for a payment
    @GetMapping("/{id}/details")
    public PaymentDetailsResponse getPaymentDetails(
            @PathVariable Long id
    ) {
        return paymentDetailsService.getPaymentDetails(id);
    }

    // Get payment by ID
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(
            @PathVariable Long id
    ) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}