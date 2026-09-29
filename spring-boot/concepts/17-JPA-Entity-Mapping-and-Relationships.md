# Topic 17 — JPA Entity Mapping & Relationships

This is one of the **highest-value JPA topics for interviews**.

Using:

```java
@ManyToOne
@OneToMany
@OneToOne
@ManyToMany
```

is easy.

Understanding **why the mapping works**, **where the foreign key lives**, **which side owns the relationship**, **what `mappedBy` actually means**, and **what cascade/orphan removal really do** is what separates basic Spring/JPA knowledge from strong interview-level knowledge.

We'll build everything from the database model first.

---

# 1. JPA starts with objects, the database starts with tables

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
order_date
customer_id
```

The database says:

```text
CUSTOMER  1 -------- N  ORDER
```

because many orders can belong to one customer.

In Java, we'd like:

```java
class Customer {
    private Long id;
    private String name;

    private List<Order> orders;
}
```

and:

```java
class Order {
    private Long id;
    private LocalDate orderDate;

    private Customer customer;
}
```

JPA's job is to map the object relationship:

```text
Customer.orders
Order.customer
```

to the relational structure:

```text
CUSTOMER.id
ORDER.customer_id
```

JPA defines these association mappings; an ordinary `@ManyToOne` usually corresponds to a foreign key, a `@OneToMany` usually corresponds to a foreign key in the associated entity's table, a `@OneToOne` commonly corresponds to a unique foreign key, and a `@ManyToMany` uses a join table. ([Jakarta EE][1])

---

# 2. `@Entity`

A persistent class is typically annotated:

```java
@Entity
public class Customer {
}
```

Conceptually:

```text
Java class
    ↓
@Entity
    ↓
JPA-managed entity
    ↓
database table
```

For example:

```java
@Entity
public class Customer {

    @Id
    private Long id;

    private String name;
}
```

The fields/properties of an entity are persistent by default unless explicitly marked otherwise, and every entity needs a primary key using `@Id` or `@EmbeddedId`. ([Jakarta EE][1])

For modern Spring applications, you'll generally see:

```java
import jakarta.persistence.*;
```

rather than the old:

```java
import javax.persistence.*;
```

because current Jakarta Persistence uses the `jakarta.persistence` namespace.

---

# 3. `@Id`

```java
@Id
private Long id;
```

means:

> This field represents the entity's primary key.

For example:

```java
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Conceptually:

```text
Java                    Database

User                    USER
-----                   ----------
id        ----------->  id PK
name      ----------->  name
```

Every entity needs a primary key. ([Jakarta EE][2])

---

# 4. `@Table`

You can explicitly control the table:

```java
@Entity
@Table(name = "users")
public class User {
}
```

Then:

```text
User
  ↓
users
```

Without an explicit table name, naming behavior is determined by JPA/provider/framework conventions.

Usually in enterprise applications you should be explicit when the database schema is already defined.

---

# 5. Basic field mapping

Suppose:

```java
@Entity
public class User {

    @Id
    private Long id;

    private String firstName;

    private String lastName;

    private Integer age;
}
```

Conceptually:

```text
USER
----------------
id
first_name
last_name
age
```

JPA maps Java state to database state.

Relationship mappings are just an extension of this idea.

---

# 6. The four relationship types

There are four fundamental association types:

```text
@OneToOne
@OneToMany
@ManyToOne
@ManyToMany
```

Think from the perspective of one entity.

### `@OneToOne`

One entity ↔ one entity.

```text
Person ─── Passport
```

### `@OneToMany`

One entity → many entities.

```text
Department ───< Employee
```

### `@ManyToOne`

Many entities → one entity.

```text
Employee >─── Department
```

### `@ManyToMany`

Many ↔ many.

```text
Student >───< Course
```

The important thing is that these are **object-model relationships** that JPA maps to relational structures.

---

# 7. Start with `@ManyToOne`

This is probably the most important relationship to understand.

Suppose:

```text
Customer
    |
    +---- Order
    +---- Order
    +---- Order
```

Many orders belong to one customer.

Therefore:

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Customer customer;
}
```

Database:

```text
ORDER
----------------
id
customer_id  ---> CUSTOMER.id
```

The foreign key is normally on the **many side**.

That's an excellent rule:

> **In a normal relational one-to-many relationship, the foreign key sits on the many side.**

The Jakarta Persistence documentation explicitly shows `@ManyToOne` with `@JoinColumn` where the foreign-key column is in the source entity's table. ([Jakarta EE][3])

---

# 8. `@JoinColumn`

Instead of relying on defaults:

```java
@ManyToOne
private Customer customer;
```

we can explicitly say:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

Now it's obvious:

```text
ORDER
----------------
id
customer_id  ---> CUSTOMER.id
```

`@JoinColumn` identifies the column used to join the association. For a many-to-one foreign-key mapping, the join column lives in the table of the source entity. ([Jakarta EE][4])

This is one annotation you should understand extremely well.

---

# 9. What does `referencedColumnName` mean?

Suppose:

```java
@ManyToOne
@JoinColumn(
    name = "customer_id",
    referencedColumnName = "id"
)
private Customer customer;
```

This says:

```text
ORDER.customer_id
        ↓
