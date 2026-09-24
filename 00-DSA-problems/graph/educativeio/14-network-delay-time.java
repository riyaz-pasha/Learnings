/**
 * ============================================================================
 * NETWORK DELAY TIME - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Can the delay times (weights) be negative?
 *    -> Why: Determines if Dijkstra's algorithm is safe to use. Negative weights require Bellman-Ford. (Constraint says >= 0).
 * Q: Are all nodes guaranteed to be connected?
 *    -> Why: Tells us if we need to handle "unreachable" nodes. (Prompt says return -1 if not possible).
 * Q: Are there multiple edges between the same two nodes?
 *    -> Why: If so, we'd need to pre-process the graph to only keep the shortest edge between them to save time. (Constraint says unique pairs).
 * Q: If multiple signals arrive at a node, which one matters?
 *    -> Why: Clarifies the physical model. The signal is received the moment the *first* (fastest) signal arrives.
 * Q: Are the node labels 0-indexed or 1-indexed?
 *    -> Why: Prevents off-by-one errors when initializing graph arrays. (Constraint says 1 to n).
 *
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to find the shortest path from a single source node `k` to *all* other nodes 
 * in a weighted directed graph. Because the signal travels simultaneously along all edges, the time it takes 
 * for *everyone* to get the signal is simply the maximum of these shortest paths. The binding constraint is 
 * that edges have varying weights, meaning a path with more nodes might take less time than a path with fewer nodes.
 *
 * Approach 1: Depth-First Search (DFS) / Backtracking
 * - What I'd try: Start at `k`. Recursively explore every outgoing edge. Maintain a `dist` array. If I reach a 
 *   node and my current elapsed time is strictly less than `dist[node]`, I update it and continue DFS from there.
 * - Why it works: It exhaustively explores every possible path, guaranteeing we eventually find the shortest ones.
 * - Why it's too slow: It eagerly dives deep into paths. If it takes a long, slow path first, it does a massive 
 *   amount of work exploring downstream nodes, only to realize later that a shorter path exists, forcing it to 
 *   re-explore that entire downstream subtree.
 * - Time Complexity: O(N!) — because in a dense graph, we end up exploring every possible permutation of paths 
 *   between nodes, leading to combinatorial explosion.
 * - Space Complexity: O(N + E) — because we store the graph as an adjacency list O(E), and the recursion stack 
 *   can go O(N) deep in the worst case (a single linear chain).
 *
 * Approach 2: Breadth-First Search (Queue) / SPFA (Shortest Path Faster Algorithm)
 * - What work is being repeated in DFS: We explore deep subtrees with sub-optimal starting times.
 * - What property removes the bottleneck: Exploring level-by-level (by edge count) is generally safer. We can 
 *   use a standard Queue. We push `(node, time)` and only push neighbors if we find a strictly shorter time.
 * - What I'd try: Start with `queue = [k]`, `dist[k] = 0`. Pop a node, check all its neighbors. If `dist[curr] + weight < dist[neighbor]`, 
 *   update `dist[neighbor]` and push the neighbor back into the queue.
 * - Why it works: It aggressively prunes paths that are clearly worse than what we've already found.
 * - Why it's too slow (for some graphs): A standard queue processes nodes based on "fewest edges", not "least time". 
 *   We could still reach a node in 2 edges (taking 100 seconds), process its neighbors, and later reach it in 
 *   5 edges (taking 10 seconds), forcing us to re-queue it and re-evaluate its neighbors.
 * - Time Complexity: O(N * E) — because in the worst-case (a graph crafted to trick BFS), a node might be pushed 
 *   back into the queue up to N-1 times, leading to repeated relaxations of its edges.
 * - Space Complexity: O(N + E) — because we store the adjacency list O(E), distance array O(N), and the queue O(N).
 *
 * Approach 3: Dijkstra's Algorithm (The Optimum)
 * - What property removes the SPFA bottleneck: If we always process the node with the absolute *smallest total time 
 *   seen so far*, we are guaranteed that its shortest path is permanently locked in. Why? Because all edge weights 
 *   are >= 0. No other path could possibly reach this node faster, because any detour would have to go through a 
 *   node that currently has an equal or greater time.
 * - What I'd try: Swap the standard `Queue` for a `PriorityQueue` (Min-Heap) ordered by total elapsed time. 
 *   Pop the smallest, lock in its time, and push its neighbors to the heap.
 * - Time Complexity: O(E log E) [or O(E log N)] — because each edge is processed exactly once, and pushing/popping 
 *   from the PriorityQueue takes logarithmic time. The heap can grow to size E if we don't aggressively remove 
 *   duplicates (lazy deletion).
 * - Space Complexity: O(N + E) — because we store the adjacency list O(E), the distance array O(N), and the 
 *   PriorityQueue can hold at most O(E) elements.
 * - Decision: I would write Approach 3 (Dijkstra). It is the canonical solution for Single-Source Shortest Path 
 *   with non-negative weights, avoids the worst-case O(N*E) of SPFA, and demonstrates mastery of heaps.
 *
 * 
 * 3. EDGE CASES
 * ------------------------------------------------
 * - Disconnected Graph / Unreachable nodes: Some nodes have no incoming paths. The `dist` array will remain at 
 *   INFINITY for them, triggering the -1 return condition.
 * - N = 1 (Technically excluded by constraint 1 <= k <= n <= 100 but logically valid): Returns 0, as the source 
 *   already has the signal.
 * - Dead-ends: Nodes with incoming edges but no outgoing edges. Handled naturally; they just don't add anything 
 *   to the queue.
 * - Cyclic graphs: Dijkstra naturally breaks cycles because going in a circle strictly increases time, so the 
 *   `time < dist[node]` check will fail, preventing infinite loops.
 * - 0-weight edges: Valid. The PriorityQueue will instantly pop these (0 delay), which logically simulates 
 *   instantaneous transmission.
 *
 * 
 * 4. KEY INSIGHT & DRY RUN
 * ------------------------------------------------
 * Key Insight: A standard BFS queue expands uniformly by *number of edges*. A PriorityQueue BFS expands uniformly 
 * by *elapsed time*. When edge weights vary, time is the only metric that matters.
 *
 * ASCII Diagram:
 * Consider k = 1.
 *         (4)
 *    1 ---------> 3
 *     \           ^
 *   (1)\         /(1)
 *       v       /
 *         2 ---
 *
 * Standard BFS might visit 3 directly from 1 (Time: 4), then visit 2 (Time: 1). 
 * Then from 2, it visits 3 again (Time: 1 + 1 = 2). It has to re-update node 3!
 * Dijkstra visits by absolute time:
 *
 * Dry Run (Dijkstra):
 * Initial: dist = [∞, ∞, ∞, ∞], PQ = [(node:1, time:0)]
 * 1. Pop (1, 0). dist[1] = 0.
 *    - Neighbors of 1:
 *      - Node 3, weight 4 -> dist[3] = 0+4 = 4. Push (3, 4).
 *      - Node 2, weight 1 -> dist[2] = 0+1 = 1. Push (2, 1).
 *    - PQ state: [(2, 1), (3, 4)]
 *
 * 2. Pop (2, 1). dist[2] = 1.  <-- Dijkstra picks node 2 because 1 < 4!
 *    - Neighbors of 2:
 *      - Node 3, weight 1 -> new time = 1+1 = 2.
 *      - 2 is < dist[3] (which is 4). Update dist[3] = 2. Push (3, 2).
 *    - PQ state: [(3, 2), (3, 4)]
 *
 * 3. Pop (3, 2). dist[3] = 2.
 *    - No outgoing edges.
 *    - PQ state: [(3, 4)]
 *
 * 4. Pop (3, 4). Current time 4 is NOT < dist[3] (which is 2).
 *    - Ignore (Lazy Deletion).
 *    - PQ state: []
 * 
 * Result array: [∞, 0, 1, 2]. Max is 2 (excluding index 0). Answer = 2.
 *
 *
 * 5. PITFALLS & PATTERN RECOGNITION
 * ------------------------------------------------
 * - Pitfall 1: Failing to initialize the `dist` array to a sufficiently large number (like `Integer.MAX_VALUE`).
 * - Pitfall 2: Using a `Set` to track visited nodes but adding them to the set when *pushed* to the queue. In 
 *   Dijkstra, a node is only finalized when it is *popped* from the priority queue. (In practice, tracking 
 *   `time < dist[node]` handles visited states perfectly without needing a Set).
 * - Pitfall 3: Not accounting for disconnected nodes. You must verify that the maximum value in `dist` is not 
 *   still infinity at the end.
 *
 * Pattern Recognition: "Find the minimum time/cost to reach all destinations" -> Single Source Shortest Path.
 * - When weights are non-negative -> Dijkstra (Priority Queue).
 * - When weights can be negative -> Bellman-Ford.
 * - When the graph is an unweighted grid -> Standard BFS (Queue).
 * - Similar Problems: "Path With Maximum Probability", "Cheapest Flights Within K Stops" (Modified Dijkstra).
 *
 * 
 * 6. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the delay times could be negative? (e.g., a time-travel anomaly?)
 *  -> Dijkstra's algorithm would fail because it assumes a popped node's path is permanently optimal. We would 
 *     have to use the Bellman-Ford algorithm, which relaxes all edges V-1 times. Time complexity would shift 
 *     to O(N * E).
 * 
 * F2: What if the graph is extremely dense, meaning almost every node is connected to every other node (E ≈ V^2)?
 *  -> A PriorityQueue adds O(log E) overhead to every edge relaxation. For extremely dense graphs, a traditional 
 *     array-based Dijkstra (where we manually scan a boolean visited array to find the minimum unvisited node) 
 *     takes O(V^2) time, which can actually outperform the O(E log E) -> O(V^2 log V) heap implementation.
 *
 * F3: What if we needed to know the actual path the signal took to reach the slowest node?
 *  -> We would maintain a `parent[]` array. Whenever we successfully update `dist[neighbor]`, we also set 
 *     `parent[neighbor] = currNode`. At the end, we backtrack from the slowest node using the `parent` array 
 *     to reconstruct the path.
 *
 *
 * 7. SUMMARY
 * ------------------------------------------------
 * Core pattern: Single Source Shortest Path (SSSP) via Dijkstra's Algorithm.
 * Key observation: The total time for *all* nodes to receive the signal is the maximum of their individual 
 *                  shortest-path times from the source.
 * Most common trap: Popping a "stale" state from the Priority Queue without checking if we've already found a 
 *                   better path to that node. (Solved via lazy deletion: `if (currTime > dist[currNode]) continue;`)
 * One-line mental trigger: "Shortest path with weighted edges? Min-Heap Dijkstra."
 */

