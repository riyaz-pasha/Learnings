import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

class TopologicalSortDFS {

    public List<Integer> topoSort(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];
        Stack<Integer> stack = new Stack<>();

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                dfs(adj, visited, stack, node);
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }

    private void dfs(List<List<Integer>> adj, boolean[] visited, Stack<Integer> stack, int node) {
        visited[node] = true;

        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                dfs(adj, visited, stack, neighbor);
            }
        }

        stack.push(node);
    }

}

class TopologicalSortDFS2 {

    public List<Integer> topoSort(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];
        List<Integer> result = new ArrayList<>();

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                dfs(adj, visited, result, node);
            }
        }

        Collections.reverse(result); // reverse to get correct topo order
        return result;
    }

    private void dfs(List<List<Integer>> adj, boolean[] visited, List<Integer> result, int node) {
        visited[node] = true;

        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                dfs(adj, visited, result, neighbor);
            }
        }

        result.add(node); // add after all dependencies are done
    }
}

class TopologicalSortDFS3 {

    public List<Integer> topoSort(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];
        LinkedList<Integer> result = new LinkedList<>();

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                dfs(adj, visited, result, node);
            }
        }

        return result; // already in correct order
    }

    private void dfs(List<List<Integer>> adj, boolean[] visited, LinkedList<Integer> result, int node) {
        visited[node] = true;

        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                dfs(adj, visited, result, neighbor);
            }
        }

        result.addFirst(node); // prepend instead of reversing later
    }
}
