# Prim’s Algorithm – Time & Space Complexity (Min-Heap / Priority Queue)

Understanding the complexity of **Prim’s algorithm** is crucial for evaluating its efficiency.
Below is a **cleaned-up and well-structured** explanation for a Java implementation that uses a **Min-Priority Queue (Min-Heap)**.

---

## ⏰ Time Complexity Analysis

Let:

* `V` = number of vertices (nodes)
* `E` = number of edges

The overall complexity is dominated by **Priority Queue operations** (`offer` and `poll`).

---

### 1️⃣ Initialization

* Initialize `inMST` array → **O(V)**
* Insert edges connected to the starting node into the Priority Queue

  * In the worst case, the starting node can have degree `O(V)`
  * Each insertion costs `O(log Q)` where `Q ≤ E`

**Time:**

```
O(V log V)
```

---

### 2️⃣ Main Loop (`while (!pq.isEmpty())`)

The loop continues until the MST has `V - 1` edges.

#### a) Extract-Min (`pq.poll()`)

* Each edge inserted into the PQ can be removed at most once
* At most `E` extractions
* Each extraction costs `O(log E)`

**Total extraction cost:**

```
O(E log E)
```

---

#### b) Insertions (`pq.offer()`)

* Each edge is inserted into the PQ **at most once**

  * When its source vertex is added to the MST
* At most `E` insertions
* Each insertion costs `O(log E)`

**Total insertion cost:**

```
O(E log E)
```

---

### 3️⃣ Final Time Complexity

Combining all steps:

```
O(V log V) + O(E log E) + O(E log E)
```

Since for a connected graph `E ≥ V - 1`, the dominant term is:

```
✅ Time Complexity = O(E log E)
```

---

### 🔎 Why `O(E log V)` Is Also Correct

* In the worst case, `E ≤ V²`
* Therefore:

  ```
  log E = log(V²) = 2 log V = O(log V)
  ```

So the time complexity is often written as:

```
✅ O(E log V)
```

Both are correct; **`O(E log V)` is the standard interview-friendly form**.

---

## 💾 Space Complexity Analysis

Space usage comes from the graph representation and auxiliary data structures.

---

### Data Structures Used

| Structure                | Space      |
| ------------------------ | ---------- |
| Adjacency List (`graph`) | `O(V + E)` |
| `inMST` boolean array    | `O(V)`     |
| `mstEdges` (result list) | `O(V)`     |
| Priority Queue (`pq`)    | `O(E)`     |

---

### Final Space Complexity

```
O(V + E) + O(V) + O(V) + O(E)
```

Simplifies to:

```
✅ Space Complexity = O(V + E)
```

---

## ✅ Final Summary

| Metric    | Complexity   |
| --------- | ------------ |
| **Time**  | `O(E log V)` |
| **Space** | `O(V + E)`   |

