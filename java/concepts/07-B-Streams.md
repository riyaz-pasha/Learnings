# Topic 7B — Java Streams API

Now we get to one of the **most frequently asked Java 8 interview topics**.

If you understand Streams properly, you should be able to explain both:

```java
list.stream()
    .filter(...)
    .map(...)
    .collect(...);
```

and **what actually happens during execution**.

---

# 1. What is a Stream?

A Stream is a sequence of elements that supports **declarative/functional-style processing**.

Example:

```java
List<Integer> numbers =
        List.of(10, 20, 30, 40, 50);

List<Integer> result =
        numbers.stream()
               .filter(n -> n > 20)
               .collect(Collectors.toList());
```

Result:

```text
[30, 40, 50]
```

The important thing:

> A Stream is not a data structure. It is a pipeline for processing data.

---

# 2. Collection vs Stream

This is a common interview question.

### Collection

Stores data.

```java
List<Integer> numbers = ...
```

### Stream

Processes data.

```java
numbers.stream()
```

Think:

```text
Collection
    ↓
stores elements

Stream
    ↓
processes elements
```

A Stream doesn't replace the collection.

---

# 3. Basic Stream Pipeline

A typical Stream pipeline looks like:

```text
Source
  ↓
Intermediate operations
  ↓
Terminal operation
```

Example:

```java
numbers.stream()
       .filter(n -> n > 20)
       .map(n -> n * 2)
       .collect(Collectors.toList());
```

Here:

```text
numbers
   ↓
stream()           → source
   ↓
filter()           → intermediate
   ↓
map()              → intermediate
   ↓
collect()          → terminal
```

---

# 4. Source

A Stream can come from many sources.

### Collection

```java
list.stream();
```

### Set

```java
set.stream();
```

### Array

```java
Arrays.stream(array);
```

### Explicit values

```java
Stream.of(1, 2, 3, 4);
```

### File lines

```java
Files.lines(path);
```

---

# 5. Intermediate Operations

Intermediate operations transform or constrain the stream.

Common ones:

```text
filter
map
flatMap
distinct
sorted
limit
skip
peek
```

They return another Stream.

Example:

```java
Stream<Integer> result =
        numbers.stream()
               .filter(n -> n > 10)
               .map(n -> n * 2);
```

Notice:

```text
filter → Stream
map    → Stream
```

---

# 6. Terminal Operations

Terminal operations produce a final result or side effect.

Examples:

```text
collect
forEach
reduce
count
min
max
findFirst
findAny
anyMatch
allMatch
noneMatch
```

Example:

```java
long count =
        numbers.stream()
               .filter(n -> n > 10)
               .count();
```

After a terminal operation, the Stream is consumed.

---

# 7. A Stream Cannot Normally Be Reused

For example:

```java
Stream<Integer> stream =
        numbers.stream()
               .filter(n -> n > 10);

stream.count();

stream.forEach(System.out::println);
```

The second operation throws:

```text
IllegalStateException
```

because the stream has already been consumed.

If you need another traversal:

```java
numbers.stream()
       .filter(...);

numbers.stream()
       .forEach(...);
```

Create a new Stream.

---

# 8. `filter()`

`filter()` keeps elements that satisfy a condition.

Example:

```java
List<Integer> result =
        numbers.stream()
               .filter(n -> n % 2 == 0)
               .collect(Collectors.toList());
```

Input:

```text
[1, 2, 3, 4, 5, 6]
```

Output:

```text
[2, 4, 6]
```

`filter()` takes a:

```java
Predicate<T>
```

because it needs:

```text
T → boolean
```

---

# 9. `map()`

`map()` transforms every element.

Example:

```java
List<Integer> result =
        numbers.stream()
               .map(n -> n * 2)
               .collect(Collectors.toList());
```

Input:

```text
[1, 2, 3]
```

Output:

```text
[2, 4, 6]
```

`map()` uses:

```java
Function<T, R>
```

because:

```text
T → R
```

---

# 10. `filter()` vs `map()`

Very common interview question.

### `filter`

Decides:

> Should this element remain?

```java
.filter(n -> n > 10)
```

### `map`

Decides:

> What should this element become?

```java
.map(n -> n * 2)
```

Example:

```java
numbers.stream()
       .filter(n -> n > 10)
       .map(n -> n * 2)
```

