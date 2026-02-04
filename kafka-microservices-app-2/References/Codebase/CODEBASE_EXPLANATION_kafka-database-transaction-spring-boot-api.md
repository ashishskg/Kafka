# Codebase Explanation (Format B) — `kafka-database-transaction-spring-boot-api/`

This document is a per-file walkthrough of the `kafka-database-transaction-spring-boot-api/` application.

- Build artifacts and IDE metadata are excluded from discussion: `target/`, `.idea/`.

---

## What this app is

This app is structurally similar to `kafka-transaction-spring-boot-api/`, but adds a database write inside the transfer workflow.

- `transfer-service` (HTTP API + Kafka producer + JPA/H2)
  - Writes a transfer record to the DB.
  - Publishes withdrawal and deposit events to Kafka.
- `withdrawl-service` consumes withdrawal events.
- `deposit-service` consumes deposit events.
- `mock-service` simulates an external dependency used by `transfer-service`.

Key themes:

- Kafka producer config includes idempotence and a transaction id prefix.
- Consumers use `read_committed` isolation level.
- `transfer-service` uses JPA (H2) and explicitly defines a `JpaTransactionManager` bean named `transactionManager`.

---

# Module: `common/`

### `common/src/main/java/com/org/common/events/WithdrawalRequestedEvent.java`
- DTO for a withdrawal request.
- Fields: `senderId`, `recepientId`, `amount`.

### `common/src/main/java/com/org/common/events/DepositRequestedEvent.java`
- DTO for a deposit request.
- Fields: `senderId`, `recepientId`, `amount`.

### `common/src/main/java/com/org/common/error/RetryableException.java`
- Marker exception type for retryable Kafka processing errors.

### `common/src/main/java/com/org/common/error/NotRetryableException.java`
- Marker exception type for non-retryable Kafka processing errors.

### `common/src/main/resources/application.yaml`
- Sets Spring application name to `common`.

---

# Module: `transfer-service/` (HTTP API + DB write + Kafka producer)

### `transfer-service/src/main/java/com/org/transfer/TransferServiceApplication.java`
- Spring Boot entrypoint.
- Defines a `RestTemplate` bean.

### `transfer-service/src/main/java/com/org/transfer/config/KafkaConfig.java`
- Kafka producer + topic configuration.
- Declares:
  - producer configs (bootstrap servers, serializers, `acks`, idempotence, max in-flight, timeouts)
  - `KafkaTemplate<String, Object>`
  - `KafkaTransactionManager<String, Object>` bean named `kafkaTransactionManager`
- Also declares JPA transaction manager:
  - `@Bean("transactionManager") JpaTransactionManager jpaTransactionManager(EntityManagerFactory emf)`
- Creates topics:
  - `withdraw-money-topic`
  - `deposit-money-topic`

### `transfer-service/src/main/java/com/org/transfer/model/TransferRestModel.java`
- REST request payload:
  - `senderId`, `recepientId`, `amount`.

### `transfer-service/src/main/java/com/org/transfer/service/TransferService.java`
- Service interface with `transfer(TransferRestModel ...)`.

### `transfer-service/src/main/java/com/org/transfer/controller/TransfersController.java`
- REST controller.
- Endpoint:
  - `POST /transfers`
- Delegates to `TransferService.transfer(...)` and returns a boolean.

### `transfer-service/src/main/java/com/org/transfer/error/TransferServiceException.java`
- Runtime exception wrapper thrown when the transfer workflow fails.

### `transfer-service/src/main/java/com/org/transfer/entity/TransferEntity.java`
- JPA entity mapped to table `transfers`.
- Primary key:
  - `transferId` (`@Id`)
- Columns:
  - `senderId`, `recepientId`, `amount`.

### `transfer-service/src/main/java/com/org/transfer/repository/TransferRepository.java`
- Spring Data `JpaRepository` for `TransferEntity`.

