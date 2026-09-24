/**
 * ============================================================================
 * REORDER ROUTES TO MAKE ALL PATHS LEAD TO THE CITY ZERO - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Is the road network guaranteed to be a single connected component?
 *    -> Why: Confirms it's a true tree (N nodes, N-1 edges, fully connected), meaning exactly one path exists between any two cities.
 * Q: Can I modify the input `connections` array to save space?
 *    -> Why: Modifying input arrays is often frowned upon, but if allowed, it could theoretically save space. (Usually best to assume no).
 * Q: With N up to 50,000, should I be concerned about `StackOverflowError` in Java if the tree is essentially a straight line?
 *    -> Why: Demonstrates deep knowledge of Java's JVM limits. A deep recursion (DFS) will crash; an iterative BFS is safer.
 * Q: Do we need to return *which* edges to reverse, or just the *count*?
 *    -> Why: Dictates whether we need to maintain a collection of results or just an integer counter.
 *
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We have a directed tree, but the edges point in arbitrary directions. We need every node 
 * to have a directed path to node 0. The binding constraint is time: because N is up to 50,000, we cannot afford 
 * to search for a path from *every* individual city to 0 independently (which would be O(N^2)).
 * 
 * Approach 1: Naive Path Checking (Brute Force)
 * - What I'd try: For every city from 1 to N-1, run a DFS to find the undirected path to 0. As I walk that path 
 *   towards 0, if an edge points towards me instead of towards 0, increment a counter. 
 * - Why it works: In a tree, there is only one path between a node and 0. Checking each path explicitly guarantees correctness.
 * - Why it's too slow: Many cities share the exact same main highways to reach 0. We would traverse and re-check 
 *   the same edges near city 0 thousands of times.
 * - Time Complexity: O(N^2) — because for each of the N cities, we might traverse up to O(N) edges to reach 0.
 * - Space Complexity: O(N) — because we still need to store the graph as an adjacency list to traverse it.
 *
 * Approach 2: "Water Flowing Backwards" Global Traversal (Using a HashSet)
 * - What work is being repeated: Evaluating the same edges over and over for different starting cities.
 * - What property removes the bottleneck: The graph is connected. Instead of N people trying to find their way *to* 0, 
 *   imagine node 0 broadcasting a signal *outwards* to all N nodes. If the signal travels from `u` to `v`, but the 
 *   original one-way road is `u -> v` (pointing away from 0), it's wrong and must be reversed. If the road is `v -> u` 
 *   (pointing towards 0), it's correct!
 * - What I'd try: Build an undirected adjacency list to allow traversal, AND store the original directed edges in a 
 *   `HashSet` as strings (e.g., "u,v"). Start a BFS from 0. When moving from `curr` to `neighbor`, check if `"curr,neighbor"` 
 *   is in the set. If yes, add 1 to the reversal count.
 * - Time Complexity: O(N) — because we visit each of the N nodes and N-1 edges exactly once, and HashSet lookups are O(1).
 *   (Note: String concatenation actually makes this closer to O(N * length of string), adding overhead).
 * - Space Complexity: O(N) — because we store N-1 strings in the HashSet, plus an undirected adjacency list of size 2*(N-1).
 *
 * Approach 3: Adjacency List with Artificial Weights (The Optimum)
 * - What property removes the bottleneck: Hashing strings (or even pairing integers) is computationally heavy and wastes memory. 
 *   We can embed the "needs reversal" information directly into the adjacency list!
 * - What I'd try: When reading the input `[u, v]`, add `v` to `u`'s list with a weight of `1` (meaning: this edge points 
 *   away from 0, reversing it costs 1). Add `u` to `v`'s list with a weight of `0` (meaning: this edge points towards 0, 
 *   it's free). Then, do a simple BFS/DFS from 0. The total cost is just the sum of the artificial weights we traverse.
 * - Time Complexity: O(N) — because we visit each node and edge exactly once, using pure primitive array/list lookups.
 * - Space Complexity: O(N) — because we store exactly 2*(N-1) custom objects/arrays in the adjacency list, plus a Queue 
 *   (or recursion stack) that grows up to O(N). No HashSets, no Strings.
 * - Decision: Approach 3 using an Iterative BFS is what I'd write in a 45-minute interview. It perfectly balances 
 *   time/space optimality with production-safety (avoiding Java's DFS stack overflow on deep trees).
 *
 * 
 * 3. EDGE CASES
 * ------------------------------------------------
 * - All edges already point to 0: BFS naturally adds 0 at every step. Total cost 0.
 * - All edges point away from 0 (e.g., an outward star graph): BFS naturally adds 1 at every step. Total cost N-1.
 * - Deep Linear Tree (Linked List shape): e.g., 0 -> 1 -> 2 -> ... -> 50000. 
 *   -> This is why BFS is strictly better than DFS in Java for this problem. DFS will throw a `StackOverflowError` 
 *      at ~10,000 frames.
 * - Star Graph centered at 0: 0 is connected to 10,000 nodes directly. BFS queue gets massive on step 1, but handles 
 *   it perfectly well within O(N) memory.
 * 
 * 
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "Invert the perspective."
 * Don't figure out how to get to 0. Figure out how 0 gets to everyone else. Any road that correctly facilitates 
 * 0 getting to everyone else is actually pointing the WRONG way for our goal, and must be flipped.
 * 
 * ASCII Diagram:
 * Initial connections: [0,1], [1,3], [2,3], [4,0], [4,5]
 * 
 *       0 ---> 1 ---> 3 <--- 2
 *       ^
 *       |
 *       4 ---> 5
 * 
 * If 0 broadcasts outward (BFS):
 * - 0 visits 1. Road is 0->1. Points AWAY from 0. Cost + 1. (Reverse it to 1->0).
 * - 0 visits 4. Road is 4->0. Points TO 0. Cost + 0. (Leave it).
 * - 1 visits 3. Road is 1->3. Points AWAY from 0. Cost + 1. (Reverse it to 3->1).
 * - 3 visits 2. Road is 2->3. Points TO 0 (relative to 3, going back to 0). Cost + 0.
 * - 4 visits 5. Road is 4->5. Points AWAY from 0. Cost + 1. (Reverse it to 5->4).
 * Total Reversals: 3.
 * 
 * Dry Run (Using Approach 3's Weights on above graph):
 * Adj List:
 * 0: [1, w:1], [4, w:0] 
 * 1: [0, w:0], [3, w:1]
 * 2: [3, w:1]
 * 3: [1, w:0], [2, w:0]
 * 4: [0, w:1], [5, w:1]
 * 5: [4, w:0]
 * 
 * Queue: [0]. Visited: {0}
 * Pop 0. 
 *  - Neighbor 1 (w:1). Visited? No. Total = 0 + 1 = 1. Queue: [1]. Visited: {0,1}
 *  - Neighbor 4 (w:0). Visited? No. Total = 1 + 0 = 1. Queue: [1,4]. Visited: {0,1,4}
 * Pop 1.
 *  - Neighbor 0 (w:0). Visited? Yes. Ignore.
 *  - Neighbor 3 (w:1). Visited? No. Total = 1 + 1 = 2. Queue: [4,3]. Visited: {0,1,4,3}
 * (Continues... beautifully simple).
 * 
 * Pitfalls:
 * - Trying to track "visited" in a Tree using a HashSet. Just use a `boolean[] visited` array; it's much faster 
 *   and uses contiguous memory. Better yet, since it's a tree, just pass the `parent` node in the traversal 
 *   so you don't go backwards. `if (neighbor == parent) continue;`. (Saves O(N) space!).
 * 
 * Pattern Recognition: "Tree traversal with artificial edge weights."
 * - When you see: A tree problem asking about directionality or edge states.
 * - Think: Treat it as an undirected tree, but embed the direction/state as a 0/1 weight.
 * - Transfers to: "Minimum Edge Reversals So Every Node Is Reachable" (Harder version where you must find the 
 *   BEST capital city. You do this exact logic + tree DP/re-rooting).
 *
 * 
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if there are MULTIPLE capital cities (e.g., nodes 0, 10, and 20 are all valid targets)?
 *  -> Instead of starting the BFS with just `[0]`, we initialize the queue with ALL capital cities, and set 
 *     all of their `visited` flags to true. This becomes a Multi-Source BFS. The logic remains exactly the same.
 * 
 * F2: What if reversing a road isn't free, but each road has a `cost[i]` to reverse?
 *  -> Instead of assigning an artificial weight of `1`, we assign the weight `cost[i]` to the outgoing edge, 
 *     and `0` to the incoming edge. The sum gives the minimum cost. (Since it's still a tree, there's still 
 *     only one path, so BFS/DFS still works without needing Dijkstra).
 * 
 * F3: What if the graph is NOT a tree, but a general graph with cycles, and we just want to guarantee AT LEAST 
 *     one path from every node to 0 for the cheapest reversal cost?
 *  -> This fundamentally changes the problem. It becomes a Single-Destination Shortest Path problem. We would 
 *     assign weights the same way (1 for wrong way, 0 for right way) and run Dijkstra's Algorithm starting from 
 *     0 to find the minimum sum of weights to reach every other node.
 * 
 * 
 * 6. JAVA CODE
 * ------------------------------------------------
 */

