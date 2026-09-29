# Topic 20 — Spring Data JPA Querying in Depth

Now we get into one of the most practical JPA interview topics:

> **How do you query data in Spring Data JPA, and when do you choose each approach?**

You already know:

```text id="4v0j1u"
JpaRepository
    ↓
Persistence Context
    ↓
EntityManager
    ↓
Hibernate
    ↓
Database
```

Now we need to understand the different ways a repository can express a query:

```text id="r88vts"
1. Derived query methods
2. @Query with JPQL
3. Native SQL
4. Specifications
5. Criteria API
6. Projections
7. Pagination + Sorting
8. Bulk UPDATE / DELETE
9. EntityGraph / fetch strategies
```

Spring Data JPA 4.1.1 documents all of these as part of its query infrastructure. ([Home][1])

---

# 1. First understand the query stack

When you write:

```java
userRepository.findByEmail("john@example.com");
```

you aren't talking directly to SQL.

Conceptually:

```text
Your Java code
      ↓
Spring Data repository
      ↓
Query derivation / @Query / Specification
      ↓
JPA
      ↓
Hibernate
      ↓
SQL
      ↓
Database
```

JPA queries operate against the persistence model—entities, their state, and their relationships—and can then be translated by the persistence provider into the database's query language. ([Jakarta EE][2])

That gives us the first major distinction:

```text
JPQL
    → query the entity model

SQL
    → query the database model
```

---

# 2. Derived Query Methods

The easiest approach is:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    List<User> findByEmail(String email);
}
```

Spring Data parses the method name and derives the query.

This is called:

> **Query derivation**

Spring Data's query parser treats the part before the first `By` as the subject and the part after `By` as the predicate. ([Home][3])

---

# 3. Simple derived queries

```java
List<User> findByName(String name);
```

Conceptually:

```sql
WHERE name = ?
```

---

```java
Optional<User> findByEmail(String email);
```

Conceptually:

```sql
WHERE email = ?
```

---

```java
List<User> findByStatus(Status status);
```

Conceptually:

```sql
WHERE status = ?
```

The actual SQL generated depends on JPA/provider/database details.

---

# 4. `And`

```java
List<User> findByNameAndStatus(
        String name,
        Status status
);
```

Conceptually:

```sql
WHERE name = ?
AND status = ?
```

Spring Data supports combining predicates with keywords such as `And` and `Or`. ([Home][3])

---

# 5. `Or`

```java
List<User> findByNameOrEmail(
        String name,
        String email
);
```

Conceptually:

```sql
WHERE name = ?
OR email = ?
```

Again, Spring Data derives the query from the method structure.

---

# 6. Comparison operators

You can write:

```java
List<User> findByAgeGreaterThan(int age);
```

Conceptually:

```sql
WHERE age > ?
```

Or:

```java
List<User> findByAgeLessThan(int age);
```

Conceptually:

```sql
WHERE age < ?
```

Or:

```java
List<User> findByAgeGreaterThanEqual(int age);
```

Conceptually:

```sql
WHERE age >= ?
```

Spring Data documents keywords for comparisons, ranges, null checks, collection membership, string matching, and more. ([Home][4])

---

# 7. `Between`

```java
List<User> findByAgeBetween(
        int min,
        int max
);
```

Conceptually:

```sql
WHERE age BETWEEN ? AND ?
```

Very useful for dates:

```java
List<Invoice> findByIssuedDateBetween(
        LocalDate start,
        LocalDate end
);
```

---

# 8. `In`

```java
List<User> findByIdIn(List<Long> ids);
```

Conceptually:

```sql
WHERE id IN (?, ?, ?)
```

This is very common in real applications.

---

# 9. `NotIn`

```java
List<User> findByStatusNotIn(
        Collection<Status> statuses
);
```

Conceptually:

```sql
WHERE status NOT IN (...)
```

---

# 10. String matching

Spring Data supports query keywords such as:

```java
findByNameContaining(...)
findByNameStartingWith(...)
findByNameEndingWith(...)
```

Conceptually:

```text
Containing
    ↓
LIKE '%value%'

StartingWith
    ↓
LIKE 'value%'

EndingWith
    ↓
LIKE '%value'
```

Spring Data documents these string-matching keywords and case-sensitive/case-insensitive variants. ([Home][4])

---

# 11. Null checks

```java
List<User> findByEmailIsNull();
```

or:

```java
List<User> findByEmailIsNotNull();
```

Conceptually:

```sql
WHERE email IS NULL
```

or:

```sql
WHERE email IS NOT NULL
```

---

# 12. Sorting in derived queries

You can encode sorting:

```java
List<User> findByStatusOrderByCreatedAtDesc(
        Status status
);
```

Conceptually:

```sql
WHERE status = ?
ORDER BY created_at DESC
```

But for reusable APIs, `Sort` is often cleaner.

For example:

```java
List<User> findByStatus(
        Status status,
        Sort sort
);
```

Then:

```java
Sort sort =
        Sort.by("createdAt").descending();

