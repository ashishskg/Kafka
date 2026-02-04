package com.org.orders.service;

import com.org.common.types.OrderStatus;
import com.org.orders.dto.OrderHistory;

import java.util.List;
import java.util.UUID;

public interface OrderHistoryService {
    void add(UUID orderId, OrderStatus orderStatus);

    List<OrderHistory> findByOrderId(UUID orderId);
}
