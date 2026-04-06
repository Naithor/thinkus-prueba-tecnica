package com.naithor.thinkuspruebatecnica.domain.service;

import java.math.BigDecimal;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.port.in.SubscribeFundUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.NotificationPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;
import com.naithor.thinkuspruebatecnica.exception.ActiveSubscriptionNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.ClientNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.FundNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.InsufficientBalanceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public class SubscribeFundService implements SubscribeFundUseCase {

    private final ClientPort clientPort;
    private final FundPort fundPort;
    private final TransactionPort transactionPort;
    private final NotificationPort notificationPort;

    @Override
    public Transaction execute(String clientId, Integer fundId) {
        var fund = fundPort.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        var client = clientPort.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (!client.hasSufficientBalance(fund.getMinAmount())) {
            throw new InsufficientBalanceException(fund.getName(), fund.getMinAmount(), client.getBalance());
        }

        client.debit(fund.getMinAmount());
        clientPort.save(client);

        var transaction = Transaction.newSubscribe(clientId, fundId, fund.getMinAmount());
        transactionPort.save(transaction);

        notificationPort.send(
                client.getContactInfo(),
                fund.getName(),
                "Suscripcion exitosa al fondo %s por $%s".formatted(fund.getName(), fund.getMinAmount())
        );

        return transaction;
    }
}
