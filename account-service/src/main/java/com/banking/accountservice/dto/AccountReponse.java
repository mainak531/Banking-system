package com.banking.accountservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banking.accountservice.entity.enums.AccountStatus;
import com.banking.accountservice.entity.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReponse {
    private String accountId;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private String phone;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal dailyTransactionLimit;
    private LocalDateTime createdAt;
}