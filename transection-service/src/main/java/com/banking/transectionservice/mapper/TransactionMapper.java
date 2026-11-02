package com.banking.transectionservice.mapper;

import org.springframework.stereotype.Component;

import com.banking.transectionservice.dto.TransactionResponse;
import com.banking.transectionservice.entity.Transaction;

@Component
public class TransactionMapper {

    public TransactionResponse mapToResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(transaction.getSenderAccountNumber());
        response.setRecieverAccountNumber(transaction.getRecieverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setFailureReason(transaction.getFailureReason());
        response.setReferenceNumber(transaction.getReferenceNumber());
        
        if (transaction.getCreatedAt() != null) {
            response.setCreatedAt(transaction.getCreatedAt().toString());
        }
        if (transaction.getCompletedAt() != null) {
            response.setCompletedAt(transaction.getCompletedAt().toString());
        }
        
        return response;
    }
}
