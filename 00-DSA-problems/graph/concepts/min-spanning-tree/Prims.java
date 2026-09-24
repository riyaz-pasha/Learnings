import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Prim's Algorithm (Lazy Version) - Minimum Spanning Tree (MST)
 *
 * Goal:
 * Build MST of a connected, undirected, weighted graph.
 *
 * Greedy Rule:
 * Always pick the minimum weight edge that connects:
 *   (already selected MST nodes) -> (new node outside MST)
 *
 * ---------------------------------------------------------
 * Why PriorityQueue?
 * We need to always pick the smallest edge among all edges
 * that connect MST to non-MST vertices.
 *
 * ---------------------------------------------------------
 * Time Complexity: O(E log E) ~ O(E log V)
 * Space Complexity: O(V + E)
 */
class Prims {

    static List<Edge> primMST(List<List<Edge>> graph, int n) {

        List<Edge> mst = new ArrayList<>();
        boolean[] inMST = new boolean[n];

        int totalWeight = 0;

        // MinHeap based on edge weight
        PriorityQueue<Edge> minHeap = new PriorityQueue<>();

        // ---------------------------------------------------------
        // Step 1: Start from node 0 (can start from any node)
        // ---------------------------------------------------------
        inMST[0] = true;

        // Add all edges from node 0 into heap
        for (Edge edge : graph.get(0)) {
            minHeap.offer(edge);
        }

        // ---------------------------------------------------------
        // Step 2: Expand MST until we have (n-1) edges
        // MST always has exactly (V - 1) edges
        // ---------------------------------------------------------
        while (!minHeap.isEmpty() && mst.size() < n - 1) {

            Edge edge = minHeap.poll();

            // If destination is already inside MST, this edge is useless (cycle edge)
            if (inMST[edge.destination]) {
                continue;
            }

            // Take this edge into MST
            mst.add(edge);
            totalWeight += edge.weight;

            // Mark the new vertex as included
            inMST[edge.destination] = true;

            // Add all outgoing edges from the new node
            for (Edge neighborEdge : graph.get(edge.destination)) {
                if (!inMST[neighborEdge.destination]) {
                    minHeap.offer(neighborEdge);
                }
            }
        }

        System.out.println("Total weight of MST: " + totalWeight);
        return mst;
    }
}

class PrimStepByStep {

    public static void main(String[] args) {

        int V = 5;

        // Adjacency list representation
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }

        // Undirected edges
        addEdge(graph, 0, 1, 2);
        addEdge(graph, 0, 3, 6);
        addEdge(graph, 1, 2, 3);
        addEdge(graph, 1, 3, 8);
        addEdge(graph, 1, 4, 5);
        addEdge(graph, 2, 4, 7);
        addEdge(graph, 3, 4, 9);

        List<Edge> mst = Prims.primMST(graph, V);

        System.out.println("Edges in MST:");
        for (Edge e : mst) {
            System.out.println(e.source + " -- " + e.destination + " : " + e.weight);
        }
    }

    // Add undirected edge u <-> v
    static void addEdge(List<List<Edge>> graph, int source, int destination, int weight) {
        graph.get(source).add(new Edge(source, destination, weight));
        graph.get(destination).add(new Edge(destination, source, weight));
    }
}

class Edge implements Comparable<Edge> {

    final int source;
    final int destination;
    final int weight;

