package com.org.products.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductCreationResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
