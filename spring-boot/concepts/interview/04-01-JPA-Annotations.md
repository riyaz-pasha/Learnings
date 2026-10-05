# JPA Annotations — Interview-Focused Course

Absolutely. We’ll do this **chapter by chapter**, one concept at a time. I’ll stop after each chapter, and you say **`next`** when you’re ready.

The goal is not just to memorize annotations, but to understand:

* **What the annotation means**
* **Why it exists**
* **Where it is used**
* **How JPA/Hibernate uses it**
* **Common attributes**
* **Typical production usage**
* **Relationships with other annotations**
* **Common mistakes**
* **Interview questions**
* **What happens internally**

---

# Course Roadmap

We’ll roughly follow this progression:

### Chapter 1 — JPA Fundamentals

* `@Entity`
* `@Table`
* `@Id`
* `@GeneratedValue`
* `@Column`

### Chapter 2 — Basic Field Mapping

* `@Basic`
* `@Transient`
* `@Access`
* `@Enumerated`
* `@Temporal` / modern date-time handling
* `@Lob`

### Chapter 3 — Primary Keys

* `@GeneratedValue`
* `@SequenceGenerator`
* `@TableGenerator`
* Composite keys
* `@EmbeddedId`
* `@IdClass`

### Chapter 4 — Embeddable Objects

* `@Embeddable`
* `@Embedded`
* `@AttributeOverride`
* `@AttributeOverrides`

### Chapter 5 — Relationships

* `@OneToOne`
* `@OneToMany`
* `@ManyToOne`
* `@ManyToMany`

### Chapter 6 — Relationship Configuration

* `mappedBy`
* `@JoinColumn`
* `@JoinTable`
* `cascade`
* `orphanRemoval`
* `fetch`
* owning side vs inverse side

### Chapter 7 — Inheritance

* `@Inheritance`
* `@DiscriminatorColumn`
* `@DiscriminatorValue`
* `@MappedSuperclass`

### Chapter 8 — Advanced Entity Mapping

* `@SecondaryTable`
* `@SecondaryTables`
* `@AttributeOverride`
* `@AssociationOverride`

### Chapter 9 — Entity Lifecycle

* `@PrePersist`
* `@PostPersist`
* `@PreUpdate`
* `@PostUpdate`
* `@PreRemove`
* `@PostRemove`
* `@PostLoad`

### Chapter 10 — Entity Listeners

* `@EntityListeners`
* listener classes
* auditing patterns

### Chapter 11 — Optimistic Locking

* `@Version`

### Chapter 12 — Hibernate/JPA Practical Annotations

* `@CreationTimestamp`
* `@UpdateTimestamp`
* Hibernate-specific annotations
* JPA vs Hibernate annotations

### Chapter 13 — Fetching & Performance

* lazy vs eager
* N+1
* entity graphs
* `@NamedEntityGraph`
* `@NamedAttributeNode`
* `@NamedSubgraph`

### Chapter 14 — Query & Repository-Related Annotations

* `@NamedQuery`
* `@NamedQueries`
* `@NamedNativeQuery`
* `@SqlResultSetMapping`

### Chapter 15 — Production & Interview Scenarios

* Designing entity mappings
* Bidirectional relationships
* cascade decisions
* `orphanRemoval`
* transaction boundaries
* common Hibernate bugs
* interview traps

---

# Chapter 1 — JPA Fundamentals

Before learning individual annotations, let's understand **what JPA is actually doing**.

Suppose we have this database table:

```text
users
--------------------------------
id          BIGINT       PK
name        VARCHAR(100)
email       VARCHAR(255)
age         INTEGER
```

And we want a Java object:

```java
public class User {

    private Long id;
    private String name;
    private String email;
    private Integer age;
}
```

JPA's job is essentially to establish:

```text
Java Object                         Database
-----------                         --------

User                         --->   users
id                           --->   id
name                         --->   name
email                        --->   email
age                          --->   age
```

This is called **Object-Relational Mapping (ORM)**.

JPA annotations tell the persistence provider:

> "How should this Java class/object correspond to the relational database?"

For example:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "age")
    private Integer age;
}
```

Here:

```text
@Entity
   ↓
This Java class is a persistent entity

@Table
   ↓
Map entity to a particular database table

@Id
   ↓
This field is the primary key

@Column
   ↓
Map/configure a database column
```

Let's go through them carefully.

---

# 1. `@Entity`

`@Entity` tells JPA:

> This Java class represents a persistent entity and should be managed by the persistence context.

Example:

```java
@Entity
public class User {

    private Long id;
    private String name;
}
```

Without `@Entity`:

```java
public class User {

