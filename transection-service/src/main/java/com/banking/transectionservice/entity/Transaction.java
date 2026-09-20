package com.banking.transectionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.banking.transectionservice.entity.enums.TransactionStatus;
import com.banking.transectionservice.entity.enums.TransactionType;

@Entity
@Table(name = "Transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false, length = 20)
    private String senderAccountNumber;
    @Column(nullable = false, length = 20)
    private String recieverAccountNumber;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    @Column(length = 255)
    private String description;
    @Column(length = 255)
    private String failureReason;
    @Column(nullable = false, length = 50)
    private String referenceNumber;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}