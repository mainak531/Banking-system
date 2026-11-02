package com.banking.accountservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.enums.AccountStatus;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.dto.AccountReponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.mapper.AccountMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private static SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AccountReponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for email: {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())) {
            log.error("Account creation failed: Email {} already exists", request.getEmail());
            throw new RuntimeException("Account already exists with email: " + request.getEmail());
        }

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getOpeningBalance());
        BigDecimal dailyLimit = switch (request.getAccountType()) {
            case SAVING -> new BigDecimal("10000.00");
            case CURRENT -> new BigDecimal("100000.00");
            case FIXED_DEPOSIT -> new BigDecimal("0.00");
        };
        account.setDailyTransactionLimit(dailyLimit);

        log.info("Account created successfully with account number: {}", account.getAccountNumber());
        return accountMapper.mapToResponse(accountRepository.save(account));
    }

    /**
     * Fetch account details by account number.
     * Throws an exception if the account is not found.
     * 
     * @param accountNumber the unique account number
     * @return AccountReponse DTO containing the account details
     */
    public AccountReponse getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account with account number: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found with account number: {}", accountNumber);
                    return new RuntimeException("Account not found with account number: " + accountNumber);
                });
        return accountMapper.mapToResponse(account);
    }

    /**
     * Fetch the current balance of the specified account.
     * Throws an exception if the account is not found.
     * 
     * @param accountNumber the unique account number
     * @return BigDecimal representing the current account balance
     */
    public BigDecimal getBalance(String accountNumber) {
        log.info("Fetching balance for account number: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found with account number: {}", accountNumber);
                    return new RuntimeException("Account not found with account number: " + accountNumber);
                });
        return account.getBalance();
    }

    /*
     * Block account - called by fraud detection service via kafka
     * 
     * @param accountNumber
     */
    @Transactional
    public void blockAccount(String accountNumber) {
        log.info("Blocking account with account number: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found with account number: {}", accountNumber);
                    return new RuntimeException("Account not found with account number: " + accountNumber);
                });
        account.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
    }

    /*
     * Deduct balance from sender account
     * called by transectional service
     * 
     * @param accountNumber
     * 
     * @param amount
     */
    @Transactional
    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Deducting {} from account with account number: {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found with account number: {}", accountNumber);
                    return new RuntimeException("Account not found with account number: " + accountNumber);
                });
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            log.error("Cannot deduct balance: Account {} is not active", accountNumber);
            throw new RuntimeException("Account is not active");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            log.error("Cannot deduct balance: Insufficient balance for account {}", accountNumber);
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    /*
     * Credit balance
     * called by transectional service via kafka
     * 
     * @param accountNumber
     * 
     * @param amount
     */
    @Transactional
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Crediting {} to account with account number: {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("Account not found with account number: {}", accountNumber);
                    return new RuntimeException("Account not found with account number: " + accountNumber);
                });
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            log.error("Cannot credit balance: Account {} is not active", accountNumber);
            throw new RuntimeException("Account is not active");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    private String generateAccountNumber() {
        String accountNumber;

        do {
            long randomLong = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d", randomLong);
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}