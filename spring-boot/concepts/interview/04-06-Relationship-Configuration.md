# Chapter 6 — JPA Relationship Configuration

Chapter 5 taught us **what kind of relationship exists**:

```text
@OneToOne
@OneToMany
@ManyToOne
@ManyToMany
```

Now we need to answer the more important production questions:

* Who owns the relationship?
* Where is the foreign key?
* What does `mappedBy` really do?
* When do we use `@JoinColumn`?
* When do we use `@JoinTable`?
* What happens when an entity is deleted?
* What does `cascade` actually mean?
* What is `orphanRemoval`?
* What does `fetch` mean?
* Why does lazy loading sometimes cause exceptions?
* How do N+1 queries happen?

---

# 1. The Complete Mental Model

Consider:

```text
Customer 1 -------- * Order
```

Database:

```text
CUSTOMER
+----+-------+
| id | name  |
+----+-------+

ORDER
+----+-------------+
| id | customer_id |
+----+-------------+
```

Java:

```java
class Customer {
    List<Order> orders;
}

class Order {
    Customer customer;
}
```

JPA has to answer:

```text
1. Which Java field represents the relationship?
2. Which table contains the FK?
3. Which side controls the relationship?
4. How should inserts/updates/deletes behave?
5. When should the related entity be loaded?
```

The annotations in this chapter answer these questions.

---

# 2. `@JoinColumn`

Let's start with the most common case.

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

This says:

```text
Order.customer
       |
       v
orders.customer_id
       |
       v
customers.id
```

So:

```java
@JoinColumn(name = "customer_id")
```

means:

> Use `customer_id` as the foreign-key column for this association.

---

# 3. `@JoinColumn` Attributes

A common example:

```java
@ManyToOne
@JoinColumn(
    name = "customer_id",
    nullable = false
)
private Customer customer;
```

Important attributes include:

```java
@JoinColumn(
    name = "customer_id",
    nullable = false,
    unique = true
)
```

### `name`

Database column:

```text
customer_id
```

### `nullable`

Whether the FK can be `NULL`.

```java
nullable = false
```

means:

```text
ORDER.customer_id IS NOT NULL
```

### `unique`

Useful for enforcing uniqueness.

For example, a one-to-one FK:

```java
@OneToOne
@JoinColumn(
    name = "profile_id",
    unique = true
)
private UserProfile profile;
```

This ensures one profile isn't referenced by multiple users.

---

# 4. `mappedBy` vs `@JoinColumn`

This is a very common interview comparison.

### Owning side

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

This says:

> I define how the relationship is stored.

### Inverse side

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

This says:

> The `Order.customer` field defines how this relationship is stored.

So:

```text
                    Relationship

Customer                         Order
   |                               |
   | orders                        | customer
   |                               |
   +------------ SAME -------------+
                 |
                 v
          customer_id FK
```

---

# 5. `mappedBy` Does Not Mean "Mapped By Column"

Suppose:

```java
class Order {

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

Then:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

Correct.

Notice:

```text
mappedBy = "customer"
```

not:

```text
mappedBy = "customer_id"
```

Because `mappedBy` refers to the **Java association property**.

---

# 6. `@JoinTable`

`@JoinTable` is used when the relationship needs an intermediate table.

Most common example:

```text
Student * -------- * Course
```

Database:

```text
STUDENT
   |
   |
   v
STUDENT_COURSE
   ^
   |
   |
