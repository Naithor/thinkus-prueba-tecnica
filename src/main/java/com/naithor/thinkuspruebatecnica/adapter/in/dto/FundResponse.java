package com.naithor.thinkuspruebatecnica.adapter.in.dto;

import com.naithor.thinkuspruebatecnica.domain.model.Fund;

import java.math.BigDecimal;

public record FundResponse(
        Integer id,
        String name,
        BigDecimal minAmount,
        String category
) {
    public static FundResponse from(Fund fund) {
        return new FundResponse(
                fund.getId(),
                fund.getName(),
                fund.getMinAmount(),
                fund.getCategory()
        );
    }
}
