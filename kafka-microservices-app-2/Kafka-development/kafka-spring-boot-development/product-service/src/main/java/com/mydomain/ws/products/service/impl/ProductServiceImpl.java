package com.mydomain.ws.products.service.impl;

import com.mydomain.ws.common.Product;
import com.mydomain.ws.common.ProductCreatedEvent;
import com.mydomain.ws.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Service
@Primary
public class ProductServiceImpl implements ProductService {

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final static String topic = "product-created-events-topic";
//    private final static String topic = "topic2";
    @Override
    public String createProduct(Product product) {

        // Generate a unique product ID and set it to the product
        String productId = UUID.randomUUID().toString();
        product.setProductId(productId);
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setProduct(product);

        // Todo::Check this line in cursor tools
       // log.info("ProductService createProduct : Product : {}", product);

        // Send message synchronously to Kafka topic
        //  kafkaTemplate.send("product-created-events-topic", productId, event);


        ProducerRecord<String, ProductCreatedEvent> producerRecord = new ProducerRecord<>(topic, productId, productCreatedEvent);
        producerRecord.headers().add("messageId", UUID.randomUUID().toString().getBytes());

        // Asynchronous send with CompletableFuture
        CompletableFuture<SendResult<String, ProductCreatedEvent>> completableFuture = kafkaTemplate.send(producerRecord);
        completableFuture.whenComplete((result, exception) -> {
            if (exception != null) {
                // Handle failure
               log.error("Failed to send message: " + exception.getMessage());
            } else {
                // Handle success
                log.info("Message sent successfully with offset: " + result.getRecordMetadata());
            }
        });

//      To run it    synchronously   remove comment from below line
//      completableFuture.join();
        log.info("createProductASynchronously Before returning productId: " + productId);
        return productId;
    }

    @Override
    public String createProductSynchronously(Product product) throws Exception {

        // Generate a unique product ID and set it to the product
        String productId = UUID.randomUUID().toString();
        product.setProductId(productId);
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setProduct(product);

        ProducerRecord<String, ProductCreatedEvent> producerRecord = new ProducerRecord<>(topic, productId, productCreatedEvent);
        producerRecord.headers().add("messageId", UUID.randomUUID().toString().getBytes());
//        producerRecord.headers().add("messageId", "123".getBytes());

        SendResult<String, ProductCreatedEvent> result =
               kafkaTemplate.send(producerRecord).get();

        log.info("createProductSynchronously() Before returning productId: {} ", productId);
        return productId;
    }
}
