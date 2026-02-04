# Annotations Reference (All 5 Projects)

This document lists **all Java annotations used across the 5 projects** in this repository and explains what each is used for.

Scope:

- Source scanned: `**/*.java`
- Excluded: `target/`, `.idea/`, `.mvn/`, `kafka-volumes/`

---

# 1) Spring Boot / Spring Core

### `@SpringBootApplication`
- **Category**: Spring Boot
- **Used for**: Marks the main Spring Boot application class.
- **Effect**: Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.

### `@Configuration`
- **Category**: Spring Core
- **Used for**: Marks a class as a source of Spring bean definitions.

### `@Bean`
- **Category**: Spring Core
- **Used for**: Declares a Spring-managed bean from a `@Configuration` method.

### `@Component`
- **Category**: Spring Core
- **Used for**: Marks a class for component scanning (auto-detected and registered as a bean).

### `@Service`
- **Category**: Spring Core
- **Used for**: Specialization of `@Component` used for service-layer classes.

### `@Repository`
- **Category**: Spring Core / Data
- **Used for**: Specialization of `@Component` used for persistence-layer classes.
- **Extra behavior**: Enables Spring exception translation for persistence exceptions.

### `@Autowired`
- **Category**: Spring Core
- **Used for**: Dependency injection (field/constructor/setter injection).

### `@Value`
- **Category**: Spring Core
- **Used for**: Injects property values into fields/parameters.
- **Example use**: Topic names, bootstrap servers, ports, etc.

### `@Primary`
- **Category**: Spring Core
- **Used for**: Marks a bean as the default choice when multiple candidates exist for injection.

### `@Transactional`
- **Category**: Spring Transactions
- **Used for**: Wraps method/class execution in a transaction.
- **In this repo**: Used for both JPA transaction demarcation and/or Kafka-related transactional flows (depending on which transaction manager is referenced).

---

# 2) Spring Web (REST)

### `@RestController`
- **Category**: Spring Web
- **Used for**: Marks a class as a REST controller.
- **Effect**: Equivalent to `@Controller` + `@ResponseBody`.

### `@RequestMapping`
- **Category**: Spring Web
- **Used for**: Defines a base URL path (and optionally HTTP method/headers) for a controller or method.

### `@GetMapping`
- **Category**: Spring Web
- **Used for**: Maps HTTP GET requests to a controller method.

### `@PostMapping`
- **Category**: Spring Web
- **Used for**: Maps HTTP POST requests to a controller method.

### `@RequestBody`
- **Category**: Spring Web
- **Used for**: Binds the HTTP request body to a Java object.

### `@PathVariable`
- **Category**: Spring Web
- **Used for**: Extracts a variable part of the URL path.

### `@ResponseStatus`
- **Category**: Spring Web
- **Used for**: Sets the HTTP status code returned by a handler method.

---

# 3) Spring Kafka

### `@KafkaListener`
- **Category**: Spring Kafka
- **Used for**: Declares a Kafka message listener on one or more topics.
- **In this repo**: Used for event handlers and command handlers; sometimes with a custom `containerFactory`.

### `@KafkaHandler`
- **Category**: Spring Kafka
- **Used for**: Method-level handler inside a class annotated with `@KafkaListener`.
- **Why**: Enables multiple handler methods selected by payload type (useful for saga orchestration).

### `@Payload`
- **Category**: Spring Messaging / Kafka
- **Used for**: Binds the Kafka message payload to a method parameter.

### `@Header`
- **Category**: Spring Messaging / Kafka
- **Used for**: Binds a Kafka record header (or well-known header like `KafkaHeaders.RECEIVED_KEY`) to a method parameter.
- **In this repo**: Used to read `messageId` for idempotency.

---

# 4) Spring Data JPA / Jakarta Persistence (ORM)

### `@Entity`
- **Category**: Jakarta Persistence (JPA)
- **Used for**: Marks a class as a JPA entity mapped to a database table.

### `@Table`
- **Category**: Jakarta Persistence (JPA)
- **Used for**: Configures the table name (and other table-level metadata) for an entity.

### `@Id`
- **Category**: Jakarta Persistence (JPA)
- **Used for**: Marks the primary key field of an entity.

