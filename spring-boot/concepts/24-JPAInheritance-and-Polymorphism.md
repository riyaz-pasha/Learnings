# Topic 24 — JPA Inheritance & Polymorphism

Now we move into another important JPA interview area:

> **How do you map Java inheritance into relational database tables?**

Java naturally supports:

```java id="u4n5zv"
class Vehicle { }

class Car extends Vehicle { }

class Bike extends Vehicle { }
```

But relational databases don't have a direct equivalent of:

```text
extends
```

So JPA needs to decide:

> **How should this Java class hierarchy be represented in tables?**

Jakarta Persistence defines three inheritance mapping strategies:

```text id="0t84tv"
SINGLE_TABLE
JOINED
TABLE_PER_CLASS
```

and also provides:

```java id="8x2bpr"
@MappedSuperclass
```

which is **not itself an entity inheritance strategy**. It is a way of reusing mapped state across entities without mapping the superclass to its own table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritance), [jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

# 1. Start with the Java hierarchy

Let's use:

```java id="1v4j1o"
@Entity
public class Payment {

    @Id
    @GeneratedValue
    private Long id;

    private BigDecimal amount;
}
```

Then:

```java id="4m8b6c"
@Entity
public class CreditCardPayment extends Payment {

    private String cardLastFour;
}
```

and:

```java id="b8sy3t"
@Entity
public class BankTransferPayment extends Payment {

    private String accountNumber;
}
```

Our Java model is:

```text id="f2y11r"
                    Payment
                       |
              +--------+--------+
              |                 |
              v                 v
      CreditCardPayment   BankTransferPayment
```

Now ask:

> How many database tables?

There are three main answers.

---

# 2. Strategy #1 — `SINGLE_TABLE`

Everything goes into **one table**.

```java id="4hswcm"
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Payment {
    ...
}
```

Database:

```text id="wqujmu"
PAYMENT
------------------------------------------------
id
amount
card_last_four
account_number
dtype
```

Rows:

```text id="u0p6hf"
id   amount   card_last_four   account_number   dtype
------------------------------------------------------
1    100      1234             NULL             CARD
2    250      NULL             ACC998          BANK
3    75       5555             NULL             CARD
```

The discriminator column tells Hibernate which Java subtype a row represents.

Jakarta Persistence defines `SINGLE_TABLE` as one table for the entire hierarchy, with a discriminator column identifying the concrete entity represented by each row. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritancetype))

---

# 3. Discriminator column

For single-table inheritance, you commonly see:

```java id="zsf97b"
@DiscriminatorColumn(name = "payment_type")
```

and:

```java id="3jvi7n"
@Entity
@DiscriminatorValue("CARD")
public class CreditCardPayment
        extends Payment {
}
```

Then:

```java id="87kwft"
@Entity
@DiscriminatorValue("BANK")
public class BankTransferPayment
        extends Payment {
}
```

The table becomes:

```text id="c41i7z"
PAYMENT
--------------------------------
id
amount
card_last_four
account_number
payment_type
```

with:

```text id="x2z20e"
CARD → CreditCardPayment
BANK → BankTransferPayment
```

Hibernate documents discriminator columns and values as the mechanism used to identify concrete subclasses in a single-table hierarchy. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 4. Do I have to specify `SINGLE_TABLE`?

No.

This is important.

If you have an entity inheritance hierarchy and don't specify an inheritance strategy, the default is:

```java id="d8h0mw"
InheritanceType.SINGLE_TABLE
```

Jakarta Persistence explicitly defines `SINGLE_TABLE` as the default strategy. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritance))

So:

```java id="4jkrsm"
@Entity
public class Payment {
}
```

with subclasses is conceptually:

```text id="qne3gc"
SINGLE_TABLE
```

unless another strategy is specified.

---

# 5. Why is `SINGLE_TABLE` popular?

Because it's simple.

Suppose:

```text id="a7r8xg"
Payment
 ├── CreditCardPayment
 ├── BankTransferPayment
 └── CashPayment
```

Database:

```text id="39dzrs"
PAYMENT
```

Everything is in one table.

A query over the hierarchy can be efficient because:

```text id="wl2b7h"
SELECT
    FROM one table
```

rather than joining multiple subclass tables.

Jakarta Persistence describes `SINGLE_TABLE` as providing good support for polymorphic relationships and queries over the hierarchy. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

Hibernate likewise notes that retrieving instances from a single-table hierarchy requires querying only the single table. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 6. The biggest disadvantage of `SINGLE_TABLE`

Consider:

```text id="q7yb5x"
CreditCardPayment
    card_last_four

BankTransferPayment
    account_number
```

A single table contains both:

```text id="kct6un"
card_last_four
account_number
```

But a credit-card row doesn't need:

```text id="jaq5du"
account_number
```

and a bank-transfer row doesn't need:

```text id="gf91b4"
card_last_four
```

So you end up with:

```text id="7mcj0v"
CARD row:
card_last_four = 1234
account_number = NULL
```

and:

```text id="k4mju4"
BANK row:
card_last_four = NULL
account_number = ACC998
```

As subclasses increase, the table can become very wide and sparse.

Jakarta Persistence identifies this as the major drawback of single-table inheritance: subclass-specific columns generally need to permit nulls. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 7. `SINGLE_TABLE` diagram

```text id="h5oie2"
                    Payment
                       |
              +--------+--------+
              |                 |
              v                 v
             Card             Bank


                    DATABASE

              +-------------------------+
              |        PAYMENT          |
              +-------------------------+
              | id                      |
              | amount                  |
              | card_last_four          |
              | account_number          |
              | payment_type            |
              +-------------------------+
```

Simple.

---

# 8. Strategy #2 — `JOINED`

Now consider:

```java id="q9xyx0"
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Payment {
}
```

The superclass gets its own table:

```text id="j9vtzw"
PAYMENT
------------
id
amount
```

Then:

```text id="h4gk2m"
CREDIT_CARD_PAYMENT
-------------------
id
card_last_four
```

and:

```text id="5x3z7f"
BANK_TRANSFER_PAYMENT
---------------------
id
account_number
```

The subclass table's primary key also acts as a foreign key to the parent table.

Jakarta Persistence defines `JOINED` exactly this way: each class gets a table for its declared persistent attributes, and the subclass table's primary key serves as the foreign key to the superclass table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritancetype))

---

# 9. Database representation

For:

```text id="efm9m8"
Payment
  |
  +---- CreditCardPayment
  |
  +---- BankTransferPayment
```

we get:

```text id="m3zj1f"
PAYMENT
----------------
id PK
amount


CREDIT_CARD_PAYMENT
-------------------
id PK/FK
card_last_four


BANK_TRANSFER_PAYMENT
---------------------
id PK/FK
account_number
```

A card payment:

```text id="l3zv7k"
Payment#10
```

is represented across:

```text id="1kqk5c"
PAYMENT              CREDIT_CARD_PAYMENT
------               -------------------
10                   10
100                  1234
```

So Hibernate joins these rows to reconstruct:

```java id="fvx6f7"
CreditCardPayment
```

---

# 10. Why is `JOINED` called normalized?

Because common fields live once:

```text id="w8q0zk"
PAYMENT
-------
amount
```

rather than being copied into every subclass table.

Compare:

### SINGLE_TABLE

```text
PAYMENT
-------
amount
card_last_four
account_number
```

### JOINED

```text
PAYMENT
-------
amount

CARD
----
card_last_four

BANK
----
account_number
```

The second model is more normalized.

Jakarta Persistence describes the joined strategy as providing good support for polymorphism and being well normalized. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

Hibernate similarly identifies `JOINED` as the normalized strategy and notes that it is particularly attractive when database constraints and normalization matter. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 11. The disadvantage of `JOINED`

Suppose you query:

```java id="mg4r1y"
List<Payment> payments =
        paymentRepository.findAll();
```

Hibernate needs to determine which subtype each row represents and retrieve subclass-specific state.

That can require joins.

For example:

```sql id="jn0w73"
SELECT ...
FROM payment p
LEFT JOIN credit_card_payment c
    ON p.id = c.id
LEFT JOIN bank_transfer_payment b
    ON p.id = b.id;
```

The exact SQL is provider-generated, but the basic idea is:

```text id="9e1ie3"
Superclass data
+
subclass data
=
JOIN
```

Jakarta Persistence explicitly identifies joins as the main disadvantage of `JOINED`, especially in deep hierarchies. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 12. Deep inheritance hierarchy

Suppose:

```text id="m6b0pm"
Payment
   |
   v
ElectronicPayment
   |
   v
CardPayment
   |
   v
CreditCardPayment
```

With `JOINED`:

```text id="7ndzq8"
PAYMENT
   JOIN
ELECTRONIC_PAYMENT
   JOIN
CARD_PAYMENT
   JOIN
CREDIT_CARD_PAYMENT
```

The deeper the hierarchy, the more joins can be needed.

So:

```text id="ar8t6r"
JOINED
+
deep hierarchy
=
potentially expensive polymorphic queries
```

Jakarta Persistence explicitly calls out this performance concern. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 13. Strategy #3 — `TABLE_PER_CLASS`

Now:

```java id="22or4c"
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Payment {
}
```

Each **concrete** entity class gets its own table containing inherited fields too.

So:

```text id="px0aey"
CREDIT_CARD_PAYMENT
-------------------
id
amount
card_last_four


BANK_TRANSFER_PAYMENT
---------------------
id
amount
account_number
```

There is no common `PAYMENT` table.

Jakarta Persistence defines `TABLE_PER_CLASS` as a separate table for each concrete class, with inherited attributes included in each concrete table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritancetype))

---

# 14. Why duplicate columns?

Suppose:

```java id="m2v5kx"
Payment {
    amount;
}
```

and:

```java id="d6n5g4"
CreditCardPayment {
    cardLastFour;
}
```

The table is:

```text id="y4h5e7"
CREDIT_CARD_PAYMENT
-------------------
id
amount
card_last_four
```

And:

```text id="o6jjh4"
BANK_TRANSFER_PAYMENT
---------------------
id
amount
account_number
```

So:

```text id="5xmgpf"
amount
```

is duplicated across tables.

That's the denormalization tradeoff.

---

# 15. Polymorphic queries become difficult

Suppose:

```java id="zlm95z"
List<Payment> payments =
        paymentRepository.findAll();
```

How does the database query:

```text id="4h2st4"
CREDIT_CARD_PAYMENT
+
BANK_TRANSFER_PAYMENT
```

as one polymorphic result?

Conceptually it needs:

```sql id="rx9qfl"
SELECT ... FROM credit_card_payment
UNION ALL
SELECT ... FROM bank_transfer_payment;
```

The exact SQL/provider behavior can vary, but a polymorphic query requires combining the concrete tables.

Jakarta Persistence explicitly identifies `UNION` or multiple queries as the cost of querying across the hierarchy with `TABLE_PER_CLASS`. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 16. Why `TABLE_PER_CLASS` is less common

It has several disadvantages:

```text id="9py3ku"
duplicate inherited columns
+
poor polymorphic relationships
+
UNION-like queries
+
difficulty enforcing FKs to a superclass
```

Hibernate's documentation notes that polymorphic associations targeting the superclass are problematic because the target could correspond to multiple concrete tables. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

Jakarta Persistence 4.0's current development specification also notes that `TABLE_PER_CLASS` support isn't required for provider portability in that release. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

So:

> **Don't choose `TABLE_PER_CLASS` casually.**

---

# 17. Comparison of the three

This table is worth memorizing:

|                       | `SINGLE_TABLE`        | `JOINED`                | `TABLE_PER_CLASS`                      |
| --------------------- | --------------------- | ----------------------- | -------------------------------------- |
| Tables                | One                   | One per hierarchy class | One per concrete class                 |
| Inherited columns     | Same table            | Parent table            | Copied into child tables               |
| Discriminator         | Required conceptually | Not required            | Not used in Hibernate's implementation |
| Normalization         | Lower                 | High                    | Lower                                  |
| Polymorphic query     | Simple                | Requires joins          | Requires UNION-like combination        |
| Null subclass columns | Common                | Avoided                 | Avoided                                |
| Typical complexity    | Low                   | Medium                  | Higher                                 |
| Typical tradeoff      | Wide table            | Joins                   | Duplication + polymorphism cost        |

The Jakarta Persistence specification defines the three strategies and their core tradeoffs; Hibernate additionally documents the practical performance/constraint implications. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 18. The most important interview comparison

If asked:

> **SINGLE_TABLE vs JOINED**

say:

```text id="mmhnnj"
SINGLE_TABLE
    ↓
one table
+
fast/simple polymorphic queries
+
no subclass joins
-
wide/sparse table
-
nullable subclass columns
```

while:

```text id="71xrn2"
JOINED
    ↓
normalized tables
+
strong column constraints
+
less duplication
-
joins for subclass retrieval
-
deep hierarchies can become expensive
```

That's a strong answer.

---

# 19. `@DiscriminatorColumn`

This is generally associated with single-table inheritance.

Example:

```java id="5l8f79"
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "payment_type",
    discriminatorType = DiscriminatorType.STRING
)
public class Payment {
}
```

Now:

```text id="2evv6g"
payment_type
```

can contain:

```text id="zd74f5"
CARD
BANK
```

The discriminator identifies the concrete subclass represented by the row. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/discriminatorcolumn))

---

# 20. `@DiscriminatorValue`

Then:

```java id="k0y8r3"
@Entity
@DiscriminatorValue("CARD")
public class CreditCardPayment
        extends Payment {
}
```

and:

```java id="e42m9n"
@Entity
@DiscriminatorValue("BANK")
public class BankTransferPayment
        extends Payment {
}
```

So:

```text id="m0a8b6"
payment_type = CARD
        ↓
CreditCardPayment
```

The exact default discriminator value can be provider/specification dependent if you don't specify one, so explicitly defining values is often useful for a controlled schema.

---

# 21. Can `JOINED` use a discriminator?

Yes.

A common misconception is:

> "`JOINED` always has a discriminator."

No.

The discriminator is **not required** for `JOINED`.

Hibernate's current guide explicitly says a discriminator is not required for joined inheritance, although one can optionally be used and can simplify certain polymorphic SQL. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

So:

```text id="2qvzkf"
SINGLE_TABLE
    ↓
discriminator required

JOINED
    ↓
discriminator optional

TABLE_PER_CLASS
    ↓
Hibernate doesn't use discriminator
```

---

# 22. `@MappedSuperclass`

Now we need to distinguish this from entity inheritance.

Suppose:

```java id="wajgh6"
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime createdAt;
}
```

Then:

```java id="a8q0o4"
@Entity
public class User extends BaseEntity {

    private String name;
}
```

and:

```java id="zzl2vv"
@Entity
public class Order extends BaseEntity {

    private BigDecimal total;
}
```

There is **no `BASE_ENTITY` table**.

Instead:

```text id="62b6sr"
USER
----------------
id
created_at
name
```

and:

```text id="2slw9p"
ORDERS
----------------
id
created_at
total
```

Jakarta Persistence explicitly defines `@MappedSuperclass` as a class whose mappings are inherited by entity subclasses, while the superclass itself isn't an entity and isn't mapped to its own table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

# 23. `@MappedSuperclass` vs `@Entity`

This distinction is crucial.

### `@Entity`

```java id="r1tsmg"
@Entity
class Payment
```

means:

```text id="m4sxcp"
Payment itself
can be a persistent entity
```

### `@MappedSuperclass`

```java id="sgf8ex"
@MappedSuperclass
class BaseEntity
```

means:

```text id="ls3yyp"
BaseEntity itself
isn't an entity/table

its mappings are inherited by entities
```

So:

```text id="m70xj9"
@Entity
   ↓
persistent identity/table/entity type

@MappedSuperclass
   ↓
mapping reuse only
```

---

# 24. `@MappedSuperclass` isn't polymorphic

Suppose:

```java id="5zlpiv"
@MappedSuperclass
abstract class BaseEntity {
    Long id;
}
```

You cannot normally do:

```java id="8bz11m"
repository<BaseEntity>
```

because:

```text id="bwf0ha"
BaseEntity
```

isn't itself an entity.

You also don't have:

```java id="2w6b9y"
@Entity
class BaseEntity
```

with polymorphic queries.

This is the key:

> **`@MappedSuperclass` is for shared mapping, not for an entity hierarchy.**

Jakarta Persistence explicitly states that a mapped superclass is not a persistent type. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

# 25. The classic BaseEntity pattern

You'll often see:

```java id="8vgsxg"
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    protected Long id;

    protected LocalDateTime createdAt;

    protected LocalDateTime updatedAt;
}
```

Then:

```java id="vo7dsn"
@Entity
public class User extends BaseEntity {

    private String name;
}
```

and:

```java id="e0o21u"
@Entity
public class Product extends BaseEntity {

    private BigDecimal price;
}
```

Database:

```text id="9ry8iy"
USER
----------------
id
created_at
updated_at
name


PRODUCT
----------------
id
created_at
updated_at
price
```

Common fields are inherited into each entity's table.

That's exactly what mapped superclasses are good at.

---

# 26. `@MappedSuperclass` vs `SINGLE_TABLE`

Suppose:

```text id="h0b7d7"
Person
  |
  +---- Employee
  +---- Customer
```

If `Person` is:

```java id="3tob2u"
@Entity
@Inheritance(strategy = SINGLE_TABLE)
```

then:

```text id="nq9v5h"
PERSON
```

exists as the common table.

But if `Person` is:

```java id="2c6j8d"
@MappedSuperclass
```

then:

```text id="0zf12e"
PERSON table
```

doesn't exist.

Instead:

```text id="lrj4v8"
EMPLOYEE
CUSTOMER
```

