# 🧠 Google L5-Only DSA Problem List (25 Problems)

> Goal: **Reasoning > memorization > coding speed**

---

## 🟦 SECTION 1: Graph + State Explosion (Very High Yield)

### 1. Cheapest Flight With K Stops

(LC 787)

**Tests:**

* BFS vs Dijkstra vs Bellman-Ford
* State modeling `(node, stops)`
* Pruning logic

**L5 Follow-ups:**

* Exactly K stops
* Return path, not cost
* Negative edges
* Dynamic graph updates

---

### 2. Shortest Path With Fuel Constraint

(Variant of LC 882 / Google onsite classic)

**Tests:**

* Graph + DP state
* PriorityQueue correctness

**State:** `(city, fuelLeft)`

---

### 3. Shortest Path With One-Time Coupon

(Google favorite)

**Tests:**

* Multi-state Dijkstra
* Decision modeling

**State:** `(node, couponUsed)`

---

### 4. Network Delay Time – Extended

(LC 743, but NOT basic)

**Tests:**

* Multiple sources
* Incremental updates
* Failure scenarios

---

### 5. Detect Negative Cycle in Currency Exchange

(Arbitrage)

**Tests:**

* Bellman-Ford reasoning
* Floating point issues
* Graph modeling

---

## 🟦 SECTION 2: DP + Optimization (Where L4s Usually Break)

### 6. Burst Balloons

(LC 312)

**Tests:**

* Interval DP
* Recurrence explanation

**Follow-ups:**

* Can we reduce space?
* Why greedy fails?

---

### 7. Word Break II

(LC 140)

**Tests:**

* DP + DFS
* Memoization vs brute force

---

### 8. Longest Increasing Path in Matrix

(LC 329)

**Tests:**

* Graph interpretation of DP
* Topological sorting logic

---

### 9. Paint House III

(LC 1473)

**Tests:**

* 3D DP
* Constraint explosion handling

---

### 10. DP on Tree – Maximum Independent Set

(Google classic)

**Tests:**

* Tree DP
* Parent-child dependency reasoning

---

## 🟦 SECTION 3: Advanced Data Structures

### 11. LRU Cache with TTL

(Your version → push to L5)

**Tests:**

* Design + correctness
* Lazy vs eager eviction

---

### 12. Sliding Window Median

(LC 480)

**Tests:**

* Two heaps
* Rebalancing invariants

---

### 13. Design Hit Counter (Rolling Window)

(Google frequent)

**Tests:**

* Time-based eviction
* Space optimization

---

### 14. Autocomplete System

(LC 642)

**Tests:**

* Trie + heap
* Incremental updates

---

### 15. Design Rate Limiter (Distributed Follow-up)

**Tests:**

* Algorithm → system boundary
* Accuracy vs scalability

---

## 🟦 SECTION 4: Greedy + Proof (Google Loves “Why This Works”)

### 16. Minimum Number of Refueling Stops

(LC 871)

**Tests:**

* Greedy correctness proof
* Heap usage justification

---

### 17. Task Scheduler with Cooling Period

(LC 621)

**Tests:**

* Math + greedy
* Edge cases

---

### 18. Interval Scheduling with Resources

(Meeting Rooms II+)

**Tests:**

* Sweep line
* Heap logic

---

### 19. Split Array Largest Sum

(LC 410)

**Tests:**

* Binary search on answer
* Feasibility function

---

### 20. Job Assignment with Profit

(LC 1235)

**Tests:**

* DP + binary search
* Sorting strategy

---

## 🟦 SECTION 5: Union-Find, Geometry, and “Surprise” Problems

### 21. Accounts Merge

(LC 721)

**Tests:**

* Union-Find modeling
* Component merging

---

### 22. Number of Islands II

(LC 305)

**Tests:**

* Dynamic Union-Find
* Online queries

---

### 23. Skyline Problem

(LC 218)

**Tests:**

* Sweep line mastery
* Multiset / TreeMap usage

---

### 24. K Closest Points to Origin – Extended

(LC 973)

**Tests:**

* Heap vs QuickSelect
* Streaming variant

---

### 25. Robot Room Cleaner

(LC 489)

**Tests:**

* DFS with no grid
* Directional reasoning

---

# 🎯 How We’ll Use This List (Important)

We **will NOT just “solve” these**.

For **each problem**, I will:

1. Act as the **Google interviewer**
2. Interrupt you with:

   * Constraint changes
   * “Why not X?”
   * “What breaks?”
3. Force:

   * Trade-off discussion
   * Edge-case handling
   * Complexity defense

---

## ✅ Next Step (Tell Me This)

Pick **ONE problem** from the list above to start with.
(I recommend **#1, #3, or #11** for maximum signal.)

Once you pick:

* I’ll open the interview
* You talk, I interrupt 😄
* We’ll go **full L5 depth**

Your move 👇
