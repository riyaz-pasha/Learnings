# Topic 21 — JPA Fetching & Performance

This is where JPA becomes **really important for real-world applications and interviews**.

You can write perfectly valid JPA code and still have terrible database performance.

For example:

```java
List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    System.out.println(order.getCustomer().getName());
}
```

The code looks harmless.

But Hibernate may execute:

```text
1 query → load all orders
N queries → load each customer's data
```

So instead of:

```text
1 SQL query
```

you get:

```text
N + 1 SQL queries
```

Hibernate's documentation calls N+1 selects one of the most common causes of poor ORM performance and provides join fetching, batch fetching, and subselect fetching as ways to address it. ([Hibernate Documentation][1])

The goal of this topic is to make you comfortable with:

```text
LAZY
EAGER
N+1
JOIN FETCH
@EntityGraph
@BatchSize
SUBSELECT
Cartesian product
pagination + fetch joins
Open EntityManager in View
SQL logging
query statistics
indexes
```

---

# 1. First principle: fetching is about database round trips

When thinking about JPA performance, don't start with:

> "Is this annotation fast?"

Start with:

> **How many database round trips does this use case cause, and how much data does each round trip retrieve?**

Imagine:

```text
Database
   ↑
   |
1 SQL query
   ↑
Application
```

That's one round trip.

But:

```text
Database
   ↑
   | query 1
Application
   ↑
   | query 2
Database
   ↑
   | query 3
Application
   ↑
   ...
```

becomes expensive very quickly.

Hibernate's own performance guidance emphasizes minimizing round trips and explicitly planning the data needed by the unit of work. ([Hibernate Documentation][1])

---

# 2. LAZY vs EAGER

You already saw the defaults in the previous topic.

```text
@ManyToOne → EAGER by default
@OneToOne  → EAGER by default

@OneToMany → LAZY by default
@ManyToMany → LAZY by default
```

Those are JPA defaults. ([Hibernate Documentation][1])

But don't confuse:

```text
FetchType.LAZY / EAGER
```

with:

```text
exact SQL strategy
```

They're related, but they aren't literally synonymous with:

```text
LAZY = SELECT
EAGER = JOIN
```

Hibernate supports different fetching strategies such as `SELECT`, `JOIN`, `BATCH`, and `SUBSELECT`, and those strategies can interact with lazy/eager timing. ([Hibernate Documentation][2])

---

# 3. What does LAZY actually mean?

Suppose:

```java
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}
```

You execute:

```java
Order order = orderRepository.findById(1L)
        .orElseThrow();
```

Conceptually:

```text
Order loaded
    |
    +---- customer not necessarily initialized yet
```

Then:

```java
order.getCustomer().getName();
```

may cause Hibernate to fetch the customer.

Think:

```text
find Order
    ↓
Order available
    ↓
access customer
    ↓
fetch Customer
```

This deferred initialization is the main idea of LAZY loading.

---

# 4. EAGER does not mean "always one JOIN"

This is a very important interview trap.

Suppose:

```java
@ManyToOne(fetch = FetchType.EAGER)
private Customer customer;
```

You might think:

```sql
SELECT ...
FROM orders
JOIN customers ...
```

must happen.

Not necessarily.

Hibernate has multiple fetching strategies; a relationship can be eagerly loaded through different SQL strategies, including a separate select. Hibernate documents `SELECT` as potentially eager or lazy, and `JOIN` as inherently eager. ([Hibernate Documentation][2])

So:

> **EAGER describes when the association must be available, not a guarantee of a particular SQL shape.**

That's a very good interview answer.

---

# 5. Why EAGER can be dangerous

Suppose:

```java
Order
 ├── Customer
 ├── Payment
 ├── ShippingAddress
 └── OrderLines
```

and multiple relationships are eager.

Loading one order can cause a large object graph to be loaded even if your endpoint only needs:

```json
{
  "orderId": 123,
  "status": "PAID"
}
```

This can create:

```text
unnecessary data
+
extra SQL
+
larger memory use
+
larger joins
+
serialization problems
```

That's why many teams prefer to make fetch requirements explicit at the **query/use-case level** rather than relying on broad eager mappings.

---

# 6. The N+1 problem

Here's the classic example.

Entities:

```text
Order
   |
   +---- Customer
```

Repository:

```java
List<Order> orders =
        orderRepository.findAll();
```

Then:

```java
for (Order order : orders) {
    System.out.println(order.getCustomer().getName());
}
```

Suppose there are 100 orders.

Potential SQL:

```sql
-- Query #1
SELECT * FROM orders;
```

Then:

```sql
-- Query #2
SELECT * FROM customer WHERE id = 1;

-- Query #3
SELECT * FROM customer WHERE id = 2;

-- Query #4
SELECT * FROM customer WHERE id = 3;

...
```

Potentially:

```text
1 + 100
= 101 queries
```

That's N+1.

Hibernate specifically defines this pattern as retrieving N rows in an initial query and then issuing N subsequent queries for related data. ([Hibernate Documentation][1])

---

# 7. N+1 is not caused only by LAZY

This is another subtle point.

People often say:

> "N+1 happens because of lazy loading."

More accurately:

> **Lazy association access is a very common way to trigger N+1, but N+1 is fundamentally a query-design problem.**

Hibernate's documentation explicitly points out that the N+1 issue isn't a Hibernate-only limitation; even hand-written JDBC code can create the same inefficient access pattern. ([Hibernate Documentation][1])

So:

