package com.naithor.thinkuspruebatecnica.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.port.in.GetTransactionHistoryUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTransactionHistoryService implements GetTransactionHistoryUseCase {

    private final TransactionPort transactionPort;

    @Override
    public List<Transaction> execute(String clientId) {
        return transactionPort.findByClientId(clientId);
    }
}
