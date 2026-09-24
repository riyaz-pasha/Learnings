import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Network Delay Time
 *
 * We are given a directed graph where:
 *  - Nodes are labeled from 1 to n
 *  - Each edge (u -> v) has a non-negative travel time w
 *  - A signal starts from node k
 *
 * Goal:
 *  - Find the minimum time for the signal to reach ALL nodes
 *  - If any node is unreachable, return -1
 *
 * Key Insight:
 *  - This is a single-source shortest path problem
 *  - All edge weights are non-negative
 *  - Dijkstra’s algorithm is the optimal choice
 */
class NetworkDelayTime {

    /**
     * Edge representation in adjacency list
     * to     -> destination node
     * weight -> travel time
     */
    record Edge(int to, int weight) {}

    /**
     * State used in Dijkstra's priority queue
     * node -> current node
     * dist -> shortest known distance to this node
     */
    record State(int node, int dist) {}

    public int networkDelayTime(int n, int[][] times, int k) {

        /* --------------------------------------------------
         * STEP 1: Build the graph (Adjacency List)
         *
         * graph[u] contains all outgoing edges from u
         * We allocate size n+1 because nodes are 1-indexed
         *
         * Time:  O(E)
         * Space: O(V + E)
         * -------------------------------------------------- */
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : times) {
            int from = edge[0];
            int to = edge[1];
            int weight = edge[2];
            graph.get(from).add(new Edge(to, weight));
        }

        /* --------------------------------------------------
         * STEP 2: Distance array
         *
         * dist[i] = shortest time to reach node i from k
         * Initialize all distances to infinity
         *
         * Space: O(V)
         * -------------------------------------------------- */
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);

        // Distance to source is 0
        dist[k] = 0;

        /* --------------------------------------------------
         * STEP 3: Min-Heap (Priority Queue)
         *
         * Always expands the node with the smallest
         * known distance (Dijkstra's greedy step)
         *
         * Space: O(V)
         * -------------------------------------------------- */
        PriorityQueue<State> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(State::dist));

        minHeap.offer(new State(k, 0));

        /* --------------------------------------------------
         * STEP 4: Dijkstra's Algorithm
         *
         * Each node can be relaxed multiple times,
         * but only the shortest path survives due to pruning
         *
         * Time Complexity:
         *  - Each edge is relaxed at most once with a successful update
         *  - Each push/pop from heap costs log V
         *
         * Total Time: O((V + E) log V)
         * -------------------------------------------------- */
        while (!minHeap.isEmpty()) {
            State current = minHeap.poll();
            int node = current.node();
            int timeSoFar = current.dist();

            // Skip outdated entries
            // If we already found a better path to this node, ignore this one
            if (timeSoFar > dist[node]) {
                continue;
            }

            // Relax all outgoing edges
            for (Edge edge : graph.get(node)) {
                int nextNode = edge.to();
                int newTime = timeSoFar + edge.weight();

                // If a shorter path to nextNode is found, update it
                if (newTime < dist[nextNode]) {
                    dist[nextNode] = newTime;
                    minHeap.offer(new State(nextNode, newTime));
                }
            }
        }

        /* --------------------------------------------------
         * STEP 5: Compute the final answer
         *
         * The signal reaches all nodes when the slowest
         * (maximum distance) node receives it.
         *
         * If any node is unreachable (distance == INF),
         * return -1
         *
         * Time:  O(V)
         * Space: O(1)
         * -------------------------------------------------- */
        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }
}
