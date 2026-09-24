import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Implements Prim's Algorithm to find the Minimum Spanning Tree (MST)
 * for a connected, weighted, undirected graph.
 */
class MinimumSpanningTreePrims {

    /**
     * Represents a weighted edge in the graph.
     * 
     * @param src    The source node.
     * @param dst    The destination node.
     * @param weight The weight (cost) of the edge.
     */
    public record Edge(int src, int dst, int weight) {
    };

    /**
     * Computes the Minimum Spanning Tree using Prim's algorithm.
     * * @param n The number of nodes in the graph (0 to n-1).
     * 
     * @param graph The adjacency list representation of the graph.
     * @return A list of edges forming the MST.
     */
    public List<Edge> minimumSpanningTree(int n, List<List<Edge>> graph) {

        List<Edge> mstEdges = new ArrayList<>();
        // Keep track of nodes already included in the MST
        boolean[] inMST = new boolean[n];

        // Min-Heap to store available edges, prioritized by weight
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparing(Edge::weight));

        // Start Prim's algorithm from node 0 (can be any node)
        int startNode = 0;
        inMST[startNode] = true;

        // Initial population of the PriorityQueue with all edges from the start node
        for (Edge edge : graph.get(startNode)) {
            // Add the neighbor edge to the PQ
            pq.offer(edge);
        }

        // Main loop to build the MST
        while (!pq.isEmpty()) {
            Edge currentEdge = pq.poll();

            // Check if the destination node is already in the MST (prevents cycles)
            if (inMST[currentEdge.dst()]) {
                continue;
            }

            // Include the node and the edge in the MST
            inMST[currentEdge.dst()] = true;
            mstEdges.add(currentEdge);

            // Explore all neighbors of the newly added node
            int newNode = currentEdge.dst();
            for (Edge neighbor : graph.get(newNode)) {
                // Only consider edges leading to nodes NOT yet in the MST
                if (!inMST[neighbor.dst()]) {
                    pq.offer(neighbor);
                }
            }
        }

        return mstEdges;
    }

}

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
