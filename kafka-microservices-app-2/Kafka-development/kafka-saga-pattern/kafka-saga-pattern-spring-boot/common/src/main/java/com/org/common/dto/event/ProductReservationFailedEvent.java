package com.org.common.dto.event;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservationFailedEvent {
    private UUID productId;
    private UUID orderId;
    private Integer productQuantity;
}
