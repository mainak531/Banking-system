package com.banking.transectionservice.entity.enums;

/**
 * Represents the lifecycle state of a transaction in the system.
 *
 * Typical Transaction Flows:
 * 1. Clean Transaction: PENDING -> PROCESSING -> COMPLETED
 * 2. Fraud Detected: PENDING -> PROCESSING -> FLAGGED (blocked)
 * 3. Insufficient Balance: PENDING -> PROCESSING -> FAILED (rollback)
 * 4. Manual Verification: COMPLETED -> PENDING_VERIFICATION -> COMPLETED (verified by bank)
 * 5. Saga Refund: COMPLETED/PROCESSING -> FLAGGED (saga rollback)
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
