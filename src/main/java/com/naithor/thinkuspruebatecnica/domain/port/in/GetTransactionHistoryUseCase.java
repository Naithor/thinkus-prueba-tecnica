package com.naithor.thinkuspruebatecnica.domain.port.in;

import java.util.List;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;

public interface GetTransactionHistoryUseCase {
    List<Transaction> execute(String clientId);
}