import java.util.*;

public class NetworkDelayTime {

    /**
     * OPTIMAL APPROACH: Dijkstra's Algorithm
     * Time Complexity: O(E log E) — Each of the E edges is processed at most once. Pushing/popping from 
     *                  the PriorityQueue takes O(log E) since it can grow to size E.
     * Space Complexity: O(N + E) — Adjacency list takes O(E). Distance array takes O(N). 
     *                   PriorityQueue takes O(E) in the worst case (lazy deletion).
     */
    public int networkDelayTimeDijkstra(int[][] times, int n, int k) {
        // Step 1: Build the Adjacency List
        // Map<SourceNode, List<int[]{TargetNode, DelayTime}>>
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            graph.put(i, new ArrayList<>());
        }
        for (int[] edge : times) {
            int u = edge[0]; // source
            int v = edge[1]; // target
            int w = edge[2]; // weight / delay time
            graph.get(u).add(new int[]{v, w});
        }

        // Step 2: Initialize distance array
        // We use an array of size n+1 because node labels are 1-indexed.
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0; // Distance to the start node is 0

        // Step 3: Initialize Min-Heap (PriorityQueue)
        // PQ holds int arrays: {node, current_total_time}
        // Ordered by current_total_time (index 1) in ascending order.
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{k, 0});

        // Step 4: Process the Priority Queue
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int currNode = current[0];
            int currTime = current[1];

            // Optimization (Lazy Deletion): 
            // If we've already found a strictly shorter path to currNode before pulling this 
            // specific state out of the PQ, we discard this stale state.
            if (currTime > dist[currNode]) {
                continue;
            }

            // Relax all outgoing edges
            for (int[] neighborEdge : graph.get(currNode)) {
                int nextNode = neighborEdge[0];
                int travelTime = neighborEdge[1];
                
                // If the path strictly through currNode is faster than the best known time to nextNode...
                if (currTime + travelTime < dist[nextNode]) {
                    dist[nextNode] = currTime + travelTime;
                    pq.offer(new int[]{nextNode, dist[nextNode]});
                }
            }
        }

        // Step 5: Find the maximum time across all nodes
        int maxTime = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) {
                // If any node is still sitting at infinity, it means it's unreachable.
                return -1;
            }
            maxTime = Math.max(maxTime, dist[i]);
        }

        return maxTime;
    }


    /**
     * ALTERNATIVE APPROACH: SPFA (Shortest Path Faster Algorithm) / BFS with Queue
     * Included to demonstrate the intermediate evolutionary step. Valid, but mathematically slower.
     * Time Complexity: O(N * E) — A node can be re-added to the queue multiple times if shorter paths are found.
     * Space Complexity: O(N + E) — Adjacency list O(E) + Distance array O(N) + Queue O(N).
     */
    public int networkDelayTimeSPFA(int[][] times, int n, int k) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int i = 1; i <= n; i++) graph.put(i, new ArrayList<>());
        for (int[] edge : times) graph.get(edge[0]).add(new int[]{edge[1], edge[2]});

        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        // Standard FIFO Queue instead of PriorityQueue
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(k);

        while (!queue.isEmpty()) {
            int currNode = queue.poll();

            for (int[] neighbor : graph.get(currNode)) {
                int nextNode = neighbor[0];
                int travelTime = neighbor[1];

                if (dist[currNode] + travelTime < dist[nextNode]) {
                    dist[nextNode] = dist[currNode] + travelTime;
                    // Push to queue to evaluate its downstream neighbors with this new shorter time
                    queue.offer(nextNode);
                }
            }
        }

        int max = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) return -1;
            max = Math.max(max, dist[i]);
        }
        return max;
    }

    
    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        NetworkDelayTime solution = new NetworkDelayTime();

        // Test Case 1: Standard Example (From ASCII diagram in notes)
        // 1->3 (Time 4), 1->2 (Time 1), 2->3 (Time 1)
        int[][] times1 = {{1, 3, 4}, {1, 2, 1}, {2, 3, 1}};
        int n1 = 3;
        int k1 = 1;
        System.out.println("Test Case 1 (Expected 2): " + solution.networkDelayTimeDijkstra(times1, n1, k1));

        // Test Case 2: Disconnected graph (Node 4 is unreachable)
        int[][] times2 = {{1, 2, 1}, {2, 3, 2}};
        int n2 = 4;
        int k2 = 1;
        System.out.println("Test Case 2 (Expected -1): " + solution.networkDelayTimeDijkstra(times2, n2, k2));

        // Test Case 3: N = 1 (Source is the only node)
        int[][] times3 = {};
        int n3 = 1;
        int k3 = 1;
        System.out.println("Test Case 3 (Expected 0): " + solution.networkDelayTimeDijkstra(times3, n3, k3));

        // Test Case 4: Zero-weight edges
        int[][] times4 = {{1, 2, 0}, {2, 3, 0}};
        int n4 = 3;
        int k4 = 1;
        System.out.println("Test Case 4 (Expected 0): " + solution.networkDelayTimeDijkstra(times4, n4, k4));
        
        // Test Case 5: Verification of SPFA Alternative implementation
        System.out.println("Test Case 1 via SPFA (Expected 2): " + solution.networkDelayTimeSPFA(times1, n1, k1));
    }
}


