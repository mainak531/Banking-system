package com.banking.accountservice.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import java.util.Map;
import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /*
     * consume transection completed event from kafka
     * Credits reciever account
     * 
     * @param payload
     */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransectionCompleted(
            @Payload Map<String, Object> payload) {
        log.info("Received transaction completed event: {}", payload);
        try {
            String recieverAccount = (String) payload.get("Reciever account Number");
            BigDecimal amount = new BigDecimal(payload.get("Amount").toString());

            log.info("Crediting account: {} amount :{}", recieverAccount, amount);
            accountService.creditBalance(recieverAccount, amount);
        } catch (Exception e) {
            log.error("Error crediting account: {}", e.getMessage());
        }
    }

    /*
     * Consume fraud detected event from kafka
     * it block the account
     * 
     * @param payload
     */
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("Account Number");
            log.info("Fraud detected for account no: {} ", accountNumber);
            accountService.blockAccount(accountNumber);
        } catch (Exception e) {
            log.error("Error blocking account: {}", e.getMessage());
        }
    }
}