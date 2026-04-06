package com.naithor.thinkuspruebatecnica.adapter.out.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.model.TransactionType;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByClientIdOrderByTimestampDesc(String clientId);
    Optional<Transaction> findFirstByClientIdAndFundIdAndTypeOrderByTimestampDesc(
            String clientId, Integer fundId, TransactionType type);
}
