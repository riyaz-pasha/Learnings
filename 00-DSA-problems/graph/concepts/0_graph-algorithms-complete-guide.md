# Graph Algorithms — Complete Guide

> **How to read this guide:** Start with Section 1 (representation) and Section 2 (the master decision tree). Then jump directly to the algorithm section you need. The interview section at the end ties everything together.

---

## Table of Contents

1. [Graph Fundamentals & Representation](#1-graph-fundamentals--representation)
2. [Master Decision Tree](#2-master-decision-tree)
3. [BFS — Breadth-First Search](#3-bfs--breadth-first-search)
4. [DFS — Depth-First Search](#4-dfs--depth-first-search)
5. [Dijkstra's Algorithm](#5-dijkstras-algorithm)
6. [Bellman-Ford Algorithm](#6-bellman-ford-algorithm)
7. [Floyd-Warshall Algorithm](#7-floyd-warshall-algorithm)
8. [Topological Sort](#8-topological-sort)
9. [Cycle Detection](#9-cycle-detection)
10. [Minimum Spanning Tree — Prim's & Kruskal's](#10-minimum-spanning-tree--prims--kruskals)
11. [Union-Find (Disjoint Set Union)](#11-union-find-disjoint-set-union)
12. [0-1 BFS](#12-0-1-bfs)
13. [Algorithm Comparison Matrix](#13-algorithm-comparison-matrix)
14. [Common Confusions Resolved](#14-common-confusions-resolved)
15. [Interview Playbook](#15-interview-playbook)
16. [Problem Pattern Recognition](#16-problem-pattern-recognition)

---

## 1. Graph Fundamentals & Representation

### Core Terminology

```
Vertex (Node)    — a point in the graph
Edge             — a connection between two vertices
Directed graph   — edges have direction (A→B ≠ B→A)
Undirected graph — edges go both ways (A-B = B-A)
Weighted graph   — edges have a cost/distance
Cyclic graph     — contains at least one cycle
DAG              — Directed Acyclic Graph (directed + no cycles)
Connected graph  — every node is reachable from every other node
Sparse graph     — few edges relative to nodes (E ≈ V)
Dense graph      — many edges relative to nodes (E ≈ V²)
```

### Adjacency List vs Adjacency Matrix

```
Adjacency List  — array of lists, each list holds neighbors
Adjacency Matrix — 2D array, matrix[u][v] = weight (0 if no edge)
```

| Operation             | Adj. List         | Adj. Matrix  |
|-----------------------|-------------------|--------------|
| Space                 | O(V + E)          | O(V²)        |
| Check edge (u,v)      | O(degree(u))      | O(1)         |
| Get all neighbors of u| O(degree(u))      | O(V)         |
| Add edge              | O(1)              | O(1)         |
| Best for              | Sparse graphs     | Dense graphs |

**Rule:** Use adjacency list by default. Switch to matrix only when E ≈ V² or you need O(1) edge lookups constantly.

### Java: Building the Graph

```java
// ── Unweighted, directed ─────────────────────────────────────────
List<List<Integer>> graph = new ArrayList<>();
for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
graph.get(u).add(v);                  // directed: u → v only
graph.get(u).add(v); graph.get(v).add(u); // undirected: both ways

// ── Weighted, directed ───────────────────────────────────────────
List<List<int[]>> graph = new ArrayList<>();
for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
graph.get(u).add(new int[]{v, weight});

// ── From edge list (common in interview problems) ─────────────────
// edges = [[u, v, w], ...]
List<List<int[]>> buildGraph(int n, int[][] edges, boolean directed) {
    List<List<int[]>> g = new ArrayList<>();
    for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    for (int[] e : edges) {
        g.get(e[0]).add(new int[]{e[1], e[2]});
        if (!directed) g.get(e[1]).add(new int[]{e[0], e[2]});
    }
    return g;
}

// ── Adjacency matrix ─────────────────────────────────────────────
int[][] matrix = new int[n][n];       // default 0 = no edge
matrix[u][v] = weight;
if (!directed) matrix[v][u] = weight;
```

---

## 2. Master Decision Tree

Use this before writing a single line of code.

```
What does the problem ask?
│
├── "Is there a path between A and B?"
│     → DFS or BFS (both work, DFS is simpler)
│
├── "Shortest path (fewest hops / unweighted)"
│     → BFS
│
├── "Shortest path (minimum cost, non-negative weights)"
│     → Dijkstra's
│
├── "Shortest path (negative weights exist)"
│     → Bellman-Ford
│
├── "Shortest path between ALL pairs of nodes"
│     → Floyd-Warshall
│
├── "All paths / explore everything / find connected components"
│     → DFS
│
├── "Ordering tasks that depend on each other"
│     → Topological Sort (Kahn's BFS or DFS-based)
│
├── "Does a cycle exist?"
│     ├── Directed graph  → DFS with 3-color marking
│     └── Undirected graph → DFS with parent tracking, or Union-Find
│
├── "Connect all nodes with minimum total cost (not shortest path)"
│     → Minimum Spanning Tree
│         ├── Dense graph  → Prim's (O(V²) or O(E log V) with heap)
│         └── Sparse graph → Kruskal's (O(E log E))
│
├── "Group nodes / dynamic connectivity / number of components"
│     → Union-Find (DSU)
│
└── "Edge weights are only 0 or 1"
      → 0-1 BFS with Deque (faster than Dijkstra's for this case)
```

---

## 3. BFS — Breadth-First Search

### What it does
Explores the graph **level by level** from a source node. Guarantees the shortest path in terms of **number of hops** in an unweighted graph.

### When to use
- Shortest path in unweighted graphs
- Level-order traversal
- Finding all nodes at distance k
- Checking bipartiteness
- Multi-source shortest path (start BFS from multiple sources simultaneously)

### When NOT to use
- Weighted graphs where shortest cost ≠ fewest hops → use Dijkstra's
- Need to explore all paths → use DFS
- Memory is tight and graph is deep → DFS uses less stack space

### How it works

```
Start → push source into Queue, mark visited
Loop:
  Pop node u from front of Queue
  For each neighbor v of u:
    If v not visited:
      mark v visited
      dist[v] = dist[u] + 1
      push v into Queue
```

### Java Implementation

```java
import java.util.*;

public class BFS {

    // Single-source shortest path (unweighted)
    public static int[] bfs(List<List<Integer>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, -1);
        dist[src] = 0;

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(src);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : graph.get(u)) {
                if (dist[v] == -1) {          // not yet visited
                    dist[v] = dist[u] + 1;
                    queue.offer(v);
                }
            }
        }
        return dist;   // dist[i] = -1 means unreachable
    }

    // Multi-source BFS — useful for "nearest X" problems
    // Example: nearest 0 in a grid, nearest rotten orange
    public static int[] multiBFS(List<List<Integer>> graph, List<Integer> sources, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, -1);

        Queue<Integer> queue = new LinkedList<>();
        for (int src : sources) {
            dist[src] = 0;
            queue.offer(src);          // seed all sources at distance 0
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : graph.get(u)) {
                if (dist[v] == -1) {
                    dist[v] = dist[u] + 1;
                    queue.offer(v);
                }
            }
        }
        return dist;
    }

    // BFS on a grid (extremely common in interviews)
    public static int bfsGrid(int[][] grid, int sr, int sc, int er, int ec) {
        int rows = grid.length, cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{sr, sc, 0});   // {row, col, dist}
        visited[sr][sc] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1], d = curr[2];

            if (r == er && c == ec) return d;

            for (int[] dir : dirs) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && !visited[nr][nc] && grid[nr][nc] != 1) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc, d + 1});
                }
            }
        }
        return -1;   // unreachable
    }
}
```

### Complexity
- Time: O(V + E)
- Space: O(V) for the queue and visited array

---

## 4. DFS — Depth-First Search

### What it does
Explores as **deep as possible** before backtracking. Uses a stack (implicit via recursion, or explicit).

### When to use
- Detecting cycles
- Topological sort
- Finding connected components
- Path existence (any path, not shortest)
- Flood fill
- Solving mazes (any solution, not shortest)
- Finding all paths between two nodes
- Strongly connected components (Kosaraju's / Tarjan's)

### When NOT to use
- Shortest path in unweighted graphs → BFS
- Shortest path in weighted graphs → Dijkstra's
- Graph is very deep and stack overflow is a risk → use iterative DFS

### How it works

```
Recursive:
  dfs(u):
    mark u as visited
    for each neighbor v:
      if v not visited:
        dfs(v)

Iterative:
  push src onto Stack
  while stack not empty:
    pop u
    if u visited: skip
    mark u visited
    push all unvisited neighbors of u
```

### Java Implementation

```java
import java.util.*;

public class DFS {

    // Recursive DFS — find all reachable nodes from src
    public static void dfsRecursive(List<List<Integer>> graph,
                                    int u, boolean[] visited) {
        visited[u] = true;
        System.out.print(u + " ");
        for (int v : graph.get(u)) {
            if (!visited[v]) {
                dfsRecursive(graph, v, visited);
            }
        }
    }

    // Iterative DFS — avoids stack overflow on deep graphs
    public static void dfsIterative(List<List<Integer>> graph, int src, int n) {
        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(src);

        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (visited[u]) continue;
            visited[u] = true;
            System.out.print(u + " ");
            for (int v : graph.get(u)) {
                if (!visited[v]) stack.push(v);
            }
        }
    }

    // Count connected components (undirected graph)
    public static int countComponents(List<List<Integer>> graph, int n) {
        boolean[] visited = new boolean[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfsRecursive(graph, i, visited);
                count++;
            }
        }
        return count;
    }

    // Find all paths from src to dst
    public static List<List<Integer>> allPaths(List<List<Integer>> graph,
                                               int src, int dst, int n) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        path.add(src);
        findAllPaths(graph, src, dst, visited, path, result);
        return result;
    }

    private static void findAllPaths(List<List<Integer>> graph, int u, int dst,
                                     boolean[] visited, List<Integer> path,
                                     List<List<Integer>> result) {
        if (u == dst) {
            result.add(new ArrayList<>(path));
            return;
        }
        visited[u] = true;
        for (int v : graph.get(u)) {
            if (!visited[v]) {
                path.add(v);
                findAllPaths(graph, v, dst, visited, path, result);
                path.remove(path.size() - 1);   // backtrack
            }
        }
        visited[u] = false;   // unmark for other paths
    }
}
```

### Complexity
- Time: O(V + E)
- Space: O(V) for recursion stack / explicit stack

---

## 5. Dijkstra's Algorithm

### What it does
Finds the shortest path from a **single source** to all other nodes in a graph with **non-negative** edge weights.

### When to use
- Weighted graph, non-negative weights
- Single-source shortest path
- "Minimum cost to reach X from Y"
- Navigation, routing problems

### When NOT to use
- Negative edge weights → Bellman-Ford
- Unweighted graph → BFS (simpler and faster)
- All-pairs shortest path → Floyd-Warshall

### Core Invariant

> When a node is **popped from the min-heap**, its distance is **finalized**.
> This is only safe because weights are non-negative — a future path can't be cheaper.

### The Stale Entry Problem

Every time you find a shorter path to a node, you push a new entry into the heap. The old entry is still there. When you pop it later, you must detect and skip it:

```java
if (cost > dist[u]) continue;   // stale — skip
```

### Java Implementation

```java
import java.util.*;

public class Dijkstra {

    // Returns dist[] from src to all nodes
    public static int[] dijkstra(List<List<int[]>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, (int) 1e9);
        dist[src] = 0;

        // min-heap: {cost, node}
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, src});

        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int cost = top[0], u = top[1];

            if (cost > dist[u]) continue;      // stale entry guard — critical

            for (int[] edge : graph.get(u)) {
                int v = edge[0], w = edge[1];
                int newCost = dist[u] + w;

                if (newCost < dist[v]) {
                    dist[v] = newCost;         // update dist FIRST
                    heap.offer(new int[]{newCost, v});
                }
            }
        }
        return dist;
    }

    // With path reconstruction
    public static int[] dijkstraWithPath(List<List<int[]>> graph, int src, int n,
                                         int[] parent) {
        int[] dist = new int[n];
        Arrays.fill(dist, (int) 1e9);
        Arrays.fill(parent, -1);
        dist[src] = 0;

        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, src});

        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int cost = top[0], u = top[1];
            if (cost > dist[u]) continue;

            for (int[] edge : graph.get(u)) {
                int v = edge[0], w = edge[1];
                int newCost = dist[u] + w;
                if (newCost < dist[v]) {
                    dist[v] = newCost;
                    parent[v] = u;             // record predecessor
                    heap.offer(new int[]{newCost, v});
                }
            }
        }
        return dist;
    }

    // Reconstruct path from parent[]
    public static List<Integer> getPath(int[] parent, int dst) {
        List<Integer> path = new ArrayList<>();
        for (int node = dst; node != -1; node = parent[node]) {
            path.add(node);
        }
        Collections.reverse(path);
        return path;
    }
}
```

### Complexity
- Time: O((V + E) log V) with binary heap
- Space: O(V + E)

---

## 6. Bellman-Ford Algorithm

### What it does
Finds shortest paths from a single source, **supporting negative edge weights**. Also detects negative cycles.

### When to use
- Graph has **negative edge weights**
- Need to detect a **negative cycle**
- Graph is small (Bellman-Ford is slower than Dijkstra's)

### When NOT to use
- All weights are non-negative → Dijkstra's (faster)
- Graph is very large → Bellman-Ford is O(V·E), impractical

### Core Idea

Relax ALL edges V-1 times. After V-1 iterations, all shortest paths are found (a path can have at most V-1 edges). If you can still relax on the V-th iteration, a negative cycle exists.

```
For i = 1 to V-1:
  For each edge (u, v, w):
    if dist[u] + w < dist[v]:
      dist[v] = dist[u] + w

// Negative cycle check
For each edge (u, v, w):
  if dist[u] + w < dist[v]:
    → negative cycle detected
```

### Java Implementation

```java
public class BellmanFord {

    // edges = [[u, v, weight], ...]
    public static int[] bellmanFord(int n, int[][] edges, int src) {
        int[] dist = new int[n];
        Arrays.fill(dist, (int) 1e9);
        dist[src] = 0;

        // Relax all edges V-1 times
        for (int i = 0; i < n - 1; i++) {
            boolean updated = false;
            for (int[] edge : edges) {
                int u = edge[0], v = edge[1], w = edge[2];
                if (dist[u] != (int) 1e9 && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    updated = true;
                }
            }
            if (!updated) break;           // early exit if no changes
        }

        // V-th relaxation: detect negative cycle
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            if (dist[u] != (int) 1e9 && dist[u] + w < dist[v]) {
                System.out.println("Negative cycle detected");
                return null;
            }
        }
        return dist;
    }
}
```

### Complexity
- Time: O(V · E)
- Space: O(V)

---

## 7. Floyd-Warshall Algorithm

### What it does
Finds the shortest path between **every pair of nodes**. Works with negative weights (but not negative cycles).

### When to use
- Need ALL-PAIRS shortest paths
- Graph is small (V ≤ 500 typically)
- Need to answer many "shortest path from X to Y" queries after one preprocessing step

### When NOT to use
- Only need single-source shortest path → Dijkstra's or Bellman-Ford
- Large graph (V > 1000) → O(V³) becomes too slow

### Core Idea

For each intermediate node k, check if going through k gives a shorter path from i to j.

```
dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])
```

Run this for every k from 0 to V-1.

### Java Implementation

```java
public class FloydWarshall {

    public static int[][] floydWarshall(int[][] dist, int n) {
        // dist[i][j] = weight of direct edge, 0 if i==j, INF if no edge
        // dist is modified in-place

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != (int) 1e9 && dist[k][j] != (int) 1e9) {
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
        }

        // Detect negative cycles: if dist[i][i] < 0, negative cycle exists
        for (int i = 0; i < n; i++) {
            if (dist[i][i] < 0) {
                System.out.println("Negative cycle detected");
                return null;
            }
        }
        return dist;
    }

    // Build the initial distance matrix from edge list
    public static int[][] buildMatrix(int n, int[][] edges, boolean directed) {
        int INF = (int) 1e9;
        int[][] dist = new int[n][n];
        for (int[] row : dist) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) dist[i][i] = 0;

        for (int[] e : edges) {
            dist[e[0]][e[1]] = e[2];
            if (!directed) dist[e[1]][e[0]] = e[2];
        }
        return dist;
    }
}
```

### Complexity
- Time: O(V³)
- Space: O(V²)

---

## 8. Topological Sort

### What it does
Orders nodes of a **DAG** such that for every directed edge u→v, u comes before v. Used whenever tasks have dependencies.

### When to use
- Task scheduling with dependencies ("you must finish A before B")
- Build systems (compile order)
- Course prerequisites
- Detecting cycles in a directed graph (if topological sort fails, a cycle exists)

### When NOT to use
- Graph has a cycle → topological sort is undefined for cyclic graphs
- Undirected graph → doesn't apply

### Two Approaches

**Kahn's Algorithm (BFS-based)**
- Intuition: repeatedly remove nodes with in-degree 0
- Easy to detect cycles: if final sorted order has fewer than V nodes → cycle exists

**DFS-based**
- Intuition: add a node to the result AFTER fully exploring all its descendants
- Natural fit when you're already doing DFS

### Java Implementation

```java
import java.util.*;

public class TopologicalSort {

    // Kahn's Algorithm (BFS) — also detects cycles
    public static int[] kahnSort(List<List<Integer>> graph, int n) {
        int[] inDegree = new int[n];
        for (int u = 0; u < n; u++) {
            for (int v : graph.get(u)) {
                inDegree[v]++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) queue.offer(i);   // all sources
        }

        int[] order = new int[n];
        int idx = 0;

        while (!queue.isEmpty()) {
            int u = queue.poll();
            order[idx++] = u;
            for (int v : graph.get(u)) {
                inDegree[v]--;
                if (inDegree[v] == 0) queue.offer(v);
            }
        }

        if (idx != n) {
            System.out.println("Cycle detected — topological sort not possible");
            return null;
        }
        return order;
    }

    // DFS-based topological sort
    public static List<Integer> dfsSort(List<List<Integer>> graph, int n) {
        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(graph, i, visited, stack);
            }
        }

        List<Integer> order = new ArrayList<>();
        while (!stack.isEmpty()) order.add(stack.pop());
        return order;
    }

    private static void dfs(List<List<Integer>> graph, int u,
                             boolean[] visited, Deque<Integer> stack) {
        visited[u] = true;
        for (int v : graph.get(u)) {
            if (!visited[v]) dfs(graph, v, visited, stack);
        }
        stack.push(u);     // push AFTER exploring all descendants
    }
}
```

### Complexity
- Time: O(V + E)
- Space: O(V)

---

## 9. Cycle Detection

### Undirected Graph

Use DFS with parent tracking. A back edge to a non-parent ancestor = cycle.

```java
public class CycleDetection {

    // Undirected graph cycle detection
    public static boolean hasCycleUndirected(List<List<Integer>> graph, int n) {
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!visited[i] && dfsUndirected(graph, i, -1, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dfsUndirected(List<List<Integer>> graph, int u,
                                          int parent, boolean[] visited) {
        visited[u] = true;
        for (int v : graph.get(u)) {
            if (!visited[v]) {
                if (dfsUndirected(graph, v, u, visited)) return true;
            } else if (v != parent) {
                return true;    // back edge to non-parent = cycle
            }
        }
        return false;
    }

    // Directed graph cycle detection (3-color DFS)
    // WHITE=0 (unvisited), GRAY=1 (in progress), BLACK=2 (done)
    public static boolean hasCycleDirected(List<List<Integer>> graph, int n) {
        int[] color = new int[n];    // 0=white, 1=gray, 2=black
        for (int i = 0; i < n; i++) {
            if (color[i] == 0 && dfsDirected(graph, i, color)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dfsDirected(List<List<Integer>> graph,
                                        int u, int[] color) {
        color[u] = 1;    // mark as in-progress (GRAY)
        for (int v : graph.get(u)) {
            if (color[v] == 1) return true;    // back edge → cycle!
            if (color[v] == 0 && dfsDirected(graph, v, color)) return true;
        }
        color[u] = 2;    // mark as done (BLACK)
        return false;
    }
}
```

### When to use which

| Graph type  | Method                    | Key signal              |
|-------------|---------------------------|-------------------------|
| Undirected  | DFS + parent tracking     | v != parent → cycle     |
| Directed    | DFS + 3-color (Gray set)  | gray neighbor → cycle   |
| Either      | Union-Find (undirected only) | merge → already same set → cycle |

---

## 10. Minimum Spanning Tree — Prim's & Kruskal's

### What is an MST?

A **Minimum Spanning Tree** connects all nodes with the minimum total edge weight, using exactly V-1 edges, with no cycles.

**Critical distinction:**
> MST ≠ Shortest Path. MST minimizes the *total weight of all edges used*. Dijkstra's minimizes the *path cost from one source*. These are different problems.

### Kruskal's Algorithm

Sort all edges by weight. Add edge to MST if it doesn't create a cycle. Use Union-Find to detect cycles.

**Best for:** Sparse graphs (fewer edges)

```java
import java.util.*;

public class Kruskal {

    // Union-Find helpers
    static int[] parent, rank;

    static int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]); // path compression
        return parent[x];
    }

    static boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;         // same component → would form cycle
        if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }

    // edges = [[u, v, weight], ...]
    public static int kruskal(int n, int[][] edges) {
        parent = new int[n];
        rank   = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));   // sort by weight

        int totalCost = 0, edgesUsed = 0;
        for (int[] edge : edges) {
            if (union(edge[0], edge[1])) {
                totalCost += edge[2];
                edgesUsed++;
                if (edgesUsed == n - 1) break;    // MST complete (V-1 edges)
            }
        }
        return (edgesUsed == n - 1) ? totalCost : -1;  // -1 if not connected
    }
}
```

### Prim's Algorithm

Start from any node. Greedily add the cheapest edge that connects a new node to the MST.

**Best for:** Dense graphs (many edges)

```java
public class Prim {

    // Adjacency list version with min-heap
    public static int prim(List<List<int[]>> graph, int n) {
        boolean[] inMST = new boolean[n];
        int[] minEdge = new int[n];
        Arrays.fill(minEdge, (int) 1e9);
        minEdge[0] = 0;

        // min-heap: {cost, node}
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        heap.offer(new int[]{0, 0});

        int totalCost = 0;

        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int cost = top[0], u = top[1];

            if (inMST[u]) continue;      // already in MST
            inMST[u] = true;
            totalCost += cost;

            for (int[] edge : graph.get(u)) {
                int v = edge[0], w = edge[1];
                if (!inMST[v] && w < minEdge[v]) {
                    minEdge[v] = w;
                    heap.offer(new int[]{w, v});
                }
            }
        }
        return totalCost;
    }
}
```

### Complexity Comparison

| Algorithm   | Time              | Best for         |
|-------------|-------------------|------------------|
| Kruskal's   | O(E log E)        | Sparse graphs    |
| Prim's (heap)| O(E log V)       | Dense graphs     |
| Prim's (matrix)| O(V²)          | Very dense (adj matrix) |

---

## 11. Union-Find (Disjoint Set Union)

### What it does
Efficiently answers: "Are nodes X and Y in the same connected component?" and "Merge the components of X and Y." Supports dynamic connectivity.

### When to use
- Number of connected components
- Cycle detection in undirected graphs
- Kruskal's MST
- Dynamic connectivity problems ("as edges are added, are X and Y connected?")
- "Number of islands" type problems (though DFS also works)

### When NOT to use
- Directed graphs (Union-Find is for undirected connectivity)
- Need path information (Union-Find only tells you if connected, not the path)

### Java Implementation

```java
public class UnionFind {

    int[] parent, rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank   = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    // Find with path compression — nearly O(1) amortized
    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    // Union by rank — keeps tree flat
    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;          // already connected
        if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }

    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    public int countComponents(int n) {
        Set<Integer> roots = new HashSet<>();
        for (int i = 0; i < n; i++) roots.add(find(i));
        return roots.size();
    }
}
```

### Complexity
- Time: Nearly O(1) per operation (amortized O(α(n)), where α is the inverse Ackermann function — effectively constant)

---

## 12. 0-1 BFS

### What it does
Shortest path when edge weights are **only 0 or 1**. Faster than Dijkstra's for this specific case.

### When to use
- Grid/graph where some moves are free (cost 0) and others cost 1
- "Minimum modifications to reach goal" where each modification costs 1
- Key insight: 0-cost edges don't advance the level → push to front of deque

### Java Implementation

```java
import java.util.*;

public class ZeroOneBFS {

    // graph[u] = [[v, weight], ...] where weight is 0 or 1
    public static int[] zeroOneBFS(List<List<int[]>> graph, int src, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, (int) 1e9);
        dist[src] = 0;

        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(src);

        while (!deque.isEmpty()) {
            int u = deque.pollFirst();

            for (int[] edge : graph.get(u)) {
                int v = edge[0], w = edge[1];
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    if (w == 0) deque.addFirst(v);    // free move → front
                    else        deque.addLast(v);     // cost-1 move → back
                }
            }
        }
        return dist;
    }
}
```

### Complexity
- Time: O(V + E) — same as BFS, much better than Dijkstra's O((V+E) log V)

---

## 13. Algorithm Comparison Matrix

| Algorithm       | Graph type        | Weights         | Purpose                    | Time            |
|-----------------|-------------------|-----------------|----------------------------|-----------------|
| BFS             | Directed/Undirected | None / equal  | Shortest hops, SSSP        | O(V + E)        |
| DFS             | Directed/Undirected | Any           | Traversal, cycles, paths   | O(V + E)        |
| Dijkstra's      | Directed/Undirected | Non-negative  | SSSP weighted              | O((V+E) log V)  |
| Bellman-Ford    | Directed/Undirected | Any (neg OK)  | SSSP with neg weights      | O(V · E)        |
| Floyd-Warshall  | Directed/Undirected | Any (neg OK)  | All-pairs shortest path    | O(V³)           |
| Topological Sort| Directed, DAG only | —             | Ordering with dependencies | O(V + E)        |
| Kruskal's       | Undirected        | Non-negative    | MST (sparse)               | O(E log E)      |
| Prim's          | Undirected        | Non-negative    | MST (dense)                | O(E log V)      |
| Union-Find      | Undirected        | —               | Connectivity, components   | O(α(n)) ≈ O(1)  |
| 0-1 BFS         | Directed/Undirected | 0 or 1 only  | SSSP on 0-1 weighted       | O(V + E)        |

*SSSP = Single Source Shortest Path*

---

## 14. Common Confusions Resolved

### Confusion 1 — Dijkstra's vs Prim's (they look almost identical in code!)

Both use a min-heap. Both greedily pick the cheapest next item. But they solve completely different problems.

| Aspect     | Dijkstra's                                | Prim's                                  |
|------------|-------------------------------------------|-----------------------------------------|
| Optimizes  | Path cost from source to each node       | Total weight of all edges in the tree  |
| Key in heap| Cumulative cost from source              | Weight of the cheapest edge to MST     |
| Question   | "Shortest path from A to B?"             | "Connect all nodes cheapest?"           |
| dist[v] =  | dist[u] + weight(u,v)                    | weight(u,v) only (not cumulative)      |

```java
// Dijkstra — new cost is CUMULATIVE from source
int newCost = dist[u] + w;
if (newCost < dist[v]) { dist[v] = newCost; heap.offer(new int[]{newCost, v}); }

// Prim — key is just the EDGE WEIGHT, not cumulative
if (!inMST[v] && w < minEdge[v]) { minEdge[v] = w; heap.offer(new int[]{w, v}); }
```

### Confusion 2 — DFS vs BFS for "shortest path"

```
DFS finds A path — not necessarily the shortest one.
BFS finds THE shortest path (in unweighted graphs).

If the problem says "shortest" and graph is unweighted → always BFS.
If the problem says "find if path exists" → DFS or BFS, DFS is simpler.
```

### Confusion 3 — Topological Sort vs DFS

Topological sort IS a DFS with an additional step: push nodes to a stack after returning from all descendants. A plain DFS visits in depth-first order; topological sort produces a valid dependency order.

### Confusion 4 — When BFS is called "level-order"

They are the same algorithm. "Level-order traversal" is just BFS on a tree. The vocabulary differs between tree and graph contexts.

### Confusion 5 — Bellman-Ford detects negative cycles, Dijkstra's doesn't

Dijkstra's declares a node final when it's popped. With a negative cycle, you can loop forever and keep reducing cost — Dijkstra's never terminates correctly. Bellman-Ford runs a fixed V-1 iterations, then checks if another relaxation is still possible. If yes → negative cycle.

### Confusion 6 — Union-Find vs DFS for connected components

```
Both find connected components in O(V + E) essentially.
Use Union-Find when:
  - Edges arrive dynamically (online connectivity)
  - You'll ask "are X and Y connected?" many times
  - You need merge operations

Use DFS when:
  - Graph is static
  - You also need path info or other traversal logic
```

### Confusion 7 — "Minimum spanning tree" vs "Shortest path tree"

```
MST: minimizes TOTAL edge weight across all edges.
     A→C might not be in MST even if it's the shortest direct path.

Shortest path tree (Dijkstra's): minimizes PATH COST from source.
     Every node's path from source is optimal.

A→B→C might be in the shortest path tree (total path cost minimized)
but A→C might be in the MST (single edge weight considered for spanning).
They can produce completely different trees.
```

---

## 15. Interview Playbook

### The 60-Second Clarification Checklist

Before touching code, ask or state these:

```
1. Directed or undirected?
2. Weighted? If yes — are weights always positive?
3. Can there be cycles?
4. Is the graph connected? (or do I need to handle multiple components?)
5. What am I optimizing — hops, cost, or something else?
6. What is V and E? (helps decide O complexity target)
```

### Decision Script (say this out loud)

```
"Since the graph is [weighted/unweighted] and [has/doesn't have] negative weights,
 I'll use [BFS / Dijkstra's / Bellman-Ford].

 I'll maintain a [dist / visited] array, initialize [source=0, rest=∞/-1],
 and use a [queue / min-heap / deque].

 [For Dijkstra's:] The key invariant is that when a node is popped from
 the heap, its distance is finalized — I'll skip stale entries with
 `if cost > dist[u]: continue`."
```

### Complexity Targets by Problem Size

```
V, E ≤ 100        → Floyd-Warshall O(V³) is fine
V, E ≤ 10,000     → Dijkstra's O((V+E) log V) is fine
V, E ≤ 100,000    → Dijkstra's or BFS — avoid O(V²)
V, E ≤ 1,000,000  → BFS or Union-Find only; avoid log factors if possible
```

### Common Interview Traps

**Trap 1 — Using Dijkstra's when BFS is sufficient**

If the interviewer gives a graph with all edge weights = 1, using Dijkstra's is correct but wasteful. Say "since all weights are equal, BFS is sufficient and runs in O(V+E) vs O((V+E) log V)."

**Trap 2 — Forgetting to handle disconnected graphs**

```java
// After Dijkstra's / BFS, always check reachability
if (dist[target] == (int) 1e9 || dist[target] == -1) {
    return -1;  // unreachable
}
```

**Trap 3 — Integer overflow in dist arrays**

```java
// WRONG — overflow when adding to MAX_VALUE
Arrays.fill(dist, Integer.MAX_VALUE);
int newCost = dist[u] + w;    // MAX_VALUE + 1 = negative!

// CORRECT
Arrays.fill(dist, (int) 1e9);  // 10^9, safe to add weights up to 10^9
```

**Trap 4 — Not asking about graph direction**

Adding edges in one direction only (for undirected) is one of the most common bugs. Always confirm, then:

```java
// Undirected: add edge BOTH ways
graph.get(u).add(new int[]{v, w});
graph.get(v).add(new int[]{u, w});   // ← easily forgotten
```

**Trap 5 — Using visited[] in Dijkstra's incorrectly**

In Dijkstra's, you don't need a separate visited array — the stale-entry check handles it. Adding a redundant visited array is harmless but shows confusion. The stale check is cleaner.

**Trap 6 — Applying topological sort to a cyclic graph**

If Kahn's algorithm returns fewer than V nodes, the graph has a cycle and topological sort is undefined. Always validate:

```java
if (idx != n) throw new IllegalArgumentException("Graph has a cycle");
```

### Phrases That Score Points

| Situation                        | What to say                                                                 |
|----------------------------------|-----------------------------------------------------------------------------|
| Choosing BFS over Dijkstra's     | "BFS is sufficient here — weights are equal, so O(V+E) beats O((V+E) log V)" |
| Explaining the stale-entry guard | "I skip entries where the popped cost exceeds the recorded dist — heap can have outdated entries after relaxation" |
| Negative weights                 | "Dijkstra's fails here because it finalizes nodes on pop — a later negative edge could yield a better path" |
| MST vs shortest path             | "These solve different problems — MST minimizes total tree weight, Dijkstra's minimizes path cost from source" |
| Cycle in directed graph          | "I use 3-color DFS — gray means in current call stack, so a gray neighbor is a back edge and confirms a cycle" |
| Union-Find complexity            | "Amortized nearly O(1) per operation with path compression and union by rank" |

---

## 16. Problem Pattern Recognition

### Grid Problems

```
Plain BFS (4/8 directions, uniform cost)         → BFS
"Minimum time/steps" with uniform moves          → BFS
"Minimum cost" with variable terrain costs       → Dijkstra's
Some cells free, some cost 1                     → 0-1 BFS
"Number of islands" / connected regions          → DFS or Union-Find
"All cells reachable from border" / flood fill   → DFS or BFS
```

### Dependency / Ordering Problems

```
"Is ordering possible?" / "Prerequisites"        → Topological Sort (Kahn's)
"Find order" with dependencies                   → Topological Sort
"Detect cycle in dependencies"                   → Topological Sort (if |result| < V → cycle)
"Shortest time to complete all tasks"            → Topological Sort + relaxation (DAG SSSP)
```

### Connectivity Problems

```
"Are A and B connected?"                         → DFS or BFS or Union-Find
"Number of connected components"                 → DFS loop or Union-Find
"Bridges / articulation points"                  → Tarjan's algorithm
"Strongly connected components"                  → Kosaraju's or Tarjan's
"Dynamic: add edges, query connectivity"         → Union-Find
```

### Shortest Path Problems

```
Unweighted, single source                        → BFS
Weighted, non-negative, single source            → Dijkstra's
Weighted, negative edges, single source          → Bellman-Ford
All pairs                                        → Floyd-Warshall
0-1 weights only                                 → 0-1 BFS
DAG shortest path                                → Topo sort + relaxation
```

### Cost Minimization Problems

```
"Connect all nodes with minimum cost"            → MST (Kruskal's or Prim's)
"Minimum cost path from A to B"                  → Dijkstra's (NOT MST)
"Minimum total weight of a subgraph"             → depends on constraints
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────┐
│                   GRAPH ALGORITHM QUICK REF                 │
├──────────────────────┬──────────────────────────────────────┤
│ BFS                  │ Unweighted SSSP, level traversal     │
│ DFS                  │ Traversal, cycles, components        │
│ Dijkstra's           │ Weighted SSSP, non-negative          │
│ Bellman-Ford         │ Weighted SSSP, handles negatives     │
│ Floyd-Warshall       │ All-pairs SP (small graphs)          │
│ Topological Sort     │ DAG ordering, dependency resolution  │
│ Kruskal's            │ MST, sparse graphs                   │
│ Prim's               │ MST, dense graphs                    │
│ Union-Find           │ Dynamic connectivity, components     │
│ 0-1 BFS              │ Weighted SSSP when weights ∈ {0,1}  │
├──────────────────────┴──────────────────────────────────────┤
│ CRITICAL RULES                                              │
│  • Dijkstra's → fails with negative weights                 │
│  • Dijkstra's stale guard: if (cost > dist[u]) continue     │
│  • MST ≠ Shortest Path Tree                                 │
│  • Topological Sort → only on DAGs                          │
│  • Union-Find → only undirected graphs                      │
│  • Dist init: 0 for source, 1e9 for rest (not MAX_VALUE)    │
└─────────────────────────────────────────────────────────────┘
```
