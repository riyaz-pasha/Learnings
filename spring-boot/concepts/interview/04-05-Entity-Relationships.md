# Chapter 5 — JPA Entity Relationships

Entity relationships are one of the **most important JPA interview topics** because this is where Java's object model meets the relational database model.

We will cover:

1. Why relationships are needed
2. `@ManyToOne`
3. `@OneToMany`
4. Owning side
5. `mappedBy`
6. `@OneToOne`
7. `@ManyToMany`
8. Unidirectional vs bidirectional relationships
9. How Java relationships map to foreign keys
10. Interview questions and common traps

We will cover `cascade`, `orphanRemoval`, `fetch`, and advanced relationship configuration in **Chapter 6**.

---

# 1. Why Do We Need Relationships?

Suppose we have:

```text
Customer
---------
id
name
```

and:

```text
Order
---------
id
orderDate
customer_id
```

In a relational database:

```text
CUSTOMER
+----+-------+
| id | name  |
+----+-------+
| 1  | John  |
| 2  | Alice |
+----+-------+

ORDER
+----+------------+-------------+
| id | order_date | customer_id |
+----+------------+-------------+
| 10 | 2026-10-01 | 1           |
| 11 | 2026-10-02 | 1           |
| 12 | 2026-10-03 | 2           |
+----+------------+-------------+
```

The database represents the relationship using:

```text
ORDER.customer_id
        |
        v
CUSTOMER.id
```

In Java, we want:

```java
class Customer {
    Long id;
    String name;
}

class Order {
    Long id;
    LocalDate orderDate;

    Customer customer;
}
```

The JPA relationship mapping tells Hibernate:

> "`Order.customer` corresponds to the `customer_id` foreign key."

---

# 2. The Four Fundamental Relationships

JPA provides four major relationship annotations:

| Relationship  | Meaning                       |
| ------------- | ----------------------------- |
| `@OneToOne`   | One entity ↔ one entity       |
| `@OneToMany`  | One entity → many entities    |
| `@ManyToOne`  | Many entities → one entity    |
| `@ManyToMany` | Many entities ↔ many entities |

Example:

```text
Customer 1 -------- * Order

User 1 -------- 1 UserProfile

Student * -------- * Course
```

---

# 3. Start With `@ManyToOne`

This is arguably the **most important relationship to understand**.

Consider:

```text
Customer
    1
    |
    |
    *
Order
```

One customer can have many orders.

Therefore:

```java
Order -> Customer
```

is:

```java
@ManyToOne
```

---

# 4. Basic `@ManyToOne`

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private LocalDate orderDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

And:

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Database:

```text
CUSTOMER
+----+-------+
| id | name  |
+----+-------+
| 1  | John  |
| 2  | Alice |
+----+-------+

ORDER
+----+-------------+
| id | customer_id |
+----+-------------+
| 10 | 1           |
| 11 | 1           |
| 12 | 2           |
+----+-------------+
```

The important mapping is:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

---

# 5. What Does `@JoinColumn` Mean?

This:

```java
@JoinColumn(name = "customer_id")
```

means:

> The relationship uses the `customer_id` column in this entity's table.

So:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

roughly means:

```text
Java                         Database

Order.customer  ---------->  ORDER.customer_id
                                  |
                                  v
                              CUSTOMER.id
```

The foreign key is typically:

```sql
ALTER TABLE orders
ADD CONSTRAINT fk_order_customer
FOREIGN KEY (customer_id)
REFERENCES customer(id);
```

---

# 6. Why Is `@ManyToOne` Usually the Owning Side?

Look at the database:

```text
CUSTOMER
id
 |
 |
 v
ORDER
customer_id
```

The foreign key physically exists in:

```text
ORDER.customer_id
```

Therefore the `Order` side naturally controls the relationship.

```java
@Entity
class Order {

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

This is called the:

> **owning side of the relationship**

More precisely:

> The owning side is the side whose mapping defines how the relationship is persisted, usually the side containing the foreign key mapping.

---

# 7. Bidirectional Relationship

So far we have:

```text
Order ---> Customer
```

But perhaps we also want:

```text
Customer ---> Orders
```

In Java:

```java
Customer customer = order.getCustomer();
```

and:

```java
customer.getOrders();
```

Now we have a **bidirectional relationship**.

---

# 8. Bidirectional `@ManyToOne` + `@OneToMany`

### Customer

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();
}
```

### Order

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private LocalDate orderDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