repository.findByStatus(status, sort);
```

---

# 13. `Top` and `First`

You can limit results:

```java
User findFirstByEmailOrderByCreatedAtDesc(
        String email
);
```

Or:

```java
List<User> findTop10ByStatus(
        Status status
);
```

Spring Data's query derivation supports result-limiting keywords such as `Top` and `First`. ([Home][3])

---

# 14. Exists queries

Instead of:

```java
Optional<User> findByEmail(String email);
```

when you only need to know whether something exists:

```java
boolean existsByEmail(String email);
```

This is often semantically better.

Spring Data defines `exists…By` as an exists projection. ([Home][4])

---

# 15. Count queries

You can write:

```java
long countByStatus(Status status);
```

Conceptually:

```sql
SELECT COUNT(...)
WHERE status = ?
```

Spring Data supports `count…By` as a count projection. ([Home][4])

---

# 16. Delete derived queries

You can write:

```java
void deleteByStatus(Status status);
```

Spring Data JPA supports derived delete methods, but there is an important distinction between a derived delete and a bulk JPQL delete.

A derived delete query can load matching entities and then delete them individually, allowing entity lifecycle callbacks to run. A bulk `DELETE` query executes directly as a bulk operation and doesn't invoke those entity callbacks in the same way. ([Home][1])

That distinction becomes important later.

---

# 17. When derived queries become a problem

Simple:

```java
findByEmail(...)
```

Excellent.

Still fine:

```java
findByStatusAndDepartment(...)
```

But imagine:

```java
findByCustomerIdAndStatusAndIssuedDateBetweenAndTotalAmountGreaterThanAndPaymentTypeAndRegionOrderByIssuedDateDesc(...)
```

Now the method name itself has become a maintenance problem.

At that point, use:

```text
@Query
```

or:

```text
Specification
```

depending on the use case.

---

# 18. `@Query`

You can define a query explicitly:

```java
@Query("""
    select u
    from User u
    where u.email = :email
""")
Optional<User> findUserByEmail(
        @Param("email") String email
);
```

Spring Data JPA's `@Query` annotation allows you to declare a finder query directly on the repository method. ([Home][5])

---

# 19. JPQL

The important question:

### Is that SQL?

No.

This:

```java
select u
from User u
where u.email = :email
```

is JPQL.

Notice:

```text
User
```

is the entity.

And:

```text
u.email
```

is the Java entity property.

You're querying the JPA object model.

Jakarta Persistence defines JPQL as a query language over entities, their persistent state, and their relationships; the provider can compile it into a target language such as SQL. ([Jakarta EE][2])

---

# 20. JPQL vs SQL

### JPQL

```java
select u
from User u
where u.email = :email
```

### SQL

```sql
SELECT *
FROM users
WHERE email = ?
```

The important difference:

```text
JPQL → entities and fields
SQL  → tables and columns
```

This distinction is extremely important in interviews.

---

# 21. JPQL uses entity relationships

Suppose:

```java
Order
    @ManyToOne
    Customer customer;
```

You can write:

```java
@Query("""
    select o
    from Order o
    where o.customer.id = :customerId
""")
List<Order> findOrders(
        @Param("customerId") Long customerId
);
```

Notice:

```text
o.customer.id
```

You're navigating the Java/JPA relationship.

You don't have to write:

```sql
orders.customer_id
```

because you're not writing SQL.

---

# 22. JPQL joins

Suppose:

```text
Customer
   |
   +---- Orders
```

You can write:

```java
@Query("""
    select o
    from Order o
    join o.customer c
    where c.status = :status
""")
List<Order> findOrdersByCustomerStatus(
        @Param("status") Status status
);
```

This is a relational join expressed through the entity model.

---

# 23. `JOIN FETCH`

Now a very important performance concept.

Suppose:

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

`join fetch` is not simply an ordinary join for filtering.

It tells JPA to fetch the associated entity as part of the query result.

This is often used to address specific lazy-loading/N+1 use cases.

But it should be used deliberately because fetching large collections can multiply rows and produce unexpectedly large result sets.

---

# 24. `JOIN` vs `JOIN FETCH`

Think:

```text
JOIN
 ↓
use relationship in query
```

versus:

```text
JOIN FETCH
 ↓
use relationship + fetch associated entity
```

For example:

```sql
JOIN
```

might allow you to filter orders based on customer attributes.

Whereas:

```text
join fetch
```

also changes what gets initialized in the resulting entity graph.

This is why fetch joins are an important tool when dealing with lazy associations.

---

# 25. `@Query` parameters

You can use named parameters:

```java
@Query("""
    select u
    from User u
    where u.email = :email
      and u.status = :status
""")
List<User> findUsers(
        @Param("email") String email,
        @Param("status") Status status
);
```

This is generally much easier to read than positional parameters:

```java
where u.email = ?1
and u.status = ?2
```

Both are possible.

---

# 26. Aggregation queries

This is especially relevant to your earlier JPA work.

Suppose you need:

```text
total invoices
total amount
average amount
```

You can write:

```java
@Query("""
    select
        count(i),
        coalesce(sum(i.totalAmount), 0),
        coalesce(avg(i.totalAmount), 0)
    from Invoice i
    where i.status = :status
""")
Object[] getSummary(
        @Param("status") Status status
);
```

Conceptually:

```sql
COUNT(...)
SUM(...)
AVG(...)
```

The important distinction is:

```text
entity query
    ↓
returns entities

aggregation query
    ↓
returns scalar/aggregate result
```

---

# 27. Why `COALESCE` matters

Suppose:

```sql
SUM(...)
```

has no matching rows.

Depending on the expression, the result can be `NULL`.

You often want:

```sql
0
```

instead.

So:

```java
coalesce(sum(i.totalAmount), 0)
```

means:

```text
SUM returns null
        ↓
COALESCE
        ↓
0
```

This is particularly useful for summary/aggregation APIs.

---

# 28. DTO projections

Suppose the query returns:

```text
firstName
lastName
email
```

but your entity has:

```text
id
firstName
lastName
email
password
address
orders
...
```

You don't necessarily want the full entity.

Spring Data supports **projections** so you can selectively retrieve data. ([Home][6])

---

# 29. Interface projection

Define:

```java
public interface UserSummary {

    String getFirstName();

    String getLastName();

    String getEmail();
}
```

Then:

```java
List<UserSummary> findByStatus(Status status);
```

Spring Data can construct an interface-based projection for the query result. Spring Data JPA generally uses JPA `Tuple` queries to construct interface projection proxies. ([Home][6])

This is extremely convenient.

---

# 30. Class-based projection

You can also use a DTO:

```java
public record UserSummary(
        String firstName,
        String lastName,
        String email
) {
}
```

For JPQL, a class-based projection traditionally uses a constructor expression:

```java
@Query("""
    select new com.example.dto.UserSummary(
        u.firstName,
        u.lastName,
        u.email
    )
    from User u
    where u.status = :status
""")
List<UserSummary> findSummaries(
        @Param("status") Status status
);
```

Spring Data JPA documents constructor expressions for JPQL class-based projections. ([Home][6])

---

# 31. Why projections are useful

Suppose:

```text
User
---------
id
name
email
passwordHash
address
orders
roles
preferences
...
```

But your endpoint only needs:

```json
{
  "name": "John",
  "email": "john@example.com"
}
```

Loading a complete entity when you only need two values can be unnecessary.

Projection gives:

```text
database
   ↓
