import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

class Prims {

    static List<Edge> primMST(List<List<Edge>> graph, int n) {
        List<Edge> mst = new ArrayList<>();
        boolean[] visited = new boolean[n];
        int totalWeight = 0;
        PriorityQueue<Edge> minHeap = new PriorityQueue<>();

        visited[0] = true;
        for (Edge edge : graph.get(0)) {
            minHeap.offer(edge);
        }

        while (!minHeap.isEmpty()) {
            Edge edge = minHeap.poll();
            if (visited[edge.destination]) {
                continue;
            }
            visited[edge.destination] = true;
            mst.add(edge);
            totalWeight += edge.weight;
            for (Edge neighborEdge : graph.get(edge.destination)) {
                if (!visited[neighborEdge.destination]) {
                    minHeap.offer(neighborEdge);
                }
            }
        }
        System.out.println("Total weight : " + totalWeight);
        return mst;
    }

}

class PrimStepByStep {

    public static void main(String[] args) {
        int V = 5; // Number of vertices

        // Step 1: Create an adjacency list to represent the undirected graph
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }

        // Add undirected edges
        addEdge(graph, 0, 1, 2);
        addEdge(graph, 0, 3, 6);
        addEdge(graph, 1, 2, 3);
        addEdge(graph, 1, 3, 8);
        addEdge(graph, 1, 4, 5);
        addEdge(graph, 2, 4, 7);
        addEdge(graph, 3, 4, 9);

        // Run Prim's Algorithm
        Prims.primMST(graph, V);
    }

    // Utility function to add an undirected edge
    static void addEdge(List<List<Edge>> graph, int source, int destination, int weight) {
        graph.get(source).add(new Edge(source, destination, weight));
        graph.get(destination).add(new Edge(destination, source, weight));
    }

}

class Edge implements Comparable<Edge> {

    final int source, destination, weight;

    public Edge(int source, int destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge o) {
        return this.weight - o.weight;
    }

}

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
