# Chapter 7 — JPA Inheritance Mapping

Inheritance is where Java's object-oriented model:

```text
Animal
  |
  +---- Dog
  |
  +---- Cat
```

needs to be represented in a relational database.

The key problem is:

> **Java supports inheritance naturally, but relational databases don't have a direct inheritance concept.**

JPA gives us several strategies to bridge that gap.

---

# 1. The Basic Problem

Suppose we have:

```java
class Employee {
    Long id;
    String name;
}

class Developer extends Employee {
    String programmingLanguage;
}

class Manager extends Employee {
    int teamSize;
}
```

Object-oriented model:

```text
                 Employee
                 /      \
                /        \
         Developer      Manager
```

But what should the database look like?

We have several choices.

### Option 1 — One table

```text
EMPLOYEE
--------------------------------
id
name
programming_language
team_size
type
```

### Option 2 — Separate tables with joins

```text
EMPLOYEE
id
name

DEVELOPER
id
programming_language

MANAGER
id
team_size
```

### Option 3 — Separate complete tables

```text
DEVELOPER
id
name
programming_language

MANAGER
id
name
team_size
```

JPA supports these using inheritance strategies.

---

# 2. `@Inheritance`

The main annotation is:

```java
@Inheritance
```

Example:

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

This tells JPA:

> How should the inheritance hierarchy be represented in the database?

The three important strategies are:

```java
InheritanceType.SINGLE_TABLE
InheritanceType.JOINED
InheritanceType.TABLE_PER_CLASS
```

---

# 3. The Three Strategies

| Strategy          | Database design                        |
| ----------------- | -------------------------------------- |
| `SINGLE_TABLE`    | One table for entire hierarchy         |
| `JOINED`          | Parent + child tables                  |
| `TABLE_PER_CLASS` | Separate table for each concrete class |

Visual:

```text
SINGLE_TABLE

EMPLOYEE
+----+------+-------------+----------+
| id | name | language    | teamSize |
+----+------+-------------+----------+
```

```text
JOINED

EMPLOYEE
   |
   +---- DEVELOPER
   |
   +---- MANAGER
```

```text
TABLE_PER_CLASS

DEVELOPER
+----+------+----------+

MANAGER
+----+------+----------+
```

---

# 4. Strategy 1 — `SINGLE_TABLE`

This is usually the easiest strategy to understand.

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Developer:

```java
@Entity
public class Developer extends Employee {

    private String programmingLanguage;
}
```

Manager:

```java
@Entity
public class Manager extends Employee {

    private Integer teamSize;
}
```

Database:

```text
EMPLOYEE
+----+-------+----------------------+----------+
| id | name  | programming_language | teamSize |
+----+-------+----------------------+----------+
| 1  | John  | Java                 | NULL     |
| 2  | Alice | NULL                 | 10       |
+----+-------+----------------------+----------+
```

One table contains every type.

---

# 5. The Problem With `SINGLE_TABLE`

Notice:

```text
Developer
teamSize = NULL

Manager
programmingLanguage = NULL
```

There can be many nullable columns.

For example:

```text
EMPLOYEE
----------------------------------
id
name
programming_language
team_size
salary_band
sales_region
commission
...
```

As the hierarchy grows, the table can become wide.

But there is a major advantage.

### Performance

A query such as:

```java
employeeRepository.findAll();
```

can often use one table without joins.

```sql
SELECT *
FROM employee;
```

So:

```text
SINGLE_TABLE
    |
    +--> simple queries
    +--> fewer joins
    +--> generally good read performance
    |
    +--> potentially many NULL columns
```

---

# 6. Discriminator Column

How does Hibernate know whether:

```text
id = 1
```

is a:

```text
Developer
```

or:

```text
Manager
```

?

It uses a **discriminator column**.

You can explicitly configure it:

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "employee_type")
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Database:

```text
EMPLOYEE
+----+-------+--------------+----------------------+
| id | name  | employee_type| programming_language |
+----+-------+--------------+----------------------+
| 1  | John  | DEV          | Java                 |
| 2  | Alice | MGR          | NULL                 |
+----+-------+--------------+----------------------+
```

The discriminator tells Hibernate:

```text
DEV -> Developer
MGR -> Manager
```

---

# 7. `@DiscriminatorValue`

We can specify the discriminator value for each subclass.

