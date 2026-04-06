package com.naithor.thinkuspruebatecnica.adapter.out.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.naithor.thinkuspruebatecnica.domain.model.Fund;

public interface FundRepository extends MongoRepository<Fund, Integer> {
}
