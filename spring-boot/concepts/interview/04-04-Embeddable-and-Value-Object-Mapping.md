# Chapter 4 — Embeddable & Value Object Mapping

This chapter introduces an important JPA concept:

> **Not every Java class needs to become a database table.**

Sometimes a class is simply a group of related attributes that belongs to another entity.

For example:

```text
User
 ├── id
 ├── name
 └── Address
      ├── street
      ├── city
      └── postalCode
```

We may want this Java model:

```java
class User {
    private Long id;
    private String name;
    private Address address;
}

class Address {
    private String street;
    private String city;
    private String postalCode;
}
```

But we don't necessarily want:

```text
users
addresses
```

Instead, we can store everything in:

```text
users
------------------------------------------------
id
name
street
city
postal_code
```

This is where:

* `@Embeddable`
* `@Embedded`
* `@AttributeOverride`
* `@AttributeOverrides`

come in.

---

# 1. Entity vs Embeddable

Before annotations, understand the conceptual difference.

## Entity

An entity has its own identity.

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
}
```

Conceptually:

```text
User
 ↓
has identity
 ↓
id = 101
 ↓
users table
```

---

## Embeddable

An embeddable does **not** have its own independent entity identity.

```java
@Embeddable
public class Address {

    private String street;
    private String city;
    private String postalCode;
}
```

It's a reusable group of fields.

Conceptually:

```text
Address
   ↓
no independent @Id
   ↓
embedded inside an Entity
```

---

# 2. `@Embeddable`

Let's define an address:

```java
@Embeddable
public class Address {

    private String street;

    private String city;

    private String postalCode;
}
```

This tells JPA:

> This class is a persistable value type whose fields can be embedded into an entity's table.

Notice:

```java
@Embeddable
```

but **no**:

```java
@Id
```

because `Address` isn't an independent entity.

---

# 3. `@Embedded`

Now embed it into an entity:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;

    @Embedded
    private Address address;
}
```

Now JPA conceptually maps:

```text
User
│
├── id
├── name
└── address
     ├── street
     ├── city
     └── postalCode
```

to:

```text
users
--------------------------------
id
name
street
city
postal_code
```

There is **no separate Address table**.

---

# 4. `@Embedded` Is Often Optional

This:

```java
@Embedded
private Address address;
```

can often simply be:

```java
private Address address;
```

if JPA can determine that `Address` is an embeddable.

The explicit annotation is useful for readability and when you want to configure the embedding.

---

# 5. Why Use Embeddables?

Suppose your application has:

```text
User
Customer
Supplier
Employee
```

and all of them have an address.

Without an embeddable:

```java
class User {
    private String street;
    private String city;
    private String postalCode;
}

class Customer {
    private String street;
    private String city;
    private String postalCode;
}
```

This duplicates the structure.

Instead:

```java
@Embeddable
public class Address {

    private String street;
    private String city;
    private String postalCode;
}
```

Then:

```java
@Entity
public class User {

    @Embedded
    private Address address;
}
```

and:

```java
@Entity
public class Customer {

    @Embedded
    private Address address;
}
```

Now the structure is reusable.

---

# 6. Very Important: Embeddable ≠ Entity

This is a common interview question.

### Entity

```java
@Entity
public class Address {

    @Id
    private Long id;
}
```

This means:

```text
Address
   ↓
independent identity
   ↓
database table
```

### Embeddable

```java
@Embeddable
public class Address {

    private String city;
}
```

means:

```text
Address
   ↓
no independent identity
   ↓
fields belong to owning entity
```

---

# 7. Example Database Mapping

Java:

```java
@Embeddable
public class Address {

    private String street;

    private String city;

    private String postalCode;
}
```

Entity:

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    private String name;

    @Embedded
    private Address address;
}
```

Database:

```text
users
------------------------------------------------
id
name
street
city
postal_code
------------------------------------------------
```

Not:

```text
users
addresses
```

That's the key idea.

---

# 8. `@AttributeOverride`

Now suppose we have:

```java
@Embeddable
public class Address {

    private String street;
    private String city;
    private String postalCode;
}
```

Normally JPA might map:

```text
street
city
postal_code
```

But perhaps our database uses:

```text
home_street
home_city
home_postal_code
```

We can override the column mapping.

```java
@Embedded
@AttributeOverride(
    name = "street",
    column = @Column(name = "home_street")
)
private Address homeAddress;
```

Now:

```text
Address.street
      ↓
home_street
```

---

# 9. `@AttributeOverrides`

If we need multiple overrides:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "home_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "home_city")
    ),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "home_postal_code")
    )
})
private Address homeAddress;
```

Mapping:

```text
Address                  Database

street       --------->  home_street
city         --------->  home_city
postalCode   --------->  home_postal_code
```

---

# 10. Why Are Overrides Needed?

This becomes especially useful when the same embeddable appears **more than once**.