selected columns/data
   ↓
DTO/projection
   ↓
API
```

instead of:

```text
database
   ↓
full entity graph
   ↓
DTO
```

Spring Data explicitly describes projections as a way to retrieve partial views of an aggregate. ([Home][6])

---

# 32. One important projection limitation

Projections don't magically make every nested relationship cheap.

Spring Data notes that nested properties that require joins can cause the full nested property to be materialized rather than giving you arbitrarily fine-grained column selection across a graph. ([Home][6])

So don't think:

> "Projection always means exactly these columns and nothing else."

The query shape still matters.

---

# 33. Native SQL

Sometimes JPQL isn't sufficient or convenient.

Spring Data supports:

```java
@Query(
    value = """
        SELECT *
        FROM users
        WHERE email = :email
    """,
    nativeQuery = true
)
User findByEmailNative(
        @Param("email") String email
);
```

Current Spring Data JPA also provides:

```java
@NativeQuery
```

which is essentially a composed form of `@Query(nativeQuery = true)` with additional capabilities such as SQL result-set mapping. ([Home][1])

---

# 34. JPQL vs native SQL

| JPQL                                         | Native SQL                            |
| -------------------------------------------- | ------------------------------------- |
| Entity-oriented                              | Database-oriented                     |
| Uses entity names                            | Uses table names                      |
| Uses entity properties                       | Uses columns                          |
| More database-portable                       | Can use DB-specific features          |
| Provider translates to SQL                   | You write SQL                         |
| Generally preferred for standard JPA queries | Useful when DB-specific SQL is needed |

Jakarta Persistence explicitly defines JPQL as independent of the particular database schema, while native SQL is available when you need to operate directly against the database. ([Jakarta EE][2])

---

# 35. When would you use native SQL?

Possible reasons:

```text
database-specific function
complex SQL
vendor-specific feature
legacy schema/query
recursive CTE
window functions
specialized performance tuning
```

But don't use native SQL just because it looks familiar.

For ordinary entity queries:

```text
derived query
or
JPQL
```

is often cleaner.

---

# 36. Pagination

Suppose your database has:

```text
10 million users
```

Don't do:

```java
List<User> findAll();
```

and send 10 million records.

Use:

```java
Page<User> findByStatus(
        Status status,
        Pageable pageable
);
```

Then:

```java
Pageable pageable =
        PageRequest.of(
                0,
                20,
                Sort.by("createdAt").descending()
        );
```

Spring Data supports `Page` and `Slice` as repository result types and requires a `Pageable` parameter for these paginated methods. ([Home][7])

---

# 37. What is `Page`?

`Page<T>` gives you:

```text
content
page number
page size
total elements
total pages
...
```

It's useful when your API needs:

```text
"there are 137 total results"
```

Spring Data defines `Page` as a slice with additional information such as the total number of results. ([Home][7])

---

# 38. What is `Slice`?

`Slice<T>` primarily tells you:

```text
content
is there a next slice?
```

You don't necessarily need total-count information.

This can be useful for:

```text
infinite scrolling
"load more"
```

Spring Data explicitly describes `Slice` as a sized chunk with an indication of whether more data is available. ([Home][7])

---

# 39. Why `Page` can be more expensive

To produce:

```text
totalElements
totalPages
```

Spring Data may need a count query in addition to the content query.

For a complicated query:

```text
content query
+
count query
```

can be significantly more expensive than:

```text
content query
```

Spring Data's `@Query` API supports an explicit `countQuery` for pagination when deriving the count query isn't sufficient. ([Home][5])

This is why `Slice` can be attractive when the total count isn't actually required.

---

# 40. Sorting

You can pass:

```java
Sort sort =
        Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.asc("name")
        );
```

Then:

```java
repository.findByStatus(status, sort);
```

For pageable APIs:

```java
Pageable pageable =
    PageRequest.of(
        0,
        20,
        Sort.by("createdAt").descending()
    );
```

Spring Data's pageable abstraction includes page number, page size, offset, and sort information. ([Home][8])

---

# 41. Dynamic filtering — `Specification`

Now we reach one of the most useful enterprise querying techniques.

Suppose API filters are:

```text
status       optional
startDate    optional
endDate      optional
department   optional
minAmount    optional
maxAmount    optional
```

You could create an enormous derived method.

Don't.

This is where:

```java
Specification<T>
```

becomes useful.

Spring Data JPA's `Specification` API is built on the JPA Criteria API and provides reusable predicates that can be composed. ([Home][9])

---

# 42. Repository setup

You need:

```java
public interface UserRepository
        extends JpaRepository<User, Long>,
                JpaSpecificationExecutor<User> {
}
```

Then:

```java
repository.findAll(specification);
```

Spring Data JPA explicitly documents extending `JpaSpecificationExecutor` to support specifications. ([Home][9])

---

# 43. A Specification

Conceptually:

```java
public static Specification<User> hasStatus(Status status) {

    return (root, query, cb) ->
            cb.equal(root.get("status"), status);
}
```

This says:

```text
User.status = status
```

Another:

```java
public static Specification<User> createdAfter(
        LocalDate date) {

    return (root, query, cb) ->
            cb.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    date
            );
}
```

---

# 44. Compose specifications

Now:

```java
Specification<User> spec =
        Specification.where(hasStatus(status))
                     .and(createdAfter(startDate));
```

Then:

```java
repository.findAll(spec);
```

Conceptually:

```text
status = ?
AND createdAt >= ?
```

The key advantage is **composition**.

Spring Data's documentation highlights composition as one of the major reasons specifications are useful. ([Home][9])

---

# 45. Optional filters

This is where Specifications really shine.

Suppose:

```java
Specification<User> spec =
        Specification.where(null);

