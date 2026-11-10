# Chapter 9 — Complete Implementation Using Java, Spring Boot and PostgreSQL

Now let's turn everything we've learned into an actual implementation.

We'll stop discussing isolated concepts and build one working architecture incrementally.

Our example will be an Order Checkout Saga Orchestrator.

We'll use:

- Java 21
- Spring Boot
- PostgreSQL
- Spring JDBC / `JdbcTemplate`
- Multiple background workers
- REST APIs for participant communication
- Transactional Outbox for reliable event publishing

We'll deliberately start without Kafka, Redis or other infrastructure. We can introduce those when the basic implementation requires them.

The goal is to build the smallest correct architecture first, then evolve it.

## 1. What exactly are we building?

Our workflow:

```
ORDER_CHECKOUT

1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. CREATE_SHIPMENT
```

Dependencies:

```
CREATE_ORDER
     |
     v
RESERVE_INVENTORY
     |
     v
CHARGE_PAYMENT
     |
     v
CREATE_SHIPMENT
```

Compensations:

```
CREATE_SHIPMENT
     |
     X
     |
     v
REFUND_PAYMENT
     |
     v
RELEASE_INVENTORY
     |
     v
CANCEL_ORDER
```

Suppose the shipment creation fails permanently.

Our orchestrator should automatically execute the required compensations.

The final state should be:

```
SAGA = COMPENSATED

ORDER = CANCELLED
INVENTORY = RELEASED
PAYMENT = REFUNDED
SHIPMENT = NOT_CREATED
```

Every operation must survive application restarts, worker crashes, duplicate HTTP requests and participant failures.

## 2. Project structure

Let's start with a modular structure.

We don't need to create multiple microservices for the orchestrator itself.

One Spring Boot application can contain all these modules.

```
saga-orchestrator/
│
├── api/
│   ├── SagaController.java
│   └── dto/
│       ├── StartSagaRequest.java
│       └── SagaResponse.java
│
├── definition/
│   ├── SagaDefinition.java
│   ├── SagaStepDefinition.java
│   └── WorkflowRegistry.java
│
├── domain/
│   ├── SagaStatus.java
│   ├── ActionStatus.java
│   ├── ActionType.java
│   └── ParticipantResult.java
│
├── application/
│   ├── SagaApplicationService.java
│   ├── SagaExecutionService.java
│   ├── CompensationService.java
│   └── FailureDecisionService.java
│
├── persistence/
│   ├── SagaRepository.java
│   ├── SagaStepRepository.java
│   ├── SagaActionRepository.java
│   ├── AttemptRepository.java
│   └── OutboxRepository.java
│
├── worker/
│   ├── SagaWorker.java
│   ├── ActionClaimer.java
│   ├── LeaseManager.java
│   └── WorkerConfiguration.java
│
├── participant/
│   ├── ParticipantClient.java
│   ├── PaymentClient.java
│   ├── InventoryClient.java
│   └── ShippingClient.java
│
├── scheduler/
│   ├── RetryScheduler.java
│   └── LeaseRecoveryScheduler.java
│
└── config/
    ├── DatabaseConfiguration.java
    └── HttpClientConfiguration.java
```

The separation is intentional.

For example:

- `SagaApplicationService` accepts and creates workflows.
- `SagaWorker` executes actions.
- `ActionClaimer` manages ownership.
- `ParticipantClient` communicates with remote services.
- `FailureDecisionService` determines whether to retry, reconcile or fail.
- `CompensationService` creates and schedules recovery actions.

You can combine classes in a smaller application, but keeping responsibilities separate makes correctness easier to reason about.

# 3. Define the state enums first

Before creating the tables or workers, define the states that our code recognizes.

### SagaStatus.java

```
public enum SagaStatus {

    RUNNING,

    COMPENSATING,

    SUCCEEDED,

    COMPENSATED,

    FAILED,

    MANUAL_INTERVENTION_REQUIRED
}
```

### ActionStatus.java

```
public enum ActionStatus {

    BLOCKED,

    READY,

    RUNNING,

    RETRY_WAIT,

    RECONCILIATION_WAIT,

    WAITING_FOR_RESULT,

    SUCCEEDED,

    FAILED,

    MANUAL_INTERVENTION_REQUIRED
}
```

### ActionType.java

```
public enum ActionType {

    FORWARD,

    COMPENSATE
}
```

Why separate `ActionType` and `ActionStatus`?

Because they answer different questions.

```
ActionType:
    What kind of operation is this?

ActionStatus:
    What is the current execution state?
```

For example:

```
ActionType = FORWARD
ActionStatus = SUCCEEDED
```

Or:

```
ActionType = COMPENSATE
ActionStatus = RETRY_WAIT
```

A refund can be waiting for retry even though it is a compensation action.

This separation will keep the implementation clean.

# 4. First, design the actual PostgreSQL schema

We'll create the database schema based on the architecture from the previous chapters.

One small refinement from our earlier conceptual models:

We'll separate three concepts:

| Table          | Represents                                                |
| -------------- | --------------------------------------------------------- |
| `sagas`        | One business workflow instance                            |
| `saga_steps`   | The immutable step definitions instantiated for that saga |
| `saga_actions` | Actual executable forward and compensation commands       |

An attempt is not another action. It is one execution attempt of an action.

Let's implement these tables.

## 4.1 `sagas`

