# 24. Merge Join

We have now seen:

* **Nested Loop Join** → repeatedly look for matching rows
* **Hash Join** → build a hash table, then probe it
* **Merge Join** → **sort both inputs and walk through them together**

The core idea is:

> **If both inputs are ordered by the join key, PostgreSQL can efficiently move through them like two sorted lists.**

---

# 1. The basic idea

Suppose we have:

```text
customers

id
--
1
2
3
4
```

and:

```text
orders

customer_id
-----------
1
1
2
4
4
```

We want:

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Both sides can be viewed as sorted by:

```text
customer_id
```

Then PostgreSQL can walk through them:

```text
customers:  1  2  3  4
            ↓
orders:     1  1  2  4  4
            ↓
            compare
```

Instead of repeatedly searching or building a hash table, it moves forward through the sorted inputs.

---

# 2. The two-pointer mental model

The easiest way to understand Merge Join is with **two pointers**.

Imagine:

```text
customers:  [1]  2   3   4
             ↑

orders:     [1]  1   2   4   4
             ↑
```

Compare:

```text
1 == 1
```

Match!

Then continue:

```text
customers:   1  [2]  3   4
                 ↑

orders:      1  [1]  2   4   4
                  ↑
```

The exact implementation has additional details, especially around duplicate values, but the fundamental idea is:

```text
Compare current values
       |
       +---- equal → produce matches
       |
       +---- left smaller → advance left
       |
       +---- right smaller → advance right
```

---

# 3. Visualizing the algorithm

Suppose:

```text
A = 1, 3, 5, 8
B = 1, 2, 5, 7, 8
```

Start:

```text
A: [1] 3  5  8
B: [1] 2  5  7  8
    ↑
```

Compare:

```text
1 = 1
```

Match.

Advance appropriately:

```text
A:  1 [3] 5  8
B:  1 [2] 5  7  8
```

Now:

```text
3 > 2
```

So advance B:

```text
A:  1  3 [5] 8
B:  1  2 [5] 7  8
```

Now:

```text
5 = 5
```

Match.

Continue:

```text
A:  1  3  5 [8]
B:  1  2  5 [7] 8
```

Since:

```text
8 > 7
```

advance B:

```text
A:  1  3  5 [8]
B:  1  2  5  7 [8]
```

Match:

```text
8 = 8
```

Done.

---

# 4. Why does sorting matter?

Merge Join relies on ordered data.

Consider:

```text
A:
8 1 5 3

B:
5 2 8 1
```

There is no useful sequential ordering.

To use the merge strategy, PostgreSQL needs:

```text
A:
1 3 5 8

B:
1 2 5 8
```

Now it can efficiently walk through both.

So conceptually:

```text
Unsorted input
     |
     v
   SORT
     |
     v
Sorted input
     |
     +----------+
                |
                v
           MERGE JOIN
                ^
                |
     +----------+
     |
   SORT
     |
     v
Unsorted input
```

---

# 5. Sorting can be expensive

This is one of the important trade-offs.

Suppose PostgreSQL has:

```text
10 million customers
10 million orders
```

and neither input is already ordered by the join key.

It might need to:

```text
customers
   ↓
sort

orders
   ↓
sort

sorted customers + sorted orders
   ↓
Merge Join
```

The Merge Join itself may be efficient, but the sorting work can be significant.

Therefore:

> Merge Join is especially attractive when the inputs are already ordered or can be obtained in the required order efficiently.

---

# 6. Where can the ordering come from?

There are several possibilities.

One important source is an **index**.

Suppose:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

An index can provide rows in index-key order.

Conceptually:

```text
orders
   |
   v
index on customer_id
   |
   v
1
1
2
4
4
```

This may help PostgreSQL obtain ordered input without performing a separate explicit sort.

However:

> Having an index does not guarantee PostgreSQL will choose a Merge Join.

The planner evaluates the total cost of available plans.

---

# 7. Merge Join with indexes

Imagine:

```text
customers
---------
id

orders
------
customer_id
```

and indexes:

```text
customers(id)
orders(customer_id)
```

Then PostgreSQL may potentially obtain both inputs in join-key order.

Conceptually:

```text
customers index
      |
      v
1  2  3  4
      \
       \
        → Merge Join
       /
      /
orders index
      |
      v
1  1  2  4  4
```