```text
LAZY
   ↓
can expose N+1

but

N+1
   ↓
is fundamentally inefficient data access
```

---

# 8. Solving N+1 with `JOIN FETCH`

Suppose:

```java
@Entity
class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}
```

You need orders **and their customers** for one use case.

Use:

```java
@Query("""
    select o
    from Order o
    join fetch o.customer
""")
List<Order> findOrdersWithCustomers();
```

Now Hibernate can fetch the orders and associated customers in the same SQL operation.

Hibernate's HQL documentation describes `join fetch` as overriding the laziness of an association and fetching it using a SQL join. ([Hibernate Documentation][3])

---

# 9. What SQL might that produce?

Conceptually:

```sql
SELECT
    o.id,
    o.order_date,
    o.customer_id,
    c.id,
    c.name
FROM orders o
JOIN customers c
    ON c.id = o.customer_id;
```

Instead of:

```text
1 query
+
N customer queries
```

you get a join-based query.

That's why fetch joins are such an important tool.

---

# 10. `JOIN FETCH` vs ordinary `JOIN`

Consider:

```java
select o
from Order o
join o.customer c
where c.status = :status
```

This joins the relationship for query logic.

But:

```java
select o
from Order o
join fetch o.customer
```

also tells Hibernate:

> **Fetch the associated customer as part of the resulting object graph.**

The distinction is critical.

```text
JOIN
  → use relationship in query

JOIN FETCH
  → use relationship + fetch association
```

Hibernate's HQL documentation explicitly describes this difference. ([Hibernate Documentation][3])

---

# 11. `LEFT JOIN FETCH`

Suppose some orders don't have a customer.

Using:

```java
join fetch
```

is an inner join.

Orders without a matching customer can therefore be excluded.

Use:

```java
left join fetch
```

when you want the parent rows retained even when the association is absent.

Hibernate's query documentation specifically distinguishes inner fetch joins from left fetch joins in this way. ([Hibernate Documentation][4])

Think:

```text
JOIN FETCH
    ↓
only rows with matching association

LEFT JOIN FETCH
    ↓
also retain rows without association
```

---

# 12. The best mental model for fetch joins

Suppose your endpoint needs:

```text
Order
+
Customer
+
LineItems
```

Instead of:

```text
load Order
   ↓
access Customer
   ↓
query Customer
   ↓
access LineItems
   ↓
query LineItems
```

plan the fetch:

```text
query
   ↓
Order + Customer + LineItems
   ↓
process object graph
```

Hibernate's performance guidance explicitly recommends determining the needed associations at the beginning of the unit of work and fetching them in one or a small number of queries. ([Hibernate Documentation][1])

---

# 13. But fetch joins have a danger

Suppose:

```text
Order
  |
  +---- Customer        (to-one)
  |
  +---- LineItems       (to-many)
```

Fetching both can be reasonable.

But suppose:

```text
Author
  |
  +---- Books
  |
  +---- Awards
```

and both are collections.

Now:

```sql
Author
JOIN Books
JOIN Awards
```

can create a Cartesian multiplication.

If an author has:

```text
10 books
5 awards
```

the joined result can contain roughly:

```text
10 × 5 = 50 rows
```

for that author.

Hibernate explicitly warns that fetching multiple many-valued associations in parallel can create a Cartesian product and very poor performance. ([Hibernate Documentation][3])

This is a huge interview point.

---

# 14. "More JOIN FETCH" is not always better

This is the trap:

> "N+1? I'll add five fetch joins."

Now you can have:

```text
Order
 ├── lineItems
 ├── payments
 ├── shipments
 ├── discounts
 └── auditEntries
```

and end up with an enormous SQL result.

So the correct principle is:

> **Fetch what the use case requires, but don't blindly fetch an entire object graph.**

---

# 15. To-one vs to-many fetch joins

A useful rule of thumb:

### To-one

```text
@ManyToOne
@OneToOne
```

are generally much safer to fetch together.

### To-many

```text
@OneToMany
@ManyToMany
```

can dramatically increase row counts.

Hibernate's current HQL documentation explicitly says fetching several to-one associations is fine, while fetching multiple collections in parallel can create Cartesian products. ([Hibernate Documentation][3])

---

# 16. `@EntityGraph`

Another major solution is:

```java
@EntityGraph
```

Spring Data JPA supports JPA entity graphs on repository methods and allows ad-hoc paths through `attributePaths`. ([Home][5])

Example:

```java
@EntityGraph(attributePaths = {"customer"})
Optional<Order> findById(Long id);
```

Conceptually:

```text
normal query
      +
specified fetch plan
```

This is an elegant way of saying:

> "For this repository method, fetch this association too."

---

# 17. Why `@EntityGraph` is useful

Suppose you have:

```java
OrderRepository
```

with many methods:

```java
findById(...)
findByStatus(...)
findByCustomerId(...)
findRecentOrders(...)
```

One use case needs:

```text
Order + Customer
```

but another only needs:

```text
Order
```

You don't necessarily want to change the global relationship mapping.

Instead:

```java
@EntityGraph(attributePaths = "customer")
Optional<Order> findById(Long id);
```

Now the fetch plan is attached specifically to this query method.

Spring Data JPA supports both named entity graphs and dynamically defined graphs via `attributePaths`. ([Home][5])

---

# 18. Named `EntityGraph`

You can define:

```java
@NamedEntityGraph(
    name = "Order.withCustomer",
    attributeNodes = {
        @NamedAttributeNode("customer")
    }
)
@Entity
public class Order {
}
```

