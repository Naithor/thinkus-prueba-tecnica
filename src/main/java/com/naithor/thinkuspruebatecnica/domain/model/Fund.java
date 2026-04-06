package com.naithor.thinkuspruebatecnica.domain.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "funds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund {

    @Id
    private Integer id;
    private String name;
    private BigDecimal minAmount;
    private String category;
}
