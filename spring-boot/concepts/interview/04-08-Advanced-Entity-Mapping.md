# Chapter 8 — Advanced Entity Mapping

In the previous chapters, we mostly had a simple assumption:

```text
Java Entity
     |
     v
One database table
```

For example:

```java
@Entity
class User {
    Long id;
    String name;
    String email;
}
```

maps roughly to:

```text
USER
----------------
id
name
email
```

But real systems sometimes have more complicated schemas.

You may encounter:

```text
One Java entity
      |
      +---- TABLE A
      |
      +---- TABLE B
```

or:

```text
One embeddable
      |
      +---- used multiple times
      |
      +---- different column names
```

or:

```text
Inherited association
      |
      +---- needs a different join column
```

This chapter covers the annotations used for these situations.

---

# 1. `@SecondaryTable`

`@SecondaryTable` allows a **single entity to map its persistent fields across multiple database tables**.

This is the key idea:

```text
                    User Entity
                        |
             +----------+----------+
             |                     |
             v                     v
         USER table          USER_DETAILS table
```

This is **not inheritance**.

It is still **one entity**.

---

# 2. Basic Example

Suppose the database is:

```text
USER
----------------
id
username
email


USER_DETAILS
----------------
user_id
phone
address
date_of_birth
```

But from the Java application's perspective, we want:

```java
@Entity
class User {

    Long id;
    String username;
    String email;

    String phone;
    String address;
    LocalDate dateOfBirth;
}
```

One `User` entity spans two tables.

---

# 3. Mapping With `@SecondaryTable`

```java
@Entity
@Table(name = "users")
@SecondaryTable(
    name = "user_details",
    pkJoinColumns = @PrimaryKeyJoinColumn(
        name = "user_id",
        referencedColumnName = "id"
    )
)
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String username;

    private String email;

    @Column(table = "user_details")
    private String phone;

    @Column(table = "user_details")
    private String address;

    @Column(table = "user_details")
    private LocalDate dateOfBirth;
}
```

There are two important pieces here.

### First:

```java
@SecondaryTable(...)
```

tells JPA:

> This entity also uses another table.

### Second:

```java
@Column(table = "user_details")
```

tells JPA:

> This particular field belongs to the secondary table.

---

# 4. How the Database Looks

Primary table:

```text
USERS
+----+----------+----------------+
| id | username | email          |
+----+----------+----------------+
| 1  | john     | john@test.com  |
+----+----------+----------------+
```

Secondary table:

```text
USER_DETAILS
+---------+-------+---------+--------------+
| user_id | phone | address | date_of_birth|
+---------+-------+---------+--------------+
| 1       | ...   | ...     | ...          |
+---------+-------+---------+--------------+
```

Relationship:

```text
USERS
  id
   |
   | PK = FK
   |
   v
USER_DETAILS
  user_id
```

---

# 5. Why Is It Called "Secondary Table"?

Because the entity has:

```text
Primary table
+
Secondary table(s)
```

For example:

```text
User
 |
 +---- users
 |
 +---- user_details
```

But from Java's perspective:

```text
User
```

is still **one entity**.

There aren't two entities.

---

# 6. `@SecondaryTable` vs `@OneToOne`

This distinction is important.

Suppose:

```text
USER
id
name

USER_DETAILS
user_id
phone
```

You could model it as:

### Option A

One entity across two tables:

```java
@SecondaryTable(name = "user_details")
```

### Option B

Two entities:

```java
User
   |
   | @OneToOne
   v
UserDetails
```

These are fundamentally different models.

---

# 7. When Would You Use `@SecondaryTable`?

Use it when:

> The two tables are really just different physical storage portions of the **same logical entity**.

For example:

```text
USER
basic information

USER_DETAILS
extended information
```

but the application conceptually considers both as one `User`.

You might encounter this when:

* integrating with an existing legacy database
* dealing with legacy schemas
* splitting very wide tables
* mapping tables that share the same primary key
* preserving an existing database structure

---

# 8. `@SecondaryTables`

What if one entity spans **more than two tables**?

Use:

```java
@SecondaryTables
```

Example:

```text
User
 |
 +---- USERS
 |
 +---- USER_DETAILS
 |
 +---- USER_AUDIT
```

Mapping:

```java
@Entity
@Table(name = "users")
@SecondaryTables({
    @SecondaryTable(
        name = "user_details",
        pkJoinColumns = @PrimaryKeyJoinColumn(
            name = "user_id"
        )
    ),
    @SecondaryTable(
        name = "user_audit",
        pkJoinColumns = @PrimaryKeyJoinColumn(
            name = "user_id"
        )
    )
})
public class User {
    ...
}
```

Then fields can specify which table they belong to:

```java
@Column(table = "user_details")
private String phone;

@Column(table = "user_audit")
private Instant lastModified;

@Column(table = "user_audit")
private String modifiedBy;
```

---

# 9. Mental Model for `@SecondaryTable`

Think:

```text
                    User Entity
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
        USERS     USER_DETAILS   USER_AUDIT
```

Still:

```text
ONE Java entity
```

not:

```text
THREE Java entities
```

---

# 10. Important Interview Question

### Does `@SecondaryTable` create a relationship between two entities?

**No.**

There aren't two entities involved.

It maps one entity across multiple tables.

Compare:

```text
@SecondaryTable
```

with:

```text
@OneToOne
```

### `@SecondaryTable`

```text
One Entity
   |
   +---- Table A
   |
   +---- Table B
```

### `@OneToOne`

```text
Entity A
   |
   | relationship
   v
Entity B
```

That's a very important distinction.

---

# 11. `@PrimaryKeyJoinColumn`

Notice:

```java
pkJoinColumns = @PrimaryKeyJoinColumn(
    name = "user_id",
    referencedColumnName = "id"
)
```

This defines how the secondary table joins to the primary table.

Conceptually:

```text
USERS
id
 |
 | =
 |
 v
USER_DETAILS
user_id
```

So:

```text
USERS.id
     =
USER_DETAILS.user_id
```

---

# 12. Why Primary-Key Join?

Unlike a normal:

```text
@ManyToOne
```

where you might have:

```text
order.customer_id
```

the secondary table commonly uses the **same primary key**.

Example:

```text
USERS
id = 100

USER_DETAILS
user_id = 100
```

Therefore:

```text
USER_DETAILS.user_id
```

identifies the same logical `User`.

---

# 13. `@AttributeOverride`

We already encountered this concept with `@Embeddable`.

Now let's understand it in more depth.

Suppose:

```java
@Embeddable
public class Address {

    private String street;

    private String city;

    private String postalCode;
}
```

And:

```java
@Entity
public class User {

    @Embedded
    private Address address;
}
```

By default, columns might be:

```text
street
city
postalCode
```

But suppose we want:

```text
address_street
address_city
address_postal_code
```

We can use:

```java
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "address_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "address_city")
    ),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "address_postal_code")
    )
})
```

---

# 14. Why Is `@AttributeOverride` Needed?

Because the same embeddable class may be used in different contexts.

For example:

```java
@Embeddable
class Address {
    String street;
    String city;
}
```

Then:

```java
@Entity
class Customer {

    @Embedded
    Address billingAddress;

    @Embedded
    Address shippingAddress;
}
```

Both would otherwise try to use:

```text
street
city
```

for their columns.

That creates a collision.

---

# 15. Solving the Collision

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "billing_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "billing_city")
    )
})
private Address billingAddress;
```

And:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "shipping_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "shipping_city")
    )
})
private Address shippingAddress;
```

Database:

```text
CUSTOMER
------------------------------------------------
id
billing_street
billing_city
shipping_street
shipping_city
```

---

# 16. `@AttributeOverride` vs `@AttributeOverrides`

### One attribute

```java
@AttributeOverride(
    name = "street",
    column = @Column(name = "billing_street")
)
```

### Multiple attributes

```java
@AttributeOverrides({
    @AttributeOverride(...),
    @AttributeOverride(...),
    @AttributeOverride(...)
})
```

Simple:

```text
@AttributeOverride
       |
       +--> one attribute

@AttributeOverrides
       |
       +--> multiple attributes
```

---

# 17. Another Use Case

Suppose:

```java
@Embeddable
class Money {

    private BigDecimal amount;

    private String currency;
}
```

And:

```java
@Entity
class Product {

    @Embedded
    private Money price;
}
```

Default:

```text
amount
currency
```

But perhaps you need:

```text
price_amount
price_currency
```

Use:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "amount",
        column = @Column(name = "price_amount")
    ),
    @AttributeOverride(
        name = "currency",
        column = @Column(name = "price_currency")
    )
})
private Money price;
```

---

# 18. `@AssociationOverride`

Now we move from **attributes** to **relationships**.

`@AttributeOverride` changes mapping for a basic/embedded attribute.

`@AssociationOverride` changes mapping for an **association**.

Think:

```text
@AttributeOverride
       |
       +--> basic attribute


@AssociationOverride
       |
       +--> relationship
```

---

# 19. Example of Association Override

Suppose we have:

```java
@Embeddable
public class ContactInfo {

    private String phone;

    @ManyToOne
    private Country country;
}
```

Now suppose this embeddable is used in different entities.

```java
@Entity
class Customer {

    @Embedded
    private ContactInfo contactInfo;
}
```

and:

```java
@Entity
class Supplier {

    @Embedded
    private ContactInfo contactInfo;
}
```

Maybe the existing database schema requires different join columns.

For example:

```text
CUSTOMER.country_id
SUPPLIER.country_code
```

We can override the association mapping.

---

# 20. `@AssociationOverride` Example

Conceptually:

```java
@Embedded
@AssociationOverride(
    name = "country",
    joinColumns = @JoinColumn(name = "customer_country_id")
)
private ContactInfo contactInfo;
```

Here:

```text
name = "country"
```

refers to the association inside:

```java
ContactInfo.country
```

and:

```java
@JoinColumn(name = "customer_country_id")
```

changes its join-column mapping.

---

# 21. `@AssociationOverrides`

Just like attributes:

```java
@AssociationOverride
```

is for one association.

```java
@AssociationOverrides({
    @AssociationOverride(...),
    @AssociationOverride(...)
})
```

is for multiple associations.

---

# 22. Attribute vs Association

This distinction is worth memorizing.

Suppose:

```java
@Embeddable
class EmployeeInfo {

    String name;

    String email;

    Department department;
}
```

If:

```text
name
email
```

need different column mappings:

```java
@AttributeOverride
```

If:

```text
department
```

needs a different relationship mapping:

```java
@AssociationOverride
```

So:

```text
Embedded Object
       |
       +---- Basic attribute
       |        |
       |        +--> @AttributeOverride
       |
       +---- Association
                |
                +--> @AssociationOverride
```

---

# 23. Nested Embeddables + Overrides

Things can become more interesting.

Suppose:

```java
@Embeddable
class Address {

    private String street;

    @Embedded
    private Location location;
}
```

and:

```java
@Embeddable
class Location {

    private String city;
    private String country;
}
```

Then:

```java
@Entity
class Customer {

    @Embedded
    private Address address;
}
```

You may need to override nested properties.

For example:

```java
@AttributeOverride(
    name = "location.city",
    column = @Column(name = "address_city")
)
```

The important concept is:

> Overrides can target attributes inside nested embeddable structures.

---

# 24. `@SecondaryTable` vs `@Inheritance`

Another important interview comparison.

### `@SecondaryTable`

```text
One entity
    |
    +---- Table A
    |
    +---- Table B
```

### `@Inheritance`

```text
Entity hierarchy
      |
      +---- Subclass A
      |
      +---- Subclass B
```

Example:

```text
@SecondaryTable

User
 |
 +---- USER
 |
 +---- USER_DETAILS
```

versus:

```text
@Inheritance

Employee
 |
 +---- Developer
 |
 +---- Manager
```

The first is **one entity split across tables**.

The second is **multiple entity types in an inheritance hierarchy**.

---

# 25. `@SecondaryTable` vs `@OneToOne`

This is another common interview trap.

Suppose:

```text
USER
id
name