import java.util.*;

public class ReorderRoutes {

    // Using a record for clarity in the adjacency list: contains neighbor ID and artificial cost
    record Edge(int to, int cost) {}

    /**
     * OPTIMAL APPROACH: Iterative BFS with Artificial Edge Weights
     * Time Complexity: O(N) — We process each of the N nodes and 2*(N-1) edges exactly once.
     * Space Complexity: O(N) — Adjacency list holds 2*(N-1) edges. Queue holds at most O(N) nodes.
     */
    public int minReorder(int n, int[][] connections) {
        // Step 1: Build the Adjacency List
        // Array of Lists is faster and more memory efficient than Map<Integer, List>
        List<Edge>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }
        
        for (int[] conn : connections) {
            int u = conn[0];
            int v = conn[1];
            
            // "u -> v" points AWAY from 0 (if we traverse from 0). Reversing it costs 1.
            graph[u].add(new Edge(v, 1));
            
            // "v -> u" points TOWARDS 0. We traverse it for free.
            graph[v].add(new Edge(u, 0));
        }
        
        // Step 2: Initialize Iterative BFS
        int totalReversals = 0;
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        
        queue.offer(0);
        visited[0] = true;
        
        // Step 3: Traverse the tree outwards from 0
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            
            for (Edge edge : graph[curr]) {
                int neighbor = edge.to();
                int cost = edge.cost();
                
                // If we haven't visited this node yet, it means we are moving strictly "outwards" from 0
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    totalReversals += cost; // Accumulate the artificial weight
                    queue.offer(neighbor);
                }
            }
        }
        
        return totalReversals;
    }

    /**
     * ALTERNATIVE: DFS Approach (Very clean, but riskier)
     * Included to show how avoiding the 'visited' array via a 'parent' pointer works in trees.
     * Note: In Java, this risks StackOverflowError if the tree is extremely deep (e.g., 50,000 nodes in a line).
     * Time Complexity: O(N)
     * Space Complexity: O(N)
     */
    public int minReorderDFS(int n, int[][] connections) {
        List<Edge>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
        for (int[] conn : connections) {
            graph[conn[0]].add(new Edge(conn[1], 1));
            graph[conn[1]].add(new Edge(conn[0], 0));
        }
        
        // Start DFS from node 0. It has no parent, so pass -1.
        return dfs(0, -1, graph);
    }

    private int dfs(int curr, int parent, List<Edge>[] graph) {
        int cost = 0;
        for (Edge edge : graph[curr]) {
            int neighbor = edge.to();
            // Tree optimization: Instead of a boolean[] visited, just don't go back to the parent.
            if (neighbor != parent) {
                cost += edge.cost() + dfs(neighbor, curr, graph);
            }
        }
        return cost;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        ReorderRoutes solver = new ReorderRoutes();
        
        System.out.println("--- Testing Reorder Routes to Make All Paths Lead to the City Zero ---");

        // Test Case 1: Standard Example (From Dry Run)
        int n1 = 6;
        int[][] connections1 = {{0,1}, {1,3}, {2,3}, {4,0}, {4,5}};
        System.out.println("Test Case 1 (Expected 3): " + solver.minReorder(n1, connections1));

        // Test Case 2: All edges point correctly towards 0
        int n2 = 5;
        int[][] connections2 = {{1,0}, {2,0}, {3,2}, {4,2}};
        System.out.println("Test Case 2 (Expected 0): " + solver.minReorder(n2, connections2));

        // Test Case 3: All edges point away from 0 (Star graph outward)
        int n3 = 4;
        int[][] connections3 = {{0,1}, {0,2}, {0,3}};
        System.out.println("Test Case 3 (Expected 3): " + solver.minReorder(n3, connections3));

        // Test Case 4: Deep linear tree (0 <- 1 <- 2 -> 3 -> 4)
        // 1 points to 0 (cost 0). 2 points to 1 (cost 0). 2 points to 3 (wrong, cost 1). 3 points to 4 (wrong, cost 1).
        // Wait, input is [u,v].
        int n4 = 5;
        int[][] connections4 = {{1,0}, {2,1}, {2,3}, {3,4}}; 
        // 0 visits 1 (cost 0, since it's 1->0). 
        // 1 visits 2 (cost 0, since it's 2->1).
        // 2 visits 3 (cost 1, since it's 2->3).
        // 3 visits 4 (cost 1, since it's 3->4).
        System.out.println("Test Case 4 (Expected 2): " + solver.minReorder(n4, connections4));
    }
}