CUSTOMER.id
```

Where:

```text
name
```

means:

> the foreign-key column in the current entity's table.

And:

```text
referencedColumnName
```

means:

> the referenced column in the target table.

By default, the referenced column is generally the target entity's primary key. ([Jakarta EE][4])

---

# 10. Bidirectional relationship

Now we want both sides.

`Customer` should know its orders:

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

And:

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

Now:

```text
Customer
   |
   | orders
   v
Order
   |
   | customer
   v
Customer
```

This is a **bidirectional relationship**.

---

# 11. What is `mappedBy`?

This is probably the **single most misunderstood JPA annotation**.

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

> "The `customer` field in `Order` owns the relationship mapping."

Notice:

```text
mappedBy = "customer"
              ↑
        Java field name
```

It is **not**:

```text
mappedBy = "customer_id"
```

It is:

```text
mappedBy = "customer"
```

because `customer` is the Java association field in `Order`.

The Jakarta Persistence documentation defines `mappedBy` as identifying the field/property on the target entity that owns the relationship. ([Jakarta EE][5])

---

# 12. The owning side

This is critical.

Our mapping:

```java
Customer:

@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

and:

```java
Order:

@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

The owner is:

```text
Order.customer
```

not:

```text
Customer.orders
```

Why?

Because `Order` contains:

```java
@JoinColumn(name = "customer_id")
```

and therefore controls the foreign key mapping.

---

# 13. Why does JPA need an owning side?

Because a relational database has one actual foreign key.

We have:

```text
ORDER.customer_id
```

There aren't two separate relationships in the database.

The Java model exposes:

```text
Customer.orders
```

and:

```text
Order.customer
```

but both represent the **same database relationship**.

Therefore JPA needs to know:

> Which side is authoritative for updating that relationship?

That's the owning side.

For bidirectional associations, Jakarta Persistence requires an owning side and an inverse side; modifying only the inverse side does not establish the database relationship. ([Jakarta EE][3])

---

# 14. A very important example

Suppose:

```java
Customer customer = new Customer();

Order order = new Order();

customer.getOrders().add(order);
```

But we don't do:

```java
order.setCustomer(customer);
```

What happens?

The owning side is still:

```java
order.customer
```

and we never changed it.

Therefore, the relationship may not be persisted as intended.

You should instead maintain **both sides**:

```java
customer.getOrders().add(order);
order.setCustomer(customer);
```

This is why good entity models often provide helper methods.

---

# 15. Helper methods

Instead of allowing:

```java
customer.getOrders().add(order);
```

everywhere, use:

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

Then:

```java
customer.addOrder(order);
```

keeps both sides synchronized.

This is much safer.

---

# 16. Why `mappedBy` doesn't mean "join using this column"

This is a subtle interview trap.

Wrong interpretation:

```text
mappedBy = "customer"
```

means:

> join using column `customer`.

No.

It means:

> The relationship is already mapped by the `customer` association property on the other entity.

The actual database column is determined by:

```java
@JoinColumn(name = "customer_id")
```

on the owning side.

So:

```text
mappedBy
    ↓
Java association property

@JoinColumn
    ↓
database join column
```

Memorize that distinction.

---

# 17. `@OneToMany`

Now look from the parent's perspective:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

This says:

```text
one Customer
    |
    +---- many Orders
```

But `@OneToMany` itself does not mean:

> "Create a foreign key in the customer table."

In the typical bidirectional mapping:

```text
CUSTOMER
    id

ORDER
    id
    customer_id
```

The foreign key remains in `ORDER`.

That is why:

```java
@OneToMany(mappedBy = "customer")
```

is commonly paired with:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
```

The current Jakarta Persistence documentation shows this exact pattern. ([Jakarta EE][5])

---

# 18. Unidirectional `@OneToMany`

You can also have:

```java
@Entity
public class Customer {

    @OneToMany
    private List<Order> orders;
}
```

There is no:

```java
Order.customer
```

This is a **unidirectional** relationship.

JPA can map such a relationship using a foreign-key strategy or a join table depending on the mapping. The specification explicitly supports both patterns. ([Jakarta EE][5])

For many real applications, a bidirectional:

```java
Customer.orders
Order.customer
```

mapping is more natural when the child needs to navigate to its parent.

---

