package com.naithor.thinkuspruebatecnica.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;

public interface TransactionPort {
    Transaction save(Transaction transaction);
    List<Transaction> findByClientId(String clientId);
    Optional<Transaction> findLatestByClientIdAndFundIdAndType(String clientId, Integer fundId, TransactionType type);
}
