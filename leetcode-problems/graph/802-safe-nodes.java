import java.util.ArrayList;
import java.util.List;

class SafeStateNodes {
    public List<Integer> eventualSafeNodes(int[][] graph) {
        List<Integer> safeNodes = new ArrayList<>();
        int[] visitState = new int[graph.length]; // 0-unvisited , 1 -> visiting, 2-> visited

        for (int i = 0; i < graph.length; i++) {
            if (dfs(graph, i, visitState)) {
                safeNodes.add(i);
            }
        }
        return safeNodes.stream().distinct().sorted().toList();
    }

    private boolean dfs(int[][] graph, int node, int[] visitState) {
        if (visitState[node] != 0) {
            return visitState[node] == 2;
        }
        visitState[node] = 1;

        for (Integer neighbor : graph[node]) {
            if (visitState[neighbor] == 1 || !dfs(graph, neighbor, visitState)) {
                return false;
            }
        }

        visitState[node] = 2;
        return true;
    }
}
