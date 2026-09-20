package com.banking.notificationservice.service;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * NotificationService — consumes events from various Kafka topics and dispatches
 * notifications to users (e.g., email, SMS, push). In this implementation,
 * notifications are logged; a real system would integrate an email/SMS provider.
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Notifies the user that an OTP has been generated for a suspicious transaction.
     * Topic: transaction.otp.generated — published by transection-service.
     *
     * @param payload contains transactionId, accountNumber, otp, amount, reason
     */
    @KafkaListener(topics = "transaction.otp.generated", groupId = "notification-service-group")
    public void onOtpGenerated(@Payload Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("accountNumber");
        String otp = (String) payload.get("otp");
        String reason = (String) payload.get("reason");
        Object amount = payload.get("amount");

        log.info("=== OTP NOTIFICATION ===");
        log.info("Account   : {}", accountNumber);
        log.info("Transaction: {}", transactionId);
        log.info("Amount    : {}", amount);
        log.info("Reason    : {}", reason);
        log.info("OTP       : {} (expires in 5 minutes)", otp);
        log.info("Message   : Your transaction of {} requires verification. Use OTP {} to confirm.", amount, otp);
    }

    /**
     * Notifies the sender and receiver that a transaction has been completed.
     * Topic: transaction.completed — published by transection-service.
     *
     * @param payload contains transactionId, "Reciever account Number", Amount
     */
    @KafkaListener(topics = "transaction.completed", groupId = "notification-service-group")
    public void onTransactionCompleted(@Payload Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String receiverAccount = (String) payload.get("Reciever account Number");
        Object amount = payload.get("Amount");

        log.info("=== TRANSACTION COMPLETED NOTIFICATION ===");
        log.info("Transaction ID  : {}", transactionId);
        log.info("Receiver Account: {}", receiverAccount);
        log.info("Amount Credited : {}", amount);
        log.info("Message: Your account {} has been credited with {}. Transaction ID: {}",
                receiverAccount, amount, transactionId);
    }

    /**
     * Notifies the account holder that their account has been blocked due to fraud.
     * Topic: fraud.detected — published by fraud-detection-service.
     *
     * @param payload contains accountNumber, transactionId, reason
     */
    @KafkaListener(topics = "fraud.detected", groupId = "notification-service-group")
    public void onFraudDetected(@Payload Map<String, Object> payload) {
        String accountNumber = (String) payload.get("accountNumber");
        String transactionId = (String) payload.get("transactionId");
        String reason = (String) payload.get("reason");

        log.warn("=== FRAUD ALERT NOTIFICATION ===");
        log.warn("Account    : {}", accountNumber);
        log.warn("Transaction: {}", transactionId);
        log.warn("Reason     : {}", reason);
        log.warn("Message: ALERT — Fraudulent activity detected on account {}. Your account has been blocked. " +
                "Transaction {} was rejected. Reason: {}", accountNumber, transactionId, reason);
    }

    /**
     * Notifies the sender that their transaction has been refunded (saga compensation).
     * Topic: transaction.refunded — published by transection-service on OTP failure.
     *
     * @param payload contains transactionId, accountNumber, amount
     */
    @KafkaListener(topics = "transaction.refunded", groupId = "notification-service-group")
    public void onTransactionRefunded(@Payload Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("accountNumber");
        Object amount = payload.get("amount");

        log.info("=== REFUND NOTIFICATION ===");
        log.info("Transaction: {}", transactionId);
        log.info("Account    : {}", accountNumber);
        log.info("Amount     : {}", amount);
        log.info("Message: Your transaction {} has been cancelled. {} has been refunded to account {}.",
                transactionId, amount, accountNumber);
    }
}
