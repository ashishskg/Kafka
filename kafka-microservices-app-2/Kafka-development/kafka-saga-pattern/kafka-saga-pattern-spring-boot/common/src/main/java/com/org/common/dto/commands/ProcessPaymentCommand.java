package com.org.common.dto.commands;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {
    private UUID orderId;
    private UUID productId;
    private BigDecimal productPrice;
    private Integer productQuantity;

}