COURSE
```

Mapping:

```java
@ManyToMany
@JoinTable(
    name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
private Set<Course> courses;
```

---

# 7. Understanding `joinColumns` and `inverseJoinColumns`

This looks complicated:

```java
@JoinTable(
    name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
```

Just remember:

```text
Student
   |
   | student_id
   v
STUDENT_COURSE
   ^
   | course_id
   |
Course
```

`joinColumns`:

> FK pointing to the entity on this side.

`inverseJoinColumns`:

> FK pointing to the other entity.

---

# 8. `mappedBy` in Many-to-Many

Student:

```java
@ManyToMany
@JoinTable(
    name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
private Set<Course> courses;
```

Course:

```java
@ManyToMany(mappedBy = "courses")
private Set<Student> students;
```

Only one side defines:

```java
@JoinTable
```

The other side says:

```java
mappedBy = "courses"
```

Therefore:

```text
Student.courses
      |
      | OWNING SIDE
      v
student_course
      ^
      |
Course.students
      |
      | INVERSE SIDE
```

---

# 9. Cascade — One of the Most Important Concepts

Now suppose:

```text
Customer
   |
   +---- Order
   +---- Order
   +---- Order
```

You save a Customer.

Should JPA automatically save its Orders?

Or delete a Customer.

Should JPA automatically delete its Orders?

This is what **cascade** controls.

---

# 10. What Does Cascade Mean?

Cascade means:

> Propagate certain entity lifecycle operations from one entity to its related entities.

Example:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.PERSIST
)
private List<Order> orders;
```

If:

```java
entityManager.persist(customer);
```

then JPA can also persist the orders.

Conceptually:

```text
persist(customer)
       |
       v
Customer
       |
       | CASCADE PERSIST
       v
Orders
```

---

# 11. Cascade Types

JPA provides:

```java
CascadeType.PERSIST
CascadeType.MERGE
CascadeType.REMOVE
CascadeType.REFRESH
CascadeType.DETACH
CascadeType.ALL
```

Let's understand each.

---

# 12. `CascadeType.PERSIST`

```java
cascade = CascadeType.PERSIST
```

When parent is persisted:

```java
entityManager.persist(parent);
```

the persist operation cascades to the relationship.

Example:

```java
Customer customer = new Customer();

Order order = new Order();

customer.addOrder(order);

entityManager.persist(customer);
```

With:

```java
cascade = CascadeType.PERSIST
```

the new order can also be persisted.

---

# 13. `CascadeType.MERGE`

Suppose you have a detached entity:

```text
Database
   |
   v
Entity
   |
   v
detached
```

Then:

```java
entityManager.merge(customer);
```

With:

```java
cascade = CascadeType.MERGE
```

the merge operation can cascade to associated entities.

Important interview distinction:

```text
persist -> new entity
merge   -> detached/entity state synchronization
```

---

# 14. `CascadeType.REMOVE`

This is potentially dangerous.

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.REMOVE
)
private List<Order> orders;
```

Now:

```java
entityManager.remove(customer);
```

can cause:

```text
DELETE Customer
       |
       | cascade REMOVE
       v
DELETE Order 1
DELETE Order 2
DELETE Order 3
```

This is appropriate when the child has no independent meaning outside the parent.

---

# 15. `CascadeType.ALL`

```java
cascade = CascadeType.ALL
```

means:

```text
PERSIST
MERGE
REMOVE
REFRESH
DETACH
```

all cascade.

It's convenient:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.ALL
)
private List<Order> orders;
```

But don't blindly use it.

### Interview answer

> `CascadeType.ALL` is not automatically best practice. Cascade should reflect ownership and lifecycle semantics.

---

# 16. Cascade Does NOT Mean Database Cascade

This distinction is important.

JPA cascade:

```java
cascade = CascadeType.REMOVE
```

is an **ORM-level behavior**.

Database cascade:

```sql
ON DELETE CASCADE
```

is a **database-level constraint behavior**.

They are different mechanisms.

```text
JPA Cascade
    |
    v
Hibernate/JPA
    |
    v
SQL statements


DB Cascade
    |
    v
Database
    |
    v
Automatically handles FK-dependent rows
```

You can use either or both depending on your architecture.

---

# 17. `orphanRemoval`

Now imagine:

```text
Customer
   |
   +---- Order 1
   +---- Order 2
```

Suppose Java code does:

```java
customer.getOrders().remove(order1);
```

What should happen to `order1` in the database?

Should it remain?

Or should it be deleted?

`orphanRemoval` answers this.

---

# 18. Basic `orphanRemoval`

```java
@OneToMany(
    mappedBy = "customer",
    orphanRemoval = true
)
private List<Order> orders;
```

If an order is removed from the parent's collection and becomes an orphan, JPA can delete it.

Conceptually:

```text
Before:

Customer
   |
   +---- Order 1
   +---- Order 2


remove(Order 1)


After:

Customer
   |
   +---- Order 2
```

And database:

```text
DELETE FROM orders
WHERE id = 1;
```

---

# 19. `orphanRemoval` vs `CascadeType.REMOVE`

This is a **very common interview question**.

### `CascadeType.REMOVE`

Triggered when the parent is removed:

```java
remove(customer)
```

Conceptually:

```text
remove Customer
       |
       v
remove Orders
```

### `orphanRemoval`

Triggered when the child is removed from the parent's relationship:

```java
customer.getOrders().remove(order);
```

Conceptually:

```text
remove Order from collection
       |
       v
Order becomes orphan
       |
       v
DELETE Order
```

---

# 20. Important Difference

```text
CascadeType.REMOVE
        |
        v
Parent is deleted
        |
        v
Child is deleted
```

while:

```text
orphanRemoval
        |
        v
Child is disconnected/removed from parent
        |
        v
Child is deleted
```

They solve related but different problems.

---

# 21. `orphanRemoval` Usually Makes Sense for Composition

Consider:

```text
Order
  |
  +---- OrderItem
  +---- OrderItem
  +---- OrderItem
```

An `OrderItem` probably has no independent business meaning outside its Order.

Therefore:

```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<OrderItem> items;
```

is a common design.

But:

```text
Employee ---- Department
```

should usually **not** use:

```java
orphanRemoval = true
```

because removing an employee from a department shouldn't mean:

> Delete the employee from the database.

---

# 22. Cascade + Orphan Removal

A very common aggregate-style mapping:

```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<OrderItem> items = new ArrayList<>();
```

This means roughly:

```text
Order lifecycle
      |
      +---- persist
      |       |
      |       v
      |   OrderItems persisted
      |
      +---- remove
      |       |
      |       v
      |   OrderItems removed
      |
      +---- remove item from collection
              |
              v
         OrderItem deleted
```

This is useful when `OrderItem` is truly owned by `Order`.

---

# 23. Don't Use `CascadeType.REMOVE` Blindly

Suppose:

```text
Student * ----- * Course
```

and you write:

```java
@ManyToMany(cascade = CascadeType.ALL)
private Set<Course> courses;
```

Potentially:

```text
Delete Student
      |
      v
Delete Course
```

That can be disastrous.

Why?

Because the course may belong to many other students.

```text
Student A ----+
              |
Student B ----+---- Course Java
              |
Student C ----+
```

Deleting Student A should not delete:

```text
Course Java
```

for everyone else.

### Production rule

Be very cautious with `CascadeType.REMOVE` on:

```text
@ManyToMany
```

and shared entities.

---

# 24. Fetching

Now another major concept:

```java
fetch = FetchType.LAZY
```

or:

```java
fetch = FetchType.EAGER
```

This controls **when associated data is loaded**.

---

# 25. LAZY

Suppose:

```java
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;
```

You load:

```java
Order order = repository.findById(10L);
```

Hibernate may initially load only:

```sql
SELECT *
FROM orders
WHERE id = 10;
```

The Customer isn't necessarily fetched immediately.

Later:

```java
order.getCustomer().getName();
```

Hibernate may execute another query:

```sql
SELECT *
FROM customer
WHERE id = ?;
```

Conceptually:

```text
Load Order
    |
    v
Order loaded
    |
    | customer not initialized yet
    v
Customer proxy/reference
    |
    |
getCustomer().getName()
    |
    v
SELECT Customer
```

---

# 26. EAGER

With:

```java
@ManyToOne(fetch = FetchType.EAGER)
private Customer customer;
```

the provider is instructed that the association should be eagerly fetched.

Conceptually:

```text
Load Order
    |
    +---- Order
    |
    +---- Customer
```

The exact SQL strategy can vary by provider/query, so don't equate EAGER strictly with "one SQL JOIN."

That's an important interview nuance.

---

# 27. Default Fetch Types

JPA defaults are:

| Relationship  | Default |
| ------------- | ------- |
| `@ManyToOne`  | `EAGER` |
| `@OneToOne`   | `EAGER` |
| `@OneToMany`  | `LAZY`  |
| `@ManyToMany` | `LAZY`  |

This is frequently asked in interviews.

A common production preference is to explicitly think through fetching rather than relying blindly on defaults.

---

# 28. Why Is EAGER Dangerous?

Imagine:

```text
Order
 |
 +---- Customer
 |
 +---- Payment
 |
 +---- ShippingAddress
 |
 +---- OrderItems
       |
       +---- Product
```

If many relationships are eager:

```text
Order
  |
  +--> Customer
  +--> Payment
  +--> Address
  +--> Items
         |
         +--> Product
```

Loading one entity can cause a large object graph to be loaded.

This can result in:

* unnecessary database queries
* large result sets
* memory usage
* complicated joins
* performance problems

Therefore, blindly making everything `EAGER` is a common mistake.

---

# 29. The Famous N+1 Problem

Suppose:

```java
List<Order> orders = orderRepository.findAll();
```

This executes:

```sql
SELECT *
FROM orders;
```

Suppose there are 100 orders.

Then code does:

```java
for (Order order : orders) {
    System.out.println(order.getCustomer().getName());
}
```

Potentially:

```text
1 query
+
100 customer queries
=
101 queries
```

Hence:

```text
N + 1
```

Example:

```text
Query 1:
SELECT * FROM orders;

Query 2:
SELECT * FROM customer WHERE id = 1;

Query 3:
SELECT * FROM customer WHERE id = 2;

Query 4:
SELECT * FROM customer WHERE id = 3;

...

Query 101
```

This is one of the most important Hibernate performance problems.

We'll cover solutions such as **fetch joins and entity graphs** in the performance chapter.

---

# 30. `LazyInitializationException`

Another famous Hibernate interview question.

Suppose:

```java
@Transactional
public Order getOrder(Long id) {
    return repository.findById(id).orElseThrow();
}
```

Then transaction ends.

Later, outside the persistence context:

```java
order.getCustomer().getName();
```

If `customer` is lazy and hasn't been initialized:

```text
Persistence Context
       |
       v
Transaction ends
       |
       v
Session closed
       |
       v
Access lazy relationship
       |
       v
Hibernate cannot load it
       |
       v
LazyInitializationException
```

---

# 31. What Should You NOT Do?

A common "fix":

```java
@ManyToOne(fetch = FetchType.EAGER)
```

everywhere.

This may hide the exception but create performance problems.

Better approaches include:

* fetch required data within transaction
* fetch join
* entity graphs
* DTO projections
* proper service-layer boundaries

We'll cover these later.

---

# 32. `fetch` Is Not the Same as SQL JOIN

This is another interview trap.

People often say:

> "EAGER means Hibernate always uses JOIN."

Not necessarily.

`EAGER` means:

> The association should be available eagerly.

Hibernate may use:

```sql
JOIN
```

or:

```sql
SELECT
```

or another provider-specific strategy.

Therefore:

```text
EAGER != JOIN
LAZY  != "never load"
```

LAZY means:

> Defer loading until needed, where the provider can support it.

---

# 33. Owning Side + Cascade + Orphan Removal + Fetch

These four concepts solve **different problems**.

This distinction is extremely useful in interviews:

| Concept         | Question it answers                      |
| --------------- | ---------------------------------------- |
| Owning side     | Who maps/controls the relationship?      |
| `mappedBy`      | Which other property owns the mapping?   |
| `@JoinColumn`   | Which FK column represents it?           |
| `@JoinTable`    | Which intermediate table represents it?  |
| `cascade`       | Should entity operations propagate?      |
| `orphanRemoval` | Should disconnected children be deleted? |
| `fetch`         | When should related data be loaded?      |

Don't mix them.

---

# 34. Complete Example

Let's create:

```text
Customer 1 ---- * Order 1 ---- * OrderItem
```

### Customer

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Order> orders = new ArrayList<>();

    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setCustomer(null);
    }
}
```

### Order

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
```

