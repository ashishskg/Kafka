package com.org.products.service.handler;

import com.org.common.dto.Product;
import com.org.common.dto.commands.CancelProductReservationCommand;
import com.org.common.dto.commands.ProductReservationCancelledEvent;
import com.org.common.dto.commands.ReserveProductCommand;
import com.org.common.dto.event.ProductReservationFailedEvent;
import com.org.common.dto.event.ProductReservedEvent;
import com.org.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${products.commands.topic.name}")
@RequiredArgsConstructor
@Slf4j
public class ProductCommandsHandler {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${products.events.topic.name}")
    private String productsEventsTopicName;

    @KafkaHandler
    public void handleCommand(@Payload ReserveProductCommand command) {

        try {
            Product desiredProduct = new Product(command.getProductId(), command.getProductQuantity());
            Product reserveProduct = productService.reserve(desiredProduct, command.getOrderId());
            ProductReservedEvent productReservedEvent = new ProductReservedEvent(
                    command.getOrderId(), command.getProductId(),
                    reserveProduct.getPrice(), command.getProductQuantity()
            );
            kafkaTemplate.send(productsEventsTopicName, productReservedEvent);
            log.info("Sent ProductReservedEvent {}", productReservedEvent);

        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            ProductReservationFailedEvent productReservationFailedEvent = new ProductReservationFailedEvent(
                   command.getProductId(), command.getOrderId(), command.getProductQuantity());
            kafkaTemplate.send(productsEventsTopicName, productReservationFailedEvent);
        }

    }

    @KafkaHandler
    public void onHandle(@Payload CancelProductReservationCommand command) {
        Product productToCancel = new Product(command.getProductId(), command.getProductQuantity());
        productService.cancelReservation(productToCancel, command.getOrderId());

        ProductReservationCancelledEvent productReservationCancelledEvent = new ProductReservationCancelledEvent(
                command.getProductId(), command.getOrderId());

        kafkaTemplate.send(productsEventsTopicName, productReservationCancelledEvent);
    }
}