Then:

```java
@EntityGraph(
    value = "Order.withCustomer"
)
Optional<Order> findById(Long id);
```

The named graph centralizes the fetch plan, while ad-hoc `attributePaths` lets you define it directly on the repository method. ([Home][6])

---

# 19. `FETCH` vs `LOAD` EntityGraph

Spring Data's `EntityGraph` supports:

```text
FETCH
LOAD
```

The current API defines:

### `FETCH`

Attributes explicitly listed in the graph are treated as eager; attributes not listed are treated as lazy.

### `LOAD`

Listed attributes are treated as eager; unspecified attributes keep their defined/default fetch behavior. ([Home][7])

This is a useful advanced detail, but for interviews the most important thing is:

```text
@EntityGraph
    ↓
query-specific fetch plan
```

---

# 20. `JOIN FETCH` vs `@EntityGraph`

Both can solve similar use cases.

### `JOIN FETCH`

```java
@Query("""
    select o
    from Order o
    join fetch o.customer
""")
```

The fetch requirement is expressed directly in the query.

### `@EntityGraph`

```java
@EntityGraph(attributePaths = "customer")
```

The fetch plan is expressed separately from the query.

Think:

```text
JOIN FETCH
    → query-based fetch specification

@EntityGraph
    → fetch-plan metadata
```

Neither is universally better.

---

# 21. Batch fetching

What if you don't want one giant join?

Hibernate supports **batch fetching**.

Suppose:

```text
Orders:
1
2
3
4
5
```

each references a customer.

Instead of:

```text
query orders

customer 1
customer 2
customer 3
customer 4
customer 5
```

Hibernate can batch the related IDs:

```sql
SELECT ...
FROM customers
WHERE id IN (1, 2, 3, 4, 5);
```

This is batch fetching.

Hibernate documents `BATCH` as loading a group of associated items using an `IN` restriction based on a batch size. ([Hibernate Documentation][2])

---

# 22. `@BatchSize`

Hibernate provides:

```java
@BatchSize(size = 20)
```

You can use it on an entity association or entity depending on the use case.

Conceptually:

```text
Instead of:

customer 1 → SELECT
customer 2 → SELECT
customer 3 → SELECT
...

Do:

customers 1-20 → one SELECT ... WHERE id IN (...)
```

Hibernate documents `@BatchSize` as a way to control batch fetching, and the default batch-fetch size is otherwise not enabled globally unless configured or explicitly annotated. ([Hibernate Documentation][8])

---

# 23. Batch fetching solves vs mitigates N+1

Important wording:

```text
JOIN FETCH
    → often eliminates the N+1 pattern

BATCH
    → reduces the number of follow-up queries
```

Suppose you have 100 orders.

Without batching:

```text
1 + 100 = 101 queries
```

With batch size 20:

```text
1 + 5 = 6 queries
```

That's dramatically better.

But it is still multiple queries.

Hibernate's own guide notes that batch fetching can mitigate N+1, while join fetching is the more direct solution when appropriate. ([Hibernate Documentation][1])

---

# 24. Subselect fetching

Hibernate also supports:

```text
SUBSELECT
```

Conceptually:

```text
First query:
find all Orders matching X

Then:
load associated collections using a subselect
based on the owners found by the first query
```

Hibernate documents `SUBSELECT` as a secondary select that uses the restriction from the owner-loading query, and notes it can be configured explicitly. ([Hibernate Documentation][2])

This can be useful in certain collection-fetching situations.

---

# 25. Batch vs subselect vs join fetch

Think:

### JOIN

```text
one query
+
join relationships
```

### BATCH

```text
one initial query
+
a few IN queries
```

### SUBSELECT

```text
one initial query
+
another query based on the initial owner selection
```

Hibernate describes all three as association-fetching strategies. ([Hibernate Documentation][2])

---

# 26. Which one should you prefer?

Don't memorize:

> "JOIN is always best."

More useful:

```text
Need associations for this specific use case?
    ↓
JOIN FETCH / EntityGraph

Join would cause huge Cartesian result?
    ↓
Consider batch/subselect or multiple carefully planned queries

Need only subset of data?
    ↓
Projection / DTO query
```

Hibernate's current guide generally favors join fetching, while also identifying batch/subselect as useful in cases where join fetching would create excessive result sizes. ([Hibernate Documentation][1])

---

# 27. The DTO alternative

Suppose the API needs:

```json
{
  "orderId": 10,
  "customerName": "Alice",
  "total": 2500
}
```

Instead of:

```text
Order
  ↓
Customer
  ↓
LineItems
  ↓
Payment
...
```

you can query exactly what you need:

```java
@Query("""
    select new com.example.dto.OrderSummary(
        o.id,
        c.name,
        o.total
    )
    from Order o
    join o.customer c
""")
List<OrderSummary> findOrderSummaries();
```

Now:

```text
Database
   ↓
required data only
   ↓
DTO
```

This can be much better for reporting/search endpoints.

---

# 28. Fetch join vs projection

Think:

### Entity use case

Need to manipulate a managed entity:

```text
Order
Customer
```

Then:

```text
fetch join / EntityGraph
```

can make sense.

### Read-only API

Need:

```text
id
name
total
```

Then:

```text
DTO projection
```

may be more appropriate.

This avoids loading a large object graph just to serialize a small response.

---

# 29. The "Open Session in View" issue

Spring Boot web applications currently register the Open EntityManager in View pattern by default, which allows lazy loading during view rendering; you can disable it using:

```properties
spring.jpa.open-in-view=false
```

The current Spring Boot documentation explicitly documents this default and property. ([Home][9])

This is important because it can hide lazy-loading problems.

---

# 30. What is Open EntityManager in View?

Conceptually:

```text
HTTP request
   |
   v
EntityManager opened
   |
Controller
   |
Service
   |
Repository
   |
response rendering
   |
EntityManager remains available
   |
HTTP response complete
```

So code that touches a lazy relationship later in request processing may still be able to initialize it.

That can make an application appear to work even though the service layer didn't explicitly fetch the required relationship.

---

# 31. Why OSIV can hide N+1

Suppose your service returns:

```java
List<Order>
```

and the web layer later does:

```java
order.getCustomer().getName()
```

With an open persistence context:

```text
still possible to lazy-load
```

So the API works.

But you might have silently created:

```text
1 + N queries
```

during serialization or response creation.

This is one reason explicit fetch planning is valuable.

---

# 32. Should we disable OSIV?

There isn't one universal answer.

But many enterprise teams choose:

```properties
spring.jpa.open-in-view=false
```

to force lazy-loading requirements to be handled within explicit transaction/service boundaries rather than allowing database access to leak into web rendering.

The important architectural question is:

> **Do we want our web layer to be able to trigger database queries implicitly?**

If not, disable OSIV and fetch what you need before leaving the transaction.

Spring Boot explicitly exposes this as a configurable property. ([Home][9])

---

# 33. A very common bad pattern

```java
@Transactional
public List<Order> getOrders() {
    return repository.findAll();
}
```

Then:

```text
transaction ends
       ↓
controller
       ↓
JSON serializer
       ↓
order.getCustomer()
```

If the persistence context is closed, this can produce:

```text
LazyInitializationException
```

or, with OSIV enabled, it may quietly issue more SQL.

Better:

```java
@Transactional
public List<OrderDto> getOrders() {

    List<Order> orders =
            repository.findOrdersWithCustomers();

    return orders.stream()
            .map(this::toDto)
            .toList();
}
```

Now the required data is fetched and mapped inside the transaction.

---

# 34. N+1 and serialization

This deserves attention.

Suppose:

```java
@GetMapping
public List<Order> getOrders() {
    return orderRepository.findAll();
}
```

Jackson serializes:

```text
Order
   ↓
customer
   ↓
orders
   ↓
customer
   ↓
...
```

You can accidentally trigger:

```text
lazy queries
+
huge graphs
+
recursive relationships
```

That's another reason DTOs are useful.

---

# 35. Pagination + collection fetch joins

This is an advanced and important area.

Suppose:

```text
Order
  |
  +---- LineItems
```

and you do:

```java
Page<Order>
```

with a collection `JOIN FETCH`.

A relational join produces one row per line item, not one row per order.

So:

```text
Order 1 → 5 line items
```

could create:

```text
5 SQL rows
```

for one order.

That complicates pagination.

---

# 36. The classic problem

Suppose you ask for:

```text
20 orders
```

but each order has 10 line items.

A naive SQL limit over the joined result might operate on:

```text
200 joined rows
```

rather than:

```text
20 logical Order entities
```

That's why collection fetch joins + pagination historically required special care.

Hibernate's current versions have improved this area, but behavior is **version/provider dependent**, so you should never give a blanket statement that "pagination + collection fetch join is always broken." Hibernate 7.4's documentation notes that supported databases can now process limits/pagination for collection fetch joins in SQL in cases that previously fell back to in-memory limiting. ([Hibernate Documentation][10])

For interviews, the safe answer is:

> **Collection fetch joins complicate pagination because joins multiply rows; the exact behavior depends on Hibernate version and database, so I would verify the generated SQL and avoid blindly combining large collection fetch joins with paginated queries.**

That's much better than quoting an outdated absolute rule.

---

# 37. A common solution: two-step query

For a large paginated parent collection:

### Step 1

Fetch IDs:

```text
20 Order IDs
```

### Step 2

Fetch those orders + required associations:

```text
Order IDs
   ↓
JOIN FETCH Customer / LineItems
```

Conceptually:

```text
Page query
    ↓
IDs
    ↓
fetch associated data
    ↓
assemble result
```

This gives you much more control over pagination.

The exact implementation varies, but the pattern is common.

---

# 38. Another solution: DTO projection

Instead of:

```text
Page<Order>
```

you might query:

```text
Page<OrderSummary>
```

where the SQL result is already shaped for the endpoint.

This avoids materializing a large entity graph.

For reporting/search APIs, this is often a strong design.

---

# 39. Cartesian product in detail

Let's understand why multiple collections are dangerous.

Suppose:

```text
Author A
  ├── Book1
  ├── Book2
  ├── Book3
  |
  ├── Award1
  └── Award2
```

Query:

```sql
author
JOIN books
JOIN awards
```

The database can produce:

```text
Book1 Award1
Book1 Award2
Book2 Award1
Book2 Award2
Book3 Award1
Book3 Award2
```

Six rows.

The logical data was:

```text
3 books
2 awards
```

but the result contains:

```text
3 × 2 = 6
```

combinations.

With:

```text
100 books
+
100 awards
```

you can get:

```text
10,000
```

joined combinations for one author.

Hibernate explicitly warns about exactly this kind of Cartesian explosion when fetching multiple collections in parallel. ([Hibernate Documentation][3])

---

# 40. This is why multiple collection fetching needs planning

Instead of:

```text
Author
 + books
 + awards
 + comments
 + payments
```

in one giant fetch join, consider:

```text
Query 1 → Author + to-one data
Query 2 → Books
Query 3 → Awards
```

or:

```text
Query 1 → Author + Books
Query 2 → Awards with batch/subselect
```

One or two well-designed queries can be much better than one huge join.

Hibernate's guide explicitly identifies batch/subselect fetching as useful when join fetching multiple collections would create an excessively large Cartesian result. ([Hibernate Documentation][11])

---

# 41. Measuring performance — don't guess

When debugging JPA performance:

> **Look at the generated SQL.**

Hibernate provides SQL logging options such as:

```properties
hibernate.show_sql=true
hibernate.format_sql=true
```

and logging categories including:

```text
org.hibernate.SQL
org.hibernate.orm.jdbc.bind
```

Hibernate's guide documents these configuration options and recommends SQL logging when troubleshooting generated SQL. ([Hibernate Documentation][1])

---

# 42. Example SQL logging

You might configure:

```properties
spring.jpa.properties.hibernate.format_sql=true
```

and logging:

```properties
logging.level.org.hibernate.SQL=DEBUG
```

Then instead of guessing what JPA did, you can actually observe:

```sql
select
    o1_0.id,
    o1_0.customer_id,
    o1_0.total
from orders o1_0
```

followed by:

```sql
select
    c1_0.id,
    c1_0.name
from customers c1_0
where c1_0.id=?
```

Immediately:

```text
Aha — N+1.
```

---

# 43. Hibernate statistics

Hibernate can also collect runtime statistics when:

```properties
hibernate.generate_statistics=true
```

is enabled.

Its `Statistics` API exposes information such as entity/query/cache statistics. Hibernate documents statistics as an observability tool for understanding ORM activity. ([Hibernate Documentation][1])

This can be very useful for diagnosing:

```text
number of queries
cache hits
entity operations
```

rather than relying only on logs.

---

# 44. Slow-query logging

Hibernate also provides:

```properties
hibernate.log_slow_query
```

to log queries exceeding a configured execution-time threshold.

It also supports:

```properties
hibernate.use_sql_comments=true
```

which can help identify the origin of SQL during investigation. ([Hibernate Documentation][1])

This is useful in production diagnostics.

---

# 45. SQL count matters more than Java elegance

Consider:

```java
List<Order> orders =
        repository.findAll();

orders.forEach(order -> {
    ...
});
```

The Java code might be beautiful.

But if it produces:

```text
501 SQL statements
```

for a page of 500 orders, that's a performance problem.

Conversely:

```java
@Query(...)
```

may look more complicated but produce:

```text
1 well-indexed SQL query
```

Therefore:

> **JPA performance must ultimately be evaluated at the SQL/database level.**

---

# 46. Indexes matter

Suppose your query is:

```sql
SELECT *
FROM invoices
WHERE job_number = ?
AND issued_date >= ?
AND issued_date <= ?;
```

JPA can generate a perfect query.

But if the database doesn't have useful indexes, it can still be slow.

So JPA performance requires both:

```text
application/query design
+
database design
```

Think:

```text
JPA
  ↓
SQL shape

Database
  ↓
indexes + execution plan
```

You need both.

---

# 47. Index on foreign keys

For:

```text
ORDER.customer_id
```

queries such as:

```sql
WHERE customer_id = ?
```

or joins on `customer_id` can benefit from an appropriate index.

JPA mapping doesn't automatically mean:

> "The database has the optimal indexes for this workload."

Schema design and indexing are separate concerns.

---

# 48. Composite indexes

Suppose the common query is:

```sql
WHERE job_number = ?
AND issued_date >= ?
AND status = ?
```

A carefully designed composite index can be much more valuable than three arbitrary individual indexes.

The exact best index requires examining:

```text
query patterns
selectivity
data distribution
DB optimizer
```

This is a database-level performance decision.

---

# 49. Don't fetch what you don't need

This rule is simple but incredibly powerful.

Bad:

```text
Customer
    ↓
Orders
    ↓
LineItems
    ↓
Products
    ↓
Categories
```

when the endpoint needs only:

```text
customer.id
customer.name
```

Better:

```text
SELECT required columns
    ↓
DTO/projection
```

This is why projections, fetch plans, and query-specific loading matter.

---

# 50. Entity graphs and API use cases

Suppose:

```text
GET /orders/{id}
```

needs:

```text
Order
Customer
```

while:

```text
GET /orders
```

needs only:

```text
Order
```

Don't change:

```java
@ManyToOne(fetch = FetchType.EAGER)
```

just to solve the first endpoint.

Instead:

```text
GET /orders/{id}
   ↓
@EntityGraph(customer)

GET /orders
   ↓
Order only
```

That's a much more precise design.

---

# 51. LAZY is a tool, not a performance solution by itself

This is important.

Changing:

```java
@ManyToOne(fetch = FetchType.EAGER)
```

to:

```java
@ManyToOne(fetch = FetchType.LAZY)
```

doesn't magically make the application fast.

It changes **when** the association is fetched.

You still need to decide:

```text
Should this query fetch it?
How?
Join?
Batch?
Projection?
EntityGraph?
```

That's the real performance question.

---

# 52. EAGER is not a performance optimization

Another common interview mistake:

> "I'll make everything EAGER so I don't get LazyInitializationException."

This can produce:

```text
huge queries
+
extra SQL
+
unnecessary data
+
large object graphs
```