Suppose a customer has:

```text
homeAddress
workAddress
```

Both use the same `Address` class.

```java
@Entity
public class Customer {

    @Id
    private Long id;

    @Embedded
    private Address homeAddress;

    @Embedded
    private Address workAddress;
}
```

Houston, we have a mapping problem.

Both Address objects contain:

```text
street
city
postalCode
```

We can't have:

```text
street
city
postal_code
street
city
postal_code
```

in one table.

We need different column names.

---

# 11. Same Embeddable Twice

We can do:

```java
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
            name = "street",
            column = @Column(name = "home_street")
        ),
        @AttributeOverride(
            name = "city",
            column = @Column(name = "home_city")
        ),
        @AttributeOverride(
            name = "postalCode",
            column = @Column(name = "home_postal_code")
        )
    })
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
            name = "street",
            column = @Column(name = "work_street")
        ),
        @AttributeOverride(
            name = "city",
            column = @Column(name = "work_city")
        ),
        @AttributeOverride(
            name = "postalCode",
            column = @Column(name = "work_postal_code")
        )
    })
    private Address workAddress;
}
```

Database:

```text
customers
----------------------------------------------------
id

home_street
home_city
home_postal_code

work_street
work_city
work_postal_code
----------------------------------------------------
```

This is one of the best practical examples for understanding `@AttributeOverrides`.

---

# 12. `@AttributeOverride` vs `@AttributeOverrides`

Simple rule:

### One attribute

```java
@AttributeOverride(...)
```

### Multiple attributes

```java
@AttributeOverrides({
    @AttributeOverride(...),
    @AttributeOverride(...)
})
```

Think:

```text
@AttributeOverride
       ↓
      one

@AttributeOverrides
       ↓
     multiple
```

---

# 13. Embeddable Can Contain Another Embeddable

Embeddables can be composed.

For example:

```java
@Embeddable
public class Coordinates {

    private Double latitude;
    private Double longitude;
}
```

Then:

```java
@Embeddable
public class Address {

    private String street;
    private String city;

    @Embedded
    private Coordinates coordinates;
}
```

Then:

```java
@Entity
public class User {

    @Id
    private Long id;

    @Embedded
    private Address address;
}
```

Conceptually:

```text
User
 │
 └── Address
      │
      ├── street
      ├── city
      │
      └── Coordinates
           ├── latitude
           └── longitude
```

Database:

```text
users
--------------------------------
id
street
city
latitude
longitude
```

---

# 14. Nested Attribute Overrides

This can become more complicated.

Suppose:

```java
@Embeddable
class Coordinates {

    private Double latitude;
    private Double longitude;
}
```

and:

```java
@Embeddable
class Address {

    private String city;

    @Embedded
    private Coordinates coordinates;
}
```

Now an entity wants to override:

```text
coordinates.latitude
```

The attribute path can be used:

```java
@AttributeOverride(
    name = "coordinates.latitude",
    column = @Column(name = "lat")
)
```

The exact nesting and override rules can get complex, but the important interview concept is:

> Attribute overrides allow the owning entity to customize the column mapping of attributes inherited from an embeddable structure.

---

# 15. Embeddables as Value Objects

This is a very important domain-modeling concept.

Suppose:

```java
@Embeddable
public class Money {

    private BigDecimal amount;
    private String currency;
}
```

Then:

```java
@Entity
public class Product {

    @Id
    private Long id;

    @Embedded
    private Money price;
}
```

Conceptually:

```text
Product
   |
   └── Money
        ├── amount
        └── currency
```

Database:

```text
products
--------------------------------
id
price_amount
price_currency
```

The `Money` object isn't independently persisted.

It is part of the Product.

This is a classic **value object** pattern.

---

# 16. Entity vs Value Object

This distinction is extremely useful in interviews.

Imagine:

```text
Address
```

Should Address be an entity?

Ask:

> Does Address need an independent identity and lifecycle?

If yes:

```text
@Entity
Address
```

If no:

```text
@Embeddable
Address
```

For example:

### Value object

```text
Customer
   |
   └── Address
```

Address exists as part of Customer.

### Entity

```text
Customer
   |
   └── Address
          ↑
      independent
      identity
```

Maybe multiple customers reference the same address entity.

Then it might make sense to model it as an entity.

---

# 17. Lifecycle Difference

Suppose:

```java
@Embedded
private Address address;
```

The Address doesn't have its own independent persistence lifecycle.

If the Customer is deleted:

```text
Customer deleted
     ↓
Address disappears as part of Customer row
```

because the Address fields are columns in that row.

With:

```java
@Entity
class Address
```

you have:

```text
customer
address
```

and a relationship between them.

Now Address has an independent persistence identity and lifecycle.

---

# 18. Embeddable vs One-to-One

Another common interview scenario.

You have:

```text
User
Address
```

Should you use:

```java
@Embedded
private Address address;
```

or:

```java
@OneToOne
private Address address;
```

### `@Embedded`

Means:

```text
User table
--------------------------------
user_id
street
city
postal_code
```

### `@OneToOne`

Means:

```text
users
----------------
id
address_id


addresses
----------------
id
street
city
postal_code
```

Conceptually:

```text
@Embedded

User
 |
 +-- Address fields
       ↓
   same table
```

versus:

```text
@OneToOne

User
 |
 +----> Address
          ↓
     separate table
```

This is not simply a technical choice.

It's a **domain modeling decision**.

---

# 19. A Production Example

Consider an e-commerce application.

You might have:

```java
@Embeddable
public class Money {

    private BigDecimal amount;

    private String currency;
}
```

Then:

```java
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Embedded
    private Money price;
}
```

Database:

```text
product
------------------------------------------------
id
name
amount
currency
------------------------------------------------
```

Another entity:

```java
@Entity
public class Order {

    @Id
    private Long id;

    @Embedded
    private Money total;
}
```

Now both Product and Order reuse the same conceptual value type.

---

# 20. Important: Embeddable Does Not Mean "Reusable Table"

This is a common misunderstanding.

Wrong:

```text
@Embeddable
     ↓
Creates a reusable table
```

Correct:

```text
@Embeddable
     ↓
Defines a reusable group of persistent attributes
     ↓
Those attributes are stored in the owning entity's table
```

---

# 21. Interview Questions

## Q1. What is `@Embeddable`?

`@Embeddable` defines a class whose state can be embedded into an entity and persisted as part of the entity. It does not have an independent entity identity.

---

## Q2. What is `@Embedded`?

It tells JPA to embed an embeddable object's attributes into the owning entity's persistence mapping.

---

## Q3. Does an `@Embeddable` create a separate table?

**No.**

Its attributes are normally stored as columns of the owning entity's table.

---

## Q4. Can an embeddable have `@Id`?

Generally, no.

An embeddable does not represent an independent entity identity.

A composite ID class annotated with `@Embeddable` is a special case where it participates as the entity's identifier through `@EmbeddedId`.

---

## Q5. Why would you use `@AttributeOverride`?

To customize the column mapping of an attribute inherited from an embeddable.

Example:

```java
@AttributeOverride(
    name = "city",
    column = @Column(name = "home_city")
)
```

---

## Q6. Why use `@AttributeOverrides`?

When multiple attributes of an embeddable need custom mappings.

---

## Q7. Why might you use an embeddable instead of an entity?

When the object:

* Doesn't need independent identity
* Doesn't need an independent lifecycle
* Conceptually belongs to its owner
* Is naturally modeled as a value object

Examples:

```text
Money
Address
Coordinates
Name
DateRange
```

---

# 22. Important Interview Scenario

### Interviewer:

> We have `Customer` with `homeAddress` and `workAddress`. Both have `street`, `city`, and `postalCode`. How would you model this?

Good answer:

```java
@Embeddable
class Address {

    private String street;
    private String city;
    private String postalCode;
}
```

Then:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(
        name = "street",
        column = @Column(name = "home_street")
    ),
    @AttributeOverride(
        name = "city",
        column = @Column(name = "home_city")
    ),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "home_postal_code")
    )
})
private Address homeAddress;
```

and similarly for:

```java
private Address workAddress;
```

with `work_*` columns.

This demonstrates that you understand:

```text
@Embeddable
+
@Embedded
+
@AttributeOverrides
```

rather than just memorizing definitions.

---

# ⭐ Chapter 4 Mental Model

Remember:

```text
                 Java Model
                     │
                     ▼
              ┌─────────────┐
              │   Entity    │
              │   @Entity   │
              └──────┬──────┘
                     │
                     │ owns
                     ▼
              ┌─────────────┐
              │ Value Type  │
              │ @Embeddable │
              └──────┬──────┘
                     │
                     │ @Embedded
                     ▼
             Same entity table
```

And when the same value object appears multiple times:

```text
Address
  │
  ├── homeAddress
  │       ↓
  │   @AttributeOverrides
  │       ↓
  │   home_street
  │   home_city
  │
  └── workAddress
          ↓
      @AttributeOverrides
          ↓
      work_street
      work_city
```

### The key interview distinction

```text
@Entity
    → Has identity
    → Independent persistence lifecycle
    → Usually maps to its own table

@Embeddable
    → No independent identity
    → Part of another entity
    → Fields stored in owner's table
    → Excellent for value objects
```

**Next chapter:** **Entity Relationships** — `@OneToOne`, `@OneToMany`, `@ManyToOne`, and `@ManyToMany`. We'll spend significant time here because this is the most important and most commonly misunderstood part of JPA interviews: **owning side, foreign keys, `mappedBy`, join tables, and how the Java relationship translates into actual database tables.**


