package com.naithor.thinkuspruebatecnica.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.NotificationPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;
import com.naithor.thinkuspruebatecnica.exception.ClientNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.FundNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.InsufficientBalanceException;

@ExtendWith(MockitoExtension.class)
class SubscribeFundServiceTest {

    @Mock
    private ClientPort clientPort;

    @Mock
    private FundPort fundPort;

    @Mock
    private TransactionPort transactionPort;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private SubscribeFundService subscribeFundService;

    private Client client;
    private Fund fund;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id("client-1")
                .balance(new BigDecimal("500000"))
                .notificationPreference(com.naithor.thinkuspruebatecnica.domain.model.NotificationPreference.EMAIL)
                .contactInfo("test@email.com")
                .build();

        fund = Fund.builder()
                .id(1)
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minAmount(new BigDecimal("75000"))
                .category("FPV")
                .build();
    }

    @Test
    @DisplayName("should subscribe successfully when client has enough balance")
    void shouldSubscribeSuccessfully() {
        // Arrange
        when(fundPort.findById(1)).thenReturn(Optional.of(fund));
        when(clientPort.findById("client-1")).thenReturn(Optional.of(client));
        when(clientPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = subscribeFundService.execute("client-1", 1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.SUBSCRIBE);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("75000"));
        assertThat(result.getClientId()).isEqualTo("client-1");
        assertThat(result.getFundId()).isEqualTo(1);
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(client.getBalance()).isEqualTo(new BigDecimal("425000"));
        verify(clientPort).save(client);
        verify(transactionPort).save(any(Transaction.class));
        verify(notificationPort).send(eq("test@email.com"), eq("FPV_BTG_PACTUAL_RECAUDADORA"), any());
    }

    @Test
    @DisplayName("should throw FundNotFoundException when fund does not exist")
    void shouldThrowWhenFundNotFound() {
        // Arrange
        when(fundPort.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subscribeFundService.execute("client-1", 99))
                .isInstanceOf(FundNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("should throw ClientNotFoundException when client does not exist")
    void shouldThrowWhenClientNotFound() {
        // Arrange
        when(fundPort.findById(1)).thenReturn(Optional.of(fund));
        when(clientPort.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subscribeFundService.execute("unknown", 1))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("should throw InsufficientBalanceException when client balance is not enough")
    void shouldThrowWhenInsufficientBalance() {
        // Arrange
        var poorClient = Client.builder()
                .id("client-2")
                .balance(new BigDecimal("10000"))
                .notificationPreference(com.naithor.thinkuspruebatecnica.domain.model.NotificationPreference.EMAIL)
                .contactInfo("poor@email.com")
                .build();

        when(fundPort.findById(1)).thenReturn(Optional.of(fund));
        when(clientPort.findById("client-2")).thenReturn(Optional.of(poorClient));

        // Act & Assert
        assertThatThrownBy(() -> subscribeFundService.execute("client-2", 1))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("FPV_BTG_PACTUAL_RECAUDADORA")
                .hasMessageContaining("10000");
    }
}
