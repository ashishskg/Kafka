package com.org.orders.saga;

import com.org.common.dto.commands.*;
import com.org.common.dto.event.*;
import com.org.common.types.OrderStatus;
import com.org.orders.service.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@KafkaListener(topics = {
        "${orders.events.topic.name}",
        "${products.events.topic.name}",
        "${payments.events.topic.name}"
})
@RequiredArgsConstructor
public class OrderSaga {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderHistoryService orderHistoryService;

    @Value("${products.commands.topic.name}")
    private String productsCommandTopic;

    @Value("${payments.commands.topic.name}")
    private String paymentsCommandTopic;

    @Value("${orders.commands.topic.name}")
    private String ordersCommandsTopic;

    @KafkaHandler
    public void handleEvent(@Payload OrderCreatedEvent event) {

        log.info("Received OrderCreatedEvent {}", event);
        ReserveProductCommand command = new ReserveProductCommand(
                event.getProductId(), event.getProductQuantity(), event.getOrderId()
        );

        log.info("Sending OrderCreatedEvent {}", command);
        this.kafkaTemplate.send(productsCommandTopic, command);
        log.info("Sent OrderCreatedEvent {}", command);
        log.info("Sending new OrderCreatedEvent {}", command);
        orderHistoryService.add(event.getOrderId(), OrderStatus.CREATED);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservedEvent event) {
        log.info("Received ProductReservedEvent {}", event);

        ProcessPaymentCommand processPaymentCommand = new ProcessPaymentCommand(
                event.getOrderId(), event.getProductId(),
                event.getProductPrice(), event.getProductQuantity());

        kafkaTemplate.send(paymentsCommandTopic, processPaymentCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationFailedEvent event) {
        log.info("Received ProductReservationFailedEvent {}", event);
        orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentProcessEvent paymentProcessEvent) {
        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessEvent.getOrderId());;
        kafkaTemplate.send(ordersCommandsTopic, approveOrderCommand);
    }

    @KafkaHandler
    public void handleEvent(@Payload OrderApprovedEvent orderApprovedEvent) {
        orderHistoryService.add(orderApprovedEvent.getOrderId(), OrderStatus.APPROVED);
    }

    @KafkaHandler
    public void handleEvent(@Payload PaymentFailedEvent paymentFailedEvent) {
        CancelProductReservationCommand cancelProductReservationCommand = new CancelProductReservationCommand(
                paymentFailedEvent.getProductId(),
                paymentFailedEvent.getOrderId(),
                paymentFailedEvent.getProductQuantity());
        kafkaTemplate.send(productsCommandTopic, cancelProductReservationCommand);

    }

    @KafkaHandler
    public void handleEvent(@Payload ProductReservationCancelledEvent event) {
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(event.getOrderId());
        kafkaTemplate.send(ordersCommandsTopic, rejectOrderCommand);
        orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
    }

}
