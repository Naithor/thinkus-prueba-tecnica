package com.naithor.thinkuspruebatecnica.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.naithor.thinkuspruebatecnica.domain.model.Fund;

public interface FundPort {
    Optional<Fund> findById(Integer id);
    List<Fund> findAll();
}
