package com.banking.transectionservice.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.banking.transectionservice.client.AccountServiceClient;
import com.banking.transectionservice.dto.TransactionResponse;
import com.banking.transectionservice.dto.TransferRequest;
import com.banking.transectionservice.entity.Transaction;
import com.banking.transectionservice.entity.enums.TransactionStatus;
import com.banking.transectionservice.entity.enums.TransactionType;
import com.banking.transectionservice.mapper.TransactionMapper;
import com.banking.transectionservice.repository.TransactionRepository;
import com.banking.transectionservice.event.TransactionIntiatedEvent;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionMapper transactionMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";

    /**
     * Saga Step 1: Initiate Transfer
     *
     * 1. Deducts the requested amount from the sender's account via Feign client.
     * 2. Saves the transaction record with PROCESSING status.
     * 3. Publishes a "transaction.initiated" Kafka event to trigger fraud check.
     *
     * @param request The transfer details (sender, receiver, amount, description)
     * @return TransactionResponse with the newly created transaction details
     */
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Processing transfer of {} from {} to {}",
                request.getAmount(),
                request.getSenderAccountNumber(),
                request.getRecieverAccountNumber());

        // Step 1: Deduct from sender (will throw if account blocked or insufficient funds)
        accountServiceClient.deductBalance(request.getSenderAccountNumber(), request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setRecieverAccountNumber(request.getRecieverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setReferenceNumber(UUID.randomUUID().toString());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING with ID: {}", savedTransaction.getId());

        // Step 3: Publish initiated event to trigger fraud check
        TransactionIntiatedEvent event = new TransactionIntiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getRecieverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription());

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
        log.info("Transaction initiated event sent to Kafka with ID: {}", savedTransaction.getId());

        return transactionMapper.mapToResponse(savedTransaction);
    }

    public TransactionResponse getTransaction(String transactionId) {
        log.info("Fetching transaction with ID: {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));
        return transactionMapper.mapToResponse(transaction);
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        log.info("Fetching transaction history for account: {}", accountNumber);
        List<Transaction> transactions = transactionRepository
                .findBySenderAccountNumberOrRecieverAccountNumber(accountNumber, accountNumber);
        List<TransactionResponse> responseList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            responseList.add(transactionMapper.mapToResponse(transaction));
        }
        return responseList;
    }

    /**
     * Saga Step 4 (OTP path): Verify OTP submitted by user.
     *
     * On success: mark COMPLETED and publish transaction.completed to credit receiver.
     * On failure: mark FAILED, refund sender via creditBalance (compensating transaction).
     *
     * @param transactionId the ID of the transaction to verify
     * @param otp           the OTP submitted by the user
     * @return TransactionResponse with the updated transaction status
     */
    public TransactionResponse verifyOTP(String transactionId, String otp) {
        log.info("Verifying OTP for transaction ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING_VERIFICATION) {
            throw new RuntimeException("Transaction " + transactionId + " is not awaiting OTP verification");
        }

        if ("123456".equals(otp)) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
            log.info("OTP verified for transaction {}", transactionId);

            // Credit the receiver
            Map<String, Object> completedEvent = new HashMap<>();
            completedEvent.put("transactionId", transactionId);
            completedEvent.put("Reciever account Number", transaction.getRecieverAccountNumber());
            completedEvent.put("Amount", transaction.getAmount());

            kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transactionId, completedEvent);
            log.info("Transaction completed event published for {}", transactionId);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Invalid OTP");
            transactionRepository.save(transaction);

            // Saga compensation: refund the sender
            log.info("Invalid OTP — refunding sender for transaction {}", transactionId);
            accountServiceClient.creditBalance(transaction.getSenderAccountNumber(), transaction.getAmount());

            throw new RuntimeException("Invalid OTP for transaction ID: " + transactionId);
        }

        return transactionMapper.mapToResponse(transaction);
    }
}