### OrderItem

```java
@Entity
public class OrderItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
```

---

# 35. Understand This Mapping

Database:

```text
CUSTOMER
    |
    | 1
    |
    | *
    v
ORDERS
    |
    | 1
    |
    | *
    v
ORDER_ITEM
```

Foreign keys:

```text
ORDERS.customer_id
       |
       v
CUSTOMER.id


ORDER_ITEM.order_id
       |
       v
ORDERS.id
```

Java:

```text
Customer
   |
   | @OneToMany
   v
Order
   |
   | @OneToMany
   v
OrderItem
```

Reverse references:

```text
Order.customer
OrderItem.order
```

are the owning sides.

---

# 36. Why This Is a Good Aggregate Mapping

Consider:

```text
Customer
   |
   +---- Order
            |
            +---- OrderItem
            +---- OrderItem
```

If an Order owns its OrderItems:

```java
cascade = CascadeType.ALL
orphanRemoval = true
```

can make sense.

But you probably don't want:

```text
Delete Customer
       |
       v
Delete customer account
       |
       v
Delete all historical orders
```

In a real banking/e-commerce system, orders may need to remain for:

* audit
* reporting
* legal retention
* financial records

So **cascade decisions must follow business lifecycle**, not simply object hierarchy.

This is a very strong production/interview point.

