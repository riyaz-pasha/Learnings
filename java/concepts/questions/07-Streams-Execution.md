# Java Streams: Execution — Lazy vs Eager, Intermediate vs Terminal

The most important thing to understand about Java Streams is:

> **A Stream pipeline does not execute when you create it. It executes when a terminal operation asks for the result.**

Let's build this from the ground up.

---

## 1. First: What is a Stream pipeline?

Consider:

```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

List<Integer> result = numbers.stream()
        .filter(n -> n % 2 == 0)
        .map(n -> n * 10)
        .toList();
```

You can mentally divide this into:

```text
Source
  ↓
numbers.stream()

  ↓
Intermediate operation
filter()

  ↓
Intermediate operation
map()

  ↓
Terminal operation
toList()
```

So:

```text
numbers
   │
   ▼
 stream()
   │
   ▼
 filter()
   │
   ▼
 map()
   │
   ▼
 toList()
   │
   ▼
 Result
```

But **Java does not immediately process all the numbers when `filter()` or `map()` is called.**

That is the key concept.

---

# 2. Intermediate vs Terminal operations

There are two major categories.

## Intermediate operations

Intermediate operations:

* return another `Stream`
* build/extend the pipeline
* are generally **lazy**

Examples:

```java
filter()
map()
flatMap()
distinct()
sorted()
limit()
skip()
peek()
```

For example:

```java
Stream<Integer> stream = numbers.stream()
        .filter(n -> n > 2)
        .map(n -> n * 10);
```

At this point:

> **Nothing has actually been processed yet.**

You have essentially described **what should happen later**.

---

## Terminal operations

Terminal operations:

* produce a final result
* consume the stream
* trigger execution of the pipeline

Examples:

```java
toList()
collect()
forEach()
count()
findFirst()
findAny()
anyMatch()
allMatch()
noneMatch()
reduce()
min()
max()
```

For example:

```java
List<Integer> result = numbers.stream()
        .filter(n -> n > 2)
        .map(n -> n * 10)
        .toList();
```

`toList()` is terminal.

It causes the pipeline to actually execute.

---

# 3. What does "lazy" actually mean?

This is one of the most important interview concepts.

Look at this:

```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);

Stream<Integer> stream = numbers.stream()
        .filter(n -> {
            System.out.println("filter: " + n);
            return n % 2 == 0;
        })
        .map(n -> {
            System.out.println("map: " + n);
            return n * 10;
        });

System.out.println("Stream created");
```

What gets printed?

```text
Stream created
```

That's it.

You might expect:

```text
filter: 1
filter: 2
map: 2
filter: 3
...
```

But nothing happens.

Why?

Because:

```java
filter(...)
map(...)
```

are **intermediate operations**.

They are lazy.

---

# 4. Add a terminal operation

Now:

```java
List<Integer> result = numbers.stream()
        .filter(n -> {
            System.out.println("filter: " + n);
            return n % 2 == 0;
        })
        .map(n -> {
            System.out.println("map: " + n);
            return n * 10;
        })
        .toList();
```

Now execution happens.

Output:

```text
filter: 1
filter: 2
map: 2
filter: 3
filter: 4
map: 4
filter: 5
```

Result:

```text
[20, 40]
```

Notice something very important.

It does **not** execute like this:

```text
filter all elements
    ↓
1, 2, 3, 4, 5

then map all elements
    ↓
20, 40
```

Instead, it behaves more like:

```text
1
 ↓
filter → rejected

2
 ↓
filter → accepted
 ↓
map → 20

3
 ↓
filter → rejected

4
 ↓
filter → accepted
 ↓
map → 40

5
 ↓
filter → rejected
```

This is called **pipeline fusion / per-element processing**.

---

# 5. The most important visualization

Consider:

```java
numbers.stream()
        .filter(n -> n > 2)
        .map(n -> n * 10)
        .toList();
```

Many beginners imagine:

```text
             FILTER
1 ─────────────┐
2 ─────────────┤
3 ─────────────┤──→ [3,4,5]
4 ─────────────┤
5 ─────────────┘

             MAP
[3,4,5] ─────────→ [30,40,50]

             COLLECT
[30,40,50]
```

But conceptually Java Streams are much closer to:

```text
1 → filter → rejected

2 → filter → rejected

3 → filter → map → 30 → collect

4 → filter → map → 40 → collect

5 → filter → map → 50 → collect
```

This is a major reason streams can be efficient.

---

# 6. Why is laziness useful?

Because Java doesn't necessarily have to process everything.

Consider:

```java
Optional<Integer> result = numbers.stream()
        .filter(n -> n > 2)
        .findFirst();
```

Suppose:

```text
numbers = [1, 2, 3, 4, 5]
```

Execution:

```text
1 → filter → false

2 → filter → false

3 → filter → true
        ↓
     findFirst()
        ↓
      STOP
```

It doesn't need to process:

```text
4
5
```

Result:

```java
Optional[3]
```

This is called **short-circuiting**.

---

# 7. Lazy + short-circuiting

This is where Streams become particularly interesting.

```java
boolean result = numbers.stream()
        .filter(n -> n > 2)
        .map(n -> n * 10)
        .anyMatch(n -> n > 30);
```

Input:

```text
[1, 2, 3, 4, 5]
```

Execution:

```text
1
 ↓
filter > 2?
NO

2
 ↓
filter > 2?
NO

3
 ↓
filter
YES
 ↓
map → 30
 ↓
30 > 30?
NO

4
 ↓
filter
YES
 ↓
map → 40
 ↓
40 > 30?
YES
 ↓
STOP
```

`5` is never processed.

---

# 8. Intermediate operations don't necessarily execute immediately

Consider:

```java
Stream<Integer> stream = numbers.stream();

stream = stream.filter(n -> n > 2);

stream = stream.map(n -> n * 10);

stream = stream.filter(n -> n < 50);
```

Still:

```text
NO elements processed
```

You've simply created a pipeline:

```text
source
  ↓
filter(n > 2)
  ↓
map(n * 10)
  ↓
filter(n < 50)
```

Execution starts only when something like this happens:

```java
stream.toList();
```

---

# 9. Terminal operation triggers the pipeline

Think of it this way:

### Before terminal operation

```text
"Tell me WHAT you want."

stream()
   ↓
filter()
   ↓
map()
   ↓
sorted()
```

Java is essentially building a recipe.

### Terminal operation

```java
toList()
```

means:

> "Okay, now actually execute that recipe."

So:

```text
Pipeline definition
       ↓
     lazy
       ↓
Terminal operation
       ↓
    execution
```

---

# 10. Example with `peek()`

`peek()` is an intermediate operation.

It is particularly useful for understanding execution.

```java
List<Integer> result = numbers.stream()
        .filter(n -> n > 2)
        .peek(n -> System.out.println("After filter: " + n))
        .map(n -> n * 10)
        .peek(n -> System.out.println("After map: " + n))
        .toList();
```

Output:

```text
After filter: 3
After map: 30

After filter: 4
After map: 40

After filter: 5
After map: 50
```

Again:

```text
3 → filter → peek → map → peek
4 → filter → peek → map → peek
5 → filter → peek → map → peek
```

Not:

```text
filter everything
    ↓
peek everything
    ↓
map everything
    ↓
peek everything
```

---

# 11. But `sorted()` is interesting

Not every intermediate operation behaves the same way internally.

Consider:

```java
numbers.stream()
        .filter(n -> n > 1)
        .sorted()
        .map(n -> n * 10)
        .toList();
```

`sorted()` generally needs to see the elements before it can determine their sorted order.

For example:

```text
Input:

5 1 4 2 3

      ↓
   sorted()

1 2 3 4 5

      ↓
   map()

10 20 30 40 50
```

