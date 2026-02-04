package com.org.common.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductInsufficientQuantityException extends RuntimeException {
    private final UUID productId;
    private final UUID orderId;

    public ProductInsufficientQuantityException(UUID productId, UUID orderId) {
        super("Product " + productId + " has insufficient quantity in the stock for order " + orderId);
        this.productId = productId;
        this.orderId = orderId;
    }
}
