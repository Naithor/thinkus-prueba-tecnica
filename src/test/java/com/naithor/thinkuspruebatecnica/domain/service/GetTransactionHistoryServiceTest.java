package com.naithor.thinkuspruebatecnica.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;

@ExtendWith(MockitoExtension.class)
class GetTransactionHistoryServiceTest {

    @Mock
    private TransactionPort transactionPort;

    @InjectMocks
    private GetTransactionHistoryService getTransactionHistoryService;

    @Test
    @DisplayName("should return transaction history for a client")
    void shouldReturnTransactionHistory() {
        // Arrange
        var transactions = List.of(
                Transaction.builder()
                        .id("tx-1")
                        .clientId("client-1")
                        .fundId(1)
                        .type(TransactionType.SUBSCRIBE)
                        .amount(new BigDecimal("75000"))
                        .build()
        );
        when(transactionPort.findByClientId("client-1")).thenReturn(transactions);

        // Act
        List<Transaction> result = getTransactionHistoryService.execute("client-1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("tx-1");
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.SUBSCRIBE);
    }

    @Test
    @DisplayName("should return empty list when client has no transactions")
    void shouldReturnEmptyListWhenNoTransactions() {
        // Arrange
        when(transactionPort.findByClientId("client-1")).thenReturn(List.of());

        // Act
        List<Transaction> result = getTransactionHistoryService.execute("client-1");

        // Assert
        assertThat(result).isEmpty();
    }
}