The correct solution to a lazy-loading problem is usually to make the required data available intentionally.

Not:

```java
fetch = FetchType.EAGER
```

everywhere.

---

# 53. The Open Session in View trap

Suppose the service returns:

```java
Order
```

with:

```java
customer = LAZY
```

OSIV enabled:

```text
controller accesses customer
    ↓
database query may occur
```

Developer:

> "Everything works."

But production:

```text
100 orders
    ↓
100 extra queries
```

Now latency spikes.

This is why explicit fetch planning is far safer than relying on accidental lazy loading through the web layer.

Spring Boot currently documents OSIV as enabled by default for web applications and provides `spring.jpa.open-in-view=false` to turn it off. ([Home][9])

---

# 54. A good service-layer pattern

```java
@Transactional(readOnly = true)
public OrderDto getOrder(Long id) {

    Order order =
            repository.findOrderWithCustomer(id)
                    .orElseThrow();

    return new OrderDto(
            order.getId(),
            order.getCustomer().getName(),
            order.getTotal()
    );
}
```

Repository:

```java
@Query("""
    select o
    from Order o
    join fetch o.customer
    where o.id = :id
""")
Optional<Order> findOrderWithCustomer(
        @Param("id") Long id
);
```

Now:

```text
transaction
   ↓
fetch required graph
   ↓
map DTO
   ↓
leave transaction
```

Clean and predictable.

---

# 55. A better API for collections

Suppose:

```text
GET /orders
```

returns summaries.

Instead of:

```java
List<Order>
```

consider:

```java
Page<OrderSummary>
```

using a projection.

Then:

```text
database
   ↓
only required fields
   ↓
pagination
   ↓
DTO
   ↓
JSON
```

You avoid loading:

```text
Customer
LineItems
Payment
Shipping
```

unless the endpoint actually needs them.

---

# 56. How to diagnose an N+1 problem

An excellent interview answer is:

### Step 1

Enable SQL logging.

```text
org.hibernate.SQL
```

### Step 2

Count SQL statements.

```text
1 + N?
```

### Step 3

Identify what causes the repeated query.

Usually:

```text
lazy association access
```

### Step 4

Choose the correct fix:

```text
JOIN FETCH
@EntityGraph
batch fetching
projection
```

### Step 5

Re-measure.

Don't stop after changing an annotation.

Hibernate provides SQL logging and statistics specifically to help this kind of diagnosis. ([Hibernate Documentation][1])

---

# 57. Don't assume one query is always better

This is an advanced but important point.

Suppose:

```text
Query A → 1 million joined rows
```

while:

```text
Query A → 1000 parent rows
Query B → 1000 related rows
```

Two carefully targeted queries can be better than one giant query that creates a huge intermediate result.

Therefore:

> **Optimize data access, not merely query count.**

The goal is:

```text
minimal total work
```

not necessarily:

```text
minimum number of SQL statements
```

---

# 58. The hierarchy of performance thinking

When investigating a slow JPA endpoint, examine:

```text
1. Number of SQL queries
        ↓
2. Amount of data returned
        ↓
3. Join cardinality
        ↓
4. Index usage
        ↓
5. Query execution plan
        ↓
6. Persistence-context overhead
        ↓
7. Object mapping/materialization
        ↓
8. Network/serialization
```

That's a much stronger methodology than:

> "Change LAZY to EAGER."

---

# 59. Common EPAM scenario

### Interviewer:

> We load 100 orders and then accessing `order.getCustomer()` causes 101 queries. What is happening?

Answer:

> This is the N+1 select problem. The initial query loads the 100 orders, and accessing the customer association causes additional queries for the associated customers. I would inspect the generated SQL first and then consider a query-specific fetch join, `@EntityGraph`, batch fetching, or a projection depending on what the use case actually requires. Hibernate documents join fetching, batch fetching, and subselect fetching specifically as association-fetching strategies. ([Hibernate Documentation][1])

Excellent.

---

# 60. Another interview scenario

> Why not just make `Customer` EAGER?

Answer:

> EAGER changes the required loading semantics but doesn't guarantee a particular SQL strategy or eliminate unnecessary data retrieval. It can also cause large object graphs or additional queries. I'd prefer to define the fetch plan at the use-case/query level. Hibernate supports multiple fetching strategies, and Spring Data supports query-specific entity graphs. ([Hibernate Documentation][2])

---

# 61. Another scenario

> We have two `@OneToMany` collections. Can I `JOIN FETCH` both?

Answer:

> Technically a provider may allow it, but fetching multiple to-many collections in parallel can create a Cartesian product and a very large result set. I'd examine the cardinalities and consider multiple queries, batch/subselect fetching, or a different DTO-oriented query. Hibernate explicitly warns about this case. ([Hibernate Documentation][3])

---

# 62. Another scenario

> `JOIN FETCH` fixed our N+1 but now the query is extremely slow.

Possible reason:

```text
Cartesian explosion
```

or:

```text
too many columns
```

or:

```text
large joined result
```

or:

```text
missing indexes
```

or:

```text
poor execution plan
```

So:

```text
N+1 fixed
≠
performance automatically fixed
```

Always inspect the SQL and database behavior.

---

# 63. Another scenario

> Why does my lazy association work in the controller even though the transaction ended?

Possible explanation:

```text
Open EntityManager in View
```

may still be keeping the persistence context available during web request processing.

Spring Boot currently enables this pattern by default for web applications unless you disable it. ([Home][9])

---

# 64. Another scenario

