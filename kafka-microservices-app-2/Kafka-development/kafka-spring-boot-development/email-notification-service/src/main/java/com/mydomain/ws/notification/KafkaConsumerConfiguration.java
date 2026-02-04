package com.mydomain.ws.notification;

import com.mydomain.ws.common.ProductCreatedEvent;
import com.mydomain.ws.notification.error.NotRetryableException;
import com.mydomain.ws.notification.error.RetryableException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfiguration {

    @Autowired
    Environment environment;

    @Bean
    public ConsumerFactory<String, ProductCreatedEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("spring.kafka.bootstrap-servers"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("spring.kafka.consumer.group-id"));

        // IMPORTANT: use ErrorHandlingDeserializer wrapper
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // delegate deserializers
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        // Use JacksonJsonDeserializer for the actual JSON -> POJO work
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        // Tell JacksonJsonDeserializer what type to build
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, ProductCreatedEvent.class.getName());
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, ProductCreatedEvent> consumerFactory, KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate
    ) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(5000, 3));

        // Do NOT retry deserialization problems (bad JSON/type/etc.)
//        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(NotRetryableException.class, DeserializationException.class);
        errorHandler.addRetryableExceptions(RetryableException.class);

        ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate(ProducerFactory<String, ProductCreatedEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    ProducerFactory<String, ProductCreatedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("spring.kafka.bootstrap-servers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
