import java.util.ArrayList;
import java.util.List;

public class CycleDetectionUndirected {

    public static void main(String[] args) {
        int V = 5;
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++)
            graph.add(new ArrayList<>());

        // Example: 0-1-2-0 forms a cycle
        graph.get(0).add(1);
        graph.get(1).add(0);
        graph.get(1).add(2);
        graph.get(2).add(1);
        graph.get(2).add(0);
        graph.get(0).add(2);

        boolean[] visited = new boolean[V];

        for (int i = 0; i < V; i++) {
            if (!visited[i] && isCyclic(i, -1, graph, visited)) {
                System.out.println("Cycle Detected in Undirected Graph");
                return;
            }
        }

        System.out.println("No Cycle Found");
    }

    static boolean isCyclic(int node, int parent, List<List<Integer>> graph, boolean[] visited) {
        visited[node] = true;

        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                if (isCyclic(neighbor, node, graph, visited))
                    return true;
            } else if (neighbor != parent) {
                return true;
            }
        }
        return false;
    }

}