Visual:

```text
                 Java

Customer                         Order
---------                        ---------
id                               id
name                             orderDate
orders ----------------------->  customer
       <-------------------------
```

More precisely:

```text
Customer
   |
   | orders
   |
   v
Order
   |
   | customer
   |
   v
Customer
```

---

# 9. What Does `mappedBy` Mean?

This is one of the **most frequently asked JPA interview questions**.

We have:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

What does:

```java
mappedBy = "customer"
```

mean?

It means:

> "The relationship is already mapped by the `customer` field on the `Order` entity."

Look at `Order`:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

Therefore:

```java
mappedBy = "customer"
```

refers to this Java field:

```java
private Customer customer;
```

### Important

`mappedBy` refers to the **Java property name**, not the database column.

Correct:

```java
mappedBy = "customer"
```

Not:

```java
mappedBy = "customer_id"
```

And not:

```java
mappedBy = "customerId"
```

unless that is actually the association field.

---

# 10. Mental Model for `mappedBy`

Think of it as:

```text
Customer
@OneToMany(mappedBy = "customer")
              |
              |
              +---- "Go look at Order.customer"
                                |
                                v
                        @ManyToOne
                        @JoinColumn(...)
                        Customer customer
```

So:

```java
mappedBy = "customer"
```

means:

> "Order.customer owns the relationship. Customer.orders is just the inverse representation."

---

# 11. Why Do We Need `mappedBy`?

Without `mappedBy`, JPA may interpret both sides as independent relationship mappings.

For example:

```java
Customer:

@OneToMany
private List<Order> orders;
```

and:

```java
Order:

@ManyToOne
private Customer customer;
```

JPA sees:

```text
Customer ---> Order
```

and:

```text
Order ---> Customer
```

as potentially separate mappings.

For a bidirectional relationship, we want:

```text
ONE DATABASE RELATIONSHIP

Customer <----------------> Order
             |
             |
       customer_id
```

not two independently mapped relationships.

Therefore:

```java
@OneToMany(mappedBy = "customer")
```

says:

> "Don't create/manage another relationship from this side. The `Order.customer` mapping is the owner."

---

# 12. Owning Side vs Inverse Side

This is extremely important for interviews.

Our example:

```java
Customer
@OneToMany(mappedBy = "customer")
List<Order> orders;
```

and:

```java
Order
@ManyToOne
@JoinColumn(name = "customer_id")
Customer customer;
```

Therefore:

```text
                Relationship
                     |
          +----------+----------+
          |                     |
          v                     v
      Customer                Order
      inverse                 owning
      side                    side
          |                     |
      mappedBy                 FK
```

### Owning side

```java
Order.customer
```

### Inverse/non-owning side

```java
Customer.orders
```

---

# 13. A Very Important Interview Trap

Suppose you write:

```java
Customer customer = new Customer();

Order order = new Order();

customer.getOrders().add(order);
```

But you **don't** do:

```java
order.setCustomer(customer);
```

What happens?

The Java object graph says:

```text
Customer
   |
   | orders
   v
 Order
```

But the owning side says:

```text
Order.customer = null
```

Since `Order.customer` is the owning side, the relationship may not be persisted as you expect.

Therefore, bidirectional relationships should generally keep **both sides synchronized**.

A common pattern:

```java
public void addOrder(Order order) {
    orders.add(order);
    order.setCustomer(this);
}
```

And:

```java
public void removeOrder(Order order) {
    orders.remove(order);
    order.setCustomer(null);
}
```

This keeps the Java object graph consistent.

We'll revisit the consequences of this in **cascade/orphan removal**.

---

# 14. `@OneToMany`

Now let's focus specifically on:

```java
@OneToMany
```

It means:

> One entity is associated with multiple entities.

Example:

```text
Customer
   |
   +---- Order 1
   |
   +---- Order 2
   |
   +---- Order 3
```

Java:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

The database does **not** normally store an array of order IDs inside the customer row.

Instead:

```text
CUSTOMER

id
1


ORDER

id    customer_id
10       1
11       1
12       1
```

This is an important relational-model concept.

---

# 15. Unidirectional `@OneToMany`

You can also have:

```java
@Entity
public class Customer {

    @OneToMany
    private List<Order> orders;
}
```

without:

```java
Order.customer
```

This is a **unidirectional** relationship.

Java navigation:

```text
Customer ---> Orders
```

but:

```text
Order -X-> Customer
```

