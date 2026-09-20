package com.banking.transectionservice.entity.enums;

/**
 * Represents the nature of the financial transaction.
 *
 * Types:
 * - DEPOSIT: Adding funds to an account (e.g., cash deposit, salary credit).
 * - WITHDRAWAL: Removing funds from an account (e.g., ATM withdrawal).
 * - PAYMENT: Paying for a service or bill from an account.
 * - TRANSFER: Moving funds between two accounts (e.g., peer-to-peer, account-to-account).
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    PAYMENT,
    TRANSFER
}
