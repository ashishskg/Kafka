package com.org.common.events;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestedEvent {
    private String senderId;
    private String recepientId;
    private BigDecimal amount;
}
