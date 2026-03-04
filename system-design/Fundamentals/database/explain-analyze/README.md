# The Story of Why Databases Got Smart: Query Planning & Optimization

Let's go back to the early days. Imagine you've just built a simple database system. You can store data in tables, and users can write SQL queries. Life is good. Then one day, a user runs this query:

```sql
SELECT name FROM employees WHERE department = 'Engineering';
```

Your naive database does the obvious thing — it opens the `employees` table, reads every single row from disk, checks if `department = 'Engineering'`, and returns the matches. For 100 rows, this is instant. For 10 million rows, your user is now making coffee and coming back to check if the query finished.

This is the **problem that query optimization was born to solve.**

---

## The Naive Executor's Problem

Here's the thing about SQL that makes it tricky: SQL is a *declarative* language. You're telling the database **what** you want, not **how** to get it. When you write:

```sql
SELECT name FROM employees WHERE department = 'Engineering';
```

You're not saying "read the table row by row." You're just saying "give me names of engineers." The database has to figure out the *how* on its own. And there are often dozens of valid ways to execute even a simple query — some of which are thousands of times faster than others.

Early database systems in the 1970s just executed queries in the most straightforward way possible: read everything, filter, done. This worked fine for small datasets. But as databases grew to millions and then billions of rows, this approach became completely untenable. A full table scan on a billion-row table might take 20 minutes. But if there's an index on the `department` column, the same query could return in milliseconds.

The key insight the researchers at IBM's System R project had in the late 1970s was this: **the database itself should be responsible for finding the best execution strategy.** The user should just describe what they want, and the database should figure out the smartest way to deliver it. This is where the **query optimizer** was born.

---

## What is a Query Planner/Optimizer?

Think of the query optimizer as a **travel planner for your SQL query.** You tell it your destination ("give me engineers' names"), and it figures out the best route — considering traffic (data size), road quality (indexes), and shortcuts (statistics about your data).

In PostgreSQL, when you fire a query, it goes through roughly these stages before a single row is read:

```
Your SQL string
      ↓
  [Parser]          → Checks syntax, turns SQL into a parse tree
      ↓
  [Analyzer]        → Resolves table/column names, checks they exist
      ↓
  [Rewriter]        → Applies rules (like view expansion)
      ↓
  [Planner/Optimizer] ← THIS is what we're studying
      ↓
  [Executor]        → Actually runs the chosen plan
```

The planner/optimizer is the brain. It takes your query, considers all the ways it could be executed, estimates the cost of each, and picks the cheapest one.

---

## Why Can't It Just Always Read the Whole Table?

Let's make this concrete. Say your `employees` table has 5 million rows, and only 500 of them are in Engineering. Reading 5 million rows to find 500 is like reading an entire encyclopedia to find one article. If you have an index (think of it like the encyclopedia's index section), you can jump straight to "Engineering" and find all references in milliseconds.

But here's the twist — the optimizer doesn't *always* use the index. Suppose you query for all employees who are **not** in Engineering, and 4.9 million rows qualify. Now reading the whole table sequentially is actually *faster* than jumping around the disk using an index (because sequential disk reads are much faster than random ones). The optimizer has to be smart enough to know this.

This is why optimization is needed: **the best strategy depends on the data, its distribution, and the query itself** — and these change constantly.

---

## A Quick Mental Exercise Before We Go Deeper

Before we move on, try to think about this: if you have a table of 1 million orders and you want to find all orders from a specific customer who placed exactly 3 orders, would you rather scan all 1 million rows or use something that lets you jump directly to that customer's orders? Now flip it — what if you want all orders from the year 2024, and 700,000 of those million orders are from 2024? Which approach makes more sense now?

That intuition you're building — "is it worth jumping around or just reading everything?" — is exactly the intuition the query optimizer has, but formalized with math.

---

This is the foundation. The optimizer exists because SQL is declarative, execution strategies vary wildly in performance, and the right strategy depends on your specific data. 

Take a moment to let that sink in. Does this make sense so far? Do you have any questions about why optimization is needed before we move on to the really interesting part — how the optimizer actually represents and transforms your query internally (logical vs physical plans)?

---

# Chapter 2: The Blueprint and the Build Plan — Logical vs Physical Plans

Let's pick up right where we left off. The optimizer has received your SQL query and needs to figure out the best way to run it. But here's an interesting design question the early database engineers faced: should the optimizer work directly on the raw SQL text? Should it immediately start thinking about disk reads and memory buffers?

They realized the answer was no — and for a really elegant reason. There are actually **two very different problems** that need to be solved separately:

1. **What** does the query logically mean — what transformations of data does it represent?
2. **How** should we physically execute those transformations on real hardware?

Mixing these two concerns together creates a mess. So they introduced a clean separation: the **logical plan** and the **physical plan**.

---

## The Logical Plan: What You're Asking For

Imagine you're an architect designing a house. Your first step isn't to call the plumber — it's to draw a blueprint. The blueprint says "there will be a kitchen here, a bathroom there, with these dimensions." It doesn't say *which* plumber, *which* brand of pipes, or *which* tools will be used. It just describes the structure.

The logical plan is that blueprint. It describes your query as a **tree of abstract relational operations** — things like "filter rows," "project columns," "join two tables" — without committing to any specific implementation.

Let's take a simple query:

```sql
SELECT name, salary
FROM employees
WHERE department = 'Engineering';
```

The logical plan for this would look like:

```
     Project (name, salary)
           |
     Filter (department = 'Engineering')
           |
        Scan (employees)
```

Read this tree from bottom to top — that's the direction data flows. First you scan the `employees` table (all of it, conceptually), then you filter to keep only Engineering rows, then you project (pick) just the `name` and `salary` columns.

Notice what's missing from this plan: there's no mention of indexes, no mention of whether we read the table sequentially or randomly, no mention of memory. This plan is **logically correct** for the query, but it says nothing about how to actually execute it on a real PostgreSQL instance. It's pure logic.

Now here's where it gets interesting — the optimizer can **rewrite and transform** the logical plan while keeping it logically equivalent. These transformations are called **rule-based optimizations**, and we'll get to them shortly. But the key insight is: it's much easier to reason about logical equivalences at this abstract level, before you've committed to any physical details.

---

## The Physical Plan: The Construction Crew

Once the logical plan is solid, the optimizer's job is to turn it into a **physical plan** — a concrete execution strategy that specifies exactly *how* each operation will be performed using the specific tools PostgreSQL has available.

Every abstract logical operation gets replaced by a specific physical implementation. "Scan" could become a *sequential scan* (read every page of the table in order) or an *index scan* (use a B-tree index to jump to specific rows). "Join" could become a *nested loop join*, a *hash join*, or a *merge join* — three very different algorithms we'll study in depth later.