each contains the inherited fields.

This distinction is absolutely fundamental.

---

# 27. `@MappedSuperclass` doesn't support polymorphic relationships

Suppose:

```java id="vwh5vu"
@MappedSuperclass
class BaseEntity
```

Then another entity cannot meaningfully have:

```java id="1r9iyj"
@ManyToOne
BaseEntity entity;
```

as a polymorphic association in the way an entity superclass can be targeted.

Because:

```text id="eck1sq"
BaseEntity
```

isn't itself an entity/table target.

This is an important interview distinction.

---

# 28. Entity inheritance is polymorphic

Suppose:

```java id="51i3sx"
@Entity
class Payment {
}
```

and:

```java id="2dv8cg"
@Entity
class CardPayment extends Payment {
}
```

A query:

```java id="2q1mr7"
select p from Payment p
```

is polymorphic.

It can return:

```text id="5g4zq0"
Payment
CardPayment
BankPayment
...
```

Jakarta Persistence explicitly states that a query against an inherited entity class is polymorphic and can return instances of inheriting entity classes. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 29. Polymorphic associations

Suppose:

```java id="ddx3u5"
@Entity
class PaymentRequest {

    @ManyToOne
    private Payment payment;
}
```

If `Payment` has subclasses:

```text id="4u4m9b"
CardPayment
BankPayment
```

then the association can refer to either.

Jakarta Persistence describes associations targeting an inherited entity class as inherently polymorphic. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This is one reason:

```text id="vc1b4n"
@Entity superclass
```

is conceptually different from:

```text id="ke7tp8"
@MappedSuperclass
```

---

# 30. `TABLE_PER_CLASS` and polymorphic associations

Now see the problem.

Suppose:

```text id="k7t5cw"
CREDIT_CARD_PAYMENT
-------------------
id
amount
card_last_four

BANK_PAYMENT
------------
id
amount
account_number
```

Where would this foreign key go?

```java id="u6xgl8"
@ManyToOne
private Payment payment;
```

There isn't one common:

```text id="6k4j7v"
PAYMENT
```

table.

The target could be:

```text id="2w7rlz"
CREDIT_CARD_PAYMENT
```

or:

```text id="q8y3i6"
BANK_PAYMENT
```

A single ordinary relational FK can't reference multiple unrelated target tables.

Hibernate's guide explicitly calls this a major weakness of `TABLE_PER_CLASS`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 31. Strategy selection is a database design decision

Don't decide based purely on Java inheritance.

Ask:

```text id="bfj8a0"
How many subclasses?

How many subclass-specific fields?

How frequently do we query polymorphically?

How important are NOT NULL constraints?

How wide can the table become?

How deep is the hierarchy?

How important is normalization?

Are polymorphic associations required?
```

Then choose.

This is exactly the kind of reasoning interviewers are looking for.

---

# 32. When `SINGLE_TABLE` makes sense

Suppose:

```text id="xw5gu8"
Payment
 ├── CardPayment
 ├── BankPayment
 └── CashPayment
```

and each subclass only has a couple of extra attributes.

Then:

```text id="0k8efl"
SINGLE_TABLE
```

can be attractive because:

```text id="l3srdg"
simple schema
+
simple polymorphic queries
+
no joins
```

Hibernate's current guide notes that single-table works particularly well when subclasses add few or no attributes. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 33. When `JOINED` makes sense

Suppose:

```text id="dl95r3"
Document
 ├── Invoice
 ├── Contract
 ├── PurchaseOrder
```

and each subclass has many distinct fields.

A giant single table could become:

```text id="n9pvn5"
DOCUMENT
---------
invoice_no
contract_term
purchase_order_no
...
```

with lots of nulls.

`JOINED` may make the model cleaner:

```text id="l8r5q4"
DOCUMENT
INVOICE
CONTRACT
PURCHASE_ORDER
```

It is particularly attractive where:

```text id="5q2sq5"
normalization
+
constraints
+
clean subtype tables
```

matter.

Hibernate documents joined inheritance as a good fit when constraints and normalization are priorities. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 34. When `TABLE_PER_CLASS` might make sense

It can make sense if:

```text id="xb2qcb"
polymorphic queries are rare
+
each concrete type is queried independently
+
duplicated inherited columns are acceptable
```

For example, maybe:

```text id="jvf4op"
ReportA
ReportB
ReportC
```

share a Java abstraction but almost never need:

```text id="0tcrh8"
query all Reports
```

Even then, consider whether `@MappedSuperclass` is actually the simpler design.

---

# 35. `@MappedSuperclass` may be the better choice

Suppose you have:

```text id="7zqypk"
User
Product
Invoice
Order
```

They all need:

```text id="ts45ko"
id
createdAt
updatedAt
```

but they have **no true domain inheritance relationship**.

Don't create:

```text id="7fr9q0"
Entity
   |
   +-- User
   +-- Product
   +-- Invoice
   +-- Order
```

just for code reuse.

Use:

```java id="iyc5sy"
@MappedSuperclass
abstract class AuditableEntity {
    ...
}
```

This communicates:

> These entities share persistent fields, not a polymorphic business hierarchy.

That's much cleaner.

---

# 36. Don't use inheritance just to avoid duplication

For example:

```java id="rbn0ni"
@MappedSuperclass
class BaseEntity {

    Long id;
    Instant createdAt;
}
```

is reasonable.

But:

```text id="wb3d4f"
Animal
Dog
Cat
```

as actual domain entities makes sense when:

```text id="m91f3x"
Dog IS-A Animal
Cat IS-A Animal
```

Inheritance should represent a real conceptual relationship.

---

# 37. Entity inheritance vs composition

Another interview-level design question:

> "Should I use JPA inheritance here?"

Not automatically.

Sometimes:

```text id="y7lf2e"
inheritance
```

is less flexible than:

```text id="73jk1d"
composition
```

For example:

```text id="c51hbr"
Order
   |
   +---- ShippingDetails
```

may be better than:

```text id="pn7ufb"
SpecialOrder
InternationalOrder
DomesticOrder
```

