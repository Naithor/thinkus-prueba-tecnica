package com.naithor.thinkuspruebatecnica.adapter.out;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.naithor.thinkuspruebatecnica.adapter.out.repository.ClientRepository;
import com.naithor.thinkuspruebatecnica.domain.model.Client;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientMongoAdapter implements ClientPort {

    private final ClientRepository repository;

    @Override
    public Optional<Client> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Client save(Client client) {
        return repository.save(client);
    }
}
