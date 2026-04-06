package com.naithor.thinkuspruebatecnica.exception;

public final class FundNotFoundException extends RuntimeException implements DomainException {

    public FundNotFoundException(Integer fundId) {
        super("Fondo no encontrado con id: %d".formatted(fundId));
    }
}