if (status != null) {
    spec = spec.and(hasStatus(status));
}

if (startDate != null) {
    spec = spec.and(createdAfter(startDate));
}

if (department != null) {
    spec = spec.and(hasDepartment(department));
}
```

Then:

```java
repository.findAll(spec);
```

You don't need 15 repository methods.

---

# 46. Why Specifications are good for REST search APIs

Imagine:

```text
GET /users?
    status=ACTIVE
    &department=IT
    &startDate=2026-01-01
    &endDate=2026-09-01
```

You can translate each optional API filter into a specification:

```text
status       → hasStatus()
department   → hasDepartment()
startDate    → issuedAfter()
endDate      → issuedBefore()
```

Then compose them.

That's an excellent use case.

---

# 47. This connects directly to your invoice filtering pattern

The same reasoning applies to the invoice filtering approach you've already worked with: your filters such as job numbers and an issued-date range naturally fit a composable `Specification`, and `JpaSpecificationExecutor` lets the normal repository execute that dynamically composed query. ([Home][9])

The important architectural distinction is:

```text
simple fixed query
    ↓
@Query / derived method

many optional filters
    ↓
Specification
```

---

# 48. Specification vs `@Query`

Consider:

```text
status is optional
startDate is optional
endDate is optional
jobNumbers are optional
amount range optional
```

With `@Query`, you'd end up with things like:

```java
where (:status is null or i.status = :status)
and (:startDate is null or i.issuedDate >= :startDate)
and ...
```

That can work.

But with many filters it becomes increasingly difficult to read and maintain.

With Specifications:

```text
hasStatus(status)
.and(issuedAfter(start))
.and(issuedBefore(end))
.and(hasJobNumbers(jobNumbers))
```

The individual predicates remain reusable.

---

# 49. Criteria API

Specifications are based on the JPA Criteria API.

Direct Criteria API looks like:

```java
CriteriaBuilder cb =
        entityManager.getCriteriaBuilder();

CriteriaQuery<User> query =
        cb.createQuery(User.class);

Root<User> user =
        query.from(User.class);

query.where(
    cb.equal(user.get("status"), Status.ACTIVE)
);

List<User> result =
        entityManager
            .createQuery(query)
            .getResultList();
```

Jakarta Persistence defines Criteria queries as object-based query definitions rather than string-based JPQL. The `CriteriaBuilder` constructs these query objects and `EntityManager.createQuery(...)` executes them. ([Jakarta EE][10])

---

# 50. Why would we use Criteria API directly?

It is useful when you need highly dynamic queries and don't want to manually construct query strings.

But:

```java
CriteriaQuery
CriteriaBuilder
Root
Predicate
Join
```

can become verbose.

That's why Spring Data's:

```java
Specification
```

is often a more convenient abstraction for dynamic predicates.

Think:

```text
Criteria API
   ↓
lower-level JPA query construction

Specification
   ↓
reusable/composable Spring Data abstraction
```

---

# 51. Specification lifecycle concept

Conceptually:

```text
Specification
      ↓
Predicate
      ↓
Criteria API
      ↓
JPA query
      ↓
Hibernate
      ↓
SQL
```

So Specification isn't a completely separate query engine.

It's a convenient way to construct Criteria-based predicates.

Spring Data documents this directly. ([Home][9])

---

# 52. Projections + Specifications

Modern Spring Data JPA allows you to combine these concepts.

You can take a specification and use the fluent query API to project results.

Conceptually:

```java
repository.findBy(
    specification,
    query -> query
        .as(UserSummary.class)
        .page(pageable)
);
```

Spring Data's current Specification API documents operations such as:

```text
sortBy
limit
as
project
first
one
all
page
slice
scroll
stream
count
exists
```

through its fluent query API. ([Home][9])

This is useful when you have:

```text
dynamic filters
+
pagination
+
DTO projection
```

all in one use case.

---

# 53. Bulk UPDATE

Now consider:

```java
@Modifying
@Query("""
    update User u
    set u.status = :status
    where u.department = :department
""")
int updateStatus(
        @Param("status") Status status,
        @Param("department") String department
);
```

The important annotation is:

```java
@Modifying
```

Spring Data uses it to treat the `@Query` method as a modifying query rather than a select query. It applies to operations such as `INSERT`, `UPDATE`, `DELETE`, and DDL statements. ([Home][11])

---

# 54. Why bulk updates are special

Suppose the persistence context currently contains:

```text
User#1 → ACTIVE
User#2 → ACTIVE
User#3 → ACTIVE
```

Then you execute:

```sql
UPDATE users
SET status = 'INACTIVE'
```

directly as a bulk operation.

Database:

```text
User#1 → INACTIVE
User#2 → INACTIVE
User#3 → INACTIVE
```

But the persistence context could still contain:

```text
User#1 → ACTIVE
User#2 → ACTIVE
User#3 → ACTIVE
```

Now:

```text
Persistence Context ≠ Database
```

Spring Data's documentation explicitly warns about this and explains that modifying queries don't automatically clear the persistence context by default. ([Home][1])

---

# 55. `clearAutomatically`

You can write:

```java
@Modifying(clearAutomatically = true)
@Query("""
    update User u
    set u.status = :status
""")
int updateAllStatus(Status status);
```

Then Spring can clear the persistence context after executing the modifying query.

Current Spring Data JPA also provides:

```java
flushAutomatically = true
```

to flush pending changes before executing the modifying query. ([Home][11])

So:

```text
flushAutomatically
    ↓
flush BEFORE bulk operation

clearAutomatically
    ↓