> How would you return order summaries efficiently?

A good answer:

```text
DTO projection
+
pagination
+
only required joins
+
appropriate indexes
```

rather than:

```text
load entire Order entity graph
```

---

# 65. Important Hibernate-specific annotations

You should recognize:

```java
@BatchSize
@Fetch
```

`@BatchSize` controls batch fetching.

Hibernate's fetch modes include:

```text
SELECT
JOIN
BATCH
SUBSELECT
```

and Hibernate documents `@Fetch(SUBSELECT)` as a way to request subselect fetching for collections. ([Hibernate Documentation][8])

These are **Hibernate-specific**, not pure JPA.

That's another interview distinction:

```text
JPA standard
    ↓
FetchType.LAZY/EAGER
@EntityGraph
JOIN FETCH

Hibernate-specific
    ↓
@BatchSize
@Fetch
Hibernate statistics
```

---

# 66. JPA vs Hibernate performance features

This matters in interviews.

### Standard-ish JPA concepts

```text
FetchType
JOIN FETCH
EntityGraph
Criteria
JPQL
```

### Hibernate-specific features

```text
@BatchSize
@Fetch
Hibernate statistics
specific fetch configuration
Hibernate-specific query features
```

So if an interviewer asks:

> "Is `@BatchSize` standard JPA?"

Answer:

> No. `@BatchSize` is a Hibernate-specific annotation.

---

# 67. A clean fetch strategy example

Suppose:

```text
Invoice
 ├── Customer        to-one
 ├── LineItems       to-many
 └── Payments        to-many
```

Endpoint A:

```text
GET /invoices/{id}
```

needs:

```text
Invoice + Customer + LineItems
```

Use:

```text
join fetch / EntityGraph
```

Endpoint B:

```text
GET /invoices
```

needs only:

```text
Invoice id
status
total
```

Use:

```text
projection
+
pagination
```

Endpoint C:

```text
batch processing
```

needs large numbers of invoices with line items.

Consider:

```text
batch fetching
+
controlled batch size
+
flush/clear
```

This is the mindset that makes JPA performance manageable.

---

# 68. One more subtle issue: `DISTINCT`

Suppose:

```java
@Query("""
    select o
    from Order o
    join fetch o.lineItems
""")
List<Order> findOrders();
```

Because one order can have many line items, SQL produces multiple rows for the same order.

JPA/Hibernate has mechanisms around entity result handling, and `DISTINCT` is sometimes used:

```java
select distinct o
from Order o
join fetch o.lineItems
```

The exact SQL and duplicate handling are provider/version dependent, so don't treat `DISTINCT` as a universal fix for all fetch problems.

More importantly:

> `DISTINCT` does not magically eliminate the database work caused by the underlying join.

The database may still have to process all joined rows.

---

# 69. Query result size is the real issue

Suppose:

```text
10,000 orders
```

and:

```text
each order = 20 line items
```

A join can represent:

```text
200,000 joined rows
```

Even if you eventually get:

```text
10,000 Order objects
```

the database and network had to process a much larger intermediate result.

So always ask:

```text
How many logical entities?
How many joined rows?
```

This is a key database-performance mindset.

---

# 70. The final performance architecture

A healthy REST/JPA application often looks like:

```text
HTTP Request
      |
      v
Controller
      |
      v
Service @Transactional
      |
      v
Use-case-specific query
      |
      +---- projection
      |
      +---- fetch join
      |
      +---- EntityGraph
      |
      +---- Specification
      |
      v
Hibernate
      |
      v
SQL
      |
      v
Database
      |
      +---- indexes
      +---- execution plan
```

And you measure:

```text
SQL count
query duration
returned rows
entity count
cache statistics
```

rather than guessing.

---

# 71. EPAM interview cheat sheet

## LAZY

```text
Association isn't required to be initialized immediately.
```

It can reduce unnecessary initial loading but can expose N+1 if accessed repeatedly.

---

## EAGER

```text
Association must be available eagerly.
```

It does **not** mean "always JOIN."

---

## N+1

```text
1 query for parents
+
N queries for associations
```

One of the most common ORM performance problems. ([Hibernate Documentation][1])

---

## `JOIN FETCH`

```text
Fetch association as part of query join.
```

Great for specific use cases. ([Hibernate Documentation][3])

---

## `@EntityGraph`

```text
Specify query fetch plan.
```

Supported directly by Spring Data JPA. ([Home][5])

---

## `@BatchSize`

```text
Fetch several associated entities/collections at once using batched IDs.
```

Hibernate-specific. ([Hibernate Documentation][8])

---

## `SUBSELECT`

```text
Fetch associated collections using a secondary select based on owner selection.
```

Hibernate-specific. ([Hibernate Documentation][2])

---

## Multiple collection fetch joins

```text
Potential Cartesian explosion.
```

Use very carefully. ([Hibernate Documentation][3])

---

## OSIV

Spring Boot currently enables Open EntityManager in View for web applications by default; it can be disabled with:

```properties
spring.jpa.open-in-view=false
```

([Home][9])

---

# 72. The most important decision tree

When you see a performance problem:

```text
                    Slow JPA query
                          |
                          v
                  Inspect generated SQL
                          |
                  +-------+-------+
                  |               |
             N+1 queries      huge query
                  |               |
                  v               v
          +-------+------+    inspect joins
          |       |      |         |
        Fetch   Entity  Batch      |
        Join    Graph             |
          |       |                |
          +-------+                |
                  |                |
                  v                v
             re-measure      too many rows?
                                  |
                         +--------+--------+
                         |                 |
                        yes               no
                         |                 |
                    split queries      indexes /
                    projection         execution plan
```

