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