clear AFTER bulk operation
```

These are very good advanced interview points.

---

# 56. Bulk DELETE vs entity DELETE

This distinction is worth understanding.

### Entity deletion

```java
repository.delete(user);
```

works through entity lifecycle management.

### Bulk delete

```java
@Modifying
@Query("delete from User u where u.status = :status")
void deleteByStatus(Status status);
```

runs a bulk JPQL delete directly.

Spring Data JPA documents that derived delete methods and bulk delete queries have different behavior, especially regarding entity loading and lifecycle callbacks. ([Home][1])

---

# 57. Why bulk operations can be much faster

Suppose:

```text
1,000,000 rows
```

Entity-by-entity deletion:

```text
load entity
remove entity
load entity
remove entity
...
```

Bulk delete:

```sql
DELETE FROM users
WHERE status = 'INACTIVE'
```

One database operation can be dramatically more efficient.

But the tradeoff is:

```text
less entity lifecycle processing
+
persistence context synchronization concerns
```

So bulk operations are a performance tool, not a drop-in replacement for normal entity manipulation.

---

# 58. EntityGraph

Suppose you have:

```java
Order
  |
  +-- customer
  +-- lineItems
```

and the default mappings are lazy.

Instead of hardcoding a fetch join into every query, Spring Data JPA supports:

```java
@EntityGraph(attributePaths = {
    "customer",
    "lineItems"
})
Optional<Order> findById(Long id);
```

Spring Data JPA supports JPA `EntityGraph`s on repository methods, including dynamic fetch graphs using `attributePaths`. ([Home][12])

Think:

```text
@EntityGraph
    ↓
tell JPA what associations this query should fetch
```

This is another tool for controlling fetch plans.

---

# 59. `JOIN FETCH` vs `@EntityGraph`

Both can solve certain fetch requirements.

### `JOIN FETCH`

Expressed directly in the query:

```java
@Query("""
    select o
    from Order o
    join fetch o.customer
    where o.id = :id
""")
```

### `@EntityGraph`

Fetch plan expressed separately:

```java
@EntityGraph(attributePaths = "customer")
Optional<Order> findById(Long id);
```

Conceptually:

```text
JOIN FETCH
    ↓
query-specific fetch instruction

EntityGraph
    ↓
fetch-plan metadata
```

Neither is universally superior; the right choice depends on how you want to structure the repository/query.

---

# 60. The N+1 connection

Suppose:

```java
List<Order> orders =
        repository.findAll();
```

Then:

```java
for (Order order : orders) {
    order.getCustomer().getName();
}
```

Potentially:

```text
1 query
+
N customer queries
```

To address a specific use case, you might use:

```text
JOIN FETCH
```

or:

```text
@EntityGraph
```

or:

```text
DTO projection
```

The deeper lesson is:

> **Query design and entity mapping are connected.**

You cannot discuss JPA performance without talking about both.

---

# 61. Query method return types

A query can return:

```java
User
Optional<User>
List<User>
Page<User>
Slice<User>
Window<User>
```

among other supported forms. Spring Data JPA 4.1.1 documents these query return types explicitly. ([Home][7])

Choose based on the semantics of the use case.

For example:

```text
one expected result
    ↓
Optional<User>

many results
    ↓
List<User>

pagination with total
    ↓
Page<User>

pagination without total count
    ↓
Slice<User>
```

---

# 62. `Optional` vs nullable entity

Compare:

```java
User findByEmail(String email);
```

with:

```java
Optional<User> findByEmail(String email);
```

The first can return:

```text
null
```

when no result is found.

The second makes absence explicit:

```text
Optional.empty()
```

Spring Data's query return-type documentation describes these semantics. ([Home][7])

For "zero or one" lookups, `Optional` is generally a nice repository contract.

---

# 63. Single result and multiple results

Suppose:

```java
Optional<User> findByEmail(String email);
```

but the database contains two matching users.

That's a data/model problem.

Spring Data's documented query semantics expect at most one result for singular return types; multiple matching results can trigger `IncorrectResultSizeDataAccessException`. ([Home][7])

This is another reason database uniqueness constraints matter.

For something like email:

```text
business requirement
     ↓
unique email
     ↓
DB UNIQUE constraint
```

should usually reinforce:

```text
repository returns Optional<User>
```

---

# 64. Repository query selection — the decision tree

This is one of the most useful things to memorize.

### Question 1

Is the query very simple?

```text
findByEmail
findByStatus
findByNameAndStatus
```

Use:

```text
Derived query
```

---

### Question 2

Is it a fixed custom query?

```text
join
aggregation
complex condition
custom projection
```

Use:

```text
@Query
```

---

### Question 3

Is it database-specific?

```text
vendor-specific SQL
advanced SQL features
legacy query
```

Consider:

```text
@NativeQuery
```

---

### Question 4

Are filters dynamic and optional?

```text
status?
date?
department?
amount?
customer?
```

Use:

```text
Specification
```

---

### Question 5

Do you need only part of the entity?

Use:

```text
Projection
```

---

### Question 6

Do you need pagination?

Use:

```text
Pageable
+
Page/Slice
```

---

### Question 7

Do you need special association fetching?

Use:

```text
@EntityGraph
```

or:

```text
JOIN FETCH
```

depending on the query.

---

# 65. A real-world example

Suppose we have:

```java
@Entity
public class Invoice {

    @Id
    private Long id;

    private String jobNumber;

    private LocalDate issuedDate;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;
}
```

Now our requirements evolve.

### Simple lookup

```java
Optional<Invoice> findById(Long id);
```

Use derived query.

### Fixed status query

```java
List<Invoice> findByStatus(InvoiceStatus status);
```

Use derived query.

### Complex summary

```text
SUM(totalAmount)
COUNT(*)
conditional aggregates
```

Use:

```text
@Query
```

### Dynamic API filters

```text
jobNumbers optional
startDate optional
endDate optional
status optional
```

Use:

```text
Specification
```

### Summary only needs totals

Use:

```text
projection / aggregation DTO
```

This is how you should approach repository design instead of choosing one query mechanism for everything.

---

# 66. Aggregation + DTO

Instead of:

```java
Object[] result
```

you can use a DTO/projection.

For example:

```java
public record InvoiceSummary(
        BigDecimal totalNet,
        BigDecimal totalApproved
) {
}
```

Then use an appropriate JPQL constructor expression:

```java
@Query("""
    select new com.example.dto.InvoiceSummary(
        coalesce(sum(i.totalAmount), 0),
        coalesce(sum(
            case
                when i.status = 'PAID'
                then i.totalAmount
                else 0
            end
        ), 0)
    )
    from Invoice i
    where i.jobNumber in :jobNumbers
""")
InvoiceSummary getSummary(
        @Param("jobNumbers")
        List<String> jobNumbers
);
```

Now:

```text
query
   ↓
