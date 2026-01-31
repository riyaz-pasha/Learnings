## 1️⃣ What they are (core idea)

### **TreeMap**

* A **sorted map** (key → value)
* Backed by a **Red-Black Tree** (self-balancing BST)
* Maintains **keys in sorted order**

### **PriorityQueue**

* A **heap-based** data structure
* Orders elements by **priority** (min or max)
* Only guarantees **top element** is accessible efficiently

---

## 2️⃣ Ordering & Access Guarantees

| Feature          | TreeMap                 | PriorityQueue            |
| ---------------- | ----------------------- | ------------------------ |
| Ordering         | Fully sorted by key     | Only top element ordered |
| Access min       | `firstKey()` → O(log n) | `peek()` → O(1)          |
| Access max       | `lastKey()` → O(log n)  | O(n)                     |
| Remove arbitrary | O(log n)                | O(n)                     |
| Iterate sorted   | Yes                     | No                       |

👉 **Key insight:**

> **TreeMap maintains global order**
> **PriorityQueue maintains partial order**

---

## 3️⃣ Time Complexity (very important)

### **TreeMap**

| Operation       | Time     |
| --------------- | -------- |
| Insert          | O(log n) |
| Delete          | O(log n) |
| Search          | O(log n) |
| Min / Max       | O(log n) |
| Floor / Ceiling | O(log n) |

### **PriorityQueue**

| Operation        | Time     |
| ---------------- | -------- |
| Insert           | O(log n) |
| Remove top       | O(log n) |
| Peek top         | O(1)     |
| Remove arbitrary | O(n)     |
| Search           | O(n)     |

---

## 4️⃣ Functional Capabilities (big differentiator)

### TreeMap supports:

* `floorKey()`, `ceilingKey()`
* `lowerKey()`, `higherKey()`
* Range queries (`subMap`, `headMap`, `tailMap`)
* Duplicate handling via counts (multiset behavior)

### PriorityQueue supports:

* Fast access to **min / max**
* Efficient **greedy algorithms**
* No range or order navigation

---

## 5️⃣ Memory & Structure

| Aspect                | TreeMap                       | PriorityQueue       |
| --------------------- | ----------------------------- | ------------------- |
| Backing structure     | Red-Black Tree                | Binary Heap         |
| Memory overhead       | Higher (tree nodes, pointers) | Lower               |
| Stability of ordering | Stable                        | Unstable beyond top |

---

## 6️⃣ When to Use Which (MOST IMPORTANT)

### ✅ Use **TreeMap** when:

* You need **sorted data**
* You need **range queries**
* You need **both min & max**
* You need to **remove arbitrary elements**
* You need **frequency counting with ordering**

📌 **Common problems**

* Skyline problem
* Sweep line algorithms
* Calendar booking
* Sliding window with ordered elements
* Interval overlap tracking

```java
TreeMap<Integer, Integer> map = new TreeMap<>();
map.put(x, map.getOrDefault(x, 0) + 1);
map.firstKey(); // min
map.lastKey();  // max
```

---

### ✅ Use **PriorityQueue** when:

* You only care about **min or max**
* You want **fast greedy decisions**
* You don’t need ordering beyond the top
* You don’t need deletion of random elements

📌 **Common problems**

* Dijkstra
* Prim’s / Kruskal’s
* Top K elements
* Merge K sorted lists
* Meeting rooms (heap version)

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(10);
pq.poll(); // smallest element
```

---

## 7️⃣ Side-by-Side Example (Interview Gold)

### **Meeting Rooms II**

**PriorityQueue approach**

* Track only earliest ending meeting
* Faster, simpler

**TreeMap approach**

* Track all ongoing meetings with counts
* Useful when you need **extra control / queries**

👉 **If question asks “minimum” → PriorityQueue**
👉 **If question asks “exact ordering / removal / range” → TreeMap**

---

## 8️⃣ One-line Interview Summary

> **PriorityQueue** is for **fast min/max extraction**
> **TreeMap** is for **ordered data with navigation & control**

---
