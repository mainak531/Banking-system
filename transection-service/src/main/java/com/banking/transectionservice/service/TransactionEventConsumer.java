package com.banking.transectionservice.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.banking.transectionservice.entity.Transaction;
import com.banking.transectionservice.entity.enums.TransactionStatus;
import com.banking.transectionservice.repository.TransactionRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";
    private static final long OTP_EXPIRY_MINUTES = 5;

    /**
     * Saga Step 2a: Verification required
     * Triggered by fraud-detection-service when a suspicious transaction is found.
     * Generates a 6-digit OTP, stores it in Redis, and publishes an OTP event.
     *
     * @param payload Kafka message containing transactionId, senderAccountNumber, amount, reason
     */
    @KafkaListener(topics = "verification.required", groupId = "transaction-service-group")
    public void consumeVerificationRequired(@Payload Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");
            log.info("Verification required for transaction {}: reason {}", transactionId, reason);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction {} is not in PROCESSING state, skipping OTP generation", transactionId);
                return;
            }

            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);
            String otpKey = "OTP:" + accountNumber + ":" + transactionId;

            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);
            log.info("OTP generated for transaction {}, expires in {} minutes", transactionId, OTP_EXPIRY_MINUTES);

            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("reason", reason);
            otpEvent.put("otp", otp);
            otpEvent.put("amount", payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpEvent);
            log.info("OTP event published for transaction {}", transactionId);

        } catch (Exception e) {
            log.error("Error handling verification required: {}", e.getMessage(), e);
        }
    }

    /**
     * Saga Step 2b: Fraud check passed — transaction is clean.
     * Transitions the transaction directly to COMPLETED and credits the receiver.
     *
     * @param payload Kafka message containing transactionId
     */
    @KafkaListener(topics = "fraud.check.clean", groupId = "transaction-service-group")
    public void consumeFraudCheckClean(@Payload Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            log.info("Fraud check clean for transaction {}", transactionId);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction {} is not in PROCESSING state, skipping", transactionId);
                return;
            }

            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            // Publish completed event so account-service credits the receiver
            Map<String, Object> completedEvent = new HashMap<>();
            completedEvent.put("transactionId", transactionId);
            completedEvent.put("Reciever account Number", transaction.getRecieverAccountNumber());
            completedEvent.put("Amount", transaction.getAmount());

            kafkaTemplate.send("transaction.completed", transactionId, completedEvent);
            log.info("Transaction {} marked COMPLETED and completed event published", transactionId);

        } catch (Exception e) {
            log.error("Error handling fraud check clean: {}", e.getMessage(), e);
        }
    }
}
