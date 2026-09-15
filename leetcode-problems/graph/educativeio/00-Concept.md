# Graphs (DSA) — From Zero to Mastery (Java)

---

## 0. Why Graphs Feel Hard (and the mental shift that fixes it)

Arrays and trees have an obvious "shape" — you can draw them in one line or with a clean root-to-leaf hierarchy. Graphs don't give you that comfort. A graph is just:

> **A bunch of things (nodes/vertices) and some connections between them (edges).**

That's it. No inherent shape. No inherent order. A tree is *secretly a special graph* — one with no cycles and exactly one path between any two nodes. Once you internalize "tree ⊂ graph", half the intimidation disappears, because you already know how to traverse trees (DFS/BFS) — graphs just need two extra rules:

1. **You must track "visited" nodes** — because unlike a tree, you can walk in a circle forever (cycles) or reach the same node via two different paths.
2. **There is no fixed "root"** — connections can go anywhere, so you often need to think about *which node do I even start from*.

Master those two adjustments, and 80% of graph problems become "DFS or BFS, with a twist."

---

## 1. Core Vocabulary (say these until they're automatic)

| Term | Meaning |
|---|---|
| **Vertex / Node** | A single entity (a city, a person, a webpage) |
| **Edge** | A connection between two vertices |
| **Directed graph** | Edges have direction: A→B doesn't imply B→A (Twitter follows) |
| **Undirected graph** | Edges are two-way: A—B implies B—A (Facebook friends) |
| **Weighted graph** | Edges carry a cost/distance (roads with distances) |
| **Unweighted graph** | All edges are "equal" (cost = 1) |
| **Degree** | Number of edges touching a vertex (in-degree/out-degree for directed) |
| **Path** | A sequence of vertices connected by edges |
| **Cycle** | A path that starts and ends at the same vertex |
| **Connected graph** | Every vertex can reach every other vertex (undirected) |
| **Connected component** | A maximal group of vertices all reachable from each other |
| **DAG** | Directed Acyclic Graph — directed, no cycles (task dependencies, build systems) |
| **Sparse vs Dense** | Few edges (E ≈ V) vs many edges (E ≈ V²) |

**Complexity shorthand you'll use constantly:** `V` = number of vertices, `E` = number of edges.

---

## 2. How to Represent a Graph in Code

This is the *first decision* in every graph problem, and it affects everything downstream.

### 2.1 Adjacency List (99% of the time, this is what you want)

Each vertex stores a list of its neighbors.

```java
// Unweighted, using ArrayList of ArrayLists
int n = 5; // number of vertices
List<List<Integer>> adj = new ArrayList<>();
for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

// add an undirected edge u-v
adj.get(u).add(v);
adj.get(v).add(u);

// add a directed edge u->v
adj.get(u).add(v);
```

For **weighted** graphs, store pairs `(neighbor, weight)`:

```java
// Using int[]{neighbor, weight}
List<List<int[]>> adj = new ArrayList<>();
for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

adj.get(u).add(new int[]{v, weight});
adj.get(v).add(new int[]{u, weight}); // if undirected
```

Or, if vertices are labeled with Strings/objects, use a `Map<String, List<String>>`.

**Space:** O(V + E) — efficient for sparse graphs (most real-world & interview graphs).
**Check "is u connected to v?"**: O(degree of u) — you scan the list.

### 2.2 Adjacency Matrix

A `V x V` grid where `matrix[u][v] = 1` (or weight) if an edge exists.

```java
int[][] matrix = new int[n][n];
matrix[u][v] = 1; // directed
matrix[u][v] = matrix[v][u] = 1; // undirected
```

**Space:** O(V²) — wasteful for sparse graphs, fine for dense ones.
**Check "is u connected to v?"**: O(1) — direct lookup. This is the matrix's superpower.

### 2.3 Edge List

Just a flat list of edges: `[(u1,v1), (u2,v2), ...]`. Rarely used for traversal, but this is exactly the format Kruskal's MST algorithm wants, because you need to sort edges by weight.

```java
int[][] edges = {{0,1,4}, {1,2,3}, {0,2,7}}; // {u, v, weight}
```

### Decision table

