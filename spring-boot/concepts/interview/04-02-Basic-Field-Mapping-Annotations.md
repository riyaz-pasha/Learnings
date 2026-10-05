# Chapter 2 — Basic Field Mapping Annotations

In Chapter 1, we learned how an **entity maps to a table**.

Now we go one level deeper:

```text
Entity
   ↓
Table
   ↓
Fields / Properties
   ↓
Columns
```

This chapter focuses on annotations that control **how individual Java attributes are persisted**.

We will cover:

1. `@Basic`
2. `@Transient`
3. `@Access`
4. `@Enumerated`
5. `@Lob`
6. Date/time mapping
7. `@Temporal` and why it is mostly obsolete with modern Java time types
8. Important interview traps

---

# 1. `@Basic`

`@Basic` indicates that a field/property should be mapped as a basic persistent attribute.

Example:

```java
@Entity
public class User {

    @Id
    private Long id;

    @Basic
    private String name;
}
```

In practice, you will rarely write `@Basic` explicitly.

Why?

Because **basic mapping is the default** for most supported Java types.

So this:

```java
private String name;
```

is effectively treated as a basic persistent attribute.

Therefore:

```java
@Basic
private String name;
```

and:

```java
private String name;
```

usually have the same basic meaning.

---

## `@Basic` attributes

It has two important attributes:

```java
@Basic(
    optional = true,
    fetch = FetchType.EAGER
)
private String name;
```

### `optional`

```java
@Basic(optional = false)
private String name;
```

Conceptually:

> This attribute should have a non-null value.

But be careful.

`optional` is primarily a JPA mapping-level indication. It is **not a replacement for Bean Validation** such as:

```java
@NotNull
```

and you shouldn't confuse it with:

```java
@Column(nullable = false)
```

---

## `fetch`

```java
@Basic(fetch = FetchType.LAZY)
private String description;
```

Possible values:

```java
FetchType.EAGER
FetchType.LAZY
```

However, this is an important Hibernate interview topic:

> **LAZY loading of basic attributes is not something you should casually rely on.**

Support for lazy basic attributes can depend on provider capabilities and bytecode enhancement.

For normal relationships, lazy loading is much more commonly discussed:

```java
@OneToMany(fetch = FetchType.LAZY)
```

We'll discuss that extensively when we reach relationships.

---

# 2. `@Transient`

This one is extremely important.

Suppose we have:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String firstName;

    private String lastName;

    private String fullName;
}
```

We might want:

```java
fullName = firstName + " " + lastName
```

but we don't want a `full_name` database column.

We can use:

```java
@Transient
private String fullName;
```

Now:

```text
User
--------------------------------
id          → database
firstName   → database
lastName    → database
fullName    → NOT database
```

Example:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String firstName;

    private String lastName;

    @Transient
    private String fullName;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

---

# Why would we use `@Transient`?

Typical examples:

### Derived values

```java
@Transient
private String fullName;
```

### UI-specific values

```java
@Transient
private boolean selected;
```

### Temporary calculation

```java
@Transient
private BigDecimal calculatedDiscount;
```

### Runtime-only state

```java
@Transient
private SomeExternalObject object;
```

The key idea:

> `@Transient` tells JPA **not to persist this attribute**.

---

# Very Important Interview Question

## `@Transient` vs Java `transient`

These are **not the same thing**.

### JPA:

```java
@Transient
private String temporaryValue;
```

means:

> Don't persist this attribute using JPA.

### Java:

```java
private transient String temporaryValue;
```

means:

> Don't include this field in Java's built-in serialization mechanism.

So:

```text
@Transient
     ↓
JPA persistence

transient
     ↓
Java serialization
```

You can even have:

```java
@Transient
private transient String temporaryValue;
```

although there is usually no reason to do so unless both behaviors are desired.

### Interview answer

If interviewer asks:

> What is the difference between `@Transient` and `transient`?

Answer:

> `@Transient` is a JPA annotation that excludes an attribute from ORM persistence. `transient` is a Java language modifier that excludes a field from Java serialization. They belong to different mechanisms.

---

# 3. `@Access`

This is one of those annotations that beginners often ignore but interviewers like.

JPA can access entity state in two ways:

```text
FIELD access
PROPERTY access
```

---

# Field Access

Example:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Notice:

```java
@Id
private Long id;
```

The annotation is placed directly on the field.

Therefore JPA uses **field access**.

Conceptually:

```text
Hibernate
   |
   | directly accesses
   v
private fields
```

It doesn't need your getter/setter to read the persistent state.

---

# Property Access

Now:

```java
@Entity
public class User {

    private Long id;

    private String name;

    @Id
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

The mapping annotation is on the getter:

```java
@Id
public Long getId()
```

Therefore JPA uses **property access**.

Conceptually:

```text
Hibernate
   |
   | uses
   v
getters/setters
```

---

# `@Access`

You can explicitly specify it:

```java
@Access(AccessType.FIELD)
```

or:

```java
@Access(AccessType.PROPERTY)
```

Example:

```java
@Entity
@Access(AccessType.FIELD)
public class User {

    @Id
    private Long id;

    private String name;
}
```

---

# Why does this matter?

Suppose:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String firstName;