    private Long id;
    private String name;
}
```

JPA doesn't automatically treat `User` as a persistent entity.

---

## What does "managed entity" mean?

This is an important interview concept.

Suppose:

```java
User user = entityManager.find(User.class, 1L);
```

JPA returns a `User` object.

That object can become **managed by the persistence context**.

Conceptually:

```text
Database
   |
   | SELECT
   v
Persistence Context
   |
   | managed object
   v
User object
```

If you modify it:

```java
user.setName("Riyaz");
```

Hibernate can detect that change and eventually execute:

```sql
UPDATE users
SET name = 'Riyaz'
WHERE id = 1;
```

This mechanism is called **dirty checking**.

We'll cover persistence context and dirty checking later because they're extremely important for interviews.

---

# 2. `@Table`

`@Table` specifies the database table associated with an entity.

Example:

```java
@Entity
@Table(name = "users")
public class User {
}
```

This means:

```text
User
  ↓
users table
```

Without `@Table`:

```java
@Entity
public class User {
}
```

JPA uses a default table name according to its naming rules/provider configuration.

Usually:

```text
User → user
```

or potentially another naming convention depending on the JPA provider/framework configuration.

Therefore, if you explicitly want a particular table:

```java
@Table(name = "users")
```

is clearer.

---

## `@Table` has other useful attributes

For example:

```java
@Table(
    name = "users",
    schema = "application"
)
```

This can map to:

```text
application.users
```

It can also define table-level unique constraints and indexes.

For example:

```java
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
    }
)
```

Conceptually:

```text
users
-----------------
id
name
email  ← UNIQUE
```

And indexes can also be declared:

```java
@Table(
    name = "users",
    indexes = {
        @Index(
            name = "idx_user_email",
            columnList = "email"
        )
    }
)
```

Important distinction:

> `@Table` describes the **table**, while `@Column` describes an individual **column**.

---

# 3. `@Id`

`@Id` identifies the **primary key** of the entity.

Example:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Mapping:

```text
Java                         Database

User
 ├── id       ────────────>  id (PK)
 └── name     ────────────>  name
```

Every JPA entity must have an identifier.

This is one of the most important rules:

> **Every entity must have a primary-key mapping.**

---

## Why does JPA need an ID?

Because Hibernate needs to uniquely identify an entity.

Imagine:

```text
User #1
id = 10
name = Alice

User #2
id = 20
name = Bob
```

Hibernate can distinguish:

```text
User(id=10)
User(id=20)
```

This becomes especially important for the persistence context.

Conceptually:

```text
Persistence Context

(User.class, 10) → User object
(User.class, 20) → User object
```

The entity's identity is essentially based on:

```text
Entity type + primary key
```

This is why `@Id` is fundamental to JPA.

---

# 4. `@GeneratedValue`

Now suppose we don't want to manually assign:

```java
user.setId(1001L);
```

Instead, we want the database/application to generate the ID.

We can use:

```java
@Id
@GeneratedValue
private Long id;
```

Example:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Then:

```java
User user = new User();
user.setName("Alice");

entityManager.persist(user);
```

The generated ID might become:

```text
id = 42
```

---

# 5. Generation Strategies

This is a **very common interview question**.

`@GeneratedValue` supports strategies such as:

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

```java
@GeneratedValue(strategy = GenerationType.SEQUENCE)
```

```java
@GeneratedValue(strategy = GenerationType.TABLE)
```

```java
@GeneratedValue(strategy = GenerationType.AUTO)
```

---

## `IDENTITY`

Example:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Typically uses a database identity/auto-increment column.

For example, PostgreSQL/MySQL-style databases may have mechanisms that generate IDs when inserting rows.

Conceptually:

```sql
INSERT INTO users (name)
VALUES ('Alice');
```

Database generates:

```text
id = 101
```

### Important

With identity generation, the database typically generates the ID as part of the insert.

---

# 6. `SEQUENCE`

Example:

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

Hibernate can obtain a value from the sequence and use it for the entity.

Conceptually:

```text
Sequence
   |
   | next value
   v
101
   |
   v
INSERT INTO users(id, name)
VALUES (101, 'Alice')
```

PostgreSQL commonly makes sequences particularly relevant.

---

## `@SequenceGenerator`

We can configure the sequence:

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

There are two names here:

```text
name = "user_seq"
```

This is the JPA generator name.

And:

```text
sequenceName = "user_id_seq"
```

This is the database sequence name.

So:

```text
JPA
 |
 | user_seq
 v
SequenceGenerator
 |
 | user_id_seq
 v