This is one situation where Merge Join can become attractive.

---

# 8. Merge Join with duplicate keys

This is very important because real-world joins often have duplicate keys.

Suppose:

```text
customers

id
--
1
2
```

and:

```text
orders

customer_id
-----------
1
1
1
2
```

For:

```text
customer_id = 1
```

there are three matching orders.

The Merge Join must produce:

```text
customer 1 + order 1
customer 1 + order 2
customer 1 + order 3
```

So Merge Join does **not** eliminate duplicate matches.

Just like Nested Loop and Hash Join:

> The join algorithm does not change the logical cardinality of the JOIN.

---

# 9. One-to-many example

Consider:

```text
customers

1 Alice
2 Bob
3 Charlie
```

and:

```text
orders

101 → customer 1
102 → customer 1
103 → customer 2
104 → customer 2
105 → customer 2
```

Sorted:

```text
customers
1
2
3

orders
1
1
2
2
2
```

Merge Join walks through them:

```text
1 ↔ 1
1 ↔ 1

2 ↔ 2
2 ↔ 2
2 ↔ 2

3 ↔ nothing
```

For an INNER JOIN:

```text
3
```

doesn't appear because there is no matching order.

---

# 10. Why is Merge Join called "Merge"?

Think about merging two sorted lists.

For example:

```text
A = 1 3 5 8
B = 1 2 5 7 8
```

You're essentially walking through both lists and determining how their values relate.

The process is similar to the merge phase of a merge-sort algorithm.

The important property is:

```text
Both inputs are ordered
        ↓
Compare current values
        ↓
Advance one or both pointers
        ↓
Continue
```

---

# 11. Equality joins

Merge Join is commonly associated with equality joins:

```sql
ON a.id = b.a_id
```

because both sides can be ordered by the same join key.

For example:

```text
A:
1  3  5  7  9

B:
2  3  5  8  9
```

The algorithm can efficiently walk through the values.

---

# 12. Merge Join can support more than simple equality

Merge-style strategies are particularly useful when the join condition has suitable ordering properties, including certain non-equality comparisons.

For example, merge joins can support certain inequality join conditions such as:

```sql
ON a.value < b.value
```

under appropriate circumstances.

But don't make the simplistic rule:

> "Merge Join = inequality joins."

That's incorrect.

The most important beginner mental model is:

> **Merge Join needs compatible ordered inputs and uses that ordering to efficiently find matches.**

---

# 13. PostgreSQL execution plan

You may eventually see something like:

```text
Merge Join
  Merge Cond: (c.id = o.customer_id)
  -> Index Scan using customers_pkey on customers c
  -> Index Scan using idx_orders_customer_id on orders o
```

Conceptually:

```text
                    Merge Join
                       |
              Merge Cond: c.id = o.customer_id
                   /          \
                  /            \
                 v              v
          customers index    orders index
             sorted             sorted
```

This is a very useful plan shape to recognize.

---

# 14. `Merge Cond`

When you see:

```text
Merge Cond: (c.id = o.customer_id)
```

think:

> "These two ordered streams are being merged using this condition."

For example:

```text
customers:
1 2 3 4 5
 ↓ ↓ ↓ ↓ ↓

orders:
1 1 2 4 4 5
 ↓ ↓ ↓ ↓ ↓ ↓

       MERGE
```

---

# 15. Merge Join vs Hash Join

Now we can compare the two.

| Characteristic            | Hash Join            | Merge Join                      |
| ------------------------- | -------------------- | ------------------------------- |
| Main idea                 | Build hash + probe   | Walk sorted inputs              |
| Requires ordering?        | No                   | Yes                             |
| Hash table?               | Yes                  | No                              |
| Sorting may be needed?    | No                   | Yes                             |
| Memory concern            | Hash table           | Sort operations                 |
| Excellent for             | Large equality joins | Ordered inputs / useful indexes |
| Index required?           | No                   | No                              |
| Can benefit from indexes? | Sometimes indirectly | Yes, for ordered access         |
| Equality joins            | Excellent fit        | Excellent fit                   |
| Duplicate matches         | Preserved            | Preserved                       |

The key difference:

```text
Hash Join
    ↓
Hash the data

Merge Join
    ↓
Order the data
```