USER_DETAILS
user_id
phone
```

### `@SecondaryTable`

```text
One User entity
        |
        +---- USERS
        +---- USER_DETAILS
```

### `@OneToOne`

```text
User entity
     |
     | association
     v
UserDetails entity
```

Use `@OneToOne` when `UserDetails` has its **own entity identity/lifecycle/behavior**.

Use `@SecondaryTable` when the two tables are merely different storage portions of the **same entity**.

---

# 26. `@SecondaryTable` vs `@Embedded`

These can also look similar.

### `@Embedded`

```text
User
 |
 +---- Address
```

Address fields become columns in the same table:

```text
USER
----------------
id
street
city
postal_code
```

### `@SecondaryTable`

```text
User
 |
 +---- USERS
 |
 +---- USER_DETAILS
```

The distinction is:

```text
@Embedded
    = object structure

@SecondaryTable
    = physical table structure
```

---

# 27. A Very Useful Mental Model

Think about these annotations in layers:

```text
ENTITY
 |
 +-----------------------+
 |                       |
 v                       v
Object/value mapping     Relationship mapping
 |
 +--> @Embedded
 +--> @AttributeOverride
 +--> @AttributeOverrides
 |
 |
 +--> @AssociationOverride
```

And table structure:

```text
ENTITY
 |
 +--> @Table
 |
 +--> @SecondaryTable
 +--> @SecondaryTables
```

And inheritance:

```text
ENTITY HIERARCHY
 |
 +--> @Inheritance
 +--> @DiscriminatorColumn
 +--> @DiscriminatorValue
 +--> @MappedSuperclass
```

---

# 28. Production Scenario — Legacy Database

Imagine you're integrating with a legacy database:

```text
CUSTOMER
------------------
customer_id
name
email


CUSTOMER_CONTACT
------------------
customer_id
phone
mobile
address
```

But your application wants:

```java
Customer
```

as one logical entity.

You might use:

```java
@Entity
@Table(name = "customer")
@SecondaryTable(
    name = "customer_contact",
    pkJoinColumns = @PrimaryKeyJoinColumn(
        name = "customer_id"
    )
)
class Customer {
    ...
}
```

This is a good example of when `@SecondaryTable` becomes useful.

---

# 29. Production Scenario — Reusable Value Object

Suppose many entities have:

```text
Money
Address
Coordinates
AuditInfo
```

Instead of duplicating fields:

```java
class Product {
    BigDecimal amount;
    String currency;
}

class Invoice {
    BigDecimal amount;
    String currency;
}
```

use:

```java
@Embeddable
class Money {
    BigDecimal amount;
    String currency;
}
```

Then:

```java
@Embedded
private Money price;
```

and:

```java
@Embedded
private Money total;
```

If the column names differ:

```java
@AttributeOverride
```

becomes useful.

---

# 30. Interview Question: What Does `@SecondaryTable` Do?

Strong answer:

> `@SecondaryTable` maps an entity to an additional database table. The entity remains a single JPA entity, and the secondary table is typically joined to the primary table using the entity's primary key.

---

# 31. Interview Question: Does `@SecondaryTable` Create Another Entity?

**No.**

```text
@SecondaryTable
    |
    v
one entity
    |
    +---- multiple tables