    private String lastName;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

With field access, JPA maps the fields directly.

It does **not** consider every getter to be a persistent property.

That's useful because you can have calculated getters:

```java
public String getFullName() {
    return firstName + " " + lastName;
}
```

without accidentally mapping them.

---

# Critical Rule

JPA determines the default access strategy based on where `@Id` is placed.

### `@Id` on field

```java
@Id
private Long id;
```

→ **FIELD access**

### `@Id` on getter

```java
@Id
public Long getId() {
    return id;
}
```

→ **PROPERTY access**

This is an excellent interview question.

---

# Can we mix field and property access?

Yes, but carefully.

You can use:

```java
@Access(AccessType.FIELD)
```

at the entity level and override access for a particular attribute.

For example:

```java
@Entity
@Access(AccessType.FIELD)
public class User {

    @Id
    private Long id;

    private String name;

    @Access(AccessType.PROPERTY)
    private String something;
}
```

However, mixing access strategies unnecessarily can make entity mappings confusing.

### Production recommendation

Usually choose one strategy consistently.

Most modern Hibernate/Spring applications commonly use:

```java
@Id
private Long id;
```

and therefore **field access**.

---

# 4. `@Enumerated`

Suppose we have:

```java
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
```

And:

```java
@Entity
public class User {

    @Id
    private Long id;

    private UserStatus status;
}
```

We need to tell JPA how the enum should be stored.

Use:

```java
@Enumerated(EnumType.STRING)
private UserStatus status;
```

---

# Two Enum Strategies

There are two:

```java
EnumType.STRING
EnumType.ORDINAL
```

---

## `EnumType.STRING`

```java
@Enumerated(EnumType.STRING)
private UserStatus status;
```

Database:

```text
status
---------
ACTIVE
INACTIVE
SUSPENDED
```

This is generally the **recommended choice**.

---

# Why?

Suppose initially:

```java
enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
```

Ordinal values:

```text
ACTIVE    → 0
INACTIVE  → 1
SUSPENDED → 2
```

Now imagine you add:

```java
enum UserStatus {
    ACTIVE,
    PENDING,
    INACTIVE,
    SUSPENDED
}
```

Now:

```text
ACTIVE    → 0
PENDING   → 1
INACTIVE  → 2
SUSPENDED → 3
```

Previously:

```text
INACTIVE = 1
```

Now:

```text
INACTIVE = 2
```

Existing database values become dangerous.

---

# `EnumType.ORDINAL`

```java
@Enumerated(EnumType.ORDINAL)
private UserStatus status;
```

Database stores:

```text
ACTIVE     → 0
INACTIVE   → 1
SUSPENDED  → 2
```

This is compact but fragile.

The ordinal depends on enum declaration order.

---

# Interview Answer

If interviewer asks:

> Which enum mapping should you normally use?

Say:

> `EnumType.STRING` is generally preferred because the database stores the enum name rather than its ordinal position. Ordinal mapping can break when enum constants are reordered, inserted, or removed.

---

# 5. `@Lob`

`@Lob` means:

> Map this attribute as a large object.

LOB = **Large Object**

There are two broad categories:

```text
CLOB → character data
BLOB → binary data
```

For example:

```java
@Lob
private String documentContent;
```

could represent large textual content.

Or:

```java
@Lob
private byte[] document;
```

could represent binary data.

---

# Typical examples

### Large text

```java
@Lob
private String content;
```

Potential database representation:

```text
CLOB / TEXT-like type
```

depending on the database/provider.

### Binary data

```java
@Lob
private byte[] fileContent;
```

Potential database representation:

```text
BLOB / BYTEA-like type
```

depending on the database.

---

# Should we store files directly in the database?

This is more of a system-design/production question.

For small or moderate binary data, storing in DB may sometimes be appropriate.

But for large files such as:

```text
PDF
Images
Videos
Large documents
```

a common production architecture is:

```text
Application
     |
     +------ metadata ------> PostgreSQL
     |
     +------ file ----------> S3/Object Storage
```

Database:

```text
document
----------------
id
file_name
storage_key
content_type
size
```

Object storage:

```text
s3://bucket/documents/abc.pdf
```

So don't automatically interpret `@Lob` as:

> "This is how I should store every uploaded file."

It is an ORM mapping capability, not an architectural recommendation.

---

# 6. Date and Time Mapping

This is an important area because older JPA versions and modern Java use different approaches.

Modern Java provides:

```java
LocalDate
LocalTime
LocalDateTime
Instant
OffsetDateTime
ZonedDateTime
```

For example:

```java
@Entity
public class User {

    @Id
    private Long id;

    private LocalDate birthDate;

    private LocalDateTime createdAt;

    private Instant updatedAt;
}
```

Modern JPA providers can map these Java 8+ time types appropriately.

Usually, you don't need an annotation.

For example:

```java
private LocalDate birthDate;
```

can map naturally to a SQL date.

---

# `LocalDate`

Represents:

```text
2026-10-05
```

No time.

Good for:

```text
Date of birth
Holiday
Business date
Invoice date
```

---

# `LocalDateTime`

Represents:

```text
2026-10-05 14:30:00
```

It has date + time but **no timezone/offset**.

Good when the business meaning does not require a global instant.

---

# `Instant`

Represents a point on the global timeline.

Example:

```text
2026-10-05T09:00:00Z
```

Very useful for things like:

```text
createdAt
updatedAt
event timestamp
audit timestamp
```

where you care about an absolute point in time.

---

# `OffsetDateTime`

Contains:

```text
date + time + offset
```

Example:

```text
2026-10-05T14:30:00+05:30
```

Useful when the offset itself matters.

---

# 7. `@Temporal`

This is an important historical JPA annotation.

Older Java applications used:

```java
java.util.Date
java.util.Calendar
```

For example:

```java
@Temporal(TemporalType.DATE)
private Date birthDate;
```

Possible values:

```java
TemporalType.DATE
TemporalType.TIME
TemporalType.TIMESTAMP
```

---

## `DATE`

```java
@Temporal(TemporalType.DATE)
private Date birthDate;
```

Stores date portion.

---

## `TIME`

```java
@Temporal(TemporalType.TIME)
private Date startTime;
```

Stores time portion.

---

## `TIMESTAMP`

```java
@Temporal(TemporalType.TIMESTAMP)
private Date createdAt;
```

Stores date + time.

---

# Is `@Temporal` required for `LocalDate`?

**No.**

This is an important interview point.

`@Temporal` is designed for legacy:

```java
java.util.Date
java.util.Calendar
```

Modern Java date/time types such as:

```java
LocalDate
LocalDateTime
Instant
```

are handled directly by modern JPA providers.

So don't write:

```java
@Temporal(...)
private LocalDate createdAt;
```

That's not the intended usage.

---

# 8. Complete Example

Putting Chapter 2 together:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    @Transient
    private String displayName;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Lob
    private String bio;

