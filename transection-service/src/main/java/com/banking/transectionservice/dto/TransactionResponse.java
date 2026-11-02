package com.banking.transectionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.banking.transectionservice.entity.enums.TransactionStatus;
import com.banking.transectionservice.entity.enums.TransactionType;

import java.math.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private String id;
    private String senderAccountNumber;
    private String recieverAccountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String failureReason;
    private String referenceNumber;
    private String createdAt;
    private String completedAt;
}
