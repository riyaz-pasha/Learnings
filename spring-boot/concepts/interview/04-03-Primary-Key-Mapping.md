# Chapter 3 — Primary Key Mapping in JPA

Primary keys are one of the most important JPA topics because they affect:

* Entity identity
* Persistence Context
* `find()`
* `merge()`
* `equals()` / `hashCode()`
* ID generation
* Database performance
* Composite keys
* Relationships

We'll go from simple IDs to composite IDs.

---

# 1. What is an Entity Identifier?

Every JPA entity needs an identifier.

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Here:

```text
User
 |
 +-- id = 101
```

The identifier uniquely identifies the entity within its entity type.

Conceptually:

```text
Persistence Context

(User, 101) ───────> User object
(User, 102) ───────> User object
(User, 103) ───────> User object
```

This is why `@Id` isn't simply:

> "This column happens to be a database primary key."

It is also part of **JPA entity identity**.

---

# 2. Simple Primary Key

The simplest mapping is:

```java
@Id
private Long id;
```

Possible ID types include:

```java
Long
Integer
UUID
String
```

For example:

```java
@Entity
public class User {

    @Id
    private UUID id;

    private String name;
}
```

However, numeric generated IDs are still extremely common.

---

# 3. Manually Assigned IDs

You don't have to use `@GeneratedValue`.

For example:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Application:

```java
User user = new User();

user.setId(1001L);
user.setName("Alice");

entityManager.persist(user);
```

The application supplies:

```text
id = 1001
```

This can be appropriate when the identifier naturally comes from an external system.

---

# 4. Automatically Generated IDs

More commonly:

```java
@Id
@GeneratedValue
private Long id;
```

Now the persistence/database mechanism generates the identifier.

The four standard JPA strategies are:

```text
AUTO
IDENTITY
SEQUENCE
TABLE
```

Let's understand each properly.

---

# 5. `GenerationType.AUTO`

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

Meaning:

> Let the persistence provider choose the appropriate generation strategy.

Hibernate might choose different mechanisms depending on:

* Database
* Hibernate version
* Configuration
* Identifier type
* Dialect

Therefore:

```java
GenerationType.AUTO
```

doesn't mean:

> Always use auto-increment.

It means:

> Provider, you decide.

---

# 6. `GenerationType.IDENTITY`

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

The database generates the ID as part of the insert.

Conceptually:

```text
Hibernate
   |
   | INSERT
   ↓
Database
   |
   | generates ID
   ↓
101
```

Example:

```sql
INSERT INTO users (name)
VALUES ('Alice');
```

Database generates:

```text
id = 101
```

---

## Why does IDENTITY matter to Hibernate?

Because Hibernate doesn't generally know the generated ID until the database performs the insert.

That has implications for:

* Insert batching
* Flush behavior
* Persistence ordering

This is an excellent interview topic.

### Interview question

> Why can `IDENTITY` interfere with batching?

Because the generated identifier is typically obtained from the database as part of the insert, which can constrain Hibernate's ability to defer/group inserts compared with sequence-based strategies.

Don't oversimplify this into:

> "IDENTITY never supports batching."

The exact behavior depends on Hibernate/database capabilities and version.

---

# 7. `GenerationType.SEQUENCE`

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)
private Long id;
```

This uses a database sequence.

For example:

```text
user_id_seq
```

Conceptually:

```text
Hibernate
    |
    | next sequence value
    ↓
Database sequence
    |
    ↓
101
    |
    ↓
INSERT
```

This is especially relevant to databases such as PostgreSQL and Oracle.

---

# 8. Why SEQUENCE Can Be Better for Hibernate

Consider inserting 1,000 users.

With a sequence, Hibernate can potentially obtain identifiers efficiently and batch inserts.

For example:

```text
Sequence
   ↓
1001
1002
1003
1004
...
```

Hibernate can then organize inserts efficiently.

This is one reason sequence-based generation is often attractive for high-throughput applications on databases that support sequences.

---

# 9. `@SequenceGenerator`

Suppose your database has:

```sql
user_id_seq
```

You can explicitly configure it:

```java
@Id
@GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "user_seq"
)
@SequenceGenerator(
    name = "user_seq",
    sequenceName = "user_id_seq",
    allocationSize = 50
)
private Long id;
```

There are several concepts here.

---

## `generator`

```java
generator = "user_seq"
```

This refers to the JPA generator definition.

---

## `@SequenceGenerator.name`

```java
name = "user_seq"
```

This is the generator's **logical JPA name**.

---

## `sequenceName`

```java
sequenceName = "user_id_seq"
```

This is the actual database sequence name.

So:

```text
JPA
 │
 │ "user_seq"
 ↓
