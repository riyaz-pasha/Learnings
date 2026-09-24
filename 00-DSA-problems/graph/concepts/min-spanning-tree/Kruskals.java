import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Kruskal's Algorithm (Minimum Spanning Tree - MST)
 *
 * Problem:
 * Given a connected, undirected, weighted graph,
 * find a spanning tree with minimum total edge weight.
 *
 * MST Properties:
 * - Contains exactly (V - 1) edges
 * - Connects all vertices
 * - Contains NO cycles
 *
 * Core Idea (Greedy):
 * Always pick the smallest edge that does NOT form a cycle.
 *
 * Cycle detection is done using DSU / Union-Find.
 *
 * Time Complexity: O(E log E)
 *   - Sorting edges dominates.
 *
 * Space Complexity: O(V + E)
 */
class KruskalsAlgorithm {

    // Edge representation
    static class Edge {
        int u;      // source vertex
        int v;      // destination vertex
        int weight; // edge weight

        Edge(int u, int v, int weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
    }

    /**
     * Disjoint Set Union (Union-Find)
     *
     * Supports:
     * - find(x): find parent representative of a set
     * - union(a, b): merge sets
     *
     * Optimizations:
     * - Path Compression
     * - Union by Rank
     *
     * Amortized Time: ~ O(α(V)) per operation (almost constant)
     */
    static class DisjointSet {

        private final int[] parent;
        private final int[] rank;

        DisjointSet(int n) {
            parent = new int[n];
            rank = new int[n];

            // initially each node is its own parent (separate set)
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        // Find the representative of the set
        public int find(int node) {
            if (parent[node] != node) {
                parent[node] = find(parent[node]); // path compression
            }
            return parent[node];
        }

        // Union two sets
        public boolean union(int a, int b) {

            int rootA = find(a);
            int rootB = find(b);

            // already in same set => union not possible (cycle would form)
            if (rootA == rootB) return false;

            // union by rank
            if (rank[rootA] < rank[rootB]) {
                parent[rootA] = rootB;
            } else if (rank[rootA] > rank[rootB]) {
                parent[rootB] = rootA;
            } else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }

            return true;
        }
    }

    /**
     * Returns edges that belong to the MST.
     *
     * Steps:
     * 1. Sort edges by weight
     * 2. Use DSU to check if adding an edge forms a cycle
     * 3. Add edge if it connects two different components
     * 4. Stop once we have V-1 edges
     */
    public static List<Edge> findMST(int vertices, List<Edge> edges) {

        // Sort edges by increasing weight
        edges.sort(Comparator.comparingInt(e -> e.weight));

        DisjointSet dsu = new DisjointSet(vertices);

        List<Edge> mst = new ArrayList<>();

        for (Edge edge : edges) {

            // If edge connects 2 different components, take it
            if (dsu.union(edge.u, edge.v)) {
                mst.add(edge);

                // MST always has exactly (V-1) edges
                if (mst.size() == vertices - 1) {
                    break;
                }
            }
        }

        return mst;
    }

    public static void main(String[] args) {

        int vertices = 4;

        List<Edge> edges = List.of(
                new Edge(0, 1, 10),
                new Edge(0, 2, 6),
                new Edge(0, 3, 5),
                new Edge(1, 3, 15),
                new Edge(2, 3, 4)
        );

        List<Edge> mst = findMST(vertices, new ArrayList<>(edges));

        System.out.println("Edges in the Minimum Spanning Tree (Kruskal):");

        int totalWeight = 0;

        for (Edge e : mst) {
            System.out.println(e.u + " -- " + e.v + " : " + e.weight);
            totalWeight += e.weight;
        }

        System.out.println("Total weight of MST: " + totalWeight);
    }
}


/*
 * Kruskal’s Algorithm - Step-by-Step Explanation
 *
 * Goal:
 *   - Find the Minimum Spanning Tree (MST) of a connected, undirected, weighted graph.
 *   - MST includes all vertices with minimum total edge weight and no cycles.
 *
 * Step 1: Sort all edges
 *   - Sort the edges in non-decreasing order of their weight.
 *   - This allows us to consider the smallest available edge first.
 *
 * Step 2: Initialize Disjoint Set Union (Union-Find)
 *   - Each vertex is its own parent (self loop).
 *   - This helps us efficiently detect cycles when adding edges.
 *   - Use path compression and union by rank for optimization.
 *
 * Step 3: Iterate through sorted edges
 *   - For each edge (u, v):
 *       a) Find the root of u and the root of v using `find()`.
 *       b) If roots are different, the edge does not form a cycle:
 *           - Include this edge in the MST.
 *           - Perform `union(u, v)` to merge the sets.
 *       c) If roots are the same, skip the edge (it would create a cycle).
 *   - Continue until MST includes exactly (V - 1) edges.
 *
 * Step 4: Output the MST
 *   - Print the edges included in the MST.
 *   - Print the total weight of the MST.
 *
 * Time Complexity:
 *   - Sorting edges: O(E log E)
 *   - Union-Find operations: O(E α(V)) ≈ O(E), where α is the inverse Ackermann function (very slow-growing)
 *   - Overall: O(E log E), where E = number of edges
 *
 * Space Complexity:
 *   - O(V + E) for storing edges and parent/rank arrays
 *
 * Pros:
 *   - Simple and efficient for sparse graphs
 *   - Easy to implement with Union-Find
 *
 * Cons:
 *   - Needs edge list (not adjacency list/matrix)
 *   - Not efficient for dense graphs compared to Prim’s with a Fibonacci heap
 */

/*
* DETAILED COMPLEXITY ANALYSIS:
* 
* TIME COMPLEXITY:
* 1. SORTING EDGES: O(E log E)
*    - Dominant factor in the algorithm
*    - Since E ≤ V(V-1)/2, we have E log E ≤ V² log V²
*    - Can also be written as O(E log V)
* 
* 2. UNION-FIND INITIALIZATION: O(V)
*    - Create parent and rank arrays
*    - Initialize each element
* 
* 3. PROCESSING EDGES: O(E × α(V))
*    - Process at most E edges
*    - Each edge requires 2 find() operations and possibly 1 union()
*    - find() and union() are O(α(V)) amortized
*    - α(V) ≈ 4 for V ≤ 2^65536 (practically constant)
* 
* 4. OVERALL TIME: O(E log E) + O(V) + O(E × α(V)) = O(E log E)
* 
* SPACE COMPLEXITY:
* 1. INPUT EDGES: O(E) - list of all edges
* 2. UNION-FIND STRUCTURE:
*    - parent[]: O(V)
*    - rank[]: O(V)
* 3. RESULT MST: O(V) - stores V-1 edges
* 4. SORTING: O(log E) additional space for sorting algorithm
* 5. TOTAL SPACE: O(V + E)
* 
* COMPARISON WITH PRIM'S ALGORITHM:
* 
* | Aspect | Prim's | Kruskal's |
* |--------|--------|-----------|
* | Time | O((V + E) log V) | O(E log E) |
* | Space | O(V + E) | O(V + E) |
* | Approach | Vertex-based (grow MST) | Edge-based (sort all edges) |
* | Best for | Dense graphs | Sparse graphs |
* | Data structure | Priority Queue | Union-Find |
* | Implementation | More complex | Simpler conceptually |
* 
* WHEN TO USE KRUSKAL'S:
* - Sparse graphs where E << V²
* - When edges are already sorted or nearly sorted
* - When you need to process edges in weight order anyway
* - Simple implementation requirements
*/