---

# 16. Merge Join vs Nested Loop

Now compare all three:

```text
Nested Loop
------------
Take one row
   ↓
Find matching rows
   ↓
Repeat
```

```text
Hash Join
---------
Build hash table
   ↓
Probe with other input
```

```text
Merge Join
----------
Sort/order both inputs
   ↓
Walk through them together
```

A simplified comparison:

| Algorithm   | Mental model                            |
| ----------- | --------------------------------------- |
| Nested Loop | "Look again for every outer row"        |
| Hash Join   | "Build a lookup structure and probe it" |
| Merge Join  | "Walk through two sorted lists"         |

---

# 17. Why an index can make Merge Join attractive

Suppose you already have:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

and PostgreSQL needs rows ordered by:

```text
orders.customer_id
```

The index itself has ordering.

Conceptually:

```text
Index:
1
1
2
2
4
4
5
7
```

So instead of:

```text
table
 ↓
sort
 ↓
ordered rows
```

PostgreSQL may use:

```text
index
 ↓
ordered rows
```

That can make a Merge Join more attractive.

---

# 18. But indexes aren't free

An important misconception is:

> "If I create indexes on both join columns, PostgreSQL will use Merge Join."

Not necessarily.

The planner considers:

```text
Index scan cost
+
Merge Join cost
```

versus alternatives such as:

```text
Sequential scan
+
Hash Join
```

For large tables, a sequential scan followed by Hash Join can sometimes be cheaper than reading through indexes.

So:

```text
Index exists
    ≠
Index will be used
```

And:

```text
Index exists
    ≠
Merge Join will be chosen
```

---

# 19. A useful real-world scenario

Imagine a query that needs ordered output:

```sql
SELECT
    c.id,
    o.id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
ORDER BY c.id;
```

If both inputs can already be accessed in `id` / `customer_id` order, Merge Join may fit naturally with the required ordering.

Conceptually:

```text
Ordered customers
        +
Ordered orders
        |
        v
   Merge Join
        |
        v
Ordered result
```

This can potentially avoid unnecessary sorting later.

Again, the planner decides whether that is actually cheaper.

---

# 20. Merge Join and memory

Hash Join's major memory concern is:

```text
Hash table
```

Merge Join's major concern can instead be:

```text
Sorting
```

Suppose the input isn't already ordered:

```text
large table
    ↓
   Sort
    ↓
ordered table
```

Sorting itself requires resources.

If the data cannot be handled entirely in memory, PostgreSQL's sort operation may use temporary disk files.

So the broad contrast is:

```text
Hash Join
    → hash memory / possible batching

Merge Join
    → sorting resources / possible external sorting
```

---

# 21. Merge Join and `work_mem`

`work_mem` is relevant here too because sorting operations consume memory.

For example:

```sql
SHOW work_mem;
```

A larger available working memory can sometimes allow a sort to stay in memory rather than spilling to temporary storage.

But, just like with Hash Join:

> Don't blindly increase `work_mem`.

A query can contain multiple operations, and many concurrent queries can multiply memory consumption.

---

# 22. What if the data is already sorted?

This is where Merge Join becomes especially interesting.

Imagine PostgreSQL can obtain:

```text
customers:
1 2 3 4 5 6

orders:
1 1 2 3 5 6
```

without an expensive sort.

Then it can essentially do:

```text
customers ────────────┐
                      │
                      v
                  Merge Join
                      ^
                      │
orders ───────────────┘
```

The Merge Join can process the streams efficiently.

This is one of its strongest scenarios.

---

# 23. Merge Join and join cardinality

Remember the earlier concept:

> **The join algorithm does not determine how many logical matches exist.**

Suppose:

```text
A key = 10
B has 100 rows with key = 10
```

The result can contain:

```text
100 matching combinations
```

Merge Join still has to produce them.

Likewise:

```text
A has 50 rows with key = 10
B has 100 rows with key = 10
```

Potentially:

```text
50 × 100 = 5,000
```

result combinations for that key.

So:

```text
Merge Join
    ≠
No duplicates
```

and:

```text
Hash Join
    ≠
No duplicates
```

and:

```text
Nested Loop
    ≠
No duplicates
```

All three implement join semantics.