# 19. Why unidirectional `OneToMany` can be less attractive

Consider:

```java
Customer
   |
   +--> Order
   +--> Order
```

with only:

```java
Customer.orders
```

Hibernate may need to manage a collection representation in a way that is less efficient than the conventional bidirectional foreign-key mapping.

Hibernate's documentation specifically notes that a bidirectional `@OneToMany` is more efficient because the child (`@ManyToOne`) controls the association. ([Hibernate Documentation][6])

So the common production mapping is:

```java
@OneToMany(mappedBy = "customer")
```

plus:

```java
@ManyToOne
@JoinColumn(...)
```

rather than blindly using unidirectional `@OneToMany`.

---

# 20. Default fetch types

This is a classic interview question.

For standard JPA mappings:

| Relationship  | Default fetch |
| ------------- | ------------- |
| `@ManyToOne`  | `EAGER`       |
| `@OneToOne`   | `EAGER`       |
| `@OneToMany`  | `LAZY`        |
| `@ManyToMany` | `LAZY`        |

The Jakarta Persistence specification defines these defaults, with lazy loading for collection associations and eager loading by default for one-to-one and many-to-one associations. ([Jakarta EE][1])

So:

```java
@ManyToOne
private Customer customer;
```

is eager by default.

Whereas:

```java
@OneToMany
private List<Order> orders;
```

is lazy by default.

---

# 21. EAGER vs LAZY

This deserves a separate mental model.

### EAGER

When the entity is loaded, the associated data is required to be fetched eagerly according to the mapping.

Conceptually:

```text
find Customer
    ↓
Customer + associated relationship
```

### LAZY

The association may be loaded when accessed while the entity remains managed and the persistence context can initialize it.

Conceptually:

```text
find Customer
    ↓
Customer
    ↓
customer.getOrders()
    ↓
load orders
```

The specification treats lazy fetching as a hint to the provider, while eager fetching is a requirement to fetch the association eagerly. ([Jakarta EE][1])

---

# 22. Why LAZY is usually important for collections

Suppose:

```text
Customer
  |
  +--- 10,000 orders
```

You execute:

```java
customerRepository.findById(1L);
```

Do you necessarily want all 10,000 orders immediately?

Often no.

That's why collections are normally lazy.

The association:

```java
@OneToMany
private List<Order> orders;
```

can be initialized when needed.

---

# 23. The famous LazyInitializationException

Suppose:

```java
@Transactional
public Customer getCustomer(Long id) {
    return repository.findById(id).orElseThrow();
}
```

The method returns the entity.

Later, outside the transaction:

```java
customer.getOrders();
```

The collection may need to be initialized, but the original persistence context may no longer be available.

This can result in:

```text
LazyInitializationException
```

This is an extremely common interview question.

The important lesson isn't:

> "Make everything EAGER."

That's usually a poor solution.

The better question is:

> "Where should the required relationship be fetched as part of the use case?"

Possible approaches include:

```text
fetch join
EntityGraph
explicit query
DTO projection
appropriate transaction boundary
```

We'll dedicate a later topic to this because it connects directly to the **N+1 query problem**.

---

# 24. `@ManyToOne` and `optional`

Consider:

```java
@ManyToOne(optional = false)
@JoinColumn(name = "customer_id")
private Customer customer;
```

This says that the association is not optional.

Conceptually:

```text
Order
   |
   +--> must have Customer
```

Without:

```java
optional = false
```

the association is optional by default.

The Jakarta Persistence documentation specifies `optional=true` by default for `ManyToOne`, meaning the relationship may be null. ([Jakarta EE][3])

---

# 25. `nullable = false`

You may also see:

```java
@JoinColumn(
    name = "customer_id",
    nullable = false
)
```

This concerns the database column constraint.

For example:

```java
@ManyToOne(optional = false)
@JoinColumn(
    name = "customer_id",
    nullable = false
)
private Customer customer;
```

Now you're expressing the non-null relationship both at the object mapping level and the database join-column level.

Don't confuse:

```text
optional
```

with:

```text
nullable
```

They describe related but distinct aspects:

```text
optional
    ↓
association semantics

nullable
    ↓
join-column/database nullability
```

---

# 26. Cascade — one of the most important concepts

Consider:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.ALL
)
private List<Order> orders;
```

What does cascade mean?

It means certain persistence operations performed on the parent are propagated to the associated entity.

For example:

```text
Customer
   |
   +---- Order
   +---- Order
```

If cascade persist is enabled:

```java
entityManager.persist(customer);
```

the persist operation can cascade to associated orders.

Jakarta Persistence defines cascade as the operations that are propagated to the target of an association; by default, no operations are cascaded. ([Jakarta EE][5])

---

# 27. Cascade types

The important cascade types are:

```java
CascadeType.PERSIST
CascadeType.MERGE
CascadeType.REMOVE
CascadeType.REFRESH
CascadeType.DETACH
CascadeType.ALL
```

Think:

```text
PERSIST
    ↓