SequenceGenerator
 │
 │ "user_id_seq"
 ↓
Database sequence
```

---

# 10. `allocationSize`

This is an important interview topic.

Example:

```java
@SequenceGenerator(
    name = "user_seq",
    sequenceName = "user_id_seq",
    allocationSize = 50
)
```

`allocationSize` tells the provider how many identifier values it can allocate as a block.

Conceptually:

```text
Database sequence
       |
       | allocate block
       ↓
1001 - 1050
```

Hibernate can then use those identifiers without necessarily asking the database for every individual entity.

This can significantly reduce database round trips.

---

# 11. Why Can `allocationSize` Cause Confusion?

Suppose someone thinks:

```java
allocationSize = 50
```

means:

> The database sequence increments by 50.

Not necessarily in the simplistic sense.

It represents the ORM's allocation strategy, and Hibernate/database sequence configuration need to be compatible.

Modern Hibernate has sophisticated sequence optimizers, so the exact database sequence behavior depends on the configuration and optimizer.

### Interview-safe answer

> `allocationSize` controls how many identifier values the JPA provider allocates at a time for efficiency. With sequence generation, it is related to Hibernate's identifier optimizer and should be configured consistently with the database sequence strategy.

---

# 12. `GenerationType.TABLE`

Example:

```java
@Id
@GeneratedValue(strategy = GenerationType.TABLE)
private Long id;
```

Instead of using a database sequence or identity mechanism, the provider maintains identifier values in a table.

Conceptually:

```text
id_generator
---------------------------
generator_name | next_val
users          | 1001
orders         | 5001
```

Hibernate accesses/updates that table to obtain IDs.

Historically this was useful for databases without native sequence/identity support.

Today, it is generally less attractive than native database mechanisms.

---

# 13. `@TableGenerator`

You can configure the table generator:

```java
@Id
@GeneratedValue(
    strategy = GenerationType.TABLE,
    generator = "user_table_gen"
)
@TableGenerator(
    name = "user_table_gen",
    table = "id_generator",
    pkColumnName = "generator_name",
    valueColumnName = "next_val",
    pkColumnValue = "users",
    allocationSize = 50
)
private Long id;
```

Conceptually:

```text
id_generator
--------------------------------
generator_name | next_val
users          | 1051
orders         | 5021
```

Again, this is relatively uncommon in modern production systems.

---

# 14. Generation Strategy Comparison

| Strategy   | Mechanism                  | Common today? |
| ---------- | -------------------------- | ------------- |
| `AUTO`     | Provider chooses           | Yes           |
| `IDENTITY` | DB identity/auto-increment | Yes           |
| `SEQUENCE` | DB sequence                | Yes           |
| `TABLE`    | Generator table            | Rare          |

A good interview answer isn't:

> "SEQUENCE is always better."

Instead:

> "The appropriate strategy depends on the database and workload. On databases with good sequence support, sequence-based generation can provide better batching and allocation flexibility. IDENTITY is simple and widely used, particularly where the database's identity mechanism is preferred."

---

# 15. Composite Primary Keys

Now things become more interesting.

Suppose we have an order item:

```text
order_id
product_id
quantity
```

The database primary key might be:

```text
PRIMARY KEY(order_id, product_id)
```

So:

```text
OrderItem
----------------------
order_id       PK
product_id     PK
quantity
```

This is a **composite primary key**.

JPA provides two major approaches:

```text
@EmbeddedId
@IdClass
```

---

# 16. `@EmbeddedId`

First create a key class:

```java
@Embeddable
public class OrderItemId {

    private Long orderId;

    private Long productId;

    // equals()
    // hashCode()
}
```

Then:

```java
@Entity
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    private Integer quantity;
}
```

Conceptually:

```text
OrderItem
    |
    +-- id
         |
         +-- orderId
         +-- productId
```

Database:

```text
order_item
--------------------
order_id
product_id
quantity
```

---

# 17. Why `@Embeddable`?

`@Embeddable` means:

> This class doesn't represent its own entity/table. Its fields are embedded into another entity.

Example:

```java
@Embeddable
public class OrderItemId {

