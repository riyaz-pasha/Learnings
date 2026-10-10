# Chapter 2 — Database Design and Implementation

In this chapter, we'll implement the Transactional Outbox Pattern using Spring Boot, Spring Data JPA, PostgreSQL, and Kafka.

We'll follow one concrete workflow: when a customer places an order, the application must persist the order and an `OrderCreated` event atomically, then publish that event asynchronously.

## 1. Understand the complete architecture

The implementation has three responsibilities.

1\. Order Service

Saves the order and outbox event in one transaction

2\. PostgreSQL

`orders` + `outbox_events`

Both records commit or roll back together

3\. Outbox Relay

Reads pending events and publishes them to Kafka

Kafka → Inventory Service

The key design decision is that the Order Service never needs Kafka to be available to commit an order and its event record.

## 2. Design the outbox table

First, let's create the tables in PostgreSQL.

### Business table

```
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

This table holds the actual business data.

### Outbox table

```
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,

    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,

    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
```

Let's understand the important columns.

| Column           | Purpose                                                                              |
| ---------------- | ------------------------------------------------------------------------------------ |
| `id`             | Unique identifier for the event; useful for deduplication.                           |
| `aggregate_type` | Identifies the business entity type, such as `Order`.                                |
| `aggregate_id`   | Identifies the specific entity, such as an order UUID.                               |
| `event_type`     | Describes the event, such as `OrderCreated`.                                         |
| `payload`        | Contains the event data serialized as JSON.                                          |
| `created_at`     | Records when the event was created.                                                  |
| `published_at`   | `NULL` means pending; a timestamp means the relay recorded successful publication.   |
| `attempts`       | Tracks publication attempts or failures, depending on how you implement the counter. |
| `last_error`     | Stores diagnostic information from the most recent failure.                          |

Notice that `aggregate_id` is not declared as a foreign key to `orders` in this example. This is a deliberate schema choice, not a requirement of the pattern. If you use foreign keys, retention and cleanup must respect their constraints.

### Why not use a `status` column?

We could instead use a status such as `PENDING` or `PUBLISHED`. Using `published_at IS NULL` as the pending condition is a simple alternative.

A production design might also need states such as `PROCESSING` or `FAILED`, particularly when implementing concurrent relays, leases, or a dead-letter workflow. We'll examine those trade-offs in a later chapter.

### Add an index for polling

The relay repeatedly searches for unpublished messages. Avoid scanning the entire outbox table as it grows.

```
CREATE INDEX idx_outbox_pending
    ON outbox_events (created_at, id)
    WHERE published_at IS NULL;
```

This PostgreSQL partial index covers records that have not yet been marked as published. The exact index should be validated against your polling query and actual workload.

## 3. Define what belongs in the event payload

An `OrderCreated` event might look like this:

```
{
  "eventId": "9bf1d5f9-7c80-4d7c-9e7d-3fbb68774920",
  "eventType": "OrderCreated",
  "orderId": "c4e7d6ac-3bdd-4ce9-9708-bc9cc9157e1a",
  "customerId": "61bc58e3-ef14-4930-82e1-6c21e7bf9350",
  "totalAmount": 1250.00
}
```

The event should contain the information downstream consumers need, while avoiding unnecessary personal or sensitive data.

A useful distinction:

- The outbox row ID identifies the event record.
- The aggregate ID identifies the business entity.
- The event type identifies what happened.
- The payload contains the facts consumers need.

An event ID should remain stable across retries. Do not generate a new event ID each time the relay tries to publish the same row.

## 4. The most important part: Save both records in one transaction

Here is the central business rule:

```
BEGIN TRANSACTION

    INSERT INTO orders (...);

    INSERT INTO outbox_events (...);

COMMIT;
```

If either insert fails, both must roll back.

Let's implement that with Spring Boot.

### Step 1: Represent the event

```
public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount
) {}
```

Records are useful for event payloads because they provide an immutable data carrier.

### Step 2: Map the outbox entity

With Hibernate 6, a JSON field can be mapped using `@JdbcTypeCode(SqlTypes.JSON)`.

```
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            JsonNode payload,
            Instant createdAt
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
```

This is an illustrative entity mapping. Your real entity should map all required columns consistently with the PostgreSQL schema, using migrations as the source of truth.

### Step 3: Define the repository

```
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent>
    findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
