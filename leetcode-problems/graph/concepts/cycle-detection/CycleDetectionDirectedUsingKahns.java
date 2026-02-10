import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Cycle Detection in Directed Graph using Kahn's Algorithm (Topological Sort -
 * BFS)
 *
 * Key Idea:
 * - A Directed Acyclic Graph (DAG) always has a valid topological ordering.
 * - Kahn's algorithm generates topological order by repeatedly removing nodes
 * with indegree = 0.
 *
 * If a cycle exists:
 * - Nodes inside the cycle will never reach indegree = 0
 * - So they can never be removed
 * - Therefore, we will NOT be able to process all vertices
 *
 * If processedCount < V => cycle exists.
 *
 * Time Complexity: O(V + E)
 * Space Complexity: O(V)
 */
public class CycleDetectionDirectedUsingKahns {

    public static boolean hasCycle(List<List<Integer>> graph) {

        int V = graph.size();

        int[] indegree = new int[V];

        // Step 1: Compute indegree of every node
        // indegree[v] = number of incoming edges into v
        for (int u = 0; u < V; u++) {
            for (int v : graph.get(u)) {
                indegree[v]++;
            }
        }

        // Step 2: Add all nodes with indegree = 0 into queue
        // These nodes can safely come first in topological ordering.
        Queue<Integer> queue = new ArrayDeque<>();

        for (int node = 0; node < V; node++) {
            if (indegree[node] == 0) {
                queue.offer(node);
            }
        }

        int processedCount = 0;

        // Step 3: Process nodes in BFS order
        while (!queue.isEmpty()) {

            int node = queue.poll();
            processedCount++;

            // Remove outgoing edges node -> neighbor
            // That means reduce indegree of neighbors
            for (int neighbor : graph.get(node)) {

                indegree[neighbor]--;

                // If indegree becomes 0, neighbor is now "free" to be processed
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Step 4:
        // If we processed all nodes => graph is DAG (no cycle)
        // If some nodes were not processed => those nodes are part of a cycle
        return processedCount != V;
    }

    public static void main(String[] args) {

        int V = 4;

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }

        // Graph:
        // 0 -> 1 -> 2 -> 3
        // ^ |
        // |_________|
        graph.get(0).add(1);
        graph.get(1).add(2);
        graph.get(2).add(3);
        graph.get(3).add(1); // cycle edge

        System.out.println(hasCycle(graph) ? "Cycle Detected" : "No Cycle Detected");
    }
}

// Why processedCount != V means cycle?
//
// In a DAG, there is always at least one node with indegree = 0.
// Kahn's algorithm repeatedly removes such nodes and reduces indegrees.
//
// If a cycle exists, nodes in that cycle will always have indegree >= 1
// (because they point to each other).
// So they will never enter the queue.
//
// Therefore, BFS cannot process all vertices.
// processedCount < V => cycle exists.


/*
 * Why normal BFS cannot detect cycles in a Directed Graph?
 *
 * BFS uses only visited[] to avoid revisiting nodes.
 * But in a directed graph, reaching an already visited node does NOT always mean cycle.
 *
 * Example (NO cycle):
 * 0 -> 1
 * 0 -> 2
 * 2 -> 1
 *
 * BFS from 0 visits 1 first, then later reaches 1 again via 2.
 * Node 1 is already visited, but there is no cycle.
 *
 * So "visited neighbor" is NOT a valid condition for cycle detection in directed graphs.
 *
 * To correctly detect cycle we need:
 *
 * 1) DFS + recursion stack (inPath[]):
 *    - Detects BACK EDGE (edge to a node currently in recursion path).
 *    - If neighbor is inPath => cycle exists.
 *
 * 2) Kahn’s Algorithm (Topological Sort using indegree):
 *    - A DAG always has a topological ordering.
 *    - If we cannot process all nodes (processedCount < V),
 *      then a cycle exists.
 */
