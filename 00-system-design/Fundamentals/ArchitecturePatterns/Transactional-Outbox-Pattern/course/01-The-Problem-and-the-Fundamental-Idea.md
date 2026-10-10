# Course: Message Outbox Pattern

Backend Engineering · Distributed Systems · System Design · Interview Preparation

The Transactional Outbox Pattern is a widely used technique for reliably publishing messages from a database-backed application to a message broker such as Kafka, RabbitMQ, or Amazon SQS.

We'll cover the core concepts chapter by chapter, with practical production examples, diagrams, implementation details, failure scenarios, and interview questions. We'll move to the next chapter only when you say `next`.

## Course roadmap

## 01

The problem and the fundamental idea

Dual writes, lost messages, inconsistent state, and how an outbox solves them.

Current chapter

## 02

Database design and implementation

Outbox tables, transactions, example code, and message publishing.

## 03

Reliable delivery in production

Polling, CDC, retries, duplicate messages, and idempotent consumers.

## 04

Failure handling and advanced considerations

Concurrency, ordering, cleanup, monitoring, and scalability.

## 05

System design and interview preparation

End-to-end design, alternatives, trade-offs, and common interview questions.

# Chapter 1 — The Problem and the Fundamental Idea

## 1. Why do we need the Outbox Pattern?

Consider an e-commerce application built using Spring Boot and PostgreSQL. When a customer places an order, the application needs to:

1. Save the order in the database.
2. Publish an `OrderCreated` event to Kafka.
3. Let other services react to that event, such as reserving inventory or initiating fulfillment.

A simplified implementation might look like this:

```
@Transactional
public Order createOrder(CreateOrderRequest request) {
    Order order = orderRepository.save(
        new Order(request.customerId(), request.total())
    );

    kafkaTemplate.send("order-events", new OrderCreated(order.getId()));

    return order;
}
```

At first glance, this seems reasonable. However, the database transaction and the Kafka publish are two separate operations.

A PostgreSQL transaction cannot automatically roll back a Kafka message, and a Kafka failure does not automatically roll back a committed PostgreSQL transaction.

This is called the dual-write problem.

## 2. The two dangerous failure scenarios

### Scenario A: The database commits, but message publishing fails

1\. Save order to PostgreSQL

2\. PostgreSQL commits successfully

3\. Kafka publish fails

Network timeout, broker outage, or connection failure

Final state

The order exists in PostgreSQL, but no event reached Kafka.

What can happen next?

- The customer sees an order confirmation.
- The inventory service never receives the event.
- Inventory is not reserved.
- The order may remain stuck in an invalid business state.

The database is correct in isolation, but the overall business workflow is inconsistent.

### Scenario B: The message publishes, but the database rolls back

Imagine the reverse sequence:

1. The application publishes `OrderCreated` to Kafka.
2. A consumer receives the event and begins processing it.
3. The PostgreSQL transaction fails and rolls back.

Now Kafka contains an event for an order that does not exist in the database.

This is also an inconsistency, and it may be difficult to repair if downstream services have already acted on the event.

Key observation: The problem is not simply handling exceptions. It is the lack of a shared atomic transaction between the database and the message broker.

## 3. Why not use a database transaction?

A normal database transaction guarantees atomicity for operations performed within that database transaction.

For example:

```
BEGIN;

INSERT INTO orders (customer_id, status)
VALUES (101, 'CREATED');

UPDATE inventory
SET reserved_quantity = reserved_quantity + 1
WHERE product_id = 501;

COMMIT;
```

If either database operation fails, the transaction can roll back both changes.

However, publishing a message to Kafka is outside this PostgreSQL transaction.

```
PostgreSQL transaction
    ├── INSERT order
    └── UPDATE inventory
             |
           COMMIT

Kafka publish  ───────── separate operation
```

Annotating a Java method with `@Transactional` does not, by itself, make the database write and Kafka publish atomic.

Distributed transaction protocols, such as two-phase commit, can coordinate certain participating systems, but they bring additional complexity and depend on support from those systems. The outbox pattern addresses the problem without requiring a single atomic transaction across the database and broker.

## 4. The fundamental idea: Store the message in the database first

Instead of immediately publishing the event to Kafka, save the event in an outbox table in the same database transaction as the business data.

A separate process publishes the saved event to the broker afterward.

The flow becomes:

Application

Create order and prepare event

Single PostgreSQL transaction

Insert order into `orders`

Insert event into `outbox_events`

Both commit or both roll back

After commit

Outbox relay

Reads pending events and publishes them

Kafka / RabbitMQ / SQS

Downstream consumers process the event

