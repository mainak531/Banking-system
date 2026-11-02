package com.banking.accountservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banking.accountservice.dto.AccountReponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountReponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Received request to create account for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountReponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PostMapping("/{accountNumber}/block")
    public ResponseEntity<Void> blockAccount(@PathVariable String accountNumber) {
        log.info("Received request to block account with account number: {}", accountNumber);
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok().build();
    }

    /*
     * saga step 1 - deduct balance
     * called by transection service when create transection
     */
    @PostMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        log.info("Received request to deduct {} from account with account number: {}", amount, accountNumber);
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("balance deducted successfully");
    }

    /*
     * saga step 4 - compensation transection endpoint
     * called by transection service in two scenerios
     * 1. fraud detect -> refund sender (undo step 1 )
     * 2. transection completed - credit recieved
     */
    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        log.info("Received request to refund {} to account with account number: {}", amount, accountNumber);
        accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("balance credited successfully");
    }
}
