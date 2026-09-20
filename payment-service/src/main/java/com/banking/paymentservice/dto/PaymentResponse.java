package com.banking.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after initiating a payment.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private String paymentId;
    private String sourceAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private BigDecimal amount;
    private String paymentMode;
    private String status;
    private String remarks;
    private String referenceNumber;
    private LocalDateTime initiatedAt;
}