means:

```text
filter → remove unwanted elements
map    → transform remaining elements
```

---

# 11. `flatMap()`

This is one of the most important Stream concepts.

Suppose:

```java
List<List<Integer>> numbers = List.of(
    List.of(1, 2),
    List.of(3, 4),
    List.of(5, 6)
);
```

If you use:

```java
numbers.stream()
       .map(list -> list.stream())
```

you get:

```text
Stream<Stream<Integer>>
```

That's usually not what you want.

Instead:

```java
List<Integer> result =
        numbers.stream()
               .flatMap(List::stream)
               .collect(Collectors.toList());
```

Result:

```text
[1, 2, 3, 4, 5, 6]
```

Think:

```text
map
→ one input produces one output

flatMap
→ one input can produce multiple outputs,
   then flatten them into one stream
```

---

# 12. `map()` vs `flatMap()`

Suppose:

```text
Input:
[A, B, C]
```

`map` can produce:

```text
[A1, B1, C1]
```

`flatMap` can produce:

```text
[A1, A2, B1, B2, C1]
```

and flatten the nested structure.

This is particularly common when dealing with:

```text
List<List<T>>
List<Optional<T>>
nested object relationships
```

---

# 13. `distinct()`

Removes duplicates.

```java
List<Integer> result =
        numbers.stream()
               .distinct()
               .collect(Collectors.toList());
```

Input:

```text
[1, 2, 2, 3, 3, 3]
```

Result:

```text
[1, 2, 3]
```

It relies on equality semantics (`equals()`/`hashCode()`) to identify duplicates.

This connects directly to our earlier topic.

---

# 14. `sorted()`

Sorts the stream.

```java
numbers.stream()
       .sorted()
       .forEach(System.out::println);
```

For reverse order:

```java
numbers.stream()
       .sorted(Comparator.reverseOrder())
       .forEach(System.out::println);
```

For objects:

```java
employees.stream()
         .sorted(
             Comparator.comparing(Employee::getSalary)
         );
```

---

# 15. `limit()`

Takes only the first N elements.

```java
List<Integer> result =
        numbers.stream()
               .limit(3)
               .collect(Collectors.toList());
```

If:

```text
[10, 20, 30, 40, 50]
```

result:

```text
[10, 20, 30]
```

---

# 16. `skip()`

Skips the first N elements.

```java
numbers.stream()
       .skip(2)
       .forEach(System.out::println);
```

Input:

```text
[10, 20, 30, 40, 50]
```

Output:

```text
30
40
50
```

`skip()` and `limit()` are particularly useful for pagination-style processing, though database pagination should usually be done at the database level rather than loading huge datasets first.

---

# 17. `count()`

Counts elements.

```java
long count =
        numbers.stream()
               .filter(n -> n > 10)
               .count();
```

Important:

```java
count()
```

returns:

```java
long
```

not `int`.

---

# 18. `min()` and `max()`

Example:

```java
Optional<Integer> max =
        numbers.stream()
               .max(Integer::compareTo);
```

Why `Optional<Integer>`?

Because the stream might be empty.

For example:

```java
Optional<Integer> max =
        Stream.<Integer>empty()
              .max(Integer::compareTo);
```

There is no value to return.

---

# 19. `findFirst()`

Returns the first element.

```java
Optional<Integer> first =
        numbers.stream()
               .filter(n -> n > 20)
               .findFirst();
```

Result is an:

```java
Optional<Integer>
```

because there may be no matching element.

---

# 20. `findAny()`

Returns any matching element.

```java
Optional<Integer> result =
        numbers.parallelStream()
               .filter(n -> n > 20)
               .findAny();
```

This becomes particularly useful with parallel streams because the implementation doesn't have to preserve encounter order for `findAny()`.

### Difference:

```text
findFirst
→ first element according to encounter order

findAny
→ any matching element
```

If you don't care which match you get, `findAny()` can provide more flexibility, especially in parallel processing.

---

# 21. `anyMatch()`

Checks whether **at least one** element matches.

```java
boolean result =
        numbers.stream()
               .anyMatch(n -> n > 100);
```

Think:

```text
"Does ANY element satisfy this?"
```

Returns:

```java
boolean
```

---

# 22. `allMatch()`

Checks whether **every** element matches.

