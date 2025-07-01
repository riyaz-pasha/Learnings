import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CycleDetectionUsingKahns {

    public static void main(String[] args) {
        int V = 4; // Number of vertices
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++)
            graph.add(new ArrayList<>());

        // Directed edges
        graph.get(0).add(1);
        graph.get(1).add(2);
        graph.get(2).add(3);
        graph.get(3).add(1); // Creates a cycle (1 -> 2 -> 3 -> 1)

        if (hasCycle(V, graph)) {
            System.out.println("Cycle Detected");
        } else {
            System.out.println("No Cycle Detected");
        }
    }

    static boolean hasCycle(int V, List<List<Integer>> graph) {
        int[] inDegree = new int[V];

        // Step 1: Calculate in-degree of each node
        for (int u = 0; u < V; u++) {
            for (int v : graph.get(u)) {
                inDegree[v]++;
            }
        }

        // Step 2: Add all nodes with in-degree 0 to the queue
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < V; i++) {
            if (inDegree[i] == 0)
                queue.offer(i);
        }

        int count = 0;

        // Step 3: Process nodes
        while (!queue.isEmpty()) {
            int node = queue.poll();
            count++; // Count of processed nodes

            for (int neighbor : graph.get(node)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Step 4: If not all nodes were processed, cycle exists
        return count != V;
    }

}
