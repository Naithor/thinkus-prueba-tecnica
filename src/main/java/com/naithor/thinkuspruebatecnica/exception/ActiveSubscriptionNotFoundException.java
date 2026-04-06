package com.naithor.thinkuspruebatecnica.exception;

public final class ActiveSubscriptionNotFoundException extends RuntimeException implements DomainException {

    public ActiveSubscriptionNotFoundException(String fundName) {
        super("No se encontro una suscripcion activa para el fondo: %s".formatted(fundName));
    }
}
