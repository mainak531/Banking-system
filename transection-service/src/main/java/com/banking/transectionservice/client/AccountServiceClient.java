package com.banking.transectionservice.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for calling the account-service over HTTP.
 *
 * Saga roles:
 * - deductBalance: Step 1 — deduct from sender before processing
 * - creditBalance: Step 3 — credit receiver on completion OR refund sender on failure
 */
@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountServiceClient {

    /**
     * Saga Step 1: Deduct amount from sender account.
     * Saga Step 4 (compensation): Refund sender on OTP failure — reuse this endpoint by crediting back.
     */
    @PostMapping("/api/v1/accounts/{accountNumber}/deduct")
    String deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount);

    /**
     * Saga Step 3: Credit amount to receiver account.
     * Also used as the compensation (refund) by crediting the sender back.
     */
    @PostMapping("/api/v1/accounts/{accountNumber}/credit")
    String creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount);
}
