package com.mydomain.ws.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Product {

    private String productId;
    private String title;
    private BigDecimal price;
    private Integer quantity;
}