The physical plan for our query might look like this (assuming there's an index on `department`):

```
     Project (name, salary)
           |
     Filter (department = 'Engineering')
           |
     Index Scan on employees
       (using idx_dept)
```

Or, without an index:

```
     Project (name, salary)
           |
     Filter (department = 'Engineering')
           |
   Sequential Scan on employees
```

Both physical plans correspond to the *same* logical plan. They're both correct answers. But they have very different performance characteristics. The physical plan is what PostgreSQL's executor actually runs.

You can actually **see** the physical plan PostgreSQL chose by using `EXPLAIN`:

```sql
EXPLAIN SELECT name, salary
FROM employees
WHERE department = 'Engineering';
```

PostgreSQL will print out its chosen physical plan — something we'll learn to read in detail later in this series.

---

## Why This Separation Matters: A Story of Rewriting

Here's a situation that really shows why the two-level approach is powerful. Consider this query:

```sql
SELECT name
FROM employees
WHERE department = 'Engineering' AND salary > 80000;
```

The naive logical plan is:

```
   Project (name)
        |
   Filter (department = 'Engineering' AND salary > 80000)
        |
   Scan (employees)
```

But what if there's an index on `salary` but not on `department`? The optimizer, working at the logical level, can *rewrite* the filter. It knows that `AND` filters can be applied in any order — filtering by salary first or department first gives the same result. So it can restructure the logical plan to make better use of the available index when it maps to the physical plan.

This is the power of the separation: **logical transformations preserve correctness, and physical selection optimizes performance.** The optimizer can freely play with the logical plan, trying different equivalent arrangements, before committing to a physical implementation.

---

## The Bridge Between Them: The Optimizer's Core Loop

Here's how PostgreSQL actually connects these two levels. Once it has the initial logical plan, it does roughly two things:

First, it applies **rule-based rewrites** — transformations that are *always* beneficial, like pushing filters down the tree so they happen earlier (filtering 4.9 million rows down to 500 before doing expensive operations is almost always a win). These rules are baked in and don't require statistics about your data.

Second, it applies **cost-based optimization** — it considers multiple candidate physical plans, estimates the cost of each one using statistics about your table, and picks the cheapest. This is where the real intelligence lives, and it's the next chapter of our story.

---

## A Mental Model to Carry Forward

Here's a way to keep these two concepts distinct in your mind. The **logical plan** answers: "If I were explaining this query to another person, what are the steps?" The **physical plan** answers: "If I were a PostgreSQL engineer implementing this on a specific machine with specific indexes and memory constraints, what exact algorithms would I use?"

One is about meaning. The other is about machinery.

---

Before we move on, let me ask you something to check your understanding. Suppose you have this query:

```sql
SELECT e.name, d.budget
FROM employees e
JOIN departments d ON e.dept_id = d.id
WHERE d.location = 'Bangalore';
```

In the logical plan, the filter `d.location = 'Bangalore'` could sit above the join or it could be pushed down below the join to filter the `departments` table *before* joining. Which arrangement do you think would be faster, and why? Think about how many rows each arrangement would need to join.

Once that clicks, we're perfectly set up to talk about **cost-based vs rule-based optimization, statistics, cardinality estimation, and selectivity** — which is where the optimizer goes from being clever to being almost magical.

Ready for Chapter 3?

---

# Chapter 3: How the Optimizer Thinks — Rules, Costs, and the Art of Estimation

Let's start with the answer to the question I left you with at the end of Chapter 2, because it leads us perfectly into this chapter.

The filter `d.location = 'Bangalore'` should absolutely be pushed *below* the join — applied to the `departments` table *before* joining. Why? Because imagine `departments` has 500 rows but only 3 are in Bangalore. If you filter first, you're joining `employees` with just 3 rows. If you filter after the join, you're doing all the work of joining `employees` with all 500 department rows, producing a massive intermediate result, and *then* throwing most of it away. You did expensive work for nothing.

This intuition — "do the most restrictive operations as early as possible" — is one of the oldest and most important ideas in query optimization. And it comes in two flavors: rules that are always true, and cost estimates that depend on your specific data.

---

## Rule-Based Optimization: The Laws That Always Hold

Back in the early days of databases, before anyone had figured out how to estimate costs, optimizers were entirely **rule-based**. The idea was simple: certain transformations of a query plan are *always* beneficial regardless of the data. You just encode these rules and apply them mechanically.

The most important rule is called **predicate pushdown**, and it's exactly what we just discussed. PostgreSQL always pushes filter conditions as far down the tree as possible, because filtering early means carrying fewer rows through the rest of the plan.

Another always-valid rule is **projection pushdown** — if you only need `name` and `salary` from a table with 30 columns, discard the other 28 columns as early as possible so you're not carrying unnecessary data through memory.

There's also **constant folding** — if you write `WHERE salary > 40000 + 10000`, PostgreSQL rewrites this to `WHERE salary > 50000` at planning time, so it doesn't recompute `40000 + 10000` for every row.

These rules are applied purely mechanically. PostgreSQL doesn't need to know anything about your data to apply them. They're just algebraically true, the same way `2x + 4x = 6x` is always true in math regardless of what `x` is.

But here's the problem the early rule-based systems ran into. Consider this query:

```sql
SELECT *
FROM employees e
JOIN departments d ON e.dept_id = d.id
JOIN projects p ON e.id = p.lead_id
WHERE e.department = 'Engineering';
```

You have three tables. You have to join them in some order — either `(employees JOIN departments) JOIN projects`, or `employees JOIN (departments JOIN projects)`, or `(employees JOIN projects) JOIN departments`. That's 3 possible orderings for 3 tables. For 5 tables it's 60 orderings. For 10 tables it's over 3.5 million.

No rule can tell you which ordering is best, because it *depends entirely on the data* — specifically on how many rows each table has and how selective each join condition is. This is the wall that rule-based optimization hits, and it's why **cost-based optimization** was invented, most famously in IBM's System R project around 1979.

---

## Cost-Based Optimization: Giving the Optimizer a Crystal Ball

The core idea of cost-based optimization is beautiful in its simplicity: **assign a numerical cost to every possible plan, and pick the one with the lowest cost.**

Cost in PostgreSQL is measured in abstract units that roughly correspond to disk page reads. A sequential scan of a large table has a high cost. An index lookup has a low cost. A hash join might have a moderate upfront cost (building the hash table) but a low per-row cost afterward.

But here's the catch — to estimate the cost of a plan, the optimizer needs to know roughly **how many rows each operation will produce**. If a filter reduces 1 million rows down to 10, that's very different from reducing 1 million rows down to 900,000. The rest of the plan changes dramatically. This is the problem of **cardinality estimation**.

---

## Cardinality Estimation: Guessing Intelligently

"Cardinality" just means the number of rows. The cardinality of a table is how many rows it has. The cardinality of a filter operation's output is how many rows survive the filter.

The optimizer can't run your query to find out how many rows each step produces — that would defeat the whole point of planning ahead. Instead, it has to *estimate* using **statistics** it has collected about your table.

In PostgreSQL, these statistics are gathered by the `ANALYZE` command (which runs automatically as part of `VACUUM ANALYZE`). PostgreSQL stores these statistics in a system catalog called `pg_statistic`, and you can view a friendlier version of it in `pg_stats`.

The statistics PostgreSQL collects include things like the total number of rows in the table, the number of distinct values in each column, a list of the most common values and how frequently they appear, and a histogram of value distribution for range queries.

Let's see how this plays out with a real example. Say your `employees` table has 1,000,000 rows, and PostgreSQL has gathered statistics showing that the `department` column has 20 distinct values, all roughly equally common.

Now you run:

```sql
SELECT * FROM employees WHERE department = 'Engineering';
```

PostgreSQL estimates the output cardinality like this: if there are 20 equally common departments, each one appears in about 1/20th of all rows. So the filter is expected to return roughly 1,000,000 / 20 = **50,000 rows**. This estimate is the cardinality of the filter's output.

If that estimate is reasonably accurate, PostgreSQL can now make a smart decision: 50,000 rows out of 1,000,000 is 5% of the table. That might still be enough rows that a sequential scan is competitive with an index scan, depending on how the data is physically laid out. The optimizer actually has to weigh the options with concrete cost numbers.

---

## Selectivity: How Picky is Your Filter?

Selectivity is the concept that ties cardinality estimation together. It's simply the **fraction of rows that survive a filter** — a number between 0 and 1.

A filter with selectivity of 0.001 means only 0.1% of rows pass through. That's a very *selective* filter — it's picky, it throws away almost everything. An index is extremely valuable here because you're jumping straight to a tiny slice of the table.

A filter with selectivity of 0.9 means 90% of rows pass through. That's a very *unselective* filter — it barely reduces the data. Using an index here would likely be *slower* than a sequential scan, because you'd be making nearly as many random disk jumps as there are rows in the table, whereas a sequential scan reads the data in smooth, efficient sweeps.

Let's make this concrete with three scenarios using the same table of 1,000,000 employees:

**Scenario A:** `WHERE employee_id = 12345` — employee IDs are unique, so selectivity ≈ 0.000001. Only 1 row survives. Index scan is the clear winner.

**Scenario B:** `WHERE department = 'Engineering'` — 20 departments, selectivity ≈ 0.05. About 50,000 rows survive. PostgreSQL might use an index, might not — it depends on table size and physical layout.

**Scenario C:** `WHERE hire_date > '2000-01-01'` — if 95% of your employees were hired after 2000, selectivity ≈ 0.95. Sequential scan almost certainly wins here.

You can actually see PostgreSQL's cardinality estimates with `EXPLAIN`:

```sql
EXPLAIN SELECT * FROM employees WHERE department = 'Engineering';
```

```
Seq Scan on employees  (cost=0.00..24831.00 rows=50000 width=64)
  Filter: (department = 'Engineering')
```

See that `rows=50000`? That's PostgreSQL's cardinality estimate. And the `cost=0.00..24831.00` is the estimated cost range — the first number is the startup cost (cost before first row is returned) and the second is the total cost. These numbers are what the optimizer uses to compare plans against each other.

---

## When Estimates Go Wrong: The Optimizer's Achilles Heel

Here's something the textbooks sometimes gloss over — cardinality estimation is fundamentally *hard*, and when it goes wrong, the optimizer can make catastrophically bad decisions.

The classic failure case is **correlated columns**. Suppose your `employees` table has both a `department` column and a `location` column. Most engineers work in Bangalore, and most Bangalore employees are engineers. The two columns are correlated. But PostgreSQL's default statistics treat each column independently. So if you write:

```sql
SELECT * FROM employees
WHERE department = 'Engineering' AND location = 'Bangalore';
```

PostgreSQL multiplies the two selectivities: if Engineering is 5% and Bangalore is 40%, it estimates 5% × 40% = 2% of rows survive. But in reality, because of the correlation, maybe 4.9% of rows survive. This might not sound like a big difference, but when this estimate feeds into a join or sort decision, it can cascade into a completely wrong physical plan.

This is why PostgreSQL introduced **extended statistics** in version 10 — you can explicitly tell it that two columns are correlated, and it will gather joint statistics on them. But that's a more advanced topic for later.

---

## The Big Picture: How These Pieces Fit Together

Let's zoom out and see the full pipeline with fresh eyes. When you run a query, PostgreSQL's optimizer:

First applies all its rule-based rewrites — push predicates down, fold constants, simplify expressions. These are free wins that require no data knowledge.

Then it enumerates candidate physical plans — different join orderings, different index choices, different algorithm selections. For small numbers of tables this is exhaustive; for large queries it uses heuristics to prune the search space.

For each candidate plan, it walks the plan tree bottom-up, estimating the cardinality at each node using the collected statistics, and computing a cost based on those estimates.

Finally, it picks the plan with the lowest estimated total cost.

The whole process happens in **milliseconds** for most queries — the planning overhead is negligible compared to execution time. But for very complex queries with many joins, planning itself can take noticeable time, which is part of why PostgreSQL has a `geqo_threshold` setting that switches to a genetic algorithm for queries with many tables (but that's deep PostgreSQL internals territory for much later).

---

## A Thought Experiment to Solidify This

Imagine PostgreSQL has never run `ANALYZE` on your table, so it has no statistics. What would it do? It falls back to hardcoded defaults — it assumes a table has about 1000 rows, that equality filters have selectivity 0.5%, and range filters have selectivity 33.3%. These defaults are often wildly wrong for real data. This is why running `ANALYZE` regularly is one of the most fundamental database maintenance tasks — stale or missing statistics is one of the most common causes of slow queries in production PostgreSQL systems.

You can actually see the damage bad statistics cause. If PostgreSQL thinks your 10-million-row table has 1000 rows, it will confidently choose a nested loop join that performs beautifully on 1000 rows and catastrophically on 10 million.

---

We've now covered the full thinking apparatus of the optimizer — rules, costs, statistics, cardinality, and selectivity. In the next chapter, we're going to turn this knowledge toward something deeply practical: **how join order affects performance, and why the optimizer's job gets exponentially harder as you add more tables.** We'll also introduce the concept of intermediate result sizes and show, with real numbers, why getting join order wrong can make a query 1000x slower.

Does everything here make sense? The key ideas to carry forward are: rules are free wins, cost-based decisions need statistics, cardinality is the estimated row count at each step, and selectivity tells you how much a filter reduces your data. Solid on all of these?

---

# Chapter 4: Join Order — The Decision That Can Make or Break Your Query

Let's start with a story. It's 2003, and a developer at a growing e-commerce company writes what looks like a perfectly reasonable query to generate a sales report:

```sql
SELECT c.name, p.title, o.amount
FROM customers c
JOIN orders o ON c.id = o.customer_id
JOIN products p ON o.product_id = p.id
WHERE p.category = 'Electronics';
```

Three tables, a couple of joins, one filter. The query runs for 45 minutes and times out. The developer is baffled — the tables aren't even that large. What went wrong?

The answer was join order. The database was joining `customers` (2 million rows) with `orders` (15 million rows) first, producing an intermediate result of tens of millions of rows, and *then* filtering by electronics category. If it had simply filtered `products` to electronics first (maybe 500 rows) and then joined outward, the whole query would have finished in seconds.

This is the central drama of join optimization, and it's what this chapter is all about.

---

## What Does "Join Order" Actually Mean?

When you write a query with multiple joins, SQL doesn't specify the order in which joins should happen — remember, SQL is declarative. You're just saying "connect these tables." The optimizer is free to execute the joins in any order it chooses, as long as the final result is the same.

For two tables, there's only one join to do, so order doesn't matter much. But the moment you have three tables — let's call them A, B, and C — you suddenly have choices:

```
Option 1: (A JOIN B) JOIN C   -- join A and B first, then join that result with C
Option 2: (A JOIN C) JOIN B   -- join A and C first, then bring in B
Option 3: (B JOIN C) JOIN A   -- join B and C first, then bring in A
```

And for each of these, you could also flip which table is on which "side" of the join, which matters for algorithms like nested loop join (which we'll cover in the next chapter). So the search space grows very fast — with 4 tables you have 24 orderings, with 5 you have 120, with 10 you have over 3.5 million. This is the combinatorial explosion that made join ordering one of the hardest problems in database research.

---

## The Intermediate Result: The Hidden Cost

The key to understanding why join order matters so much is the concept of the **intermediate result** — the temporary table that gets produced when you join two things together, before the next operation happens.

Let's build a concrete example with real numbers. Suppose you have three tables:

```
customers  →  2,000,000 rows
orders     →  10,000,000 rows  (each customer has ~5 orders on average)
products   →  50,000 rows      (only 500 are in the 'Electronics' category)
```

And your query has the filter `WHERE p.category = 'Electronics'`, which has a selectivity of 500/50,000 = 0.01 (1% of products).

Now let's trace through two different join orderings and watch the intermediate result sizes.

**Plan A: Join customers → orders → products (then filter)**

```
Step 1: Scan customers             →  2,000,000 rows
Step 2: Join with orders           →  ~10,000,000 rows  (massive intermediate!)
Step 3: Join with products         →  ~10,000,000 rows  (still huge)
Step 4: Filter p.category = 'Elec →  ~100,000 rows  (now we finally filter)
```

The database had to build and carry a 10-million-row intermediate result through most of the work. That intermediate result needs memory (or spills to disk if memory runs out), and every subsequent operation has to process all of it.

**Plan B: Filter products first → join orders → join customers**

```
Step 1: Scan products + filter     →  500 rows  (electronics only!)
Step 2: Join with orders           →  ~some orders for those 500 products
Step 3: Join with customers        →  final result
```

The moment we applied the filter to `products` first, we reduced our working set from 50,000 rows to 500. Everything downstream operates on a tiny fraction of the data. The intermediate results stay small throughout.

This is the fundamental principle: **the best join order is the one that keeps intermediate results as small as possible for as long as possible.** And because filters reduce row counts, you generally want to apply selective filters early and join the tables that have already been filtered.

---

## How the Optimizer Finds the Best Order: Dynamic Programming

Now you might wonder — if there are millions of possible orderings for complex queries, how does PostgreSQL evaluate them without itself taking forever?

The answer is a technique called **dynamic programming**, and it's the core of the System R approach that PostgreSQL inherits. The insight is that you don't need to evaluate every complete plan from scratch. Instead, you build up optimal sub-plans and combine them.

Here's how it works conceptually. PostgreSQL starts by finding the best way to access each individual table — sequential scan or index scan, and at what cost. These are the building blocks. Then it considers all pairs of tables and finds the best way to join each pair, using the single-table costs it already computed. Then it considers all triples, using the best pair costs. And so on, building up to the full query.

At each level, it keeps only the best plan for each sub-problem and discards the rest. This dramatically reduces the search space. For most real queries (say, under 8-10 tables), PostgreSQL can explore the space exhaustively using this approach. For queries with more tables, it switches to a genetic algorithm that sacrifices optimality for speed — but that's an edge case most of us rarely hit.

---

## Let's Watch PostgreSQL Actually Do This

Let's set up a small scenario you can try in PostgreSQL to see this in action. Imagine you have these tables:

```sql
-- A small departments table
CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    name TEXT,
    location TEXT
);

-- A larger employees table
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name TEXT,
    dept_id INT REFERENCES departments(id),
    salary INT
);

-- An even larger activity log
CREATE TABLE activity_log (
    id SERIAL PRIMARY KEY,
    employee_id INT REFERENCES employees(id),
    action TEXT,
    logged_at TIMESTAMP
);
```

Now suppose `departments` has 50 rows, `employees` has 100,000 rows, and `activity_log` has 5,000,000 rows. And you run:

```sql
EXPLAIN SELECT e.name, d.name, a.action
FROM departments d
JOIN employees e ON e.dept_id = d.id
JOIN activity_log a ON a.employee_id = e.id
WHERE d.location = 'Bangalore';
```

Even though you wrote the joins in the order `departments → employees → activity_log`, PostgreSQL is completely free to reorder them. After running `ANALYZE` on all three tables, it will likely recognize that filtering `departments` by `location = 'Bangalore'` might leave only a handful of rows, and it will structure the join to start from that small filtered result and build outward.

The `EXPLAIN` output will show you the actual order it chose, and crucially, it will show you the `rows=` estimate at each step so you can see how intermediate result sizes were estimated.

---

## The Danger Zone: When the Optimizer Gets It Wrong

Here's something important to understand — the optimizer's join ordering decision is only as good as its cardinality estimates. If the estimates are wrong, it can choose a terrible join order with complete confidence.

The classic production disaster story goes like this: a developer adds a new filter to an existing query, say `WHERE status = 'pending' AND priority = 'critical'`. These two columns are correlated — critical items are almost always pending. But PostgreSQL estimates the combined selectivity by multiplying independent selectivities: maybe it thinks 10% of rows are pending and 5% are critical, so it estimates 0.5% survive the combined filter. The actual number is 8% (because of the correlation). PostgreSQL, thinking this filter is very selective, decides to use it as the driving table in a nested loop join that would be great for 0.5% but is terrible for 8%. The query that used to run in 2 seconds now takes 4 minutes.

This is why understanding join ordering isn't just academic. When you encounter a slow query in production, one of the first things you should ask is: "Is the optimizer choosing a bad join order because of bad estimates?" You can check this by running `EXPLAIN ANALYZE` (which shows both estimated *and* actual row counts) and looking for nodes where the estimate and reality diverge dramatically.

---

## A Thought Experiment to Test Your Intuition

Before we move on, consider this scenario. You're joining two tables: `users` (50 million rows) and `premium_users` (10,000 rows), on `users.id = premium_users.user_id`. There's no filter — you want all premium users with their full user details.

Which table should drive the join — should you scan `premium_users` and look up each match in `users`, or should you scan `users` and look up each match in `premium_users`? Think about the size of the intermediate results and what happens in each direction.

The answer leads us naturally into our next chapter, because it depends entirely on *which join algorithm* you're using — and different algorithms have very different preferences for which table is "outer" and which is "inner." That's exactly what we're covering next: **the three core join algorithms — nested loop join, hash join, and merge join — and when PostgreSQL picks each one.**

Does the join ordering story make sense? The key ideas are: intermediate result size is what determines plan cost, small intermediate results = fast queries, and the optimizer uses dynamic programming to find the best ordering based on cardinality estimates. Any questions before we dive into the algorithms?

---

# Chapter 5: The Three Join Algorithms — How Databases Actually Connect Tables

Let's answer the thought experiment from last chapter first, because it's the perfect doorway into this topic.

You're joining `users` (50 million rows) with `premium_users` (10,000 rows). If you scan all 50 million users and try to find each one in `premium_users`, you're doing 50 million lookups. But if you scan the 10,000 premium users and look up each one in the `users` table (using an index), you're doing only 10,000 lookups. The second approach wins by a factor of 5,000. The direction matters enormously — and it depends on the algorithm.

This is the story of how database engineers, faced with the problem of connecting two tables efficiently, invented three fundamentally different strategies, each with its own strengths and weaknesses. Understanding these algorithms is one of the most practically useful things you can learn about databases, because they show up directly in every `EXPLAIN` output you'll ever read.

---

## The Problem They Were All Solving

Let's set the stage. You have two tables — in join algorithm literature, they're called the **outer table** (or left table) and the **inner table** (or right table). You want to find all pairs of rows where some condition is true, typically an equality like `e.dept_id = d.id`.

The brute-force approach is obvious: for every row in the outer table, check every row in the inner table. This is correct, but it's quadratic — if both tables have N rows, you do N² comparisons. For two tables with 1,000 rows each, that's 1,000,000 comparisons. For two tables with 1,000,000 rows each, that's 1,000,000,000,000 comparisons. Nobody has time for that.

The three join algorithms are three different answers to the question: "How do we do this without the quadratic blowup?"

---

## Algorithm 1: Nested Loop Join — The Simple but Powerful One

The nested loop join is the oldest and most intuitive algorithm. It's essentially the brute-force approach, but it becomes tremendously efficient when combined with an index.

Here's the core idea in pseudocode:

```
for each row R in the outer table:
    for each row S in the inner table where S matches R:
        emit (R, S)
```

Without any index, this is exactly the quadratic brute-force. But the magic happens when there's an **index on the inner table's join column**. Instead of scanning all of the inner table for each outer row, you do a lightning-fast index lookup. The inner loop becomes essentially O(1) instead of O(N).

Let's trace through a real example step by step. Take our classic query:

```sql
SELECT e.name, d.name
FROM departments d        -- outer table (50 rows)
JOIN employees e ON e.dept_id = d.id   -- inner table (100,000 rows)
WHERE d.location = 'Bangalore';
```

Suppose PostgreSQL filters `departments` down to 5 Bangalore departments first, and there's a B-tree index on `employees.dept_id`. Here's what happens:

```
Nested Loop Join
      |
      ├── Outer: Seq Scan on departments (filter: location='Bangalore')
      │         → produces 5 rows
      │
      └── Inner: Index Scan on employees (using idx_dept_id)
                → for each of the 5 dept rows, look up matching employees
```

Step by step:
- Row 1 from departments: `{id: 3, name: 'Engineering', location: 'Bangalore'}` — PostgreSQL immediately uses the index to find all employees with `dept_id = 3`. Say it finds 200. Emit 200 result rows.
- Row 2 from departments: `{id: 7, name: 'Design', location: 'Bangalore'}` — index lookup for `dept_id = 7`, finds 80 employees. Emit 80 result rows.
- ... and so on for all 5 departments.

Total comparisons: not 50 × 100,000 = 5,000,000. Instead, just 5 index lookups, each finding a small number of employees. This is incredibly efficient.

**When does PostgreSQL love nested loop join?** When the outer table is small (or has been filtered down to small), and there's a good index on the inner table's join column. It's also the only algorithm that can start returning rows immediately — it doesn't need to read both entire tables before producing output. This property is called **pipelining**, and we'll come back to it.

**When does it fall apart?** When the outer table is large and there's no index on the inner table. Now you're back to quadratic behavior. Imagine scanning 1,000,000 outer rows and doing a sequential scan of 1,000,000 inner rows for each one. That's a trillion operations — a disaster.

---

## Algorithm 2: Hash Join — The Workhorse of Analytical Queries

Hash join was developed to solve exactly the case where nested loop fails: two large tables with no useful indexes. It was a revelation when it was introduced, because it brought join complexity down from O(N²) to O(N) — linear rather than quadratic.

The idea is elegant. Instead of comparing every pair of rows, you build a lookup structure from one table and probe it with the other. Here's the core pseudocode:

```
-- Phase 1: Build
for each row R in the smaller (inner) table:
    compute hash(R.join_column)
    insert R into hash_table[hash(R.join_column)]

-- Phase 2: Probe
for each row S in the larger (outer) table:
    compute hash(S.join_column)
    look up hash_table[hash(S.join_column)]
    for each match found, emit (S, match)
```

Let's trace through a concrete example. You're joining `orders` (10,000,000 rows) with `products` (50,000 rows) on `o.product_id = p.id`:

```
Hash Join
      |
      ├── Build side: Seq Scan on products (50,000 rows)
      │         → PostgreSQL reads all 50,000 product rows
      │         → builds an in-memory hash table keyed on p.id
      │         → hash table has ~50,000 buckets
      │
      └── Probe side: Seq Scan on orders (10,000,000 rows)
                → for each order row, compute hash(o.product_id)
                → look up that bucket in the hash table
                → if found, emit the joined row
```

Step by step for a few rows:
- Build phase: PostgreSQL scans `products` entirely and builds the hash table. `product_id=1` goes to bucket 47, `product_id=2` goes to bucket 1203, etc. This takes time proportional to the size of `products`.
- Probe phase: First order row has `product_id=47`. Hash it, look it up in the table — O(1) lookup, found, emit the joined row. Next order row has `product_id=1`. Hash it, look it up — found, emit. And so on for all 10 million orders.

Total work: O(50,000) to build + O(10,000,000) to probe = O(10,050,000). Linear in the total data size. Compare this to nested loop without an index: O(10,000,000 × 50,000) = O(500,000,000,000). The hash join is about 50,000 times less work.

**The critical question:** which table should you build the hash table from? Always the **smaller** one. Building a hash table from the larger table wastes memory, and if the hash table doesn't fit in memory (controlled by PostgreSQL's `work_mem` setting), it has to spill to disk in chunks — called a **batch hash join** — which is still correct but noticeably slower. PostgreSQL automatically chooses the smaller table as the build side.

**When does PostgreSQL love hash join?** When both tables are large, there are no useful indexes, and the join is an equality condition. It's the go-to algorithm for full table joins in analytical workloads.

**When does it fall apart?** First, it requires an equality condition — you can't build a hash table for range joins like `a.date BETWEEN b.start AND b.end`. Second, if `work_mem` is too small and the hash table spills to disk, performance degrades. Third, unlike nested loop, hash join can't return any rows until the entire build phase is complete — it has high **startup cost** but then processes the probe side very quickly.

---

## Algorithm 3: Merge Join — The Elegant One for Sorted Data

Merge join is the most elegant of the three algorithms, and it has a beautiful intuition behind it. If both tables are **already sorted** on the join column, you can join them in a single linear pass — like merging two sorted lists.

Imagine you're matching students to their grades. You have a sorted list of student IDs: `[1, 2, 3, 4, 5...]` and a sorted list of grade records also by student ID: `[1, 1, 2, 3, 3, 3, 4...]`. You can walk both lists simultaneously with two pointers, matching them as you go. You never need to go backward, and you never need to compare every combination. One linear pass, and you're done.

Here's the algorithm:

```
sort outer table on join column (if not already sorted)
sort inner table on join column (if not already sorted)

pointer_outer = first row of outer
pointer_inner = first row of inner

while both pointers are valid:
    if outer.join_col == inner.join_col:
        emit all matching combinations, advance pointers
    elif outer.join_col < inner.join_col:
        advance outer pointer
    else:
        advance inner pointer
```

Let's trace through a small example. Joining `departments` and `employees` on `dept_id`:

```
Departments (sorted by id):  [1, 2, 3, 4, 5]
Employees (sorted by dept_id): [1, 1, 2, 2, 2, 3, 5, 5]
```

```
Step 1: dept=1, emp=1  → match! emit rows. advance emp.
Step 2: dept=1, emp=1  → match! emit rows. advance emp.
Step 3: dept=1, emp=2  → no match (1 < 2). advance dept.
Step 4: dept=2, emp=2  → match! emit rows. advance emp.
... and so on
```

Single pass, linear time, no hash table needed.

**The catch:** both inputs need to be sorted. Sorting is O(N log N), which is more expensive than linear. So merge join is only a win when the data is *already sorted* — either because the join column has an index (which stores data in sorted order), or because a previous step in the query plan already sorted the data, or because the table was loaded in sorted order.

In PostgreSQL's `EXPLAIN` output, you'll sometimes see an explicit **Sort** node above a table scan feeding into a merge join — PostgreSQL decided that the cost of sorting plus the merge join was still cheaper than a hash join, perhaps because `work_mem` is tight and hash join would have to spill.

**When does PostgreSQL love merge join?** When both inputs are already sorted (especially when using index scans that produce sorted output), when the dataset is very large and memory is limited (merge join has predictable memory usage), and when the join produces a large fraction of the combined rows.

---

## Side by Side: The Decision Matrix

Now let's put all three algorithms together so you can develop the intuition for when each one wins:

```
┌─────────────────┬──────────────────┬───────────────────┬─────────────────┐
│                 │  Nested Loop     │   Hash Join       │   Merge Join    │
├─────────────────┼──────────────────┼───────────────────┼─────────────────┤
│ Best when       │ Small outer +    │ Large tables,     │ Inputs already  │
│                 │ index on inner   │ no index          │ sorted          │
├─────────────────┼──────────────────┼───────────────────┼─────────────────┤
│ Join types      │ Any              │ Equality only     │ Equality only   │
├─────────────────┼──────────────────┼───────────────────┼─────────────────┤
│ Memory usage    │ Very low         │ build_side size   │ Low (streaming) │
├─────────────────┼──────────────────┼───────────────────┼─────────────────┤
│ First row out   │ Immediately      │ After build phase │ After sort      │
├─────────────────┼──────────────────┼───────────────────┼─────────────────┤
│ Complexity      │ O(N) with index  │ O(N+M)            │ O(N log N)      │
│                 │ O(N×M) without   │                   │ (if unsorted)   │
└─────────────────┴──────────────────┴───────────────────┴─────────────────┘
```

---

## Seeing All Three in PostgreSQL

You can actually force PostgreSQL to use specific join algorithms by temporarily disabling the others, which is a great way to build intuition. These are session-level settings that only affect your current connection:

```sql
-- Disable hash join and merge join to force nested loop
SET enable_hashjoin = off;
SET enable_mergejoin = off;
EXPLAIN ANALYZE SELECT ...;

-- Re-enable everything
SET enable_hashjoin = on;
SET enable_mergejoin = on;

-- Or disable just nested loop to see what hash join costs
SET enable_nestloop = off;
EXPLAIN ANALYZE SELECT ...;
```

This is extremely useful for debugging — if you force a different algorithm and the query gets faster, that tells you the optimizer was making a wrong choice, likely because of bad cardinality estimates.

---

## A Real EXPLAIN Output Showing All Three

Here's what each algorithm looks like in an actual PostgreSQL `EXPLAIN` output, so you can recognize them in the wild:

```sql
-- Nested Loop Join
Nested Loop  (cost=0.43..1200.50 rows=500 width=72)
  ->  Index Scan on departments d  (cost=0.28..8.30 rows=5 width=36)
        Index Cond: (location = 'Bangalore')
  ->  Index Scan on employees e  (cost=0.42..150.00 rows=100 width=36)
        Index Cond: (dept_id = d.id)   -- note: references d.id from outer loop

-- Hash Join
Hash Join  (cost=1500.00..45000.00 rows=100000 width=72)
  Hash Cond: (o.product_id = p.id)
  ->  Seq Scan on orders o  (cost=0.00..25000.00 rows=1000000 width=36)
  ->  Hash  (cost=800.00..800.00 rows=50000 width=36)   -- this is the build phase
        ->  Seq Scan on products p  (cost=0.00..800.00 rows=50000 width=36)

-- Merge Join
Merge Join  (cost=25000.00..35000.00 rows=100000 width=72)
  Merge Cond: (e.dept_id = d.id)
  ->  Sort  (cost=12000.00..12500.00 rows=100000 width=36)
        Sort Key: e.dept_id
        ->  Seq Scan on employees e
  ->  Sort  (cost=120.00..132.50 rows=50 width=36)
        Sort Key: d.id
        ->  Seq Scan on departments d
```

Notice how the nested loop's inner node (`Index Scan on employees`) references `d.id` from the outer loop — it's literally being re-executed once per outer row. The hash join shows a dedicated `Hash` node representing the build phase. The merge join shows explicit `Sort` nodes where the data wasn't already ordered.

---

## One Concept to Preview: Pipelining vs Materialization

I mentioned earlier that nested loop can return its first row immediately, while hash join has to finish the entire build phase first. This difference — whether an operator can pass rows to the next operator one at a time as they're produced, versus whether it needs to accumulate all rows first — is called **pipelining vs materialization**, and it's an important concept that affects how PostgreSQL manages memory and latency.

We'll cover this in detail in the next chapter alongside other core execution operators like scan, filter, sort, and aggregate. Understanding pipelining is what will make execution plans truly click, because it explains *why* certain plan shapes are more memory-efficient than others.

---

Before we move on — the join ordering intuition from Chapter 4 and the algorithm intuition from this chapter are closely linked. The optimizer not only decides *which tables to join first* but also *which algorithm to use for each join*, and these decisions interact. A join ordering that enables a nested loop (because one side is small) might be much better than a join ordering that forces a hash join on two huge tables.

Take a moment to think about this: given what you now know, why might PostgreSQL sometimes choose a *more expensive* join ordering if it enables a nested loop join that can use an index, over a *theoretically cheaper* ordering that forces a hash join? The answer has to do with that `rows=` estimate and the real cost of random I/O versus sequential I/O — something we'll revisit when we talk about index scans in the next chapter.

Ready to go deeper into execution plans and operators?

---

# Chapter 6: Inside the Execution Engine — How PostgreSQL Actually Runs a Plan

So far we've been living in the optimizer's world — the world of planning, estimating, and deciding. But once the optimizer hands off its chosen physical plan, a completely different part of PostgreSQL takes over: the **executor**. This is the engine that actually touches your data, moves rows through memory, and produces the results you see on your screen.

The story of how the executor was designed is actually a beautiful piece of computer science, and understanding it will make every `EXPLAIN` output you ever read feel like a transparent window into what your database is doing.

---

## The Volcano Model: PostgreSQL's Execution Philosophy

In 1994, a researcher named Goetz Graefe published a paper describing an execution model called the **Volcano model** (sometimes called the iterator model). PostgreSQL's executor is built on this exact model, and once you understand it, the tree structure of execution plans will make perfect intuitive sense.

The central idea is this: every operator in a query plan — every scan, every filter, every join, every sort — implements the same simple interface with essentially one method: **"give me your next row."**

When the executor wants results, it calls `GetNext()` on the top of the plan tree. That operator calls `GetNext()` on its child, which calls `GetNext()` on *its* child, and so on down the tree until you hit a node that actually reads from disk. A row bubbles back up through the chain, gets processed at each level, and eventually arrives at the top as a result row.

Here's a simple plan tree to make this concrete:

```
         Project (name, salary)       ← you call GetNext() here
               |
         Filter (dept = 'Eng')        ← calls GetNext() on Scan
               |
         Seq Scan (employees)         ← reads from disk
```

The execution dance works like this: you ask Project for a row. Project asks Filter for a row. Filter asks SeqScan for a row. SeqScan reads a row from disk and hands it up. Filter checks: does this row have `dept = 'Engineering'`? If yes, it hands it up to Project. Project picks just `name` and `salary` and hands it to you. Then you ask for another row, and the whole pull-chain repeats.

This is a **pull-based** execution model. The top of the tree pulls rows up from the bottom, one at a time, on demand. It's elegant because each operator doesn't need to know what kind of operator is above or below it — they all speak the same `GetNext()` language.

---

## Pipelining: The Art of Not Storing What You Don't Have To

The Volcano model naturally gives rise to **pipelining**, and this is one of the most important concepts in query execution. A pipelined operator processes one row at a time and immediately passes it upstream — it never accumulates a collection of rows in memory. It's like an assembly line where each station hands off work to the next station as soon as it's done, rather than waiting for a whole batch to finish.

Let's look at the three-operator chain above again. The `Filter` operator is fully pipelined. The moment `SeqScan` hands it a row, it checks the condition and either passes it up or discards it. At any given moment, `Filter` is holding at most one row in memory. The whole chain uses a tiny, constant amount of working memory regardless of how many rows are in the table. That's a beautiful property — it means you can process a 10-billion-row table with the same memory footprint as a 100-row table, as long as your operators are pipelined.

But here's the catch that the database engineers ran into: **not all operators can be pipelined.** Some operators are fundamentally "blocking" — they absolutely must see all their input rows before they can produce even a single output row. These operators **materialize** their input, meaning they store the full intermediate result in memory (or on disk if it doesn't fit). Two of the most important blocking operators are `Sort` and `HashAggregate`.

Think about why `Sort` must block. If you're sorting a million employees by salary and the cheapest-paid employee happens to be the very last row in the table, you can't output the first sorted row until you've read every single input row. There's no way around this — sorting is inherently blocking.

The same logic applies to `HashAggregate`, which computes things like `COUNT(*)`, `SUM()`, or `AVG()` grouped by some column. Before it can tell you "Engineering department has 1,247 employees", it needs to have counted all employees in Engineering — which means it needs to have seen all the rows.

Here's how pipelining vs materialization plays out in a plan tree:

```
         Project (name, dept, total_sal)     ← pipelined
               |
         HashAggregate                       ← BLOCKING: must see all rows
         (GROUP BY dept, SUM(salary))        before outputting anything
               |
         Filter (hire_year > 2018)           ← pipelined
               |
         Seq Scan (employees)               ← pipelined (streams from disk)
```

In this plan, the SeqScan streams rows up through Filter (which pipelines them), and they pile up inside HashAggregate until every qualifying employee has been processed. Only then does HashAggregate start emitting grouped result rows upward. The blocking nature of HashAggregate creates a **pipeline breaker** — a wall in your plan tree where rows accumulate before the work above it can even begin.

This has a very practical implication you can observe: if you run a query with a `GROUP BY` or `ORDER BY` on a large table and you're watching the progress, it will appear to "hang" for a while (the blocking phase) and then suddenly spit out all the results at once. With a simple filtered query that's fully pipelined, you'd see the first result rows almost immediately.

---

## The Core Operators, One by One

Now let's walk through each fundamental operator you'll see in PostgreSQL's execution plans, understanding both what it does and when it blocks.

### Sequential Scan (Seq Scan)

This is the simplest operator — it reads every page of a table from disk in order, from first to last, and emits rows one by one. It's pipelined (each row is emitted as it's read), it uses minimal memory, and it's very fast for large fractions of a table because modern disks and operating systems are highly optimized for sequential I/O.

