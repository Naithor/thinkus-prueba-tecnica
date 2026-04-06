package com.naithor.thinkuspruebatecnica.adapter.out;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.naithor.thinkuspruebatecnica.adapter.out.repository.FundRepository;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FundMongoAdapter implements FundPort {

    private final FundRepository repository;

    @Override
    public Optional<Fund> findById(Integer id) {
        return repository.findById(id);
    }

    @Override
    public List<Fund> findAll() {
        return repository.findAll();
    }
}
