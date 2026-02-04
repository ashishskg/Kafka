package com.org.common.events;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequestedEvent {

    private String senderId;
    private String recepientId;
    private BigDecimal amount;
}