Database sequence
```

`allocationSize` is also important for performance because Hibernate can allocate IDs in batches rather than requesting every ID individually.

---

# 7. `TABLE`

```java
@GeneratedValue(strategy = GenerationType.TABLE)
```

Uses a table to maintain generated ID values.

Conceptually:

```text
id_generator
----------------------
generator_name | value
user           | 101
```

Hibernate updates this table to obtain new identifiers.

This approach is generally less common in modern applications because database-native identity/sequence mechanisms are usually preferable.

---

# 8. `AUTO`

```java
@GeneratedValue(strategy = GenerationType.AUTO)
```

You're essentially telling JPA:

> Choose an appropriate generation strategy for this database/provider.

Hibernate decides what strategy is appropriate.

So:

```java
@GeneratedValue(strategy = GenerationType.AUTO)
```

does **not** necessarily mean one fixed mechanism.

It can differ depending on the database and provider.

---

# 9. `@Column`

`@Column` maps a Java field/property to a database column and allows you to configure that mapping.

Example:

```java
@Column(name = "user_name")
private String name;
```

Mapping:

```text
Java field              DB column

name       ---------->  user_name
```

---

## Common `@Column` attributes

### `name`

```java
@Column(name = "user_name")
private String name;
```

---

### `nullable`

```java
@Column(nullable = false)
private String email;
```

Conceptually:

```sql
email VARCHAR(...) NOT NULL
```

Important interview distinction:

`nullable = false` is primarily a **database schema constraint declaration**.

It does not mean:

> "JPA will never allow Java code to put null into this field."

It is not a general Java runtime null check.

---

### `unique`

```java
@Column(unique = true)
private String email;
```

Indicates uniqueness at the database schema level.

But for production systems, explicit named constraints/indexes are often preferable:

```java
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_email",
            columnNames = "email"
        )
    }
)
```

---

### `length`

```java
@Column(length = 100)
private String name;
```

Typically corresponds to something like:

```sql
VARCHAR(100)
```

This primarily affects schema generation.

---

### `precision` and `scale`

Very important for monetary values.

```java
@Column(
    precision = 19,
    scale = 4
)
private BigDecimal amount;
```

Conceptually:

```text
precision = total number of digits
scale     = digits after decimal point
```

For:

```text
123456789012345.1234
```

there are:

```text
19 total digits
4 decimal digits
```

---

# 10. Putting Everything Together

A typical simple entity:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "user_name",
        nullable = false,
        length = 100
    )
    private String name;

    @Column(
        name = "email",
        nullable = false,
        unique = true,
        length = 255
    )
    private String email;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;
}
```

The conceptual mapping is:

```text
                 Java
                  |
                  v
             ┌──────────┐
             │   User   │
             └──────────┘
                  |
       ┌──────────┼───────────┐
       ↓          ↓           ↓
      id         name        email
       |          |           |
       |          |           |
       ↓          ↓           ↓
      PK       user_name    email
       |                      |
       |                      |
       └──────────────┬───────┘
                      ↓
                   users
                   TABLE
```

---

# Interview Questions — Chapter 1

### Q1. What is `@Entity`?

**Answer:**

`@Entity` marks a Java class as a JPA entity whose instances can be persisted to a relational database and managed by the persistence context.

---

### Q2. Is `@Entity` a Hibernate-specific annotation?

**No.**

It is part of the JPA/Jakarta Persistence specification.

Modern applications generally use:

```java
jakarta.persistence.Entity
```

rather than the old:

```java
javax.persistence.Entity
```

---

### Q3. Can an entity exist without `@Table`?

**Yes.**

`@Table` is optional.

If omitted, the persistence provider determines the default table name according to its naming rules.

---

### Q4. Can an entity exist without `@Id`?

**No, not as a normal JPA entity.**

An entity needs an identifier mapping.

---

### Q5. What's the difference between `@Id` and `@GeneratedValue`?

Very common interview question.

```java
@Id
@GeneratedValue
private Long id;
```

`@Id` says:

> This is the entity's primary-key attribute.

`@GeneratedValue` says:

> The value of this primary key should be generated automatically.

Therefore:

```text
@Id
        = identity

@GeneratedValue
        = how the identity value is generated
```

---

### Q6. `IDENTITY` vs `SEQUENCE`?

High-level answer:

|                                        | IDENTITY              | SEQUENCE                        |
| -------------------------------------- | --------------------- | ------------------------------- |
| ID generated by                        | DB identity mechanism | DB sequence                     |
| Sequence object                        | No                    | Yes                             |
| ID can often be obtained before INSERT | Generally no          | Yes                             |
| Batching implications                  | Can be less flexible  | Usually better                  |
| PostgreSQL                             | Supported             | Very commonly used              |
| Performance                            | Depends               | Often excellent with allocation |

