package com.banking.frauddetectionservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.model.FraudCheckResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_TOPIC = "fraud.check.clean";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";

    /** Maximum transactions allowed per 60-second window per account */
    @Value("${fraud.max-transaction-per-minute:5}")
    private int maxTransactionPerMinute;

    /** Multiplier over the rolling average to flag an amount as suspicious */
    @Value("${fraud.suspicious-amount-multiplier:5}")
    private double suspiciousAmountMultiplier;

    /** Maximum fraction of account balance that can be transferred in one go (e.g. 0.90) */
    @Value("${fraud.max-balance-percentage:0.90}")
    private double maxBalancePercentage;

    /**
     * Main entry point called by FraudDetectionEventConsumer.
     * Runs three checks: velocity, amount, and balance drain.
     * Publishes either a "verification.required" or "fraud.check.clean" Kafka event.
     *
     * @param payload Kafka message map from "transaction.initiated" topic
     */
    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        Object amountObj = payload.get("amount");

        if (transactionId == null || accountNumber == null || amountObj == null) {
            log.error("Invalid fraud check payload — missing required fields: {}", payload);
            return;
        }

        BigDecimal transactionAmount;
        try {
            transactionAmount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid amount in fraud check payload: {}", amountObj);
            return;
        }

        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);
        if (senderBalance == null) {
            log.error("Failed to fetch sender balance for account {} — skipping fraud check", accountNumber);
            return;
        }

        log.info("Fraud check: transactionId={} account={} amount={} balance={}",
                transactionId, accountNumber, transactionAmount, senderBalance);

        FraudCheckResult result = performFraudChecks(accountNumber, transactionAmount, senderBalance);

        if (result.isFraud()) {
            log.warn("Suspicious activity detected for account {}: {} — requesting OTP verification",
                    accountNumber, result.getReason());

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", transactionAmount);
            verificationEvent.put("reason", result.getReason());
            verificationEvent.put("senderAccountNumber", accountNumber);

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, verificationEvent);
            log.info("Verification required event published for transaction {}", transactionId);
        } else {
            log.info("Transaction {} passed all fraud checks", transactionId);

            Map<String, Object> cleanEvent = new HashMap<>();
            cleanEvent.put("transactionId", transactionId);
            cleanEvent.put("isFraud", false);
            cleanEvent.put("reason", null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_TOPIC, transactionId, cleanEvent);
            log.info("Fraud check clean event published for transaction {}", transactionId);
        }
    }

    private FraudCheckResult performFraudChecks(String accountNumber, BigDecimal amount, BigDecimal senderBalance) {
        if (isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(true, "Velocity limit exceeded: too many transactions in 60 seconds");
        }
        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(true, "Suspicious amount: transaction significantly exceeds average");
        }
        if (isBalanceDrainExceeded(senderBalance, amount)) {
            return new FraudCheckResult(true,
                    "Transaction exceeds " + (int) (maxBalancePercentage * 100) + "% of account balance");
        }
        return new FraudCheckResult(false, null);
    }

    /**
     * Velocity check: counts transactions per account in the last 60 seconds using Redis.
     * Uses an atomic increment with expiry so the counter resets automatically.
     *
     * @param accountNumber the account to check
     * @return true if the velocity limit is exceeded
     */
    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            // First transaction in this window — set a 60-second TTL
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        log.info("Velocity check — account: {} count: {}/{}", accountNumber, count, maxTransactionPerMinute);
        return count != null && count > maxTransactionPerMinute;
    }

    /**
     * Amount check: flags transactions that are significantly higher than the
     * account's rolling average transaction amount (stored in Redis).
     *
     * Uses an exponential moving average (EMA) to reduce the impact of outliers.
     *
     * @param accountNumber the account to check
     * @param amount        the current transaction amount
     * @return true if the amount is suspicious
     */
    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {
        String avgKey = "fraud:avg_amount:" + accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);

        if (avgStr == null) {
            // No history yet — store this as the first data point and don't flag
            redisTemplate.opsForValue().set(avgKey, amount.toPlainString());
            return false;
        }

        BigDecimal avgAmount = new BigDecimal(avgStr);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(suspiciousAmountMultiplier));
        boolean suspicious = amount.compareTo(threshold) > 0;

        // Update the rolling average (simple moving average with new data point)
        BigDecimal newAvg = avgAmount.add(amount).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey, newAvg.toPlainString());

        log.info("Amount check — amount: {} avgAmount: {} threshold: {} suspicious: {}",
                amount, avgAmount, threshold, suspicious);

        return suspicious;
    }

    /**
     * Balance drain check: flags transactions that would drain more than the configured
     * percentage of the sender's account balance (e.g., 90%).
     *
     * @param senderBalance the current balance of the sender
     * @param amount        the transaction amount
     * @return true if the transaction drains too much of the balance
     */
    private boolean isBalanceDrainExceeded(BigDecimal senderBalance, BigDecimal amount) {
        BigDecimal maxAllowed = senderBalance.multiply(BigDecimal.valueOf(maxBalancePercentage));
        boolean exceeded = amount.compareTo(maxAllowed) > 0;

        log.info("Balance drain check — balance: {} maxAllowed: {} amount: {} exceeded: {}",
                senderBalance, maxAllowed, amount, exceeded);

        return exceeded;
    }
}
