# Codebase Explanation (Format B) — `kafka-saga-pattern/kafka-saga-pattern-spring-boot/`

This document is a per-file walkthrough of the Saga Orchestration demo.

- Build/runtime artifacts are excluded: `target/`, `.idea/`, and the large `kafka-volumes/` folder (Kafka data volumes).

---

## What this app is

This application demonstrates the **Saga Orchestration pattern** using Kafka topics as the communication mechanism between microservices.

### Modules

- **`orders-service/`**
  - Receives REST order requests and publishes `OrderCreatedEvent`.
  - Runs the *orchestrator* (`OrderSaga`) that reacts to events and sends commands.
- **`product-service/`**
  - Manages product inventory.
  - Consumes product commands (reserve/cancel) and emits product events.
- **`payment-service/`**
  - Consumes payment commands and emits payment events.
  - Calls the credit-card processor service.
- **`credit-card-processor-service/`**
  - A REST stub that simulates credit card processing.
- **`common/`**
  - Shared DTOs (commands/events) and shared exceptions/types.

### Kafka topics

Defined in `application.yaml` for each service:

- **Orders**
  - `orders-commands-topic`
  - `orders-events-topic`
- **Products**
  - `products-commands-topic`
  - `products-events-topic`
- **Payments**
  - `payments-commands-topic`
  - `payments-events-topic`

---

# Root project files

### `pom.xml`
- Multi-module Maven aggregator for the saga app.
- Declares modules:
  - `orders-service`, `product-service`, `payment-service`, `credit-card-processor-service`

### `docker-compose.yml`
- Spins up a 3-node Kafka cluster using the `apache/kafka:4.0.0` image.
- Exposes brokers on:
  - `localhost:9091`, `localhost:9092`, `localhost:9093`
- Mounts persistent volumes under `../kafka-volumes/server-*`.

---

# Module: `common/`

### `common/pom.xml`
- Maven module containing shared code (DTOs, commands/events, exceptions, enums).

## DTOs (`common/src/main/java/com/org/common/dto/*`)

### `.../CreditCardProcessRequest.java`
- Payload sent from `payment-service` to `credit-card-processor-service`.
- Validated request: `creditCardNumber`, `paymentAmount`.

### `.../Order.java`
- Shared order DTO: `orderId`, `customerId`, `productId`, `productQuantity`, `status`.

### `.../Product.java`
- Shared product DTO: `id`, `name`, `price`, `quantity`.
- Contains convenience ctor for reservation/cancel flows: `(productId, quantity)`.

### `.../Payment.java`
- Shared payment DTO: `id`, `orderId`, `productId`, `productPrice`, `productQuantity`.
- Contains convenience ctor for processing payment commands.

### `.../Shipment.java`
- Shared shipment DTO (present for completeness; not central to the saga flow in the inspected code).

## Commands (`common/src/main/java/com/org/common/dto/commands/*`)

### `.../ReserveProductCommand.java`
- Command from orchestrator to product-service to reserve stock.

### `.../CancelProductReservationCommand.java`
- Command from orchestrator to product-service to roll back reservation.

### `.../ProcessPaymentCommand.java`
- Command from orchestrator to payment-service to process payment.

### `.../ApproveOrderCommand.java`
- Command from orchestrator to orders-service to mark order approved.

### `.../RejectOrderCommand.java`
- Command from orchestrator to orders-service to mark order rejected.

### `.../ProductReservationCancelledEvent.java`
- Event emitted after product-service cancels reservation (used by orchestrator to reject order).

## Events (`common/src/main/java/com/org/common/dto/event/*`)

### `.../OrderCreatedEvent.java`
- Event emitted by orders-service after creating an order.

### `.../OrderApprovedEvent.java`
- Event emitted by orders-service after approving an order.

### `.../ProductReservedEvent.java`
- Event emitted by product-service after successfully reserving inventory.

### `.../ProductReservationFailedEvent.java`
- Event emitted by product-service when reservation fails (insufficient stock).

### `.../PaymentProcessEvent.java`
- Event emitted by payment-service after payment succeeds.

### `.../PaymentFailedEvent.java`
- Event emitted by payment-service when payment fails (e.g., CCP unavailable).

## Exceptions (`common/src/main/java/com/org/common/exceptions/*`)

### `.../ProductInsufficientQuantityException.java`
- Thrown when product quantity is insufficient.

### `.../CreditCardProcessorUnavailableException.java`
- Thrown when the remote CCP service is unreachable.

## Types (`common/src/main/java/com/org/common/types/*`)

### `.../OrderStatus.java`
- Enum used across services to represent order state.

---

# Module: `orders-service/`

### `orders-service/pom.xml`
- Maven module for orders microservice.