```
CREATE TABLE sagas (

    id UUID PRIMARY KEY,

    tenant_id VARCHAR(100) NOT NULL,

    saga_type VARCHAR(100) NOT NULL,

    definition_version INTEGER NOT NULL,

    business_reference VARCHAR(255),

    status VARCHAR(50) NOT NULL,

    input_payload JSONB NOT NULL,

    context JSONB NOT NULL DEFAULT '{}'::jsonb,

    failure_code VARCHAR(100),

    failure_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_saga_status CHECK (
        status IN (
            'RUNNING',
            'COMPENSATING',
            'SUCCEEDED',
            'COMPENSATED',
            'FAILED',
            'MANUAL_INTERVENTION_REQUIRED'
        )
    )
);
```

Important fields:

- `id`: Unique saga execution ID.
- `tenant_id`: Tenant ownership and isolation.
- `saga_type`: Which workflow definition to use.
- `definition_version`: Which immutable workflow version this saga uses.
- `business_reference`: Associated business entity.
- `status`: Overall workflow state.
- `input_payload`: Original request data.
- `context`: Data accumulated during execution.

We will not use `current_step` as our authoritative progress indicator.

The individual actions and their dependencies determine what can execute next.

## 4.2 `saga_steps`

```
CREATE TABLE saga_steps (

    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    step_key VARCHAR(150) NOT NULL,

    step_order INTEGER NOT NULL,

    participant_key VARCHAR(100) NOT NULL,

    forward_operation VARCHAR(150) NOT NULL,

    forward_request_template JSONB NOT NULL,

    compensation_operation VARCHAR(150),

    compensation_request_template JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (saga_id, step_key),

    UNIQUE (saga_id, step_order)
);
```

This table represents the workflow definition as instantiated for a particular saga.

Example:

| Step              | Participant | Forward operation | Compensation     |
| ----------------- | ----------- | ----------------- | ---------------- |
| CREATE_ORDER      | ORDER       | createOrder       | cancelOrder      |
| RESERVE_INVENTORY | INVENTORY   | reserveInventory  | releaseInventory |
| CHARGE_PAYMENT    | PAYMENT     | chargePayment     | refundPayment    |
| CREATE_SHIPMENT   | SHIPPING    | createShipment    | cancelShipment   |

Why store these definitions?

Imagine we deploy a new version of the workflow tomorrow.

An existing saga should not accidentally change its operations because the application code was updated.

Persisting the instantiated step definitions helps preserve the original execution contract.

For longer-running workflows, the operation handlers themselves must also remain compatible with in-flight definition versions.

## 4.3 `saga_actions`

This is the table our workers will primarily interact with.

```
CREATE TABLE saga_actions (

    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    step_id UUID NOT NULL REFERENCES saga_steps(id),

    action_key VARCHAR(200) NOT NULL,

    action_type VARCHAR(30) NOT NULL,

    participant_key VARCHAR(100) NOT NULL,

    operation_key VARCHAR(150) NOT NULL,

    idempotency_key VARCHAR(255) NOT NULL,

    request_payload JSONB NOT NULL,

    response_payload JSONB,

    status VARCHAR(50) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMPTZ,

    first_attempt_at TIMESTAMPTZ,

    last_attempt_at TIMESTAMPTZ,

    action_deadline_at TIMESTAMPTZ,

    lease_owner VARCHAR(255),

    lease_until TIMESTAMPTZ,

    claim_generation BIGINT NOT NULL DEFAULT 0,

    last_error_code VARCHAR(100),

    last_error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (saga_id, action_key),

    UNIQUE (idempotency_key),

    CONSTRAINT chk_action_type CHECK (
        action_type IN ('FORWARD', 'COMPENSATE')
    ),

    CONSTRAINT chk_action_status CHECK (
        status IN (
            'BLOCKED',
            'READY',
            'RUNNING',
            'RETRY_WAIT',
            'RECONCILIATION_WAIT',
            'WAITING_FOR_RESULT',
            'SUCCEEDED',
            'FAILED',
            'MANUAL_INTERVENTION_REQUIRED'
        )
    )
);
```

Why is this table central?

Because it represents the actual work that needs to happen.

A forward action:

```
ACTION-1001

action_type = FORWARD
operation_key = chargePayment
status = READY
```

A compensation action:

```
ACTION-1002

action_type = COMPENSATE
operation_key = refundPayment
status = READY
```

Both use exactly the same worker execution infrastructure.

Notice that we persist:

```
participant_key
operation_key
request_payload
idempotency_key
```

The worker does not have to reconstruct everything from the original HTTP request.

It reads a durable action and executes it.

## 4.4 `saga_action_dependencies`

We need to represent dependencies explicitly.

```
CREATE TABLE saga_action_dependencies (

    action_id UUID NOT NULL
        REFERENCES saga_actions(id),

    depends_on_action_id UUID NOT NULL
        REFERENCES saga_actions(id),

    PRIMARY KEY (
        action_id,
        depends_on_action_id
    ),

    CONSTRAINT chk_no_self_dependency CHECK (
        action_id <> depends_on_action_id
    )
);
```

For example:

```
CREATE_ORDER
     |
     v
RESERVE_INVENTORY
     |
     v
CHARGE_PAYMENT
     |
     v
CREATE_SHIPMENT
```

We create dependency records:

```
RESERVE_INVENTORY depends on CREATE_ORDER

CHARGE_PAYMENT depends on RESERVE_INVENTORY

CREATE_SHIPMENT depends on CHARGE_PAYMENT
```