```

This derived query retrieves at most 100 unpublished events, oldest first.

The example is sufficient for understanding the design. By itself, however, it is not a concurrency-safe claiming mechanism for multiple relay instances; we'll improve that later.

### Step 4: Write the order and event in one transaction

```
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createOrder(CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Order order = new Order(
                orderId,
                request.customerId(),
                request.totalAmount(),
                "CREATED"
        );

        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                orderId,
                request.customerId(),
                request.totalAmount()
        );

        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                "Order",
                orderId,
                "OrderCreated",
                objectMapper.valueToTree(event),
                Instant.now()
        );

        outboxRepository.save(outboxEvent);

        return orderId;
    }
}
```

Here, `Order`, `OrderRepository`, and `CreateOrderRequest` represent the corresponding business entity, repository, and request DTO in your application.

The central point is not the surrounding boilerplate. It is the transaction boundary.

Both repositories must participate in the same PostgreSQL transaction, using the same appropriate transaction manager and datasource. Under that condition:

- If saving the order fails, the event is not committed.
- If saving the event fails, the order is not committed.
- If the transaction commits, both records are durable according to the database's configured durability guarantees.

Also, note what this method does not do: it does not call Kafka.

### A crucial Spring interview detail

`@Transactional` works through Spring's transaction infrastructure. It does not automatically coordinate arbitrary resources.

For example, simply adding `@Transactional` to a method that writes to PostgreSQL and calls Kafka does not make those two operations atomic.

The outbox works because both database writes participate in the same database transaction.

## 5. Build the outbox relay

We've now saved the order and event together. The next responsibility is to publish the persisted event to Kafka.

For this chapter, we'll use polling: a scheduled task periodically queries the outbox table for pending events.

### Step 1: Enable scheduled tasks

Add `@EnableScheduling` to your Spring Boot application configuration:

```
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
```

### Step 2: Implement the relay

Assume that the Kafka topic is `order-events` and the payload is serialized as JSON.

```
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxStatusService statusService;

    public OutboxRelay(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxStatusService statusService
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.statusService = statusService;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        var events = outboxRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        "order-events",
                        event.getAggregateId().toString(),
                        event.getPayload().toString()
                ).get(10, TimeUnit.SECONDS);

                statusService.markPublished(event.getId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                statusService.recordFailure(
                        event.getId(),
                        e.getMessage()
                );
            }
        }
    }
}
```

The example waits for Kafka's send future to complete before marking the event as published. The broker acknowledgment and durability guarantees still depend on your Kafka configuration.

The relay has four main steps:

1. Find events that have not been marked as published.
2. Publish an event to Kafka.
3. Wait for the send operation to succeed.
4. Record publication success in PostgreSQL.

If publishing fails, the event remains pending and can be retried on a later poll.

The `OutboxStatusService` is deliberately a separate Spring bean. Its transactional methods should run in their own database transactions rather than relying on self-invocation of a transactional method within the same class.

### Step 3: Update outbox status transactionally

The relay should not mark an event as published until the broker send has succeeded.

```
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxStatusService {

    private final OutboxEventRepository repository;

    public OutboxStatusService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = repository.findById(eventId)
                .orElseThrow();

        event.markPublished(Instant.now());
    }

    @Transactional
    public void recordFailure(UUID eventId, String error) {
        OutboxEvent event = repository.findById(eventId)
                .orElseThrow();

        event.recordFailure(error);
    }
}
```

For the `recordFailure` method, add the following to the `OutboxEvent` entity from the previous section:

```
@Column(nullable = false)
private int attempts;

@Column(name = "last_error")
private String lastError;

