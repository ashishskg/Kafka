package com.org.orders.dto;

import com.org.common.types.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryResponse {
    private UUID id;
    private UUID orderId;
    private OrderStatus status;
    private Timestamp createdAt;
}
