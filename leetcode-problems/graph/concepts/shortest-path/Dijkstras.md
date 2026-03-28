# ✅ Version 1: Dijkstra **WITHOUT** `visited[]`

### (Lazy Dijkstra – **Most Common in Interviews & LeetCode**)

```java
// Dijkstra's Algorithm (Lazy Version - WITHOUT visited array)
// Time Complexity: O((V + E) * log V)
// Space Complexity: O(V + E)
//
// Key idea:
// - A node may enter the priority queue multiple times
// - We only process the entry that matches the current shortest distance
// - Outdated entries are ignored

public int networkDelayTime(int[][] times, int n, int k) {

    // Adjacency list: u -> {v, weight}
    Map<Integer, List<int[]>> graph = new HashMap<>();
    for (int[] time : times) {
        graph.computeIfAbsent(time[0], x -> new ArrayList<>())
             .add(new int[]{time[1], time[2]});
    }

    // dist[i] = shortest distance from source k to node i
    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;

    // Min-heap storing {distance, node}
    PriorityQueue<int[]> pq =
            new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
    pq.offer(new int[]{0, k});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int currDist = curr[0];
        int node = curr[1];

        // If this entry is outdated, skip it
        if (currDist > dist[node]) continue;

        // Relax all outgoing edges
        if (graph.containsKey(node)) {
            for (int[] edge : graph.get(node)) {
                int nextNode = edge[0];
                int weight = edge[1];
                int newDist = currDist + weight;

                if (newDist < dist[nextNode]) {
                    dist[nextNode] = newDist;
                    pq.offer(new int[]{newDist, nextNode});
                }
            }
        }
    }

    // Find the maximum time among all reachable nodes
    int maxTime = 0;
    for (int i = 1; i <= n; i++) {
        if (dist[i] == Integer.MAX_VALUE) return -1;
        maxTime = Math.max(maxTime, dist[i]);
    }

    return maxTime;
}
```

### 🧠 Mental Model

> “Process a node **only if** this is the shortest distance we’ve seen so far.”

---

# ✅ Version 2: Dijkstra **WITH** `visited[]`

### (Eager Dijkstra – **Classic Textbook Version**)

```java
// Dijkstra's Algorithm (Eager Version - WITH visited array)
// Time Complexity: O((V + E) * log V)
// Space Complexity: O(V + E)
//
// Key idea:
// - Once a node is popped from the priority queue the first time,
//   its shortest distance is finalized
// - Each node is processed exactly once

public int networkDelayTime(int[][] times, int n, int k) {

    // Adjacency list: u -> {v, weight}
    Map<Integer, List<int[]>> graph = new HashMap<>();
    for (int[] time : times) {
        graph.computeIfAbsent(time[0], x -> new ArrayList<>())
             .add(new int[]{time[1], time[2]});
    }

    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;

    boolean[] visited = new boolean[n + 1];

    PriorityQueue<int[]> pq =
            new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
    pq.offer(new int[]{0, k});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int node = curr[1];

        // If already finalized, skip
        if (visited[node]) continue;
        visited[node] = true;

        // Relax edges
        if (graph.containsKey(node)) {
            for (int[] edge : graph.get(node)) {
                int nextNode = edge[0];
                int weight = edge[1];

                if (!visited[nextNode] &&
                    dist[node] + weight < dist[nextNode]) {
                    dist[nextNode] = dist[node] + weight;
                    pq.offer(new int[]{dist[nextNode], nextNode});
                }
            }
        }
    }

    int maxTime = 0;
    for (int i = 1; i <= n; i++) {
        if (dist[i] == Integer.MAX_VALUE) return -1;
        maxTime = Math.max(maxTime, dist[i]);
    }

    return maxTime;
}
```

### 🧠 Mental Model

> “The first time a node comes out of the PQ, its shortest path is fixed forever.”

---

# 🚫 What NOT to Do (Common Bug)

```java
// ❌ DON'T do this
if (visited[node]) continue;
if (currDist > dist[node]) continue;
```

Using **both** is redundant and confusing.