So `sorted()` is an intermediate operation, but it is **stateful**.

---

# 12. Stateless vs Stateful intermediate operations

This is another useful interview distinction.

## Stateless

Each element can be processed independently.

Examples:

```java
filter()
map()
flatMap()
peek()
```

For:

```java
map(x -> x * 2)
```

Java doesn't need to remember previous elements.

---

## Stateful

The operation may need information about multiple elements.

Examples:

```java
sorted()
distinct()
```

For:

```java
distinct()
```

Java needs to remember what it has already seen.

For:

```java
sorted()
```

Java needs to gather elements so it can order them.

So:

```text
Intermediate operations

        ┌──────────────────┐
        │                  │
   Stateless          Stateful
        │                  │
    filter()            sorted()
    map()               distinct()
    flatMap()
    peek()
```

---

# 13. Another important distinction: eager vs lazy

"Lazy" and "eager" describe **when computation happens**.

### Lazy

Computation is delayed until needed.

Streams:

```java
stream.filter(...)
      .map(...)
```

are lazy.

### Eager

Computation happens immediately.

For example, ordinary collection operations often produce a result immediately.

Conceptually:

```java
List<Integer> filtered = new ArrayList<>();

for (Integer n : numbers) {
    if (n > 2) {
        filtered.add(n);
    }
}
```

The loop executes immediately.

---

# 14. Streams don't store transformed results

This is another common misconception.

Suppose:

```java
Stream<Integer> stream = numbers.stream()
        .filter(n -> n > 2)
        .map(n -> n * 10);
```

You might imagine:

```text
stream
 ↓
[30, 40, 50]
```

Not really.

The stream represents a **pipeline of operations over the source**.

Conceptually:

```text
Source
  │
  ├── filter
  │
  ├── map
  │
  └── terminal operation
```

The resulting values are produced as the pipeline executes.

---

# 15. Streams are single-use

Once a terminal operation consumes a stream, you cannot reuse it.

```java
Stream<Integer> stream = numbers.stream();

stream.filter(n -> n > 2)
      .toList();
```

Now:

```java
stream.count();
```

will fail with:

```text
java.lang.IllegalStateException:
stream has already been operated upon or closed
```

Why?

Because a Stream represents a computation that has already been consumed.

If you need another operation:

```java
numbers.stream()
        .count();
```

Create another stream.

---

# 16. Terminal operations can have different behavior

Not all terminal operations process everything.

### Processes everything

```java
toList()
collect()
forEach()
count()
```

For example:

```java
numbers.stream()
        .map(...)
        .toList();
```

Generally needs all relevant elements.

### Can stop early

```java
findFirst()
findAny()
anyMatch()
allMatch()
noneMatch()
limit()
```

For example:

```java
numbers.stream()
        .filter(n -> n > 100)
        .findFirst();
```

As soon as a matching element is found:

```text
STOP
```

---

# 17. `limit()` is intermediate but can stop processing

This is interesting:

```java
List<Integer> result = numbers.stream()
        .filter(n -> n % 2 == 0)
        .limit(2)
        .toList();
```

Input:

```text
1 2 3 4 5
```

Execution:

```text
1 → filter → reject

2 → filter → accept → 1st result

3 → filter → reject

4 → filter → accept → 2nd result

limit(2) reached
        ↓
       STOP
```

`5` doesn't need to be processed.

---

# 18. Order of operations matters

Compare:

### Version A

```java
numbers.stream()
        .filter(n -> n % 2 == 0)
        .map(n -> n * 10)
        .limit(3)
        .toList();
```

with:

### Version B

```java
numbers.stream()
        .map(n -> n * 10)
        .filter(n -> n % 2 == 0)
        .limit(3)
        .toList();
```

They may produce the same result for this particular transformation, but execution characteristics can differ.

A much more important example:

```java
numbers.stream()
        .map(expensiveOperation)
        .filter(condition)
        .limit(3)
```