In an `EXPLAIN` output it looks like:

```
Seq Scan on employees  (cost=0.00..24831.00 rows=1000000 width=64)
  Filter: (salary > 50000)
```

Notice the filter is shown *inside* the Seq Scan node — PostgreSQL applies the filter at the scan level as an optimization, discarding non-matching rows before they even fully enter the execution pipeline. This is sometimes called a **pushed-down filter** or **qual** (qualification condition).

### Index Scan

An index scan uses a B-tree (or other) index to find specific rows by jumping directly to their location on disk. Rather than reading every page, it follows the index to find the exact pages containing qualifying rows.

```
Index Scan using idx_salary on employees
  (cost=0.43..85.50 rows=10 width=64)
  Index Cond: (salary > 150000)
```

The important thing to understand about index scans is that they involve **random I/O** — jumping to different pages across the disk rather than reading them in sequence. For a small number of rows, this is dramatically faster than a sequential scan. But as you return a larger fraction of the table, the random jumps accumulate and you can end up doing *more* I/O work than a sequential scan would. This is the crossover point the optimizer is always trying to estimate.

PostgreSQL has a concept called the **random page cost** (`random_page_cost`, default 4.0) versus **sequential page cost** (`seq_page_cost`, default 1.0), which captures this intuition numerically. Random I/O is assumed to be 4x more expensive than sequential I/O. On modern SSDs, random I/O is much closer to sequential I/O, which is why lowering `random_page_cost` to around 1.1–1.5 is often recommended for SSD-backed PostgreSQL instances — it makes the optimizer more willing to use indexes.

