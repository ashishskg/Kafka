# Codebase Explanation (Format B) — `kafka-integration-test/`

This document is a per-file walkthrough of the `kafka-integration-test/` application.

- Build artifacts and IDE metadata are excluded from discussion: `target/`, `.idea/`.

---

## What this app is

`kafka-integration-test/` demonstrates how to:

- Publish a Kafka event (`ProductCreatedEvent`) from a Spring Boot REST API.
- Validate Kafka publishing end-to-end using **Spring Kafka’s Embedded Kafka** in an integration test.

Unlike `kafka-spring-boot-development/`, here the `ProductCreatedEvent` DTO is *flattened* (fields like `title`, `price`, etc.), rather than wrapping a `Product` object.

---

# Module: `common/`

### `common/src/main/java/com/mydomain/ws/common/ProductCreatedEvent.java`
- **What it is**
  - A Kafka event payload DTO representing a “product created” event.
- **Fields**
  - `productId`, `title`, `price`, `quantity`
- **How it’s used**
  - Produced by `product-service` and consumed by the test consumer in `ProductServiceIntegrationTest`.
- **Notes**
  - Uses Lombok (`@Getter`, `@Setter`, `@ToString`, `@NoArgsConstructor`, `@AllArgsConstructor`).

### `common/src/main/resources/application.yaml`
- Sets the Spring app name to `common-service`.

---

# Module: `product-service/` (Kafka Producer)

### `product-service/src/main/java/com/mydomain/ws/products/ProductServiceApplication.java`
- Spring Boot entrypoint (`@SpringBootApplication`).

### `product-service/src/main/java/com/mydomain/ws/products/config/KafkaConfig.java`
- Kafka producer and topic configuration.
- Declares:
  - `ProducerFactory<String, ProductCreatedEvent>`
  - `KafkaTemplate<String, ProductCreatedEvent>`
  - Kafka topic `product-created-events-topic` (3 partitions, 3 replicas, `min.insync.replicas=2`).
- Reads producer properties from `application.yaml` (bootstrap servers, serializers, idempotence options).

### `product-service/src/main/java/com/mydomain/ws/products/controller/ProductController.java`
- REST controller.
- Endpoint:
  - `POST /products`
- Accepts:
  - `CreateProductRestModel` (title/price/quantity).
- Flow:
  - calls `productService.createProduct(product)`
  - returns HTTP 201 with `productId` or HTTP 500 with `ErrorMessage`.

### `product-service/src/main/java/com/mydomain/ws/products/exception/ErrorMessage.java`
- Error response DTO: `timestamp`, `message`, `details`.

### `product-service/src/main/java/com/mydomain/ws/products/model/CreateProductRestModel.java`
- Request DTO for `POST /products`.
- Fields: `title`, `price`, `quantity`.

### `product-service/src/main/java/com/mydomain/ws/products/service/ProductService.java`
- Service interface.
- Method:
  - `createProduct(CreateProductRestModel productRestModel)`

### `product-service/src/main/java/com/mydomain/ws/products/service/impl/ProductServiceImpl.java`
- Main implementation (`@Primary`).
- Responsibilities:
  - generates a UUID `productId`
  - builds a `ProductCreatedEvent(productId, title, price, quantity)`
  - publishes it to Kafka topic `product-created-events-topic`
  - attaches Kafka header `messageId` (UUID)
  - uses `kafkaTemplate.send(...).get()` to publish **synchronously**
  - logs the result metadata: partition, topic, offset.

### `product-service/src/main/resources/application.yaml`
- Runs on port `8081`.
- Kafka producer configuration:
  - `bootstrap-servers: localhost:9092,localhost:9094`
  - serializers: String key, Jackson JSON value
  - reliability: `acks=all`, `retries=10`, `enable.idempotence=true`, `max.in.flight=5`

---

# Tests

### `product-service/src/test/java/com/mydomain/ws/products/notification/common/ProductServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

### `product-service/src/test/resources/application-test.properties`
- Test-only Kafka consumer properties used by the integration test:
  - `spring.kafka.consumer.group-id=product-created-group`
  - `spring.kafka.consumer.auto.offset.reset=earliest`
  - `product-created-events-topic-name=product-created-events-topic`

### `product-service/src/test/java/com/mydomain/ws/products/notification/common/ProductServiceIntegrationTest.java`
- **What it is**
  - End-to-end integration test that verifies producing a Kafka message.
- **Kafka setup**
  - `@EmbeddedKafka(partitions=3, topics={"product-created-events-topic"})`
  - Overrides producer bootstrap servers to embedded broker:
    - `spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}`
- **Consumer inside the test**
  - Creates a `KafkaMessageListenerContainer` with:
    - `ErrorHandlingDeserializer` + `JacksonJsonDeserializer`
    - `group-id` from `application-test.properties`
    - `auto.offset.reset=earliest`
- **Test scenario**
  - Calls `productService.createProduct(...)`.
  - Polls a queue for the produced Kafka record.
  - Asserts the Kafka message is present and that `quantity/title/price` match.

---

## End-to-end flow (this app)

1. `POST /products` -> `ProductController`
2. `ProductServiceImpl` creates `ProductCreatedEvent` and publishes to `product-created-events-topic`
3. Integration test runs an embedded Kafka broker and a test consumer.
4. Test consumer reads the message and asserts correctness.
