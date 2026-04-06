package com.naithor.thinkuspruebatecnica.exception;

public final class ClientNotFoundException extends RuntimeException implements DomainException {

    public ClientNotFoundException(String clientId) {
        super("Cliente no encontrado con id: %s".formatted(clientId));
    }
}
