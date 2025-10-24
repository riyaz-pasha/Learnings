
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class DetectCycleDirectedGraphBFS {

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        // compute indegree for every node
        // push all nodes with indegree 0 to the queue
        // pop node from queue
        // iterate neighbor nodes and decrement indegree count of neighbors
        // if any neighbors indegree becomes 0 then add it to the queue
        // repeat this.
        // if we processed all v nodes then no cycle else cycle exists
        // TC-O(V+E) SC-O(V)

        int[] indegree = new int[V];

        for (int node = 0; node < V; node++) {
            for (int neighbor : adj.get(node)) {
                indegree[neighbor]++;
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int node = 0; node < V; node++) {
            if (indegree[node] == 0) {
                queue.offer(node);
            }
        }

        int count = 0;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            count++;

            for (int neighbor : adj.get(current)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return count != V;
    }
    /*
     * 🧮 Complexity
     * Time: O(V + E) — every node and edge processed once.
     * Space: O(V) — for queue and indegree array.
     */

}
