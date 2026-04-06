# BTG Pactual - Investment Fund Platform

## Implementation Plan

### Architecture
- **Spring Boot 4.0.5** + **Java 26** + **MongoDB (NoSQL)**
- **Clean Architecture** with hexagonal ports & adapters
- **Optimistic Locking** + **MongoDB Transactions** for concurrency safety

---

### File Structure to Create

```
src/main/java/com/naithor/thinkuspruebatecnica/
├── ThinkusPruebaTecnicaApplication.java          # Add @EnableRetry, @EnableAsync
├── config/
│   ├── MongoConfig.java                          # MongoDB transaction manager
│   ├── OpenApiConfig.java                        # Swagger/OpenAPI config
│   └── AsyncConfig.java                          # @Async thread pool
├── domain/
│   ├── model/
│   │   ├── Client.java                           # @Document, @Version for optimistic locking
│   │   ├── Fund.java                             # @Document, fund catalog
│   │   ├── Transaction.java                      # @Document, audit trail
│   │   ├── TransactionType.java                  # SUBSCRIBE, CANCEL
│   │   └── NotificationPreference.java           # EMAIL, SMS
│   ├── port/
│   │   ├── in/
│   │   │   ├── SubscribeFundUseCase.java         # interface
│   │   │   ├── CancelSubscriptionUseCase.java    # interface
│   │   │   └── GetTransactionHistoryUseCase.java # interface
│   │   └── out/
│   │       ├── ClientPort.java                   # CRUD + balance operations
│   │       ├── FundPort.java                     # Fund lookup
│   │       ├── TransactionPort.java              # Transaction persistence
│   │       └── NotificationPort.java             # Send notification
│   └── service/
│       ├── SubscribeFundService.java             # Core business logic
│       ├── CancelSubscriptionService.java        # Core business logic
│       └── GetTransactionHistoryService.java     # Query service
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   ├── FundController.java               # REST endpoints
│   │   │   └── ClientController.java             # Client preferences
│   │   ├── dto/
│   │   │   ├── SubscribeRequest.java
│   │   │   ├── TransactionResponse.java
│   │   │   ├── FundResponse.java
│   │   │   └── ErrorResponse.java
│   │   └── mapper/
│   │       └── TransactionMapper.java
│   └── out/
│       ├── repository/
│       │   ├── ClientRepository.java             # MongoRepository
│       │   ├── FundRepository.java               # MongoRepository
│       │   └── TransactionRepository.java        # MongoRepository
│       ├── ClientAdapter.java                    # implements ClientPort
│       ├── FundAdapter.java                      # implements FundPort
│       ├── TransactionAdapter.java               # implements TransactionPort
│       └── notification/
│           ├── EmailNotificationService.java     # implements NotificationPort
│           └── SmsNotificationService.java       # implements NotificationPort
├── exception/
│   ├── InsufficientBalanceException.java
│   ├── FundNotFoundException.java
│   ├── ActiveSubscriptionNotFoundException.java
│   ├── GlobalExceptionHandler.java               # @RestControllerAdvice
│   └── BusinessRuleException.java
└── seeder/
    └── FundSeeder.java                           # CommandLineRunner, seed 5 funds

src/test/java/com/naithor/thinkuspruebatecnica/
├── domain/service/
│   ├── SubscribeFundServiceTest.java
│   ├── CancelSubscriptionServiceTest.java
│   └── SubscribeFundServiceConcurrencyTest.java  # ExecutorService race condition test
├── adapter/in/web/
│   └── FundControllerTest.java                   # @WebMvcTest
└── integration/
    └── FundApiIntegrationTest.java               # @SpringBootTest + embedded Mongo

src/main/resources/
└── application.yml                               # MongoDB, mail, retry config

src/main/resources/sql/
└── part2-solution.sql                            # Parte 2 SQL query

deploy/
└── cloudformation/
    └── btg-pactual-platform.yml                  # AWS CloudFormation template

README.md
```

---

### pom.xml Changes

