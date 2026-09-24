# ✅ GRAPH BASICS (Must Know)

### **Graph Types**

| Type                 | Meaning                   |
| -------------------- | ------------------------- |
| **Undirected**       | edges are two-way         |
| **Directed**         | edges are one-way         |
| **Weighted**         | edges have cost/weight    |
| **Unweighted**       | all edges weight = 1      |
| **Cyclic / Acyclic** | cycle exists or not       |
| **DAG**              | Directed Acyclic Graph    |
| **Connected Graph**  | every node reachable      |
| **Tree**             | connected + acyclic graph |
| **Forest**           | multiple trees            |

---

# ✅ GRAPH REPRESENTATION

## 1️⃣ Adjacency List (most used)

* `ArrayList<ArrayList<Integer>>`
* Space: **O(V + E)**
* Traversal: efficient

## 2️⃣ Adjacency Matrix

* `V x V matrix`
* Space: **O(V²)**
* Edge lookup: **O(1)**

✅ **Use matrix when graph is dense**
✅ **Use list when sparse (common in interviews)**

---

# ✅ IMPORTANT TERMINOLOGIES

| Term       | Meaning                        |
| ---------- | ------------------------------ |
| Degree     | number of edges from node      |
| Indegree   | incoming edges (directed)      |
| Outdegree  | outgoing edges                 |
| Path       | sequence of edges              |
| Cycle      | path that returns to same node |
| Components | separate subgraphs             |

---

# ✅ BFS (Breadth First Search)

### When to Use

* **shortest path in unweighted graph**
* level-order traversal

### Time Complexity

* **O(V + E)**

### Space

* **O(V)** (queue + visited)

### Confusion Point

✅ BFS gives shortest path only when **all edges weight = 1**.

---

# ✅ DFS (Depth First Search)

### When to Use

* cycle detection
* topological sort
* connected components

### Time

* **O(V + E)**

### Space

* **O(V)** recursion stack

### Confusion Point

DFS does NOT guarantee shortest path.

---

# ✅ CONNECTED COMPONENTS

### Undirected Graph

Run BFS/DFS from every unvisited node.

### Time

* **O(V + E)**

### Space

* **O(V)**

---

# ✅ CYCLE DETECTION

## 1️⃣ Undirected Graph Cycle Detection

### Method: DFS + parent tracking

If you visit a visited node that is **not parent** → cycle.

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
Visited neighbor is OK if it’s parent.

---

## 2️⃣ Directed Graph Cycle Detection

### Method 1: DFS + recursion stack (pathVis)

If node already in recursion stack → cycle.

Time: **O(V + E)**
Space: **O(V)**

### Method 2: Kahn’s Algorithm (Toposort)

If topo size < V → cycle exists.

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
Directed cycle detection ≠ parent tracking.

---

# ✅ TOPOLOGICAL SORT (Only for DAG)

### Meaning

Linear ordering where **u → v means u comes before v**

## Methods

### 1️⃣ DFS + Stack

Time: **O(V + E)**
Space: **O(V)**

### 2️⃣ Kahn’s Algorithm (BFS indegree)

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
Toposort possible **only if no cycle** (DAG).

---

# ✅ SHORTEST PATH ALGORITHMS (Most Confusing Part)

---

## 1️⃣ BFS Shortest Path (Unweighted)

Works when all weights = 1.

Time: **O(V + E)**
Space: **O(V)**

---

## 2️⃣ Dijkstra (Non-negative weights only)

### Works For

* weighted graph
* weights >= 0

### Time

* Using PQ: **O((V + E) log V)**
* Using array: **O(V²)**

### Space

* **O(V)**

⚠️ Confusion Points
❌ fails with negative weights
✅ can be used on directed/undirected

---

## 3️⃣ Bellman Ford (Handles negative weights)

### Works For

* negative weights
* detects negative cycle

### Time

* **O(V * E)**

### Space

* **O(V)**

⚠️ Confusion
If relaxation still possible in Vth iteration → **negative cycle**.

---

## 4️⃣ Floyd Warshall (All-pairs shortest path)

### Time

* **O(V³)**

### Space

* **O(V²)**

⚠️ Used when V is small (~400 max).

---

## 5️⃣ DAG Shortest Path (Best if DAG)

Use topo order + relax edges.

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
DAG shortest path can handle **negative weights** too (since no cycle).

---

# ✅ MINIMUM SPANNING TREE (MST)

### Meaning

Connect all vertices with minimum total weight.
Only for **undirected weighted graph**.

---

## 1️⃣ Prim’s Algorithm

### Idea

Grow MST like BFS using min edge.

Time:

* PQ: **O(E log V)**
  Space: **O(V)**

