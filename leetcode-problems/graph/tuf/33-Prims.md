That's a great request! Understanding the complexity of Prim's algorithm is crucial for evaluating its efficiency.

Here are the detailed time and space complexities for your Java implementation of Prim's algorithm, which uses a standard **Min-Priority Queue** (Min-Heap).

---

## ⏰ Time Complexity Analysis

The time complexity is primarily determined by the operations on the Priority Queue ($\text{add}$/$\text{offer}$ and $\text{extract-min}$/$\text{poll}$).

* Let $V$ be the number of vertices (nodes, $n$).
* Let $E$ be the number of edges in the graph.

### 1. Initialization
* The array `inMST` is initialized in $O(V)$ time.
* The edges connected to the starting node are added to the Priority Queue. In the worst case, the starting node has a high degree, leading to $O(V)$ insertions.
* **Time:** $O(V \log V)$ (Each of the $V$ insertions takes $O(\log Q)$, where $Q$ is the PQ size, which is at most $E$).

### 2. Main Loop (`while (!pq.isEmpty())`)
The loop runs until the MST is formed, which involves adding $V-1$ edges.

* **Edge Extraction ($\text{pq.poll()}$):** This operation, which extracts the minimum weight edge, happens at most **$E$ times** (once for every edge that gets added to the PQ). Since the PQ size is at most $E$, each extraction takes $O(\log E)$.
    * **Total Time for Extractions:** $O(E \log E)$.

* **Edge Insertion ($\text{pq.offer()}$):** Every edge in the entire graph is considered for insertion into the PQ **at most once** (when its source node is added to the MST). Therefore, there are at most $E$ insertions in total. Each insertion takes $O(\log E)$.
    * **Total Time for Insertions:** $O(E \log E)$.

### 3. Final Complexity

Combining the steps, the total time complexity is:
$$O(V \log V) + O(E \log E) + O(E \log E)$$

Since for a connected graph $E \ge V-1$, the $O(E \log E)$ term usually dominates.

$$\text{Time Complexity: } \mathbf{O(E \log E)}$$

***Note on $\mathbf{\log E}$ vs. $\mathbf{\log V}$:** Since $E$ is at most $V^2$ (for a complete graph), $\log E$ is $O(\log V^2) = O(2 \log V) = O(\log V)$. Therefore, the complexity is often cited as $\mathbf{O(E \log V)}$ for consistency with other graph algorithms.*

---

## 💾 Space Complexity Analysis

The space complexity is determined by the auxiliary data structures used to store the graph and the algorithm's state.

* **`graph` (Adjacency List):** The input itself requires space proportional to the number of nodes plus the number of edges.
    * **Space:** $O(V + E)$

* **`inMST` (boolean array):** Stores a boolean value for every vertex.
    * **Space:** $O(V)$

* **`mstEdges` (result list):** The final MST has exactly $V-1$ edges.
    * **Space:** $O(V)$

* **`pq` (PriorityQueue):** In the worst-case scenario (a highly connected graph), the PQ can store all edges that connect the growing MST to the unvisited nodes. This maximum size is bounded by $E$.
    * **Space:** $O(E)$

### Final Complexity
The total space complexity is the sum of the space required for the input and the auxiliary structures:
$$O(V + E) + O(V) + O(V) + O(E)$$

$$\text{Space Complexity: } \mathbf{O(V + E)}$$

This is an excellent result, as it means the algorithm only requires space linearly proportional to the size of the input graph.