persist child too

MERGE
    ↓
merge child too

REMOVE
    ↓
remove child too

ALL
    ↓
all cascade operations
```

A common interview mistake is to say:

> "`CascadeType.ALL` means child is always deleted when the parent is deleted."

That's incomplete.

`ALL` means all defined cascade operations are propagated.

Deletion is specifically associated with:

```java
CascadeType.REMOVE
```

and `ALL` includes it.

---

# 28. Cascade does NOT mean foreign-key relationship

Another common misconception:

```java
cascade = CascadeType.ALL
```

doesn't mean:

> "database cascading."

These are different concepts.

### JPA cascade

```text
Java entity operation
        ↓
propagated by JPA
```

### Database cascade

```text
SQL foreign key
        ↓
ON DELETE CASCADE
        ↓
database performs deletion
```

They are not the same mechanism.

That's an important distinction in interviews.

---

# 29. `CascadeType.REMOVE`

Suppose:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.REMOVE
)
private List<Order> orders;
```

Then:

```java
entityManager.remove(customer);
```

can propagate removal to the orders.

Conceptually:

```text
remove(Customer)
      ↓
remove(Order 1)
remove(Order 2)
remove(Order 3)
```

But be careful.

Should deleting a customer really delete all associated records?

That is a **domain decision**, not something you add just because the annotation is available.

---

# 30. `CascadeType.ALL` — use carefully

You'll often see:

```java
cascade = CascadeType.ALL
```

and people add it everywhere.

That's dangerous.

Suppose:

```text
Student
   |
   +---- Course
```

If you put `CascadeType.REMOVE` on a many-to-many relationship, deleting one student could conceptually propagate removal toward shared course entities.

That usually isn't what you want.

Cascade should reflect **ownership/lifecycle**, not simply convenience.

A strong interview statement is:

> "I use cascading when the lifecycle of the associated entity is owned by the parent. I don't blindly use `CascadeType.ALL`, especially for shared/reference entities."

---

# 31. `orphanRemoval`

Now we reach one of the most important distinctions.