depending on whether those concepts are really subtypes.

This is a domain-modeling decision before it's a JPA decision.

---

# 38. A tricky question: abstract entity

You can have:

```java id="j50d1c"
@Entity
public abstract class Payment {
}
```

It can participate in the inheritance hierarchy and be referenced in polymorphic mappings/queries even though you don't instantiate it directly.

Jakarta Persistence explicitly permits abstract entity classes to participate in relationships and appear in the query `FROM` clause. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

So:

```text id="p3v8bv"
abstract
    ≠
not an entity
```

That's another good interview nuance.

---

# 39. `@Entity` abstract superclass vs `@MappedSuperclass`

These are very different.

### Abstract `@Entity`

```java id="kw2qbm"
@Entity
@Inheritance(strategy = JOINED)
abstract class Payment
```

means:

```text id="r6m05h"
Payment participates in persistence
+
inheritance mapping
+
polymorphic queries
+
may have table depending on strategy
```

### `@MappedSuperclass`

```java id="m5s74j"
@MappedSuperclass
abstract class BaseEntity
```

means:

```text id="am3o7o"
no entity identity of its own
+
no table
+
no polymorphic queries
+
mapping reuse
```

Memorize this comparison.

---

# 40. Accessing subclass-specific fields

Suppose:

```java id="jfw1xi"
Payment payment = ...
```

and actual object is:

```text id="0b31gp"
CreditCardPayment
```

You can use polymorphism:

```java id="gx6u2n"
if (payment instanceof CreditCardPayment card) {
    card.getCardLastFour();
}
```

But generally, if your application constantly needs:

```text id="zprxd1"
instanceof
```

to decide what subtype it's handling, question whether the domain model or API abstraction is appropriate.

That's a design smell worth recognizing.

---

# 41. Discriminator values are implementation data

Suppose:

```text id="1tq5jz"
payment_type = "CARD"
```

Don't generally expose:

```text id="2x36fb"
CARD
BANK
```

as a database implementation detail throughout your application.

The Java model already knows:

```text id="0we4db"
CreditCardPayment
BankTransferPayment
```

The discriminator is primarily a persistence concern.

This is another reason to use domain types rather than passing discriminator strings around.

---

# 42. Inheritance and relationships

Suppose:

```java id="odq5gw"
@Entity
class Payment {
}
```

and:

```java id="l9s0tx"
@ManyToOne
private Payment payment;
```

This can point to:

```text id="aa2s0z"
CreditCardPayment
```

or:

```text id="asw7du"
BankTransferPayment
```

This is a:

> **polymorphic association**

Jakarta Persistence explicitly defines an association targeting an inherited entity class as polymorphic. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 43. `SINGLE_TABLE` polymorphic association

This is straightforward.

```text id="vev5r0"
PAYMENT
----------------
id
amount
type
card_last_four
account_number
```

One FK:

```text id="e4b6f4"
PAYMENT_REQUEST.payment_id
       ↓
PAYMENT.id
```

The discriminator tells Hibernate whether the target is:

```text id="cgj1tw"
CreditCardPayment
```

or:

```text id="beqnv2"
BankTransferPayment
```

---

# 44. `JOINED` polymorphic association

Still straightforward conceptually:

```text id="af1bk0"
PAYMENT
   ↑
   |
CARD_PAYMENT

PAYMENT
   ↑
   |
BANK_PAYMENT
```

A foreign key points to:

```text id="h7ffxo"
PAYMENT.id
```

and Hibernate determines the concrete subtype using the hierarchy.

---

# 45. `TABLE_PER_CLASS` polymorphic association

Now:

```text id="a4uyp9"
CARD_PAYMENT
BANK_PAYMENT
```

No common target table.

A foreign key to:

```text id="8c4j1d"
Payment
```

can't directly reference both.

That's one of the reasons `TABLE_PER_CLASS` provides poor polymorphic association support. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 46. Important: only the root specifies `@Inheritance`

Suppose:

```java id="l3mmj7"
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Payment {
}
```

Then:

```java id="2p4hk5"
@Entity
public class CardPayment extends Payment {
}
```

You generally do **not** put:

```java id="5gwqg1"
@Inheritance(...)
```

on `CardPayment`.

The inheritance strategy is specified on the **root entity of the hierarchy**. Jakarta Persistence explicitly requires this mapping strategy to be defined at the root. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritance))

---

# 47. A common interview trap

Question:

> "Can I use different inheritance strategies for each subclass?"

For portable Jakarta Persistence, no—mixing strategies within one hierarchy isn't generally supported/required.

The inheritance mapping strategy applies to the hierarchy from the root. The current Jakarta Persistence specification explicitly says support for mixing strategies within a hierarchy is not required. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 48. Inheritance and ID generation

The root of an entity hierarchy still needs an appropriate primary-key mapping.

With:

```java id="g2szv8"
@Id
@GeneratedValue
private Long id;
```

the ID uniquely identifies entity instances within the hierarchy.

With `JOINED`, the same ID is used across the corresponding superclass/subclass rows:

```text id="phkcc5"
PAYMENT
id = 10

CARD_PAYMENT
id = 10
```

The subclass primary key doubles as the foreign key to the superclass table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritancetype))

---

# 49. Why `JOINED` can enforce stronger constraints

Suppose:

```text id="4x8r33"
CreditCardPayment.cardLastFour
```

must not be null.

With `JOINED`:

```text id="49h3pm"
CARD_PAYMENT
-----------------
id
card_last_four NOT NULL
```

That constraint is natural.

With `SINGLE_TABLE`:

```text id="c6h5g9"
PAYMENT
------------------
card_last_four
```

the database cannot simply declare:

```sql id="yy3t3n"
card_last_four NOT NULL
```

because bank-payment rows don't need it.

Jakarta Persistence highlights this exact constraint tradeoff between `SINGLE_TABLE` and `JOINED`. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 50. Hibernate's discriminator optimization for `JOINED`

An advanced point:

`JOINED` doesn't necessarily require the same discriminator approach as `SINGLE_TABLE`.

Hibernate says a discriminator can optionally be added to `JOINED`, and this may simplify generated SQL for polymorphic queries. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

This is provider-specific optimization territory.

For interviews:

```text id="n4siwa"
JOINED does not require discriminator.
```

is the important fact.

---

# 51. Inheritance + lazy loading

Suppose:

```java id="lno6lh"
@ManyToOne(fetch = FetchType.LAZY)
private Payment payment;
```

and the actual payment is a subclass.

Hibernate may use proxying/instrumentation and determine the subtype as necessary.

This is another area where:

```text id="3q2yj2"
inheritance
+
proxy
+
lazy loading
```

can interact.

Don't assume that runtime JPA behavior is identical to:

```text id="4y9z88"
new CreditCardPayment()
```

because the provider can use proxies and other mechanisms.

---

# 52. Inheritance and queries

Suppose:

```java id="pzyy5s"
List<Payment> payments =
        entityManager
            .createQuery(
                "select p from Payment p",
                Payment.class
            )
            .getResultList();
```

This is polymorphic.

The result can contain:

```text id="w40ajw"
Payment
CreditCardPayment
BankTransferPayment
CashPayment
```

Jakarta Persistence explicitly requires queries against an entity superclass to be polymorphic. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

The database implementation depends on the selected inheritance strategy.

---

# 53. Querying only one subclass

You can query:

```java id="6xr2eo"
select p
from CreditCardPayment p
```

Now Hibernate can focus on the concrete subclass.

With:

```text id="r6h4it"
SINGLE_TABLE
```

it can use:

```text id="3g6vau"
payment_type = 'CARD'
```

With:

```text id="c7v3cf"
JOINED
```

it can query the relevant joined tables.

With:

```text id="r3is2x"
TABLE_PER_CLASS
```

it can query the concrete table directly.

So the inheritance strategy affects the physical SQL required by polymorphic queries.

---

# 54. Inheritance and indexes

Suppose `SINGLE_TABLE` uses:

```text id="h5xljk"
payment_type
```

and you frequently query:

```sql id="6qkl2o"
WHERE payment_type = 'CARD'
```

An index on the discriminator may potentially help depending on the workload/database.

Similarly, subclass-specific search columns may need indexes:

```text id="mh9i5x"
card_last_four
account_number
```

Remember:

> JPA inheritance strategy doesn't remove ordinary database-performance considerations.

---

# 55. The real-world decision

Imagine your hierarchy:

```text id="q3gyj2"
Vehicle
 ├── Car
 ├── Truck
 ├── Motorcycle
```

### Option A — SINGLE_TABLE

```text id="8kpyqy"
VEHICLE
----------------
id
type
car_doors
truck_capacity
bike_engine_cc
```

Good:

```text id="v6c6vq"
simple
fast polymorphic queries
```

Bad:

```text id="yqj6uy"
many nullable subtype fields
```

---

### Option B — JOINED

```text id="se4y5x"
VEHICLE
CAR
TRUCK
MOTORCYCLE
```

Good:

```text id="jt7mvx"
normalized
clean constraints
```

Bad:

```text id="tw3d0v"
joins
```

---

### Option C — TABLE_PER_CLASS

```text id="fyl7k8"
CAR
TRUCK
MOTORCYCLE
```

Good:

```text id="g3b9fj"
simple concrete-table queries
```

Bad:

```text id="a4yksv"
duplicated columns
poor polymorphic associations
UNION-style polymorphic queries
```

---

# 56. The interview question: "Which strategy is fastest?"

Don't answer:

> `SINGLE_TABLE`.

A better answer:

> It depends on the access patterns and hierarchy. `SINGLE_TABLE` can make polymorphic reads simple because only one table is involved, while `JOINED` can incur joins but gives better normalization and constraints. `TABLE_PER_CLASS` can make concrete-type access straightforward but makes polymorphic queries and associations more difficult.

That's an experienced answer.

---

# 57. The interview question: "Which is the most normalized?"

Generally:

```text id="3c5qq5"
JOINED
```

because inherited attributes aren't duplicated across concrete subclass tables.

Hibernate explicitly describes `JOINED` as normalized compared with the denormalization of `SINGLE_TABLE` and `TABLE_PER_CLASS`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 58. The interview question: "Why does SINGLE_TABLE need nullable subclass columns?"

Because:

```text id="jyyf7j"
one table
+
different subclasses
```

means columns for subtype-specific attributes exist in rows belonging to other subtypes.

For example:

```text id="hcf1ac"
CARD
→ account_number = NULL

BANK
→ card_last_four = NULL
```

Jakarta Persistence explicitly identifies this as the principal schema drawback of single-table inheritance. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 59. The interview question: "Why might JOINED be slow?"

Because reconstructing:

```text id="voa8lf"
Subclass
```

can require:

```text id="eq2gib"
Superclass JOIN subclass
```

and a deep hierarchy can require multiple joins.

Jakarta Persistence explicitly notes this as a disadvantage. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 60. The interview question: "Why can TABLE_PER_CLASS be problematic?"

Because:

```text id="1wlx4o"
inherited fields duplicated
+
polymorphic queries need UNION/multiple queries
+
polymorphic FK relationships are problematic
```

The persistence specification and Hibernate documentation both describe these drawbacks. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 61. The interview question: "`@MappedSuperclass` vs `@Inheritance`?"

Excellent question.

Answer:

> `@Inheritance` is for a persistent entity hierarchy where the superclass and subclasses participate in polymorphic persistence. `@MappedSuperclass` is for reusing mapped fields/properties across multiple entities; the mapped superclass itself is not an entity and has no table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

# 62. The interview question: "Does `@MappedSuperclass` create a table?"

No.

```text id="5sk7c3"
@MappedSuperclass
    ↓
NO table
```

The fields are mapped into the subclass's table. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

# 63. The interview question: "Does an abstract `@Entity` have a table?"

That depends on the inheritance strategy.

For example:

```java id="x1s5la"
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Payment
```

