package com.org.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private Integer productQuantity;
}
