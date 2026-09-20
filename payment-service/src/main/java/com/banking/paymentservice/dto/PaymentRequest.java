package com.banking.paymentservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating a payment through an external payment gateway.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Beneficiary account number is required")
    private String beneficiaryAccountNumber;

    @NotBlank(message = "Beneficiary IFSC code is required")
    private String ifscCode;

    @NotBlank(message = "Beneficiary name is required")
    private String beneficiaryName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /** Payment mode: UPI, NEFT, RTGS, IMPS */
    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    private String remarks;
}
