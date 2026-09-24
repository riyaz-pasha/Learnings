## ✅ `Set` Interface (Java)

The `Set` is an interface, and common implementations include:

### 1. **HashSet**

* Backed by a **HashMap** internally.
* **Not ordered**.

| Operation            | Time Complexity                           |
| -------------------- | ----------------------------------------- |
| `add(E e)`           | O(1) average, O(n) worst-case (rehashing) |
| `remove(Object o)`   | O(1) average                              |
| `contains(Object o)` | O(1) average                              |
| `size()`             | O(1)                                      |
| Iteration            | O(n)                                      |

---

### 2. **LinkedHashSet**

* Maintains **insertion order**.
* Slightly slower than `HashSet`.

| Operation            | Time Complexity |
| -------------------- | --------------- |
| `add(E e)`           | O(1) average    |
| `remove(Object o)`   | O(1)            |
| `contains(Object o)` | O(1)            |
| Iteration            | O(n)            |

---

### 3. **TreeSet**

* Backed by a **Red-Black Tree (Self-balancing BST)**.
* Maintains **natural ordering** or **custom comparator order**.

| Operation                      | Time Complexity |
| ------------------------------ | --------------- |
| `add(E e)`                     | O(log n)        |
| `remove(Object o)`             | O(log n)        |
| `contains(Object o)`           | O(log n)        |
| `first()` / `last()`           | O(log n)        |
| `ceiling(E e)` / `floor(E e)`  | O(log n)        |
| `higher(E e)` / `lower(E e)`   | O(log n)        |
| Iteration (in-order traversal) | O(n)            |

---

## ✅ Summary Table

| Operation Type   | HashSet | LinkedHashSet | TreeSet  |
| ---------------- | ------- | ------------- | -------- |
| `add()`          | O(1)    | O(1)          | O(log n) |
| `remove()`       | O(1)    | O(1)          | O(log n) |
| `contains()`     | O(1)    | O(1)          | O(log n) |
| Maintains order? | ❌       | ✅ Insertion   | ✅ Sorted |
| Iteration Order  | Random  | Insertion     | Sorted   |
| Space Usage      | O(n)    | O(n)          | O(n)     |

---

