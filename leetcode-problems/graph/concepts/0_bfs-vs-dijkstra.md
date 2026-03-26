# BFS vs Dijkstra's — Complete Guide for Shortest Path Problems

---

## 1. The One Rule That Decides Everything

> **If all edges have equal (or no) weight → BFS**
> **If edges have different weights → Dijkstra's**

That's the entire decision. Everything below is the *why*, the *how*, and how not to freeze in an interview.

---

## 2. Mental Model: What Each Algorithm Is Doing

### BFS — "Explore by hop count"

BFS expands nodes **level by level**. It implicitly treats every edge as cost = 1.
The first time BFS reaches a node, it is **guaranteed** to be the shortest path in hops.

```
Queue (FIFO): [ A ]
Level 0: A
Level 1: B, C          (1 hop from A)
Level 2: D, E, F       (2 hops from A)
```

### Dijkstra's — "Explore by minimum cumulative cost"

Dijkstra's always expands the **globally cheapest unfinalized node** next.
It uses a **min-heap** to always pick the next best candidate.

```
Min-Heap (priority queue):
Pop cheapest → relax its neighbors → push updated costs back in
```

---

## 3. Why BFS Breaks on Weighted Graphs

Consider this graph:

```
        1           10
  A ─────────► B ────────► D
  │                         ▲
  │ 1           1           │
  └───────────► C ──────────┘
```

| Path         | Hops | Actual Cost |
|--------------|------|-------------|
| A → B → D   |  2   | 1 + 10 = **11** |
| A → C → D   |  2   | 1 + 1  = **2**  |

BFS reaches D via `A→B→D` first (both are 2-hop paths, BFS explores them equally).
It declares cost = 2 hops and moves on. **The actual cost is 11 — completely wrong.**

Dijkstra's correctly picks `A→C→D` because it tracks cumulative cost, not hop count.

---

## 4. Dijkstra's — How the dist[] Array Works

### Initialization

```java
int[] dist = new int[n];
Arrays.fill(dist, Integer.MAX_VALUE);  // ∞ for all nodes
dist[src] = 0;                          // source costs 0
```

### The Relaxation Condition

For every neighbor `v` of current node `u`:

```
if dist[u] + weight(u, v) < dist[v]:
    dist[v] = dist[u] + weight(u, v)
    push (dist[v], v) into min-heap
```

This is called **relaxation** — you're asking "can I reach v cheaper through u?"

### The Stale-Entry Guard (Most Critical Part)

The heap may contain outdated entries. When you pop `(cost, node)`:

```java
if (cost > dist[node]) continue;   // stale — skip it
```

**Why does this happen?** When you find a shorter path to a node, you push a new entry into the heap. But the old, more expensive entry is still there — you can't efficiently remove it from a heap. So you just detect and skip it on pop.

---

## 5. Step-by-Step dist[] Evolution

Graph: `A→B(4), A→C(1), C→B(1), B→D(2), C→D(8)`

```
Initial:
  dist = [A:0, B:∞, C:∞, D:∞]
  heap = [(0,A)]

Step 1 — Pop (0,A):
  A→B: 0+4=4 < ∞  → dist[B]=4, push (4,B)
  A→C: 0+1=1 < ∞  → dist[C]=1, push (1,C)
  dist = [A:0, B:4, C:1, D:∞]
  heap = [(1,C), (4,B)]

Step 2 — Pop (1,C):
  C→B: 1+1=2 < 4  → dist[B]=2, push (2,B)  ← relaxation!
  C→D: 1+8=9 < ∞  → dist[D]=9, push (9,D)
  dist = [A:0, B:2, C:1, D:9]
  heap = [(2,B), (4,B)←stale, (9,D)]

Step 3 — Pop (2,B):
  B→D: 2+2=4 < 9  → dist[D]=4, push (4,D)  ← relaxation again!
  dist = [A:0, B:2, C:1, D:4]
  heap = [(4,B)←stale, (4,D), (9,D)←stale]

Step 4 — Pop (4,B):
  4 > dist[B]=2 → STALE, skip!
  heap = [(4,D), (9,D)←stale]

Step 5 — Pop (4,D):
  4 == dist[D]=4 → valid, no outgoing edges
  D is finalized with cost 4
  heap = [(9,D)←stale]

Step 6 — Pop (9,D):
  9 > dist[D]=4 → STALE, skip!
  heap = []  ← done!

Final: dist = [A:0, B:2, C:1, D:4]
Shortest path to D = A → C → B → D, cost = 4
```

