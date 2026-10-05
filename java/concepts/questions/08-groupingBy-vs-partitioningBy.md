# Java: `groupingBy()` vs `partitioningBy()`

Both are **Collectors** used with Java Streams to group elements, but they answer different questions.

> **`groupingBy()` → “Group by some key.”**
> **`partitioningBy()` → “Split into two groups based on true/false.”**

---

## 1. `groupingBy()`

Use `groupingBy()` when you have **multiple possible groups**.

### Example

Suppose we have employees:

```java
record Employee(String name, String department) {}
```

```java
List<Employee> employees = List.of(
    new Employee("Alice", "IT"),
    new Employee("Bob", "HR"),
    new Employee("Charlie", "IT"),
    new Employee("David", "Finance"),
    new Employee("Eve", "HR")
);
```

We want:

```text
IT      -> [Alice, Charlie]
HR      -> [Bob, Eve]
Finance -> [David]
```

Use:

```java
Map<String, List<Employee>> result =
    employees.stream()
        .collect(Collectors.groupingBy(Employee::department));
```

### Result

```text
{
    IT=[Alice, Charlie],
    HR=[Bob, Eve],
    Finance=[David]
}
```

The classifier:

```java
Employee::department
```

determines **which key/group** each element belongs to.

---

# 2. `partitioningBy()`

Use `partitioningBy()` when you have a **boolean condition**.

For example:

> Separate employees whose salary is greater than ₹100,000 from everyone else.

```java
Map<Boolean, List<Employee>> result =
    employees.stream()
        .collect(Collectors.partitioningBy(
            employee -> employee.salary() > 100_000
        ));
```

Conceptually:

```text
true  -> employees satisfying condition
false -> employees not satisfying condition
```

So the result is always based on exactly **two logical partitions**:

```text
{
    true  = [...],
    false = [...]
}
```

---

# 3. The fundamental difference

Think of it this way:

```text
                    Stream
                       |
              ┌────────┴────────┐
              |                 |
        groupingBy()       partitioningBy()
              |                 |
        "What category?"   "Does it satisfy
                            this condition?"
              |                 |
       Multiple groups      Two groups
              |                 |
       Map<K, List<T>>    Map<Boolean, List<T>>
```

### Example

```java
groupingBy(Employee::department)
```

asks:

> "Which department?"

Possible answers:

```text
IT
HR
Finance
Sales
Marketing
...
```

Whereas:

```java
partitioningBy(Employee::isActive)
```

asks:

> "Is the employee active?"

Possible answers:

```text
true
false
```

---

# 4. Side-by-side comparison

| Feature          | `groupingBy()`    | `partitioningBy()`           |
| ---------------- | ----------------- | ---------------------------- |
| Purpose          | Group by a key    | Split by a condition         |
| Classifier       | `Function<T, K>`  | `Predicate<T>`               |
| Number of groups | Any number        | Two                          |
| Result           | `Map<K, List<T>>` | `Map<Boolean, List<T>>`      |
| Key type         | Any type          | `Boolean`                    |
| Typical question | "Which category?" | "Does it satisfy condition?" |

---

# 5. Simple example with numbers

Suppose:

```java
List<Integer> numbers =
    List.of(1, 2, 3, 4, 5, 6, 7);
```

## `groupingBy()`

Group numbers by their remainder when divided by 3:

```java
Map<Integer, List<Integer>> result =
    numbers.stream()
        .collect(Collectors.groupingBy(n -> n % 3));
```

Result:

```text
0 -> [3, 6]
1 -> [1, 4, 7]
2 -> [2, 5]
```

There are **three groups**.

---

## `partitioningBy()`

Separate even and odd:

```java
Map<Boolean, List<Integer>> result =
    numbers.stream()
        .collect(Collectors.partitioningBy(n -> n % 2 == 0));
```

Result:

```text
true  -> [2, 4, 6]
false -> [1, 3, 5, 7]
```

Exactly **two groups**.

---

# 6. Important: `partitioningBy()` is specialized for Boolean

