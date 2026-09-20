package com.banking.paymentservice.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.banking.paymentservice.dto.PaymentRequest;
import com.banking.paymentservice.dto.PaymentResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * PaymentService handles external payment gateway integrations.
 *
 * Supported modes:
 * - UPI  : Unified Payments Interface (instant, 24x7)
 * - IMPS : Immediate Payment Service (instant, 24x7)
 * - NEFT : National Electronic Funds Transfer (batched, business hours)
 * - RTGS : Real Time Gross Settlement (high-value, real-time)
 *
 * This implementation is a stub that simulates gateway responses.
 * In production, integrate with a payment aggregator (e.g. Razorpay, PayU).
 */
@Service
@Slf4j
public class PaymentService {

    /**
     * Initiates a payment through the appropriate payment mode.
     *
     * @param request payment details from the client
     * @return PaymentResponse with the payment ID, status, and reference number
     */
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating {} payment of {} from {} to {}",
                request.getPaymentMode(),
                request.getAmount(),
                request.getSourceAccountNumber(),
                request.getBeneficiaryAccountNumber());

        String paymentId = UUID.randomUUID().toString();
        String referenceNumber = generateReferenceNumber(request.getPaymentMode());

        // In production: call the actual payment gateway API here
        String status = simulateGatewayCall(request.getPaymentMode());

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(paymentId)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
                .beneficiaryName(request.getBeneficiaryName())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .status(status)
                .remarks(request.getRemarks())
                .referenceNumber(referenceNumber)
                .initiatedAt(LocalDateTime.now())
                .build();

        log.info("Payment {} initiated with status: {} reference: {}", paymentId, status, referenceNumber);
        return response;
    }

    /**
     * Returns the status of a payment by ID.
     *
     * @param paymentId the unique payment identifier
     * @return payment status string
     */
    public String getPaymentStatus(String paymentId) {
        log.info("Fetching status for payment: {}", paymentId);
        // In production: query the payment gateway or a database
        return "PROCESSING";
    }

    /**
     * Simulates a payment gateway call.
     * Replace with real gateway SDK integration in production.
     *
     * @param mode payment mode (UPI, NEFT, RTGS, IMPS)
     * @return simulated status
     */
    private String simulateGatewayCall(String mode) {
        return switch (mode.toUpperCase()) {
            case "UPI", "IMPS" -> "SUCCESS";
            case "NEFT" -> "QUEUED";
            case "RTGS" -> "PROCESSING";
            default -> "PENDING";
        };
    }

    private String generateReferenceNumber(String mode) {
        String prefix = switch (mode.toUpperCase()) {
            case "UPI" -> "UPI";
            case "NEFT" -> "NEFT";
            case "RTGS" -> "RTGS";
            case "IMPS" -> "IMPS";
            default -> "PAY";
        };
        return prefix + System.currentTimeMillis();
    }
}
