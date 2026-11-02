package com.banking.transectionservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionIntiatedEvent {
    private String transactionId;
    private String senderAccountNumber;
    private String recieverAccountNumber;
    private BigDecimal amount;
    private String description;
}