/**
 * Network Delay Time - All Approaches in One File
 *
 * Key Idea:
 * ----------
 * Find shortest path from source k to all nodes.
 * Then return the MAX distance among all nodes.
 * If any node is unreachable -> return -1
 */
public class NetworkDelayTime {

    /**
     * ================================
     * APPROACH 1: DIJKSTRA (MIN HEAP)
     * ================================
     *
     * Why Dijkstra?
     * - Graph has positive weights
     * - We need shortest path from single source
     *
     * Intuition:
     * ----------
     * Always expand the closest node first.
     *
     * Think:
     * "Signal spreads like waves → closest first"
     */
    public int networkDelayTime_Dijkstra(int[][] times, int n, int k) {

        // Step 1: Build adjacency list
        Map<Integer, List<int[]>> graph = new HashMap<>();

        for (int[] edge : times) {
            graph
                .computeIfAbsent(edge[0], x -> new ArrayList<>())
                .add(new int[]{edge[1], edge[2]}); // (neighbor, weight)
        }

        // Step 2: Min Heap -> (time, node)
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        pq.offer(new int[]{0, k});

        // Step 3: Distance array
        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        // Step 4: Dijkstra
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int currTime = current[0];
            int node = current[1];

            // Skip outdated entries (IMPORTANT optimization)
            if (currTime > dist[node]) continue;

            for (int[] neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                int nextNode = neighbor[0];
                int weight = neighbor[1];

                // Relaxation step
                if (dist[nextNode] > currTime + weight) {
                    dist[nextNode] = currTime + weight;
                    pq.offer(new int[]{dist[nextNode], nextNode});
                }
            }
        }