This allows us to support parallel workflows later without changing the fundamental model.

The workflow registry should validate that the dependency graph has no cycles before accepting a workflow definition.

## 4.5 `saga_step_attempts`

```
CREATE TABLE saga_step_attempts (

    id UUID PRIMARY KEY,

    action_id UUID NOT NULL
        REFERENCES saga_actions(id),

    attempt_number INTEGER NOT NULL,

    worker_id VARCHAR(255) NOT NULL,

    claim_generation BIGINT NOT NULL,

    started_at TIMESTAMPTZ NOT NULL,

    completed_at TIMESTAMPTZ,

    outcome VARCHAR(50),

    http_status INTEGER,

    error_code VARCHAR(100),

    error_message TEXT,

    response_payload JSONB,

    UNIQUE (action_id, attempt_number)
);
```

This records the history of execution attempts.

Example:

```
CHARGE_PAYMENT

Attempt 1: TIMEOUT
Attempt 2: HTTP_503
Attempt 3: SUCCESS
```

The action row represents the current state.

The attempt table tells us how that state was reached.

## 4.6 `saga_idempotency`

```
CREATE TABLE saga_idempotency (

    tenant_id VARCHAR(100) NOT NULL,

    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(64) NOT NULL,

    saga_id UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (
        tenant_id,
        idempotency_key
    ),

    CONSTRAINT fk_idempotency_saga
        FOREIGN KEY (saga_id)
        REFERENCES sagas(id)
        DEFERRABLE INITIALLY DEFERRED
);
```

This protects the client-to-orchestrator boundary.

The same client request should not create multiple saga instances.

The deferred foreign key lets us reserve the idempotency identity before inserting the saga, while still guaranteeing that the reference exists when the transaction commits.

## 4.7 `outbox_events`

```
CREATE TABLE outbox_events (

    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    published_at TIMESTAMPTZ,

    publish_attempts INTEGER NOT NULL DEFAULT 0,

    last_error TEXT
);
```

This table will make our event publishing reliable when we introduce a broker.

For now, we can use the database as the execution queue and still create relevant workflow events.

## 4.8 Add the important indexes

```
CREATE INDEX idx_actions_ready
ON saga_actions (created_at, id)
WHERE status = 'READY';
```

```
CREATE INDEX idx_actions_retry
ON saga_actions (next_attempt_at, id)
WHERE status = 'RETRY_WAIT';
```

```
CREATE INDEX idx_actions_expired_lease
ON saga_actions (lease_until, id)
WHERE status = 'RUNNING';
```

```
CREATE INDEX idx_attempts_action
ON saga_step_attempts (action_id, attempt_number);
```

```
CREATE INDEX idx_sagas_business_reference
ON sagas (tenant_id, business_reference);
```

This is our initial schema.

We can add partitioning, archival and specialized scheduling indexes when actual query patterns demand them.

# 5. Define workflows separately from the execution engine

Now that the database is ready, let's define the workflow.

We don't want workers to contain hardcoded logic like:

```
if (stepName.equals("CREATE_ORDER")) {
    orderService.createOrder();
} else if (stepName.equals("RESERVE_INVENTORY")) {
    inventoryService.reserve();
}
```

Imagine we have 50 different Saga types.

We don't want the worker to become one giant collection of business-specific `if/else` conditions.

Instead, introduce a workflow definition.

### SagaStepDefinition.java

```
public record SagaStepDefinition(

    String stepKey,

    int order,

    String participantKey,

    String forwardOperation,

    JsonNode forwardRequestTemplate,

    String compensationOperation,

    JsonNode compensationRequestTemplate,

    Set<String> dependsOn

) {}
```

Each definition tells us:

- What the step is.
- Which participant owns it.
- Which operation to execute.
- What request data is required.
- What compensation operation exists.
- Which steps must succeed first.

### SagaDefinition.java

```
public record SagaDefinition(

    String sagaType,

    int version,

    List<SagaStepDefinition> steps

) {}
```

For example:

```
ORDER_CHECKOUT v1

CREATE_ORDER
    participant = ORDER
    operation = createOrder
    dependsOn = []

RESERVE_INVENTORY
    participant = INVENTORY
    operation = reserveInventory
    dependsOn = [CREATE_ORDER]

CHARGE_PAYMENT
    participant = PAYMENT
    operation = chargePayment
    dependsOn = [RESERVE_INVENTORY]

CREATE_SHIPMENT
    participant = SHIPPING
    operation = createShipment
    dependsOn = [CHARGE_PAYMENT]
```

### WorkflowRegistry.java

The registry provides the appropriate workflow definition.

```
@Component
public class WorkflowRegistry {

    private final Map<String, SagaDefinition> definitions;

    public WorkflowRegistry() {

        this.definitions = Map.of(
            "ORDER_CHECKOUT",
            createOrderCheckoutDefinition()
        );
    }

    public SagaDefinition get(String sagaType) {

        SagaDefinition definition =
            definitions.get(sagaType);

        if (definition == null) {
            throw new IllegalArgumentException(
                "Unsupported saga type: " + sagaType
            );
        }

        return definition;
    }
}
```

For a real implementation, maintain versioned definitions rather than replacing the old definition when a new deployment occurs.

At application startup, validate:

1. Step keys are unique.
2. Dependencies refer to existing steps.
3. There are no dependency cycles.
4. Each required participant operation exists.
5. Compensation configuration is valid.
6. Every operation has a supported payload schema.