### Index Only Scan

This is a special and very fast variant. If all the columns your query needs are stored in the index itself (meaning the index "covers" the query), PostgreSQL doesn't need to visit the actual table pages at all. It can answer the query purely from the index.

```sql
-- If there's an index on (department, salary):
SELECT salary FROM employees WHERE department = 'Engineering';
```

```
Index Only Scan using idx_dept_salary on employees
  (cost=0.43..35.50 rows=500 width=8)
  Index Cond: (department = 'Engineering')
```

This is often significantly faster than a regular index scan because it eliminates the second I/O hop to the table. Building indexes specifically to enable index-only scans — so-called **covering indexes** — is one of the most effective query optimization techniques in practice.

### Bitmap Heap Scan

This is a PostgreSQL-specific operator that solves an interesting problem. Suppose an index scan would return 50,000 rows out of a 1,000,000-row table. That's too many for an efficient index scan (too much random I/O) but using a sequential scan wastes work reading the other 950,000 rows. What to do?

PostgreSQL's answer is the bitmap scan, and it's a clever two-phase approach. In phase one, it does a **Bitmap Index Scan** — it uses the index to collect the physical locations (page numbers) of all matching rows, storing them in a bitmap in memory. No actual row data is fetched yet. In phase two, it does a **Bitmap Heap Scan** — it sorts those page numbers and reads them in order, turning what would have been random I/O into something much closer to sequential I/O.

