package com.mydomain.ws.products.config;

import com.mydomain.ws.common.ProductCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
/**
 * Kafka Topic Architecture Diagram:
 *
 * <pre>
 *                 Topic: product-created-events-topic
 *
 *                +----------------------------------+
 *                |                                  |
 *                |        Kafka Topic                |
 *                |                                  |
 *                +----------------------------------+
 *                         |        |        |
 *                         |        |        |
 *                   --------------- ---------------
 *                  | Partition 0 | Partition 1 | Partition 2 |
 *                   --------------- ---------------
 *                       |   |   |    |   |   |    |   |   |
 *                       |   |   |    |   |   |    |   |   |
 *                 +-----------+-----------+-----------+
 *                 |  Replica   |  Replica   |  Replica |
 *                 |  (Leader)  | (Follower) | (Follower)|
 *                 +-----------+-----------+-----------+
 *
 * Example Broker Distribution:
 *
 *            Broker-1           Broker-2           Broker-3
 *         -------------      -------------      -------------
 *         | P0 (Leader) |    | P1 (Leader) |    | P2 (Leader) |
 *         | P1 (Follow) |    | P2 (Follow) |    | P0 (Follow) |
 *         | P2 (Follow) |    | P0 (Follow) |    | P1 (Follow) |
 *         -------------      -------------      -------------
 *
 * Legend:
 *  - Each partition has 3 replicas (because replicas=3)
 *  - One replica is the Leader, two are Followers
 *  - Leaders handle all reads/writes
 *  - Followers replicate data for fault tolerance
 *
 * Data Flow:
 *
 * Producer
 *    |
 *    v
 *  Kafka Broker (Leader Partition)
 *    |
 *    v
 *  Replication to Followers
 *    |
 *    v
 * Consumers read from Leaders
 *
 * Parallelism:
 *  - 3 partitions ⇒ up to 3 consumers can process messages in parallel
 *
 * Reliability:
 *  - min.insync.replicas = 2
 *  - At least 2 replicas must acknowledge a write for success (acks=all)
 * </pre>
 */

/**
 * Spring configuration that declares Kafka topics used by the application.
 *
 * <p>This configuration defines a single topic named {@code product-created-events-topic} with
 * explicit partitioning and replication settings to control scalability and fault tolerance.</p>
 *
 * <p>Definitions:
 * <ul>
 *   <li><b>Partition</b> — a topic is split into one or more partitions. Each partition is an ordered,
 *       immutable sequence of records and provides parallelism: consumers in a consumer group can
 *       read from different partitions concurrently. Ordering is guaranteed only within a single partition.</li>
 *   <li><b>Replica</b> — a copy of a partition stored on a broker. Replication provides redundancy
 *       and fault tolerance. One replica is elected the leader for a partition and handles all reads/writes;
 *       other replicas are followers that replicate the leader's data.</li>
 *   <li><b>min.insync.replicas</b> — the minimum number of replicas that must acknowledge a write for it to be
 *       considered successful when using acks=all. Increasing this value improves durability at the cost of availability.</li>
 * </ul>
 * </p>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @Value("${spring.kafka.producer.acks}")
    private String acks;

    @Value("${spring.kafka.producer.retries}")
    private  String retries;

    @Value("${spring.kafka.producer.properties.delivery.timeout.ms}")
    private String deliveryTimeoutMs;

    @Value("${spring.kafka.producer.properties.linger.ms}")
    private String lingerMs;

    @Value("${spring.kafka.producer.properties.request.timeout.ms}")
    private String requestTimeoutMs;

    @Value("${spring.kafka.producer.properties.enable.idempotence}")
    private String enableIdempotence;

    @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection}")
    private String maxInFlightRequestsPerConnection;

    Map<String, Object> producerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.parseInt(deliveryTimeoutMs));
        config.put(ProducerConfig.LINGER_MS_CONFIG, Integer.parseInt(lingerMs));
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, Integer.parseInt(requestTimeoutMs));

        // Idempotence settings (uncomment if needed)
        // acks=all is required for idempotence
         config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
         config.put(ProducerConfig.ACKS_CONFIG, acks);

         // To ensure message ordering with idempotence, set max.in.flight.requests.per.connection to less than or equal to 5
         config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
//         config.put(ProducerConfig.RETRIES_CONFIG, retries);

        return config;
    }

    @Bean
    ProducerFactory<String, ProductCreatedEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Creates the Kafka topic used for product-created events.
     *
     * <p>The topic is created with:
     * <ul>
     *   <li>{@code partitions=3} — enables parallelism and scaling across 3 partitions</li>
     *   <li>{@code replicas=3} — keeps 3 copies of each partition for redundancy</li>
     *   <li>{@code configs(Map.of("min.insync.replicas", "2"))} — requires at least 2 in-sync replicas to acknowledge writes</li>
     * </ul>
     * </p>
     *
     * @return a configured {@link NewTopic} instance
     */
    @Bean
    NewTopic createTopic() {
        String topicName = "product-created-events-topic";
//        String topicName = "topic2";
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
}