public void recordFailure(String error) {
    this.attempts++;
    this.lastError = error;
}
```

The entity's existing `markPublished` method updates `publishedAt`. Both status updates occur through managed JPA entities inside their respective transactions, so explicit `save()` calls are not required for these updates.

For brevity, this is a teaching implementation. Production code should also handle unusually long error messages, retry policies, database errors in status recording, scheduled-task overlap, and concurrency between multiple relay instances.

## 6. Walk through a successful execution

Suppose a customer places an order with ID `ORD-101`.

Step 1 — Commit the database transaction

`orders` receives `ORD-101`. `outbox_events` receives `evt-101`. Both rows commit together.

Step 2 — The scheduled relay polls

The relay finds `evt-101` because `published_at` is still `NULL`.

Step 3 — Kafka accepts the event

The relay receives a successful result from Kafka's send operation.

Step 4 — Record the successful publication

The relay updates `published_at`. Later polls skip this event.

At this point, the inventory service can consume `OrderCreated` and reserve inventory. That consumer-side processing is separate from the outbox transaction.

## 7. What happens if the relay crashes?

This is the most important limitation to understand before moving into production reliability.

Consider the following sequence:

```
1. Relay sends evt-101 to Kafka
                  |
2. Kafka accepts evt-101
                  |
3. Relay crashes before updating published_at
                  |
4. Relay restarts
                  |
5. evt-101 still appears pending
                  |
6. Relay publishes evt-101 again
```

The event may be delivered twice.

Why? Because publishing to Kafka and updating the outbox row are separate operations. Even though the relay waits for Kafka's acknowledgment, a crash can happen between receiving that acknowledgment and committing the status update.

This is why the outbox pattern alone does not guarantee exactly-once processing.

There are two complementary approaches:

- Make consumers idempotent so that processing the same event again does not repeat the business effect.
- Use appropriate broker-side capabilities where helpful, while recognizing that Kafka's producer idempotence does not automatically make the database status update atomic with the Kafka publish.

We'll cover these delivery guarantees and consumer-side deduplication in Chapter 3.

## 8. Important implementation rules

| Rule                                                    | Reason                                                                        |
| ------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Insert the event in the business transaction            | Prevent a committed business change from having no durable event record.      |
| Keep broker publishing outside the business transaction | Avoid holding database transactions open while waiting on network operations. |
| Mark published only after successful publication        | Avoid intentionally skipping events after failed sends.                       |
| Retry failed events                                     | Temporary broker or network failures should not permanently lose the event.   |
| Keep event IDs stable                                   | Consumers need a consistent identifier for deduplication.                     |
| Design for concurrent relay instances                   | Two instances can otherwise select and publish the same pending event.        |

One subtle but important point: if the relay cannot record a successful publication because PostgreSQL is temporarily unavailable, Kafka may already contain the message. The relay should not assume that its database status update failed means the broker publish failed.

## 9. Chapter 2 interview questions

Q1. Why do we insert the outbox record in the same transaction as the business record?

To guarantee that the business change and the durable intention to publish its event commit or roll back together.

Q2. Why shouldn't the order service publish directly to Kafka inside its database transaction?

The database transaction cannot normally roll back a Kafka publish. Waiting for Kafka also holds database resources for longer without removing the dual-write failure window.

Q3. Why update `published_at` only after Kafka acknowledges the send?

If we marked the event as published before sending, a failed publish could leave an event permanently skipped by normal polling.

Q4. Does marking an event as published guarantee that consumers processed it?

No. It only indicates that the relay recorded successful publication to the broker. Consumer delivery and business processing happen separately.

Q5. Can we run multiple outbox relays?

Yes, but not safely with the simple read-pending-events query shown here alone. Multiple instances can read and publish the same records concurrently. A production design needs a claiming or coordination strategy, alongside duplicate-safe consumers.

### Chapter 2 summary

You now have the basic implementation of the outbox pattern:

- PostgreSQL stores the business data and event in one transaction.
- A scheduled Spring Boot relay polls pending events.
- Kafka publication happens outside the business transaction.
- The relay records success only after the send succeeds.
- Retries preserve the ability to publish an event, but can create duplicates.

Next: Chapter 3 — Reliable Delivery in Production. We'll compare polling with Change Data Capture (CDC) using Debezium, then cover retries, at-least-once delivery, idempotent consumers, and how to handle duplicate events safely.