    private Long orderId;
    private Long productId;
}
```

There is **no separate `order_item_id` table**.

Its fields become columns of `OrderItem`'s table.

---

# 18. Composite Key Requirements

The ID class should properly implement:

```java
equals()
hashCode()
```

Why?

Because JPA needs to reason about identity and collections/persistence-context behavior.

For example:

```text
OrderItemId(10, 20)
```

must correctly compare equal to another:

```text
OrderItemId(10, 20)
```

when they represent the same identity.

A Java `record` can sometimes be a convenient way to implement an immutable key type, but whether and how you use records depends on the JPA provider/version and mapping requirements. For interview purposes, understand the underlying contract rather than assuming every record is automatically a valid JPA ID mapping.

---

# 19. `@IdClass`

The second approach is:

```java
public class OrderItemId {

    private Long orderId;
    private Long productId;
}
```

Then:

```java
@Entity
@IdClass(OrderItemId.class)
public class OrderItem {

    @Id
    private Long orderId;

    @Id
    private Long productId;

    private Integer quantity;
}
```

Notice the difference.

With `@EmbeddedId`:

```text
Entity
 |
 +-- id
      |
      +-- orderId
      +-- productId
```

With `@IdClass`:

```text
Entity
 |
 +-- orderId
 |
 +-- productId
```

The entity itself contains the ID fields.

---

# 20. `@EmbeddedId` vs `@IdClass`

This is a very common interview question.

### `@EmbeddedId`

```java
@EmbeddedId
private OrderItemId id;
```

The composite key is represented as an object.

### `@IdClass`

```java
@Id
private Long orderId;

@Id
private Long productId;
```

The ID fields are directly part of the entity.

---

## Visual comparison

### EmbeddedId

```text
OrderItem
│
├── OrderItemId
│    ├── orderId
│    └── productId
│
└── quantity
```

### IdClass

```text
OrderItem
│
├── orderId
├── productId
└── quantity
```

---

# 21. Which One Should You Prefer?

There isn't a universal rule saying:

> "`@EmbeddedId` is always correct."

But `@EmbeddedId` often provides a cleaner domain model when the composite identifier is naturally a value object.

Example:

```java
OrderItemId
```

is conceptually a single identity:

```text
(orderId, productId)
```

So:

```java
@EmbeddedId
private OrderItemId id;
```

can express that nicely.

`@IdClass` can be convenient when the individual ID fields need to appear directly on the entity and when compatibility with an existing model/query structure makes that approach more natural.

---

# 22. A More Realistic Example

Consider:

```java
@Embeddable
public class OrderItemId {

    private Long orderId;
    private Long productId;

    // equals + hashCode
}
```

Then:

```java
@Entity
@Table(name = "order_items")
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @Column(nullable = false)
    private Integer quantity;
}
```

Database:

```text
order_items
--------------------------------
order_id       PK
product_id     PK
quantity
--------------------------------
```

The primary key is:

```text
(order_id, product_id)
```

---

# 23. Composite Key + Relationship

This gets more advanced.

Imagine:

```text
Order
  |
  +---- OrderItem
          |
          +---- Product
```

And the `OrderItem` key contains:

```text
orderId
productId
```

In real applications, composite IDs can become significantly more complicated when relationships are part of the key.

This is one reason many modern systems prefer a surrogate primary key:

```text
order_item
-------------------------
id             PK
order_id       FK
product_id     FK
quantity
```

instead of:

```text
order_item
-------------------------
order_id       PK/FK
product_id     PK/FK
quantity
```

This is not a JPA requirement—it's a database/domain design decision.

---

# 24. Surrogate Key vs Natural Key

Another important interview concept.

### Surrogate key

Artificial identifier:

```text
id = 1001
```

Example:

```java
@Id
@GeneratedValue
private Long id;
```

The ID itself has no business meaning.

---

### Natural key

A business attribute identifies the entity.

Example:

```text
country_code = IN
```

or perhaps:

```text
ISBN = 978...
```

You could technically use:

```java
@Id
private String isbn;
```

But business identifiers can change or have complicated semantics.

Therefore, many systems use:

```text
surrogate primary key
+
unique business key
```

For example:

```java
@Id
@GeneratedValue
private Long id;

