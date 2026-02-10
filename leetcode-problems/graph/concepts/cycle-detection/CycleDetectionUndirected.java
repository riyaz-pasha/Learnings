import java.util.ArrayList;
import java.util.List;

/**
 * Cycle Detection in Undirected Graph using DFS
 *
 * Key Idea:
 * In an undirected graph, every edge is bidirectional.
 *
 * Example:
 * If we go from 0 -> 1, then graph also contains 1 -> 0.
 *
 * So while doing DFS, when we see a visited neighbor, we must check:
 * - If that neighbor is NOT the parent, then it means we found a cycle.
 *
 * Why?
 * Because the only visited neighbor that is allowed is the parent
 * (the node we came from).
 *
 * Time Complexity: O(V + E)
 * Space Complexity: O(V) for visited + O(V) recursion stack
 */
public class CycleDetectionUndirected {

    public static void main(String[] args) {

        int V = 5;

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }

        // Example:
        // 0 - 1
        // |  /
        // 2
        //
        // Cycle: 0 -> 1 -> 2 -> 0
        addEdge(graph, 0, 1);
        addEdge(graph, 1, 2);
        addEdge(graph, 2, 0);

        boolean[] visited = new boolean[V];

        // Graph may have multiple components, so run DFS from each node
        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (isCyclic(node, -1, graph, visited)) {
                    System.out.println("Cycle Detected in Undirected Graph");
                    return;
                }
            }
        }

        System.out.println("No Cycle Found");
    }

    // Helper to add undirected edge u <-> v
    private static void addEdge(List<List<Integer>> graph, int u, int v) {
        graph.get(u).add(v);
        graph.get(v).add(u);
    }

    /**
     * DFS cycle detection in undirected graph.
     *
     * @param node   current node being visited
     * @param parent node from which we arrived here
     *
     * If we find a visited neighbor that is NOT parent,
     * then it means there is another path back to that node => cycle exists.
     */
    static boolean isCyclic(int node, int parent,
                            List<List<Integer>> graph,
                            boolean[] visited) {

        visited[node] = true;

        for (int neighbor : graph.get(node)) {

            // If neighbor is not visited, explore it
            if (!visited[neighbor]) {
                if (isCyclic(neighbor, node, graph, visited)) {
                    return true;
                }
            }

            // If neighbor is visited and it is NOT parent,
            // then we found a cycle
            else if (neighbor != parent) {
                return true;
            }
        }

        return false;
    }
}

// Why parent is needed?
//
// In undirected graphs, every edge appears twice (u->v and v->u).
// So when DFS goes from u to v, later v will see u as an already visited neighbor.
// That should NOT be treated as a cycle.
// A cycle exists only if we find a visited neighbor that is NOT the parent.