```java
boolean result =
        numbers.stream()
               .allMatch(n -> n > 0);
```

Think:

```text
"Do ALL elements satisfy this?"
```

---

# 23. `noneMatch()`

Checks that no element matches.

```java
boolean result =
        numbers.stream()
               .noneMatch(n -> n < 0);
```

Think:

```text
"Does NONE satisfy this?"
```

---

# 24. Match operations can short-circuit

Suppose:

```java
numbers.stream()
       .anyMatch(n -> n == 5);
```

If the stream encounters `5`, it doesn't need to examine all remaining elements.

Similarly:

```text
anyMatch
→ stops when true

allMatch
→ stops when false

noneMatch
→ stops when true
```

This is called **short-circuiting**.

---

# 25. `reduce()`

`reduce()` combines elements into one result.

For example, sum:

```java
int sum =
        numbers.stream()
               .reduce(0, (a, b) -> a + b);
```

Input:

```text
[1, 2, 3, 4]
```

Conceptually:

```text
0 + 1
  ↓
1 + 2
  ↓
3 + 3
  ↓
6 + 4
  ↓
10
```

Result:

```text
10
```

---

# 26. Another reduce example

Product:

```java
int product =
        numbers.stream()
               .reduce(1, (a, b) -> a * b);
```

For:

```text
[2, 3, 4]
```

result:

```text
24
```

---

# 27. `reduce()` without an identity

You can also write:

```java
Optional<Integer> sum =
        numbers.stream()
               .reduce((a, b) -> a + b);
```

Why `Optional`?

Because an empty stream has no result.

Compare:

```java
.reduce(0, ...)
```

with:

```java
.reduce(...)
```

The first provides an identity value, so the result can always be an `int`.

---

# 28. What makes a good reduce operation?

This becomes important with parallel streams.

The reduction operation should generally be:

* associative
* compatible with the identity

For example:

```text
(a + b) + c
```

and:

```text
a + (b + c)
```

produce the same result.

Addition is associative.

This matters because parallel execution can split the data and combine partial results.

---

# 29. `forEach()`

Used to perform an action on each element.

```java
numbers.stream()
       .forEach(System.out::println);
```

It takes a:

```java
Consumer<T>
```

Remember:

```text
filter → Predicate
map    → Function
forEach → Consumer
```

---

# 30. `forEachOrdered()`

This is particularly relevant with parallel streams.

```java
numbers.parallelStream()
       .forEachOrdered(System.out::println);
```

`forEach()` doesn't guarantee encounter order for parallel execution.

`forEachOrdered()` respects encounter order where the stream has one.

But preserving order can reduce some of the performance benefits of parallel processing.

---

# 31. `collect()`

`collect()` is one of the most important terminal operations.

Example:

```java
List<Integer> result =
        numbers.stream()
               .filter(n -> n > 10)
               .collect(Collectors.toList());
```

You can collect into:

```text
List
Set
Map
String
grouped structures
```

and much more.

---

# 32. `Collectors.toList()`

Example:

```java
List<String> names =
        employees.stream()
                 .map(Employee::getName)
                 .collect(Collectors.toList());
```

This creates a list containing the mapped names.

---

# 33. `Collectors.toSet()`

```java
Set<String> names =
        employees.stream()
                 .map(Employee::getName)
                 .collect(Collectors.toSet());
```

Useful when uniqueness matters.

---

# 34. `Collectors.joining()`

Suppose:

```java
List<String> names =
        List.of("Alice", "Bob", "Charlie");
```

You can create:

```java
String result =
        names.stream()
             .collect(Collectors.joining(", "));
```

Result:

```text
Alice, Bob, Charlie
```

You can also use prefix/suffix:

```java
String result =
        names.stream()
             .collect(
                 Collectors.joining(
                     ", ",
                     "[",
                     "]"
                 )
             );
```

Result:

```text
[Alice, Bob, Charlie]
```

---

# 35. `groupingBy()`

This is **extremely important for Java interviews**.

Suppose:

```java
class Employee {
    String name;
    String department;
}
```

We want:

```text
department → employees
```

We can write:

```java
Map<String, List<Employee>> result =
        employees.stream()
                 .collect(
                     Collectors.groupingBy(
                         Employee::getDepartment
                     )
                 );
```