has a superclass table in the `JOINED` strategy because the class's persistent state is mapped to its own table.

But:

```java id="h2j6ky"
@MappedSuperclass
abstract class BaseEntity
```

never gets its own table.

So don't use:

```text id="v1fu4k"
abstract
```

as the distinction.

The distinction is:

```text id="8w8fwi"
@Entity inheritance mapping
vs
@MappedSuperclass
```

---

# 64. Practical example — audit fields

For:

```text id="xcx7hi"
User
Product
Invoice
Order
```

you probably don't have:

```text id="9o0kvi"
User IS-A BaseEntity
Product IS-A BaseEntity
```

in domain terms.

Rather:

```text id="z59l3v"
all have audit fields
```

So:

```java id="mg9sdy"
@MappedSuperclass
abstract class AuditableEntity {
    ...
}
```

is a natural fit.

---

# 65. Practical example — payment hierarchy

For:

```text id="ha4kyf"
Payment
 ├── CreditCardPayment
 ├── BankTransferPayment
 └── CashPayment
```

there is a true:

```text id="z7s0o2"
IS-A
```

relationship.

Therefore:

```java id="0e32b0"
@Entity
@Inheritance(...)
abstract class Payment
```

can make sense.

Then choose the database strategy based on:

```text id="k0o9ef"
query patterns
schema constraints
number of subtype fields
normalization
performance
```

---

# 66. Don't use inheritance just because Java supports it

This is an important software-design lesson.

Java inheritance:

```text id="ahmxz4"
extends
```

doesn't automatically mean:

```text id="4h5r8b"
JPA inheritance should be used.
```

Ask:

> Is this actually an entity hierarchy in the domain?

If not, composition or a mapped superclass may be better.

---

# 67. Complete example — `SINGLE_TABLE`

```java id="2q0w9g"
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
}
```

```java id="b8qqj0"
@Entity
@DiscriminatorValue("CARD")
public class CreditCardPayment
        extends Payment {

    private String cardLastFour;
}
```

```java id="w3f49z"
@Entity
@DiscriminatorValue("BANK")
public class BankTransferPayment
        extends Payment {

    private String accountNumber;
}
```

Database:

```text id="nl8zvc"
PAYMENT
------------------------------------------------
id | amount | payment_type | card_last_four | account_number
```

---

# 68. Complete example — `JOINED`

```java id="j0d8q3"
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
}
```

```java id="m7mfx4"
@Entity
public class CreditCardPayment
        extends Payment {

    private String cardLastFour;
}
```

```java id="g93z3s"
@Entity
public class BankTransferPayment
        extends Payment {

    private String accountNumber;
}
```

Database:

```text id="lqqjgc"
PAYMENT
---------
id
amount

CREDIT_CARD_PAYMENT
-------------------
id FK
card_last_four

BANK_TRANSFER_PAYMENT
---------------------
id FK
account_number
```

No discriminator is required.

---

# 69. Complete example — `TABLE_PER_CLASS`

```java id="p4k1d6"
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Payment {

    @Id
    @GeneratedValue
    private Long id;

    private BigDecimal amount;
}
```

Then:

```text id="3y50c9"
CREDIT_CARD_PAYMENT
-------------------
id
amount
card_last_four

BANK_TRANSFER_PAYMENT
---------------------
id
amount
account_number
```

Inherited state is duplicated into each concrete table.

---

# 70. Complete example — `@MappedSuperclass`

```java id="x3z8ty"
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    protected Long id;

    protected Instant createdAt;
}
```

Then:

```java id="r5ixou"
@Entity
public class User extends BaseEntity {
    private String name;
}
```

and:

```java id="8w4n9r"
@Entity
public class Product extends BaseEntity {
    private BigDecimal price;
}
```

Database:

```text id="knu1oa"
USER
---------
id
created_at
name

PRODUCT
---------
id
created_at
price
```

No:

```text id="6m6q8c"
BASE_ENTITY
```

table.

---

# 71. One subtle point: `@MappedSuperclass` can be overridden

Mapped-superclass mappings can be overridden in subclasses using:

```java id="zq3r4e"
@AttributeOverride
```

and:

```java id="aw2r9l"
@AssociationOverride
```

Jakarta Persistence explicitly supports overriding mapped-superclass mappings in the entity subclasses. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

Example:

```java id="v5f6s0"
@AttributeOverride(
    name = "createdAt",
    column = @Column(name = "created_timestamp")
)
```

Useful to know, but secondary to the main distinction.

---

# 72. Inheritance strategy cheat sheet

### `SINGLE_TABLE`

```text id="7a4k7s"
Java:
Payment
 ├── Card
 └── Bank

DB:
PAYMENT
```

Think:

```text id="4o5x72"
ONE TABLE
+
DISCRIMINATOR
+
FAST SIMPLE POLYMORPHISM
-
WIDE/NULLABLE
```

---

### `JOINED`

```text id="4n4cpm"
Java:
Payment
 ├── Card
 └── Bank

DB:
PAYMENT
CARD
BANK
```

Think:

```text id="6iy1a6"
NORMALIZED
+
GOOD CONSTRAINTS
-
JOINS
```

---

### `TABLE_PER_CLASS`

```text id="3ces87"
Java:
Payment
 ├── Card
 └── Bank

DB:
CARD
BANK
```

Think:

```text id="3tmc1x"
NO COMMON TABLE
+
DUPLICATED INHERITED COLUMNS
+
UNION FOR POLYMORPHISM
-
POLYMORPHIC ASSOCIATIONS
```

---

### `@MappedSuperclass`

```text id="nqf03u"
Java:
BaseEntity
 ├── User
 └── Product

DB:
USER
PRODUCT
```

Think:

```text id="kq4iik"
SHARED MAPPING
NOT AN ENTITY
NO TABLE
NO POLYMORPHISM
```

---

# 73. The decision tree

When you see inheritance, ask:

```text id="83j3cq"
Is the superclass a real domain entity?
              |
          +---+---+
          |       |
         NO      YES
          |       |
          v       v
 @MappedSuperclass
                  |
                  v
         Need polymorphic persistence?
                  |
                  v
             @Inheritance
                  |
       +----------+----------+
       |          |          |
       v          v          v
 SINGLE_TABLE  JOINED   TABLE_PER_CLASS
```

Then evaluate:

```text id="4b2xjo"
subclass field count
query patterns
constraints
normalization
polymorphic relationships
data volume
```

---

# 74. What EPAM interviewers really want

They aren't primarily testing whether you remember:

```java id="r5snj1"
InheritanceType.JOINED
```

They want you to explain:

> **Why would you choose JOINED rather than SINGLE_TABLE?**

Your answer should involve:

```text id="pr1n4f"
schema shape
query patterns
constraints
normalization
join cost
subclass count
polymorphic queries
```

That's much more impressive than naming annotations.

---

# 75. The most important interview traps

### Trap 1

> `@MappedSuperclass` is an entity superclass.

No.

It's a mapping-reuse superclass, not a persistent entity itself. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

### Trap 2

> `JOINED` requires discriminator.

No.

A discriminator isn't required for `JOINED`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

### Trap 3

> `TABLE_PER_CLASS` has one common parent table.

No.

There is no common superclass table in that strategy.

---

### Trap 4

> `SINGLE_TABLE` duplicates inherited columns.

No.

Everything is in one table.

---

### Trap 5

> `JOINED` is always slower.

No.

It requires joins, but actual performance depends on queries, indexes, hierarchy depth, data volume, and database behavior.

---

### Trap 6

> `TABLE_PER_CLASS` is always invalid.

No.

It is a legitimate mapping strategy in JPA/Hibernate, but has important portability and polymorphism tradeoffs. Current Jakarta Persistence 4.0 development documentation notes that support for it is not required for provider portability, so don't treat it as universally portable. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 76. Final architecture picture

```text id="0i5lcv"
                      JAVA MODEL

                       Payment
                          |
                +---------+---------+
                |                   |
                v                   v
          CardPayment          BankPayment


                  PERSISTENCE OPTIONS
                         
        ┌────────────────┬─────────────────┬──────────────────┐
        │                │                 │                  │
        v                v                 v                  v
   SINGLE_TABLE       JOINED         TABLE_PER_CLASS   MAPPED_SUPERCLASS
        │                │                 │                  │
        v                v                 v                  v
     PAYMENT       PAYMENT +        CARD + BANK         no superclass
                   CARD + BANK                           table
        │                │                 │
   discriminator      PK/FK          duplicated base
        │                │              attributes
        v                v                 v
    polymorphic       joins             UNION-like
       query          needed              query
```

---

# 77. The core relationship between inheritance and polymorphism

This is the key:

```text id="7epkxm"
@Entity superclass
        ↓
persistent inheritance hierarchy
        ↓
polymorphic queries
        ↓
polymorphic associations
        ↓
database mapping strategy
```

Whereas:

```text id="p8tz36"
@MappedSuperclass
        ↓
mapping reuse only
        ↓
no entity polymorphism
        ↓
no superclass table
```

---

# 78. What you should know cold for EPAM

You should now be able to explain:

```text id="w1tq9x"
@Entity inheritance
@Inheritance

SINGLE_TABLE
JOINED
TABLE_PER_CLASS

@DiscriminatorColumn
@DiscriminatorValue

@MappedSuperclass

abstract @Entity
vs
@MappedSuperclass

polymorphic queries
polymorphic associations

SINGLE_TABLE:
    one table
    discriminator
    nullable subtype columns

JOINED:
    table per class
    PK = FK
    normalized
    joins

TABLE_PER_CLASS:
    table per concrete class
    inherited columns duplicated
    UNION-like polymorphic queries
    poor polymorphic FK support

root entity and @Inheritance
inheritance strategy selection
constraints vs normalization vs performance
```

---

# The interview answer to memorize

> **JPA supports three main inheritance strategies. `SINGLE_TABLE` stores the whole hierarchy in one table and uses a discriminator, giving simple polymorphic queries but potentially producing a wide table with nullable subclass columns. `JOINED` stores each class in its own table and links subclass tables to the superclass by primary-key foreign keys, giving a normalized schema at the cost of joins. `TABLE_PER_CLASS` gives each concrete class its own table containing inherited state, but duplicates columns and makes polymorphic queries and associations more difficult. `@MappedSuperclass` is different: it provides reusable mapping metadata to entity subclasses but is not itself an entity and does not have its own table.** ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/inheritance), [jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass))

---

## Topic 24 revision drill

Take this hierarchy:

```text
Payment
 ├── CreditCardPayment
 ├── BankTransferPayment
 └── CashPayment
```

Be able to draw the database for **all four**:

```text
1. SINGLE_TABLE
2. JOINED
3. TABLE_PER_CLASS
4. @MappedSuperclass
```

Then answer:

```text
5. Which one needs a discriminator?
6. Which one gives the most normalized schema?
7. Why can SINGLE_TABLE have many NULL columns?
8. Why does JOINED require joins?
9. Why does TABLE_PER_CLASS complicate polymorphic queries?
10. Why is TABLE_PER_CLASS problematic for polymorphic foreign keys?
11. Is @MappedSuperclass an entity?
12. Can you query @MappedSuperclass directly?
13. Can @MappedSuperclass participate in polymorphic associations?
14. Where does @Inheritance go?
15. What is the default inheritance strategy?
16. Can JOINED have a discriminator?
17. Why might you choose SINGLE_TABLE over JOINED?
18. Why might you choose JOINED over SINGLE_TABLE?
19. When would @MappedSuperclass be better than entity inheritance?
20. When would composition be better than inheritance?
```

**Next topic: JPA Cascades, Orphan Removal & Entity Graph Design in depth — `PERSIST`, `MERGE`, `REMOVE`, `DETACH`, `REFRESH`, `ALL`, orphan lifecycle, delete behavior, aggregate boundaries, and the cascade bugs that commonly appear in production.**