```

---

# 32. Interview Question: What Is `@AttributeOverride`?

Strong answer:

> `@AttributeOverride` allows an embedded attribute's column mapping to be customized at the point where the embeddable is used.

Example:

```java
@Embedded
@AttributeOverride(
    name = "street",
    column = @Column(name = "billing_street")
)
private Address billingAddress;
```

---

# 33. Interview Question: What Is `@AssociationOverride`?

Strong answer:

> `@AssociationOverride` allows an association defined inside an embeddable or mapped superclass to have its relationship mapping customized by the entity using it, such as changing its join column.

---

# 34. Interview Question: What's the Difference?

### `@AttributeOverride`

```text
Changes:
column mapping
```

Example:

```java
street -> billing_street
```

### `@AssociationOverride`

```text
Changes:
relationship mapping
```

Example:

```text
country -> customer_country_id
```

---

# 35. Common Interview Traps

### Trap 1

> `@SecondaryTable` means one-to-one relationship.

**Wrong.**

It means:

```text
one entity -> multiple tables
```

---

### Trap 2

> `@MappedSuperclass` creates a base table.

**Wrong.**

The base class doesn't get its own table merely because it's a mapped superclass.

---

### Trap 3

> `@AttributeOverride` is used for relationships.

**Wrong.**

For relationships:

```text
@AssociationOverride
```

---

### Trap 4

> `@Embedded` creates a separate table.

**Wrong.**

Normally:

```text
@Embedded
```

means the embeddable's fields are mapped into the owner's table.

---

### Trap 5

> `@SecondaryTable` creates another entity.

**Wrong.**

One entity, multiple tables.

---

# 36. Complete Cheat Sheet

```text
@Embedded
    |
    +--> Embed value object's fields into entity


@AttributeOverride
    |
    +--> Change column mapping of one embedded attribute


@AttributeOverrides
    |
    +--> Change multiple embedded attributes


@AssociationOverride
    |
    +--> Change mapping of an embedded association


@AssociationOverrides
    |
    +--> Change multiple embedded associations


@SecondaryTable
    |
    +--> One entity -> additional table


@SecondaryTables
    |
    +--> One entity -> multiple additional tables


@PrimaryKeyJoinColumn
    |
    +--> Define PK-based join between primary/secondary tables
```

---

# 37. The Big Picture

At this point, you can think of JPA mapping as several independent dimensions:

```text
                         JPA ENTITY
                              |
          +-------------------+-------------------+
          |                   |                   |
          v                   v                   v
       Identity          Structure           Relationships
          |                   |                   |
       @Id                @Embedded           @ManyToOne
       @GeneratedValue     @Embeddable         @OneToMany
                           @AttributeOverride  @OneToOne
                                              @ManyToMany
```

Then advanced mapping:

```text
Entity
  |
  +---- multiple tables
  |       |
  |       +--> @SecondaryTable
  |
  +---- embedded mapping customization
  |       |
  |       +--> @AttributeOverride
  |       +--> @AssociationOverride
  |
  +---- inheritance
          |
          +--> @Inheritance
          +--> @DiscriminatorColumn
          +--> @DiscriminatorValue
          +--> @MappedSuperclass
```

---

# 38. Interview Scenario

Suppose an interviewer says:

> "Our legacy database stores customer information across three tables, but we want one `Customer` entity in Java. Would you use `@OneToOne`?"

A strong answer:

> Not necessarily. If those tables represent different physical portions of the same logical customer entity and share the customer's primary key, `@SecondaryTables` may be more appropriate. `@OneToOne` would be appropriate if those tables represent separate entities with their own domain identity and lifecycle.

That's the kind of distinction that demonstrates actual JPA understanding.

---

# 39. Final Mental Model for Chapter 8

Remember these four questions:

### "I want to reuse fields."

```text
@MappedSuperclass
```

or:

```text
@Embeddable
```

depending on whether you're sharing entity fields or modeling a value object.

### "I want to rename/customize an embedded field's column."

```text
@AttributeOverride
```

### "I want to customize an embedded relationship."

```text
@AssociationOverride
```

### "One entity needs to span multiple physical tables."

```text
@SecondaryTable
```

That's the core of this chapter.

---

## Chapter 8 complete

**Next: Chapter 9 — Entity Lifecycle & Lifecycle Callback Annotations**

We'll cover:

```text
@PrePersist
@PostPersist
@PreUpdate
@PostUpdate
@PreRemove
@PostRemove
@PostLoad
```

with the complete entity lifecycle:

```text
NEW
 |
 | persist()
 v
MANAGED
 |
 | flush/update
 v
DATABASE
 |
 | remove()
 v
REMOVED
```

and interview questions around **dirty checking, flush, callback ordering, auditing, and common lifecycle traps**.