**Add dependencies:**
- `spring-boot-starter-web`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-mail`
- `spring-boot-starter-validation`
- `spring-retry` + `spring-aspects`
- `lombok`
- `springdoc-openapi-starter-webmvc-ui` (v2.8.9)
- Test: `de.flapdoodle.embed.mongo.spring40x` (v4.21.0)

---

### Domain Models

#### Client.java
```java
@Document(collection = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    private String id;
    
    @Version
    private Long version;
    
    private BigDecimal balance;
    
    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference;
    
    private String contactInfo;
    
    public static Client newDefaultClient() {
        return Client.builder()
            .balance(new BigDecimal("500000"))
            .notificationPreference(NotificationPreference.EMAIL)
            .contactInfo("")
            .build();
    }
    
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
    
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
    
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
```

#### Fund.java
```java
@Document(collection = "funds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund {
    @Id
    private Integer id;
    private String name;
    private BigDecimal minAmount;
    private String category;
}
```

#### Transaction.java
```java
@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    private String id;
    private String clientId;
    private Integer fundId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    private BigDecimal amount;
    private Instant timestamp;
}
```

#### TransactionType.java
```java
public enum TransactionType {
    SUBSCRIBE,
    CANCEL
}
```

#### NotificationPreference.java
```java
public enum NotificationPreference {
    EMAIL,
    SMS
}
```

---

### Use Case Interfaces

#### SubscribeFundUseCase.java
```java
public interface SubscribeFundUseCase {
    Transaction execute(String clientId, Integer fundId);
}
```

#### CancelSubscriptionUseCase.java
```java
public interface CancelSubscriptionUseCase {
    Transaction execute(String clientId, Integer fundId);
}
```

#### GetTransactionHistoryUseCase.java
```java
public interface GetTransactionHistoryUseCase {
    List<Transaction> execute(String clientId);
}
```

#### ClientPort.java
```java
public interface ClientPort {
    Optional<Client> findById(String id);
    Client save(Client client);
}
```

#### FundPort.java
```java
public interface FundPort {
    Optional<Fund> findById(Integer id);
    List<Fund> findAll();
}
```

#### TransactionPort.java
```java
public interface TransactionPort {
    Transaction save(Transaction transaction);
    List<Transaction> findByClientId(String clientId);
    Optional<Transaction> findByClientIdAndFundIdAndType(String clientId, Integer fundId, TransactionType type);
}
```

#### NotificationPort.java
```java
public interface NotificationPort {
    void send(String clientContactInfo, String fundName, String message);
}
```

---

### Core Services (Business Logic)

#### SubscribeFundService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public class SubscribeFundService implements SubscribeFundUseCase {

    private final ClientPort clientPort;
    private final FundPort fundPort;
    private final TransactionPort transactionPort;
    private final NotificationPort notificationPort;

    @Override
    public Transaction execute(String clientId, Integer fundId) {
        Fund fund = fundPort.findById(fundId)
            .orElseThrow(() -> new FundNotFoundException(fundId));

        Client client = clientPort.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (!client.hasSufficientBalance(fund.getMinAmount())) {
            throw new InsufficientBalanceException(fund.getName(), fund.getMinAmount(), client.getBalance());
        }

        client.debit(fund.getMinAmount());
        clientPort.save(client);

        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID().toString())
            .clientId(clientId)
            .fundId(fundId)
            .type(TransactionType.SUBSCRIBE)
            .amount(fund.getMinAmount())
            .timestamp(Instant.now())
            .build();

        transactionPort.save(transaction);

        notificationPort.send(
            client.getContactInfo(),
            fund.getName(),
            "Suscripcion exitosa al fondo " + fund.getName() + " por $" + fund.getMinAmount()
        );

        return transaction;
    }
}
```

#### CancelSubscriptionService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final ClientPort clientPort;
    private final FundPort fundPort;
    private final TransactionPort transactionPort;

    @Override
    public Transaction execute(String clientId, Integer fundId) {
        Fund fund = fundPort.findById(fundId)
            .orElseThrow(() -> new FundNotFoundException(fundId));

        Client client = clientPort.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

        transactionPort.findByClientIdAndFundIdAndType(clientId, fundId, TransactionType.SUBSCRIBE)
            .orElseThrow(() -> new ActiveSubscriptionNotFoundException(fund.getName()));

        client.credit(fund.getMinAmount());
        clientPort.save(client);

        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID().toString())
            .clientId(clientId)
            .fundId(fundId)
            .type(TransactionType.CANCEL)
            .amount(fund.getMinAmount())
            .timestamp(Instant.now())
            .build();

        transactionPort.save(transaction);
        return transaction;
    }
}
```

#### GetTransactionHistoryService.java
```java
@Service
@RequiredArgsConstructor
public class GetTransactionHistoryService implements GetTransactionHistoryUseCase {