---

## 6. Java Implementation

### BFS — Unweighted Shortest Path

```java
import java.util.*;

public class BFS {

    // Returns shortest distance from src to all nodes
    // graph[u] = list of neighbors of u
    public static int[] bfs(List<List<Integer>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, -1);           // -1 = unvisited
        dist[src] = 0;

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(src);

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (int v : graph.get(u)) {
                if (dist[v] == -1) {     // not yet visited
                    dist[v] = dist[u] + 1;
                    queue.offer(v);
                }
            }
        }
        return dist;
    }

    public static void main(String[] args) {
        int n = 4; // nodes: 0=A, 1=B, 2=C, 3=D
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        graph.get(0).add(1); // A→B
        graph.get(0).add(2); // A→C
        graph.get(1).add(3); // B→D
        graph.get(2).add(3); // C→D

        int[] dist = bfs(graph, 0, n);
        System.out.println(Arrays.toString(dist));
        // [0, 1, 1, 2]  — all in hops, weights ignored
    }
}
```

---

### Dijkstra's — Weighted Shortest Path

```java
import java.util.*;

public class Dijkstra {

    // graph[u] = list of int[]{v, weight}
    public static int[] dijkstra(List<List<int[]>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // Min-heap: int[]{cost, node}
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, src});

        while (!heap.isEmpty()) {
            int[] curr = heap.poll();
            int cost = curr[0];
            int u    = curr[1];

            // ── KEY GUARD: skip stale entries ──────────────
            if (cost > dist[u]) continue;
            // ───────────────────────────────────────────────

            for (int[] edge : graph.get(u)) {
                int v = edge[0];
                int w = edge[1];

                int newCost = dist[u] + w;
                if (newCost < dist[v]) {
                    dist[v] = newCost;          // update dist array
                    heap.offer(new int[]{newCost, v});  // push new entry
                }
            }
        }
        return dist;
    }

    public static void main(String[] args) {
        int n = 4; // 0=A, 1=B, 2=C, 3=D
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        graph.get(0).add(new int[]{1, 4}); // A→B, w=4
        graph.get(0).add(new int[]{2, 1}); // A→C, w=1
        graph.get(2).add(new int[]{1, 1}); // C→B, w=1
        graph.get(1).add(new int[]{3, 2}); // B→D, w=2
        graph.get(2).add(new int[]{3, 8}); // C→D, w=8

        int[] dist = dijkstra(graph, 0, n);
        System.out.println(Arrays.toString(dist));
        // [0, 2, 1, 4]
    }
}
```

---

### Dijkstra's with Path Reconstruction

```java
import java.util.*;

public class DijkstraWithPath {

    public static void dijkstra(List<List<int[]>> graph, int src, int n) {
        int[] dist   = new int[n];
        int[] parent = new int[n];           // track the path
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        dist[src] = 0;

        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, src});

        while (!heap.isEmpty()) {
            int[] curr = heap.poll();
            int cost = curr[0], u = curr[1];

            if (cost > dist[u]) continue;

            for (int[] edge : graph.get(u)) {
                int v = edge[0], w = edge[1];
                int newCost = dist[u] + w;

                if (newCost < dist[v]) {
                    dist[v]   = newCost;
                    parent[v] = u;           // record how we got here
                    heap.offer(new int[]{newCost, v});
                }
            }
        }

        // Reconstruct path to node 3 (D)
        printPath(parent, 3);
        System.out.println("Cost: " + dist[3]);
    }

    private static void printPath(int[] parent, int node) {
        if (node == -1) return;
        printPath(parent, parent[node]);
        System.out.print(node + " ");
    }
}
```

---

## 7. Complexity Comparison

