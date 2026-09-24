# 📊 Prim's vs Kruskal's Algorithm — MST Comparison

## ✅ 1. Core Idea

| Feature              | **Prim’s Algorithm**                 | **Kruskal’s Algorithm**                  |
| -------------------- | ------------------------------------ | ---------------------------------------- |
| Approach             | Greedy — expand MST from a vertex    | Greedy — add smallest edge globally      |
| Strategy             | Grow MST vertex by vertex            | Grow MST edge by edge                    |
| Data Structures Used | Priority Queue (Min Heap), Visited[] | Union-Find (Disjoint Set), Edge List     |
| Input Graph Format   | Adjacency List                       | Edge List                                |
| Graph Type           | Connected Graphs only (default)      | Works on connected & disconnected graphs |

---

## ✅ 2. Time Complexity

| Operation                 | **Prim’s (Adj List + Min Heap)** | **Kruskal’s (Union-Find)**         |
| ------------------------- | -------------------------------- | ---------------------------------- |
| Sorting Required?         | ❌ No                             | ✅ Yes                              |
| Edge Selection            | O(log V) per edge (via heap)     | O(log V) per edge (via Union-Find) |
| **Total Time Complexity** | **O(E log V)**                   | **O(E log E)** ≈ **O(E log V)**    |
| Best for                  | Dense Graphs                     | Sparse Graphs                      |

---

## ✅ 3. Space Complexity

| Component             | **Prim’s**   | **Kruskal’s** |
| --------------------- | ------------ | ------------- |
| Visited Array         | O(V)         | O(V)          |
| Heap / Edge List      | O(E)         | O(E)          |
| Union-Find Structures | ❌ Not needed | ✅ O(V)        |
| **Total**             | **O(V + E)** | **O(V + E)**  |

---

## ✅ 4. When to Use What?

| Scenario                    | **Use Prim’s**                   | **Use Kruskal’s**            |
| --------------------------- | -------------------------------- | ---------------------------- |
| Graph Type                  | Dense graphs with adjacency list | Sparse graphs with edge list |
| Need Incremental MST Growth | ✅ Yes                            | ❌ No                         |
| Graph May Be Disconnected   | ❌ Needs modification             | ✅ Works (returns a forest)   |
| Implementation Simplicity   | Moderate (heap + graph)          | Simple (sort + DSU)          |
| Visualization / Teaching    | Less intuitive                   | More intuitive               |

---

## ✅ 5. Summary Table

| Category              | **Prim's**                 | **Kruskal's** |
| --------------------- | -------------------------- | ------------- |
| MST Construction      | Vertex by vertex           | Edge by edge  |
| Graph Representation  | Adjacency List             | Edge List     |
| Cycle Detection       | Visited[]                  | Union-Find    |
| Suitable Graph Type   | Dense                      | Sparse        |
| Time Complexity       | O(E log V)                 | O(E log E)    |
| Space Complexity      | O(V + E)                   | O(V + E)      |
| Handles Disconnected? | ❌ No (modification needed) | ✅ Yes         |
| Simplicity            | Medium                     | Easy          |

---

## ✅ Example Use Cases

- **Prim’s Algorithm**
  - Network design starting from a central node (e.g., routers, pipelines)
  - When graph is dense or adjacency list is available

- **Kruskal’s Algorithm**
  - Road systems, airline routes where edges are known globally
  - When working with edge list format

---



## Prim's Algorithm vs. Kruskal's Algorithm: A Side-by-Side Comparison

Both Prim's and Kruskal's algorithms are greedy algorithms used to find the Minimum Spanning Tree (MST) of a weighted, undirected, and connected graph. Here's a comparison of their characteristics:

| Feature                          | Prim's Algorithm                                                                                                                                                | Kruskal's Algorithm                                                                                                      |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| **Approach**                     | Vertex-based: Grows the MST by adding one vertex at a time from the current MST.                                                                                | Edge-based: Grows a forest of trees by adding the cheapest edges that don't form cycles.                                 |
| **Starting Point**               | Starts with an arbitrary vertex.                                                                                                                                | Starts with all edges and considers them in increasing order of weight.                                                  |
| **Building the MST**             | Maintains a single connected component (the MST) and expands it.                                                                                                | Maintains a collection of disjoint sets (trees) that are gradually merged.                                               |
| **Cycle Detection**              | Implicitly avoids cycles because it always adds an edge connecting a vertex in the MST to a vertex outside the MST.                                             | Explicitly checks for cycles before adding an edge using a Disjoint Set Union (DSU) data structure.                      |
| **Data Structures**              | Typically uses a Priority Queue (Min-Heap) to store edges with one endpoint in the MST and the other outside. Can also be implemented with an adjacency matrix. | Requires sorting the edges (can be done using various sorting algorithms) and a Disjoint Set Union (DSU) data structure. |
| **Time Complexity**              | - O(V²) with an adjacency matrix.                                                                                                                               | - O(E log E) or O(E log V) using efficient sorting and DSU.                                                              |
|                                  | - O(E log V) or O((V+E) log V) with a binary heap and adjacency list.                                                                                           |                                                                                                                          |
|                                  | - O(E + V log V) with a Fibonacci heap and adjacency list (theoretically best).                                                                                 |                                                                                                                          |
| **Space Complexity**             | - O(V + E) for adjacency list with a priority queue.                                                                                                            | - O(V + E) to store edges and the DSU structure.                                                                         |
|                                  | - O(V²) for adjacency matrix.                                                                                                                                   |                                                                                                                          |
| **Graph Requirement**            | Requires the graph to be connected to find a single MST.                                                                                                        | Can work with disconnected graphs, resulting in a Minimum Spanning Forest (MSF).                                         |
| **Implementation Complexity**    | Generally simpler to implement, especially with an adjacency matrix for dense graphs.                                                                           | Can be slightly more complex due to the need for sorting and the DSU data structure.                                     |
| **Performance on Dense Graphs**  | Often performs better than Kruskal's when the graph has a high number of edges (E ≈ V²).                                                                        | Can be less efficient on dense graphs due to the overhead of sorting all edges.                                          |
| **Performance on Sparse Graphs** | Can be less efficient than Kruskal's due to the priority queue operations.                                                                                      | Often performs better than Prim's when the graph has a low number of edges (E ≈ V).                                      |
| **Intermediate Result**          | Always maintains a connected subgraph (a tree).                                                                                                                 | Can have disconnected components (a forest) until the end.                                                               |

**When to Use Which Algorithm:**

* **Prim's Algorithm:**
    * **Dense Graphs:** When the number of edges is significantly higher than the number of vertices (E is close to V²), Prim's algorithm often has a better time complexity, especially when implemented with an adjacency matrix.
    * **Starting from a Specific Vertex:** If you need to grow the MST from a particular starting vertex, Prim's algorithm naturally fits this requirement.
    * **Simpler Implementation (for dense graphs):** With an adjacency matrix, the implementation can be straightforward.

* **Kruskal's Algorithm:**
    * **Sparse Graphs:** When the number of edges is relatively small compared to the number of vertices (E is close to V), Kruskal's algorithm generally performs better due to the efficiency of sorting edges.
    * **Disconnected Graphs:** Kruskal's can find the Minimum Spanning Forest of a disconnected graph.
    * **Easier to Parallelize:** The edge sorting step in Kruskal's can be easily parallelized.
    * **Simple Logic:** The core logic of iterating through sorted edges and checking for cycles is conceptually simple.

**Other Considerations:**

* The actual performance can depend on the specific implementation and the data structures used.
* For very large graphs, the choice of data structure (e.g., Fibonacci heaps for Prim's) can significantly impact performance.
* If the edges are already sorted or can be sorted efficiently in linear time for some specific graph structures, Kruskal's performance can be further improved.

In summary, the choice between Prim's and Kruskal's algorithm often boils down to the density of the graph. Prim's is generally preferred for dense graphs, while Kruskal's is usually more efficient for sparse graphs. However, other factors like the need for a specific starting vertex or the possibility of disconnected components can also influence the decision.