    private final TransactionPort transactionPort;

    @Override
    public List<Transaction> execute(String clientId) {
        return transactionPort.findByClientId(clientId);
    }
}
```

---

### Adapters Out (Infrastructure)

#### ClientRepository.java
```java
public interface ClientRepository extends MongoRepository<Client, String> {
}
```

#### ClientAdapter.java
```java
@Repository
@RequiredArgsConstructor
public class ClientAdapter implements ClientPort {
    private final ClientRepository repository;
    
    @Override
    public Optional<Client> findById(String id) {
        return repository.findById(id);
    }
    
    @Override
    public Client save(Client client) {
        return repository.save(client);
    }
}
```

#### FundAdapter.java
```java
@Repository
@RequiredArgsConstructor
public class FundAdapter implements FundPort {
    private final FundRepository repository;
    
    @Override
    public Optional<Fund> findById(Integer id) {
        return repository.findById(id);
    }
    
    @Override
    public List<Fund> findAll() {
        return repository.findAll();
    }
}
```

#### TransactionAdapter.java
```java
@Repository
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionPort {
    private final TransactionRepository repository;
    
    @Override
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }
    
    @Override
    public List<Transaction> findByClientId(String clientId) {
        return repository.findByClientIdOrderByTimestampDesc(clientId);
    }
    
    @Override
    public Optional<Transaction> findByClientIdAndFundIdAndType(String clientId, Integer fundId, TransactionType type) {
        return repository.findFirstByClientIdAndFundIdAndTypeOrderByTimestampDesc(clientId, fundId, type);
    }
}
```

#### EmailNotificationService.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService implements NotificationPort {
    
    @Async
    @Override
    public void send(String contactInfo, String fundName, String message) {
        log.info("[EMAIL] To: {} | Fund: {} | Message: {}", contactInfo, fundName, message);
    }
}
```

#### SmsNotificationService.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService implements NotificationPort {
    
    @Async
    @Override
    public void send(String contactInfo, String fundName, String message) {
        log.info("[SMS] To: {} | Fund: {} | Message: {}", contactInfo, fundName, message);
    }
}
```

#### NotificationDispatcher.java (Strategy Pattern)
```java
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {
    private final Map<NotificationPreference, NotificationPort> strategies;
    
    public void dispatch(Client client, String fundName, String message) {
        NotificationPort notifier = strategies.get(client.getNotificationPreference());
        if (notifier != null) {
            notifier.send(client.getContactInfo(), fundName, message);
        }
    }
}
```

---

### Adapters In (REST API)

#### FundController.java
```java
@RestController
@RequestMapping("/api/v1/funds")
@RequiredArgsConstructor
@Tag(name = "Funds", description = "Investment fund operations")
public class FundController {

    private final SubscribeFundUseCase subscribeFundUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final FundPort fundPort;

    @PostMapping("/{fundId}/subscribe")
    @Operation(summary = "Subscribe to a fund")
    public ResponseEntity<TransactionResponse> subscribe(
            @PathVariable Integer fundId,
            @Valid @RequestBody SubscribeRequest request) {
        Transaction tx = subscribeFundUseCase.execute(request.clientId(), fundId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TransactionResponse.from(tx));
    }

    @DeleteMapping("/{fundId}/subscribe")
    @Operation(summary = "Cancel fund subscription")
    public ResponseEntity<TransactionResponse> cancel(
            @PathVariable Integer fundId,
            @RequestParam String clientId) {
        Transaction tx = cancelSubscriptionUseCase.execute(clientId, fundId);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @GetMapping
    @Operation(summary = "List all funds")
    public ResponseEntity<List<FundResponse>> listFunds() {
        List<FundResponse> funds = fundPort.findAll().stream()
            .map(FundResponse::from)
            .toList();
        return ResponseEntity.ok(funds);
    }
}
```

#### ClientController.java
```java
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client operations")
public class ClientController {

    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final ClientPort clientPort;