```java
@Entity
@DiscriminatorValue("DEV")
public class Developer extends Employee {

    private String programmingLanguage;
}
```

And:

```java
@Entity
@DiscriminatorValue("MGR")
public class Manager extends Employee {

    private Integer teamSize;
}
```

Now:

```text
employee_type
----------------
DEV -> Developer
MGR -> Manager
```

---

# 8. Complete `SINGLE_TABLE` Example

### Parent

```java
@Entity
@Table(name = "employees")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "employee_type")
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

### Developer

```java
@Entity
@DiscriminatorValue("DEV")
public class Developer extends Employee {

    private String programmingLanguage;
}
```

### Manager

```java
@Entity
@DiscriminatorValue("MGR")
public class Manager extends Employee {

    private Integer teamSize;
}
```

Database:

```text
EMPLOYEES
+----+-------+--------------+----------------------+----------+
| id | name  | employee_type| programming_language | teamSize |
+----+-------+--------------+----------------------+----------+
| 1  | John  | DEV          | Java                 | NULL     |
| 2  | Alice | MGR          | NULL                 | 10       |
+----+-------+--------------+----------------------+----------+
```

---

# 9. Important Interview Point

The discriminator column is **not another Java property that you normally manipulate manually**.

Hibernate uses it internally to determine the entity subtype.

For example:

```text
employee_type = DEV
```

means:

```text
Hibernate creates Developer
```

rather than:

```text
Employee
```

---

# 10. Strategy 2 — `JOINED`

Now let's look at:

```java
@Inheritance(strategy = InheritanceType.JOINED)
```

Here the parent gets its own table.

```text
EMPLOYEE
+----+-------+
| id | name  |
+----+-------+

DEVELOPER
+----+----------------------+
| id | programming_language |
+----+----------------------+

MANAGER
+----+----------+
| id | teamSize |
+----+----------+
```

The child table's primary key is also a foreign key to the parent table.

---

# 11. `JOINED` Relationship

Conceptually:

```text
EMPLOYEE
---------
id PK
name
   ^
   |
   | FK
   |
DEVELOPER
---------
id PK/FK
programming_language
```

And:

```text
EMPLOYEE
---------
id PK
name
   ^
   |
   | FK
   |
MANAGER
---------
id PK/FK
team_size
```

So:

```text
Employee
   |
   +---- Developer
   |
   +---- Manager
```

is represented using actual relational tables.

---

# 12. `JOINED` Example

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

```java
@Entity
public class Developer extends Employee {

    private String programmingLanguage;
}
```

```java
@Entity
public class Manager extends Employee {

    private Integer teamSize;
}
```

Database:

```text
EMPLOYEE
+----+-------+
| id | name  |
+----+-------+
| 1  | John  |
| 2  | Alice |
+----+-------+

DEVELOPER
+----+--------+
| id | lang   |
+----+--------+
| 1  | Java   |
+----+--------+

MANAGER
+----+----------+
| id | teamSize |
+----+----------+
| 2  | 10       |
+----+----------+
```

---

# 13. How Does Hibernate Find a Developer?

It needs to join:

```sql
SELECT ...
FROM employee e
JOIN developer d
    ON e.id = d.id;
```

Therefore `JOINED` can require joins when retrieving subclass data.

Mental model:

```text
SINGLE_TABLE

SELECT
   |
   v
One table


JOINED

SELECT
   |
   +---- Employee
   |
   +---- Developer
```

---

# 14. Advantages of `JOINED`

### Less NULL-heavy

Developer-specific fields are in:

```text
DEVELOPER
```

Manager-specific fields are in:

```text
MANAGER
```

So we don't have:

```text
Developer.teamSize = NULL
Manager.programmingLanguage = NULL
```

all in one table.

### Better normalization

The schema more closely represents the inheritance hierarchy.

### Good when hierarchy is complex

It can be a good choice when:

* there are many subtype-specific fields
* avoiding a huge single table matters
* normalization is important

---

# 15. Disadvantages of `JOINED`

Queries can require joins.

For example:

```text
Employee
   |
   +--> Developer
   |
   +--> Manager
```

Fetching a complete hierarchy may require multiple joins.

So:

```text
JOINED
    |
    +--> normalized
    +--> fewer irrelevant NULLs
    |
    +--> more joins
