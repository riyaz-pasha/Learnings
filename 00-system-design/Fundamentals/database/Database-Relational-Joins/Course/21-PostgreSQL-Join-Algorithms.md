# 21. PostgreSQL Join Algorithms

So far, we've focused on **what JOIN means logically**.

Now we move one level deeper:

> **How does PostgreSQL actually execute a JOIN?**

When you write:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

you describe **what result you want**.

You don't explicitly tell PostgreSQL:

```text
"Use a Hash Join."
"Use a Nested Loop."
"Use a Merge Join."
```

PostgreSQL's **query planner/optimizer** decides how to execute the query.

---

# 1. Logical JOIN vs Physical JOIN

This distinction is extremely important.

When you write:

```sql
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
```

that's the **logical operation**:

```text
customers
     JOIN
orders
```

But PostgreSQL needs an actual algorithm to perform it.

Possible algorithms include:

```text
                 JOIN
                  |
        ┌─────────┼─────────┐
        ↓         ↓         ↓
   Nested Loop   Hash      Merge
                 Join       Join
```

These are **join algorithms**.

---

# 2. Why Does PostgreSQL Need Different Algorithms?

Because datasets can have very different characteristics.

Imagine:

### Case A

```text
customers = 10 rows
orders    = 10,000,000 rows
```

### Case B

```text
customers = 5,000,000 rows
orders    = 5,000,000 rows
```

### Case C

Both tables are already sorted by the join key.

Clearly, one strategy won't be optimal for every situation.

So PostgreSQL chooses an algorithm based on things such as:

* estimated number of rows
* available indexes
* table statistics
* data distribution
* join condition
* memory availability
* estimated cost
* whether data is already sorted
* filters applied before the join

---

# 3. The Three Main Join Algorithms

PostgreSQL primarily uses three join strategies:

| Algorithm   | Basic idea                                                |
| ----------- | --------------------------------------------------------- |
| Nested Loop | For each row in one side, find matching rows in the other |
| Hash Join   | Build a hash table for one side, then look up matches     |
| Merge Join  | Walk through two sorted inputs together                   |

Think of them as three different ways of solving:

```text
"Find rows where A.key = B.key"
```

---

# 4. Nested Loop Join

Let's start with the simplest concept.

Suppose:

```text
A:

1
2
3
```

and:

```text
B:

2
3
4
```

A nested loop conceptually does:

```text
for each row in A:
    find matching rows in B
```

Visual:

```text
A row 1
   ↓
scan/search B
   ↓
no match


A row 2
   ↓
scan/search B
   ↓
match 2


A row 3
   ↓
scan/search B
   ↓
match 3
```

The key idea is:

> **Take one row from the outer side and look for matching rows in the inner side.**

---

# 5. Simple Nested Loop

The most basic conceptual version is:

```text
A = 3 rows
B = 3 rows

A1 → compare with B1, B2, B3
A2 → compare with B1, B2, B3
A3 → compare with B1, B2, B3
```

Potentially:

```text
3 × 3 = 9 comparisons
```

For larger tables, that can become expensive.

But don't conclude:

> "Nested Loop is bad."

That's incorrect.

Nested Loop can be extremely efficient when the inner side can be accessed cheaply, especially through an index.

---

# 6. Nested Loop + Index

Suppose:

```text
customers
---------
id
```

has:

```text
10 customers
```

and:

```text
orders
------
customer_id
```

has:

```text
10 million orders
```

If PostgreSQL has an index:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

it can conceptually do:

```text
customer 1
   |
   ↓
index lookup orders.customer_id = 1
   ↓
matching orders


customer 2
   |
   ↓
index lookup
   ↓
matching orders


customer 3
   |
   ↓
index lookup
```

Instead of scanning all 10 million orders for every customer.

---

# 7. Nested Loop Mental Model

Think:

```text
Small outer input
       |
       +---- lookup inner
       |
       +---- lookup inner
       |
       +---- lookup inner
       |
       ↓
     results
```

This can be excellent when:

* outer side is small
* inner side has a useful index
* few rows are expected
* highly selective filtering exists

---

# 8. Example Query

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

Suppose only one customer matches:

```text
customers
   ↓
1 row
   ↓
index lookup into orders
   ↓
matching orders
```

Nested Loop can be a very natural strategy here.

---

# 9. Hash Join

Now consider two large tables.

Suppose:

```text
customers = 5 million rows
orders    = 20 million rows
```

Scanning an index separately for every customer may not be attractive.

PostgreSQL can instead use a **Hash Join**.

The basic idea:

> Build a hash table for one input, then use it to quickly find matching rows from the other input.

---

# 10. Hash Join Step 1: Build

Suppose:

```text
customers

id
--
1
2
3
4
```

PostgreSQL might build a hash table using:

```text
customer.id
```

Conceptually:

```text
customers
    |
    | hash(id)
    ↓
┌───────────────┐
│ Hash Table     │
├───────────────┤
│ 1 → customer  │
│ 2 → customer  │
│ 3 → customer  │
│ 4 → customer  │
└───────────────┘
```

This is called the **build side**.

---

# 11. Hash Join Step 2: Probe

Now PostgreSQL reads rows from `orders`.

For each order:

```text
order.customer_id
       |
       ↓
    hash()
       |
       ↓
look in hash table
```

Example:

```text
Order 101
customer_id = 2
       |
       ↓
    hash(2)
       |
       ↓
customer 2 found
       |
       ↓
produce joined row
```

Then:

```text
Order 102
customer_id = 4
       |
       ↓
    hash(4)
       |
       ↓
customer 4 found
```

This is the **probe side**.

---

# 12. Hash Join Visual

```text
                customers
                    |
                    | build hash table
                    ↓
            ┌───────────────┐
            │ Hash Table    │
            │               │
            │ 1 → Alice     │
            │ 2 → Bob       │
            │ 3 → Charlie   │
            └───────┬───────┘
                    ↑
                    |
                  probe
                    |
                  orders
```

The two key phases are:

```text
BUILD
  ↓
HASH TABLE
  ↓
PROBE
```

---

# 13. Why Is Hash Join Useful?

For large equality joins:

```sql
ON a.id = b.a_id
```

a hash table can make matching efficient.

Hash Join is particularly useful when:

* both inputs are relatively large
* join is an equality condition
* sorting isn't already advantageous
* enough memory is available for the hash operation

---

# 14. Important Limitation of Hash Join

Hash joins are fundamentally designed around **equality matching**.

For example:

```sql
ON a.id = b.a_id
```

works naturally.

But consider:

```sql
ON a.salary > b.minimum_salary
```

That's not a simple equality lookup.

A hash table is not naturally suited to answering:

```text
"Find every B where B.minimum_salary < A.salary"
```

So other strategies may be more appropriate.

---

# 15. Merge Join

The third major algorithm is the **Merge Join**.

The key idea:

> **If both inputs are sorted by the join key, walk through them together.**

Suppose:

```text
A:

1
3
5
7
```

and:

```text
B:

2
3
5
8
```

Both are sorted.

Now compare the current values.

```text
A → 1
B → 2
```

Since:

```text
1 < 2
```

advance A.

```text
A → 3
B → 2
```

Since:

```text
2 < 3
```

advance B.

Now:

```text
A → 3
B → 3
```

Match!

Then:

```text
A → 5
B → 5
```

Match again.

---

# 16. Merge Join Visual

```text
A: 1  3  5  7
   ↑
   
B: 2  3  5  8
   ↑
```

Compare:

```text
1 vs 2
 ↓
advance A

3 vs 2
 ↓
advance B

3 vs 3
 ↓
MATCH

5 vs 5
 ↓
MATCH
```

The inputs are effectively processed like two sorted lists being merged.

---

# 17. Why Is It Called Merge Join?

Because the algorithm resembles the merge phase of merge sort.

You have:

```text
sorted A
sorted B
   ↓
merge them by key
   ↓
matching rows
```

---

# 18. Where Does the Sorting Come From?

There are several possibilities.

The data might already be ordered because of:

* an index scan
* a previous operation
* an explicit sort
* another part of the query plan

If PostgreSQL needs to sort both inputs first, that sorting has a cost.

So Merge Join isn't automatically better just because the inputs can be sorted.

The planner compares the total estimated cost.

---

# 19. Comparing the Three

A simplified mental model:

| Join algorithm | Mental model           | Often useful when                             |
| -------------- | ---------------------- | --------------------------------------------- |
| Nested Loop    | For each A, find B     | Small outer input / indexed lookup            |
| Hash Join      | Build hash, probe hash | Large equality joins                          |
| Merge Join     | Walk two sorted inputs | Inputs already sorted / sorting is worthwhile |

Don't turn this into rigid rules.

The PostgreSQL planner makes the actual decision.

---

# 20. One Query, Different Possible Algorithms

Consider:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

The SQL doesn't specify:

```text
Nested Loop
```

or:

```text
Hash Join
```

or:

```text
Merge Join
```

PostgreSQL decides.

Conceptually:

```text
                 SQL query
                     |
                     ↓
               Query Planner
                     |
          ┌──────────┼──────────┐
          ↓          ↓          ↓
     Nested Loop  Hash Join  Merge Join
          |          |          |
          └──────────┼──────────┘
                     ↓
                chosen plan
```