import java.util.*;

/**
 * 🔥 Problem: Reorder Routes to Make All Paths Lead to City 0
 *
 * Key Idea:
 * ----------
 * Convert directed edges into an undirected graph,
 * but store direction info.
 *
 * If we traverse an edge that originally goes OUTWARD
 * (u -> v), then it needs to be reversed.
 *
 * -----------------------------------------------
 * Visualization Trick:
 *
 * Original:
 * u ---> v  (bad if we are moving from 0 side)
 *
 * Converted:
 * u -> v (cost = 1)   // means wrong direction
 * v -> u (cost = 0)   // means correct direction
 *
 * -----------------------------------------------
 *
 * Time Complexity: O(N)
 * Space Complexity: O(N)
 */

public class ReorderRoutesToZero {

    /**
     * Approach 1: DFS
     */
    public int minReorderDFS(int n, int[][] connections) {

        // Step 1: Build graph
        // Each node stores (neighbor, cost)
        // cost = 1 → edge needs reversal
        // cost = 0 → edge is correct
        List<List<int[]>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : connections) {
            int u = edge[0];
            int v = edge[1];

            // original direction u -> v (wrong if moving from 0)
            graph.get(u).add(new int[]{v, 1});

            // reverse direction v -> u (correct)
            graph.get(v).add(new int[]{u, 0});
        }

