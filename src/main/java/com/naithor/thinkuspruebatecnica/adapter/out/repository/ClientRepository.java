package com.naithor.thinkuspruebatecnica.adapter.out.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.naithor.thinkuspruebatecnica.domain.model.Client;

public interface ClientRepository extends MongoRepository<Client, String> {
}
