package com.banking.frauddetectionservice.service;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    /**
     * Listens to every initiated transaction before it completes.
     * Runs three checks (velocity, amount, balance drain) and publishes the result.
     *
     * @param payload Kafka message from transaction.initiated topic
     */
    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-group")
    public void consumeTransactionInitiated(@Payload Map<String, Object> payload) {
        log.info("Received transaction for fraud check: {}", payload.get("transactionId"));
        try {
            fraudDetectionService.checkTransaction(payload);
        } catch (Exception e) {
            log.error("Error in fraud detection for transaction {}: {}",
                    payload.get("transactionId"), e.getMessage(), e);
        }
    }
}