```

---

# 16. Strategy 3 — `TABLE_PER_CLASS`

Now each concrete class gets its own table containing inherited fields.

```java
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Employee {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Developer:

```java
@Entity
public class Developer extends Employee {

    private String programmingLanguage;
}
```

Manager:

```java
@Entity
public class Manager extends Employee {

    private Integer teamSize;
}
```

Database:

```text
DEVELOPER
+----+-------+----------------------+
| id | name  | programming_language |
+----+-------+----------------------+

MANAGER
+----+-------+----------+
| id | name  | teamSize |
+----+-------+----------+
```

Notice:

```text
name
```

exists in both tables.

---

# 17. Why Is It Called `TABLE_PER_CLASS`?

Because each concrete entity class gets its own table:

```text
Developer -> DEVELOPER
Manager   -> MANAGER
```

The inherited properties are duplicated into the subclass tables.

---

# 18. The Problem With `TABLE_PER_CLASS`

Suppose you ask:

```java
employeeRepository.findAll();
```

You need employees from:

```text
DEVELOPER
+
MANAGER
```

Hibernate may need something conceptually similar to:

```sql
SELECT id, name, programming_language, ...
FROM developer

UNION ALL

SELECT id, name, NULL, ...
FROM manager;
```

So polymorphic queries can become more expensive.

---

# 19. Comparison of the Three Strategies

|                     | `SINGLE_TABLE`    | `JOINED`          | `TABLE_PER_CLASS`                          |
| ------------------- | ----------------- | ----------------- | ------------------------------------------ |
| Tables              | One               | Parent + children | One per concrete class                     |
| Joins               | Low               | Higher            | No inheritance joins, but unions may occur |
| NULL columns        | More              | Fewer             | Fewer                                      |
| Normalization       | Lower             | Good              | Lower due to duplication                   |
| Polymorphic queries | Usually efficient | Can require joins | Can require unions                         |
| Schema simplicity   | Excellent         | Moderate          | Moderate                                   |
| Common choice       | Very common       | Common            | Less common                                |

---

# 20. Which Strategy Should You Choose?

There isn't one universal answer.

### `SINGLE_TABLE`

Good when:

```text
Hierarchy is relatively simple
+
Read performance matters
+
Some nullable columns are acceptable
```

### `JOINED`

Good when:

```text
Hierarchy has significant subtype-specific data
+
Schema normalization matters
+
Joins are acceptable
```

### `TABLE_PER_CLASS`

Useful in specific models, but less commonly preferred when polymorphic queries are important.

---

# 21. `@MappedSuperclass`

Now comes a very important distinction.

Suppose you have:

```text
BaseEntity
   |
   +---- User
   |
   +---- Order
   |
   +---- Product
```

But you don't want `BaseEntity` itself to be an entity.

You simply want to reuse fields:

```java
id
createdAt
updatedAt
```

This is where:

```java
@MappedSuperclass
```

comes in.

---

# 22. Example `@MappedSuperclass`

```java
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    private Instant createdAt;

    private Instant updatedAt;
}
```

Then:

```java
@Entity
public class User extends BaseEntity {

    private String name;
}
```

And:

```java
@Entity
public class Product extends BaseEntity {

    private String name;
}
```

---

# 23. What Database Does This Create?

There is **no `BASE_ENTITY` table**.

Instead:

```text
USER
+----+------+-----------+-----------+
| id | name | created_at| updated_at|
+----+------+-----------+-----------+
```

and:

```text
PRODUCT
+----+------+-----------+-----------+
| id | name | created_at| updated_at|
+----+------+-----------+-----------+
```

The fields are inherited into the entity tables.

---

# 24. `@MappedSuperclass` vs `@Inheritance`

This is one of the most important interview distinctions.

### `@Inheritance`

Represents an **entity inheritance hierarchy**.

Example:

```text
Employee
   |
   +---- Developer
   |
   +---- Manager
```

All are part of one polymorphic entity hierarchy.

### `@MappedSuperclass`

Represents **field/property reuse**.

Example:

```text
BaseEntity
   |
   +---- User
   |
   +---- Product
   |
   +---- Order
```

`BaseEntity` isn't an entity that you query polymorphically.

---

# 25. Can You Query a `@MappedSuperclass`?

Suppose:

```java
@MappedSuperclass
class BaseEntity {
    ...
}
```

You cannot normally do:

```java
entityManager.find(BaseEntity.class, id);
```

because:

> `BaseEntity` isn't an entity.

Instead:

```java
entityManager.find(User.class, id);
```

or:

```java
entityManager.find(Product.class, id);
```

---

# 26. Very Important Mental Model

Think:

```text
@Inheritance
```

as:

> **"These entities are different types of the same entity hierarchy."**

Whereas:

```text
@MappedSuperclass
```

means:

> **"These entities just share common persistent fields."**

---

# 27. Example Comparison

### Entity inheritance

```text
                    Employee
                   /        \
                  /          \
            Developer       Manager
```

You can conceptually ask:

```java
List<Employee> employees;
```

and get:

```text
Developer
Manager
Developer
Manager
```

That's polymorphism.

---

### Mapped superclass

```text
                  BaseEntity
                 /    |     \
                /     |      \
             User   Order   Product
```

But:

```text
BaseEntity
```

is not a real entity type in the persistence model.

It's primarily a mapping reuse mechanism.

---

# 28. Another Interview Trap

Can you do this?

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@MappedSuperclass
class Employee {
}
```

Generally, these represent different mapping concepts and should not be combined like this.

You choose the model based on whether the base class is:

```text
an actual entity hierarchy
```

or:

```text
just a common mapped base class.
```

---

# 29. `@DiscriminatorColumn`

Used primarily with inheritance strategies where a discriminator is needed, especially `SINGLE_TABLE`.

Example:

```java
@DiscriminatorColumn(
    name = "employee_type",
    discriminatorType = DiscriminatorType.STRING
)
```

Possible types include:

```java
STRING
CHAR
INTEGER
```

For example:

```text
employee_type
-------------
DEV
MGR
```

---

# 30. `@DiscriminatorValue`

Defines the discriminator value for a subclass.

```java
@Entity
@DiscriminatorValue("DEV")
class Developer extends Employee {
}
```

```java
@Entity
@DiscriminatorValue("MGR")
class Manager extends Employee {
}
```

Mapping:

```text
DEV -> Developer
MGR -> Manager
```

---

# 31. What Happens If You Don't Specify `@DiscriminatorValue`?

The provider can use its default discriminator-value behavior.

The exact default is provider/spec dependent in details, so in production systems, explicitly defining meaningful values can make the database easier to understand.

For example:

```java
@DiscriminatorValue("DEV")
```

is much clearer than relying on generated/default values.

---

# 32. A Real-World Example

Imagine a payment system:

```text
Payment
   |
   +---- CardPayment
   |
   +---- BankTransferPayment
   |
   +---- WalletPayment
```

Common:

```text
id
amount
created_at
status
```

Card:

```text
card_last_four
```

Bank transfer:

```text
bank_reference
```

Wallet:

```text
wallet_provider
```

With `SINGLE_TABLE`:

```text
PAYMENT
----------------------------------------------------
id
amount
status
payment_type
card_last_four
bank_reference
wallet_provider
```

Example:

```text
id | type   | amount | card_last_four | bank_reference
--------------------------------------------------------
1  | CARD   | 100    | 1234           | NULL
2  | BANK   | 200    | NULL           | ABC123
```

This can be perfectly reasonable if the hierarchy is relatively simple.

---

# 33. Interview Question: Which Strategy Has the Best Performance?

Don't answer:

> `SINGLE_TABLE` is always fastest.

Better answer:

> `SINGLE_TABLE` often has simpler and faster polymorphic reads because everything is in one table and inheritance joins aren't required, but the actual performance depends on data volume, indexes, query patterns, row width, and workload.

That's a much stronger senior-level answer.

---

# 34. Interview Question: Which Strategy Is Most Normalized?

Generally:

```text
JOINED
```

is more normalized because common attributes live in the parent table and subtype-specific attributes live in child tables.

Compare:

```text
JOINED

EMPLOYEE
id
name

DEVELOPER
id
language
```

versus:

```text
TABLE_PER_CLASS

DEVELOPER
id
name
language

MANAGER
id
name
teamSize
```

where:

```text
name
```

is duplicated.

---

# 35. Interview Question: Which Strategy Avoids NULLs?

`JOINED` and `TABLE_PER_CLASS` generally avoid the subtype-specific NULL columns characteristic of `SINGLE_TABLE`.

But they achieve this differently:

```text
JOINED
    |
    +--> separate subtype tables

TABLE_PER_CLASS
    |
    +--> complete table per concrete class
```

---

# 36. Interview Question: Why Might `TABLE_PER_CLASS` Be Problematic?

Because common inherited fields are duplicated.

And polymorphic queries such as:

```java
List<Employee> employees = repository.findAll();
```

may require combining multiple subclass tables.

Conceptually:

```sql
SELECT ...
FROM developer

UNION ALL

SELECT ...
FROM manager;
```

As the hierarchy grows, this can become cumbersome.

---

# 37. Interview Question: `@MappedSuperclass` or `@Inheritance`?

A strong answer:

> I use `@MappedSuperclass` when I only want to reuse persistent fields such as `id`, `createdAt`, and `updatedAt`, and the base class isn't itself an entity. I use `@Inheritance` when the classes represent a genuine entity hierarchy where polymorphic persistence and queries are required.

---

# 38. Common Mistakes

### Mistake 1

Thinking Java inheritance automatically tells JPA how to create tables.

It doesn't.

You need to choose an inheritance mapping strategy when using entity inheritance.

---

### Mistake 2

Confusing:

```java
@MappedSuperclass
```

with:

```java
@Inheritance
```

They solve different problems.

---

### Mistake 3

Thinking `SINGLE_TABLE` means:

> "Every subclass must have every field."

No.

All columns exist in the table, but subtype-specific columns can be `NULL` for other subtypes.

---

### Mistake 4

Thinking `JOINED` means:

> One table per Java class, completely independent.

No.

The child table references the parent table through the inherited primary key.

---

### Mistake 5

Thinking `TABLE_PER_CLASS` creates a parent table.

It generally doesn't create a table for the abstract parent.

The inherited fields are placed into the concrete subclass tables.

---

# 39. Quick Visual Summary

```text
             JPA INHERITANCE
                    |
        +-----------+-----------+
        |           |           |
        v           v           v
 SINGLE_TABLE     JOINED    TABLE_PER_CLASS
        |           |           |
        v           v           v
     One table   Parent +    Separate table
                 child       per concrete class
```

And:

```text
             COMMON FIELDS ONLY?
                     |
                     v
              @MappedSuperclass
```

---

# 40. Interview Cheat Sheet

### `@Inheritance`

Controls inheritance mapping strategy.

```java
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
```

---

### `SINGLE_TABLE`

```text
One hierarchy
     |
     v
One table
```

Pros:

* simple
* efficient polymorphic reads
* fewer joins

Cons:

* nullable subtype columns
* potentially wide table

---

### `JOINED`

```text
Parent table
     |
     +---- Child table
```

Pros:

* normalized
* fewer irrelevant NULL columns

Cons:

* joins
* potentially more complex queries

---

### `TABLE_PER_CLASS`

```text
Developer table
Manager table
```

Pros:

* independent concrete tables
* no parent-child join for simple subtype reads

Cons:

* duplicated columns
* polymorphic queries can require unions

---

### `@DiscriminatorColumn`

Identifies subtype in discriminator-based mappings.

```text
employee_type = DEV
```

---

### `@DiscriminatorValue`

Maps subtype to discriminator value.

```text
DEV -> Developer
MGR -> Manager
```

---

### `@MappedSuperclass`

Common mapping fields, **not an entity hierarchy**.

```text
BaseEntity
    |
    +--> User
    +--> Order
    +--> Product
```

No `BASE_ENTITY` table.

---

# 41. The Most Important Interview Mental Model

If the interviewer gives you:

```text
Employee
   |
   +---- Developer
   |
   +---- Manager
```

ask:

### Question 1

Are these actually different entity types?

If yes:

```text
@Inheritance
```

### Question 2

Do I just need common fields?

If yes:

```text
@MappedSuperclass
```

### Question 3

If this is real entity inheritance, how should the DB represent it?

Choose:

```text
SINGLE_TABLE
JOINED
TABLE_PER_CLASS
```

Then discuss:

```text
performance
normalization
NULL columns
joins
polymorphic queries
data volume
```

That is the kind of reasoning interviewers generally want—not just memorizing the annotation names.

---

## Chapter 7 complete

**Next: Chapter 8 — Advanced Entity Mapping**

We'll cover:

```text
@SecondaryTable
@SecondaryTables
@AttributeOverride
@AttributeOverrides
@AssociationOverride
```

and especially **how one entity can span multiple database tables**, how this differs from inheritance, and when these mappings make sense in real production systems.


