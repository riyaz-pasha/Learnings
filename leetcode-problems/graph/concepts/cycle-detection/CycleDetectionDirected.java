import java.util.ArrayList;
import java.util.List;

public class CycleDetectionDirected {

    public static void main(String[] args) {
        int V = 4;
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < V; i++)
            graph.add(new ArrayList<>());

        // Example: 0 -> 1 -> 2 -> 3 -> 1 (cycle)
        graph.get(0).add(1);
        graph.get(1).add(2);
        graph.get(2).add(3);
        graph.get(3).add(1);

        boolean[] visited = new boolean[V];
        boolean[] recStack = new boolean[V];

        for (int i = 0; i < V; i++) {
            if (isCyclic(i, graph, visited, recStack)) {
                System.out.println("Cycle Detected in Directed Graph");
                return;
            }
        }

        System.out.println("No Cycle Found");
    }

    static boolean isCyclic(int node, List<List<Integer>> graph, boolean[] visited, boolean[] recStack) {
        if (recStack[node])
            return true;
        if (visited[node])
            return false;

        visited[node] = true;
        recStack[node] = true;

        for (int neighbor : graph.get(node)) {
            if (isCyclic(neighbor, graph, visited, recStack))
                return true;
        }

        recStack[node] = false;
        return false;
    }

}