versus:

```java
numbers.stream()
        .filter(condition)
        .map(expensiveOperation)
        .limit(3)
```

The second can avoid calling the expensive operation for elements that fail the filter.

So generally:

> **Put cheap, selective operations earlier when doing so preserves correctness.**

---

# 19. A complete execution walkthrough

Consider:

```java
List<Integer> result = List.of(1, 2, 3, 4, 5, 6)
        .stream()
        .filter(n -> n % 2 == 0)
        .map(n -> n * 10)
        .filter(n -> n > 30)
        .findFirst()
        .stream()
        .toList();
```

Focus on the main pipeline:

```java
.filter(n -> n % 2 == 0)
.map(n -> n * 10)
.filter(n -> n > 30)
.findFirst()
```

Execution:

```text
1
│
├── even? NO
│
▼

2
│
├── even? YES
│
├── map → 20
│
├── > 30? NO
│
▼

3
│
├── even? NO
│
▼

4
│
├── even? YES
│
├── map → 40
│
├── > 30? YES
│
└── findFirst() → STOP
```

Notice:

```text
5 → never processed
6 → never processed
```

That's laziness + pipeline fusion + short-circuiting working together.

---

# 20. The interview mental model

When you see:

```java
source
    .filter(...)
    .map(...)
    .sorted()
    .filter(...)
    .limit(...)
    .toList();
```

Think:

### Step 1 — Source

```text
source
```

### Step 2 — Build pipeline

```text
source
   ↓
filter
   ↓
map
   ↓
sorted
   ↓
filter
   ↓
limit
```

Nothing necessarily executes yet.

### Step 3 — Terminal operation

```text
toList()
```

Now Java starts processing.

### Step 4 — Execution

Depending on the operations, Java processes elements through the pipeline, potentially stopping early.

---

# 21. Quick classification table

| Operation     | Type         | Lazy?    | Short-circuit? |
| ------------- | ------------ | -------- | -------------- |
| `filter()`    | Intermediate | Yes      | No             |
| `map()`       | Intermediate | Yes      | No             |
| `flatMap()`   | Intermediate | Yes      | No             |
| `peek()`      | Intermediate | Yes      | No             |
| `distinct()`  | Intermediate | Yes      | No             |
| `sorted()`    | Intermediate | Yes      | No             |
| `limit()`     | Intermediate | Yes      | Yes            |
| `skip()`      | Intermediate | Yes      | No             |
| `toList()`    | Terminal     | Executes | No             |
| `collect()`   | Terminal     | Executes | No             |
| `forEach()`   | Terminal     | Executes | No             |
| `count()`     | Terminal     | Executes | No             |
| `reduce()`    | Terminal     | Executes | No             |
| `findFirst()` | Terminal     | Executes | Yes            |
| `findAny()`   | Terminal     | Executes | Yes            |
| `anyMatch()`  | Terminal     | Executes | Yes            |
| `allMatch()`  | Terminal     | Executes | Yes            |
| `noneMatch()` | Terminal     | Executes | Yes            |

One subtle point:

> `sorted()` is **lazy as an intermediate operation**, but when the pipeline executes, sorting requires buffering elements before downstream processing can fully proceed.

---

# 22. The 5 things to remember for interviews

If an interviewer asks **"How does Java Stream execution work?"**, remember:

```text
1. Stream creation
       ↓
   lazy

2. Intermediate operations
       ↓
   build pipeline

3. Terminal operation
       ↓
   triggers execution

4. Execution
       ↓
   elements flow through pipeline

5. Short-circuiting
       ↓
   may stop before all elements are processed
```

The most important sentence is:

> **Intermediate operations are lazy and build a pipeline; a terminal operation triggers execution of that pipeline.**

And the deeper point is:

> **Streams generally process elements through the pipeline rather than eagerly creating an intermediate collection after every operation.**

That distinction is extremely important when understanding both **performance** and **execution order**.

