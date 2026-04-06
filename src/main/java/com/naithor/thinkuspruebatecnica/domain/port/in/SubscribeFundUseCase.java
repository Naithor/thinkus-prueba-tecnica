package com.naithor.thinkuspruebatecnica.domain.port.in;

import com.naithor.thinkuspruebatecnica.domain.model.Transaction;

public interface SubscribeFundUseCase {
    Transaction execute(String clientId, Integer fundId);
}