| Situation | Use |
|---|---|
| General traversal (DFS/BFS), sparse graph, most problems | **Adjacency List** |
| Need fast "are u,v connected?" checks, dense graph, small V | Adjacency Matrix |
| Algorithm needs to sort/process edges globally (Kruskal's MST) | Edge List |
| Grid problems (matrix of cells) | The grid *is* the graph — neighbors are computed via deltas, no explicit list needed |

**Special but common case — Grids:** A 2D grid (like an image or maze) is a graph where each cell is a vertex and edges connect to up/down/left/right (sometimes diagonal) neighbors. You don't build an adjacency list — you compute neighbors on the fly:

```java
int[] dr = {-1, 1, 0, 0};
int[] dc = {0, 0, -1, 1};
for (int d = 0; d < 4; d++) {
    int nr = r + dr[d], nc = c + dc[d];
    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
        // (nr, nc) is a neighbor of (r, c)
    }
}
```

**Pattern recognition tip:** If you see "grid", "matrix", "islands", "maze", "rooms" — your brain should immediately say *"this is a graph traversal problem in disguise."*

---

## 3. The Two Fundamental Traversals

Everything else in this guide is a variation of DFS or BFS. Get these into your fingers.

### 3.1 DFS (Depth-First Search) — "go deep, backtrack when stuck"

**Intuition:** Imagine exploring a maze with a ball of string. You pick a direction and keep walking as far as you can. When you hit a dead end, you backtrack to the last junction and try a different direction. You never revisit a place you've already been (mark it visited).

DFS uses the **call stack** (recursion) or an **explicit stack** to remember "where do I backtrack to."

```
Graph:        1
             / \
            2   3
           /     \
          4       5

DFS from 1: visit 1 → go to 2 → go to 4 (dead end, backtrack)
            → backtrack to 2 (no more neighbors, backtrack)
            → backtrack to 1 → go to 3 → go to 5
Order: 1, 2, 4, 3, 5
```

**Recursive implementation:**

```java
void dfs(int node, List<List<Integer>> adj, boolean[] visited) {
    visited[node] = true;
    System.out.println("Visiting: " + node);

    for (int neighbor : adj.get(node)) {
        if (!visited[neighbor]) {
            dfs(neighbor, adj, visited);
        }
    }
}
```

**Iterative implementation (using an explicit stack)** — useful when recursion depth could blow the call stack (very deep/large graphs):

```java
void dfsIterative(int start, List<List<Integer>> adj, int n) {
    boolean[] visited = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(start);

    while (!stack.isEmpty()) {
        int node = stack.pop();
        if (visited[node]) continue;
        visited[node] = true;
        System.out.println("Visiting: " + node);

        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                stack.push(neighbor);
            }
        }
    }
}
```

⚠️ **Subtlety:** in the iterative version, we check `visited` *when popping*, not when pushing — because the same node can be pushed multiple times before it's ever processed (multiple neighbors point to it). If you mark visited at push-time instead, you can still get correctness in simple cases, but marking-on-pop is the safer, more universal pattern (it also matches how you'll write BFS shortest-path variants later).

**Time complexity:** O(V + E) — every vertex is visited once, every edge is examined once.
**Space complexity:** O(V) — visited array + recursion stack / explicit stack.

