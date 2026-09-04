package com.recoverai.backend.controller;

import com.recoverai.backend.service.RazorpayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature,
            @RequestBody String payload
    ) {
        webhookService.process(payload, signature);
        return ResponseEntity.ok().build();
    }
}