```
Bitmap Heap Scan on employees  (cost=520.00..8500.00 rows=50000 width=64)
  Recheck Cond: (department = 'Engineering')
  ->  Bitmap Index Scan on idx_department
        (cost=0.00..507.50 rows=50000 width=0)
        Index Cond: (department = 'Engineering')
```

The bitmap scan lives in the middle ground between sequential scans and index scans. It's PostgreSQL's graceful solution to the "too many rows for index scan, too few for sequential scan" problem.

### Filter (as a standalone node)

You've already seen filters pushed into scan nodes, but sometimes you'll see a standalone `Filter` node in a plan — typically above a join, where it filters the joined output. This is a fully pipelined operator: it receives rows one at a time, checks the condition, and either passes them up or discards them.

### Sort

As we discussed, Sort is a blocking operator. PostgreSQL reads all input rows, sorts them in memory using a variant of quicksort (or merge sort if the data spills to disk), and then begins streaming sorted rows upward. The memory available for sorting is controlled by the `work_mem` setting — if the data exceeds this limit, PostgreSQL spills intermediate sort runs to temporary files on disk, which is significantly slower.

```
Sort  (cost=75000.00..77500.00 rows=1000000 width=64)
  Sort Key: salary DESC
  ->  Seq Scan on employees
```

