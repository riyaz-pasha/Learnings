## ❓ How many ways can we define a descending-order PriorityQueue?

👉 **There are 4 common / interview-acceptable ways**.

---

## ✅ 1. Using `Collections.reverseOrder()` (MOST COMMON)

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>(Collections.reverseOrder());
```

✔ Clean
✔ Readable
✔ Interview-friendly

---

## ✅ 2. Using Lambda Comparator

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>((a, b) -> b - a);
```

⚠️ Risk of **integer overflow** when values are large.

---

## ✅ 3. Using `Integer.compare()` (SAFE Lambda)

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>((a, b) -> Integer.compare(b, a));
```

✔ Overflow-safe
✔ Preferred over `b - a`

---

## ✅ 4. Using Custom Comparator Class

```java
class MaxHeapComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer a, Integer b) {
        return b.compareTo(a);
    }
}

PriorityQueue<Integer> pq =
        new PriorityQueue<>(new MaxHeapComparator());
```

✔ Useful when comparator logic is reused
✔ Verbose but clear

---

## 🚫 What You **CANNOT** Do

❌ This does **NOT** work:

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(true);
```

❌ No built-in `MaxPriorityQueue` in Java.

---

## 🧠 Internal Behavior (Important)

* `PriorityQueue` in Java is **min-heap by default**
* Comparator **reverses comparison**
* Heap structure stays the same; ordering logic changes

---

## ⏱ Complexity (Same for all)

| Operation | Time     |
| --------- | -------- |
| add()     | O(log n) |
| poll()    | O(log n) |
| peek()    | O(1)     |

---

## 🎯 Interview Recommendation (What to Say)

> “Java’s PriorityQueue is a min-heap by default.
> To create a max-heap, we provide a reverse comparator, most commonly using `Collections.reverseOrder()`.”

---

## 📝 Quick Summary

| Method                       | Safe | Clean | Preferred |
| ---------------------------- | ---- | ----- | --------- |
| `Collections.reverseOrder()` | ✅    | ✅     | ⭐⭐⭐       |
| `b - a`                      | ❌    | ✅     | ❌         |
| `Integer.compare(b,a)`       | ✅    | ✅     | ⭐⭐        |
| Custom Comparator            | ✅    | ❌     | ⭐         |

---

---

---

## 🔹 How many ways to define an **ascending order (min-heap)** `PriorityQueue`?

👉 **4 common ways** (mirroring the max-heap cases).

---

## ✅ 1. Default Constructor (MOST COMMON ⭐)

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
```

✔ Natural ordering
✔ Ascending order
✔ Best interview answer

---

## ✅ 2. Explicit Natural Order Comparator

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>(Comparator.naturalOrder());
```

✔ Same as default
✔ Useful for clarity / generics

---

## ✅ 3. Lambda Comparator (Safe)

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>((a, b) -> Integer.compare(a, b));
```

✔ Explicit
✔ Overflow-safe

---

## ✅ 4. Custom Comparator Class

```java
class MinHeapComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer a, Integer b) {
        return a.compareTo(b);
    }
}

PriorityQueue<Integer> pq =
        new PriorityQueue<>(new MinHeapComparator());
```

✔ Reusable logic
✔ Verbose but valid

---

## 🚫 What NOT to Do

❌ Risky (overflow-prone):

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>((a, b) -> a - b);
```

---

## 🧠 Important Notes (Interview Gold)

* `PriorityQueue` **defaults to min-heap**
* Comparator defines **ordering**, not structure
* Heap is **NOT sorted**, only `peek()` is guaranteed minimum

---

## ⏱ Time Complexity (All Cases)

| Operation | Time     |
| --------- | -------- |
| add()     | O(log n) |
| poll()    | O(log n) |
| peek()    | O(1)     |

---

## 🔁 Min-Heap vs Max-Heap (Quick Recall)

| Heap Type | Definition                                        |
| --------- | ------------------------------------------------- |
| Min-Heap  | `new PriorityQueue<>()`                           |
| Max-Heap  | `new PriorityQueue<>(Collections.reverseOrder())` |

---

## 🎯 Interview One-Liner

> “Java PriorityQueue is a min-heap by default using natural ordering.
> We can explicitly control ordering by supplying a comparator.”

---

Perfect — this is a **very common interview use-case** 👍
Let’s do **Employee salary heap** properly, step by step, in **Java**.

---

## 👤 Employee Class

```java
class Employee {
    int id;
    String name;
    int salary;

    Employee(int id, String name, int salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }

    @Override
    public String toString() {
        return name + " (" + salary + ")";
    }
}
```

---

# 🔹 1️⃣ Min-Heap (Ascending by Salary)

👉 Lowest salary has **highest priority**

### Using Lambda (Recommended)

```java
PriorityQueue<Employee> minHeap =
        new PriorityQueue<>(
            (e1, e2) -> Integer.compare(e1.salary, e2.salary)
        );
```

### Usage

```java
minHeap.add(new Employee(1, "Alice", 50000));
minHeap.add(new Employee(2, "Bob", 70000));
minHeap.add(new Employee(3, "Charlie", 60000));

System.out.println(minHeap.poll()); // Alice (50000)
```

---

# 🔹 2️⃣ Max-Heap (Descending by Salary)

👉 Highest salary has **highest priority**

### Using Lambda

```java
PriorityQueue<Employee> maxHeap =
        new PriorityQueue<>(
            (e1, e2) -> Integer.compare(e2.salary, e1.salary)
        );
```

### Using `Collections.reverseOrder()` ❌ (WHY NOT?)

```java
// This will NOT work for custom objects
PriorityQueue<Employee> pq =
        new PriorityQueue<>(Collections.reverseOrder());
```

❗ `reverseOrder()` works **only** when `Employee` implements `Comparable`.

---

# 🔹 3️⃣ Using Comparable (Natural Ordering)

### Employee implements `Comparable`

```java
class Employee implements Comparable<Employee> {
    int id;
    String name;
    int salary;

    Employee(int id, String name, int salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.salary, other.salary); // Min-heap
    }

    @Override
    public String toString() {
        return name + " (" + salary + ")";
    }
}
```

### Min-Heap (Default)

```java
PriorityQueue<Employee> pq = new PriorityQueue<>();
```

### Max-Heap Using Reverse Order

```java
PriorityQueue<Employee> pq =
        new PriorityQueue<>(Collections.reverseOrder());
```

---

# 🔹 4️⃣ Multi-Level Sorting (Salary → Name)

### Min-Heap: Salary ↑, Name ↑

```java
PriorityQueue<Employee> pq =
        new PriorityQueue<>(
            Comparator
                .comparingInt((Employee e) -> e.salary)
                .thenComparing(e -> e.name)
        );
```

---

# 🔹 5️⃣ Real Interview Problem: Top-K Highest Paid Employees

### Use **Min-Heap of size K**

```java
PriorityQueue<Employee> pq =
        new PriorityQueue<>(
            (a, b) -> Integer.compare(a.salary, b.salary)
        );

for (Employee e : employees) {
    pq.add(e);
    if (pq.size() > k) {
        pq.poll(); // remove smallest salary
    }
}
```

✔ Time: `O(n log k)`
✔ Space: `O(k)`

---

## 🧠 Interview Decision Guide

| Requirement        | Use                  |
| ------------------ | -------------------- |
| One ordering       | `Comparable`         |
| Multiple orderings | `Comparator`         |
| Max-heap           | Comparator / reverse |
| Top-K              | Min-heap             |

---

## 🎯 Interview One-Liner

> “For custom objects, PriorityQueue requires either Comparable or a Comparator.
> I prefer Comparator when ordering logic may change.”

---