| Aspect              | BFS                | Dijkstra's (min-heap)  |
|---------------------|--------------------|------------------------|
| Time complexity     | O(V + E)           | O((V + E) log V)       |
| Space complexity    | O(V)               | O(V + E)               |
| Data structure      | Queue (FIFO)       | PriorityQueue (min)    |
| Edge weights        | All equal (or 1)   | Non-negative only      |
| First-visit = optimal? | ✅ Yes          | ✅ Yes (when popped)   |
| Works with negatives? | N/A (no weights) | ❌ No → use Bellman-Ford |

---

## 8. Common Mistakes in the dist[] Array

### Mistake 1 — Forgetting the stale-entry guard

```java
// WRONG — processes nodes multiple times with wrong costs
while (!heap.isEmpty()) {
    int[] curr = heap.poll();
    int u = curr[1];
    // no stale check — heap bloats, wrong results possible
    for (int[] edge : graph.get(u)) { ... }
}

// CORRECT
if (cost > dist[u]) continue;  // add this line
```

### Mistake 2 — Pushing to heap BEFORE updating dist

```java
// WRONG — other threads/pops might read stale dist[v]
heap.offer(new int[]{newCost, v});
dist[v] = newCost;              // too late

// CORRECT — update dist first, then push
dist[v] = newCost;
heap.offer(new int[]{newCost, v});
```

### Mistake 3 — Using Integer.MAX_VALUE and adding to it

```java
// WRONG — Integer.MAX_VALUE + w overflows to negative
int newCost = dist[u] + w;     // dist[u] = MAX_VALUE → overflow!

// CORRECT — use a safe large value, or guard before adding
Arrays.fill(dist, (int) 1e9);  // 10^9 is safe to add to
// or:
if (dist[u] == Integer.MAX_VALUE) continue;
```

### Mistake 4 — Using Dijkstra's with negative weights

```java
// Graph: A→B(-1), A→C(3), B→C(1)
// Dijkstra finalizes C at cost 3 when popped,
// but A→B→C = -1+1 = 0 is cheaper — missed!
// → Use Bellman-Ford for negative weights
```

---

## 9. Variants and When to Use Them

```
Unweighted graph              → BFS
Weighted, non-negative        → Dijkstra's
Weighted, with negatives      → Bellman-Ford  O(V·E)
Very dense graph              → Dijkstra with adjacency matrix O(V²)
0-1 weighted graph            → 0-1 BFS with Deque (push to front if w=0)
Shortest path in a DAG        → Topological sort + relaxation O(V+E)
All-pairs shortest path       → Floyd-Warshall O(V³)
```

### 0-1 BFS — a special trick

When edge weights are only 0 or 1 (e.g. "free" or "cost-1" moves):

```java
// Use Deque instead of PriorityQueue
Deque<Integer> deque = new ArrayDeque<>();
// weight=0 edge → push to FRONT (it's still the same "level")
// weight=1 edge → push to BACK  (costs one more)
deque.addFirst(v);   // w=0
deque.addLast(v);    // w=1
```

This runs in O(V + E) — faster than Dijkstra's for this special case.

---

## 10. Interview — How to Not Get Confused

### The 3-Second Decision Framework

Ask yourself these in order:

```
1. Does the graph have weights?
     No  → BFS
     Yes → Continue

2. Are all weights equal?
     Yes → BFS (or Dijkstra, but BFS is simpler and faster)
     No  → Continue

3. Are there negative weights?
     No  → Dijkstra's
     Yes → Bellman-Ford (or SPFA)
```

### Interview-Specific Traps

**Trap 1 — "Minimum cost path" with all equal costs**

> You see edge weights of 1 everywhere.
> Many candidates reach for Dijkstra's because the word "cost" appears.
> BFS is sufficient and simpler. Say so.

**Trap 2 — "Shortest path" in a grid**

> Grid problems with uniform move cost = BFS.
> Grid problems where some cells are "free" and others cost 1 = 0-1 BFS with Deque.
> Grid problems with varying terrain costs = Dijkstra's.

**Trap 3 — Forgetting to handle disconnected nodes**

```java
// After Dijkstra's, check if target is reachable
if (dist[target] == (int) 1e9) {
    System.out.println("No path exists");
}
```

