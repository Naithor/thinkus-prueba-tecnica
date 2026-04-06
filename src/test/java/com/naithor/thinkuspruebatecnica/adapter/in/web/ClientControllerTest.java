package com.naithor.thinkuspruebatecnica.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.NotificationPreferenceRequest;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.TransactionResponse;
import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.model.NotificationPreference;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.in.GetTransactionHistoryUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.exception.ClientNotFoundException;

import com.naithor.thinkuspruebatecnica.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    @Mock
    private ClientPort clientPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ClientController(getTransactionHistoryUseCase, clientPort)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("should return transaction history for a client")
    void shouldReturnTransactionHistory() throws Exception {
        // Arrange
        var transactions = List.of(
                Transaction.builder()
                        .id("tx-1")
                        .clientId("client-1")
                        .fundId(1)
                        .type(TransactionType.SUBSCRIBE)
                        .amount(new BigDecimal("75000"))
                        .timestamp(Instant.now())
                        .build()
        );
        when(getTransactionHistoryUseCase.execute("client-1")).thenReturn(transactions);

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/client-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("tx-1"))
                .andExpect(jsonPath("$[0].type").value("SUBSCRIBE"));
    }

    @Test
    @DisplayName("should return empty list when client has no transactions")
    void shouldReturnEmptyListWhenNoTransactions() throws Exception {
        // Arrange
        when(getTransactionHistoryUseCase.execute("client-1")).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/client-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("should update notification preferences successfully")
    void shouldUpdateNotificationPreferences() throws Exception {
        // Arrange
        var client = Client.builder()
                .id("client-1")
                .balance(new BigDecimal("500000"))
                .notificationPreference(NotificationPreference.EMAIL)
                .contactInfo("old@email.com")
                .build();

        when(clientPort.findById("client-1")).thenReturn(Optional.of(client));
        when(clientPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new NotificationPreferenceRequest(NotificationPreference.SMS, "5551234567");

        // Act & Assert
        mockMvc.perform(patch("/api/v1/clients/client-1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(client.getNotificationPreference()).isEqualTo(NotificationPreference.SMS);
        assertThat(client.getContactInfo()).isEqualTo("5551234567");
    }

    @Test
    @DisplayName("should return 404 when updating preferences for non-existent client")
    void shouldReturn404WhenUpdatingPreferencesForNonExistentClient() throws Exception {
        // Arrange
        when(clientPort.findById("unknown")).thenReturn(Optional.empty());

        var request = new NotificationPreferenceRequest(NotificationPreference.EMAIL, "test@email.com");

        // Act & Assert
        mockMvc.perform(patch("/api/v1/clients/unknown/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado con id: unknown"));
    }
}