    private LocalDate birthDate;

    private Instant createdAt;
}
```

Database conceptually:

```text
users
-------------------------------------------------
id
first_name
last_name
status
bio
birth_date
created_at
-------------------------------------------------

displayName
     ↑
     |
     └── NOT persisted
```

---

# Interview Questions

## Q1. What is `@Basic`?

`@Basic` indicates a basic persistent attribute. It is usually implicit, so developers rarely need to specify it explicitly.

---

## Q2. What is the difference between `@Transient` and `transient`?

```text
@Transient
   ↓
JPA persistence

transient
   ↓
Java serialization
```

They solve different problems.

---

## Q3. How does JPA decide field vs property access?

Primarily based on where the mapping annotation for the identifier, typically `@Id`, is placed.

```java
@Id
private Long id;
```

→ field access.

```java
@Id
public Long getId() { ... }
```

→ property access.

---

## Q4. Why is `EnumType.STRING` generally preferred?

Because ordinal values depend on enum declaration order.

```text
ORDINAL

ACTIVE    = 0
INACTIVE  = 1
SUSPENDED = 2
```

Changing enum order can change persisted meanings.

`STRING` stores:

```text
ACTIVE
INACTIVE
SUSPENDED
```

which is much safer from that particular class of schema/data corruption.

---

## Q5. Does `@Enumerated` create an enum table?

No.

```java
@Enumerated(EnumType.STRING)
private UserStatus status;
```

normally stores the enum value in a column of the entity's table.

It does not automatically create:

```text
user_status
------------
id
name
```

---

## Q6. What does `@Lob` mean?

It tells JPA to map an attribute as a large object, typically a CLOB for large character data or BLOB for binary data.

---

## Q7. Is `@Temporal` needed for `LocalDate`?

No.

`@Temporal` is primarily for legacy `java.util.Date` and `Calendar` mappings.

---

# ⭐ Most Important Things to Remember

```text
@Basic
   ↓
Normal persistent attribute
   ↓
Usually implicit

@Transient
   ↓
Don't persist this attribute

@Access
   ↓
FIELD vs PROPERTY access

@Enumerated
   ↓
How enum is stored
   ↓
STRING usually preferred

@Lob
   ↓
Large text/binary data

@Temporal
   ↓
Legacy Date/Calendar mapping

LocalDate / LocalDateTime / Instant
   ↓
Modern Java time API
   ↓
Usually no annotation required
```

### One particularly important interview distinction

Don't mix these three:

```text
@Transient
transient
@Column(nullable = false)
```

They belong to three different concerns:

```text
@Transient
     → ORM mapping

transient
     → Java serialization

@Column(nullable = false)
     → database column constraint/schema
```

**Next chapter:** **Primary Key Mapping in depth** — `@GeneratedValue`, `GenerationType`, `@SequenceGenerator`, `@TableGenerator`, composite primary keys, `@EmbeddedId` vs `@IdClass`, and the interview question **"Why is `@EmbeddedId` usually preferable to `@IdClass`?"**