The important change is that the application no longer has to successfully publish the message before completing its database transaction.

It only needs to persist both the business change and the intention to publish an event.

### What is an outbox table?

Conceptually, an outbox table could look like this:

| event_id  | aggregate_id | event_type     | status      |
| --------- | ------------ | -------------- | ----------- |
| `evt-101` | `ord-501`    | `OrderCreated` | `PENDING`   |
| `evt-102` | `ord-502`    | `OrderCreated` | `PUBLISHED` |

The full records would usually also contain the event payload, creation timestamp, and possibly retry or diagnostic fields.

The outbox is a durable record of messages that the application needs to publish. Its exact schema depends on how the relay and consumers are implemented.

## 5. What guarantee does the pattern actually provide?

This is an important interview distinction.

The outbox pattern gives us atomic persistence of the business change and the event record.

Assuming both writes use the same database transaction:

| Situation                                                               | Result                                                          |
| ----------------------------------------------------------------------- | --------------------------------------------------------------- |
| Order insert succeeds and outbox insert succeeds                        | Both are committed.                                             |
| Order insert fails                                                      | Both are rolled back.                                           |
| Outbox insert fails                                                     | The order transaction is rolled back.                           |
| Database transaction commits, but Kafka is down                         | The event remains persisted in the outbox for later publishing. |
| Kafka accepts a message, but the relay crashes before recording success | The event may be published again after a retry.                 |

Notice the last row. A basic outbox implementation does not guarantee exactly-once delivery to consumers.

In typical designs, it aims for reliable eventual publication, often with at-least-once delivery, provided that the relay retries failures, persisted events are retained, and the broker eventually becomes available.

Therefore, consumers should generally be designed to handle duplicate events safely.

## 6. A production example: Order service and inventory service

Suppose an order service and an inventory service are separate microservices.

```
                    Order Service
                          |
                 ┌────────┴────────┐
                 |                 |
            orders table     outbox_events
                 |                 |
                 └──────┬──────────┘
                        |
                   DB COMMIT
                        |
                   Outbox Relay
                        |
                      Kafka
                        |
                  OrderCreated
                        |
                  Inventory Service
                        |
                Reserve inventory
```

The order service owns its order and outbox records. The inventory service consumes `OrderCreated` and manages its own inventory transaction.

This avoids tightly coupling order creation to Kafka availability. If Kafka is temporarily unavailable, the order service can still persist the order and its event together. The event is published once the relay can reach the broker.

However, the inventory update is not part of the order service's transaction. The overall workflow is still asynchronous, and business processes that span multiple services may need additional techniques, such as idempotency and the saga pattern.

## 7. Outbox pattern vs. directly publishing messages

| Aspect                       | Direct publish                                | Transactional outbox                                  |
| ---------------------------- | --------------------------------------------- | ----------------------------------------------------- |
| Database and event atomicity | Not guaranteed                                | Business write and event record are atomic            |
| Broker outage                | Can cause lost event intent after DB commit   | Event remains in the outbox                           |
| Implementation complexity    | Lower initially                               | Higher; requires an outbox and relay                  |
| Duplicate messages           | Possible, depending on retries                | Possible, especially after uncertain publish outcomes |
| Database storage and cleanup | No outbox storage                             | Outbox storage and retention required                 |
| Best fit                     | Simple workflows with acceptable failure risk | Business-critical database-to-message workflows       |

## 8. Interview-ready explanation

Question: What is the Transactional Outbox Pattern, and why do we need it?

A strong answer:

> The Transactional Outbox Pattern solves the dual-write problem in distributed systems. When an application updates its database and publishes an event to a message broker, these are two independent operations and can fail separately, resulting in inconsistent state.
>
> With the outbox pattern, the application writes both the business data and a corresponding event record to an outbox table within the same database transaction. A separate relay then reads the committed events and publishes them to the message broker.
>
> This guarantees atomic persistence of the business change and the intent to publish an event. Since publishing and marking an event as published are not normally atomic together, duplicate delivery can still occur. Therefore, consumers should be idempotent, and the relay needs retry and failure-handling logic.

### Chapter 1 — Key takeaways

- Dual-write problem: Two independent systems must be updated, but they do not share an atomic transaction.
- Transactional outbox: Write business data and the event record in one database transaction.
- Outbox relay: Publish persisted events to the message broker asynchronously.
- Delivery semantics: Duplicates remain possible; idempotent consumers are important.
- Core guarantee: Atomic database persistence, not atomic end-to-end execution across every microservice.

Next: Chapter 2 — Database design and implementation, including a PostgreSQL outbox schema, a Spring Boot example, transaction boundaries, and how the relay selects pending messages.
