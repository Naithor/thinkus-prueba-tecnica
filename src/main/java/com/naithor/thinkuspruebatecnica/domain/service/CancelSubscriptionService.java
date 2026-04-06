package com.naithor.thinkuspruebatecnica.domain.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.in.CancelSubscriptionUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;
import com.naithor.thinkuspruebatecnica.exception.ActiveSubscriptionNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.ClientNotFoundException;
import com.naithor.thinkuspruebatecnica.exception.FundNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final ClientPort clientPort;
    private final FundPort fundPort;
    private final TransactionPort transactionPort;

    @Override
    public Transaction execute(String clientId, Integer fundId) {
        var fund = fundPort.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        var client = clientPort.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        transactionPort.findLatestByClientIdAndFundIdAndType(clientId, fundId, TransactionType.SUBSCRIBE)
                .orElseThrow(() -> new ActiveSubscriptionNotFoundException(fund.getName()));

        client.credit(fund.getMinAmount());
        clientPort.save(client);

        var transaction = Transaction.newCancel(clientId, fundId, fund.getMinAmount());
        transactionPort.save(transaction);

        return transaction;
    }
}