You could technically write:

```java
numbers.stream()
    .collect(Collectors.groupingBy(n -> n % 2 == 0));
```

This also produces:

```text
true  -> [2, 4, 6]
false -> [1, 3, 5, 7]
```

So you might ask:

> Why have `partitioningBy()` at all?

Because the intent is clearer.

### `groupingBy()`

```java
groupingBy(n -> n % 2 == 0)
```

means:

> "Group according to this key."

### `partitioningBy()`

```java
partitioningBy(n -> n % 2 == 0)
```

means:

> "Partition into elements that satisfy this condition and those that don't."

The second communicates the intent much better.

---

# 7. Both support downstream collectors

This is where things become more interesting.

## `groupingBy()` + `counting()`

Count employees per department:

```java
Map<String, Long> counts =
    employees.stream()
        .collect(Collectors.groupingBy(
            Employee::department,
            Collectors.counting()
        ));
```

Result:

```text
IT      -> 2
HR      -> 2
Finance -> 1
```

---

## `partitioningBy()` + `counting()`

Count employees who satisfy a condition:

```java
Map<Boolean, Long> counts =
    employees.stream()
        .collect(Collectors.partitioningBy(
            Employee::isActive,
            Collectors.counting()
        ));
```

Result:

```text
true  -> 4
false -> 1
```

---

# 8. Another important difference: nested grouping

`groupingBy()` can naturally create multiple levels of grouping.

For example:

> Group employees by department, then by role.

```java
Map<String, Map<String, List<Employee>>> result =
    employees.stream()
        .collect(Collectors.groupingBy(
            Employee::department,
            Collectors.groupingBy(Employee::role)
        ));
```

Conceptually:

```text
IT
 ├── Developer
 │    ├── Alice
 │    └── Charlie
 │
 └── Manager
      └── John

HR
 └── Recruiter
      └── Bob
```

This is one of the major strengths of `groupingBy()`.

---

# 9. `partitioningBy()` can also have a downstream collector

For example:

```java
Map<Boolean, Set<String>> result =
    employees.stream()
        .collect(Collectors.partitioningBy(
            Employee::isActive,
            Collectors.mapping(
                Employee::name,
                Collectors.toSet()
            )
        ));
```

Now:

```text
true  -> [Alice, Charlie, ...]
false -> [Bob, ...]
```

So the structure is:

```text
partitioningBy(predicate, downstreamCollector)
```

Just like:

```text
groupingBy(classifier, downstreamCollector)
```

---

# 10. A useful interview mental model

Remember these three questions:

### Question 1

> "I want to categorize objects based on some property."

Use:

```java
groupingBy()
```

Example:

```java
groupingBy(Employee::department)
```

---

### Question 2

> "I want to divide objects into those that satisfy a condition and those that don't."

Use:

```java
partitioningBy()
```

Example:

```java
partitioningBy(Employee::isActive)
```

---

### Question 3

> "I want exactly two groups, but they're not naturally represented by a boolean condition."

Then `groupingBy()` may be more appropriate.

For example:

```text
JUNIOR
SENIOR
```

You could use:

```java
groupingBy(Employee::level)
```

rather than forcing it into `partitioningBy()`.

---

# 11. Interview answer

If an interviewer asks:

> **What is the difference between `groupingBy()` and `partitioningBy()`?**

A strong concise answer is:

> `groupingBy()` groups stream elements based on a classifier function and can produce any number of groups, returning a `Map<K, ...>`. `partitioningBy()` is specifically for dividing elements based on a boolean predicate into two partitions, `true` and `false`, returning a `Map<Boolean, ...>`. Both support downstream collectors such as `counting()`, `mapping()`, and `summarizingInt()`.

### One-line memory trick

```text
groupingBy     → many groups → Map<K, ...>
partitioningBy → two groups  → Map<Boolean, ...>
```

The next closely related concept worth learning is **`groupingBy()` vs `groupingByConcurrent()`**, because that introduces how grouping behaves with **parallel streams and concurrency**.

