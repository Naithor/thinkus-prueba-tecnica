package com.naithor.thinkuspruebatecnica.seeder;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.naithor.thinkuspruebatecnica.adapter.out.repository.FundRepository;
import com.naithor.thinkuspruebatecnica.domain.model.Fund;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FundSeeder implements CommandLineRunner {

    private final FundRepository fundRepository;

    @Override
    public void run(String... args) {
        if (fundRepository.count() == 0) {
            fundRepository.saveAll(List.of(
                    Fund.builder().id(1).name("FPV_BTG_PACTUAL_RECAUDADORA").minAmount(new BigDecimal("75000")).category("FPV").build(),
                    Fund.builder().id(2).name("FPV_BTG_PACTUAL_ECOPETROL").minAmount(new BigDecimal("125000")).category("FPV").build(),
                    Fund.builder().id(3).name("DEUDAPRIVADA").minAmount(new BigDecimal("50000")).category("FIC").build(),
                    Fund.builder().id(4).name("FDO-ACCIONES").minAmount(new BigDecimal("250000")).category("FIC").build(),
                    Fund.builder().id(5).name("FPV_BTG_PACTUAL_DINAMICA").minAmount(new BigDecimal("100000")).category("FPV").build()
            ));
        }
    }
}
