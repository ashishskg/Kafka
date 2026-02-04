# Codebase Explanation (Format B) — `kafka-transaction-spring-boot-api/`

This document is a per-file walkthrough of the `kafka-transaction-spring-boot-api/` application.

- Build artifacts and IDE metadata are excluded from discussion: `target/`, `.idea/`.

---

## What this app is

This app demonstrates a simple "transfer" workflow split into multiple Spring Boot services:

- `transfer-service` (HTTP API + Kafka producer) publishes two events:
  - `WithdrawalRequestedEvent` to `withdraw-money-topic`
  - `DepositRequestedEvent` to `deposit-money-topic`
- `withdrawl-service` consumes withdrawal events.
- `deposit-service` consumes deposit events.
- `mock-service` simulates an external dependency used by `transfer-service` to force success/failure.

Kafka reliability focus:

- The producer is configured with **idempotence** and a **transaction id prefix**.
- The consumers are configured with **`read_committed` isolation**, so they won’t see uncommitted/aborted transactional records.
- Consumers are configured with Spring Kafka error handling + a DLT publishing recoverer.

---

# Module: `common/`

### `common/src/main/java/com/org/common/events/WithdrawalRequestedEvent.java`
- DTO for a withdrawal request.
- Fields: `senderId`, `recepientId`, `amount`.
- Used as the payload published to `withdraw-money-topic`.

### `common/src/main/java/com/org/common/events/DepositRequestedEvent.java`
- DTO for a deposit request.
- Fields: `senderId`, `recepientId`, `amount`.
- Used as the payload published to `deposit-money-topic`.

### `common/src/main/java/com/org/common/error/RetryableException.java`
- Marker exception for retryable Kafka processing errors (used in consumer error handler configuration).

### `common/src/main/java/com/org/common/error/NotRetryableException.java`
- Marker exception for non-retryable Kafka processing errors.

### `common/src/main/resources/application.yaml`
- Sets Spring application name to `common`.

---

# Module: `transfer-service/` (HTTP API + transactional producer)

### `transfer-service/src/main/java/com/org/transfer/TransferServiceApplication.java`
- Spring Boot entrypoint.
- Defines a `RestTemplate` bean used by `TransferServiceImpl`.

### `transfer-service/src/main/java/com/org/transfer/config/KafkaConfig.java`
- Kafka producer + topic config.
- Declares:
  - Producer configs (bootstrap servers, serializers, `acks`, idempotence, max in-flight, timeouts)
  - `ProducerFactory<String, Object>`
  - `KafkaTemplate<String, Object>`
  - `KafkaTransactionManager<String, Object>`
- Creates topics:
  - `withdraw-money-topic`
  - `deposit-money-topic`

### `transfer-service/src/main/java/com/org/transfer/model/TransferRestModel.java`
- REST request payload for the transfer endpoint.
- Fields: `senderId`, `recepientId`, `amount`.

### `transfer-service/src/main/java/com/org/transfer/service/TransferService.java`
- Service interface with method `transfer(TransferRestModel ...)`.

### `transfer-service/src/main/java/com/org/transfer/error/TransferServiceException.java`
- Runtime exception wrapper thrown when the transfer workflow fails.

### `transfer-service/src/main/java/com/org/transfer/controller/TransfersController.java`
- REST controller.
- Endpoint:
  - `POST /transfers`
- Delegates to `TransferService.transfer(...)` and returns a boolean.

### `transfer-service/src/main/java/com/org/transfer/service/TransferServiceImpl.java`
- Implements the transfer workflow.
- Marked `@Transactional` (Spring transaction boundary).
- Flow:
  1. Build `WithdrawalRequestedEvent` and `DepositRequestedEvent` from the request.
  2. `kafkaTemplate.send(withdrawTopic, withdrawalEvent)`
  3. Call `callRemoteServce()` (HTTP call to mock-service at `http://localhost:9504/response/200`).
  4. `kafkaTemplate.send(depositTopic, depositEvent)`
- On any exception:
  - logs and throws `TransferServiceException`.
- Notes on transactions:
  - Producer configuration includes a transaction id prefix. There is also a commented-out `@Transactional(value = "kafkaTransactionManager", ...)` which indicates the intent to run the method in a Kafka transaction manager boundary.