That's a much stronger troubleshooting approach than simply changing fetch annotations.

---

# 73. What you should know cold for EPAM

You should now be able to explain:

```text
1. LAZY vs EAGER
2. Why EAGER doesn't necessarily mean JOIN
3. N+1 problem
4. Why N+1 happens
5. JOIN FETCH
6. JOIN vs JOIN FETCH
7. LEFT JOIN FETCH
8. @EntityGraph
9. FETCH vs LOAD graph semantics
10. @BatchSize
11. Hibernate BATCH fetching
12. SUBSELECT fetching
13. Multiple collection fetch joins
14. Cartesian product
15. Pagination + collection fetch join
16. DTO projection as a performance tool
17. Open EntityManager in View
18. Why OSIV can hide N+1
19. SQL logging
20. Hibernate statistics
21. Why indexes matter
22. Why query count alone isn't enough
23. How to diagnose an N+1 issue
24. How to select a fetch strategy per use case
```

---

# The one mental model to remember

Don't think:

```text
@Entity
   ↓
Hibernate magically makes it fast
```

Think:

```text
                 USE CASE
                    |
                    v
           What data do I need?
                    |
          +---------+---------+
          |         |         |
       Entity    DTO       Aggregate
          |         |         |
          v         v         v
      Fetch plan  Projection  Query
          |
    +-----+-----+
    |           |
JOIN FETCH   EntityGraph
    |
    +---- if too many rows ----+
    |                          |
Batch / Subselect         Multiple queries
    |                          |
    +------------+-------------+
                 |
                 v
                SQL
                 |
          +------+------+
          |             |
       indexes      execution plan
          |             |
          +------+------+
                 |
                 v
             performance
```

And the most important rule:

> **In JPA performance, don't optimize annotations in isolation. First determine the data the use case needs, inspect the SQL Hibernate actually generates, then choose an appropriate fetch plan—often `JOIN FETCH`, `@EntityGraph`, projection, batching, or multiple targeted queries—and verify the result at the database level.** ([Hibernate Documentation][1])

---

## Topic 21 revision questions

Before moving on, you should be able to answer these confidently:

```text
1. What exactly is LAZY loading?
2. What exactly is EAGER loading?
3. Does EAGER always use JOIN?
4. What is N+1?
5. Why does N+1 happen?
6. Can N+1 happen with EAGER?
7. How does JOIN FETCH solve N+1?
8. JOIN vs JOIN FETCH?
9. JOIN FETCH vs LEFT JOIN FETCH?
10. What is @EntityGraph?
11. @EntityGraph vs JOIN FETCH?
12. FETCH graph vs LOAD graph?
13. What is batch fetching?
14. What is @BatchSize?
15. What is SUBSELECT fetching?
16. Batch vs JOIN FETCH?
17. Why can two @OneToMany fetch joins be dangerous?
18. What is a Cartesian product in this context?
19. Why is pagination with collection fetch joins tricky?
20. How can DTO projections improve performance?
21. What is Open EntityManager in View?
22. Why can OSIV hide N+1?
23. How do you diagnose N+1 in production?
24. How do Hibernate SQL logs help?
25. What are Hibernate statistics?
26. Why aren't query count and latency the same thing?
27. Why do database indexes matter even when the JPA query is correct?
28. How would you optimize an endpoint that loads 1000 orders and each customer's name?
29. How would you optimize an endpoint that returns only 5 columns from a 30-column entity?
30. How would you handle an entity with three large `@OneToMany` collections?
```

**Next topic: JPA Locking & Concurrency — optimistic locking, `@Version`, lost updates, pessimistic locking, `SELECT ... FOR UPDATE`, race conditions, deadlocks, and how to make concurrent updates safe.**

[1]: https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com "A Short Guide to Hibernate 7"
[2]: https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com "Hibernate ORM User Guide"
[3]: https://docs.hibernate.org/stable/orm/querylanguage/html_single/?utm_source=chatgpt.com "A Guide to Hibernate Query Language"
[4]: https://docs.hibernate.org/orm/7.3/querylanguage/pdf/Hibernate_Query_Language.pdf?utm_source=chatgpt.com "A Guide to Hibernate Query Language"
[5]: https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/EntityGraph.html?utm_source=chatgpt.com "EntityGraph (Spring Data JPA 4.1.1 API)"
[6]: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html?utm_source=chatgpt.com "JPA Query Methods :: Spring Data JPA"
[7]: https://docs.spring.io/spring-data/data-jpa/reference/3.5-SNAPSHOT/api/java/org/springframework/data/jpa/repository/EntityGraph.EntityGraphType.html?utm_source=chatgpt.com "EntityGraph.EntityGraphType (Spring Data JPA 3.5.14-SNAPSHOT API)"
[8]: https://docs.hibernate.org/orm/7.2/javadocs/org/hibernate/cfg/FetchSettings.html?utm_source=chatgpt.com "FetchSettings (Hibernate Javadocs)"
[9]: https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com "SQL Databases :: Spring Boot"
[10]: https://docs.hibernate.org/orm/7.4/whats-new/?utm_source=chatgpt.com "What’s New in 7.4"
[11]: https://docs.hibernate.org/orm/7.1/introduction/pdf/Hibernate_Introduction.pdf?utm_source=chatgpt.com "A Short Guide to Hibernate 7"