    public Edge(int source, int destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    // MinHeap ordering by weight
    @Override
    public int compareTo(Edge other) {
        return Integer.compare(this.weight, other.weight);
    }
}

/*
 * Why Prim’s algorithm works?
 *
 * At any point, we have a set of vertices already in MST.
 * The minimum weight edge that connects MST to a new vertex
 * is always safe to include (Cut Property).
 *
 * This greedy step never breaks optimality.
 */


/*
 * Prim’s Algorithm - Step-by-Step Explanation
 *
 * Goal:
 *   - Find the Minimum Spanning Tree (MST) of a connected, undirected, weighted graph.
 *   - MST connects all vertices with the minimum total edge weight and no cycles.
 *
 * Step 1: Initialize
 *   - Create a `minHeap` (priority queue) to always pick the edge with the smallest weight.
 *   - Create a `visited[]` array to track included vertices in the MST.
 *   - Start from an arbitrary node (e.g., vertex 0):
 *       - Push all edges from this vertex into the `minHeap`.
 *
 * Step 2: Build MST using Priority Queue
 *   - While `minHeap` is not empty and MST is incomplete:
 *       a) Extract the minimum weight edge from the heap.
 *       b) If the destination vertex is already visited, skip it.
 *       c) Else:
 *           - Include the edge in the MST.
 *           - Mark the destination vertex as visited.
 *           - Push all adjacent edges of the new vertex (to unvisited neighbors) into the heap.
 *
 * Step 3: Output
 *   - After all vertices are visited (MST has V - 1 edges):
 *       - Print the edges of the MST.
 *       - Print the total weight.
 *
 * Time Complexity: O(E log V)
 *   - E = number of edges, V = number of vertices
 *   - PriorityQueue operations are log V
 *
 * Space Complexity: O(V + E)
 *   - For visited[], priority queue, and adjacency list
 *
 * Pros:
 *   - Efficient for dense graphs when implemented with a min-heap
 *   - Builds MST incrementally from any starting node
 *
 * Cons:
 *   - Requires a priority queue and adjacency list representation
 *   - Slightly more complex than Kruskal for edge-based logic
 */

/*
* DETAILED COMPLEXITY ANALYSIS:
* 
* TIME COMPLEXITY:
* 1. INITIALIZATION: O(V)
*    - Arrays.fill(): O(V)
*    - Initial pq.offer(): O(1)
* 
* 2. MAIN LOOP: Exactly V iterations
*    - Each vertex is added to MST exactly once
*    - Loop continues until MST has V-1 edges
* 
* 3. VERTEX EXTRACTIONS: V times, each O(log V)
*    - pq.poll(): O(log V)
*    - Total: O(V log V)
* 
* 4. EDGE PROCESSING: Each edge processed twice (undirected graph)
*    - Total edge examinations: 2E
*    - For each beneficial edge update: pq.offer() = O(log V)
*    - Total: O(E log V)
* 
* 5. FINAL TIME COMPLEXITY: O(V) + O(V log V) + O(E log V) = O((V + E) log V)
* 
* SPACE COMPLEXITY:
* 1. INPUT GRAPH: O(V + E) - adjacency list representation
* 2. ALGORITHM ARRAYS: 
*    - inMST[]: O(V)
*    - parent[]: O(V)  
*    - key[]: O(V)
* 3. PRIORITY QUEUE: O(E) worst case - might store one entry per edge
* 4. RESULT MST: O(V) - stores V-1 edges
* 5. TOTAL SPACE: O(V + E)
* 
* WHEN TO USE DIFFERENT IMPLEMENTATIONS:
* - Dense graphs (E ≈ V²): Array-based O(V²) is better
* - Sparse graphs (E << V²): Binary heap O((V + E) log V) is better
* - Theoretical optimum: Fibonacci heap O(E + V log V)
*/

/**
 * ===================== Prim's Algorithm – Time & Space Complexity =====================
 *
 * This implementation uses:
 *  - Adjacency List representation
 *  - Min-Heap (PriorityQueue) to select the minimum weight edge
 *
 * Definitions:
 *  V = Number of vertices (nodes)
 *  E = Number of edges
 *
 * -------------------------------------------------------------------------------------
 * IMPORTANT NOTE: log(E) vs log(V) CONFUSION (VERY COMMON)
 * -------------------------------------------------------------------------------------
 *
 * - PriorityQueue operations (offer / poll) take:
 *      O(log N), where N = number of elements in the heap.
 *
 * - In Prim’s algorithm, the heap stores EDGES (not vertices),
 *   so the heap size can grow up to O(E).
 *
 * - Therefore, each heap operation technically costs:
 *      O(log E)
 *
 * - HOWEVER:
 *      In any simple graph,
 *          E ≤ V²
 *
 *      Taking logarithm on both sides:
 *          log E ≤ log(V²)
 *          log E ≤ 2 log V
 *
 * - Since constants are ignored in Big-O notation:
 *      log E ≈ log V
 *
 * - That is why we WRITE:
 *      O(E log V)
 *   even though internally the heap operates on edges.
 *
 * 👉 KEY TAKEAWAY:
 *    "Heap stores edges → log E
 *     But E is bounded by V² → log E collapses to log V"
 *
 * -------------------------------------------------------------------------------------
 * TIME COMPLEXITY
 * -------------------------------------------------------------------------------------
 *
 * 1. Initialization:
 *    - inMST[] array: O(V)
 *    - PriorityQueue creation: O(1)
 *
 * 2. Heap Insertions (offer):
 *    - Each edge is inserted at most once.
 *    - Total insertions: O(E)
 *    - Cost per insertion: O(log E) ≈ O(log V)
 *
 * 3. Heap Removals (poll):
 *    - Each edge is removed at most once.
 *    - Total removals: O(E)
 *    - Cost per removal: O(log E) ≈ O(log V)
 *
 * 4. Neighbor Exploration:
 *    - All adjacency lists combined are traversed once.
 *    - Cost: O(E)
 *
 * Final Time Complexity:
 *    O(E log V)
 *
 * -------------------------------------------------------------------------------------
 * SPACE COMPLEXITY
 * -------------------------------------------------------------------------------------
 *
 * 1. Adjacency List:
 *    - Stores all vertices and edges.
 *    - Space: O(V + E)
 *
 * 2. inMST[] array:
 *    - Tracks whether a vertex is included in MST.
 *    - Space: O(V)
 *
 * 3. PriorityQueue:
 *    - Can contain up to O(E) edges in worst case.
 *    - Space: O(E)
 *
 * 4. MST Edge List:
 *    - Stores exactly (V - 1) edges.
 *    - Space: O(V)
 *
 * Final Space Complexity:
 *    O(V + E)
 *
 * -------------------------------------------------------------------------------------
 * NOTES
 * -------------------------------------------------------------------------------------
 * - This is the "Lazy" version of Prim’s algorithm.
 * - Edges leading to already visited vertices are skipped during polling.
 * - Suitable for sparse graphs.
 * - Dense graphs are better handled with adjacency matrix (O(V²)).
 *
 * -------------------------------------------------------------------------------------
 */