Consider:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<Order> orders;
```

What does:

```java
orphanRemoval = true
```

mean?

Suppose:

```java
customer.getOrders().remove(order);
```

The order has been removed from the parent's relationship.

With orphan removal enabled, JPA can remove the orphaned child entity from the database.

The specification defines `orphanRemoval` as applying remove to entities removed from the relationship, with the orphan-removal semantics defined for the association mapping. ([Jakarta EE][5])

---

# 32. `CascadeType.REMOVE` vs `orphanRemoval`

This is a **very common EPAM-style interview question**.

Suppose:

```java
@OneToMany(
    mappedBy = "customer",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<Order> orders;
```

### Parent gets deleted

```java
entityManager.remove(customer);
```

This involves:

```text
CascadeType.REMOVE
```

because removal cascades from parent to child.

### Child gets removed from collection

```java
customer.getOrders().remove(order);
```

This is where:

```text
orphanRemoval = true
```

causes the child to be treated as an orphan and removed.

So:

```text
Parent deletion
    ↓
CascadeType.REMOVE

Child removed from parent's relationship
    ↓
orphanRemoval
```

This mental distinction is extremely useful.

---

# 33. Real-world example: Invoice and LineItem

This is particularly relevant to the kind of JPA work you've done.

Suppose:

```text
Invoice
   |
   +---- LineItem
   +---- LineItem
   +---- LineItem
```

A `LineItem` has no meaningful existence without its invoice.

You might model:

```java
@Entity
public class Invoice {

    @OneToMany(
        mappedBy = "invoice",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<LineItem> lineItems = new ArrayList<>();
}
```

and:

```java
@Entity
public class LineItem {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
}
```

Now the lifecycle is:

```text
Invoice owns LineItems
```

So:

```text
delete Invoice
    ↓
delete LineItems

remove LineItem from Invoice.lineItems
    ↓
delete that LineItem
```

That's an appropriate use case for cascade + orphan removal when the child truly belongs to the parent.

---

# 34. Don't confuse `orphanRemoval` with `REMOVE` cascade

Consider:

```java
@OneToMany(
    mappedBy = "invoice",
    orphanRemoval = true
)
```

Even without:

```java
cascade = CascadeType.REMOVE
```

the orphan-removal behavior is its own mechanism for children removed from the relationship.

Conversely, `CascadeType.REMOVE` concerns propagation of a remove operation from parent to child.

They're related, but they aren't synonyms.

---

# 35. `@OneToOne`

Now:

```text
Person ─── Passport
```

One person has one passport.

You can write:

```java
@Entity
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    @JoinColumn(name = "passport_id")
    private Passport passport;
}
```

Database:

```text
PERSON
----------------
id
passport_id ---> PASSPORT.id
```

For a true one-to-one relationship, that foreign-key relationship generally needs uniqueness so that multiple rows cannot reference the same passport. Jakarta Persistence describes a one-to-one as usually mapping to a unique foreign key or shared primary-key relationship. ([Jakarta EE][7])

---

# 36. Bidirectional `OneToOne`

You could have:

```java
@Entity
public class Person {

    @OneToOne
    @JoinColumn(name = "passport_id")
    private Passport passport;
}
```

and:

```java
@Entity
public class Passport {

    @OneToOne(mappedBy = "passport")
    private Person person;
}
```

Owner:

```text
Person.passport
```

Inverse:

```text
Passport.person
```

Again:

```text
mappedBy
```

is placed on the non-owning side. Jakarta Persistence explicitly describes `mappedBy` as being specified on the inverse side of a bidirectional one-to-one association. ([Jakarta EE][7])

---

# 37. Shared primary key `OneToOne`

There is another pattern:

```text
Person
id = 10

Passport
id = 10
```

where the passport uses the person's primary key.

This can be mapped using:

```java
@MapsId
```

This is a more advanced one-to-one mapping.

You don't need to use it routinely, but know that one-to-one relationships can be implemented using either:

```text
unique foreign key
```

or:

```text
shared primary key
```

as supported by the persistence mapping model. ([Jakarta EE][7])

---

# 38. `@ManyToMany`

Now:

```text
Student >────< Course
```

A student can take many courses.

A course can have many students.

A relational database cannot simply put:

```text
Student.course_id
```

because one student can have many courses.

Nor:

```text
Course.student_id
```

because one course can have many students.

So we need:

```text
STUDENT
---------
id

COURSE
---------
id

STUDENT_COURSE
--------------
student_id
course_id
```

That's called a **join table**.

Jakarta Persistence defines `@ManyToMany` as a many-valued association mapped using an intermediate join table. ([Jakarta EE][8])

---

# 39. Mapping `ManyToMany`

Example:

```java
@Entity
public class Student {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns =
            @JoinColumn(name = "student_id"),
        inverseJoinColumns =
            @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}
```

Database:

```text
student
---------
id

course
---------
id

student_course
----------------
student_id
course_id
```

The terms:

```java
joinColumns
```

and:

```java
inverseJoinColumns
```

are worth knowing.

From `Student`'s perspective:

```text
joinColumns
    ↓
Student foreign key

inverseJoinColumns
    ↓
Course foreign key
```

---

# 40. Bidirectional `ManyToMany`

You can have:

```java
Student.courses
```

and:

```java
Course.students
```

For example:

```java
@ManyToMany
@JoinTable(...)
private Set<Course> courses;
```

and:

```java
@ManyToMany(mappedBy = "courses")
private Set<Student> students;
```

Again:

```text
Student
    ↓
owning side

Course
    ↓
inverse side
```

The owning side controls the association mapping. ([Jakarta EE][8])

---

# 41. Why `ManyToMany` is often avoided in rich domains

This is an important design insight.

Suppose:

```text
Student ↔ Course
```

Later you discover the relationship itself needs:

```text
enrollment_date
grade
status
semester
```

Now the relationship isn't just:

```text
student_id
course_id
```

It has its own data.

Instead of:

```java
@ManyToMany
```

you usually model an explicit entity:

```text
Student
   |
   +---- Enrollment
              |
              +---- Course
```

Database:

```text
ENROLLMENT
----------------
id
student_id
course_id
enrollment_date
grade
status
```

This is often a much more flexible domain model.

---

# 42. The relationship entity pattern

Instead of:

```java
@ManyToMany
private Set<Course> courses;
```

you have:

```java
@OneToMany(mappedBy = "student")
private Set<Enrollment> enrollments;
```

and:

```java
@ManyToOne
@JoinColumn(name = "course_id")
private Course course;
```

This transforms:

```text
many-to-many
```

into:

```text
one-to-many + many-to-one
```

which matches the actual join entity.

This is a very valuable database-design/JPA interview concept.

---

# 43. Owning side rule — memorize this

For:

```java
Customer
   |
   +---- orders
```

we commonly have:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

and:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

Therefore:

```text
Order = owning side
Customer = inverse side
```

Why?

Because the child has the foreign key.

So remember:

> **In a conventional bidirectional `OneToMany`/`ManyToOne`, the `ManyToOne` side is the owning side.**

Hibernate's guide also explicitly describes the child side (`ManyToOne`) as the owner in this common mapping. ([Hibernate Documentation][6])

---

# 44. A critical misconception about `mappedBy`

Some beginners think:

```java
@OneToMany(mappedBy = "customer")
```

means:

> Customer maps the database relationship.

It's almost the opposite.

`mappedBy` tells JPA:

> "Don't independently map this relationship. The other side's `customer` association already owns the mapping."

So:

```text
mappedBy
    ↓
"I am the inverse side."
```

That's the easiest mental shortcut.

---

# 45. `mappedBy` and database updates

Consider:

```java
customer.getOrders().add(order);
```

but:

```java
order.setCustomer(null);
```

The owning side says:

```text
order.customer = null
```

So the database relationship follows the owning side, not merely the collection contents.

That's why helper methods are valuable:

```java
public void addOrder(Order order) {
    orders.add(order);
    order.setCustomer(this);
}
```

You want:

```text
Java graph
```

and:

```text
JPA owning-side state
```

to remain consistent.

---

# 46. Relationship helper methods

A strong entity design often looks like:

```java
@Entity
public class Customer {

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

This encapsulates relationship management.

It prevents application code from doing:

```java
customer.getOrders().add(order);
```

without updating:

```java
order.setCustomer(customer);
```

---

# 47. `List` vs `Set`

You will see:

```java
List<Order>
```

or:

```java
Set<Order>
```

for collections.

### `List`

Allows duplicates and has ordering semantics.

### `Set`

Represents uniqueness according to Java's equality semantics.

This becomes particularly important with JPA entities because `equals()`/`hashCode()` design can affect `Set` behavior.

For now, remember:

```text
List
    ↓
ordered / duplicate-permitting collection

Set
    ↓
uniqueness semantics
```

Entity equality is important enough to deserve a dedicated later topic.

---

# 48. `@OrderBy`

You may want:

```java
@OneToMany(mappedBy = "customer")
@OrderBy("orderDate DESC")
private List<Order> orders;
```

This tells JPA to order the loaded collection according to the specified persistent attribute ordering.

This is different from:

```text
Java sorting after retrieval
```

because the persistence layer can apply the ordering when loading the collection.

---

# 49. Cascade does not mean "always save children"

Suppose:

```java
@OneToMany(cascade = CascadeType.PERSIST)
```

Then:

```text
persist parent
    ↓
persist children
```

But:

```text
merge parent
```

doesn't automatically imply merge unless appropriate cascade configuration includes:

```java
CascadeType.MERGE
```

This is why:

```java
CascadeType.ALL
```

is convenient but broad.

Understand each cascade operation rather than treating cascade as a single switch.

---

# 50. Relationship ownership and SQL

Let's make this concrete.

Mapping:

```java
@Entity
class Order {

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

Suppose:

```java
order.setCustomer(customer);
```

At flush, JPA can produce something conceptually like:

```sql
UPDATE orders
SET customer_id = ?
WHERE id = ?
```

because:

```text
Order.customer
```

controls:

```text
orders.customer_id
```

Now:

```java
customer.getOrders().add(order);
```

without changing:

```java
order.customer
```

doesn't establish that owning-side foreign-key value.

This is the practical meaning of ownership.

---

# 51. Database diagram you should visualize

For:

```java
Customer
@OneToMany(mappedBy = "customer")
List<Order> orders;

Order
@ManyToOne
@JoinColumn(name = "customer_id")
Customer customer;
```

draw:

```text
+------------------+          +----------------------+
|    CUSTOMER      |          |        ORDER         |
+------------------+          +----------------------+
| id PK            |  1    N  | id PK                |
| name             |<---------| customer_id FK       |
+------------------+          | order_date           |
                              +----------------------+
```

Then annotate:

```text
Customer.orders
    = inverse side

Order.customer
    = owning side

ORDER.customer_id
    = actual FK
```

Once this diagram is clear, most `mappedBy` questions become easy.

---

# 52. A complete production-style mapping

### Customer

```java
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "customer_id",
        nullable = false
    )
    private Customer customer;

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
```

Notice several deliberate choices:

```text
OneToMany
    ↓
LAZY by default

ManyToOne
    ↓
explicitly changed to LAZY

mappedBy
    ↓
customer

JoinColumn
    ↓
customer_id

optional=false
    ↓
business relationship required

nullable=false
    ↓
DB column non-null

cascade=ALL
    ↓
child lifecycle follows aggregate

orphanRemoval=true
    ↓
removing child from relationship deletes it
```

This is the sort of mapping you should be able to explain line by line.

---

# 53. Why I explicitly used `LAZY` on `ManyToOne`

The JPA default for `ManyToOne` is eager. ([Jakarta EE][3])

But in application design, you often don't want every:

```java
Order
```

load to automatically bring a full:

```java
Customer
```

graph with it.

So you'll often encounter:

```java
@ManyToOne(fetch = FetchType.LAZY)
```

This is especially important for controlling query behavior.

Again, though:

> Don't blindly turn every association lazy and assume the problem is solved.

Fetch strategy must match the use case.

---

# 54. The N+1 problem — introduction

Suppose you do:

```java
List<Order> orders = orderRepository.findAll();
```

and then:

```java
for (Order order : orders) {
    System.out.println(
        order.getCustomer().getName()
    );
}
```

Potentially:

```text
1 query → load orders

+ N queries → load each customer
```

So:

```text
1 + N
```

queries.

This is the famous:

> **N+1 query problem**

Relationship mappings are one of the biggest places where N+1 arises.

We'll handle this deeply in a future topic, including:

```text
JOIN FETCH
@EntityGraph
batch fetching
DTO projections
query design
```

For now, understand why relationship mapping and fetch strategy can affect SQL.

---

# 55. Don't solve N+1 by making everything EAGER

This is a very common bad answer.

Someone sees:

```text
N+1
```

and says:

> "I'll use EAGER."

That can simply move the problem elsewhere and cause unnecessarily large object graphs and expensive queries.

The right question is:

> "What data does this particular use case require, and how should I fetch it efficiently?"

That often leads to:

```text
fetch join
@EntityGraph
projection
purpose-built query
```

rather than globally changing mappings.

---

# 56. `@ManyToMany` and cascade danger

Let's revisit:

```text
Student >───< Course
```

Suppose:

```java
@ManyToMany(
    cascade = CascadeType.ALL
)
private Set<Course> courses;
```

This can be dangerous because `Course` may be a shared entity.

Imagine:

```text
Student A ──> Java
Student B ──> Java
Student C ──> Java
```

Now deleting Student A shouldn't normally delete:

```text
Java course
```

because B and C still use it.

This demonstrates the rule:

> **Cascade follows lifecycle ownership, not simply navigational convenience.**

---

# 57. Aggregate ownership

This connects to domain-driven design thinking.

Good candidate:

```text
Invoice
  |
  +--- InvoiceLine
```

The line belongs to the invoice.

Potentially:

```java
cascade = ALL
orphanRemoval = true
```

Good candidate:

```text
Student
   |
   +---- Course
```

Course is shared.

Therefore:

```text
be careful with REMOVE cascade
```

The mapping should communicate the actual business lifecycle.

---

# 58. What is the "parent" and "child"?

People frequently say:

```text
Customer = parent
Order = child
```

That's useful shorthand, but don't confuse it with:

```text
owning side
```

They are related concepts, but technically:

```text
Customer
    = logical parent

Order
    = logical child

Order.customer
    = owning side
```

The parent isn't necessarily the owning side.

In the common `OneToMany`/`ManyToOne` mapping, the child is the owning side because it contains the foreign key.

That's a good interview nuance.

---

# 59. `mappedBy` doesn't create a column

Suppose:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

Does this create:

```text
CUSTOMER.orders
```

as a database column?

No.

There is no such database column.

Instead:

```text
ORDER.customer_id
```

stores the relationship.

So:

```text
Java collection
    ≠
database column
```

It is a representation of rows related through the foreign key.

---

# 60. Relationship table summary

| Java annotation            | Typical DB representation |
| -------------------------- | ------------------------- |
| `@ManyToOne`               | FK in source entity table |
| `@OneToMany` bidirectional | FK in child table         |
| `@OneToOne`                | unique FK or shared PK    |
| `@ManyToMany`              | join table                |

These are the standard mappings described by Jakarta Persistence. ([Jakarta EE][1])

---

# 61. The interview questions you absolutely need to master

### Q1. What is `mappedBy`?

> It identifies the association field/property on the other entity that owns the bidirectional relationship. The side using `mappedBy` is the inverse side. ([Jakarta EE][5])

---

### Q2. Which side owns a `OneToMany`/`ManyToOne` relationship?

Normally:

```text
ManyToOne
```

is the owning side because it contains the foreign-key mapping.

---

### Q3. Where is the foreign key?

For:

```text
Customer 1 ---- N Order
```

normally:

```text
ORDER.customer_id
```

The many side stores the FK. ([Jakarta EE][3])

---

### Q4. What is cascade?

> Cascade tells JPA which persistence operations should propagate from one associated entity to another. By default, no operations are cascaded. ([Jakarta EE][5])

---

### Q5. What is `orphanRemoval`?

> It causes an entity removed from an association to be treated as an orphan and removed according to the orphan-removal semantics of the mapping. ([Jakarta EE][5])

---

### Q6. Difference between cascade REMOVE and orphanRemoval?

```text
delete parent
    ↓
CascadeType.REMOVE

remove child from parent's relationship
    ↓
orphanRemoval
```

---

### Q7. What is the default fetch type of `ManyToOne`?

```text
EAGER
```

### `OneToMany`?

```text
LAZY
```

### `OneToOne`?

```text
EAGER
```

### `ManyToMany`?

```text
LAZY
```

These are the standard JPA defaults. ([Jakarta EE][1])

---

### Q8. Why use `mappedBy`?

To prevent both sides of a bidirectional association from trying to independently own/map the same relationship.

---

### Q9. What is `@JoinColumn`?

It specifies the column used to join an entity association; in a normal many-to-one mapping, that column is the foreign key in the source entity's table. ([Jakarta EE][4])

---

### Q10. Why is `ManyToMany` often replaced with an explicit entity?

Because the relationship itself may need attributes such as:

```text
createdAt
status
quantity
role
effectiveDate
```

In that case, the relationship is a real domain object.

---

# 62. The most important mental model

When you see:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

immediately think:

```text
Customer
   |
   | inverse side
   |
   v
Order.customer
   |
   | owning side
   |
   v
ORDER.customer_id
   |
   v
CUSTOMER.id
```

And when you see:

```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

think:

```text
@ManyToOne
     ↓
many orders → one customer

@JoinColumn
     ↓
ORDER.customer_id

owning side
     ↓
Order.customer
```

---

# 63. One final diagram tying everything together

```text
                         OBJECT MODEL
                         
                  +-------------------+
                  |     Customer      |
                  |-------------------|
                  | id                |
                  | name              |
                  |                   |
                  | orders            |  ← inverse side
                  +---------+---------+
                            |
                            | 1
                            |
                            | N
                            v
                  +-------------------+
                  |      Order        |
                  |-------------------|
                  | id                |
                  | orderDate         |
                  |                   |
                  | customer          |  ← owning side
                  +---------+---------+
                            |
                            |
                            v
                         customer_id


                         DATABASE

             CUSTOMER                 ORDER
        +---------------+       +------------------+
        | id PK         |       | id PK            |
        | name          |       | order_date       |
        +---------------+       | customer_id FK   |
                                +------------------+
```

That single picture explains:

```text
@OneToMany
@ManyToOne
mappedBy
@JoinColumn
owning side
foreign key
bidirectional relationship
```

---

# What you should know cold for the interview

You should now be able to explain, without memorizing definitions:

```text
@Entity
@Id
@Table

@OneToOne
@OneToMany
@ManyToOne
@ManyToMany

@JoinColumn
@JoinTable

mappedBy
owning side
inverse side

FetchType.LAZY
FetchType.EAGER

CascadeType.PERSIST
CascadeType.MERGE
CascadeType.REMOVE
CascadeType.ALL

orphanRemoval

unidirectional vs bidirectional

foreign key
join table

relationship entity

N+1 problem
```

And especially these three diagrams:

```text
1.

Customer 1 ─────── N Order

CUSTOMER
   id

ORDER
   id
   customer_id FK
```

```text
2.

Customer.orders
      ↕
Order.customer

Order.customer = owning side
Customer.orders = inverse side
```

```text
3.

@ManyToMany

Student
   |
   v
STUDENT_COURSE
   |
   v
Course
```

### The one sentence to remember

> **JPA relationship annotations describe the object relationship, but the owning side determines how that relationship is mapped to the database; in the common `@OneToMany`/`@ManyToOne` mapping, the `@ManyToOne` side owns the foreign key, `mappedBy` marks the inverse side, cascade controls propagation of entity operations, and `orphanRemoval` handles children removed from an owning relationship.** ([Jakarta EE][3])

**Next topic: JPA Entity Lifecycle & Persistence Context in depth — transient vs managed vs detached vs removed, `persist`, `merge`, `detach`, `refresh`, dirty checking, flush, commit, and exactly when SQL is generated.**

[1]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/entity?utm_source=chatgpt.com "Entity (Jakarta Persistence API documentation)"
[2]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/id?utm_source=chatgpt.com "Id (Jakarta Persistence API documentation)"
[3]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/manytoone?utm_source=chatgpt.com "ManyToOne (Jakarta Persistence API documentation)"
[4]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/joincolumn?utm_source=chatgpt.com "JoinColumn (Jakarta Persistence API documentation)"
[5]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/onetomany?utm_source=chatgpt.com "OneToMany (Jakarta Persistence API documentation)"
[6]: https://docs.hibernate.org/stable/orm/userguide/html_single/?utm_source=chatgpt.com "Hibernate ORM User Guide"
[7]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/onetoone?utm_source=chatgpt.com "OneToOne (Jakarta Persistence API documentation)"
[8]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/manytomany?utm_source=chatgpt.com "ManyToMany (Jakarta Persistence API documentation)"