**When to reach for DFS:**
- Exploring *all* paths / all possibilities (backtracking, permutations on a graph)
- Detecting cycles
- Topological sort
- Finding connected components
- Path existence between two nodes (when you don't care about shortest path)
- Anything phrased as "explore as far as possible" or involving backtracking

---

### 3.2 BFS (Breadth-First Search) — "explore layer by layer"

**Intuition:** Imagine dropping a stone in a pond. The ripples expand outward in perfect circles — everything at distance 1 gets touched before anything at distance 2. That's BFS. It uses a **queue** (FIFO) to guarantee this layer-by-layer expansion.

```
Graph:        1
             / \
            2   3
           /     \
          4       5

BFS from 1: Layer 0: {1}
            Layer 1: {2, 3}
            Layer 2: {4, 5}
Order: 1, 2, 3, 4, 5
```

```java
void bfs(int start, List<List<Integer>> adj, int n) {
    boolean[] visited = new boolean[n];
    Queue<Integer> queue = new LinkedList<>();

    visited[start] = true;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        System.out.println("Visiting: " + node);

        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                visited[neighbor] = true; // mark visited at ENQUEUE time
                queue.offer(neighbor);
            }
        }
    }
}
```

⚠️ **Critical subtlety, and a classic interview bug:** In BFS, you mark a node visited **the moment you enqueue it**, not when you dequeue it. Why? Because BFS explores level by level — if you wait until dequeue-time to mark visited, the *same* node can be enqueued multiple times by different neighbors in the same layer before it's ever processed, wasting time and (in shortest-path variants) corrupting your distance values. This is the opposite of the "mark on pop" rule we used for the iterative DFS stack — and the difference matters. Get this backwards and your BFS shortest-path distances will be subtly wrong on graphs with multiple paths to the same node.

**Time complexity:** O(V + E)
**Space complexity:** O(V) — queue + visited array

**When to reach for BFS:**
- **Shortest path in an unweighted graph** (this is BFS's superpower — see §5)
- "Minimum number of steps/moves" problems
- Level-order / layer-by-layer processing
- Multi-source spreading (see §4)
- Finding the nearest occurrence of something

---

### 3.3 DFS vs BFS — how to choose in 2 seconds

Ask yourself: **"Do I care about the shortest/minimum path, or just about reachability/exploring everything?"**

| Question in the problem | Use |
|---|---|
| "Is there a path from A to B?" | Either works, DFS is simpler |
| "What is the *shortest*/*minimum* path/steps?" (unweighted) | **BFS** |
| "Find all paths" / "explore every possibility" | **DFS** (often with backtracking) |
| "Detect a cycle" | Either (details differ for directed vs undirected — see §7) |
| "Group things into clusters/components" | Either |
| "Topological order" | DFS (or Kahn's BFS-based algorithm) |
| Weighted shortest path | **Neither directly** — need Dijkstra/Bellman-Ford (see §6) |

**Golden rule to memorize:** *BFS finds shortest paths in unweighted graphs because it explores in expanding "rings" of distance — the first time you reach a node, that's guaranteed to be via the shortest path. DFS gives you no such guarantee — it might tunnel deep down a long path before trying a short one.*

---

## 4. Connected Components

**The idea:** In an undirected graph, a "connected component" is an island of mutually-reachable vertices. Some vertices may not connect to others at all (the graph isn't fully connected).

**How to identify this pattern:** Anything that says "how many groups/islands/clusters/provinces/friend circles are there?" You're being asked: *"if I do a traversal from every unvisited node, how many separate traversals do I need?"*

**The recipe (works with DFS or BFS interchangeably):**

```java
int countComponents(int n, List<List<Integer>> adj) {
    boolean[] visited = new boolean[n];
    int count = 0;

    for (int i = 0; i < n; i++) {
        if (!visited[i]) {
            count++;          // found a new, unvisited island
            dfs(i, adj, visited); // "paint" the whole island as visited
        }
    }
    return count;
}
```

**Why this works:** DFS/BFS from a node visits *everything reachable from it* — i.e., its entire component. So every time your outer loop finds an unvisited node, it must be the "discovery" of a brand-new component you haven't touched yet. Once you DFS/BFS from it, that whole component gets marked, so the loop skips it forever after.

```
Graph:  1—2   3—4—5    6

Components: {1,2}, {3,4,5}, {6}   → count = 3
```

**Classic variants of this exact pattern:**
- Number of Islands (grid version — same idea, neighbors are up/down/left/right)
- Number of Provinces (adjacency matrix version)
- Friend Circles
- Counting "connected" clusters in any relationship graph

**LeetCode-style Number of Islands (grid DFS):**

```java
int numIslands(char[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    boolean[][] visited = new boolean[rows][cols];
    int islands = 0;

    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (grid[r][c] == '1' && !visited[r][c]) {
                islands++;
                sinkIsland(grid, visited, r, c, rows, cols);
            }
        }
    }
    return islands;
}

void sinkIsland(char[][] grid, boolean[][] visited, int r, int c, int rows, int cols) {
    if (r < 0 || r >= rows || c < 0 || c >= cols) return;
    if (visited[r][c] || grid[r][c] == '0') return;

    visited[r][c] = true;
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};
    for (int d = 0; d < 4; d++) {
        sinkIsland(grid, visited, r + dr[d], c + dc[d], rows, cols);
    }
}
```

### 4.1 The Alternative: Union-Find (Disjoint Set Union)

DFS/BFS finds components in one pass over a *static* graph. But what if edges are added **one at a time** and you need to know connectivity *as you go* (dynamic connectivity)? That's when Union-Find shines — it's not a replacement for DFS/BFS, it's a different tool for a different flavor of the same question.

**Intuition:** Every vertex starts as its own boss (its own component). When you union two vertices, one component's boss becomes the other's. To check "are u and v connected?", you just check "do they have the same ultimate boss (root)?"

```java
class UnionFind {
    int[] parent, rank;

    UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i; // everyone is their own root initially
    }

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // path compression: flatten the tree as we go
        }
        return parent[x];
    }

    void union(int x, int y) {
        int rootX = find(x), rootY = find(y);
        if (rootX == rootY) return; // already connected

        // union by rank: attach smaller tree under bigger tree, keeps trees flat
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
    }

    boolean connected(int x, int y) {
        return find(x) == find(y);
    }
}
```

With path compression + union by rank, `find`/`union` run in **nearly O(1)** amortized (technically O(α(n)), the inverse Ackermann function — for all practical purposes, constant).

**When to reach for Union-Find instead of DFS/BFS:**
- Edges arrive dynamically / you need to answer connectivity queries *between* additions ("Redundant Connection", "Accounts Merge", "Number of Islands II")
- Kruskal's MST algorithm (needs to detect "would this edge create a cycle?")
- You need to count components *while* processing a stream of unions, not just once at the end

---

## 5. Shortest Path in Unweighted Graphs = BFS

We touched on this already, but let's go deep, because this is one of the highest-leverage ideas in all of graph theory.

**Why BFS = shortest path when all edges cost 1:**

Think about it level by level. BFS processes all nodes at distance `d` from the source before touching *any* node at distance `d+1`. So the very first time a node is reached, it *must* be via the shortest possible number of edges — there's no way to have found it earlier through some sneaky shorter route, because BFS has already exhausted all shorter routes at previous layers.

**Recipe: BFS with distance tracking**

```java
int[] bfsShortestPath(int start, List<List<Integer>> adj, int n) {
    int[] dist = new int[n];
    Arrays.fill(dist, -1); // -1 = unreached
    Queue<Integer> queue = new LinkedList<>();

    dist[start] = 0;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        for (int neighbor : adj.get(node)) {
            if (dist[neighbor] == -1) { // not visited yet
                dist[neighbor] = dist[node] + 1; // one more hop than current node
                queue.offer(neighbor);
            }
        }
    }
    return dist; // dist[i] = shortest #edges from start to i (-1 if unreachable)
}
```

**Pattern recognition — the words to watch for:**
- "Minimum number of moves/steps/operations to reach X"
- "Shortest path" (with no mention of weights/costs)
- "Fewest transformations" (Word Ladder-style problems)
- Anything on an unweighted grid asking for minimum steps

**If you also need to reconstruct the actual path** (not just distance), track parents:

```java
int[] parent = new int[n];
Arrays.fill(parent, -1);
// inside BFS, when discovering neighbor:
parent[neighbor] = node;

// Reconstruct path from 'end' back to 'start':
List<Integer> path = new ArrayList<>();
for (int at = end; at != -1; at = parent[at]) {
    path.add(at);
}
Collections.reverse(path);
```

---

## 6. Multi-Source BFS

**The idea:** Sometimes you don't have *one* starting point — you have many, and you want to know, for every cell/node, "what's the shortest distance to the *nearest* source?"

**The beautiful trick:** Instead of running BFS separately from each source (expensive: O(sources × (V+E))), you push **all sources into the queue at once**, with distance 0, and run a *single* BFS. The ripples from all sources expand simultaneously, and naturally, whichever ripple reaches a cell first is the nearest source — because they're all growing at the same rate (one layer per step).

```
Grid (0 = land, source = 'S'):

S . . .
. . . .
. . . S
. . . .

Instead of BFS from each S separately, put BOTH sources in
the queue at distance 0 and expand together — the wavefronts
meet in the middle, and every cell gets its TRUE nearest distance.
```

**Classic problem: "01 Matrix" / "Rotting Oranges" — distance to nearest zero/rotten orange:**

```java
int[][] multiSourceBFS(int[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    int[][] dist = new int[rows][cols];
    for (int[] row : dist) Arrays.fill(row, -1);

    Queue<int[]> queue = new LinkedList<>();

    // STEP 1: seed the queue with ALL sources at distance 0
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (grid[r][c] == 1) { // 1 = a "source" (e.g., rotten orange)
                dist[r][c] = 0;
                queue.offer(new int[]{r, c});
            }
        }
    }

    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};

    // STEP 2: normal BFS, but now expanding from ALL sources together
    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        int r = cur[0], c = cur[1];

        for (int d = 0; d < 4; d++) {
            int nr = r + dr[d], nc = c + dc[d];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && dist[nr][nc] == -1) {
                dist[nr][nc] = dist[r][c] + 1;
                queue.offer(new int[]{nr, nc});
            }
        }
    }
    return dist;
}
```

**Pattern recognition:**
- "Nearest / minimum distance to *any* of these special cells"
- "Rotting Oranges" (how long until everything rots)
- "Walls and Gates"
- "01 Matrix"
- Anything with **multiple starting points** and a "closest one wins" flavor

**Key mental model:** Multi-source BFS is identical to regular BFS — the *only* change is what you put in the queue before you start (all sources, distance 0, all marked visited upfront) instead of just one.

---

## 7. Cycle Detection

Cycle detection logic **differs** between undirected and directed graphs — this trips up almost everyone at first, so let's be very precise about *why*.

### 7.1 Undirected Graph — Cycle Detection via DFS

**The trap:** In an undirected graph, edge `u-v` means you can go from `u` to `v` AND from `v` back to `u`. So during DFS, when you're at `v` and you see `u` in your neighbor list, that's *not* a cycle — that's just the edge you arrived on! You need to explicitly ignore the "parent" (the node you came from).

```java
boolean hasCycleUndirected(int n, List<List<Integer>> adj) {
    boolean[] visited = new boolean[n];
    for (int i = 0; i < n; i++) {
        if (!visited[i]) {
            if (dfsCycleCheck(i, -1, adj, visited)) return true;
        }
    }
    return false;
}

boolean dfsCycleCheck(int node, int parent, List<List<Integer>> adj, boolean[] visited) {
    visited[node] = true;
    for (int neighbor : adj.get(node)) {
        if (!visited[neighbor]) {
            if (dfsCycleCheck(neighbor, node, adj, visited)) return true;
        } else if (neighbor != parent) {
            // visited AND not the node we just came from → back edge → CYCLE
            return true;
        }
    }
    return false;
}
```

**Intuition:** If, while exploring, you bump into a node that's *already visited* and it's *not* your immediate parent, it means there are two distinct paths to reach that node — which is the definition of a cycle.

**Alternative: Union-Find for undirected cycle detection.** Process edges one at a time; if `find(u) == find(v)` *before* you union them, adding this edge would connect two already-connected nodes → cycle.

```java
boolean hasCycleUnionFind(int n, int[][] edges) {
    UnionFind uf = new UnionFind(n);
    for (int[] edge : edges) {
        int u = edge[0], v = edge[1];
        if (uf.connected(u, v)) return true; // already same component → cycle
        uf.union(u, v);
    }
    return false;
}
```

This Union-Find approach is *the* classic solution to "Redundant Connection" style problems.

### 7.2 Directed Graph — Cycle Detection needs a 3-Color / recursion-stack approach

**The trap:** In a directed graph, seeing a "visited" node is *not enough* to declare a cycle — it might be a node that was already fully explored via a completely different, unrelated path (that's totally fine in a DAG). A cycle only exists if you reach a node that is **currently on your current DFS path** (an ancestor in the current recursion), not just "visited at some point in the past."

**Solution: track 3 states per node.**
- **White (unvisited):** haven't touched it
- **Gray (in progress):** currently on the DFS stack — an ancestor of where we are now
- **Black (done):** fully explored, and safely can't cause a cycle back to us

```java
int[] color; // 0 = white, 1 = gray, 2 = black

boolean hasCycleDirected(int n, List<List<Integer>> adj) {
    color = new int[n]; // all default to 0 (white)
    for (int i = 0; i < n; i++) {
        if (color[i] == 0) {
            if (dfsDirectedCycle(i, adj)) return true;
        }
    }
    return false;
}

boolean dfsDirectedCycle(int node, List<List<Integer>> adj) {
    color[node] = 1; // mark gray: "I'm currently on the path"

    for (int neighbor : adj.get(node)) {
        if (color[neighbor] == 1) {
            return true; // hit a gray node → it's an ANCESTOR → cycle!
        }
        if (color[neighbor] == 0 && dfsDirectedCycle(neighbor, adj)) {
            return true;
        }
    }

    color[node] = 2; // mark black: fully done, safe forever
    return false;
}
```

```
A → B → C
    ↑   ↓
    └── D

DFS: A(gray) → B(gray) → C(gray) → D(gray) → back-edge to B(gray) → CYCLE!
```

**Why this matters beyond "cycle: yes/no":** This exact gray/black idea is the backbone of **topological sort** (a DAG has no cycles, so topological sort is only possible on a DAG — and detecting "not a DAG" is literally this algorithm). If you've already studied Topological Sort with me, this should feel very familiar — same 3-color logic, just returning a cycle boolean instead of an ordering.

**Pattern recognition summary:**

| Graph type | Cycle signal during DFS |
|---|---|
| Undirected | Neighbor is visited AND is not the immediate parent |
| Directed | Neighbor is currently gray (on the active recursion stack) |

---

## 8. Weighted Shortest Paths — Dijkstra's Algorithm

Now we leave "all edges cost 1" behind. Roads have different lengths, flights have different prices — BFS's "layer by layer" trick breaks down because a longer *hop count* path might still be *cheaper* overall.

### 8.1 The Core Idea — Greedy + Priority Queue

**Intuition (the "explorer with a map of tentative distances" mental model):**

Imagine you're standing at the source with a giant table: "distance to every other city — currently ∞ for everyone except me (0)." You repeatedly do this:

1. **Pick the closest unfinalized city** you currently know about (this is why we need a priority queue — always grab the minimum).
2. **"Finalize" its distance** — you now know for certain this is the shortest possible way to reach it (proof below).
3. **Relax its neighbors** — for every neighbor, check: "is going through me shorter than what they currently have recorded?" If yes, update their tentative distance.
4. Repeat until every reachable city is finalized.

**Why picking the "closest known" node guarantees correctness (the key insight):** All edge weights are non-negative. So once you've picked the globally closest unfinalized node, there is *no way* any other, not-yet-explored path could somehow be shorter — every other path would have to go through a node that's *farther away* than the one you just picked, and adding more non-negative edge weights on top of "farther away" can only make it even farther. This greedy choice is safe **only because weights are non-negative** — this is exactly why Dijkstra fails with negative weights (see §8.4).

**"Relaxing an edge"** just means: `if (dist[u] + weight(u,v) < dist[v]) dist[v] = dist[u] + weight(u,v);` — "can I improve your distance by routing through me?"

```
        (2)
   A ──────── B
   │          │
  (4)        (1)
   │          │
   C ──────── D
        (5)

From A:
dist[A]=0
Explore A's neighbors: dist[B]=2, dist[C]=4
Pick smallest unfinalized: B (dist=2) → finalize B
  Relax B's neighbors: dist[D] = min(∞, 2+1) = 3
Pick smallest unfinalized: D (dist=3) → finalize D
  Relax D's neighbors: dist[C] = min(4, 3+5) = 4 (no improvement)
Pick smallest unfinalized: C (dist=4) → finalize C
Final: dist = {A:0, B:2, C:4, D:3}
```

### 8.2 Implementation with PriorityQueue (Java)

```java
int[] dijkstra(int src, List<List<int[]>> adj, int n) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;

    // min-heap of {distance, node} — always pop the currently-closest node
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    pq.offer(new int[]{0, src});

    boolean[] finalized = new boolean[n];

    while (!pq.isEmpty()) {
        int[] top = pq.poll();
        int d = top[0], node = top[1];

        if (finalized[node]) continue; // stale entry (we found a better path already), skip
        finalized[node] = true;

        for (int[] edge : adj.get(node)) {
            int neighbor = edge[0], weight = edge[1];
            if (!finalized[neighbor] && dist[node] + weight < dist[neighbor]) {
                dist[neighbor] = dist[node] + weight;
                pq.offer(new int[]{dist[neighbor], neighbor}); // push updated distance
            }
        }
    }
    return dist;
}
```

**Why "stale entries" happen and why the `if (finalized[node]) continue;` check is essential:** We don't have a way to *decrease* an existing entry's priority inside Java's `PriorityQueue` (no `decrease-key` operation). So instead, whenever we find a better distance to a node, we just push a *new* entry into the heap rather than updating the old one. This means the same node can sit in the heap multiple times with different (stale, outdated) distances. When we pop a node, if we've already finalized it via an earlier, better entry, we just skip this stale duplicate — cheap and correct.

**Complexity:** With a binary heap, O((V + E) log V) — each edge can trigger one heap insertion, and each insertion/removal costs O(log V).

### 8.3 BFS vs Dijkstra — one clean mental table

| | BFS | Dijkstra |
|---|---|---|
| Edge weights | All equal (1) | Non-negative, can differ |
| Data structure | Plain Queue (FIFO) | Priority Queue (min-heap) |
| Why it works | Explores in expanding equal-cost layers | Always expands the globally cheapest frontier next |
| Complexity | O(V + E) | O((V+E) log V) |

**Mental shortcut:** Dijkstra is "BFS, but the queue is replaced by a priority queue that always gives you the cheapest option next, because not all hops cost the same anymore."

### 8.4 Why Dijkstra Breaks with Negative Weights

The whole algorithm's correctness rests on: *"once finalized, a node's distance can never improve."* But if edges can be negative, this promise breaks:

```
A --(1)--> B --(1)--> C
A --(4)--> C
A ---------(-10)-------→ D --(1)--> B

If we greedily finalize B early (dist=1) because it looked cheapest,
we might miss that going A→D→B later is actually dist = -10+1 = -9,
MUCH cheaper — but B was already "locked in" as finalized!
```

Once a negative edge exists, a path you haven't explored yet could retroactively become cheaper than something you already finalized. Dijkstra has no mechanism to revisit finalized nodes, so it can produce **wrong answers** (not just slow ones) on graphs with negative edges.

**The fix — Bellman-Ford:** Instead of greedily finalizing, Bellman-Ford relaxes *every* edge, *V-1* times, brute-force style. This is slower — O(V·E) — but works correctly with negative weights (and can even *detect* negative cycles, which make "shortest path" meaningless in the first place — you could loop forever getting cheaper and cheaper).

```java
int[] bellmanFord(int src, int[][] edges, int n) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;

    // Relax ALL edges, V-1 times — guarantees shortest paths are found
    // (a shortest path has at most V-1 edges, so V-1 rounds is always enough)
    for (int i = 0; i < n - 1; i++) {
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
            }
        }
    }

    // Optional: one more round — if anything STILL improves, there's a negative cycle
    for (int[] edge : edges) {
        int u = edge[0], v = edge[1], w = edge[2];
        if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
            System.out.println("Negative cycle detected!");
        }
    }
    return dist;
}
```

**Decision table for weighted shortest paths:**

| Situation | Algorithm |
|---|---|
| All edge weights ≥ 0 | **Dijkstra** — O((V+E) log V), fast |
| Negative weights allowed, no negative cycle | **Bellman-Ford** — O(V·E), handles negatives |
| Need to detect negative cycles | **Bellman-Ford** (extra round) |
| Need shortest paths between **all pairs** of nodes | Floyd-Warshall — O(V³), fine for small V (beyond this guide's scope, but worth knowing the name) |

---

## 9. Bonus: Minimum Spanning Tree (MST) — a natural next step

Since you now have both DFS/BFS traversal *and* Union-Find *and* the "greedy + priority queue" idea from Dijkstra, MST algorithms will feel like a natural remix of tools you already have:

- **Kruskal's:** Sort all edges by weight, add them one by one using **Union-Find** to skip any edge that would create a cycle, until you've connected everything.
- **Prim's:** Just like Dijkstra, but instead of tracking "shortest distance from source", you track "cheapest edge that connects a new node to your growing tree" — same priority-queue skeleton, different relaxation rule.

(Flagging this as a natural "what's next" — happy to go deep on MST the same way if you want it as a separate session, same as we did for Topological Sort.)

---

## 10. The Master Pattern-Recognition Cheat Sheet

When you read a graph problem, run through this checklist top to bottom:

```
1. Is it secretly a graph? 
   → Grid / matrix / "islands" / "rooms" / relationships / dependencies → YES, it's a graph.

2. Directed or undirected?
   → Affects cycle detection and sometimes traversal logic.

3. Weighted or unweighted?
   → Unweighted + shortest path → BFS
   → Weighted, non-negative + shortest path → Dijkstra
   → Weighted, has negatives + shortest path → Bellman-Ford

4. What's being asked?

   "Is X reachable from Y?"              → DFS or BFS (either)
   "Shortest path / min steps"           → BFS (unweighted) or Dijkstra (weighted)
   "How many groups/islands/clusters?"   → Connected components (DFS/BFS loop, or Union-Find)
   "Nearest special cell from ANY of..." → Multi-source BFS
   "Does a cycle exist?"                 → DFS (parent-check for undirected,
                                            gray/black for directed) or Union-Find
   "Valid ordering respecting deps?"     → Topological sort (DFS-based or Kahn's BFS-based)
   "Cheapest way to connect everything"  → MST (Kruskal's / Prim's)
   "Explore all possible paths"          → DFS + backtracking

5. What should I track alongside "visited"?
   → distance[]   (BFS shortest path, Dijkstra)
   → parent[]     (path reconstruction, undirected cycle check)
   → color[]      (directed cycle detection / topo sort)
   → component ID (labeling which island each node belongs to)
```

---

## 11. Complexity Cheat Sheet

| Algorithm | Time | Space | Notes |
|---|---|---|---|
| DFS / BFS | O(V + E) | O(V) | Foundation of everything |
| Connected Components | O(V + E) | O(V) | DFS/BFS loop over all nodes |
| Union-Find (with optimizations) | ~O(1) per op | O(V) | Amortized, inverse-Ackermann |
| BFS Shortest Path (unweighted) | O(V + E) | O(V) | |
| Multi-source BFS | O(V + E) | O(V) | Same as BFS — just multiple seeds |
| Dijkstra (binary heap) | O((V+E) log V) | O(V) | Requires non-negative weights |
| Bellman-Ford | O(V · E) | O(V) | Handles negative weights, detects negative cycles |
| Cycle Detection (either type) | O(V + E) | O(V) | |
| Kruskal's MST | O(E log E) | O(V) | Sorting + Union-Find |
| Prim's MST | O(E log V) | O(V) | Priority-queue based |

---

## 12. Suggested Practice Progression

1. **Warm-up:** Write DFS and BFS from scratch on an adjacency list, both recursive and iterative, until you can do it without looking.
2. **Number of Islands** (grid DFS/BFS) — cements traversal + connected components.
3. **Rotting Oranges** — multi-source BFS.
4. **Word Ladder** — BFS shortest path on an *implicit* graph (this is where BFS really clicks, because there's no adjacency list to begin with — you build neighbors on the fly by changing one letter at a time).
5. **Course Schedule I & II** — cycle detection + topological sort in a directed graph.
6. **Redundant Connection** — Union-Find cycle detection.
7. **Network Delay Time** — vanilla Dijkstra.
8. **Cheapest Flights Within K Stops** — Bellman-Ford-flavored (Dijkstra alone gives the *wrong* answer here because of the "K stops" constraint — a great problem for understanding *why* Dijkstra's greedy assumption can fail once extra constraints are added).

---

### Quick Recap in One Paragraph

A graph is just nodes + connections, with no built-in shape — so you always track `visited` and think carefully about where to start. DFS goes deep and backtracks (call stack); BFS spreads out layer by layer (queue) and is *the* tool for shortest paths when every edge costs the same. Multi-source BFS is the same idea with multiple starting ripples. Connected components are "how many times do I need to start a fresh traversal?" — solvable via DFS/BFS or, for dynamic edge-by-edge scenarios, Union-Find. Cycle detection needs a parent-check in undirected graphs but a gray/black recursion-stack check in directed ones, because "already visited" means something different in each. Once weights enter the picture, BFS's guarantee collapses, and Dijkstra takes over — greedily finalizing the cheapest known node each time, which works *only* because non-negative weights guarantee nothing shorter can appear later; negative weights force you to Bellman-Ford instead. Every one of these algorithms is a variation on the same two skeletons: DFS and BFS.
