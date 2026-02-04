package com.org.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservedEvent {
    private UUID orderId;
    private UUID productId;
    private BigDecimal productPrice;
    private Integer productQuantity;
}
