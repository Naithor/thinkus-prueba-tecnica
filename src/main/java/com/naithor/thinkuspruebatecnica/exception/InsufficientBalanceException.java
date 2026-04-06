package com.naithor.thinkuspruebatecnica.exception;

import java.math.BigDecimal;

public final class InsufficientBalanceException extends RuntimeException implements DomainException {

    public InsufficientBalanceException(String fundName, BigDecimal required, BigDecimal available) {
        super("No tiene saldo disponible para vincularse al fondo %s. Requerido: $%s, Disponible: $%s"
                .formatted(fundName, required, available));
    }
}
