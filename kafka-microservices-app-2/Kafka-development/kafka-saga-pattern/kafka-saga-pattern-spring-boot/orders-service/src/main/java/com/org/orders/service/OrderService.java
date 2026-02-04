package com.org.orders.service;

import com.org.common.dto.Order;

import java.util.UUID;

public interface OrderService {
    Order placeOrder(Order order);

    void approveOrder(UUID orderId);

    void rejectOrder(UUID orderId);
}