---

# 37. Common Mistakes

### Mistake 1

```java
@OneToMany
private List<Order> orders;
```

without understanding how the relationship is stored.

---

### Mistake 2

Using:

```java
mappedBy = "customer_id"
```

instead of:

```java
mappedBy = "customer"
```

---

### Mistake 3

Thinking:

```java
mappedBy
```

means:

> "Create a mapping using this column."

It means almost the opposite:

> "The other association owns the mapping."

---

### Mistake 4

Using:

```java
cascade = CascadeType.ALL
```

everywhere.

---

### Mistake 5

Using:

```java
CascadeType.REMOVE
```

on shared entities.

---

### Mistake 6

Using:

```java
@ManyToMany
cascade = CascadeType.ALL
```

without considering shared entities.

---

### Mistake 7

Making every relationship:

```java
FetchType.EAGER
```

to avoid lazy-loading problems.

---

### Mistake 8

Assuming:

```java
FetchType.LAZY
```

means no query will ever happen.

---

### Mistake 9

Updating only one side of a bidirectional relationship:

```java
customer.getOrders().add(order);
```

but forgetting:

```java
order.setCustomer(customer);
```

---

# 38. Interview Scenario

### Interviewer

> We have `Department` and `Employee`. One department has many employees. How would you map it?

### Good answer