There is no back-reference.

JPA may use a join table depending on how the relationship is mapped.

For example conceptually:

```text
CUSTOMER
   |
   v
CUSTOMER_ORDER
+-------------+----------+
| customer_id | order_id |
+-------------+----------+
```

This is one reason bidirectional `@ManyToOne` + `@OneToMany` is often preferred when the relationship naturally has a foreign key on the many side.

---

# 16. `@OneToOne`

Now consider:

```text
User 1 -------- 1 UserProfile
```

Example:

```text
USER
+----+-------+
| id | name  |
+----+-------+

USER_PROFILE
+----+-----------+
| id | user_id   |
+----+-----------+
```

Java:

```java
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}
```

This means:

```text
USER.profile
     |
     v
USER.profile_id
     |
     v
USER_PROFILE.id
```

---

# 17. One-to-One Bidirectional

Suppose:

```java
@Entity
class User {

    @OneToOne
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}
```

Then:

```java
@Entity
class UserProfile {

    @OneToOne(mappedBy = "profile")
    private User user;
}
```

Here:

```text
User
 |
 | profile
 v
UserProfile
 |
 | user
 v
User
```

The owning side is:

```java
User.profile
```

because it defines:

```java
@JoinColumn(name = "profile_id")
```

The inverse side is:

```java
UserProfile.user
```

because it uses:

```java
mappedBy = "profile"
```

---

# 18. One-to-One Does Not Automatically Mean Database Uniqueness

This is a subtle interview point.

If you have:

```java
@OneToOne
@JoinColumn(name = "profile_id")
private UserProfile profile;
```

the database relationship should normally enforce that one profile cannot belong to multiple users.

That means the FK should be unique:

```text
USER

profile_id
    |
    v
USER_PROFILE.id
```

with a uniqueness constraint on `profile_id`.

Conceptually:

```sql
UNIQUE(profile_id)
```

Otherwise the database could potentially contain:

```text
USER
id   profile_id
1       100
2       100
```

which violates the intended one-to-one relationship.

---

# 19. `@ManyToMany`

Now consider:

```text
Student * -------- * Course
```

A student can take many courses.

A course can have many students.

Example:

```text
Student
-------
1 John
2 Alice

Course
------
101 Java
102 Spring
103 Database
```

Relationship:

```text
John  ---> Java
John  ---> Spring

Alice ---> Java
Alice ---> Database
```

A relational database cannot normally store this directly in either table.

We use a **join table**.

---

# 20. Many-to-Many Database Model

```text
STUDENT
+----+-------+
| id | name  |
+----+-------+
| 1  | John  |
| 2  | Alice |
+----+-------+

COURSE
+-----+----------+
| id  | name     |
+-----+----------+
| 101 | Java     |
| 102 | Spring   |
| 103 | Database |
+-----+----------+

STUDENT_COURSE
+------------+-----------+
| student_id | course_id |
+------------+-----------+
| 1          | 101       |
| 1          | 102       |
| 2          | 101       |
| 2          | 103       |
+------------+-----------+
```

The join table represents the relationship.

---

# 21. JPA `@ManyToMany`

```java
@Entity
public class Student {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}
```

And:

```java
@Entity
public class Course {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

The important part:

```java
@JoinTable(
    name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
```

---

# 22. Understanding `@JoinTable`

Think:

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

Therefore:

```java
joinColumns
```

means:

> Column referring to the entity on this side.

And:

```java
inverseJoinColumns
```

means:

> Column referring to the entity on the other side.

---

# 23. Bidirectional `@ManyToMany`

Student:

```java
@ManyToMany
@JoinTable(
    name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id")
)
private Set<Course> courses = new HashSet<>();
```

Course:

```java
@ManyToMany(mappedBy = "courses")
private Set<Student> students = new HashSet<>();
```

So:

```text
Student
   |
   | courses
   v
Course
   |
   | students
   v
Student
```

The owning side is:

```java
Student.courses
```

because it defines:

```java
@JoinTable(...)
```

The inverse side is:

```java
Course.students
```

because:

```java
mappedBy = "courses"
```

---

# 24. Important Production Warning: Many-to-Many

A direct:

```java
@ManyToMany
```

is convenient when the relationship itself has no additional information.

But suppose enrollment has:

```text
Student
Course
Enrollment
---------
enrolledAt
grade
status
```

Now the relationship itself has data.

Instead of:

```text
Student * ----- * Course
```

model:

```text
Student 1 ----- * Enrollment * ----- 1 Course
```

Database:

```text
STUDENT
   |
   | 1
   |
   *
ENROLLMENT
   *
   |
   | 1
   |
COURSE
```

Entity:

```java
@Entity
class Enrollment {

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;

    private LocalDate enrolledAt;

    private String grade;
}
```

This is often much easier to evolve and query.

**Interview answer:**

> If a many-to-many relationship has attributes of its own, model the join table as an explicit entity rather than using a direct `@ManyToMany`.

This is a very useful production design principle.

---

# 25. Unidirectional vs Bidirectional

### Unidirectional

```text
Customer -----> Orders
```

Only one entity knows about the other.

Example:

```java
class Customer {

    @OneToMany
    List<Order> orders;
}
```

### Bidirectional

```text
Customer <-----> Order
```

Both entities have references:

```java
class Customer {

    @OneToMany(mappedBy = "customer")
    List<Order> orders;
}
```

```java
class Order {

    @ManyToOne
    @JoinColumn(name = "customer_id")
    Customer customer;
}
```

---

# 26. Does Bidirectional Mean Two Database Relationships?

**No.**

This is a very common misconception.

Suppose:

```java
Customer.orders
```

and:

```java
Order.customer
```

Both point to each other.

But:

```text
Customer.orders
        \
         \
          SAME DATABASE RELATIONSHIP
         /
        /
Order.customer
```

`mappedBy` tells JPA:

> These two Java references represent the same underlying relationship.

---

# 27. Relationship Mapping Cheat Sheet

| Java relationship                  | JPA           |
| ---------------------------------- | ------------- |
| Many orders belong to one customer | `@ManyToOne`  |
| Customer has many orders           | `@OneToMany`  |
| User has one profile               | `@OneToOne`   |
| Student takes many courses         | `@ManyToMany` |

Typical mappings:

### Many-to-one

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

### One-to-many inverse side

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

### One-to-one

```java
@OneToOne
@JoinColumn(name = "profile_id")
private UserProfile profile;
```

### Many-to-many

```java
@ManyToMany
@JoinTable(...)
private Set<Course> courses;
```

---

# 28. The Most Important Concept: Owning Side

Remember this:

```text
                JPA Relationship
                      |
          +-----------+-----------+
          |                       |
          v                       v
      Owning side             Inverse side
          |                       |
          |                       |
     Defines mapping          mappedBy
          |
          v
   FK / Join Table
```

For:

```text
Customer 1 ---- * Order
```

usually:

```java
Order.customer
```

is the owning side.

For:

```text
Student * ---- * Course
```

one side owns the join table:

```java
Student.courses
```

and the other uses:

```java
mappedBy = "courses"
```

---

# 29. Interview Question: Does `mappedBy` Create a Database Column?

**No.**

This:

```java
@OneToMany(mappedBy = "customer")
```

doesn't mean:

```text
customer column
```

Instead:

```text
mappedBy = "customer"
```

references the Java association:

```java
Order.customer
```

It tells JPA:

> "This side is not responsible for mapping the relationship."

---

# 30. Interview Question: Why Is `@ManyToOne` Commonly the Owning Side?

Because the foreign key normally lives on the "many" side.

```text
CUSTOMER
   |
   | id
   |
   v
ORDER
customer_id
```

Many orders can reference the same customer:

```text
Order 1 -> Customer 10
Order 2 -> Customer 10
Order 3 -> Customer 10
```

Therefore:

```java
Order.customer
```

naturally owns the FK:

```java
@JoinColumn(name = "customer_id")
```

---

# 31. Interview Question: Can `@OneToMany` Be the Owning Side?

**Yes.**

It is possible to configure a unidirectional or join-table-based `@OneToMany`.

But for a typical relational model:

```text
Customer 1 ---- * Order
```

the natural mapping is:

```java
Customer
@OneToMany(mappedBy = "customer")

Order
@ManyToOne
@JoinColumn(name = "customer_id")
```

because the FK belongs in the `orders` table.

---

# 32. Interview Question: Which Side Should You Put `mappedBy` On?

Put:

```java
mappedBy
```

on the **inverse/non-owning side**.

Example:

```java
Customer:

@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

because:

```java
Order.customer
```

is the owning side.

---

# 33. Interview Question: What Does `mappedBy = "customer"` Refer To?

It refers to:

```java
Order.customer
```

not:

```text
customer_id
```

not:

```text
Customer
```

not:

```text
customers
```

It is the **Java field/property name**.

---

# 34. Interview Question: What Is the Difference Between `@JoinColumn` and `@JoinTable`?

### `@JoinColumn`

Typically represents a foreign-key column directly in one entity's table.

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

Database:

```text
ORDER
---------
id
customer_id
```

### `@JoinTable`

Uses an intermediate table.

```java
@ManyToMany
@JoinTable(name = "student_course")
```

Database:

```text
STUDENT
COURSE

       |
       v

STUDENT_COURSE
```

---

# 35. Interview Mental Model

When you see a JPA relationship, ask these questions in this order:

### Step 1 — What is the cardinality?

```text
1:1
1:N
N:1
N:N
```

### Step 2 — Where does the foreign key live?

For:

```text
Customer 1 ---- * Order
```

usually:

```text
Order.customer_id
```

### Step 3 — Which Java side maps that FK?

```java
Order.customer
```

### Step 4 — That's the owning side.

### Step 5 — If both sides exist, put `mappedBy` on the inverse side.

This mental process solves most basic relationship-mapping questions.

---

# 36. Relationship Diagram to JPA

Suppose the interviewer gives:

```text
Department 1 -------- * Employee
```

Database:

```text
DEPARTMENT
+----+------+
| id | name |
+----+------+

EMPLOYEE
+----+------+---------------+
| id | name | department_id |
+----+------+---------------+
```

Java:

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

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
}
```

Why?

```text
EMPLOYEE.department_id
            |
            v
      DEPARTMENT.id
