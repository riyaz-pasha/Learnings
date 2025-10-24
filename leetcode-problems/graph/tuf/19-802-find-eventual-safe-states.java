import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class EventualSafeStates {

    public List<Integer> eventualSafeNodes(int[][] graph) {
        List<List<Integer>> reverseGraph = new ArrayList<>();
        int n = graph.length;
        int[] indegree = new int[n];
        for (int node = 0; node < n; node++) {
            reverseGraph.add(new ArrayList<>());
        }

        for (int node = 0; node < n; node++) {
            for (int neighbor : graph[node]) {
                reverseGraph.get(neighbor).add(node);
                indegree[node]++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int node = 0; node < n; node++) {
            if (indegree[node] == 0) {
                queue.offer(node);
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(current);

            for (int neighbor : reverseGraph.get(current)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        Collections.sort(result);
        return result;
    }

}

class SolutionKahn {
    public List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        List<List<Integer>> rev = new ArrayList<>(n);
        int[] outdegree = new int[n];

        for (int i = 0; i < n; i++)
            rev.add(new ArrayList<>());
        for (int u = 0; u < n; u++) {
            outdegree[u] = graph[u].length;
            for (int v : graph[u]) {
                // reverse edge v -> u
                rev.get(v).add(u);
            }
        }

        Queue<Integer> q = new LinkedList<>();
        // start with terminal nodes (outdegree 0)
        for (int i = 0; i < n; i++) {
            if (outdegree[i] == 0)
                q.offer(i);
        }

        boolean[] safe = new boolean[n];
        while (!q.isEmpty()) {
            int node = q.poll();
            safe[node] = true;
            for (int pred : rev.get(node)) {
                outdegree[pred]--; // removing node reduces pred's outdegree
                if (outdegree[pred] == 0)
                    q.offer(pred);
            }
        }

        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (safe[i])
                res.add(i);
        }
        return res;
    }
    /*
     * Complexity: O(V + E) time, O(V + E) extra space for reversed graph and queue.
     */
}

class Solution {
    public List<Integer> eventualSafeNodes(int[][] graph) {
        int n = graph.length;
        int[] state = new int[n]; // 0: unvisited, 1: visiting, 2: safe
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (isSafe(i, graph, state)) {
                result.add(i);
            }
        }

        return result;
    }

    private boolean isSafe(int node, int[][] graph, int[] state) {
        if (state[node] != 0) {
            return state[node] == 2; // Return true if safe, false if visiting (cycle)
        }

        state[node] = 1; // Mark as visiting

        for (int neighbor : graph[node]) {
            if (!isSafe(neighbor, graph, state)) {
                return false; // Found a cycle or unsafe path
            }
        }

        state[node] = 2; // Mark as safe
        return true;
    }
}