    @GetMapping("/{clientId}/transactions")
    @Operation(summary = "Get transaction history")
    public ResponseEntity<List<TransactionResponse>> getHistory(@PathVariable String clientId) {
        List<TransactionResponse> history = getTransactionHistoryUseCase.execute(clientId).stream()
            .map(TransactionResponse::from)
            .toList();
        return ResponseEntity.ok(history);
    }

    @PatchMapping("/{clientId}/preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String clientId,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        Client client = clientPort.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));
        client.setNotificationPreference(request.preference());
        client.setContactInfo(request.contactInfo());
        clientPort.save(client);
        return ResponseEntity.noContent().build();
    }
}
```

---

### Exception Handling

#### GlobalExceptionHandler.java
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(FundNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFundNotFound(FundNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ActiveSubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoActiveSubscription(ActiveSubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Internal server error"));
    }
}
```

---

### DTOs

```java
public record SubscribeRequest(
    @NotBlank String clientId
) {}

public record TransactionResponse(
    String id,
    String clientId,
    Integer fundId,
    TransactionType type,
    BigDecimal amount,
    Instant timestamp
) {
    public static TransactionResponse from(Transaction tx) { ... }
}

public record FundResponse(
    Integer id,
    String name,
    BigDecimal minAmount,
    String category
) {
    public static FundResponse from(Fund fund) { ... }
}

public record ErrorResponse(String message) {}

public record NotificationPreferenceRequest(
    @NotNull NotificationPreference preference,
    @NotBlank String contactInfo
) {}
```

---

### Concurrency Safety Strategy

1. **@Version field** on Client document → MongoDB optimistic locking
2. **@Transactional** on service methods → MongoDB transactions for atomicity
3. **@Retryable** with backoff → Automatic retry on OptimisticLockingFailureException
4. **Balance validation INSIDE transaction** → No TOCTOU race conditions
5. **Test with ExecutorService** → 10 concurrent threads attempting simultaneous subscriptions

---

### application.yml
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/btg_pactual
  mail:
    host: localhost
    port: 1025
  retry:
    max-attempts: 3
    backoff:
      delay: 100

server:
  port: 8080

logging:
  level:
    com.naithor: DEBUG
```

---

### Fund Seeder
```java
@Component
@RequiredArgsConstructor
public class FundSeeder implements CommandLineRunner {
    private final FundRepository repository;
    
    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                Fund.builder().id(1).name("FPV_BTG_PACTUAL_RECAUDADORA").minAmount(new BigDecimal("75000")).category("FPV").build(),
                Fund.builder().id(2).name("FPV_BTG_PACTUAL_ECOPETROL").minAmount(new BigDecimal("125000")).category("FPV").build(),
                Fund.builder().id(3).name("DEUDAPRIVADA").minAmount(new BigDecimal("50000")).category("FIC").build(),
                Fund.builder().id(4).name("FDO-ACCIONES").minAmount(new BigDecimal("250000")).category("FIC").build(),
                Fund.builder().id(5).name("FPV_BTG_PACTUAL_DINAMICA").minAmount(new BigDecimal("100000")).category("FPV").build()
            ));
        }
    }
}
```

---

### Test Strategy

#### Unit Tests
- SubscribeFundServiceTest: mock ports, test happy path, insufficient balance, fund not found
- CancelSubscriptionServiceTest: mock ports, test happy path, no active subscription
- SubscribeFundServiceConcurrencyTest: ExecutorService with 10 threads, verify balance integrity

#### Integration Tests
- FundApiIntegrationTest: @SpringBootTest + embedded MongoDB, full API flow

---

### Parte 2 - SQL Solution

```sql
-- Clients who have enrolled in products available ONLY in branches they visit
SELECT DISTINCT c.nombre
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
    AND d.idSucursal NOT IN (
        SELECT v.idSucursal
        FROM Visitan v
        WHERE v.idCliente = c.id
    )
);
```

**Logic:** A client has a product enrolled. That product is available in certain branches. We exclude clients where the product is available in ANY branch they DON'T visit. The NOT EXISTS + NOT IN pattern ensures the product is ONLY available in branches the client visits.

---

### CloudFormation Template

Resources:
- **VPC** with public/private subnets
- **ECS Cluster + Fargate Service** for the Spring Boot app
- **Application Load Balancer** with target group
- **Amazon DocumentDB** (MongoDB-compatible)
- **Security Groups** for network isolation
- **ECR Repository** for Docker image
- **SSM Parameters** for configuration

---

### Summary of Files to Create/Modify (~30 files)

| # | Action | File |
|---|--------|------|
| 1 | Modify | `pom.xml` |
| 2 | Modify | `ThinkusPruebaTecnicaApplication.java` |
| 3 | Create | `config/MongoConfig.java` |
| 4 | Create | `config/OpenApiConfig.java` |
| 5 | Create | `config/AsyncConfig.java` |
| 6 | Create | `domain/model/Client.java` |
| 7 | Create | `domain/model/Fund.java` |
| 8 | Create | `domain/model/Transaction.java` |
| 9 | Create | `domain/model/TransactionType.java` |
| 10 | Create | `domain/model/NotificationPreference.java` |
| 11 | Create | `domain/port/in/SubscribeFundUseCase.java` |
| 12 | Create | `domain/port/in/CancelSubscriptionUseCase.java` |
| 13 | Create | `domain/port/in/GetTransactionHistoryUseCase.java` |
| 14 | Create | `domain/port/out/ClientPort.java` |
| 15 | Create | `domain/port/out/FundPort.java` |
| 16 | Create | `domain/port/out/TransactionPort.java` |
| 17 | Create | `domain/port/out/NotificationPort.java` |
| 18 | Create | `domain/service/SubscribeFundService.java` |
| 19 | Create | `domain/service/CancelSubscriptionService.java` |
| 20 | Create | `domain/service/GetTransactionHistoryService.java` |
| 21 | Create | `adapter/in/web/FundController.java` |
| 22 | Create | `adapter/in/web/ClientController.java` |
| 23 | Create | `adapter/in/dto/SubscribeRequest.java` |
| 24 | Create | `adapter/in/dto/TransactionResponse.java` |
| 25 | Create | `adapter/in/dto/FundResponse.java` |
| 26 | Create | `adapter/in/dto/ErrorResponse.java` |
| 27 | Create | `adapter/in/dto/NotificationPreferenceRequest.java` |
| 28 | Create | `adapter/out/repository/ClientRepository.java` |
| 29 | Create | `adapter/out/repository/FundRepository.java` |
| 30 | Create | `adapter/out/repository/TransactionRepository.java` |
| 31 | Create | `adapter/out/ClientAdapter.java` |
| 32 | Create | `adapter/out/FundAdapter.java` |
| 33 | Create | `adapter/out/TransactionAdapter.java` |
| 34 | Create | `adapter/out/notification/EmailNotificationService.java` |
| 35 | Create | `adapter/out/notification/SmsNotificationService.java` |
| 36 | Create | `adapter/out/notification/NotificationDispatcher.java` |
| 37 | Create | `exception/InsufficientBalanceException.java` |
| 38 | Create | `exception/FundNotFoundException.java` |
| 39 | Create | `exception/ActiveSubscriptionNotFoundException.java` |
| 40 | Create | `exception/ClientNotFoundException.java` |
| 41 | Create | `exception/BusinessRuleException.java` |
| 42 | Create | `exception/GlobalExceptionHandler.java` |
| 43 | Create | `seeder/FundSeeder.java` |
| 44 | Create | `resources/application.yml` |
| 45 | Create | `test/.../SubscribeFundServiceTest.java` |
| 46 | Create | `test/.../CancelSubscriptionServiceTest.java` |
| 47 | Create | `test/.../SubscribeFundServiceConcurrencyTest.java` |
| 48 | Create | `test/.../FundControllerTest.java` |
| 49 | Create | `test/.../FundApiIntegrationTest.java` |
| 50 | Create | `resources/sql/part2-solution.sql` |
| 51 | Create | `deploy/cloudformation/btg-pactual-platform.yml` |
| 52 | Create | `README.md` |