@Column(nullable = false, unique = true)
private String isbn;
```

This gives:

```text
Primary identity
       ↓
id

Business uniqueness
       ↓
isbn
```

---

# 25. Important Interview Trap — Primary Key vs Unique Key

These are not necessarily the same.

```java
@Id
private Long id;
```

means:

> Entity identifier / primary key.

Whereas:

```java
@Column(unique = true)
private String email;
```

means:

> Email should be unique.

You can have:

```text
id = 101
email = alice@example.com
```

and:

```text
id = 102
email = bob@example.com
```

The primary key identifies the entity.

The unique constraint enforces business/data uniqueness.

---

# 26. ID Generation and `persist()`

Consider:

```java
User user = new User();
user.setName("Alice");

entityManager.persist(user);
```

Before persistence:

```text
user.id = ?
```

After Hibernate obtains/generates the identifier:

```text
user.id = 101
```

But **exactly when the ID becomes available can depend on the generation strategy and flush behavior**.

For example, identity generation often requires the INSERT to obtain the generated ID.

This is why interviewers sometimes ask:

> "Does `persist()` immediately execute INSERT?"

The answer is:

> Not necessarily. JPA works with a persistence context, and SQL execution is commonly deferred until flush/transaction synchronization. However, certain identifier-generation strategies—particularly identity generation—can require an earlier insert to obtain the generated identifier.

That's a much better answer than:

> "`persist()` always executes INSERT."

---

# 27. Interview Questions

### Q1. What is the purpose of `@Id`?

It identifies the entity's primary-key attribute and participates in JPA entity identity.

---

### Q2. What is the difference between `@Id` and `@GeneratedValue`?

```text
@Id
    → identifies the primary-key attribute

@GeneratedValue
    → tells JPA how the value is generated
```

---

### Q3. What is the difference between IDENTITY and SEQUENCE?

```text
IDENTITY
    Database generates ID during INSERT

SEQUENCE
    Database sequence supplies ID
    before/around INSERT
```

Sequence generation can provide more flexibility for batching and ID allocation.

---

### Q4. What is `allocationSize`?

It controls how many identifiers the persistence provider can allocate as a block for sequence/table-based generation, reducing database round trips.

---

### Q5. What is a composite primary key?

A primary key consisting of multiple columns:

```text
PRIMARY KEY(order_id, product_id)
```

---

### Q6. What are the two JPA approaches for composite keys?

```text
@EmbeddedId
@IdClass
```

---

### Q7. `@EmbeddedId` vs `@IdClass`?

```text
@EmbeddedId
    Composite key represented as an embeddable object.

@IdClass
    Composite key fields remain directly on the entity.
```

---

### Q8. Why must composite ID classes implement `equals()` and `hashCode()`?

Because the ID represents entity identity and needs value-based equality semantics for correct identity handling and collection/persistence-context behavior.

---

### Q9. Is `@EmbeddedId` always better?

No.

It often gives a cleaner value-object representation, but `@IdClass` can be more convenient depending on the domain model, existing schema, query requirements, and how the ID fields need to be exposed.

---

### Q10. Is a generated ID always better than a natural ID?

No.

It's a design decision.

A generated surrogate ID is often convenient, but a natural/business key can still be valuable as a unique constraint even when a surrogate ID is used.

---

# ⭐ Chapter 3 Mental Model

Keep this picture in your head:

```text
                     Entity Identity
                           │
                           ▼
                         @Id
                           │
              ┌────────────┴────────────┐
              │                         │
        Single-column ID          Composite ID
              │                         │
              │                  ┌──────┴──────┐
              │                  │             │
              │             @EmbeddedId    @IdClass
              │
       @GeneratedValue
              │
       ┌──────┼──────────────┐
       │      │      │       │
      AUTO IDENTITY SEQUENCE TABLE
                     │
             @SequenceGenerator
```

### The most important interview takeaway

If you're asked to explain ID generation, don't just list the enum values.

Explain the relationship:

```text
@Entity
   ↓
needs identity
   ↓
@Id
   ↓
who supplies the identity?
   ↓
@GeneratedValue
   ↓
IDENTITY / SEQUENCE / TABLE / AUTO
```

And for composite identity:

```text
Multiple columns form identity
            ↓
     @EmbeddedId
        OR
       @IdClass
