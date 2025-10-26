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
