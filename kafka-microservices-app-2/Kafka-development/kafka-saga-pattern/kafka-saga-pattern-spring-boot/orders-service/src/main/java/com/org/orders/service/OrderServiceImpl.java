package com.org.orders.service;

import com.org.common.dto.Order;
import com.org.common.dto.event.OrderApprovedEvent;
import com.org.common.dto.event.OrderCreatedEvent;
import com.org.common.types.OrderStatus;
import com.org.orders.dao.jpa.entity.OrderEntity;
import com.org.orders.dao.jpa.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${orders.events.topic.name}")
    private String ordersEventsTopic;

    @Override
    public Order placeOrder(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(order.getCustomerId());
        entity.setProductId(order.getProductId());
        entity.setProductQuantity(order.getProductQuantity());
        entity.setStatus(OrderStatus.CREATED);
        orderRepository.save(entity);

        // Publish event to Kafka
        OrderCreatedEvent placeOrder = new OrderCreatedEvent(entity.getId(), entity.getCustomerId(),
                entity.getProductId(), entity.getProductQuantity());
        kafkaTemplate.send(ordersEventsTopic,  placeOrder);

        // End of Publish event to Kafka
        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getProductQuantity(),
                entity.getStatus());
    }

    @Override
    public void approveOrder(UUID orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(orderEntity, "No Order is found with id " + orderId + " in the database table.");
        orderEntity.setStatus(OrderStatus.APPROVED);
        orderRepository.save(orderEntity);

        OrderApprovedEvent orderApprovedEvent = new  OrderApprovedEvent(orderEntity.getId());
        kafkaTemplate.send(ordersEventsTopic, orderApprovedEvent);
    }

    @Override
    public void rejectOrder(UUID orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElse(null);
        Assert.notNull(orderEntity, "No Order is found with id " + orderId + " in the database table.");
        orderEntity.setStatus(OrderStatus.REJECTED);
        orderRepository.save(orderEntity);
    }

}