---

# 21. The Optimizer's Goal

PostgreSQL estimates the cost of possible plans.

Very simplified:

```text
Plan A → estimated cost 100
Plan B → estimated cost 40
Plan C → estimated cost 80

Choose Plan B
```

The actual PostgreSQL cost model is much more sophisticated.

It considers things such as:

* estimated row counts
* sequential scan cost
* random page access
* CPU operations
* sorting
* hashing
* indexes
* join order
* available memory
* selectivity

---

# 22. Statistics Matter

PostgreSQL needs information about your data to make good estimates.

For example:

```text
customers
10,000 rows
```

PostgreSQL may know approximately:

```text
customer_id distribution
status distribution
number of distinct values
NULL frequency
```

These statistics help it estimate:

```text
"How many rows will this filter produce?"
```

and:

```text
"How many rows will this JOIN produce?"
```

That estimate influences the chosen join algorithm.

---

# 23. Example of Join Selectivity

Suppose:

```sql
WHERE c.id = 42
```

PostgreSQL might estimate:

```text
customers → 1 row
```

Then:

```text
1 customer
   ↓
index lookup
   ↓
orders
```

Nested Loop becomes attractive.

But without the filter:

```text
customers → 5 million rows
orders    → 20 million rows
```

a Hash Join might become more attractive.

Same JOIN.

Different data/filters.

Potentially different algorithm.

---

# 24. Join Order Also Matters

Suppose:

```text
A JOIN B JOIN C
```

PostgreSQL doesn't necessarily execute:

```text
A → B → C
```

in the exact textual order you imagine.

It can consider different join orders.

For example:

```text
(A JOIN B) JOIN C
```

versus:

```text
A JOIN (B JOIN C)
```

The best order can dramatically affect intermediate row counts.

This connects directly to what we learned earlier about **join explosion**.

---

# 25. Example

Imagine:

```text
customers = 1,000,000
orders = 20,000,000
payments = 25,000,000
```

Suppose only:

```text
payments WHERE status = 'FAILED'
```

produces 100,000 rows.

It may be beneficial to filter that dataset early:

```text
payments
   |
   | status = FAILED
   ↓
100,000 rows
   |
   ↓
JOIN orders
   |
   ↓
JOIN customers
```

rather than carrying 25 million rows through the query.

The optimizer tries to find efficient strategies based on estimates.

---

# 26. Indexes and Join Algorithms

Indexes don't mean:

> "PostgreSQL will always use Nested Loop."

That's a common misunderstanding.

An index can make certain access paths cheaper, which may make a Nested Loop attractive.

But PostgreSQL can still choose:

```text
Hash Join
```

or:

```text
Merge Join
```

depending on the overall cost.

For example:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

can make:

```text
customer → lookup orders
```

efficient.

But if you're joining millions of rows, PostgreSQL might still determine that a Hash Join is cheaper.

---

# 27. Hash Join and Indexes

Another important misconception:

> "Hash Join requires an index."

No.

A Hash Join can work without an index on the join columns.

Conceptually:

```text
customers
   ↓
build hash

orders
   ↓
scan + probe
```

It can use sequential scans.

This is one reason Hash Join can be very effective for large equality joins.

---

# 28. Merge Join and Indexes

Indexes can also help provide sorted data.

For example, an index on:

```sql
orders(customer_id)
```

can potentially provide rows in `customer_id` order through an appropriate index scan.

That can make a Merge Join more attractive if the overall cost works out.

Again:

> **An index is an access path, not a guarantee of a particular join algorithm.**

---

# 29. A Useful Analogy

Imagine you're matching two lists of people.

### Nested Loop

Take one person from list A:

```text
Alice
```

search through list B for Alice.

Then:

```text
Bob
```

search through B.

Then:

```text
Charlie
```

search through B.

---

### Hash Join

Build a dictionary:

```text
Alice   → information
Bob     → information
Charlie → information
```

Then look up each person from the other list.

---

### Merge Join

Sort both lists:

```text
A:
Alice
Bob
Charlie

B:
Alice
Charlie
David
```

Walk through both lists together.

---

# 30. The Most Important Execution Concepts

You should now know these terms:

### Outer side

The side PostgreSQL processes as the outer input in a Nested Loop.

### Inner side

The side PostgreSQL accesses for each outer row in a Nested Loop.

### Build side

The input used to build the hash table in a Hash Join.

### Probe side

The input used to search the hash table in a Hash Join.

### Sorted inputs

Required for the merge phase of a Merge Join.

---

# 31. Join Algorithm vs Join Type

Don't confuse these.

We previously learned:

```text
INNER JOIN
LEFT JOIN
RIGHT JOIN
FULL JOIN
CROSS JOIN
```