InvoiceSummary
```

rather than:

```text
Object[]
   ↓
manual casting
```

This is usually cleaner.

---

# 67. A subtle problem with aggregation queries

Suppose your main query is:

```sql
Invoice
LEFT JOIN LineItem
```

and you calculate:

```text
SUM(invoice.totalAmount)
```

What happens if an invoice has:

```text
3 line items
```

The invoice row can appear three times in the join.

So:

```text
invoice total
invoice total
invoice total
```

could be accidentally summed three times.

This is a **query correctness** issue, not just a JPA issue.

You need to understand how joins affect aggregation.

Possible solutions can include:

```text
separate aggregation
DISTINCT where logically appropriate
subqueries
grouping
careful query design
```

This is particularly important for the type of invoice summary query you were working with.

---

# 68. A critical interview point: JPQL is not procedural

You don't write JPQL like Java:

```java
if (...)
while (...)
for (...)
```

JPQL describes the desired result.

For example:

```java
select u
from User u
where u.status = :status
```

You're describing:

> Give me users whose status matches this value.

Hibernate/JPA determines how to turn that into SQL.

---

# 69. Query parameters and SQL injection

Never build JPQL/SQL like this:

```java
String query =
    "select u from User u where u.name = '"
    + name
    + "'";
```

Use parameters:

```java
@Query("""
    select u
    from User u
    where u.name = :name
""")
```

and:

```java
@Param("name")
```

This separates:

```text
query structure
+
query data
```

and is the standard safe way to bind values.

---

# 70. Why string concatenation is especially bad for dynamic filters

Don't do:

```java
query += " and status = '" + status + "'";
```

for arbitrary request values.

Instead use:

```text
Specification
Criteria
parameterized @Query
```

This gives you cleaner and safer query construction.

---

# 71. Native SQL pagination

There is another subtle point.

With simple native queries, Spring Data can sometimes rewrite queries for pagination/sorting. For more complex native queries, you may need a count query or appropriate query-parser support. Spring Data JPA's current documentation specifically discusses `countQuery` for native pagination. ([Home][1])

So don't assume:

> "Any arbitrary native SQL + Page automatically works perfectly."

Complex SQL may require explicit pagination metadata.

---

# 72. Query performance is not determined only by Java code

Suppose these two Java repository methods are:

```java
findByStatus(...)
```

and:

```java
findByStatusAndDepartment(...)
```

You can't conclude which is faster just from the method names.

Performance depends on:

```text
generated SQL
indexes
data volume
cardinality
join strategy
execution plan
database
network
fetch size
```

For JPA performance analysis:

> **Look at the SQL and the database execution plan.**

Don't reason only from Java.

---

# 73. Derived query vs hand-written SQL — interview answer

If asked:

### "Why not always use native SQL?"

Say:

> Spring Data derived queries and JPQL let us work at the entity/domain level and reduce database-specific coupling and boilerplate. Native SQL is useful when we need database-specific features or a query that is awkward or inefficient to express through JPA. The choice should be based on query complexity, portability, performance, and existing schema requirements.

That's a strong answer.

---

# 74. `@Query` vs Specification — interview answer

Say:

> `@Query` is useful for a known, fixed query. `Specification` is useful when query predicates need to be composed dynamically, especially when many filters are optional. Specifications build reusable predicates using the Criteria API. ([Home][9])

---

# 75. Specification vs Criteria API

Say:

> The JPA Criteria API is the lower-level programmatic API for constructing queries. Spring Data JPA's `Specification` provides a reusable predicate abstraction on top of Criteria, making it easier to compose dynamic filters. ([Jakarta EE][10])

Excellent interview answer.

---

# 76. `Page` vs `Slice`

Say:

> `Page` contains pagination information including total-result information, while `Slice` mainly tells you whether another chunk exists. `Page` can therefore require count-query work that a `Slice` doesn't need. ([Home][7])

---

# 77. Entity projection vs DTO projection

Suppose:

```java
List<User>
```

returns entities.

You get:

```text
managed entities
```

A projection might return:

```text
UserSummary
```

with only the required view.

So:

```text
Entity
   ↓
full persistent model

Projection
   ↓
read-oriented subset
```

This is especially useful for reporting/search APIs.

---

# 78. A very important query-design rule

Don't ask:

> "Which JPA annotation should I use?"

Ask:

> **"What data does this use case actually need?"**

Then choose the query shape.

For example:

```text
Need whole entity?
    ↓
entity query

Need two fields?
    ↓
projection

Need summary?
    ↓
aggregation

Need optional filters?
    ↓
Specification

Need specialized DB capability?
    ↓
native SQL

Need relationships initialized?
    ↓
fetch join / EntityGraph
```

This is the mindset experienced JPA developers use.

---

# 79. Query architecture for a REST API

Suppose:

```http
GET /api/invoices
```

supports:

```text
jobNumbers
status
startDate
endDate
page
size
sort
```

A good architecture might be:

```text
Controller
    ↓
InvoiceFilterRequest
    ↓
Service
    ↓
build Specification
    ↓
repository.findAll(spec, pageable)
    ↓
Projection / Entity
    ↓
DTO
    ↓
REST response
```

Conceptually:

```text
request
   ↓
optional filter parameters
   ↓
Specification predicates
   ↓
Pageable
   ↓
database query
   ↓