When you see a `Sort` node in an explain plan, your first question should be: "Is this sort necessary?" Sometimes it's needed for an `ORDER BY` in your query. But sometimes it appears because a merge join needed sorted input, and you should ask whether the cost of sorting is justified compared to a hash join alternative.

### HashAggregate and GroupAggregate

PostgreSQL has two strategies for computing grouped aggregates. `HashAggregate` builds a hash table keyed on the group-by columns and accumulates aggregate values (sums, counts, etc.) for each group. It's fast but requires memory proportional to the number of distinct groups. `GroupAggregate` requires its input to already be sorted by the group-by columns, and then it streams through the sorted data in a single pass — it's memory-efficient but requires an upfront sort.

```
HashAggregate  (cost=35000.00..35200.00 rows=20 width=40)
  Group Key: department
  ->  Seq Scan on employees
```

If you have a `GROUP BY` with a small number of distinct values and plenty of `work_mem`, HashAggregate wins. If you have many distinct groups or limited memory, GroupAggregate (with a Sort node below it) might be cheaper.

---

## Putting It All Together: A Complete Plan Tree

Let's look at a query that exercises most of these operators at once and trace exactly what happens:

```sql
SELECT d.name, COUNT(*) as emp_count, AVG(e.salary) as avg_salary
FROM departments d
JOIN employees e ON e.dept_id = d.id
WHERE e.hire_year > 2018
GROUP BY d.name
ORDER BY avg_salary DESC;
```

