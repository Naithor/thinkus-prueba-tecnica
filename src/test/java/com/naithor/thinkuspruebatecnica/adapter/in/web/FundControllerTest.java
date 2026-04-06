package com.naithor.thinkuspruebatecnica.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.SubscribeRequest;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.in.CancelSubscriptionUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.in.SubscribeFundUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;

@ExtendWith(MockitoExtension.class)
class FundControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubscribeFundUseCase subscribeFundUseCase;

    @Mock
    private CancelSubscriptionUseCase cancelSubscriptionUseCase;

    @Mock
    private FundPort fundPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new FundController(subscribeFundUseCase, cancelSubscriptionUseCase, fundPort)
        ).build();
    }

    @Test
    void shouldSubscribeToFund() throws Exception {
        var tx = Transaction.builder()
                .id("tx-1")
                .clientId("client-1")
                .fundId(1)
                .type(TransactionType.SUBSCRIBE)
                .amount(new BigDecimal("75000"))
                .timestamp(Instant.now())
                .build();

        when(subscribeFundUseCase.execute(eq("client-1"), eq(1))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/funds/1/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribeRequest("client-1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tx-1"))
                .andExpect(jsonPath("$.type").value("SUBSCRIBE"))
                .andExpect(jsonPath("$.amount").value(75000));
    }

    @Test
    void shouldCancelSubscription() throws Exception {
        var tx = Transaction.builder()
                .id("tx-2")
                .clientId("client-1")
                .fundId(1)
                .type(TransactionType.CANCEL)
                .amount(new BigDecimal("75000"))
                .timestamp(Instant.now())
                .build();

        when(cancelSubscriptionUseCase.execute("client-1", 1)).thenReturn(tx);

        mockMvc.perform(delete("/api/v1/funds/1/subscribe")
                        .param("clientId", "client-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CANCEL"));
    }

    @Test
    void shouldListFunds() throws Exception {
        var funds = List.of(
                Fund.builder().id(1).name("FPV_BTG_PACTUAL_RECAUDADORA").minAmount(new BigDecimal("75000")).category("FPV").build(),
                Fund.builder().id(2).name("FPV_BTG_PACTUAL_ECOPETROL").minAmount(new BigDecimal("125000")).category("FPV").build()
        );

        when(fundPort.findAll()).thenReturn(funds);

        mockMvc.perform(get("/api/v1/funds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("FPV_BTG_PACTUAL_RECAUDADORA"));
    }
}