An invalid workflow should fail deployment or registration, not fail halfway through a customer's transaction.

### One important design decision

Don't store arbitrary Java code or executable scripts in the database.

Persist symbolic operation identities such as:

```
participant = PAYMENT

operation = chargePayment
```

Your application resolves these through a controlled registry of configured participant clients and handlers.

This makes workflows versionable without allowing arbitrary database content to become executable code.

# 6. Define our Order Checkout workflow

Let's create the actual definition.

For the first implementation, all forward request data can be derived from the initial checkout request.

We'll introduce responses from earlier steps into later request templates when we implement the execution engine.

Example:

```
private SagaDefinition createOrderCheckoutDefinition() {

    SagaStepDefinition createOrder =
        new SagaStepDefinition(
            "CREATE_ORDER",
            1,
            "ORDER",
            "createOrder",
            null,
            "cancelOrder",
            null,
            Set.of()
        );

    SagaStepDefinition reserveInventory =
        new SagaStepDefinition(
            "RESERVE_INVENTORY",
            2,
            "INVENTORY",
            "reserveInventory",
            null,
            "releaseInventory",
            null,
            Set.of("CREATE_ORDER")
        );

    SagaStepDefinition chargePayment =
        new SagaStepDefinition(
            "CHARGE_PAYMENT",
            3,
            "PAYMENT",
            "chargePayment",
            null,
            "refundPayment",
            null,
            Set.of("RESERVE_INVENTORY")
        );

    SagaStepDefinition createShipment =
        new SagaStepDefinition(
            "CREATE_SHIPMENT",
            4,
            "SHIPPING",
            "createShipment",
            null,
            "cancelShipment",
            null,
            Set.of("CHARGE_PAYMENT")
        );

    return new SagaDefinition(
        "ORDER_CHECKOUT",
        1,
        List.of(
            createOrder,
            reserveInventory,
            chargePayment,
            createShipment
        )
    );
}
```

We have left the payload templates as `null` here intentionally: the application will construct them from the incoming checkout request through a request-mapping layer.

For example:

```
public JsonNode buildForwardRequest(
        String stepKey,
        JsonNode input) {

    return switch (stepKey) {

        case "CREATE_ORDER" ->
            jsonObject(
                "orderId", input.get("orderId"),
                "customerId", input.get("customerId")
            );

        case "RESERVE_INVENTORY" ->
            jsonObject(
                "orderId", input.get("orderId"),
                "productId", input.get("productId"),
                "quantity", input.get("quantity")
            );

        case "CHARGE_PAYMENT" ->
            jsonObject(
                "orderId", input.get("orderId"),
                "customerId", input.get("customerId"),
                "amount", input.get("amount"),
                "currency", input.get("currency")
            );

        case "CREATE_SHIPMENT" ->
            jsonObject(
                "orderId", input.get("orderId"),
                "address", input.get("shippingAddress")
            );

        default -> throw new IllegalArgumentException(
            "Unknown step: " + stepKey
        );
    };
}
```

`jsonObject` represents a small helper that constructs an `ObjectNode` from the supplied values.

In a production implementation, I would register one typed request mapper per workflow step rather than put every mapping into one large switch statement.

The important thing is that the generated request data becomes durable when the saga is created.

For operations whose input depends on previous responses, we will use persisted templates that resolve against the saga context during execution.

# 7. Build the Saga creation API

Now let's expose the first REST endpoint.

The caller submits:

```
POST /sagas
Idempotency-Key: checkout-123
Content-Type: application/json
```

Request:

```
{
  "sagaType": "ORDER_CHECKOUT",
  "businessReference": "ORDER-123",
  "input": {
    "orderId": "ORDER-123",
    "customerId": "CUSTOMER-456",
    "productId": "PRODUCT-100",
    "quantity": 2,
    "amount": 2500,
    "currency": "INR",
    "shippingAddress": {
      "city": "Hyderabad",
      "postalCode": "500001"
    }
  }
}
```

### StartSagaRequest.java

```
public record StartSagaRequest(

    String sagaType,

    String businessReference,

    JsonNode input

) {}
```

Notice that we don't accept `tenantId` from the JSON body.

The tenant should come from the authenticated caller's security context.

Otherwise, a caller might submit another tenant's ID.

### SagaResponse.java

```
public record SagaResponse(

    UUID sagaId,

    String status,

    String statusUrl

) {}
```

### SagaController.java

```
@RestController
@RequestMapping("/sagas")
public class SagaController {

    private final SagaApplicationService sagaService;

    public SagaController(
            SagaApplicationService sagaService) {

        this.sagaService = sagaService;
    }

    @PostMapping
    public ResponseEntity<SagaResponse> startSaga(

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestBody
            StartSagaRequest request,

            @AuthenticationPrincipal
            AppPrincipal principal) {

        SagaResponse response =
            sagaService.startSaga(
                principal.tenantId(),
                idempotencyKey,
                request
            );

        return ResponseEntity
            .accepted()
            .location(URI.create(response.statusUrl()))
            .body(response);
    }
}
```

The API does not execute any participant operations.

It only:

1. Validates the request.
2. Creates the durable Saga.
3. Creates its steps and initial actions.
4. Returns the Saga ID.

Our execution engine will take over afterward.

# 8. The most important API transaction: Create everything atomically

Now we reach the first critical piece of implementation.

We must atomically create:

