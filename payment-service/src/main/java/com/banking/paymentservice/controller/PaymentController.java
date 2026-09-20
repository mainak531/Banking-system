package com.banking.paymentservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banking.paymentservice.dto.PaymentRequest;
import com.banking.paymentservice.dto.PaymentResponse;
import com.banking.paymentservice.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments
     * Initiates a payment via UPI, NEFT, RTGS, or IMPS.
     *
     * @param request payment details
     * @return PaymentResponse with payment ID and status
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received {} payment request from account: {}",
                request.getPaymentMode(), request.getSourceAccountNumber());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(paymentService.initiatePayment(request));
    }

    /**
     * GET /api/v1/payments/{paymentId}/status
     * Returns the current status of a payment.
     *
     * @param paymentId the unique payment ID
     * @return payment status string
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String paymentId) {
        log.info("Fetching status for payment: {}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }
}