**Trap 4 — Not clarifying if the graph is directed or undirected**

Always ask: "Are edges one-way or bidirectional?" This changes how you build the adjacency list.

```java
// Undirected: add edge in both directions
graph.get(u).add(new int[]{v, w});
graph.get(v).add(new int[]{u, w});  // ← don't forget this

// Directed: add only one direction
graph.get(u).add(new int[]{v, w});
```

**Trap 5 — Confusing "shortest path" with "minimum spanning tree"**

> Dijkstra's = shortest path from one source to all nodes.
> Prim's / Kruskal's = minimum spanning tree (connects all nodes, minimum total weight).
> They use similar structures but answer completely different questions.

### What to Say Out Loud in an Interview

When given a graph problem, walk through this:

```
"Let me first check the constraints —
 are edges weighted? If yes, are weights uniform?
 Any negative weights?

 Since [reason], I'll use [BFS / Dijkstra's].

 I'll maintain a dist array initialized to infinity,
 dist[source] = 0, and use a [queue / min-heap].

 For Dijkstra's, the key thing is the stale-entry guard
 when popping from the heap."
```

This signals to the interviewer that you understand the decision, not just the code.

### Quick Phrases to Remember

| Situation | What to say |
|-----------|-------------|
| Unweighted, fewest hops | "BFS naturally gives me this in O(V+E)" |
| Weighted, non-negative | "Dijkstra's with a min-heap, O((V+E) log V)" |
| Negative weights | "Need Bellman-Ford here, Dijkstra's can't handle negatives" |
| Asked why not Dijkstra for unweighted | "BFS is simpler and faster — O(V+E) vs O((V+E) log V)" |
| Stale entries | "I guard against this with `if cost > dist[u]: skip`" |

---

## 11. Full Java Template (Interview-Ready)

```java
import java.util.*;

/**
 * Single-source shortest path on a weighted directed graph.
 * Node IDs: 0 to n-1
 * graph[u] = [ {v, weight}, ... ]
 */
public class ShortestPath {

    // ── BFS: for unweighted graphs ──────────────────────────────────────────
    public static int[] bfs(List<List<Integer>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, -1);
        dist[src] = 0;
        Queue<Integer> q = new LinkedList<>();
        q.offer(src);
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : graph.get(u)) {
                if (dist[v] == -1) {
                    dist[v] = dist[u] + 1;
                    q.offer(v);
                }
            }
        }
        return dist;
    }

    // ── Dijkstra's: for non-negative weighted graphs ────────────────────────
    public static int[] dijkstra(List<List<int[]>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, (int) 1e9);
        dist[src] = 0;

        // min-heap on cost
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, src});

        while (!heap.isEmpty()) {
            int[] top  = heap.poll();
            int cost   = top[0];
            int u      = top[1];

            if (cost > dist[u]) continue;      // stale entry guard

            for (int[] edge : graph.get(u)) {
                int v       = edge[0];
                int w       = edge[1];
                int newCost = dist[u] + w;

                if (newCost < dist[v]) {
                    dist[v] = newCost;
                    heap.offer(new int[]{newCost, v});
                }
            }
        }
        return dist;
    }

    // ── Helper: build adjacency list ────────────────────────────────────────
    public static List<List<int[]>> buildGraph(int n, int[][] edges, boolean directed) {
        List<List<int[]>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (int[] e : edges) {
            graph.get(e[0]).add(new int[]{e[1], e[2]});
            if (!directed) graph.get(e[1]).add(new int[]{e[0], e[2]});
        }
        return graph;
    }
}
```

---

## 12. Summary

```
All edges equal weight?
  └── YES → BFS
        Data structure : Queue (FIFO)
        Time           : O(V + E)
        dist init      : dist[src] = 0, rest = -1 (unvisited)
        First visit    : guaranteed optimal

  └── NO  → Dijkstra's
        Data structure : PriorityQueue<int[]> min-heap on cost
        Time           : O((V + E) log V)
        dist init      : dist[src] = 0, rest = 1e9 (infinity)
        Key guard      : if (cost > dist[u]) continue;
        Constraint     : weights must be non-negative
        Negative edges → Bellman-Ford instead
```
