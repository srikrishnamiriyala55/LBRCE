package com.web.sms.controller;

import com.web.sms.service.RazorpayPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/payments/razorpay")
public class RazorpayWebhookController {
    private final RazorpayPaymentService razorpayPaymentService;

    public RazorpayWebhookController(RazorpayPaymentService razorpayPaymentService) {
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        razorpayPaymentService.processWebhook(rawBody, signature);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