```
Client idempotency record

Saga record

All saga step records

All forward action records

All action dependency records

Initial outbox event
```

If something fails, everything must roll back.

There is also a duplicate-request problem.

Suppose:

```
Request A:

Idempotency-Key = checkout-123

Request B:

Idempotency-Key = checkout-123
```

Both arrive simultaneously.

We need only one saga.

The database will decide which request wins through the unique constraint on `saga_idempotency`.

## 8.1 Calculate a stable request hash

We need to detect this:

```
First request:

checkout-123
amount = 2500
```

```
Second request:

checkout-123
amount = 5000
```

The second request should receive `409 Conflict`.

Create a helper:

```
@Component
public class RequestHasher {

    private final ObjectMapper objectMapper;

    public RequestHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(StartSagaRequest request) {

        try {

            // Canonicalize the JSON before hashing.
            // Object property ordering and equivalent
            // JSON representations must be normalized.

            JsonNode canonical =
                canonicalize(request);

            byte[] bytes =
                objectMapper.writeValueAsBytes(canonical);

            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(bytes);

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                "Unable to hash request",
                ex
            );
        }
    }
}
```

`canonicalize` recursively sorts object properties and normalizes the request representation. It must also define treatment of omitted fields, explicit nulls, numbers and defaults.

This detail matters: hashing arbitrary JSON strings directly would treat different property ordering or whitespace as different requests.

## 8.2 Implement SagaApplicationService

Let's implement the most important part.

The service first reserves the client idempotency key. Only the request that successfully reserves the key is allowed to create the Saga.

A deferred foreign key lets us do this before the Saga row exists, while still ensuring the referenced Saga exists at commit time.

```
@Service
public class SagaApplicationService {

    private final NamedParameterJdbcTemplate jdbc;
    private final WorkflowRegistry workflowRegistry;
    private final RequestHasher requestHasher;
    private final ObjectMapper objectMapper;
    private final StepRequestMapper requestMapper;

    public SagaApplicationService(
            NamedParameterJdbcTemplate jdbc,
            WorkflowRegistry workflowRegistry,
            RequestHasher requestHasher,
            ObjectMapper objectMapper,
            StepRequestMapper requestMapper) {

        this.jdbc = jdbc;
        this.workflowRegistry = workflowRegistry;
        this.requestHasher = requestHasher;
        this.objectMapper = objectMapper;
        this.requestMapper = requestMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SagaResponse startSaga(
            String tenantId,
            String idempotencyKey,
            StartSagaRequest request) {

        validateRequest(tenantId, idempotencyKey, request);

        SagaDefinition definition =
            workflowRegistry.get(request.sagaType());

        String requestHash =
            requestHasher.hash(request);

        UUID proposedSagaId = UUID.randomUUID();

        // Step 1: Atomically reserve the client idempotency key.
        List<UUID> inserted = reserveIdempotencyKey(
            tenantId,
            idempotencyKey,
            requestHash,
            proposedSagaId
        );

        // Step 2: Another request already used this key.
        if (inserted.isEmpty()) {

            ExistingIdempotency existing =
                findExistingIdempotency(
                    tenantId,
                    idempotencyKey
                );

            if (!existing.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                    "Idempotency key was reused with a different request"
                );
            }

            // Return the existing Saga, never create another one.
            return findSagaResponse(existing.sagaId());
        }

        // Step 3: Create the main Saga record.
        insertSaga(
            proposedSagaId,
            tenantId,
            request,
            definition
        );

        // Step 4: Create all step and forward action records.
        createStepsAndActions(
            proposedSagaId,
            request.input(),
            definition
        );

        // Step 5: Record a durable event for notification.
        insertOutboxEvent(
            proposedSagaId,
            "SAGA_ACCEPTED"
        );

        // Step 6: Return only after this transactional method commits.
        return new SagaResponse(
            proposedSagaId,
            SagaStatus.RUNNING.name(),
            "/sagas/" + proposedSagaId
        );
    }
}
```

The annotations and helper methods assume the class is called through Spring's transaction proxy. The transaction commits when the method returns successfully to its caller.

Let's understand each helper.

### Reserve idempotency key

```
private List<UUID> reserveIdempotencyKey(
        String tenantId,
        String key,
        String requestHash,
        UUID sagaId) {

    String sql = """
        INSERT INTO saga_idempotency (
            tenant_id,
            idempotency_key,
            request_hash,
            saga_id
        )
        VALUES (
            :tenantId,
            :key,
            :requestHash,
            :sagaId
        )
        ON CONFLICT (tenant_id, idempotency_key)
        DO NOTHING
        RETURNING saga_id
        """;

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("key", key)
            .addValue("requestHash", requestHash)
            .addValue("sagaId", sagaId);

    return jdbc.query(
        sql,
        params,
        (rs, rowNum) ->
            rs.getObject("saga_id", UUID.class)
    );
}
```

Why does this work?

Imagine two simultaneous requests with the same key.

```
Request A:
    INSERT idempotency key
    SUCCESS

Request B:
    INSERT same key
    WAITS

Request A:
    Creates saga and steps
    COMMIT

Request B:
    ON CONFLICT DO NOTHING
    Returns no inserted row
```

Request B then retrieves the already-existing Saga.

If Request A crashes and rolls back, Request B can successfully insert the key and create the Saga itself.

The database unique constraint coordinates the requests, even when they reach different application instances.

### Find existing idempotency record

