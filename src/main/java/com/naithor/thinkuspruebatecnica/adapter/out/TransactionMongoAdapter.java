package com.naithor.thinkuspruebatecnica.adapter.out;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.naithor.thinkuspruebatecnica.adapter.out.repository.TransactionRepository;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;
import com.naithor.thinkuspruebatecnica.domain.port.out.TransactionPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionMongoAdapter implements TransactionPort {

    private final TransactionRepository repository;

    @Override
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    @Override
    public List<Transaction> findByClientId(String clientId) {
        return repository.findByClientIdOrderByTimestampDesc(clientId);
    }

    @Override
    public Optional<Transaction> findLatestByClientIdAndFundIdAndType(String clientId, Integer fundId, TransactionType type) {
        return repository.findFirstByClientIdAndFundIdAndTypeOrderByTimestampDesc(clientId, fundId, type);
    }
}
