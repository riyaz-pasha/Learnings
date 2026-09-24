---

# 1️⃣ Heap-Based Order Book (Individual Orders in Heaps)

### Data Structures

* **Max-Heap** for BUY orders
* **Min-Heap** for SELL orders
* Each heap stores **individual orders**

Let:

* **N** = total number of active orders
* **M** = number of matched trades

---

## ⏱ Time Complexity

| Operation          | Complexity   | Explanation     |
| ------------------ | ------------ | --------------- |
| Insert order       | **O(log N)** | Heap insertion  |
| Best price lookup  | **O(1)**     | Heap root       |
| Match (per trade)  | **O(log N)** | Heap poll       |
| Partial fill       | **O(1)**     | Quantity update |
| FIFO at same price | ❌            | Not guaranteed  |

**Total matching cost:**

```
O(M log N)
```

---

## 🧠 Space Complexity

| Component | Space |
| --------- | ----- |
| Buy heap  | O(N)  |
| Sell heap | O(N)  |
| Orders    | O(N)  |

**Total Space:**

```
O(N)
```

---

## ⚠️ Limitations

* FIFO at same price is not natural
* High heap churn under load
* Not price-level optimized

---

# 2️⃣ Price-Level Grouped Order Book (TreeMap + FIFO Queues)

### Data Structures

* **TreeMap<Price, Queue<Order>>** for BUY (descending)
* **TreeMap<Price, Queue<Order>>** for SELL (ascending)

Let:

* **N** = total active orders
* **P** = number of distinct price levels
* **Q** = average orders per price level
* **M** = number of matched trades

---

## ⏱ Time Complexity

| Operation         | Complexity   | Explanation                 |
| ----------------- | ------------ | --------------------------- |
| Insert order      | **O(log P)** | TreeMap insert              |
| Best price lookup | **O(log P)** | firstKey()                  |
| Match (per trade) | **O(log P)** | Remove price level if empty |
| FIFO within price | **O(1)**     | Queue poll                  |
| Partial fill      | **O(1)**     | Quantity update             |

**Total matching cost:**

```
O(M log P)
```

Since **P ≪ N** in real systems → faster than heap-based.

---

## 🧠 Space Complexity

| Component    | Space |
| ------------ | ----- |
| TreeMaps     | O(P)  |
| Order queues | O(N)  |
| Orders       | O(N)  |

**Total Space:**

```
O(N + P)
≈ O(N)
```

---

# 3️⃣ Direct Comparison (Interview Gold)

| Aspect              | Heap-Based | Price-Level Grouped |
| ------------------- | ---------- | ------------------- |
| Insert order        | O(log N)   | **O(log P)**        |
| Match order         | O(log N)   | **O(log P)**        |
| FIFO per price      | ❌          | ✅                   |
| Real exchange model | ❌          | ✅                   |
| High-volume scaling | ⚠️         | ✅                   |
| Space               | O(N)       | O(N + P)            |

---

# 4️⃣ What to Say Out Loud (Memorize)

> “Heap-based matching works but operates on individual orders, leading to O(log N) operations.
> Price-level grouping reduces this to O(log P), preserves FIFO, and scales better since P is much smaller than N.”

That sentence alone is **strong-hire level**.

---