Page<Invoice>
```

This is a very common enterprise Spring architecture.

---

# 80. A useful repository example

```java
public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>,
                JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceNumber(
            String invoiceNumber
    );

    List<Invoice> findByStatus(
            InvoiceStatus status
    );

    Page<Invoice> findByJobNumber(
            String jobNumber,
            Pageable pageable
    );

    @Query("""
        select new com.example.InvoiceSummary(
            count(i),
            coalesce(sum(i.totalAmount), 0)
        )
        from Invoice i
        where i.status = :status
    """)
    InvoiceSummary getSummary(
            @Param("status") InvoiceStatus status
    );
}
```

Notice that the repository uses several mechanisms appropriately:

```text
findByInvoiceNumber
    → derived query

findByStatus
    → derived query

findByJobNumber + Pageable
    → derived + pagination

summary
    → @Query + aggregation + DTO
```

And dynamic filtering would be handled through:

```text
JpaSpecificationExecutor
```

---

# 81. What not to do

Don't create:

```text
500 repository methods
```

just because the API has 500 possible filter combinations.

For example:

```text
findByStatus()
findByStatusAndDate()
findByStatusAndDepartment()
findByStatusAndDateAndDepartment()
findByStatusAndDateAndDepartmentAndAmount()
...
```

This doesn't scale.

Dynamic query requirements are exactly where composable Specifications or another dynamic query mechanism become valuable.

---

# 82. One more important distinction: query vs fetch

Suppose:

```java
select o
from Order o
join o.customer c
where c.status = :status
```

This says:

> Join customer so I can use it in the query.

Whereas:

```java
select o
from Order o
join fetch o.customer
where c.status = :status
```

changes the fetching behavior as well.

So:

```text
JOIN
    = relational query operation

FETCH JOIN
    = relational query operation + entity fetch strategy
```

This distinction is extremely useful when debugging N+1 behavior.

---

# 83. The complete query toolbox

Think of Spring Data JPA as giving you layers:

```text
Level 1
────────
Derived methods

findByEmail()
findByStatus()
findByAgeGreaterThan()
```

```text
Level 2
────────
@Query / JPQL

join
aggregation
custom predicates
custom projection
```

```text
Level 3
────────
Specifications

dynamic optional filters
composable predicates
```

```text
Level 4
────────
Native SQL

database-specific / advanced SQL
```

And orthogonal tools:

```text
Projection
Pagination
Sorting
EntityGraph
Bulk operations
```

That's the complete picture.

---

# 84. Interview cheat sheet

### Derived query

```java
findByEmail(...)
```

Use for simple predictable predicates.

---

### `@Query`

```java
@Query("""
   select ...
   from ...
   where ...
""")
```

Use for fixed custom queries.

---

### Native query

```java
@NativeQuery(...)
```

Use when native SQL is justified.

Current Spring Data JPA 4.1.1 supports `@NativeQuery` as a shortcut around `@Query(nativeQuery = true)` with extra native-query features. ([Home][13])

---

### Specification

```java
repository.findAll(specification);
```

Use for dynamic predicates.

([Home][9])

---

### Projection

```java
UserSummary
```

Use when you only need a subset of data.

([Home][6])

---

### Page

```java
Page<User>
```

Use when total-pagination information is needed.

---

### Slice

```java
Slice<User>
```

Use when you mainly need "is there another chunk?"

([Home][7])

---

### `@Modifying`

```java
@Modifying
@Query("update ...")
```

Use for explicit modifying JPQL/native queries.

([Home][11])

---

### `@EntityGraph`

```java
@EntityGraph(attributePaths = ...)
```

Use to control which associations are fetched for a repository query.

([Home][12])

---

# 85. The five things EPAM is likely to probe

## 1. JPQL vs SQL

You should immediately say:

```text
JPQL → entities/properties
SQL  → tables/columns
```

---

## 2. Derived query vs `@Query`

```text
simple → derived

complex but fixed → @Query
```

---

## 3. `@Query` vs Specification

```text
fixed query → @Query

dynamic optional predicates → Specification
```

---

## 4. Page vs Slice

```text
Page  → total count information

Slice → whether another chunk exists
```

---

## 5. Bulk UPDATE vs normal entity update

```text
entity update
    → persistence context + dirty checking

bulk UPDATE
    → direct query operation
    → persistence-context synchronization concerns
```

Spring Data specifically warns that modifying queries can leave managed entities stale unless the persistence context is handled appropriately. ([Home][1])

---

# 86. EPAM interview scenario

Imagine they ask:

> "We have an API with 8 optional filters. How would you implement it?"

Don't immediately start writing:

```java
@Query(...)
```

A strong thought process is:

```text
8 optional filters
       ↓
dynamic query
       ↓
Specification
       ↓
JpaSpecificationExecutor
       ↓
Pageable
       ↓
Page/DTO
```

Example:

```java
Page<Invoice> result =
        invoiceRepository.findAll(
                specification,
                pageable
        );