### `@GeneratedValue`
- **Category**: Jakarta Persistence (JPA)
- **Used for**: Indicates the primary key value is generated automatically.
- **In this repo**: Used for numeric IDs and UUID-based IDs depending on the entity.

### `@Column`
- **Category**: Jakarta Persistence (JPA)
- **Used for**: Configures column-level mapping (name, nullability, uniqueness, etc.).

---

# 5) Jakarta Validation (Bean Validation)

### `@Valid`
- **Category**: Jakarta Validation
- **Used for**: Triggers validation of a nested object or request body.
- **In this repo**: Used on REST request DTOs.

### `@NotNull`
- **Category**: Jakarta Validation
- **Used for**: Requires a field/parameter value to be non-null.

### `@NotBlank`
- **Category**: Jakarta Validation
- **Used for**: Requires a `String` to be non-null and contain at least one non-whitespace character.

### `@Positive`
- **Category**: Jakarta Validation
- **Used for**: Requires a numeric value to be > 0.

### `@Nonnull`
- **Category**: Jakarta / Nullness
- **Used for**: Indicates the value should not be null.
- **Note**: This is a nullness annotation (often used for documentation and/or tooling) and is distinct from Bean Validation constraints like `@NotNull`.

---

# 6) Lombok (Code Generation)

These annotations generate boilerplate at compile time.

### `@Getter`
- **Category**: Lombok
- **Used for**: Generates getters for fields.

### `@Setter`
- **Category**: Lombok
- **Used for**: Generates setters for fields.

### `@ToString`
- **Category**: Lombok
- **Used for**: Generates a `toString()` implementation.

### `@Data`
- **Category**: Lombok
- **Used for**: Generates getters/setters, `toString()`, `equals()`, `hashCode()`, and a required-args constructor.

### `@NoArgsConstructor`
- **Category**: Lombok
- **Used for**: Generates a no-argument constructor.

### `@AllArgsConstructor`
- **Category**: Lombok
- **Used for**: Generates a constructor with all fields as parameters.

### `@RequiredArgsConstructor`
- **Category**: Lombok
- **Used for**: Generates a constructor for `final` fields (and fields marked `@NonNull`).
- **In this repo**: Often used with constructor injection.

### `@Slf4j`
- **Category**: Lombok
- **Used for**: Injects an SLF4J `log` field.

---

# 7) Testing (JUnit 5 + Spring Test + Spring Kafka Test)

### `@SpringBootTest`
- **Category**: Spring Test
- **Used for**: Loads full Spring application context for tests.

### `@Test`
- **Category**: JUnit 5
- **Used for**: Marks a test method.

### `@BeforeAll`
- **Category**: JUnit 5
- **Used for**: Runs once before all tests in a class.

### `@AfterAll`
- **Category**: JUnit 5
- **Used for**: Runs once after all tests in a class.

### `@TestInstance`
- **Category**: JUnit 5
- **Used for**: Controls test instance lifecycle (e.g., per-class).

### `@ActiveProfiles`
- **Category**: Spring Test
- **Used for**: Activates a Spring profile for the test context (e.g., `test`).

### `@DirtiesContext`
- **Category**: Spring Test
- **Used for**: Marks the Spring context as "dirty" so it gets closed and rebuilt for isolation.

### `@EmbeddedKafka`
- **Category**: Spring Kafka Test
- **Used for**: Starts an embedded Kafka broker for tests.
- **In this repo**: Used to validate Kafka publishing end-to-end.

---

# 8) Java language

### `@Override`
- **Category**: Java
- **Used for**: Indicates a method overrides a superclass method or implements an interface method.

---

## Notes / Tips

- **Kafka listeners**: `@KafkaListener` typically works with a `ConcurrentKafkaListenerContainerFactory` bean. This repo sometimes names it `kafkaListenerContainerFactory`.
- **Idempotency pattern**: combination of `@Header("messageId")` + DB table (processed events) to prevent handling the same Kafka message twice.
- **Transactions**: `@Transactional` can be bound to different transaction managers (JPA vs Kafka) depending on configuration and qualifiers.