---

# 24. A complete conceptual flow

Here's the full Merge Join picture:

```text
             TABLE A
                |
                v
       Is it ordered by key?
          /             \
        YES              NO
         |                |
         |              SORT
         |                |
         +-------+--------+
                 |
                 v
          Ordered A
                 |
                 |
                 v
             MERGE JOIN
                 ^
                 |
                 |
          Ordered B
                 ^
                 |
         +-------+--------+
         |                |
       YES               NO
         |                |
         |              SORT
         |                |
         v                |
      TABLE B ------------+
```

Then:

```text
Ordered A + Ordered B
        ↓
compare keys
        ↓
advance through inputs
        ↓
produce matching rows
```

---

# 25. When should you think "Merge Join"?

When you see a query like:

```sql
FROM A
JOIN B
    ON A.key = B.key
```

and especially when:

* both inputs are large
* the join key has useful ordering
* indexes can provide the required order
* sorting is already needed for another reason
* the planner estimates Merge Join to be cheaper than alternatives

Think:

```text
"Can PostgreSQL get both sides ordered?"
```

If yes, Merge Join becomes a plausible strategy.

---

# 26. The three join algorithms together

At this point, you should have this mental model:

```text
                    JOIN
                     |
          +----------+----------+
          |          |          |
          v          v          v
     Nested Loop  Hash Join  Merge Join
          |          |          |
          v          v          v
     Repeated     Build +    Sorted streams
      lookup       Probe       + walk
```

### Nested Loop

```text
Outer row
   ↓
find matches
   ↓
repeat
```

### Hash Join

```text
Build
  ↓
Hash table
  ↓
Probe
```

### Merge Join

```text
Sort/order A
     +
Sort/order B
     ↓
Walk together
```

---

# 27. The most important distinction

Don't confuse:

```text
INNER JOIN
LEFT JOIN
RIGHT JOIN
FULL JOIN
```

with:

```text
Nested Loop
Hash Join
Merge Join
```

The first group describes:

> **What result should the query produce?**

The second group describes:

> **How PostgreSQL can physically execute the join.**

For example:

```text
LEFT JOIN
```

could potentially be executed as:

```text
Nested Loop Left Join
```

or:

```text
Hash Left Join
```

or:

```text
Merge Left Join
```

depending on the query and planner decisions.

---

# 28. Interview answer

If someone asks:

> What is a Merge Join?

A good answer is:

> A Merge Join is a PostgreSQL join algorithm that processes two inputs ordered by the join key and walks through them to find matching rows. If the inputs aren't already ordered, PostgreSQL may need to sort them. Merge Join is particularly useful when the required ordering is already available, for example through suitable indexes, or when sorting is otherwise worthwhile.

---

# 29. One-line mental models

You can now memorize these:

```text
Nested Loop
→ "For each row, find its matches."

Hash Join
→ "Build a hash table, then probe it."

Merge Join
→ "Walk through two ordered inputs together."
```

Or:

```text
Nested Loop → LOOK UP
Hash Join   → HASH
Merge Join  → SORT + WALK
```

---

# 30. Final mental picture

```text
              PostgreSQL JOIN
                    |
       +------------+------------+
       |            |            |
       v            v            v
   Nested Loop   Hash Join   Merge Join
       |            |            |
       |            |            |
   repeated      build hash    ordered A
   matching      + probe          +
                               ordered B
                                   |
                                   v
                              walk together
```

### The key questions for Merge Join

When you see a Merge Join in a plan, ask:

1. **Are both inputs ordered by the join key?**
2. **Where did that ordering come from?**

   * Index?
   * Sort?
3. **Was a large sort required?**
4. **How many rows are being processed?**
5. **Are there many duplicate join keys?**
6. **Is the ordering useful for subsequent operations such as `ORDER BY`?**
7. **How does its total cost compare with Hash Join or Nested Loop?**

> **Hash Join trades memory for fast equality matching.**
> **Merge Join trades ordering/sorting work for efficient sequential matching.**

---

## Next

**25. `EXPLAIN` / `EXPLAIN ANALYZE`**

This is where we put everything together and learn how to actually **see PostgreSQL's chosen join algorithm, estimated rows, actual rows, cost, execution time, loops, scans, and other plan information**.

