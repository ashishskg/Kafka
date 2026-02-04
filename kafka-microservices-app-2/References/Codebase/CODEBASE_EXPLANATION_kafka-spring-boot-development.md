# Codebase Explanation (Format B)

This document captures a per-file walkthrough of the repository.

- Build/runtime artifacts are intentionally excluded from discussion: `target/`, `.idea/`, and `kafka-saga-pattern/kafka-volumes/`.
- This file will be appended as additional applications are documented.

---

# App 1: `kafka-spring-boot-development/`

This app is a small microservices playground:

- `product-service` publishes `ProductCreatedEvent` to Kafka.
- `email-notification-service` consumes that event, calls a mock HTTP endpoint, and uses an H2 table to ensure idempotent processing (don’t process the same Kafka message twice).
- `other-service` exposes simple HTTP endpoints returning `200` or `500` (used to simulate downstream failures).

## Module: `common/`

### `common/src/main/java/com/mydomain/ws/common/Product.java`
- A DTO representing a product: `productId`, `title`, `price`, `quantity`.
- Used inside Kafka messages (`ProductCreatedEvent` wraps it).
- Lombok generates boilerplate getters/setters.

### `common/src/main/java/com/mydomain/ws/common/ProductCreatedEvent.java`
- Kafka event payload class.
- Contains a single field: `product`.
- Producer serializes it to JSON; consumer deserializes it back to this class.

### `common/src/main/resources/application.yaml`
- Minimal Spring config: sets application name to `common-service`.

## Module: `product-service/` (Kafka Producer)

### `product-service/src/main/java/com/mydomain/ws/products/ProductServiceApplication.java`
- Spring Boot entrypoint (`@SpringBootApplication`).

### `product-service/src/main/java/com/mydomain/ws/products/config/KafkaConfig.java`
- Kafka producer + topic provisioning configuration.
- Reads producer configs from `application.yaml` (bootstrap servers, serializers, idempotence settings).
- Declares beans:
  - `ProducerFactory<String, ProductCreatedEvent>`
  - `KafkaTemplate<String, ProductCreatedEvent>`
- Creates topic `product-created-events-topic` with:
  - `partitions=3`, `replicas=3`, `min.insync.replicas=2`

### `product-service/src/main/java/com/mydomain/ws/products/controller/ProductController.java`
- REST controller for creating products.
- Endpoint: `POST /products`
- Calls `productService.createProductSynchronously(product)`.
- On error returns `ErrorMessage` with HTTP 500; on success returns HTTP 201 with `productId`.

### `product-service/src/main/java/com/mydomain/ws/products/exception/ErrorMessage.java`
- Error response DTO: `timestamp`, `message`, `details`.

### `product-service/src/main/java/com/mydomain/ws/products/service/ProductService.java`
- Service interface:
  - `createProduct(Product product)` (async style)
  - `createProductSynchronously(Product product)` (waits for Kafka ack)

### `product-service/src/main/java/com/mydomain/ws/products/service/impl/ProductServiceImpl.java`
- Primary (`@Primary`) implementation of `ProductService`.
- Creates `productId` and builds a `ProductCreatedEvent`.
- Uses a `ProducerRecord` so it can attach Kafka headers:
  - Adds `messageId` header (UUID) used by the consumer for idempotency.
- `createProduct(...)` sends asynchronously using `CompletableFuture`.
- `createProductSynchronously(...)` blocks on `kafkaTemplate.send(...).get()`.

### `product-service/src/main/java/com/mydomain/ws/products/service/impl/ProductServiceImplWithoutProducerRecord.java`
- Alternate implementation (not primary).
- Sends with `kafkaTemplate.send(topic, key, value)`.
- Does not add the `messageId` header.

### `product-service/src/main/resources/application.yaml`
- Runs on port `8081`.
- Kafka producer configuration:
  - `bootstrap-servers: localhost:9092,localhost:9094`
  - serializers: String key, Jackson JSON value
  - reliability: `acks=all`, `retries=10`, `enable.idempotence=true`, `max.in.flight=5`