Conceptually:

```text
IT
 ├── Alice
 └── Bob

HR
 ├── John
 └── Sarah
```

---

# 36. `groupingBy()` with counting

Suppose we want:

```text
department → number of employees
```

Then:

```java
Map<String, Long> count =
        employees.stream()
                 .collect(
                     Collectors.groupingBy(
                         Employee::getDepartment,
                         Collectors.counting()
                     )
                 );
```

Result conceptually:

```text
IT → 5
HR → 3
Finance → 4
```

This is a very common interview coding pattern.

---

# 37. `groupingBy()` with another downstream collector

Suppose we want:

```text
department → employee names
```

Then:

```java
Map<String, List<String>> result =
        employees.stream()
                 .collect(
                     Collectors.groupingBy(
                         Employee::getDepartment,
                         Collectors.mapping(
                             Employee::getName,
                             Collectors.toList()
                         )
                     )
                 );
```

This is a more advanced but useful pattern.

---

# 38. `partitioningBy()`

`partitioningBy()` divides elements into exactly two groups:

```text
true
false
```

Example:

```java
Map<Boolean, List<Integer>> result =
        numbers.stream()
               .collect(
                   Collectors.partitioningBy(
                       n -> n % 2 == 0
                   )
               );
```

Conceptually:

```text
true  → [2, 4, 6]
false → [1, 3, 5]
```

Difference:

```text
groupingBy
→ potentially many groups

partitioningBy
→ exactly two groups based on boolean condition
```

---

# 39. `toMap()`

Suppose:

```java
Map<Integer, String> result =
        employees.stream()
                 .collect(
                     Collectors.toMap(
                         Employee::getId,
                         Employee::getName
                     )
                 );
```

This creates:

```text
employeeId → employeeName
```

---

# 40. Important `toMap()` trap

What happens if two employees have the same ID?

This:

```java
Collectors.toMap(
    Employee::getId,
    Employee::getName
)
```

can throw:

```text
IllegalStateException
```

because duplicate keys aren't automatically resolved.

You can provide a merge function:

```java
Collectors.toMap(
    Employee::getId,
    Employee::getName,
    (existing, replacement) -> existing
)
```

Now you explicitly decide what happens.

This is a **very good interview question**.

---

# 41. Stream laziness

This is one of the most important concepts.

Consider:

```java
Stream<Integer> stream =
        numbers.stream()
               .filter(n -> {
                   System.out.println("filter: " + n);
                   return n > 10;
               });
```

Nothing necessarily gets printed yet.

Why?

Because:

```text
filter()
```

is an intermediate operation.

Streams are generally **lazy**.

Execution starts when a terminal operation is invoked:

```java
stream.count();
```

Then the pipeline executes.

---

# 42. Why are Streams lazy?

Because this allows the Stream implementation to optimize the pipeline.

For example:

```java
numbers.stream()
       .filter(n -> n > 10)
       .map(n -> n * 2)
       .findFirst();
```

Java doesn't necessarily:

```text
filter every element
then map every element
then find first
```

Instead, conceptually it can process elements through the pipeline:

```text
element 1
 ↓
filter
 ↓
if accepted → map
 ↓
check findFirst
```

and stop as soon as the terminal operation has enough information.

---

# 43. Example of short-circuiting

Suppose:

```java
List<Integer> numbers =
        List.of(1, 2, 3, 4, 5, 6);
```

And:

```java
Optional<Integer> result =
        numbers.stream()
               .filter(n -> n % 2 == 0)
               .map(n -> n * 10)
               .findFirst();
```

Conceptually:

```text
1 → filter false
2 → filter true → map → 20 → first → STOP
```

It doesn't need to process:

```text
3, 4, 5, 6
```

This is an important consequence of:

```text
lazy evaluation
+
short-circuiting
```

---

# 44. Intermediate vs Terminal

Memorize this table:

| Intermediate | Terminal  |
| ------------ | --------- |
| filter       | collect   |
| map          | forEach   |
| flatMap      | reduce    |
| distinct     | count     |
| sorted       | min       |
| limit        | max       |
| skip         | findFirst |
| peek         | findAny   |
|              | anyMatch  |
|              | allMatch  |
|              | noneMatch |

### Intermediate

Returns:

```text
Stream
```

### Terminal

Ends the pipeline.

