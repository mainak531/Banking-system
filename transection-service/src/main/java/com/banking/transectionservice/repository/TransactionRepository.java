package com.banking.transectionservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banking.transectionservice.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    List<Transaction> findBySenderAccountNumberOrRecieverAccountNumber(String senderAccountNumber,
            String recieverAccountNumber);
}