```

---

**Next chapter:** **Embeddable/value-object mapping** — `@Embeddable`, `@Embedded`, `@AttributeOverride`, `@AttributeOverrides`, nested embeddables, reusable address/value objects, and the important interview distinction between an **Entity and a Value Object**.




---
---
---









## Definitions

| Term | What it is | Example |
|---|---|---|
| **Natural key** | A key made of data that already has business meaning | `email`, `isbn`, `country_code` |
| **Surrogate key** | A system-generated key with no business meaning | `id BIGINT` from SEQUENCE/IDENTITY, UUID |
| **Composite key** | A key made of **two or more columns** (can be natural, surrogate, or a mix) | `(order_id, product_id)` |

Surrogate vs natural is about **where the value comes from**. Composite is about **how many columns** the key has. A key can be both natural and composite.

## Natural key

The application assigns the ID, so there is no `@GeneratedValue`.

```java
@Entity
public class Country {
    @Id
    private String code;   // "IN", "US"
    private String name;
}
```

- **Pros:** meaningful, no extra column, no extra index.
- **Cons:** business values change (emails, phone numbers) and **primary keys should be immutable**. Updating a PK cascades to every foreign key, and JPA does not support changing an entity's ID.
- **Gotcha:** with assigned IDs, `merge()` (and Spring Data's `save()`) cannot tell whether the entity is new, so it issues a **SELECT first**. Fix this with `@Version` or by implementing `Persistable`.

## Surrogate key

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NaturalId                 // Hibernate-specific
    @Column(nullable = false, unique = true)
    private String email;
}
```

- **Pros:** stable and immutable, compact and fast for joins and FKs, and independent of business rule changes.
- **Cons:** an extra column, and it does not enforce business uniqueness by itself. **Always add a unique constraint on the natural key** alongside it.
- **Best practice:** use a surrogate PK plus a unique natural key. Hibernate's `@NaturalId` also lets you look up by the business key (`session.byNaturalId(User.class).using("email", e).load()`) with its own cache support.

## Composite key

JPA gives you two ways to map one. Both require the key class to be `Serializable` with proper `equals()`/`hashCode()`.

**1. `@EmbeddedId`** (the key is a single embedded object):

```java
@Embeddable
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
    // equals() and hashCode() on both fields
}

@Entity
public class OrderItem {
    @EmbeddedId
    private OrderItemId id;

    private int quantity;
}
```

**2. `@IdClass`** (the key fields are declared directly on the entity):

```java
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
    // equals() and hashCode()
}

@Entity
@IdClass(OrderItemId.class)
public class OrderItem {
    @Id private Long orderId;
    @Id private Long productId;
    private int quantity;
}
```

| Aspect | `@EmbeddedId` | `@IdClass` |
|---|---|---|
| Key access | `item.getId().getOrderId()` | `item.getOrderId()` |
| JPQL | `i.id.orderId` | `i.orderId` |
| Key as a reusable object | Yes | Less so (fields are duplicated) |
| Typical use | Cleaner, more common | Legacy schemas, simpler queries |

### Composite key that is also a foreign key (`@MapsId`)

```java
@Embeddable
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
}

@Entity
public class OrderItem {
    @EmbeddedId
    private OrderItemId id;

    @ManyToOne @MapsId("orderId")
    private Order order;

    @ManyToOne @MapsId("productId")
    private Product product;
}
```

`@MapsId` tells JPA that the FK column **is** the corresponding part of the PK, so you don't map the same column twice.

## Gotchas

- **No `@GeneratedValue` on the composite as a whole.** You assign it, or generate one part yourself.
- **Broken `equals()`/`hashCode()`** in the key class causes silent persistence-context bugs, because the key is used as the map key for entity identity.
- **Composite keys propagate.** Every child table's FK needs all the columns, which makes joins and indexes wider.
- **Spring Data JPA:** the repository is `JpaRepository<OrderItem, OrderItemId>`.
- **Pure join tables** (`order_id, product_id` and nothing else) are the classic good fit. Once the link table gets its own attributes or its own children, a surrogate PK is usually easier.

## Which to choose

| Situation | Choice |
|---|---|
| General entities | Surrogate PK, plus a unique constraint (or `@NaturalId`) on the business key |
| Truly immutable reference data (country codes) | Natural key is fine |
| Many-to-many link table, optionally with extra columns | Composite key (`@EmbeddedId` + `@MapsId`), or a surrogate if it grows complex |
| Legacy schema you can't change | Composite key with `@IdClass` or `@EmbeddedId` as the schema dictates |

