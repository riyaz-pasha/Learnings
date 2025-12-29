Below is a **1-page, interview-ready revision sheet** covering the **entire thread** — **min heap & max heap**, primitives + custom objects, **all ways of defining PriorityQueue in Java**.

You can **copy-paste and revise before interviews** 👌
(Plain text, concise, no fluff)

---

# 🔥 Java Heap / PriorityQueue — 1-Page Revision Notes

---

## 1️⃣ Heap Basics

* Java `PriorityQueue` = **Binary Heap**
* **Default** → **Min-Heap**
* Heap is **NOT sorted**
* Only `peek()` is guaranteed min / max
* Backed by **array**

Index rules:

```
parent = (i - 1) / 2
left   = 2*i + 1
right  = 2*i + 2
```

---

## 2️⃣ Min-Heap (Ascending Order)

### ✅ Way 1 — Default (BEST ⭐)

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
```

### ✅ Way 2 — Natural Order

```java
PriorityQueue<Integer> pq =
    new PriorityQueue<>(Comparator.naturalOrder());
```

### ✅ Way 3 — Lambda (Safe)

```java
PriorityQueue<Integer> pq =
    new PriorityQueue<>((a, b) -> Integer.compare(a, b));
```

### ❌ Avoid (Overflow Risk)

```java
(a, b) -> a - b
```

---

## 3️⃣ Max-Heap (Descending Order)

### ✅ Way 1 — reverseOrder (BEST ⭐)

```java
PriorityQueue<Integer> pq =
    new PriorityQueue<>(Collections.reverseOrder());
```

### ✅ Way 2 — Lambda (Safe)

```java
PriorityQueue<Integer> pq =
    new PriorityQueue<>((a, b) -> Integer.compare(b, a));
```

### ❌ Avoid

```java
(a, b) -> b - a
```

---

## 4️⃣ Custom Objects (Employee Salary Heap)

### Employee

```java
class Employee {
    int id;
    String name;
    int salary;
}
```

---

### 🔹 Min-Heap (Salary ↑)

```java
PriorityQueue<Employee> pq =
    new PriorityQueue<>(
        (e1, e2) -> Integer.compare(e1.salary, e2.salary)
    );
```

---

### 🔹 Max-Heap (Salary ↓)

```java
PriorityQueue<Employee> pq =
    new PriorityQueue<>(
        (e1, e2) -> Integer.compare(e2.salary, e1.salary)
    );
```

---

## 5️⃣ Using Comparable (Natural Ordering)

```java
class Employee implements Comparable<Employee> {
    int salary;

    public int compareTo(Employee e) {
        return Integer.compare(this.salary, e.salary);
    }
}
```

### Min-Heap

```java
PriorityQueue<Employee> pq = new PriorityQueue<>();
```

### Max-Heap

```java
PriorityQueue<Employee> pq =
    new PriorityQueue<>(Collections.reverseOrder());
```

⚠️ `reverseOrder()` works **only if Comparable is implemented**

---

## 6️⃣ Multi-Level Sorting (Interview Favorite)

### Salary ↑ → Name ↑

```java
PriorityQueue<Employee> pq =
    new PriorityQueue<>(
        Comparator
            .comparingInt((Employee e) -> e.salary)
            .thenComparing(e -> e.name)
    );
```

---

## 7️⃣ Top-K Pattern (VERY IMPORTANT)

### Top-K Highest Elements → **Min-Heap of size K**

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();

for (int x : nums) {
    pq.add(x);
    if (pq.size() > k) pq.poll();
}
```

Time: `O(n log k)`
Space: `O(k)`

---

## 8️⃣ Comparable vs Comparator

| Feature         | Comparable   | Comparator |
| --------------- | ------------ | ---------- |
| Logic location  | Inside class | Outside    |
| Multiple orders | ❌            | ✅          |
| reverseOrder()  | ✅            | ❌          |
| Preferred       | ❌            | ✅          |

---

## 9️⃣ Time Complexity

| Operation | Time     |
| --------- | -------- |
| add       | O(log n) |
| poll      | O(log n) |
| peek      | O(1)     |

---

## 🎯 Interview One-Liners

* “PriorityQueue is a min-heap by default.”
* “Max-heap is created using reverse comparator.”
* “For custom objects, use Comparator for flexibility.”
* “Top-K problems use a heap of size K.”

---
