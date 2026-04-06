package com.naithor.thinkuspruebatecnica.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.NotificationPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;

@ExtendWith(MockitoExtension.class)
class SubscribeFundServiceConcurrencyTest {

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

    private Fund fund;

    @BeforeEach
    void setUp() {
        fund = Fund.builder()
                .id(1)
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minAmount(new BigDecimal("75000"))
                .category("FPV")
                .build();
    }

    @Test
    void shouldHandleConcurrentSubscriptionsSafely() throws Exception {
        var successCount = new AtomicInteger(0);
        var conflictCount = new AtomicInteger(0);
        var threadCount = 10;

        when(fundPort.findById(1)).thenReturn(Optional.of(fund));
        lenient().when(clientPort.findById(anyString())).thenAnswer(invocation -> {
            var clientId = invocation.getArgument(0, String.class);
            return Optional.of(Client.builder()
                    .id(clientId)
                    .balance(new BigDecimal("500000"))
                    .notificationPreference(com.naithor.thinkuspruebatecnica.domain.model.NotificationPreference.EMAIL)
                    .contactInfo("test@email.com")
                    .version(0L)
                    .build());
        });
        when(clientPort.save(any())).thenAnswer(invocation -> {
            var c = invocation.<Client>getArgument(0);
            if (c.getVersion() != null && c.getVersion() > 0 && Math.random() > 0.5) {
                throw new OptimisticLockingFailureException("Version conflict");
            }
            c.setVersion(c.getVersion() + 1);
            return c;
        });
        lenient().when(transactionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationPort).send(anyString(), anyString(), anyString());

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            var futures = new CompletableFuture<?>[threadCount];

            for (var i = 0; i < threadCount; i++) {
                final var clientId = "client-" + i;

                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        subscribeFundService.execute(clientId, 1);
                        successCount.incrementAndGet();
                    } catch (OptimisticLockingFailureException e) {
                        conflictCount.incrementAndGet();
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).join();
        }

        assertThat(successCount.get()).isGreaterThan(0);
        System.out.println("Successful: %d, Conflicts handled: %d".formatted(successCount.get(), conflictCount.get()));
    }
}
