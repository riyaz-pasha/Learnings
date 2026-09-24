# ☕ Java 8 Streams — EPAM Interview Prep Guide

> A comprehensive reference guide covering Java 8 functional programming, Streams API, Collectors, Optional, Comparators, Collections Framework, and 52 coding interview problems with complete solutions.

## 🎯 Overview & Battle Plan

EPAM coding rounds heavily test **Java 8 Streams, Collectors, Optional, Comparators, and functional programming**. This guide compiles all essential core concepts and 52 commonly asked interview coding problems.

| Metric | Value |
| :--- | :--- |
| **Core Concept Modules** | 9 modules |
| **Total Practice Problems** | 52 problems |
| 🟢 **Easy Problems** | 11 |
| 🟡 **Medium Problems** | 23 |
| 🔴 **Hard Problems** | 18 |

## 📑 Table of Contents <a id="table-of-contents"></a>

### Core Concepts

- [🌊 Stream Basics](#stream-basics)
- [⚙️ Intermediate Operations](#intermediate)
- [🏁 Terminal Operations](#terminal)
- [🗂️ Collectors](#collectors)
- [📦 Optional](#optional)
- [📚 Collections Framework](#collections)
- [⚖️ Comparator & Comparable](#comparator)
- [λ Functional Interfaces](#functional)
- [⚡ Parallel Streams](#parallel)
- [⚡ Quick Cheat Sheet](#quick-cheat-sheet)

### Practice Problems by Category

- [**Basic Streams** (10)](#basic-streams)
  - [🟢 #1 Sum of all elements](#problem-1)
  - [🟢 #2 Filter even numbers](#problem-2)
  - [🟢 #3 Convert list to uppercase](#problem-3)
  - [🟢 #4 Find first element starting with 'A'](#problem-4)
  - [🟢 #5 Count strings longer than 5 chars](#problem-5)
  - [🟢 #6 Remove duplicates and sort](#problem-6)
  - [🟢 #7 Find max and min](#problem-7)
  - [🟢 #8 Check if any/all/none match](#problem-8)
  - [🟢 #9 Flatten nested lists (flatMap)](#problem-9)
  - [🟢 #10 Join strings with delimiter](#problem-10)
- [**Strings** (7)](#strings)
  - [🟡 #11 Find duplicate characters in a string](#problem-11)
  - [🟡 #12 First non-repeating character](#problem-12)
  - [🟡 #13 Count vowels and consonants](#problem-13)
  - [🟡 #14 Reverse each word in a sentence](#problem-14)
  - [🟡 #15 Check if string is a palindrome](#problem-15)
  - [🟡 #16 Word frequency count](#problem-16)
  - [🔴 #17 Longest word in a sentence](#problem-17)
- [**Numbers** (8)](#numbers)
  - [🟢 #18 Find average of list](#problem-18)
  - [🟡 #19 Fibonacci using iterate](#problem-19)
  - [🟡 #20 Find prime numbers up to N](#problem-20)
  - [🟡 #21 Square of even numbers](#problem-21)
  - [🟡 #22 Second largest number](#problem-22)
  - [🟡 #23 Partition numbers into even/odd](#problem-23)
  - [🟡 #24 Sum of digits using streams](#problem-24)
  - [🔴 #25 Running total (prefix sum)](#problem-25)
- [**Objects & Employees** (7)](#objects-employees)
  - [🟡 #26 Group employees by department](#problem-26)
  - [🟡 #27 Highest salary per department](#problem-27)
  - [🟡 #28 Average salary by department](#problem-28)
  - [🟡 #29 Filter and sort employees](#problem-29)
  - [🟡 #30 Top 3 highest paid employees](#problem-30)
  - [🔴 #31 Department with highest average salary](#problem-31)
  - [🔴 #32 Employees with same salary](#problem-32)
- [**Maps & Collections** (6)](#maps-collections)
  - [🟡 #33 Invert a Map](#problem-33)
  - [🟡 #34 Sort Map by value](#problem-34)
  - [🟡 #35 Merge two maps](#problem-35)
  - [🟡 #36 Find most frequent element](#problem-36)
  - [🔴 #37 Group anagrams together](#problem-37)
  - [🔴 #38 Count character frequency in a list of strings](#problem-38)
- [**Advanced** (7)](#advanced)
  - [🔴 #39 Custom Collector — Sum of squares](#problem-39)
  - [🔴 #40 Nested grouping](#problem-40)
  - [🔴 #41 Collectors.toMap with duplicate key handling](#problem-41)
  - [🔴 #42 Stream with index (enumerate)](#problem-42)
  - [🔴 #43 Transpose a matrix](#problem-43)
  - [🔴 #44 Sliding window maximum](#problem-44)
  - [🔴 #45 Longest consecutive sequence length](#problem-45)
- [**EPAM Favorites** (7)](#epam-favorites)
  - [🟡 #46 List of Strings → Map<length, List<String>>](#problem-46)
  - [🟡 #47 Integer list to comma-separated string](#problem-47)
  - [🔴 #48 Find all pairs summing to target](#problem-48)
  - [🔴 #49 Statistics summary](#problem-49)
  - [🔴 #50 Chained Optional operations](#problem-50)
  - [🔴 #51 Infinite stream with limit](#problem-51)
  - [🔴 #52 Collect to immutable map with transformation](#problem-52)

---

## ⚡ Quick Cheat Sheet <a id="quick-cheat-sheet"></a>

| Operation / API | Type | Description / Purpose |
| :--- | :--- | :--- |
| `filter(Predicate)` | Intermediate (Lazy) | Keeps only elements matching condition |
| `map(Function)` | Intermediate (Lazy) | Transforms each element 1-to-1 |
| `flatMap(Function)` | Intermediate (Lazy) | Flattens nested streams/collections 1-to-N |
| `distinct()` | Intermediate (Lazy) | Removes duplicates using `.equals()` |
| `sorted()` / `sorted(Comparator)` | Intermediate (Lazy) | Sorts in natural or custom order |
| `peek(Consumer)` | Intermediate (Lazy) | Performs side-effect (debugging) without consuming |
| `limit(n)` / `skip(n)` | Intermediate (Lazy) | Truncates or skips elements |
| `collect(Collector)` | Terminal (Eager) | Gathers stream results into List, Set, Map, String, etc. |
| `groupingBy(classifier)` | Collector | Groups elements into a `Map<K, List<V>>` or downstream collector |
| `partitioningBy(predicate)` | Collector | Splits stream into `Map<Boolean, List<T>>` |
| `reduce(identity, accumulator)` | Terminal (Eager) | Folds stream elements into a single aggregate value |
| `Optional.ofNullable(val)` | Utility | Wraps value safely, preventing `NullPointerException` |

---

## 📖 Concept Modules

### 🌊 Stream Basics <a id="stream-basics"></a>

A **Stream** is a sequence of elements supporting sequential and parallel aggregate operations.

#### Key Traits
- **Not a data structure**: Does not store elements; carries values from a source through a pipeline of computational steps.
- **Functional in nature**: Does not modify the underlying data source.
- **Lazily evaluated**: Intermediate operations run only when a terminal operation is invoked.
- **Traversable only ONCE**: Once consumed by a terminal operation, a stream cannot be reused.

#### Stream Pipeline Architecture
```text
Source → Intermediate Operations (Lazy) → Terminal Operation (Eager)
```

#### Creating Streams
```java
collection.stream()              // From any Collection
Arrays.stream(arr)                // From an array
Stream.of(1, 2, 3)                // From explicit values
Stream.iterate(0, n -> n + 1)     // Infinite stream (seed + unary op)
Stream.generate(Math::random)     // Infinite stream (supplier)
IntStream.range(1, 10)            // Primitives: 1 to 9
IntStream.rangeClosed(1, 10)      // Primitives: 1 to 10
```

[↑ Back to Table of Contents](#table-of-contents)

---

### ⚙️ Intermediate Operations <a id="intermediate"></a>

Intermediate operations return a new `Stream`. They are **lazy** — execution is deferred until a terminal operation is executed.

| Method | Parameter | Description |
| :--- | :--- | :--- |
| `filter(Predicate)` | `Predicate<T>` | Keep elements that match the condition |
| `map(Function)` | `Function<T, R>` | Transform each element to another object |
| `flatMap(Function)` | `Function<T, Stream<R>>` | Flattens nested streams (e.g. `List<List<T>>` to `List<T>`) |
| `distinct()` | none | Removes duplicate elements (uses `equals()` / `hashCode()`) |
| `sorted()` | none | Sorts in natural ordering |
| `sorted(Comparator)` | `Comparator<T>` | Sorts using custom comparator |
| `peek(Consumer)` | `Consumer<T>` | Inspects elements without consuming (ideal for debugging) |
| `limit(n)` | `long` | Truncates stream to first `n` elements |
| `skip(n)` | `long` | Discards first `n` elements |
| `mapToInt/Long/Double` | Primitive Function | Maps object stream to primitive stream (`IntStream`, etc.) |

#### Example Pipeline
```java
list.stream()
    .filter(s -> s.startsWith("A"))
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());
```

[↑ Back to Table of Contents](#table-of-contents)

---

### 🏁 Terminal Operations <a id="terminal"></a>

Terminal operations are **eager** — they trigger the execution of the pipeline, consume the stream, and produce a final non-stream result (collection, number, boolean, or void).

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `collect(Collector)` | `R` | Gathers elements into container/map/summary |
| `forEach(Consumer)` | `void` | Performs an action for each element |
| `forEachOrdered(Consumer)` | `void` | Performs an action in encounter order (especially in parallel) |
| `count()` | `long` | Returns count of elements |
| `findFirst()` | `Optional<T>` | Returns the first element |
| `findAny()` | `Optional<T>` | Returns any element (faster in parallel streams) |
| `anyMatch(Predicate)` | `boolean` | `true` if at least one element matches |
| `allMatch(Predicate)` | `boolean` | `true` if all elements match |
| `noneMatch(Predicate)` | `boolean` | `true` if no elements match |
| `min(Comparator)` | `Optional<T>` | Minimum element based on comparator |
| `max(Comparator)` | `Optional<T>` | Maximum element based on comparator |
| `reduce(identity, BinaryOperator)` | `T` | Folds stream elements into a single accumulated result |
| `toArray()` | `Object[]` / `A[]` | Converts stream elements into an array |

#### Reduce Example
```java
int sum = list.stream().reduce(0, Integer::sum);
```

[↑ Back to Table of Contents](#table-of-contents)

---

### 🗂️ Collectors <a id="collectors"></a>

The `java.util.stream.Collectors` utility class provides factory methods for reduction and accumulation operations.

#### Common Collector Factories
```java
Collectors.toList()
Collectors.toSet()
Collectors.toMap(keyMapper, valueMapper)
Collectors.toMap(k, v, mergeFunction)          // Handle duplicate keys
Collectors.toUnmodifiableList()                // Java 10+
Collectors.joining()
Collectors.joining(", ", "[", "]")            // Delimiter, prefix, suffix
Collectors.counting()
Collectors.summingInt(fn)
Collectors.averagingInt(fn)
Collectors.summarizingInt(fn)                  // Returns count, sum, min, max, avg
Collectors.groupingBy(classifier)
Collectors.groupingBy(classifier, downstream)
Collectors.partitioningBy(predicate)
Collectors.mapping(fn, downstream)
```

#### Collector Examples
```java
// 1. Group by first character
Map<Character, List<String>> grouped =
    list.stream().collect(Collectors.groupingBy(s -> s.charAt(0)));

// 2. Count per group (downstream collector)
Map<String, Long> countByDept =
    employees.stream().collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));

// 3. Partition even/odd (boolean keys)
Map<Boolean, List<Integer>> parts =
    nums.stream().collect(Collectors.partitioningBy(n -> n % 2 == 0));
```

[↑ Back to Table of Contents](#table-of-contents)

---

### 📦 Optional <a id="optional"></a>

`Optional<T>` is a container object that may or may not contain a non-null value, designed to prevent `NullPointerException`.

```java
// Creating Optional
Optional.of(value)                       // Throws NullPointerException if value is null
Optional.ofNullable(value)               // Safe: returns Optional.empty() if null
Optional.empty()                        // Explicitly empty

// Checking
opt.isPresent()                         // true if non-null value is present
opt.isEmpty()                           // Java 11+

// Accessing
opt.get()                               // Throws NoSuchElementException if empty (use with caution!)
opt.orElse(defaultValue)                // Returns default if empty
opt.orElseGet(Supplier)                 // Lazy default value evaluation
opt.orElseThrow()                       // Throws NoSuchElementException (Java 10+)
opt.orElseThrow(CustomException::new)   // Throws custom exception

// Transforming
opt.map(fn)                             // Maps value if present
opt.flatMap(fn)                         // Maps returning Optional without double wrapping
opt.filter(predicate)                   // Keeps value only if matching predicate
opt.ifPresent(Consumer)                 // Runs Consumer only if value present
opt.ifPresentOrElse(Consumer, Runnable) // Java 9+: handle both present and empty
```

> [!TIP]
> **Best Practice:** Avoid calling `Optional.get()` directly without checking `isPresent()`. Prefer declarative methods like `orElse()`, `orElseGet()`, `orElseThrow()`, or `ifPresent()`.

[↑ Back to Table of Contents](#table-of-contents)

---

### 📚 Collections Framework <a id="collections"></a>

Quick reference of core collections, ordering, thread-safety, and complexity:

#### List (Ordered, permits duplicates)
- `ArrayList`: Fast random access $O(1)$, slow insert/delete in middle $O(n)$.
- `LinkedList`: Fast insert/delete at ends $O(1)$, slow indexed access $O(n)$.
- `Vector`: Synchronized `ArrayList` legacy class (rarely recommended).

#### Set (No duplicates)
- `HashSet`: Hash table based, $O(1)$ add/contains, no order guarantee.
- `LinkedHashSet`: Maintains insertion order.
- `TreeSet`: Red-Black tree based, sorted order (natural/Comparator), $O(\log n)$ operations.

#### Map (Key-Value pairs)
- `HashMap`: $O(1)$ lookup/insert, unordered, allows 1 `null` key.
- `LinkedHashMap`: Maintains insertion order (or access order for LRU caches).
- `TreeMap`: Sorted by keys, $O(\log n)$ lookup/insert.
- `Hashtable`: Synchronized legacy class, rejects `null` keys/values.
- `ConcurrentHashMap`: Thread-safe without locking whole map, rejects `null` keys/values.

#### Queue / Deque
- `PriorityQueue`: Heap-based, sorted by priority order (natural or Comparator).
- `ArrayDeque`: Resizable array FIFO queue + LIFO stack, faster than `Stack` and `LinkedList`.
- `LinkedList`: Implements both `List` and `Deque` interfaces.

> [!NOTE]
> **Implementation Insight:** `HashSet` internally wraps a `HashMap` where the set elements are stored as map keys with a dummy `PRESENT` object value! Default initial capacity is `16` with a load factor of `0.75`.

[↑ Back to Table of Contents](#table-of-contents)

---

### ⚖️ Comparator & Comparable <a id="comparator"></a>

#### Comparable (Natural Ordering)
Implemented inside the domain class itself:

```java
class Employee implements Comparable<Employee> {
    private String name;
    
    @Override
    public int compareTo(Employee other) {
        return this.name.compareTo(other.name);
    }
}
```

#### Comparator (External / Custom Ordering)
Passed into sorting methods or stream operations:

```java
// 1. Lambda syntax
Comparator<Employee> byAge = (a, b) -> a.getAge() - b.getAge();

// 2. Method reference
Comparator<Employee> byName = Comparator.comparing(Employee::getName);

// 3. Multi-field chaining
Comparator<Employee> comp =
    Comparator.comparing(Employee::getDept)
              .thenComparing(Employee::getName)
              .thenComparingInt(Employee::getAge);

// 4. Reversal
comp.reversed();
Comparator.reverseOrder();

// 5. Null-safe sorting
Comparator.nullsFirst(Comparator.naturalOrder());
Comparator.nullsLast(Comparator.naturalOrder());
```

[↑ Back to Table of Contents](#table-of-contents)

---

### λ Functional Interfaces <a id="functional"></a>

All in the `java.util.function` package and annotated with `@FunctionalInterface`:

| Interface | Signature | SAM Method | Typical Usage |
| :--- | :--- | :--- | :--- |
| `Predicate<T>` | `T -> boolean` | `boolean test(T t)` | Filtering conditions |
| `Function<T, R>` | `T -> R` | `R apply(T t)` | Mapping and transforming |
| `BiFunction<T, U, R>` | `T, U -> R` | `R apply(T t, U u)` | Combining two inputs to output |
| `Consumer<T>` | `T -> void` | `void accept(T t)` | Iterating, printing, logging |
| `BiConsumer<T, U>` | `T, U -> void` | `void accept(T t, U u)` | Map iteration (`forEach`) |
| `Supplier<T>` | `() -> T` | `T get()` | Factory, lazy evaluation |
| `UnaryOperator<T>` | `T -> T` | `T apply(T t)` | Single type transform |
| `BinaryOperator<T>` | `T, T -> T` | `T apply(T t1, T t2)` | Reductions / aggregations |

#### Primitive Variants (Avoid Auto-Boxing Overhead)
- `IntPredicate`, `LongPredicate`, `DoublePredicate`
- `IntFunction<R>`, `IntConsumer`, `IntSupplier`
- `IntUnaryOperator`, `IntBinaryOperator`

#### Composition Methods
- `Predicate`: `.and()`, `.or()`, `.negate()`
- `Function`: `.compose()`, `.andThen()`, `Function.identity()`
- `Consumer`: `.andThen()`

#### Example
```java
Predicate<String> nonEmpty = s -> !s.isEmpty();
Predicate<String> longStr = s -> s.length() > 5;
Predicate<String> combined = nonEmpty.and(longStr);
```

[↑ Back to Table of Contents](#table-of-contents)

---

### ⚡ Parallel Streams <a id="parallel"></a>

Parallel streams partition data across multiple threads using the common `ForkJoinPool`.

```java
collection.parallelStream()
stream.parallel()
stream.sequential()  // Convert back to sequential
```

#### When to Use
- Large datasets (10,000+ elements)
- CPU-intensive, stateless, non-blocking calculations
- Operations where encounter order does not matter

#### When NOT to Use
- Small datasets (thread coordination overhead exceeds execution speedup)
- Blocking I/O or database calls (starves the shared common ForkJoinPool)
- Stateful operations or shared mutable state

#### Thread Safety Example
```java
// WRONG — race conditions on ArrayList.add()!
List<Integer> result = new ArrayList<>();
list.parallelStream().forEach(result::add);

// RIGHT — use thread-safe reduction/collection
List<Integer> result = list.parallelStream()
                           .collect(Collectors.toList());
```

#### Key Gotchas
1. `forEach` does not preserve order in parallel; use `forEachOrdered` if order is required.
2. `reduce` operations must be associative and stateless.
3. Avoid mutating shared state across parallel threads.

[↑ Back to Table of Contents](#table-of-contents)

---

## 💻 Practice Problems

### Basic Streams <a id="basic-streams"></a>

#### Problem 1: Sum of all elements <a id="problem-1"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Given a list of integers, find the sum using streams.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
int sum = nums.stream()
              .mapToInt(Integer::intValue)
              .sum();
// OR
int sum2 = nums.stream().reduce(0, Integer::sum);
System.out.println(sum); // 15
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 2: Filter even numbers <a id="problem-2"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> From a list of integers, collect only even numbers.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8);
List<Integer> evens = nums.stream()
                          .filter(n -> n % 2 == 0)
                          .collect(Collectors.toList());
// [2, 4, 6, 8]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 3: Convert list to uppercase <a id="problem-3"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Given a list of strings, return a new list with all strings in uppercase.

**Solution:**
```java
List<String> names = Arrays.asList("alice", "bob", "charlie");
List<String> upper = names.stream()
                          .map(String::toUpperCase)
                          .collect(Collectors.toList());
// [ALICE, BOB, CHARLIE]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 4: Find first element starting with 'A' <a id="problem-4"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> From a list of strings, find the first one starting with 'A'.

**Solution:**
```java
List<String> names = Arrays.asList("Bob","Alice","Anna","Charlie");
Optional<String> first = names.stream()
                              .filter(s -> s.startsWith("A"))
                              .findFirst();
first.ifPresent(System.out::println); // Alice
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 5: Count strings longer than 5 chars <a id="problem-5"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Count how many strings in a list have length > 5.

**Solution:**
```java
List<String> words = Arrays.asList("apple","banana","kiwi","strawberry","fig");
long count = words.stream()
                  .filter(s -> s.length() > 5)
                  .count();
// 2 (banana, strawberry)
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 6: Remove duplicates and sort <a id="problem-6"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Given a list with duplicates, return a sorted list of unique elements.

**Solution:**
```java
List<Integer> nums = Arrays.asList(5,3,1,3,2,5,4,2);
List<Integer> result = nums.stream()
                           .distinct()
                           .sorted()
                           .collect(Collectors.toList());
// [1, 2, 3, 4, 5]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 7: Find max and min <a id="problem-7"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Find the max and min values from a list of integers.

**Solution:**
```java
List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
Optional<Integer> max = nums.stream().max(Integer::compareTo);
Optional<Integer> min = nums.stream().min(Integer::compareTo);
// OR using IntStream
int max2 = nums.stream().mapToInt(i->i).max().getAsInt(); // 9
int min2 = nums.stream().mapToInt(i->i).min().getAsInt(); // 1
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 8: Check if any/all/none match <a id="problem-8"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Check if any number is negative, all are positive, and none exceed 100.

**Solution:**
```java
List<Integer> nums = Arrays.asList(5, 10, 15, 20, 25);
boolean anyNeg    = nums.stream().anyMatch(n -> n < 0);   // false
boolean allPos    = nums.stream().allMatch(n -> n > 0);   // true
boolean noneOver  = nums.stream().noneMatch(n -> n > 100);// true
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 9: Flatten nested lists (flatMap) <a id="problem-9"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Given a list of lists, flatten them into a single list.

**Solution:**
```java
List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2, 3),
    Arrays.asList(4, 5),
    Arrays.asList(6, 7, 8, 9)
);
List<Integer> flat = nested.stream()
                           .flatMap(Collection::stream)
                           .collect(Collectors.toList());
// [1, 2, 3, 4, 5, 6, 7, 8, 9]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 10: Join strings with delimiter <a id="problem-10"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Basic Streams

**Problem Statement:**
> Join a list of strings with ', ' as delimiter, wrapped in [ and ].

**Solution:**
```java
List<String> names = Arrays.asList("Alice","Bob","Charlie");
String result = names.stream()
                     .collect(Collectors.joining(", ", "[", "]"));
// [Alice, Bob, Charlie]
```

[↑ Back to Table of Contents](#table-of-contents)

---

### Strings <a id="strings"></a>

#### Problem 11: Find duplicate characters in a string <a id="problem-11"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Find all characters that appear more than once in a string.

**Solution:**
```java
String str = "programming";
Map<Character, Long> freq = str.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

freq.entrySet().stream()
    .filter(e -> e.getValue() > 1)
    .map(Map.Entry::getKey)
    .forEach(System.out::println);
// r, g, m
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 12: First non-repeating character <a id="problem-12"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Find the first character in a string that doesn't repeat.

**Solution:**
```java
String str = "swiss";
Optional<Character> result = str.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
    .entrySet().stream()
    .filter(e -> e.getValue() == 1)
    .map(Map.Entry::getKey)
    .findFirst();

result.ifPresent(System.out::println); // w
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 13: Count vowels and consonants <a id="problem-13"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Count the number of vowels and consonants in a string.

**Solution:**
```java
String str = "Hello World";
long vowels = str.toLowerCase().chars()
    .filter(c -> "aeiou".indexOf(c) != -1)
    .count(); // 3

long consonants = str.toLowerCase().chars()
    .filter(Character::isLetter)
    .filter(c -> "aeiou".indexOf(c) == -1)
    .count(); // 7
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 14: Reverse each word in a sentence <a id="problem-14"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Given a sentence, reverse each word but keep the word order.

**Solution:**
```java
String sentence = "Hello World Java";
String result = Arrays.stream(sentence.split(" "))
    .map(word -> new StringBuilder(word).reverse().toString())
    .collect(Collectors.joining(" "));
// olleH dlroW avaJ
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 15: Check if string is a palindrome <a id="problem-15"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Check if a given string is a palindrome using streams.

**Solution:**
```java
String str = "racecar";
String reversed = IntStream.range(0, str.length())
    .mapToObj(i -> String.valueOf(str.charAt(str.length() - 1 - i)))
    .collect(Collectors.joining());
boolean isPalindrome = str.equals(reversed); // true

// Cleaner approach:
boolean isPalin = IntStream.range(0, str.length() / 2)
    .allMatch(i -> str.charAt(i) == str.charAt(str.length()-1-i));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 16: Word frequency count <a id="problem-16"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Strings

**Problem Statement:**
> Count the frequency of each word in a sentence.

**Solution:**
```java
String sentence = "the quick brown fox jumps over the lazy dog the fox";
Map<String, Long> freq = Arrays.stream(sentence.split(" "))
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
// {the=3, fox=2, quick=1, brown=1, ...}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 17: Longest word in a sentence <a id="problem-17"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Strings

**Problem Statement:**
> Find the longest word in a given sentence.

**Solution:**
```java
String sentence = "Java streams are very powerful";
Optional<String> longest = Arrays.stream(sentence.split(" "))
    .max(Comparator.comparingInt(String::length));
longest.ifPresent(System.out::println); // powerful

// Get length too:
int maxLen = Arrays.stream(sentence.split(" "))
    .mapToInt(String::length).max().getAsInt(); // 8
```

[↑ Back to Table of Contents](#table-of-contents)

---

### Numbers <a id="numbers"></a>

#### Problem 18: Find average of list <a id="problem-18"></a>

- **Difficulty:** 🟢 Easy
- **Category:** Numbers

**Problem Statement:**
> Calculate the average of a list of integers.

**Solution:**
```java
List<Integer> nums = Arrays.asList(10, 20, 30, 40, 50);
OptionalDouble avg = nums.stream()
    .mapToInt(Integer::intValue)
    .average();
avg.ifPresent(System.out::println); // 30.0

// Using Collectors:
Double avg2 = nums.stream()
    .collect(Collectors.averagingInt(Integer::intValue)); // 30.0
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 19: Fibonacci using iterate <a id="problem-19"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> Generate first N Fibonacci numbers using Stream.iterate.

**Solution:**
```java
int n = 10;
Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
      .limit(n)
      .map(f -> f[0])
      .forEach(System.out::println);
// 0, 1, 1, 2, 3, 5, 8, 13, 21, 34
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 20: Find prime numbers up to N <a id="problem-20"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> Find all prime numbers up to N using streams.

**Solution:**
```java
int n = 50;
List<Integer> primes = IntStream.rangeClosed(2, n)
    .filter(num -> IntStream.rangeClosed(2, (int) Math.sqrt(num))
                            .allMatch(i -> num % i != 0))
    .boxed()
    .collect(Collectors.toList());
// [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 21: Square of even numbers <a id="problem-21"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> From a list, filter even numbers and return list of their squares, sorted descending.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
List<Integer> result = nums.stream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .sorted(Comparator.reverseOrder())
    .collect(Collectors.toList());
// [100, 64, 36, 16, 4]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 22: Second largest number <a id="problem-22"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> Find the second largest number in a list.

**Solution:**
```java
List<Integer> nums = Arrays.asList(5, 3, 9, 1, 7, 2, 8, 4, 6);
Optional<Integer> second = nums.stream()
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();
second.ifPresent(System.out::println); // 8
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 23: Partition numbers into even/odd <a id="problem-23"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> Partition a list of integers into even and odd groups.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
Map<Boolean, List<Integer>> partitioned = nums.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
// {true=[2,4,6,8,10], false=[1,3,5,7,9]}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 24: Sum of digits using streams <a id="problem-24"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Numbers

**Problem Statement:**
> Find the sum of digits of a given number using streams.

**Solution:**
```java
int number = 12345;
int sumOfDigits = String.valueOf(number).chars()
    .map(Character::getNumericValue)
    .sum();
// 15

// Alternative:
int sum2 = Integer.toString(number).chars()
    .map(c -> c - '0')
    .sum();
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 25: Running total (prefix sum) <a id="problem-25"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Numbers

**Problem Statement:**
> Convert a list of integers into their running totals.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
// Using iterate (Java 9+)
int[] running = {0};
List<Integer> prefixSum = nums.stream()
    .map(n -> running[0] += n)
    .collect(Collectors.toList());
// [1, 3, 6, 10, 15]
```

[↑ Back to Table of Contents](#table-of-contents)

---

### Objects & Employees <a id="objects-employees"></a>

#### Problem 26: Group employees by department <a id="problem-26"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Objects & Employees

**Problem Statement:**
> Given a list of Employee(name, dept, salary), group them by department.

**Solution:**
```java
// Employee class has: name, dept, salary
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept));

// Count per department:
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 27: Highest salary per department <a id="problem-27"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Objects & Employees

**Problem Statement:**
> Find the employee with the highest salary in each department.

**Solution:**
```java
Map<String, Optional<Employee>> highestPaid = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));

// To get the actual employee (not Optional):
Map<String, Employee> result = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDept,
        Function.identity(),
        BinaryOperator.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 28: Average salary by department <a id="problem-28"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Objects & Employees

**Problem Statement:**
> Calculate average salary per department.

**Solution:**
```java
Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.averagingDouble(Employee::getSalary)
    ));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 29: Filter and sort employees <a id="problem-29"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Objects & Employees

**Problem Statement:**
> Get names of employees with salary > 50000, sorted alphabetically.

**Solution:**
```java
List<String> names = employees.stream()
    .filter(e -> e.getSalary() > 50000)
    .sorted(Comparator.comparing(Employee::getName))
    .map(Employee::getName)
    .collect(Collectors.toList());
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 30: Top 3 highest paid employees <a id="problem-30"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Objects & Employees

**Problem Statement:**
> Find the top 3 employees by salary.

**Solution:**
```java
List<Employee> top3 = employees.stream()
    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
    .limit(3)
    .collect(Collectors.toList());

// Just names:
List<String> top3Names = employees.stream()
    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
    .limit(3)
    .map(Employee::getName)
    .collect(Collectors.toList());
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 31: Department with highest average salary <a id="problem-31"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Objects & Employees

**Problem Statement:**
> Find the department name with the highest average salary.

**Solution:**
```java
Optional<Map.Entry<String, Double>> topDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.averagingDouble(Employee::getSalary)
    ))
    .entrySet().stream()
    .max(Map.Entry.comparingByValue());

topDept.ifPresent(e -> 
    System.out.println(e.getKey() + ": " + e.getValue()));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 32: Employees with same salary <a id="problem-32"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Objects & Employees

**Problem Statement:**
> Group employees who share the same salary.

**Solution:**
```java
Map<Double, List<String>> sameSalary = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getSalary,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));

// Filter groups with more than one employee:
sameSalary.entrySet().stream()
    .filter(e -> e.getValue().size() > 1)
    .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
```

[↑ Back to Table of Contents](#table-of-contents)

---

### Maps & Collections <a id="maps-collections"></a>

#### Problem 33: Invert a Map <a id="problem-33"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Maps & Collections

**Problem Statement:**
> Given Map<String, Integer>, invert it to Map<Integer, String>.

**Solution:**
```java
Map<String, Integer> original = Map.of("a", 1, "b", 2, "c", 3);
Map<Integer, String> inverted = original.entrySet().stream()
    .collect(Collectors.toMap(
        Map.Entry::getValue,
        Map.Entry::getKey
    ));
// {1=a, 2=b, 3=c}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 34: Sort Map by value <a id="problem-34"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Maps & Collections

**Problem Statement:**
> Sort a Map<String, Integer> by its values.

**Solution:**
```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 85); scores.put("Bob", 72); scores.put("Charlie", 93);

// Sort by value ascending:
Map<String, Integer> sorted = scores.entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
    ));
// {Bob=72, Alice=85, Charlie=93}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 35: Merge two maps <a id="problem-35"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Maps & Collections

**Problem Statement:**
> Merge two maps; if same key exists, sum the values.

**Solution:**
```java
Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
Map<String, Integer> map2 = Map.of("b", 10, "c", 20, "d", 4);

Map<String, Integer> merged = Stream.of(map1, map2)
    .flatMap(m -> m.entrySet().stream())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        Integer::sum
    ));
// {a=1, b=12, c=23, d=4}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 36: Find most frequent element <a id="problem-36"></a>

- **Difficulty:** 🟡 Medium
- **Category:** Maps & Collections

**Problem Statement:**
> Find the most frequently occurring element in a list.

**Solution:**
```java
List<String> items = Arrays.asList("apple","banana","apple","cherry","banana","apple");
Optional<String> mostFrequent = items.stream()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    .entrySet().stream()
    .max(Map.Entry.comparingByValue())
    .map(Map.Entry::getKey);
mostFrequent.ifPresent(System.out::println); // apple
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 37: Group anagrams together <a id="problem-37"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Maps & Collections

**Problem Statement:**
> Given a list of strings, group words that are anagrams of each other.

**Solution:**
```java
List<String> words = Arrays.asList("eat","tea","tan","ate","nat","bat");
Map<String, List<String>> anagrams = words.stream()
    .collect(Collectors.groupingBy(word -> {
        char[] chars = word.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }));
// {aet=[eat, tea, ate], ant=[tan, nat], abt=[bat]}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 38: Count character frequency in a list of strings <a id="problem-38"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Maps & Collections

**Problem Statement:**
> Given a list of strings, count total frequency of each character across all strings.

**Solution:**
```java
List<String> words = Arrays.asList("hello", "world", "java");
Map<Character, Long> charFreq = words.stream()
    .flatMapToInt(String::chars)
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
// {h=1, e=1, l=3, o=2, w=1, r=1, d=1, j=1, a=2, v=1}
```

[↑ Back to Table of Contents](#table-of-contents)

---

### Advanced <a id="advanced"></a>

#### Problem 39: Custom Collector — Sum of squares <a id="problem-39"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Use reduce to calculate sum of squares of a list.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
int sumOfSquares = nums.stream()
    .reduce(0, (acc, n) -> acc + n * n);
// 55 (1+4+9+16+25)

// Or using mapToInt:
int sos = nums.stream()
    .mapToInt(n -> n * n)
    .sum();
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 40: Nested grouping <a id="problem-40"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Group employees first by department, then by gender within each department.

**Solution:**
```java
Map<String, Map<String, List<Employee>>> grouped = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.groupingBy(Employee::getGender)
    ));
// {IT={Male=[...], Female=[...]}, HR={Male=[...], Female=[...]}}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 41: Collectors.toMap with duplicate key handling <a id="problem-41"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Convert a list to a Map<name, salary>, handling duplicate names by keeping higher salary.

**Solution:**
```java
List<Employee> employees = ...; // may have duplicate names
Map<String, Double> empSalaryMap = employees.stream()
    .collect(Collectors.toMap(
        Employee::getName,
        Employee::getSalary,
        Math::max  // merge function: keep higher salary
    ));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 42: Stream with index (enumerate) <a id="problem-42"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Print each element with its index (like Python's enumerate).

**Solution:**
```java
List<String> items = Arrays.asList("apple","banana","cherry");
IntStream.range(0, items.size())
    .forEach(i -> System.out.println(i + ": " + items.get(i)));
// 0: apple, 1: banana, 2: cherry

// Collect to map with index as key:
Map<Integer, String> indexed = IntStream.range(0, items.size())
    .boxed()
    .collect(Collectors.toMap(i -> i, items::get));
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 43: Transpose a matrix <a id="problem-43"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Transpose a 2D list (matrix) using streams.

**Solution:**
```java
int[][] matrix = {{1,2,3},{4,5,6},{7,8,9}};
int rows = matrix.length, cols = matrix[0].length;
int[][] transposed = IntStream.range(0, cols)
    .mapToObj(col -> IntStream.range(0, rows)
                              .map(row -> matrix[row][col])
                              .toArray())
    .toArray(int[][]::new);
// {{1,4,7},{2,5,8},{3,6,9}}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 44: Sliding window maximum <a id="problem-44"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Find max of every k-sized sliding window in a list.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1,3,-1,-3,5,3,6,7);
int k = 3;
List<Integer> result = IntStream.range(0, nums.size() - k + 1)
    .mapToObj(i -> nums.subList(i, i + k))
    .map(window -> window.stream().mapToInt(Integer::intValue).max().getAsInt())
    .collect(Collectors.toList());
// [3, 3, 5, 5, 6, 7]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 45: Longest consecutive sequence length <a id="problem-45"></a>

- **Difficulty:** 🔴 Hard
- **Category:** Advanced

**Problem Statement:**
> Find the length of the longest consecutive sequence in an unsorted list.

**Solution:**
```java
List<Integer> nums = Arrays.asList(100, 4, 200, 1, 3, 2);
Set<Integer> numSet = new HashSet<>(nums);
int maxLen = numSet.stream()
    .filter(n -> !numSet.contains(n - 1)) // start of sequence
    .mapToInt(start -> {
        int len = 0;
        while (numSet.contains(start + len)) len++;
        return len;
    })
    .max().orElse(0);
// 4 (sequence: 1,2,3,4)
```

[↑ Back to Table of Contents](#table-of-contents)

---

### EPAM Favorites <a id="epam-favorites"></a>

#### Problem 46: List of Strings → Map<length, List<String>> <a id="problem-46"></a>

- **Difficulty:** 🟡 Medium
- **Category:** EPAM Favorites

**Problem Statement:**
> Group strings by their length.

**Solution:**
```java
List<String> words = Arrays.asList("hi","hey","hello","java","is","fun");
Map<Integer, List<String>> byLength = words.stream()
    .collect(Collectors.groupingBy(String::length));
// {2=[hi, is], 3=[hey, fun], 4=[java], 5=[hello]}
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 47: Integer list to comma-separated string <a id="problem-47"></a>

- **Difficulty:** 🟡 Medium
- **Category:** EPAM Favorites

**Problem Statement:**
> Convert List<Integer> to a comma-separated String.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
String result = nums.stream()
    .map(String::valueOf)
    .collect(Collectors.joining(", "));
// "1, 2, 3, 4, 5"
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 48: Find all pairs summing to target <a id="problem-48"></a>

- **Difficulty:** 🔴 Hard
- **Category:** EPAM Favorites

**Problem Statement:**
> Find all pairs in a list that sum to a target value.

**Solution:**
```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
int target = 8;
Set<List<Integer>> pairs = IntStream.range(0, nums.size())
    .boxed()
    .flatMap(i -> IntStream.range(i+1, nums.size())
        .filter(j -> nums.get(i) + nums.get(j) == target)
        .mapToObj(j -> Arrays.asList(nums.get(i), nums.get(j))))
    .collect(Collectors.toSet());
// [[1,7], [2,6], [3,5]]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 49: Statistics summary <a id="problem-49"></a>

- **Difficulty:** 🔴 Hard
- **Category:** EPAM Favorites

**Problem Statement:**
> Get count, sum, min, max, and average from a list in one pass.

**Solution:**
```java
List<Integer> nums = Arrays.asList(5, 2, 8, 1, 9, 3, 7, 4, 6);
IntSummaryStatistics stats = nums.stream()
    .mapToInt(Integer::intValue)
    .summaryStatistics();

System.out.println("Count: " + stats.getCount());  // 9
System.out.println("Sum: "   + stats.getSum());    // 45
System.out.println("Min: "   + stats.getMin());    // 1
System.out.println("Max: "   + stats.getMax());    // 9
System.out.println("Avg: "   + stats.getAverage()); // 5.0
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 50: Chained Optional operations <a id="problem-50"></a>

- **Difficulty:** 🔴 Hard
- **Category:** EPAM Favorites

**Problem Statement:**
> Given a user lookup that might return null, safely extract and transform their city name to uppercase.

**Solution:**
```java
// userService.findById may return null
// user.getAddress() may return null
// address.getCity() may return null

Optional<String> cityUpper = Optional.ofNullable(userService.findById(id))
    .map(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase);

String city = cityUpper.orElse("UNKNOWN");

// This replaces deeply nested null checks:
// if (user != null && user.getAddress() != null && 
//     user.getAddress().getCity() != null) { ... }
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 51: Infinite stream with limit <a id="problem-51"></a>

- **Difficulty:** 🔴 Hard
- **Category:** EPAM Favorites

**Problem Statement:**
> Generate an infinite stream of random numbers and collect first 5 that are > 50.

**Solution:**
```java
List<Integer> result = Stream.generate(() -> new Random().nextInt(100))
    .filter(n -> n > 50)
    .limit(5)
    .collect(Collectors.toList());

// Generate natural numbers and find first 5 multiples of 7:
List<Integer> mult7 = Stream.iterate(1, n -> n + 1)
    .filter(n -> n % 7 == 0)
    .limit(5)
    .collect(Collectors.toList());
// [7, 14, 21, 28, 35]
```

[↑ Back to Table of Contents](#table-of-contents)

#### Problem 52: Collect to immutable map with transformation <a id="problem-52"></a>

- **Difficulty:** 🔴 Hard
- **Category:** EPAM Favorites

**Problem Statement:**
> Given list of employees, create Map<name, String> where value is 'dept-salary' string.

**Solution:**
```java
Map<String, String> empInfo = employees.stream()
    .collect(Collectors.toMap(
        Employee::getName,
        e -> e.getDept() + "-" + e.getSalary()
    ));
// {"Alice" -> "IT-75000.0", "Bob" -> "HR-60000.0", ...}

// Using Collectors.toUnmodifiableMap (Java 10+):
Map<String, String> immutable = employees.stream()
    .collect(Collectors.toUnmodifiableMap(
        Employee::getName,
        e -> e.getDept() + "-" + e.getSalary()
    ));
```

[↑ Back to Table of Contents](#table-of-contents)

---