        boolean[] visited = new boolean[n];

        return dfs(0, graph, visited);
    }

    private int dfs(int node, List<List<int[]>> graph, boolean[] visited) {

        visited[node] = true;

        int changes = 0;

        for (int[] neighbor : graph.get(node)) {
            int next = neighbor[0];
            int cost = neighbor[1];

            if (!visited[next]) {
                /*
                 * If cost = 1 → edge is wrongly directed
                 * So we must reverse it
                 */
                changes += cost;

                changes += dfs(next, graph, visited);
            }
        }

        return changes;
    }

    /**
     * Approach 2: BFS (Same logic, iterative)
     *
     * Preferred if recursion depth is risky (n up to 50k)
     */
    public int minReorderBFS(int n, int[][] connections) {

        List<List<int[]>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : connections) {
            int u = edge[0];
            int v = edge[1];

            graph.get(u).add(new int[]{v, 1}); // needs reversal
            graph.get(v).add(new int[]{u, 0}); // correct
        }

        boolean[] visited = new boolean[n];

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(0);
        visited[0] = true;

        int changes = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int[] neighbor : graph.get(node)) {
                int next = neighbor[0];
                int cost = neighbor[1];

                if (!visited[next]) {
                    visited[next] = true;

                    changes += cost; // count reversal if needed

                    queue.offer(next);
                }
            }
        }

        return changes;
    }

    /**
     * 🚀 Approach 3: Optimal Greedy Insight (Same as DFS/BFS)
     *
     * There is no better than O(N) since we must visit all nodes.
     */

    /**
     * 🧪 Dry Run:
     *
     * connections = [[0,1],[1,3],[2,3],[4,0],[4,5]]
     *
     * Graph built:
     *
     * 0 -> 1 (1)
     * 1 -> 0 (0)
     *
     * Start from 0:
     * 0 → 1 (cost 1) → reverse
     * 1 → 3 (cost 1) → reverse
     * 3 → 2 (cost 0) → ok
     * 0 → 4 (cost 0) → ok
     * 4 → 5 (cost 1) → reverse
     *
     * Total = 3
     */

    public static void main(String[] args) {
        ReorderRoutesToZero sol = new ReorderRoutesToZero();

        int n = 6;
        int[][] connections = {
                {0,1},{1,3},{2,3},{4,0},{4,5}
        };

        System.out.println("DFS Answer: " + sol.minReorderDFS(n, connections));
        System.out.println("BFS Answer: " + sol.minReorderBFS(n, connections));
    }
}