Those describe **join semantics**.

Now we're learning:

```text
Nested Loop
Hash Join
Merge Join
```

Those describe **execution algorithms**.

They answer different questions.

### Join type

> What rows should the result contain?

### Join algorithm

> How should PostgreSQL efficiently find those matching rows?

---

# 32. Visualizing the Difference

```text
SQL

A
|
| INNER JOIN
|
B
|
↓
What should result contain?
        ↓
     JOIN TYPE
        ↓
     INNER JOIN


Then PostgreSQL asks:

"How should I execute it?"
        |
   ┌────┼────┐
   ↓    ↓    ↓
Nested Hash Merge
Loop   Join  Join
```

This distinction is fundamental.

---

# 33. Can LEFT JOIN Use Hash Join?

Yes.

The SQL join type and physical algorithm are separate concepts.

For example, PostgreSQL can potentially execute a LEFT JOIN using a:

```text
Hash Left Join
```

Similarly, you may see:

```text
Nested Loop Left Join
```

or:

```text
Merge Left Join
```

So don't think:

```text
LEFT JOIN = Nested Loop
```

That is incorrect.

---

# 34. Why You Should Learn This

At first, join algorithms may seem like PostgreSQL internals.

But they become extremely useful when debugging slow queries.

Suppose you run:

```sql
EXPLAIN ANALYZE
SELECT ...
FROM customers c
JOIN orders o
    ON c.id = o.customer_id;
```

and PostgreSQL reports:

```text
Hash Join
```

Now you understand:

```text
Hash Join
   ↓
build hash table
   ↓
probe with other input
```

If you see:

```text
Nested Loop
```

you understand:

```text
outer rows
   ↓
repeated inner lookup
```

And if you see:

```text
Merge Join
```

you understand:

```text
sorted input
   ↓
walk both inputs together
```

This makes execution plans much less mysterious.

---

# 35. Quick Comparison

```text
                    JOIN
                     |
        ┌────────────┼────────────┐
        ↓            ↓            ↓
   Nested Loop    Hash Join    Merge Join
        |            |            |
        ↓            ↓            ↓
    repeated      hash table    sorted
    lookup        lookup        inputs
        |            |            |
        ↓            ↓            ↓
 small/selective  large = join   sorted data
 indexed lookup   equality       can help
```

---

# 36. What Determines the Choice?

A simplified decision model:

```text
                    JOIN
                      |
              What does data look like?
                      |
          ┌───────────┼────────────┐
          ↓           ↓            ↓
    Small/selective  Large       Sorted
    indexed lookup  equality     inputs
          |           |            |
          ↓           ↓            ↓
    Nested Loop    Hash Join    Merge Join
```

But remember:

> This is a **mental model**, not PostgreSQL's actual decision tree.

The actual planner evaluates costs.

---

# 37. The Golden Rules

1. SQL JOIN describes **logical semantics**.
2. PostgreSQL chooses a **physical join algorithm**.
3. The three major algorithms are:

   * Nested Loop
   * Hash Join
   * Merge Join
4. Nested Loop repeatedly accesses the inner side for outer rows.
5. Nested Loop can be excellent with a small/selective outer input and useful index.
6. Hash Join builds a hash table and probes it.
7. Hash Join is especially useful for large equality joins.
8. Hash Join does not require an index.
9. Merge Join processes sorted inputs together.
10. Indexes can sometimes provide useful ordering.
11. Indexes do not guarantee Nested Loop.
12. The same SQL query can use different algorithms under different conditions.
13. Join type (`INNER`, `LEFT`, etc.) is different from join algorithm (`Hash`, `Nested Loop`, etc.).
14. PostgreSQL's optimizer chooses based on estimated cost.
15. Statistics and row-count estimates strongly influence the decision.
16. Join order can have a major impact on performance.
17. `EXPLAIN` lets you see which algorithm PostgreSQL actually chose.

---

# 38. One Sentence to Remember

> **A JOIN tells PostgreSQL what relationships you want; a join algorithm tells PostgreSQL how it should efficiently find those relationships.**

```text
              Your SQL
                 |
                 ↓
          "JOIN these tables"
                 |
                 ↓
             Planner
                 |
       ┌─────────┼─────────┐
       ↓         ↓         ↓
 Nested Loop  Hash Join  Merge Join
       |
       ↓
 actual execution
```

## Next: 22. Nested Loop Join

We'll go deeper into **Nested Loop Join**, including:

* outer vs inner table
* index nested loops
* cost intuition
* when it becomes dangerous
* why `loops=` appears in `EXPLAIN ANALYZE`
* practical examples
* how a small outer table can make a huge inner table surprisingly efficient.