⚠️ Confusion:
Prim looks like Dijkstra but:

* Dijkstra minimizes **distance to node**
* Prim minimizes **edge weight to MST**

---

## 2️⃣ Kruskal’s Algorithm

### Idea

Sort edges + take smallest edge avoiding cycle (DSU)

Time:

* Sorting edges: **O(E log E)**
* DSU ops: ~O(E α(V)) ≈ O(E)

Space:

* **O(V)**

⚠️ Confusion:
Kruskal needs **DSU**.

---

# ✅ DISJOINT SET UNION (DSU / Union Find)

### Operations

* findParent()
* union()

### Optimizations

* path compression
* union by rank/size

Time per operation:

* **O(α(V)) ~ almost O(1)**

Space:

* **O(V)**

---

# ✅ STRONGLY CONNECTED COMPONENTS (SCC)

### Only for Directed Graph

All nodes mutually reachable.

---

## 1️⃣ Kosaraju Algorithm

Steps:

1. topo sort (finish time stack)
2. reverse graph
3. DFS in stack order

Time: **O(V + E)**
Space: **O(V + E)**

---

## 2️⃣ Tarjan Algorithm

Single DFS using low-link.

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
SCC ≠ connected components (undirected).

---

# ✅ BRIDGES and ARTICULATION POINTS (Very Important)

## Bridge (critical edge)

Removing it increases components.

## Articulation Point (critical node)

Removing it disconnects graph.

Algorithm: DFS + tin/low (Tarjan style)

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
Used only for **undirected graphs** in interviews (mostly).

---

# ✅ BIPARTITE GRAPH

### Meaning

Graph can be colored using 2 colors such that no adjacent nodes share same color.

### Detection

BFS/DFS coloring.

Time: **O(V + E)**
Space: **O(V)**

⚠️ Confusion:
Odd-length cycle ⇒ NOT bipartite.

---

# ✅ GRAPH COLORING (General)

* NP Hard for k-coloring
* Bipartite is special case (k=2)

---

# ✅ DETECTING NEGATIVE CYCLE

Use Bellman Ford:
If relaxation possible after V-1 rounds → negative cycle.

Time: **O(VE)**

---

# ✅ GRAPH TRAVERSAL SUMMARY TABLE

| Algorithm      | Works on            | Use case                   | Time         | Space  |
| -------------- | ------------------- | -------------------------- | ------------ | ------ |
| BFS            | any                 | shortest path (unweighted) | O(V+E)       | O(V)   |
| DFS            | any                 | cycle, components, topo    | O(V+E)       | O(V)   |
| Topo Sort      | DAG                 | ordering                   | O(V+E)       | O(V)   |
| Dijkstra       | non-neg weights     | shortest path              | O((V+E)logV) | O(V)   |
| Bellman Ford   | negative allowed    | shortest path + neg cycle  | O(VE)        | O(V)   |
| Floyd Warshall | all pairs           | dense graphs               | O(V³)        | O(V²)  |
| Prim           | undirected weighted | MST                        | O(ElogV)     | O(V)   |
| Kruskal        | undirected weighted | MST                        | O(ElogE)     | O(V)   |
| Kosaraju       | directed            | SCC                        | O(V+E)       | O(V+E) |
| Tarjan         | directed            | SCC                        | O(V+E)       | O(V)   |

---

# ✅ MOST COMMON CONFUSION POINTS (Super Important)

### 🔥 BFS vs DFS

* BFS = level-wise
* DFS = depth-wise
* BFS shortest path only for unweighted graphs

---

### 🔥 Prim vs Dijkstra

* Both use PQ
* Prim chooses min edge to MST
* Dijkstra chooses min distance to node

---

### 🔥 Dijkstra vs Bellman Ford

* Dijkstra fails for negative weights
* Bellman works but slower

---

### 🔥 Toposort vs Cycle detection

* DAG → toposort exists
* If cycle exists → topo not possible

---

### 🔥 Tree vs Graph

Tree is a special graph:

* connected
* no cycles
* edges = V-1

---

# ✅ QUICK IDENTIFICATION GUIDE (Interview Trick)

### If question says…

| Keyword                         | Use              |
| ------------------------------- | ---------------- |
| shortest path unweighted        | BFS              |
| shortest path weighted positive | Dijkstra         |
| shortest path weighted negative | Bellman Ford     |
| all pairs shortest path         | Floyd Warshall   |
| ordering dependencies           | Toposort         |
| detect cycle directed           | DFS stack / Kahn |
| detect cycle undirected         | DFS parent       |
| MST minimum wiring              | Prim / Kruskal   |
| groups in directed graph        | SCC              |
| critical edge/node              | Bridges/AP       |
| 2-color possible                | Bipartite        |

---