```

That demonstrates architectural understanding rather than just annotation knowledge.

---

# 87. Another interview scenario

> "You only need the invoice ID, job number and total amount. Why load the whole Invoice entity?"

Answer:

> "I would consider a projection or DTO projection because the use case only needs a subset of the entity's data. That can reduce unnecessary data retrieval and avoid loading an entity graph that isn't needed." ([Home][6])

---

# 88. Another interview scenario

> "We need to update 500,000 rows. Would you load all entities and call `save()`?"

Usually not.

Consider a bulk:

```java
@Modifying
@Query(...)
```

operation if the business rules and lifecycle requirements allow it.

But then account for:

```text
persistence-context staleness
entity callbacks
flush/clear requirements
```

Spring Data's modifying-query behavior specifically documents these concerns. ([Home][1])

---

# 89. Another interview scenario

> "Why is our query returning duplicate parent entities?"

Suppose:

```text
Order
JOIN OrderLine
```

and one order has:

```text
5 lines
```

The join can produce:

```text
Order #1
Order #1
Order #1
Order #1
Order #1
```

So you need to reason about:

```text
join cardinality
+
distinct
+
pagination
+
aggregation
```

This is an SQL/query-model problem first, not a Spring annotation problem.

---

# 90. Another interview scenario

> "Why did my bulk update not change the entity I already loaded?"

Because:

```text
bulk query updates database
```

while:

```text
persistence context
```

may still contain the old entity state.

This is exactly why Spring Data's `@Modifying` has `clearAutomatically` and `flushAutomatically` options. ([Home][11])

---

# 91. The ultimate decision matrix

| Requirement                    | Preferred tool                |
| ------------------------------ | ----------------------------- |
| `findByEmail`                  | Derived query                 |
| `findByStatusAndDepartment`    | Derived query                 |
| Complex fixed JPQL             | `@Query`                      |
| Aggregation                    | `@Query` / projection         |
| Dynamic optional filters       | `Specification`               |
| Database-specific query        | Native SQL / `@NativeQuery`   |
| Partial fields                 | Projection                    |
| Pagination with totals         | `Page`                        |
| Pagination without total       | `Slice`                       |
| Update/delete many rows        | Bulk `@Modifying` query       |
| Specific association fetch     | `JOIN FETCH` / `@EntityGraph` |
| Highly dynamic low-level query | Criteria API                  |

That table is very useful for revision.

---

# 92. The whole query pipeline

Memorize this:

```text
                    Repository Method
                           |
          +----------------+----------------+
          |                |                |
       Derived           @Query        Specification
          |                |                |
          +----------------+----------------+
                           |
                           v
                       JPA Query
                           |
                    +------+------+
                    |             |
                  JPQL          Native
                    |             |
                    +------+------+
                           |
                           v
                        Hibernate
                           |
                           v
                           SQL
                           |
                           v
                       Database
```

Then:

```text
                       Query Result
                           |
          +----------------+----------------+
          |                |                |
        Entity          Projection      Aggregation
          |                |                |
          +----------------+----------------+
                           |
                           v
                         DTO/API
```

---

# 93. What you should know cold for EPAM

You should now be able to explain:

```text
Derived queries
@Query
JPQL
Native SQL
Specification
Criteria API

Projection
DTO projection
Interface projection

Page
Slice
Pageable
Sort

JOIN
JOIN FETCH
@EntityGraph

@Modifying
Bulk UPDATE
Bulk DELETE

Persistence-context staleness after bulk operations
```

And the core selection rule:

```text
simple query
    → derived method

fixed complex query
    → @Query

dynamic query
    → Specification

DB-specific query
    → native SQL

partial data
    → projection

large result set
    → pagination

special fetch requirement
    → JOIN FETCH / EntityGraph
```

---

# The one answer to memorize

> **Spring Data JPA gives several query mechanisms. Derived methods are ideal for simple predicates, `@Query` is useful for fixed custom JPQL or native queries, Specifications are suited to dynamically composed filters, projections are useful when only part of an entity is required, and `Page`/`Slice` handle pagination. All of these ultimately work through JPA infrastructure and the provider, which translates the query into database operations.** ([Home][1])

---

## Topic 20 revision questions

Before moving on, make sure you can answer:

```text
1. What is query derivation?
2. How does findByEmail() work?
3. What is JPQL?
4. JPQL vs SQL?
5. Why does JPQL use entity names instead of table names?
6. What is @Query?
7. When would you use @Query instead of a derived method?
8. What is a native query?
9. What is a projection?
10. Interface projection vs DTO projection?
11. What is Specification?
12. Why is Specification useful for optional filters?
13. Specification vs Criteria API?
14. Page vs Slice?
15. What is Pageable?
16. What is @Modifying?
17. Why can bulk UPDATE create stale entities in the persistence context?
18. What are clearAutomatically and flushAutomatically?
19. JOIN vs JOIN FETCH?
20. @EntityGraph vs JOIN FETCH?
21. How can N+1 happen?
22. How would you design an API with 8 optional filters?
23. How would you optimize a query that only needs 3 fields?
24. When would you use native SQL?
25. How can joins accidentally duplicate rows in aggregate queries?
```

**Next topic: JPA Fetching & Performance — Lazy vs Eager in real life, the N+1 problem, `JOIN FETCH`, `@EntityGraph`, batch fetching, Cartesian explosions, pagination with joins, and how to read Hibernate SQL to diagnose performance problems.**

[1]: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html?utm_source=chatgpt.com "JPA Query Methods :: Spring Data JPA"
[2]: https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m1?utm_source=chatgpt.com "Jakarta Persistence"
[3]: https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html?utm_source=chatgpt.com "Defining Query Methods :: Spring Data JPA"
[4]: https://docs.spring.io/spring-data/commons/reference/repositories/query-keywords-reference.html?utm_source=chatgpt.com "Repository query keywords :: Spring Data Commons"
[5]: https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/Query.html?utm_source=chatgpt.com "Query (Spring Data JPA 4.1.1 API)"
[6]: https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html?utm_source=chatgpt.com "Projections :: Spring Data JPA"
[7]: https://docs.spring.io/spring-data/jpa/reference/repositories/query-return-types-reference.html?utm_source=chatgpt.com "Repository query return types :: Spring Data JPA"
[8]: https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/domain/Pageable.html?utm_source=chatgpt.com "Pageable (Spring Data Core 4.1.1 API)"
[9]: https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html?utm_source=chatgpt.com "Specifications :: Spring Data JPA"
[10]: https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4?utm_source=chatgpt.com "Jakarta Persistence"
[11]: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/Modifying.html?utm_source=chatgpt.com "Modifying (Spring Data JPA Parent 4.1.0 API)"
[12]: https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/EntityGraph.html?utm_source=chatgpt.com "EntityGraph (Spring Data JPA 4.1.1 API)"
[13]: https://docs.spring.io/spring-data/data-jpa/reference/api/java/org/springframework/data/jpa/repository/NativeQuery.html?utm_source=chatgpt.com "NativeQuery (Spring Data JPA 4.1.1 API)"