```

Therefore:

```text
Employee.department
       ^
       |
 owning side
```

and:

```text
Department.employees
       ^
       |
 inverse side
```

---

# 37. One Big Interview Table

| Concept        | Meaning                                             |
| -------------- | --------------------------------------------------- |
| `@ManyToOne`   | Many entities reference one entity                  |
| `@OneToMany`   | One entity references many entities                 |
| `@OneToOne`    | One entity references one entity                    |
| `@ManyToMany`  | Many entities reference many entities               |
| `@JoinColumn`  | FK column mapping                                   |
| `@JoinTable`   | Intermediate/join table mapping                     |
| `mappedBy`     | Identifies the association mapped by the other side |
| Owning side    | Side responsible for relationship mapping           |
| Inverse side   | Side using `mappedBy`                               |
| Bidirectional  | Both entities can navigate to each other            |
| Unidirectional | Only one entity can navigate to the other           |

---

# 38. Chapter 5 — Interview Questions

You should be able to answer these without looking at notes:

### Basic

1. What are the four JPA relationship types?
2. What is the difference between `@OneToMany` and `@ManyToOne`?
3. How does a JPA relationship map to a foreign key?
4. What is `@JoinColumn`?
5. What is `@JoinTable`?

### Important

6. What is the owning side of a relationship?
7. Why is `@ManyToOne` usually the owning side?
8. What does `mappedBy` mean?
9. Does `mappedBy` refer to a database column?
10. Why do we need `mappedBy` in bidirectional relationships?
11. What happens if both sides are treated as owning sides?
12. What is a unidirectional relationship?
13. What is a bidirectional relationship?
14. Does bidirectional mean two database relationships?

### Production-level

15. Why can direct `@ManyToMany` become problematic?
16. When should a many-to-many relationship be modeled as an explicit entity?
17. Why should both sides of a bidirectional relationship be kept synchronized in Java?
18. How would you map `Department 1 -> N Employee`?
19. Where would the foreign key go?
20. Which side is the owning side and why?

---

## The key mental model to remember

For most relationships, start from the **database**:

```text
                DATABASE

Customer
   id
    ^
    |
    | FK
    |
Order
customer_id
```

Then map it into Java:

```text
                JAVA

Customer                  Order
   ^                         |
   |                         |
   |                         v
orders <-------------- customer
 inverse                  owning
```

And:

```java
@OneToMany(mappedBy = "customer")
```

means:

> **"Customer.orders is not the owner. Look at Order.customer for the actual relationship mapping."**

That sentence is worth remembering for interviews.

---

**Next chapter:** **Chapter 6 — Relationship Configuration**, where we'll go deeper into `mappedBy`, `@JoinColumn`, `@JoinTable`, `cascade`, `orphanRemoval`, `fetch`, owning/inverse sides, and the common Hibernate/JPA relationship traps.