The important interview point is:

> **Don't blindly choose `IDENTITY`; understand how the chosen database and Hibernate handle identifier generation and batching.**

---

### Q7. Is `@Column(nullable = false)` the same as Java validation?

No.

These are different concerns.

```java
@Column(nullable = false)
private String email;
```

is about database/schema nullability.

Whereas:

```java
@NotNull
private String email;
```

is Bean Validation.

They can be used together:

```java
@NotNull
@Column(nullable = false)
private String email;
```

They solve related but different problems.

---

# Important Interview Mental Model

Remember these five annotations as the foundation:

```text
@Entity
   ↓
"This Java class is persistent"

@Table
   ↓
"Which database table?"

@Id
   ↓
"Which field identifies the entity?"

@GeneratedValue
   ↓
"How is the ID generated?"

@Column
   ↓
"How does this field map/configure its column?"
```

If you understand those five, you've got the foundation for almost everything that follows.

**Next chapter:** `@Basic`, `@Transient`, `@Access`, `@Enumerated`, `@Lob`, and Java date/time mapping — including the common interview traps around **field vs property access and why `@Transient` is not the same as Java's `transient` keyword**.





----
----
----






`IDENTITY` and `SEQUENCE` are two `GenerationType` strategies for letting the database generate primary keys. The difference that matters is **when the ID becomes known**, and that drives almost everything else.

## How each works

**IDENTITY** uses an auto-increment column (`AUTO_INCREMENT` in MySQL, `IDENTITY` in SQL Server, `GENERATED ... AS IDENTITY` in Postgres). The database assigns the ID **at INSERT time**. Hibernate has no way to know the ID until the row is actually inserted.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**SEQUENCE** uses a database sequence object. Hibernate asks the sequence for a value **before** the INSERT, so the entity has its ID while it is still only in the persistence context.

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
@SequenceGenerator(name = "order_seq", sequenceName = "order_seq", allocationSize = 50)
private Long id;
```

## The key consequence: JDBC batching

Because IDENTITY needs the INSERT to obtain the ID, Hibernate must **execute each insert immediately** on `persist()`, which disables JDBC insert batching.

```java
for (int i = 0; i < 1000; i++) {
    em.persist(new Order());   // IDENTITY: 1000 separate INSERTs, fired right here
}
```

With SEQUENCE, IDs are known up front, so Hibernate can defer the inserts until flush and send them in batches (given `hibernate.jdbc.batch_size` is set). This also fits the write-behind model of the persistence context, where SQL is delayed until flush or commit.

## allocationSize and the pooled optimizer

By default, `allocationSize = 50`. Hibernate fetches one sequence value and uses it to hand out 50 IDs in memory, so it makes roughly **one sequence call per 50 inserts** instead of one per insert. This is the `pooled` optimizer.

Two gotchas:
- The `allocationSize` in the annotation **must match** the sequence's `INCREMENT BY` in the database. A mismatch gives duplicate-key errors or large ID gaps.
- Other systems inserting into the same table with their own `nextval` calls can collide with Hibernate's pooled ranges unless they follow the same increment convention.

## Comparison

| Aspect | IDENTITY | SEQUENCE |
|---|---|---|
| ID known | After INSERT | Before INSERT |
| JDBC batch inserts | Disabled | Supported |
| Round trips | One INSERT per entity, immediate | Sequence call every N IDs, then batched |
| DB support | MySQL, SQL Server, Postgres, etc. | Postgres, Oracle, SQL Server 2012+, MariaDB 10.3+; **not MySQL** |
| Gaps in IDs | Rare | Common (rollbacks, pooled blocks) |
| Setup | Simplest | Needs matching generator config |

## Which to choose

- **SEQUENCE** is the default recommendation when the database supports it, especially for write-heavy workloads or batch imports.
- **IDENTITY** is the pragmatic choice on **MySQL**, which has no sequences. Accept the loss of batching, or use a table-based generator or application-generated IDs (UUID/ULID/Snowflake) if batch performance matters.
- Avoid `GenerationType.AUTO` unless you know what it resolves to. It depends on dialect and Hibernate version, and in Hibernate 6 it picks SEQUENCE on databases that support it, which can surprise people migrating from 5.x, where it often meant `hibernate_sequence` or IDENTITY.

## Interview one-liner

"IDENTITY gets the ID from the insert itself, so Hibernate must insert immediately and can't batch. SEQUENCE fetches IDs ahead of time, often in blocks via `allocationSize`, so inserts can be deferred and batched, which is why it's preferred wherever the database supports it."

Want me to go into how this interacts with `persist()` vs `merge()` and flush timing, or the `TABLE` strategy as the third option?

