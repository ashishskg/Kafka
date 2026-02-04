package com.mydomain.ws.products.service.impl;

import com.mydomain.ws.common.ProductCreatedEvent;
import com.mydomain.ws.products.model.CreateProductRestModel;
import com.mydomain.ws.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Slf4j
@Service
@Primary
public class ProductServiceImpl implements ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final static String topic = "product-created-events-topic";

    @Override
    public String createProduct(CreateProductRestModel productRestModel) throws ExecutionException, InterruptedException {

        // Generate a unique product ID and set it to the product
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId,
                productRestModel.getTitle(), productRestModel.getPrice(),
                productRestModel.getQuantity());

        log.info("Before publishing a ProductCreatedEvent");

        ProducerRecord<String, ProductCreatedEvent> producerRecord = new ProducerRecord<>(topic, productId, productCreatedEvent);

        producerRecord.headers().add("messageId", UUID.randomUUID().toString().getBytes());

        // Asynchronous send with CompletableFuture
        SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send(producerRecord).get();

        log.info("Partition: " + result.getRecordMetadata().partition());
        log.info("Topic: " + result.getRecordMetadata().topic());
        log.info("Offset: " + result.getRecordMetadata().offset());

        log.info("***** Returning product id");

        return productId;
    }
}