### `product-service/src/test/java/com/mydomain/ws/products/notification/common/ProductServiceApplicationTests.java`
- Basic `@SpringBootTest` context load test.

## Module: `email-notification-service/` (Kafka Consumer + Idempotency store)

### `email-notification-service/src/main/java/com/mydomain/ws/notification/EmailNotificationServiceApplication.java`
- Spring Boot entrypoint.
- Declares a `RestTemplate` bean.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/H2ConsoleServletConfig.java`
- Registers H2 web console servlet at `/h2-console/*`.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/KafkaConsumerConfiguration.java`
- Creates `ConsumerFactory<String, ProductCreatedEvent>` using `ErrorHandlingDeserializer`.
- Configures `ConcurrentKafkaListenerContainerFactory` with:
  - `DeadLetterPublishingRecoverer`
  - `DefaultErrorHandler` using `FixedBackOff(5000, 3)`
  - Not retryable: `NotRetryableException`, `DeserializationException`
  - Retryable: `RetryableException`
- Defines a producer + `KafkaTemplate` for publishing to DLT.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/ProductCreatedEventHandler.java`
- Kafka consumer:
  - `@KafkaListener(topics = "product-created-events-topic")`
- Requires header: `messageId`.
- Implements idempotency:
  - checks DB for existing `messageId` in `processed_events`; if found, returns.
- Calls downstream stub:
  - `GET http://localhost:8083/response/200`
- Errors:
  - `ResourceAccessException` -> `RetryableException` (retries)
  - `HttpServerErrorException` or generic -> `NotRetryableException` (DLT)
- Saves `(messageId, productId)` in DB after successful handling.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/error/RetryableException.java`
- Marker exception to signal retry.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/error/NotRetryableException.java`
- Marker exception to avoid retry (send to DLT).

### `email-notification-service/src/main/java/com/mydomain/ws/notification/io/ProcessedEventEntity.java`
- JPA entity mapped to table `processed_events`:
  - `messageId` is unique and non-null.
  - `productId` is non-null.

### `email-notification-service/src/main/java/com/mydomain/ws/notification/io/ProcessedEventRepository.java`
- Spring Data `JpaRepository`.
- Adds `findByMessageId` for idempotency checks.

### `email-notification-service/src/main/resources/application.yaml`
- Runs on port `8082`.
- Kafka:
  - `bootstrap-servers: localhost:9092,localhost:9094`
  - consumer `group-id: product-created-events`
- H2 + JPA enabled (`ddl-auto: update`).

### `email-notification-service/src/test/java/com/mydomain/ws/notification/EmailNotificationServiceApplicationTests.java`
- Basic Spring context load test.

## Module: `other-service/` (HTTP stub)

### `other-service/src/main/java/com/mydomain/ws/other/OtherServiceApplication.java`
- Spring Boot entrypoint.

### `other-service/src/main/java/com/mydomain/ws/other/StatusCheckController.java`
- Endpoints:
  - `GET /response/200` -> HTTP 200
  - `GET /response/500` -> HTTP 500
- Used by `email-notification-service` to simulate downstream success/failure.

### `other-service/src/main/resources/application.yaml`
- Runs on port `8083`.

### `other-service/src/test/java/com/mydomain/ws/other/OtherServiceApplicationTests.java`
- Basic Spring context load test.

## End-to-end flow (App 1)

1. Client calls `POST /products` on `product-service`.
2. `ProductServiceImpl` publishes `ProductCreatedEvent` to Kafka topic `product-created-events-topic` with:
   - key = `productId`
   - header `messageId`
3. `email-notification-service` consumes the event.
4. It checks `processed_events` by `messageId` to ensure idempotency.
5. Calls `other-service` endpoint.
6. Saves the processed message in H2.