```java
@Entity
class Department {

    @OneToMany(mappedBy = "department")
    private List<Employee> employees;
}
```

```java
@Entity
class Employee {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

Then explain:

> The FK belongs in `employee.department_id`, so `Employee.department` is the owning side. `Department.employees` is the inverse side and uses `mappedBy = "department"`.

That's much better than just giving the annotations.

---

# 39. Interview Scenario — `orphanRemoval`

### Interviewer

> What's the difference between `CascadeType.REMOVE` and `orphanRemoval=true`?

A strong answer:

> `CascadeType.REMOVE` propagates a remove operation from the parent to associated entities when the parent is removed. `orphanRemoval` deletes a child when it is removed from the parent's relationship and becomes an orphan. They can be used together when the child is truly owned by the parent.

---

# 40. Interview Scenario — Fetch

### Interviewer

> Why don't you make everything EAGER?

Answer:

> EAGER relationships can cause unnecessary loading and large object graphs, and may contribute to performance issues. I generally prefer deliberate fetching and use LAZY relationships where appropriate, then fetch required associations explicitly through fetch joins, entity graphs, or DTO projections.

---

# 41. Interview Scenario — Many-to-Many

### Interviewer

> When would you avoid `@ManyToMany`?

Answer:

> If the relationship itself has attributes, such as `enrolledAt`, `quantity`, `role`, `status`, or `createdAt`, I would model the join table as an explicit entity. It gives the relationship its own identity and makes the model easier to evolve and query.

---

# 42. The Interview Cheat Sheet

```text
@ManyToOne
    |
    +--> Usually owning side
    |
    +--> Usually contains FK
    |
    +--> @JoinColumn


@OneToMany
    |
    +--> Usually inverse side in bidirectional mapping
    |
    +--> mappedBy = "fieldOnOwningSide"


@OneToOne
    |
    +--> FK on one side
    |
    +--> Often unique FK


@ManyToMany
    |
    +--> Join table
    |
    +--> One side owns @JoinTable
    |
    +--> Other side uses mappedBy
```

Then:

```text
cascade
    |
    +--> propagate entity operations


orphanRemoval
    |
    +--> delete child when it becomes orphan


fetch
    |
    +--> control loading strategy
```

---

# 43. Most Important Distinctions

Memorize these:

```text
mappedBy
    = relationship ownership mapping


@JoinColumn
    = FK column


@JoinTable
    = intermediate table


cascade
    = propagate entity operation


orphanRemoval
    = delete orphaned child


LAZY
    = defer loading


EAGER
    = association should be available eagerly
```

And:

```text
JPA cascade != DB ON DELETE CASCADE
```

and:

```text
EAGER != SQL JOIN
```

and:

```text
mappedBy != database column name
```

These three are excellent interview traps.

---

## Chapter 6 complete

The next chapter is **Chapter 7 — JPA Inheritance Mapping**, covering:

```text
@Inheritance
@DiscriminatorColumn
@DiscriminatorValue
@MappedSuperclass
```

and the three major strategies:

```text
SINGLE_TABLE
JOINED
TABLE_PER_CLASS
```

including **database schemas, SQL behavior, performance trade-offs, and interview questions**.