---

# ⭐ Which One Should YOU Use?

| Situation               | Best Choice           |
| ----------------------- | --------------------- |
| LeetCode                | ✅ Without `visited[]` |
| Interviews              | ✅ Without `visited[]` |
| Teaching / theory       | With `visited[]`      |
| Competitive programming | Without `visited[]`   |

👉 **Your original solution is already the preferred one.**

---

# 🎯 Interview One-Liner (Memorize This)

> “Dijkstra can be implemented either with a visited array or by lazily skipping outdated heap entries. Both are correct.”



---

Short answer: **Yes, the asymptotic time complexity is the same** for both versions of Dijkstra’s (with a `visited` array vs without), **but the number of heap operations differs**, which affects constants in practice.

Let’s break it down clearly 👇

---

# 🔹 Two Variants of Dijkstra

## ✅ 1. With `visited[]` array

### Idea

* Once a node is **finalized (shortest distance found)** → mark `visited[node] = true`
* Never process it again

### Key behavior

* Each node is processed **exactly once**
* Edges are relaxed once per node

---

## ✅ 2. Without `visited[]` array (Lazy Dijkstra)

### Idea

* Allow multiple entries in the priority queue
* Skip outdated entries using:

```java
if (currentDist > dist[node]) continue;
```

### Key behavior

* Same node can be pushed multiple times into PQ
* Only process when it's the **best (latest) distance**

---

# 🔹 Code Difference (Core Logic)

### With visited

```java
if (visited[node]) continue;
visited[node] = true;
```

### Without visited (lazy)

```java
if (currentDist > dist[node]) continue;
```

---

# 🔹 Time Complexity Analysis

Let:

* `V` = vertices
* `E` = edges

---

## ✅ With visited[]

### Operations

* Each node processed once → `V`
* Each edge relaxed once → `E`
* PQ operations → `log V`

### Complexity

```
O((V + E) log V)
```

---

## ✅ Without visited[] (Lazy version)

### What changes?

* A node can be inserted **multiple times** into PQ
* Worst case: every edge relaxation pushes into PQ

### Operations

* PQ insertions → up to `E`
* PQ removals → up to `E`

### Complexity

```
O(E log V)
```

---

# 🔥 Final Comparison

| Version           | Time Complexity  | Notes                        |
| ----------------- | ---------------- | ---------------------------- |
| With visited[]    | O((V + E) log V) | Cleaner, fewer PQ ops        |
| Without visited[] | O(E log V)       | More PQ inserts, but simpler |

---

# ⚠️ Important Insight (INTERVIEW GOLD)

👉 Since in most graphs:

```
E ≥ V
```

So:

```
O((V + E) log V) ≈ O(E log V)
```

👉 **That’s why both are considered SAME complexity asymptotically**

---

# 🔹 Practical Difference

## With visited[]

* Less heap operations
* Slightly faster in practice
* More controlled

## Without visited[]

* Simpler code
* No need for visited array
* Slight overhead due to duplicate entries

---

# 🔹 Example to Understand Difference

Graph:

```
0 → 1 (10)
0 → 1 (5)
```

### Without visited:

* (10,1) pushed
* (5,1) pushed
* First pop → (5,1) ✅
* Later pop → (10,1) ❌ skipped

👉 Extra work happens

---

# 🔹 When to Use What?

| Scenario             | Recommendation                      |
| -------------------- | ----------------------------------- |
| Interviews           | ✅ Without visited (clean + common)  |
| Performance-critical | ✅ With visited                      |
| Competitive coding   | ✅ Without visited (faster to write) |

---

# 🧠 Final Takeaway

* **Both have same Big-O:** `O(E log V)`
* Difference is in:

  * Constant factors
  * Number of PQ operations

👉 Think of it as:

> With visited = "strict processing"
> Without visited = "lazy but safe processing"

---

If you want next level clarity, I can:

* Show **step-by-step dry run of both versions**
* OR give **Java implementations side-by-side**
* OR connect this to **why decrease-key is avoided in Java PQ**

Just tell me 👍
