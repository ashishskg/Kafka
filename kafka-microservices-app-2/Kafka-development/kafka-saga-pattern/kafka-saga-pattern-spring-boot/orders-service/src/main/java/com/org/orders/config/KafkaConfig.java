package com.org.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Value("${orders.events.topic.name}")
    private String ordersEventsTopicName;
    @Value("${products.commands.topic.name}")
    private String productsCommandTopicName;
    @Value("${payments.commands.topic.name}")
    private String paymentsCommandTopicName;
    @Value("${orders.commands.topic.name}")
    private String ordersCommandsTopicName;

    private final static Integer TOPIC_REPLICATION_FACTOR = 3;
    private final static Integer TOPIC_PARTITIONS = 3;

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    NewTopic createOrdersEventsTopic() {
        return TopicBuilder.name(ordersEventsTopicName)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    NewTopic createProductsCommandTopic() {
        return TopicBuilder.name(productsCommandTopicName)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    NewTopic createPaymentsCommandTopic() {
        return TopicBuilder.name(paymentsCommandTopicName)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    NewTopic createOrdersCommandsTopic() {
        return TopicBuilder.name(ordersCommandsTopicName)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

}