A reasonable physical plan for this might look like:

```
Sort (avg_salary DESC)                        ← BLOCKING: sorts final groups
  |
HashAggregate (GROUP BY d.name)               ← BLOCKING: accumulates counts/sums
  |
Hash Join (e.dept_id = d.id)                  ← BLOCKING: builds hash table first
  |                    |
  |              Seq Scan on departments       ← streams 50 rows (build side)
  |
Filter (hire_year > 2018)                     ← pipelined
  |
Seq Scan on employees                         ← streams rows from disk
```

Now let's trace the execution flow. The Seq Scan on `employees` starts streaming rows upward. Each row passes through Filter — rows from 2018 or earlier are discarded, rows from 2019 onward continue. These qualifying employee rows feed into the Hash Join as the probe side. But before probing can start, the Hash Join has already built its hash table from the full scan of `departments` (50 rows — fast).

Once the hash table is ready, each filtered employee row is probed against it. Matches produce combined rows (employee + department data), which flow up into HashAggregate. HashAggregate accumulates counts and salary sums for each department name. Only after every employee row has been processed does HashAggregate start emitting group result rows upward. Those group rows then hit the Sort node, which again blocks until all groups arrive, sorts them by `avg_salary` descending, and finally streams the sorted results to you.

This query has three pipeline breakers: Hash Join (build phase), HashAggregate, and Sort. It's a "heavy" query from an execution perspective, but there's no avoiding any of these blockers given the operations requested.