---

# 45. `peek()`

`peek()` allows you to observe elements as they pass through the pipeline.

```java
numbers.stream()
       .filter(n -> n > 10)
       .peek(n -> System.out.println("After filter: " + n))
       .map(n -> n * 2)
       .toList();
```

But don't generally use `peek()` as your main business-logic mechanism.

It is primarily useful for debugging/inspection.

---

# 46. Streams don't modify the original collection

Suppose:

```java
List<Integer> numbers =
        new ArrayList<>(List.of(1, 2, 3));

List<Integer> result =
        numbers.stream()
               .map(n -> n * 2)
               .toList();
```

`numbers` remains:

```text
[1, 2, 3]
```

and:

```text
result
```

contains:

```text
[2, 4, 6]
```

The Stream pipeline itself doesn't mutate the source unless your operations explicitly introduce side effects.

---

# 47. `map()` example with objects

Suppose:

```java
List<Employee> employees;
```

and we want employee names:

```java
List<String> names =
        employees.stream()
                 .map(Employee::getName)
                 .toList();
```

This is one of the most common real-world Stream patterns.

---

# 48. Filtering objects

```java
List<Employee> seniorEmployees =
        employees.stream()
                 .filter(e -> e.getSalary() > 100000)
                 .toList();
```

Meaning:

```text
Employee
 ↓
filter salary
 ↓
Employee
```

Notice `filter()` doesn't change the type.

---

# 49. Mapping objects

```java
List<String> names =
        employees.stream()
                 .map(Employee::getName)
                 .toList();
```

Here:

```text
Employee
 ↓
String
```

That's why `map()` uses `Function<T,R>`.

---

# 50. A complete example

Suppose:

```java
List<Employee> employees;
```

We want:

> names of employees from IT earning more than 100000, sorted alphabetically.

```java
List<String> result =
        employees.stream()
                 .filter(e -> e.getDepartment().equals("IT"))
                 .filter(e -> e.getSalary() > 100000)
                 .map(Employee::getName)
                 .sorted()
                 .toList();
```

Pipeline:

```text
Employee
   ↓
department filter
   ↓
salary filter
   ↓
name extraction
   ↓
sorting
   ↓
List<String>
```

This is exactly the kind of code you should be comfortable explaining in an interview.

---

# 51. `toList()` vs `Collectors.toList()`

Modern Java provides:

```java
stream.toList()
```

Example:

```java
List<String> names =
        employees.stream()
                 .map(Employee::getName)
                 .toList();
```

This is concise.

Historically/common Java 8 style:

```java
.collect(Collectors.toList())
```

One important distinction:

`Stream.toList()` returns an **unmodifiable** list.

If you need a mutable list, use something like:

```java
.collect(Collectors.toCollection(ArrayList::new))
```

or:

```java
.collect(Collectors.toList())
```

depending on the exact mutability guarantees you need.

---

# 52. Parallel Streams

Java also supports:

```java
numbers.parallelStream()
```

Instead of processing sequentially:

```text
1 → 2 → 3 → 4 → 5 → 6
```

the work may be divided across multiple threads.

Conceptually:

```text
        Stream
          |
     split workload
       /      \
    part 1   part 2
      |        |
   threads   threads
       \      /
        combine
```

---

# 53. Should you always use parallelStream?

**No.**

Parallelism has overhead.

For small collections:

```text
parallelization overhead
>
actual computation benefit
```

Also, operations involving:

* shared mutable state
* I/O
* ordering requirements
* synchronization
* non-associative reductions

can make parallel streams problematic or ineffective.

Use parallel streams based on measurement and workload characteristics, not simply because multiple cores exist.

---

# 54. Dangerous example with shared mutable state

Avoid:

```java
List<Integer> result = new ArrayList<>();

numbers.parallelStream()
       .forEach(n -> result.add(n));
```

`ArrayList` isn't thread-safe, so this introduces concurrency problems.

Prefer a collector:

```java
List<Integer> result =
        numbers.parallelStream()
               .map(n -> n * 2)
               .toList();
```

Let the Stream framework handle the accumulation.

---

# 55. Side effects in Streams

Avoid:

```java
List<String> result = new ArrayList<>();

names.stream()
     .filter(...)
     .forEach(name -> result.add(name));
```

Instead:

```java
List<String> result =
        names.stream()
             .filter(...)
             .toList();
```

The second version expresses the intent directly.

This is an important principle:

> Prefer stateless, side-effect-free Stream operations.

---

# 56. Stream vs Parallel Stream

### Sequential

```java
list.stream()
```

Usually processes in one thread.

### Parallel

```java
list.parallelStream()
```

May process in parallel using the common ForkJoinPool.

Don't assume:

```text
parallelStream = automatically faster
```

It isn't.

---

# 57. A very important interview question

### What is the difference between `map()` and `flatMap()`?

Strong answer:

> "`map()` transforms each element into one result, so it can produce a nested structure such as `Stream<List<T>>`. `flatMap()` transforms each element into a stream and then flattens those resulting streams into a single stream."

Example:

```java
List<List<Integer>> input = ...
```

```java
input.stream()
     .map(List::stream)
```

gives:

```text
Stream<Stream<Integer>>
```

whereas:

```java
input.stream()
     .flatMap(List::stream)
```

gives:

```text
Stream<Integer>
```

---

# 58. Another very important interview question

### `findFirst()` vs `findAny()`?

```text
findFirst
→ respects encounter order

findAny
→ any matching element
```

`findAny()` is particularly useful when order doesn't matter, especially with parallel streams.

---

# 59. Another one

### `reduce()` vs `collect()`?

A useful distinction:

`reduce()` is primarily about reducing a stream to **one combined value**:

```java
int sum =
    numbers.stream()
           .reduce(0, Integer::sum);
```

`collect()` is designed for **mutable result accumulation**, such as:

```java
List<T>
Set<T>
Map<K,V>
grouped results
```

For example:

```java
List<Integer> result =
    numbers.stream()
           .filter(...)
           .collect(Collectors.toList());
```

---

# 60. Stream execution mental model

This is the most important thing to understand:

```text
             SOURCE
               |
               v
        Intermediate ops
        -----------------
        filter
        map
        flatMap
        sorted
        distinct
        limit
               |
               v
          TERMINAL OP
        -----------------
        collect
        reduce
        count
        findFirst
        forEach
               |
               v
             RESULT
```

But execution is generally **lazy**.

The pipeline isn't necessarily executed operation-by-operation over the entire collection.

Instead, once a terminal operation starts, elements can flow through the pipeline, and short-circuiting operations may stop processing early.

---

# 61. EPAM Stream questions you should master

These are particularly worth practicing:

### Fundamentals

1. What is a Stream?
2. Collection vs Stream?
3. Intermediate vs terminal operations?
4. Why are Streams lazy?
5. Can you reuse a Stream?
6. Does Stream modify the original collection?

### Operations

7. `filter()` vs `map()`
8. `map()` vs `flatMap()`
9. `findFirst()` vs `findAny()`
10. `anyMatch()` vs `allMatch()` vs `noneMatch()`
11. `limit()` vs `skip()`
12. `distinct()` internally depends on what?
13. `sorted()` with Comparator
14. `reduce()` with/without identity
15. `reduce()` vs `collect()`

### Collectors

16. `toList()`
17. `toSet()`
18. `toMap()`
19. Duplicate-key problem with `toMap()`
20. `groupingBy()`
21. `partitioningBy()`
22. `joining()`
23. Downstream collectors

### Advanced

24. What is short-circuiting?
25. What is lazy evaluation?
26. What is a parallel stream?
27. When should you avoid parallel streams?
28. Why is shared mutable state dangerous?
29. `forEach()` vs `forEachOrdered()`
30. `peek()` — when should it be used?

---

## The 10 methods I'd memorize first

If you want the **highest interview value**, know these extremely well:

```text
filter()
    → keep matching elements

map()
    → transform elements

flatMap()
    → transform + flatten

sorted()
    → sort

distinct()
    → remove duplicates

collect()
    → build result

reduce()
    → combine into one result

groupingBy()
    → group elements

findFirst()
    → first matching element

anyMatch()
    → whether any element matches
```

And remember the functional interfaces behind them:

```text
filter    → Predicate
map       → Function
forEach   → Consumer
```

**Next topic: `Optional` in Java 8**, followed by the deeper Java 8 interview topics around interface default/static methods, and then we'll move into JVM memory/GC and multithreading.