### `transfer-service/src/main/java/com/org/transfer/service/TransferServiceImpl.java`
- Implements the transfer workflow.
- Annotated with `@Transactional("transactionManager")` (uses the JPA transaction manager).
- Flow:
  1. Build `WithdrawalRequestedEvent` + `DepositRequestedEvent`.
  2. Build `TransferEntity`, copy request fields, generate `transferId`, and `transferRepository.save(...)`.
  3. Publish withdrawal event to `withdraw-money-topic`.
  4. Call mock service at `http://localhost:9504/response/200`.
  5. Publish deposit event to `deposit-money-topic`.
- On exception:
  - throws `TransferServiceException`.

### `transfer-service/src/main/resources/application.yaml`
- Runs on port `9502`.
- Kafka producer config:
  - idempotence enabled
  - `acks=all`
  - transactional id prefix
- Also configures H2 in-memory database (`jdbc:h2:mem:transferdb...`) and enables H2 console.
- DEBUG logging enabled for Kafka and JPA transaction internals.

### `transfer-service/src/test/java/com/org/transfer/TransferServiceApplicationTests.java`
- Basic Spring `@SpringBootTest` context load test.

---

# Module: `withdrawl-service/` (Kafka consumer)

### `withdrawl-service/src/main/java/com/org/withdrawl/WithdrawlServiceApplication.java`
- Spring Boot entrypoint.

### `withdrawl-service/src/main/java/com/org/withdrawl/config/KafkaConsumerConfiguration.java`
- Kafka consumer container setup:
  - uses `ErrorHandlingDeserializer` + `JacksonJsonDeserializer`
  - sets `isolation.level = read_committed`
  - configures retry + DLT publishing via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.

### `withdrawl-service/src/main/java/com/org/withdrawl/handler/WithdrawalRequestedEventHandler.java`
- Kafka listener on topic `withdraw-money-topic`.
- Logs received withdrawal event amount.

### `withdrawl-service/src/main/resources/application.yaml`
- Runs on port `9503`.
- Kafka consumer:
  - bootstrap servers
  - group id
  - `isolation-level: read_committed`

### `withdrawl-service/src/test/java/com/org/withdrawl/WithdrawlServiceApplicationTests.java`
- Basic context load test.

---

# Module: `deposit-service/` (Kafka consumer)

### `deposit-service/src/main/java/com/org/deposit/DepositServiceApplication.java`
- Spring Boot entrypoint.

### `deposit-service/src/main/java/com/org/deposit/config/KafkaConsumerConfiguration.java`
- Kafka consumer container setup:
  - uses `ErrorHandlingDeserializer` + `JacksonJsonDeserializer`
  - sets `isolation.level = read_committed`
  - configures retry + DLT publishing via `DefaultErrorHandler`.

### `deposit-service/src/main/java/com/org/deposit/handler/DepositRequestedEventHandler.java`
- Kafka listener on topic `deposit-money-topic`.
- Logs received deposit event.

### `deposit-service/src/main/resources/application.yaml`
- Runs on port `9501`.
- Kafka consumer:
  - bootstrap servers
  - group id
  - `isolation-level: read_committed`

### `deposit-service/src/test/java/com/org/deposit_service/DepositServiceApplicationTests.java`
- Basic context load test.

---

# Module: `mock-service/` (HTTP stub)

### `mock-service/src/main/java/com/mydomain/ws/mock/MockServiceApplication.java`
- Spring Boot entrypoint.

### `mock-service/src/main/java/com/mydomain/ws/mock/StatusCheckController.java`
- Endpoints:
  - `GET /response/200` -> HTTP 200
  - `GET /response/500` -> HTTP 500

### `mock-service/src/main/resources/application.yaml`
- Runs on port `9504`.

### `mock-service/src/test/java/com/mydomain/ws/other/OtherServiceApplicationTests.java`
- Basic context load test.

---

## End-to-end flow (this app)

1. Client calls `POST /transfers` on `transfer-service`.
2. `transfer-service` saves a `TransferEntity` into H2.
3. `transfer-service` publishes `WithdrawalRequestedEvent` to `withdraw-money-topic`.
4. Calls mock-service.
5. On success, publishes `DepositRequestedEvent` to `deposit-money-topic`.
6. `withdrawl-service` and `deposit-service` consume the events.
