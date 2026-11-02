package com.banking.accountservice.mapper;

import org.springframework.stereotype.Component;

import com.banking.accountservice.dto.AccountReponse;
import com.banking.accountservice.entity.Account;

@Component
public class AccountMapper {

    public AccountReponse mapToResponse(Account account) {
        if (account == null) {
            return null;
        }
        
        return AccountReponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