### `orders-service/src/main/java/com/org/orders/OrdersServiceApplication.java`
- Spring Boot entrypoint.

### `orders-service/src/main/java/com/org/orders/web/controller/OrdersController.java`
- REST API:
  - `POST /orders` (accepts `CreateOrderRequest`, returns `CreateOrderResponse`, HTTP 202)
  - `GET /orders/{orderId}/history` (returns `OrderHistoryResponse` list)

### `orders-service/src/main/java/com/org/orders/dto/CreateOrderRequest.java`
- Input DTO for placing an order (validated).

### `orders-service/src/main/java/com/org/orders/dto/CreateOrderResponse.java`
- Output DTO returned after placing an order.

### `orders-service/src/main/java/com/org/orders/dto/OrderHistory.java`
- Internal DTO representing stored order status changes.

### `orders-service/src/main/java/com/org/orders/dto/OrderHistoryResponse.java`
- Output DTO returned by the history endpoint.

### `orders-service/src/main/java/com/org/orders/config/KafkaConfig.java`
- Declares Kafka topics used by orders-service:
  - orders events + orders commands + products commands + payments commands
- Provides a `KafkaTemplate<String,Object>` bean.

### `orders-service/src/main/java/com/org/orders/service/OrderService.java`
- Service interface: place/approve/reject order.

### `orders-service/src/main/java/com/org/orders/service/OrderServiceImpl.java`
- Persists orders using JPA repository.
- Publishes `OrderCreatedEvent` and `OrderApprovedEvent` to `orders-events-topic`.

### `orders-service/src/main/java/com/org/orders/service/OrderHistoryService.java`
- Service interface to record and fetch order history.

### `orders-service/src/main/java/com/org/orders/service/OrderHistoryServiceImpl.java`
- Writes order status transitions to an order-history table.

### `orders-service/src/main/java/com/org/orders/service/handler/OrdersCommandHandler.java`
- Kafka consumer for `orders-commands-topic`.
- Handles:
  - `ApproveOrderCommand` -> `orderService.approveOrder(...)`
  - `RejectOrderCommand` -> `orderService.rejectOrder(...)`

### `orders-service/src/main/java/com/org/orders/saga/OrderSaga.java`
- The **Saga Orchestrator**.
- Listens to multiple *events* topics and reacts by sending *commands*:
  - `OrderCreatedEvent` -> send `ReserveProductCommand`
  - `ProductReservedEvent` -> send `ProcessPaymentCommand`
  - `PaymentProcessEvent` -> send `ApproveOrderCommand`
  - `PaymentFailedEvent` -> send `CancelProductReservationCommand`
  - `ProductReservationCancelledEvent` -> send `RejectOrderCommand`
  - `ProductReservationFailedEvent` -> mark order rejected
- Writes status changes via `OrderHistoryService`.

### `orders-service/src/main/resources/application.yaml`
- Configures:
  - HTTP port `8081`
  - Kafka bootstrap servers `9091/9092/9093`
  - producer reliability settings (acks/idempotence)
  - consumer group and deserializer configuration
  - topic names under `orders/products/payments.*.topic.name`
  - H2 in-memory DB for orders + history.

### `orders-service/src/test/java/com/org/orders/OrdersServiceApplicationTests.java`
- Basic Spring context load test.

---

# Module: `product-service/`

### `product-service/pom.xml`
- Maven module for products microservice.

### `product-service/src/main/java/com/org/products/ProductServiceApplication.java`
- Spring Boot entrypoint.

### `product-service/src/main/java/com/org/products/web/controller/ProductsController.java`
- REST API:
  - `GET /products` -> list all products
  - `POST /products` -> create product

### `product-service/src/main/java/com/org/products/dto/ProductCreationRequest.java`
- Input DTO for product creation.

### `product-service/src/main/java/com/org/products/dto/ProductCreationResponse.java`
- Output DTO returned after product creation.

### `product-service/src/main/java/com/org/products/dao/jpa/entity/ProductEntity.java`
- JPA entity mapped to `products`.

### `product-service/src/main/java/com/org/products/dao/jpa/repository/ProductRepository.java`
- Spring Data repository for product persistence.

### `product-service/src/main/java/com/org/products/service/ProductService.java`
- Service interface: reserve, cancel reservation, save, findAll.

### `product-service/src/main/java/com/org/products/service/ProductServiceImpl.java`
- Implements business logic:
  - `reserve(...)` decreases quantity (throws `ProductInsufficientQuantityException` if not enough stock)
  - `cancelReservation(...)` restores quantity
  - `save(...)` persists new product
  - `findAll(...)` returns DTOs.