---

## The `work_mem` Setting: Why It Matters So Much

Now that you understand which operators materialize data, you can appreciate why `work_mem` is one of the most impactful PostgreSQL settings to tune. It controls how much memory each blocking operator gets. The key word is "each" — a single query with three Sort/Hash nodes can use up to `3 × work_mem`. PostgreSQL's default is 4MB, which is very conservative. For analytical workloads with large sorts and hash joins, increasing this to 64MB or 256MB can make queries dramatically faster because they avoid spilling to disk.

You can set it per-session for specific heavy queries without affecting the whole server:

```sql
SET work_mem = '256MB';
-- now run your big analytical query
SELECT ...;
```

---

## What You've Just Learned to See

You now have the full mental model for reading an execution plan. Every node is either pipelined (fast, memory-efficient, starts producing output immediately) or blocking (must accumulate all input first). The tree structure tells you the order of data flow. The cost estimates tell you where the expensive work is. And the `rows=` estimates tell you whether the optimizer had good information to work with.

In the next chapter, we're going to take all of this and apply it directly to reading real `EXPLAIN` and `EXPLAIN ANALYZE` output in PostgreSQL — including the exact meaning of every number, how to spot bad estimates, how to identify the slowest node in a plan, and how to start reasoning about fixes. This is where everything becomes immediately practical for your day-to-day work with PostgreSQL.

Does this all feel coherent so far? The key thread to hold onto is: the Volcano model pulls rows up through a tree of operators, pipelined operators are free to stream, blocking operators create memory pressure and latency walls, and `work_mem` is what separates in-memory efficiency from disk-spilling pain.