### `transfer-service/src/main/resources/application.yaml`
- Runs on port `9502`.
- Kafka producer is configured with:
  - `transaction-id-prefix: transfer-service-${random.value}-`
  - `enable.idempotence: true`
  - `acks: all`
  - `max.in.flight.requests.per.connection: 5`
- Topic names provided via properties:
  - `withdraw-money-topic`
  - `deposit-money-topic`
- TRACE logging enabled for Spring Kafka transaction components.

### `transfer-service/src/test/java/com/org/transfer/TransferServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

---

# Module: `withdrawl-service/` (Kafka consumer)

### `withdrawl-service/src/main/java/com/org/withdrawl/WithdrawlServiceApplication.java`
- Spring Boot entrypoint.

### `withdrawl-service/src/main/java/com/org/withdrawl/config/KafkaConsumerConfiguration.java`
- Configures:
  - `ConsumerFactory<String, Object>`
    - Value deserialization via `ErrorHandlingDeserializer` delegating to `JacksonJsonDeserializer`
    - `JacksonJsonDeserializer.TRUSTED_PACKAGES = "*"`
    - `isolation.level = read_committed`
  - `ConcurrentKafkaListenerContainerFactory` with:
    - `DeadLetterPublishingRecoverer`
    - `DefaultErrorHandler` + `FixedBackOff(5000, 3)`
    - retryable/non-retryable exception classification using `RetryableException` / `NotRetryableException`
  - `KafkaTemplate` + producer factory to publish failed messages to DLT.

### `withdrawl-service/src/main/java/com/org/withdrawl/handler/WithdrawalRequestedEventHandler.java`
- Kafka listener:
  - `@KafkaListener(topics = "withdraw-money-topic", containerFactory = "kafkaListenerContainerFactory")`
- Handles `WithdrawalRequestedEvent` and logs received amount.

### `withdrawl-service/src/main/resources/application.yaml`
- Runs on port `9503`.
- Kafka consumer config:
  - `bootstrap-servers: localhost:9092`
  - `group-id: amount-withdrawl-event`
  - `isolation-level: read_committed`

### `withdrawl-service/src/test/java/com/org/withdrawl/WithdrawlServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

---

# Module: `deposit-service/` (Kafka consumer)

### `deposit-service/src/main/java/com/org/deposit/DepositServiceApplication.java`
- Spring Boot entrypoint.

### `deposit-service/src/main/java/com/org/deposit/config/KafkaConsumerConfiguration.java`
- Similar structure to `withdrawl-service` consumer configuration:
  - consumer factory uses ErrorHandlingDeserializer + JacksonJsonDeserializer
  - sets `isolation.level = read_committed`
  - configures retry + dead-letter publishing.
- Note: bootstrap server is read from `kafka.consumer.bootstrap-servers` (different key than `application.yaml` uses).

### `deposit-service/src/main/java/com/org/deposit/handler/DepositRequestedEventHandler.java`
- Kafka listener:
  - `@KafkaListener(topics = "deposit-money-topic", containerFactory = "kafkaListenerContainerFactory")`
- Logs the received `DepositRequestedEvent`.

### `deposit-service/src/main/resources/application.yaml`
- Runs on port `9501`.
- Kafka consumer:
  - `bootstrap-servers: localhost:9092`
  - `group-id: amount-deposit-event`
  - `isolation-level: read_committed`

### `deposit-service/src/test/java/com/org/deposit_service/DepositServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

---

# Module: `mock-service/` (HTTP stub)

### `mock-service/src/main/java/com/mydomain/ws/mock/MockServiceApplication.java`
- Spring Boot entrypoint.

### `mock-service/src/main/java/com/mydomain/ws/mock/StatusCheckController.java`
- Endpoints:
  - `GET /response/200` -> HTTP 200
  - `GET /response/500` -> HTTP 500
- Used by `transfer-service` to simulate downstream success/failure.

### `mock-service/src/main/resources/application.yaml`
- Runs on port `9504`.

### `mock-service/src/test/java/com/mydomain/ws/other/OtherServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

---

## End-to-end flow (this app)

1. Client calls `POST /transfers` on `transfer-service`.
2. `transfer-service` publishes `WithdrawalRequestedEvent` to `withdraw-money-topic`.
3. `transfer-service` calls `mock-service` HTTP endpoint.
4. If the mock call succeeds, `transfer-service` publishes `DepositRequestedEvent` to `deposit-money-topic`.
5. `withdrawl-service` consumes withdrawal events; `deposit-service` consumes deposit events.