```
private ExistingIdempotency findExistingIdempotency(
        String tenantId,
        String key) {

    String sql = """
        SELECT request_hash, saga_id
        FROM saga_idempotency
        WHERE tenant_id = :tenantId
          AND idempotency_key = :key
        """;

    return jdbc.queryForObject(
        sql,
        Map.of(
            "tenantId", tenantId,
            "key", key
        ),
        (rs, rowNum) -> new ExistingIdempotency(
            rs.getString("request_hash"),
            rs.getObject("saga_id", UUID.class)
        )
    );
}

private record ExistingIdempotency(
    String requestHash,
    UUID sagaId
) {}
```

The hash comparison protects against reusing an existing idempotency key with a different business request.

### Insert main Saga

```
private void insertSaga(
        UUID sagaId,
        String tenantId,
        StartSagaRequest request,
        SagaDefinition definition) {

    String sql = """
        INSERT INTO sagas (
            id,
            tenant_id,
            saga_type,
            definition_version,
            business_reference,
            status,
            input_payload
        )
        VALUES (
            :id,
            :tenantId,
            :sagaType,
            :version,
            :businessReference,
            'RUNNING',
            CAST(:input AS jsonb)
        )
        """;

    jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("id", sagaId)
            .addValue("tenantId", tenantId)
            .addValue("sagaType", definition.sagaType())
            .addValue("version", definition.version())
            .addValue("businessReference",
                request.businessReference())
            .addValue("input",
                toJson(request.input()))
    );
}

private String toJson(JsonNode node) {
    try {
        return objectMapper.writeValueAsString(node);
    } catch (JsonProcessingException ex) {
        throw new IllegalStateException(
            "Unable to serialize JSON",
            ex
        );
    }
}
```

Notice that the workflow version comes from the resolved definition, not from an arbitrary client-supplied version.

# 9. Create the steps, forward actions and dependencies

This is where the workflow definition becomes durable execution records.

We need to:

1. Create all step records.
2. Create all forward actions.
3. Give the first eligible action READY status.
4. Keep dependent actions BLOCKED.
5. Persist the dependency relationships.

Let's implement it.

```
private void createStepsAndActions(
        UUID sagaId,
        JsonNode input,
        SagaDefinition definition) {

    Map<String, UUID> stepIds = new HashMap<>();

    Map<String, UUID> actionIds = new HashMap<>();

    // First create all logical steps.
    for (SagaStepDefinition step : definition.steps()) {

        UUID stepId = UUID.randomUUID();

        JsonNode forwardRequest =
            requestMapper.mapForwardRequest(step, input);

        jdbc.update(
            """
            INSERT INTO saga_steps (
                id,
                saga_id,
                step_key,
                step_order,
                participant_key,
                forward_operation,
                forward_request_template,
                compensation_operation,
                compensation_request_template
            )
            VALUES (
                :id,
                :sagaId,
                :stepKey,
                :stepOrder,
                :participant,
                :operation,
                CAST(:request AS jsonb),
                :compensationOperation,
                CAST(:compensationRequest AS jsonb)
            )
            """,
            new MapSqlParameterSource()
                .addValue("id", stepId)
                .addValue("sagaId", sagaId)
                .addValue("stepKey", step.stepKey())
                .addValue("stepOrder", step.order())
                .addValue("participant", step.participantKey())
                .addValue("operation", step.forwardOperation())
                .addValue("request", toJson(forwardRequest))
                .addValue(
                    "compensationOperation",
                    step.compensationOperation()
                )
                .addValue(
                    "compensationRequest",
                    step.compensationRequestTemplate() == null
                        ? null
                        : toJson(step.compensationRequestTemplate())
                )
        );

        stepIds.put(step.stepKey(), stepId);
    }

    // Then create all forward actions.
    for (SagaStepDefinition step : definition.steps()) {

        UUID stepId = stepIds.get(step.stepKey());

        UUID actionId = UUID.randomUUID();

        boolean isInitiallyReady =
            step.dependsOn().isEmpty();

        JsonNode request =
            requestMapper.mapForwardRequest(step, input);

        jdbc.update(
            """
            INSERT INTO saga_actions (
                id,
                saga_id,
                step_id,
                action_key,
                action_type,
                participant_key,
                operation_key,
                idempotency_key,
                request_payload,
                status
            )
            VALUES (
                :id,
                :sagaId,
                :stepId,
                :actionKey,
                'FORWARD',
                :participant,
                :operation,
                :idempotencyKey,
                CAST(:request AS jsonb),
                :status
            )
            """,
            new MapSqlParameterSource()
                .addValue("id", actionId)
                .addValue("sagaId", sagaId)
                .addValue("stepId", stepId)
                .addValue("actionKey",
                    step.stepKey() + ":FORWARD")
                .addValue("participant",
                    step.participantKey())
                .addValue("operation",
                    step.forwardOperation())
                .addValue("idempotencyKey",
                    sagaId + ":" + step.stepKey() + ":FORWARD")
                .addValue("request", toJson(request))
                .addValue(
                    "status",
                    isInitiallyReady ? "READY" : "BLOCKED"
                )
        );

        actionIds.put(step.stepKey(), actionId);
    }

    // Finally persist dependencies.
    for (SagaStepDefinition step : definition.steps()) {

        UUID currentAction =
            actionIds.get(step.stepKey());

        for (String dependency : step.dependsOn()) {

            UUID prerequisiteAction =
                actionIds.get(dependency);

            if (prerequisiteAction == null) {
                throw new IllegalStateException(
                    "Unknown dependency: " + dependency
                );
            }

            jdbc.update(
                """
                INSERT INTO saga_action_dependencies (
                    action_id,
                    depends_on_action_id
                )
                VALUES (
                    :actionId,
                    :dependencyId
                )
                """,
                Map.of(
                    "actionId", currentAction,
                    "dependencyId", prerequisiteAction
                )
            );
        }
    }
}
```

