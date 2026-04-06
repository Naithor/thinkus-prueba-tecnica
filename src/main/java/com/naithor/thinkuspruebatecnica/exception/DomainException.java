package com.naithor.thinkuspruebatecnica.exception;

public sealed interface DomainException
        permits InsufficientBalanceException, FundNotFoundException,
                ActiveSubscriptionNotFoundException, ClientNotFoundException {
    String getMessage();
}
