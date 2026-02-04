package com.mydomain.ws.common;


import com.mydomain.ws.common.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProductCreatedEvent {
    private Product product;
}