One small optimization: avoid rendering the forward payload twice in production. Build it once, then retain it in a map keyed by step.

The important point is the transaction boundary.

Even though this method performs many INSERT statements, it is called from the transactional `startSaga()` method.

Therefore:

```
BEGIN

Create Saga
Create 4 Steps
Create 4 Forward Actions
Create 3 Dependency Records
Create Outbox Event

COMMIT
```

If the fourth action insertion fails:

```
ROLLBACK EVERYTHING
```

There will never be a partially created workflow.

### Why not create compensation actions immediately?

We could.

But our initial implementation will create them when compensation becomes necessary.

Why?

At saga creation, no business effect has happened yet.

We don't need to schedule refund, inventory release or order cancellation.

When the orchestrator enters COMPENSATING, it will inspect confirmed successful forward actions and create the corresponding compensation actions.

The unique action key will ensure compensation planning cannot accidentally create duplicates.

## 9.1 Implement the request mapper

Our previous service uses:

```
requestMapper.mapForwardRequest(step, input);
```

Let's implement the simple version.

```
@Component
public class StepRequestMapper {

    public JsonNode mapForwardRequest(
            SagaStepDefinition step,
            JsonNode input) {

        ObjectNode result =
            JsonNodeFactory.instance.objectNode();

        switch (step.stepKey()) {

            case "CREATE_ORDER" -> {
                result.set("orderId", input.get("orderId"));
                result.set("customerId", input.get("customerId"));
            }

            case "RESERVE_INVENTORY" -> {
                result.set("orderId", input.get("orderId"));
                result.set("productId", input.get("productId"));
                result.set("quantity", input.get("quantity"));
            }

            case "CHARGE_PAYMENT" -> {
                result.set("orderId", input.get("orderId"));
                result.set("customerId", input.get("customerId"));
                result.set("amount", input.get("amount"));
                result.set("currency", input.get("currency"));
            }

            case "CREATE_SHIPMENT" -> {
                result.set("orderId", input.get("orderId"));
                result.set("address", input.get("shippingAddress"));
            }

            default -> throw new IllegalArgumentException(
                "Unsupported step: " + step.stepKey()
            );
        }

        return result;
    }
}
```

This is a simple demonstration, not the final extensible request-mapping architecture.

In a more mature implementation, use one typed mapper per step.

Also, our current example assumes all forward requests can be derived from the initial input.

For real workflows, a later action might need an identifier returned by an earlier participant.

For example:

```
RESERVE_INVENTORY returns RES-987.

CHARGE_PAYMENT or its compensation might need RES-987.
```

We will handle this using persisted response data and a durable Saga context in the execution implementation.

# 10. What does the database look like after accepting a request?

Let's submit:

```
Idempotency-Key: checkout-123

Saga Type: ORDER_CHECKOUT

Business Reference: ORDER-123
```

Suppose the orchestrator generates:

```
Saga ID = SAGA-1001
```

The resulting records should look like this.

### `sagas`

| id        | saga_type      | status  |
| --------- | -------------- | ------- |
| SAGA-1001 | ORDER_CHECKOUT | RUNNING |

### `saga_steps`

| Step              | Order | Participant |
| ----------------- | ----- | ----------- |
| CREATE_ORDER      | 1     | ORDER       |
| RESERVE_INVENTORY | 2     | INVENTORY   |
| CHARGE_PAYMENT    | 3     | PAYMENT     |
| CREATE_SHIPMENT   | 4     | SHIPPING    |

### `saga_actions`

| Action                    | Type    | Status  |
| ------------------------- | ------- | ------- |
| CREATE_ORDER:FORWARD      | FORWARD | READY   |
| RESERVE_INVENTORY:FORWARD | FORWARD | BLOCKED |
| CHARGE_PAYMENT:FORWARD    | FORWARD | BLOCKED |
| CREATE_SHIPMENT:FORWARD   | FORWARD | BLOCKED |

### `saga_action_dependencies`

```
RESERVE_INVENTORY:FORWARD
    depends on CREATE_ORDER:FORWARD

CHARGE_PAYMENT:FORWARD
    depends on RESERVE_INVENTORY:FORWARD

CREATE_SHIPMENT:FORWARD
    depends on CHARGE_PAYMENT:FORWARD
```

The important thing:

Only CREATE_ORDER is initially eligible for execution.

The other actions exist in the database but cannot be claimed yet.

### `saga_idempotency`

```
tenant_id       = TENANT-1
idempotency_key = checkout-123
request_hash    = HASH-A
saga_id         = SAGA-1001
```

Now, if the client repeats the original request, the orchestrator finds:

```
checkout-123 -> SAGA-1001
```

It does not create another Saga.

# 11. Add a Saga status API

The caller should be able to inspect the execution status.

```
GET /sagas/SAGA-1001
```

Response:

```
{
  "sagaId": "SAGA-1001",
  "sagaType": "ORDER_CHECKOUT",
  "businessReference": "ORDER-123",
  "status": "RUNNING",
  "createdAt": "2026-11-10T10:30:00Z"
}
```