## Interview one-liner

"A natural key comes from business data, a surrogate key is system-generated with no meaning, and a composite key spans multiple columns. I default to a surrogate PK for stability and join performance, and enforce the natural key with a unique constraint or `@NaturalId`. I use composite keys mainly for link tables or legacy schemas, via `@EmbeddedId` or `@IdClass`, with correct `equals()` and `hashCode()`."

Want to continue with `@NaturalId` caching, or with how `persist()` vs `merge()` behaves for assigned and composite IDs?




## Short answer

- **`equals()`/`hashCode()`** are needed because the persistence context uses the ID as a **hash map key** to guarantee one object per row.
- **`Serializable`** is needed because IDs get **stored, copied, and moved** (caches, session replication, detached entities). It is also simply mandated by the JPA spec.

## Why `equals()`/`hashCode()`

The persistence context (first-level cache) is conceptually a map keyed by **(entity type, ID)**:

```
Map<EntityKey(OrderItem, OrderItemId), OrderItem>
```

Every operation that must answer "do I already have this row loaded?" does a lookup in that map:

- `em.find(OrderItem.class, id)` checks the map before hitting the DB.
- `persist()`/`merge()` check whether an entity with that ID is already managed.
- Loading the same row via a query returns the existing managed instance, not a new one. This is the identity guarantee: **same row, same Java object, within one persistence context**.

For a `Long` or `String` ID, `equals()`/`hashCode()` already work by value. A composite key class is **your own class**, so without overrides it falls back to `Object`'s identity-based versions:

```java
OrderItemId a = new OrderItemId(1L, 10L);
OrderItemId b = new OrderItemId(1L, 10L);
a.equals(b);   // false without an override, even though they represent the same row
```

Consequences of getting this wrong:

| Symptom | Cause |
|---|---|
| `em.find(OrderItem.class, new OrderItemId(1L, 10L))` always hits the DB, or returns a second copy of an already-loaded row | The lookup key never matches the stored key |
| `NonUniqueObjectException` ("a different object with the same identifier value was already associated with the session") | The context can't recognize the two keys as equal, or can't reliably tell them apart |
| Duplicate entries in a `Set`/`Map` of keys or entities | Value-equal keys treated as distinct |
| Cache misses in the second-level cache | The cache key includes the ID |

The rule: **all fields that make up the key must be in both methods**, and nothing else.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderItemId that)) return false;
    return Objects.equals(orderId, that.orderId)
        && Objects.equals(productId, that.productId);
}

@Override
public int hashCode() {
    return Objects.hash(orderId, productId);
}
```

## Why `Serializable`

An ID is not just used in memory inside one session. It can be:

1. **Stored in the second-level cache**, where keys may be serialized (disk, or distributed caches like Infinispan/Hazelcast/Redis).
2. **Replicated across cluster nodes**, for example in HTTP session replication when detached entities live in the session.
3. **Passed across tiers**, such as detached entities sent over RMI or serialized through remoting layers.
4. **Used as a reference by Hibernate internally** (for example, entity keys in `PersistenceContext` serialization, and lazy proxies, which hold the ID and must be serializable themselves).

Hence the JPA spec requires a primary key class to be `Serializable`, public, with a public no-arg constructor, and to define `equals()`/`hashCode()`. Hibernate enforces the Serializable part and fails fast on a composite ID class that isn't.

If you never use second-level caching, clustering, or remoting, you will not see this requirement bite at runtime. It is still required, and declaring it costs nothing.

## Practical checklist for a key class

- `implements Serializable` (add a `serialVersionUID` as good hygiene)
- Public no-arg constructor
- `equals()`/`hashCode()` over exactly the key fields
- Prefer **immutable** fields, or at least never mutate them after the entity is persisted, because changing a field changes the hash code of a key already sitting in a map

## Interview one-liner

"A composite key class needs `equals()`/`hashCode()` because the persistence context is effectively a map keyed by entity type plus ID, and without value-based equality `find()` and identity guarantees break. It needs `Serializable` because IDs end up in caches, replicated sessions, and detached or remoted entities, and the JPA spec mandates it."

Want to go on to `persist()` vs `merge()` with assigned and composite IDs, or `@NaturalId` caching?


