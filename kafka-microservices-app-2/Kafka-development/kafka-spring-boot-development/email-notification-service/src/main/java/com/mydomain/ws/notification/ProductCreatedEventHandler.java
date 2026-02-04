package com.mydomain.ws.notification;

import com.mydomain.ws.common.ProductCreatedEvent;
import com.mydomain.ws.notification.error.NotRetryableException;
import com.mydomain.ws.notification.error.RetryableException;
import com.mydomain.ws.notification.io.ProcessedEventEntity;
import com.mydomain.ws.notification.io.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
@KafkaListener(topics = "product-created-events-topic")
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;
    private final ProcessedEventRepository processedEventRepository;

//    public ProductCreatedEventHandler(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header(value = "messageId", required = true) String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        log.info("Received event {}", productCreatedEvent);

        // Check if the message has already been processed before
        ProcessedEventEntity existingRecord = processedEventRepository.findByMessageId((messageId));

        if(existingRecord != null) {
            log.info("Found existing record {}", existingRecord);
            return;
        }

        String requestUrl = "http://localhost:8083/response/200";

        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                log.info("Received response from Product Service: {}", response.getBody());
            }
        } catch(ResourceAccessException e) {
            log.error("Resource access exception", e);
            throw new RetryableException(e);
        } catch(HttpServerErrorException e) {
           log.error(e.getMessage());
           throw new NotRetryableException(e);
        } catch(Exception e)    {
            log.error(e.getMessage());
            throw new NotRetryableException(e);
        }

        // Save a unique message id in the database table
        try {
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProduct().getProductId()));
        } catch (DataIntegrityViolationException e) {
            log.error("Error saving processed event", e);
            throw new NotRetryableException(e);
        }
    }
}