### `product-service/src/main/java/com/org/products/service/handler/ProductCommandsHandler.java`
- Kafka consumer on `products-commands-topic`.
- Handles:
  - `ReserveProductCommand` -> tries reservation, emits `ProductReservedEvent` or `ProductReservationFailedEvent`
  - `CancelProductReservationCommand` -> cancels reservation, emits `ProductReservationCancelledEvent`

### `product-service/src/main/java/com/org/products/config/KafkaConfig.java`
- Declares `products-events-topic` and provides a `KafkaTemplate<String,Object>`.

### `product-service/src/main/resources/application.yaml`
- Configures:
  - HTTP port `8082`
  - Kafka cluster + producer/consumer settings
  - disables auto-topic creation
  - topic names under `products.commands.topic.name` and `products.events.topic.name`.

### `product-service/src/test/java/com/org/products/ProductServiceApplicationTests.java`
- Basic context load test.

---

# Module: `payment-service/`

### `payment-service/pom.xml`
- Maven module for payments microservice.

### `payment-service/src/main/java/com/org/payments/PaymentServiceApplication.java`
- Spring Boot entrypoint.

### `payment-service/src/main/java/com/org/payments/config/ApplicationConfig.java`
- Provides a `RestTemplate` bean used for CCP calls.

### `payment-service/src/main/java/com/org/payments/config/KafkaConfig.java`
- Declares `payments-events-topic` and provides a `KafkaTemplate<String,Object>`.

### `payment-service/src/main/java/com/org/payments/dao/jpa/entity/PaymentEntity.java`
- JPA entity mapped to `payments`.

### `payment-service/src/main/java/com/org/payments/dao/jpa/repository/PaymentRepository.java`
- Spring Data repository for payments.

### `payment-service/src/main/java/com/org/payments/service/CreditCardProcessorRemoteService.java`
- Interface for the CCP integration.

### `payment-service/src/main/java/com/org/payments/service/CreditCardProcessorRemoteServiceImpl.java`
- Calls remote CCP endpoint: `${remote.ccp.url}/ccp/process`.
- Converts connection failures into `CreditCardProcessorUnavailableException`.

### `payment-service/src/main/java/com/org/payments/service/PaymentService.java`
- Service interface for payment processing and listing.

### `payment-service/src/main/java/com/org/payments/service/PaymentServiceImpl.java`
- Calculates total price, calls CCP, persists payment record.

### `payment-service/src/main/java/com/org/payments/service/handler/PaymentsCommandsHandler.java`
- Kafka consumer on `payments-commands-topic`.
- Handles `ProcessPaymentCommand`:
  - on success emits `PaymentProcessEvent`
  - on CCP failure emits `PaymentFailedEvent`.

### `payment-service/src/main/resources/application.yaml`
- Configures:
  - HTTP port `8083`
  - Kafka cluster + topic names
  - `remote.ccp.url: http://localhost:8084`
  - payment command/events topic names.

### `payment-service/src/test/java/com/org/payments/PaymentServiceApplicationTests.java`
- Basic context load test.

---

# Module: `credit-card-processor-service/`

### `credit-card-processor-service/pom.xml`
- Maven module for CCP stub.

### `credit-card-processor-service/src/main/java/com/org/creditcardprocessor/CreditCardProcessorServiceApplication.java`
- Spring Boot entrypoint.

### `credit-card-processor-service/src/main/java/com/org/creditcardprocessor/web/controller/CreditCardProcessorController.java`
- REST endpoint:
  - `POST /ccp/process` (HTTP 202)
- Logs the request, simulating an external credit card processor.

### `credit-card-processor-service/src/main/resources/application.yaml`
- Configures HTTP port `8084`.

### `credit-card-processor-service/src/test/java/com/org/creditcardprocessor/CreditCardProcessorServiceApplicationTests.java`
- Basic context load test.

---

## End-to-end saga flow (happy path)

1. Client `POST /orders` -> orders-service saves order + emits `OrderCreatedEvent`.
2. `OrderSaga` sees `OrderCreatedEvent` -> sends `ReserveProductCommand`.
3. product-service reserves inventory -> emits `ProductReservedEvent`.
4. `OrderSaga` sees `ProductReservedEvent` -> sends `ProcessPaymentCommand`.
5. payment-service calls CCP and saves payment -> emits `PaymentProcessEvent`.
6. `OrderSaga` sees `PaymentProcessEvent` -> sends `ApproveOrderCommand`.
7. orders-service approves order -> emits `OrderApprovedEvent`.

## Failure paths

- **Product reservation fails** -> product-service emits `ProductReservationFailedEvent` -> saga marks order rejected.
- **Payment fails (CCP unavailable)** -> payment-service emits `PaymentFailedEvent` -> saga sends `CancelProductReservationCommand` -> product-service emits `ProductReservationCancelledEvent` -> saga sends `RejectOrderCommand`.