Implementation:

```
@GetMapping("/{sagaId}")
public SagaResponse getSaga(
        @PathVariable UUID sagaId,
        @AuthenticationPrincipal AppPrincipal principal) {

    return sagaService.getSaga(
        principal.tenantId(),
        sagaId
    );
}
```

Repository query:

```
SELECT id, status
FROM sagas
WHERE id = :sagaId
  AND tenant_id = :tenantId;
```

Always scope status lookups by the authenticated tenant.

A saga ID alone should not grant access to another tenant's workflow.

For a duplicate POST request, we return the existing saga identity. The caller can retrieve its current state using the status endpoint.

You might also expose:

```
GET /sagas/{sagaId}/steps
```

Example:

```
{
  "sagaId": "SAGA-1001",
  "steps": [
    {
      "step": "CREATE_ORDER",
      "forwardStatus": "SUCCEEDED"
    },
    {
      "step": "RESERVE_INVENTORY",
      "forwardStatus": "READY"
    },
    {
      "step": "CHARGE_PAYMENT",
      "forwardStatus": "BLOCKED"
    },
    {
      "step": "CREATE_SHIPMENT",
      "forwardStatus": "BLOCKED"
    }
  ]
}
```

The status API reads the authoritative primary if immediate read-after-write consistency is required.

# 12. What should we test before implementing workers?

We have completed the first important milestone: accepting and durably creating sagas.

Before moving to worker execution, test the correctness of this foundation.

I would write these integration tests against a real PostgreSQL instance, preferably using Testcontainers.

### Test 1: Normal Saga creation

```
POST /sagas

Expected:

1 saga record
4 step records
4 forward action records
3 dependency records
1 idempotency record

Only the first action is READY.
```

### Test 2: Duplicate request

Send the same request twice:

```
Request A:
Idempotency-Key = checkout-123

Request B:
Idempotency-Key = checkout-123
```

Expected:

```
Same saga ID returned.

Only one saga created.
Only four forward actions created.
```

### Test 3: Same key, different request

```
First:
amount = 2500

Second:
amount = 5000
```

Expected:

```
409 Conflict
```

No second saga.

### Test 4: Concurrent duplicate requests

Start 20 concurrent requests with the same key.

Expected:

```
Exactly one saga.
Exactly four forward actions.
All successful requests resolve to the same saga ID.
```

This test is particularly important because a single-threaded unit test cannot expose all idempotency race conditions.

### Test 5: Failure during Saga creation

Simulate a database failure while inserting an action or dependency.

Expected:

```
No saga record.
No step records.
No action records.
No idempotency record.
No outbox event.
```

Everything rolls back.

### Test 6: Workflow definition validation

Try registering:

```
A depends on B
B depends on C
C depends on A
```

Expected:

```
Workflow definition rejected due to cyclic dependency.
```

Don't wait until a customer creates this workflow to discover the problem.

# 13. What have we implemented?

We now have the first working layer of our orchestrator.

```
Client
  |
  | POST /sagas
  | Idempotency-Key
  v
Saga Controller
  |
  v
Saga Application Service
  |
  |-- Validate workflow
  |
  |-- Reserve idempotency key
  |
  |-- Create saga
  |
  |-- Create steps
  |
  |-- Create forward actions
  |
  |-- Create dependencies
  |
  |-- Insert outbox event
  |
  v
PostgreSQL COMMIT
  |
  v
202 ACCEPTED
```

Everything required to execute the workflow is now durably stored.

One naming refinement for the SQL: in this initial implementation, the request mapper creates concrete request JSON at saga creation time. Therefore, `forward_request_template` in `saga_steps` would be more accurately named `forward_request_snapshot`. Use the template name only when storing an unresolved template that will be rendered later.

We have not yet implemented the worker. That is intentional. The API must be able to create a complete, recoverable Saga before we introduce execution.

## Chapter 9 — Implementation roadmap

| Part | Implementation                                                                    |
| ---- | --------------------------------------------------------------------------------- |
| 9.1  | Project structure, workflow definitions, schema and Saga creation API — completed |
| 9.2  | Worker engine, atomic claiming, leases and fencing                                |
| 9.3  | Participant HTTP clients and execution-result handling                            |
| 9.4  | Participant-side idempotency implementation                                       |
| 9.5  | Retry scheduler, timeouts and reconciliation                                      |
| 9.6  | Compensation engine                                                               |
| 9.7  | Transactional Outbox and event publishing                                         |
| 9.8  | Integration tests, failure injection and production hardening                     |

# Next: Chapter 9.2 — Implementing the Actual Worker Engine

We'll write the Java code for:

1. `ActionClaimer` — claims READY, due retries and expired leases.
2. Atomic PostgreSQL claiming with `FOR UPDATE SKIP LOCKED`.
3. Creating an attempt record in the same claim transaction.
4. `SagaWorker` — executing the claimed action outside the transaction.
5. Lease heartbeat and fencing-token validation.
6. `ParticipantClient` — routing an action to the correct service.
7. `ActionCompletionService` — atomically persisting success and activating dependent actions.
8. Crash recovery when the participant succeeds but the worker dies before updating PostgreSQL.

We'll trace an actual `SAGA-1001` execution from `CREATE_ORDER = READY` until the next action becomes READY, including the SQL and Java transaction boundaries.

That's where our database design finally becomes an executing orchestration engine.