        // Step 5: Find answer
        int answer = 0;

        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) return -1; // unreachable
            answer = Math.max(answer, dist[i]);
        }

        return answer;
    }

    /**
     * ===================================
     * APPROACH 2: DIJKSTRA (WITHOUT HEAP)
     * ===================================
     *
     * Uses array instead of heap
     * Pick minimum distance node manually
     *
     * Time: O(V^2)
     * Useful when V is small (n <= 100)
     */
    public int networkDelayTime_Dijkstra_Array(int[][] times, int n, int k) {

        // Build graph
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int[] edge : times) {
            graph.computeIfAbsent(edge[0], x -> new ArrayList<>())
                 .add(new int[]{edge[1], edge[2]});
        }

        int[] dist = new int[n + 1];
        boolean[] visited = new boolean[n + 1];

        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        for (int i = 1; i <= n; i++) {

            // Find minimum unvisited node
            int minNode = -1;
            int minDist = Integer.MAX_VALUE;

            for (int j = 1; j <= n; j++) {
                if (!visited[j] && dist[j] < minDist) {
                    minDist = dist[j];
                    minNode = j;
                }
            }

            // No reachable node left
            if (minNode == -1) break;

            visited[minNode] = true;

            // Relax neighbors
            for (int[] neighbor : graph.getOrDefault(minNode, Collections.emptyList())) {
                int next = neighbor[0];
                int weight = neighbor[1];

                if (dist[next] > dist[minNode] + weight) {
                    dist[next] = dist[minNode] + weight;
                }
            }
        }

        int answer = 0;

        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) return -1;
            answer = Math.max(answer, dist[i]);
        }

        return answer;
    }

    /**
     * =====================================
     * APPROACH 3: BELLMAN-FORD
     * =====================================
     *
     * Works even with negative weights (not needed here)
     *
     * Time: O(V * E)
     * Slower but simple
     */
    public int networkDelayTime_BellmanFord(int[][] times, int n, int k) {

        int[] dist = new int[n + 1];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[k] = 0;

        // Relax edges V-1 times
        for (int i = 1; i < n; i++) {
            boolean updated = false;

            for (int[] edge : times) {
                int u = edge[0], v = edge[1], w = edge[2];

                if (dist[u] != Integer.MAX_VALUE && dist[v] > dist[u] + w) {
                    dist[v] = dist[u] + w;
                    updated = true;
                }
            }

            // Optimization: stop early
            if (!updated) break;
        }

        int answer = 0;

        for (int i = 1; i <= n; i++) {
            if (dist[i] == Integer.MAX_VALUE) return -1;
            answer = Math.max(answer, dist[i]);
        }

        return answer;
    }

    /**
     * =====================================
     * APPROACH 4: FLOYD-WARSHALL (ALL PAIRS)
     * =====================================
     *
     * Overkill for this problem
     *
     * Time: O(n^3)
     * Space: O(n^2)
     */
    public int networkDelayTime_FloydWarshall(int[][] times, int n, int k) {

        int[][] dist = new int[n + 1][n + 1];

        // Initialize
        for (int i = 1; i <= n; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
            dist[i][i] = 0;
        }

        for (int[] edge : times) {
            dist[edge[0]][edge[1]] = edge[2];
        }

        // Core logic
        for (int via = 1; via <= n; via++) {
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= n; j++) {

                    if (dist[i][via] != Integer.MAX_VALUE &&
                        dist[via][j] != Integer.MAX_VALUE) {

                        dist[i][j] = Math.min(dist[i][j],
                                dist[i][via] + dist[via][j]);
                    }
                }
            }
        }

        int answer = 0;

        for (int i = 1; i <= n; i++) {
            if (dist[k][i] == Integer.MAX_VALUE) return -1;
            answer = Math.max(answer, dist[k][i]);
        }

        return answer;
    }

    /**
     * =============================
     * DRY RUN (IMPORTANT FOR INTERVIEW)
     * =============================
     *
     * times = [[2,1,1],[2,3,1],[3,4,1]], n=4, k=2
     *
     * Step 1:
     * dist = [∞, ∞, 0, ∞, ∞]
     *
     * Step 2:
     * From 2 → update 1 and 3
     * dist = [∞, 1, 0, 1, ∞]
     *
     * Step 3:
     * From 1 → nothing
     * From 3 → update 4
     * dist = [∞, 1, 0, 1, 2]
     *
     * Answer = max = 2
     */
}
