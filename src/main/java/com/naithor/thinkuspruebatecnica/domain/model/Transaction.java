package com.naithor.thinkuspruebatecnica.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String id;
    private String clientId;
    private Integer fundId;
    private TransactionType type;
    private BigDecimal amount;
    private Instant timestamp;

    public static Transaction newSubscribe(String clientId, Integer fundId, BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fundId)
                .type(TransactionType.SUBSCRIBE)
                .amount(amount)
                .timestamp(Instant.now())
                .build();
    }

    public static Transaction newCancel(String clientId, Integer fundId, BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fundId)
                .type(TransactionType.CANCEL)
                .amount(amount)
                .timestamp(Instant.now())
                .build();
    }
}
