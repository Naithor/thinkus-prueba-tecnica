package com.naithor.thinkuspruebatecnica.domain.port.out;

import java.util.Optional;

import com.naithor.thinkuspruebatecnica.domain.model.Client;

public interface ClientPort {
    Optional<Client> findById(String id);
    Client save(Client client);
}
