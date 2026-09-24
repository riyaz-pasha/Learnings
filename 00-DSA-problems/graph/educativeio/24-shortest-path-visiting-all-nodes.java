/**
 * ============================================================================
 * SHORTEST PATH VISITING ALL NODES - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: What is the maximum number of nodes in the graph?
 *    -> Why: This is the most critical question. A constraint of N <= 12 screams NP-Hard / Bitmask DP or BFS. If N were 100, we'd need an approximation algorithm like Christofides, as finding an exact shortest path covering all nodes (TSP variant) is impossible in polynomial time.
 * Q: Are the edges weighted?
 *    -> Why: Determines whether we can use a standard Queue (unweighted BFS) or must use a PriorityQueue (Dijkstra's). (Rule: Unweighted).
 * Q: Is revisiting nodes strictly required?
 *    -> Why: If the graph is a star graph (center node connected to all others), we MUST revisit the center node to reach every leaf.
 * Q: Can the start and end nodes be any node?
 *    -> Why: Dictates our initialization. We must evaluate paths starting from *every* node simultaneously to find the global minimum.
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to find the shortest path that touches every node. Unlike standard shortest 
 * path problems, we are allowed (and often required) to revisit nodes. The binding constraint is avoiding 
 * infinite loops while allowing necessary backtracking. A standard `visited` array fails because marking a 
 * node as visited permanently blocks us from passing through it again to reach an unvisited part of the graph.
 *
 * Approach 1: Depth-First Search (DFS) / Backtracking (Brute Force)
 * - What I'd naturally try: Start from node 0. Recurse to all neighbors. Keep a `Set` of visited nodes. 
 *   If the Set size == N, record the path length. To allow revisiting, I'd allow the DFS to step onto already 
 *   visited nodes, perhaps capping the max path length to prevent infinite recursion. Repeat for all starting nodes.
 * - Why it works: It exhaustively tries every possible walk through the graph.
 * - Why it's too slow: Allowing revisits causes an exponential explosion in valid paths. The graph traversal 
 *   turns into an infinite random walk. Even with aggressive bounding, it explores massively redundant paths.
 * - Time Complexity: O(N!) or worse — because the branching factor remains near O(V) at every step, and path 
 *   lengths can exceed N.
 * - Space Complexity: O(N^2) — because the recursion stack can grow as deep as the maximum path length allowed.
 *
 * Approach 2: Floyd-Warshall + Traveling Salesperson DP (Alternative)
 * - What work is being repeated: Stepping through nodes just to travel between two unvisited nodes.
 * - What property removes the bottleneck: We can precompute the shortest distances between ALL pairs of nodes 
 *   using Floyd-Warshall. Then, the problem reduces to the classic Traveling Salesperson Problem (TSP) with 
 *   revisits allowed, solvable via Bitmask DP.
 * - Time Complexity: O(N^3 + N^2 * 2^N) — O(N^3) for Floyd-Warshall, then O(N^2 * 2^N) to process every state 
 *   (current_node, visited_mask) and attempt a transition to every other unvisited node.
 * - Space Complexity: O(N * 2^N) — For the DP memoization table `dp[node][mask]`.
 * - Why we keep looking: While perfectly valid, doing an all-pairs shortest path precomputation is overkill 
 *   when the edge weights are all exactly 1. We can merge the pathfinding and state-tracking into a single step.
 *
 * Approach 3: State-Space BFS with Bitmask (The Optimum)
 * - What property improves the bottleneck further: Since edge weights are 1, a level-by-level BFS is guaranteed 
 *   to find the shortest path first. To solve the infinite loop problem, we redefine our "visited" definition. 
 *   We don't care if we've visited Node 2 before. We care if we've visited Node 2 *with the exact same set of 
 *   previously visited nodes*. 
 * - What I'd try: Represent the set of visited nodes as an integer bitmask. The state becomes a tuple: 
 *   `(currentNode, visitedMask)`. We use a Queue to run a Multi-Source BFS, starting simultaneously from 
 *   all N nodes. If we reach a state we've seen before, we prune it. The moment any state reaches a mask 
 *   of all 1s, we return the steps taken!
 * - Time Complexity: O(N^2 * 2^N) — There are N * 2^N possible unique states. From each state, we can transition 
 *   to at most N neighbors, leading to our upper bound of operations. For N=12, 12^2 * 4096 ≈ 589,000 ops, well 
 *   within time limits.
 * - Space Complexity: O(N * 2^N) — For the `boolean[][] visited` array tracking which (node, mask) states 
 *   have been queued, and the Queue itself which can hold up to that many elements.
 * - Decision: Approach 3 is what I'd write in an interview. It's the cleanest and most direct application 
 *   of graph theory adapted to small N constraints.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - N = 1 (Single Node Graph): We start at node 0 and have already visited all nodes. Returns 0 steps.
 * - The Star Graph: Center node connected to leaves. e.g. [[1,2,3], [0], [0], [0]].
 *   The BFS will smoothly travel 1 -> 0 -> 2 -> 0 -> 3. The state masks change every time we hit 0!
 * - The Line Graph: 0-1-2-3-4. BFS starts everywhere, but the branch starting at 0 or 4 will reach the 
 *   all-1s mask first.
 *
 *
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "Bitmasks as Visited Sets"
 * When N <= 12, an integer (which has 32 bits) can easily represent the presence or absence of each node 
 * in our journey. 
 * Node 0 visited -> 0001 (Decimal 1)
 * Node 2 visited -> 0100 (Decimal 4)
 * Nodes 0 and 2 visited -> 0101 (Decimal 5)
 * A state `(Node 2, Mask 5)` is fundamentally different from `(Node 2, Mask 7)`.
 *
 * ASCII Diagram (The Multi-Source BFS Advantage):
 *   1 --- 0 --- 2
 * 
 * If we start BFS *only* at 0: 0 -> 1 -> 0 -> 2. (Steps: 3)
 * If we start BFS simultaneously everywhere, the branch starting at 1: 1 -> 0 -> 2. (Steps: 2)
 * The multi-source BFS evaluates the 1-start branch and finds the target mask one level earlier!
 *
 * Dry Run:
 * Graph: 0-1-2. Target mask: 111 (Binary) = 7.
 * Initial Queue:
 * [(Node:0, Mask:001), (Node:1, Mask:010), (Node:2, Mask:100)] (All distance 0)
 * 
 * Level 1 (Distance 1):
 * - Pop (0, 001): Neigh = [1]. New Mask = 001 | 010 = 011. Push (1, 011).
 * - Pop (1, 010): Neigh = [0,2]. New Masks = 011 and 110. Push (0, 011), (2, 110).
 * - Pop (2, 100): Neigh = [1]. New Mask = 100 | 010 = 110. Push (1, 110).
 * Queue: [(1, 011), (0, 011), (2, 110), (1, 110)]
 * 
 * Level 2 (Distance 2):
 * - Pop (1, 011): Neigh = [0,2].
 *   - Visit 0 -> Mask = 011 | 001 = 011. (State (0, 011) already visited!). Skip.
 *   - Visit 2 -> Mask = 011 | 100 = 111 (7). TARGET REACHED! Return distance 2.
 *
 * Pitfalls:
 * - Forgetting to use the bitwise OR `|` operator correctly to add a node to a mask: `mask | (1 << nextNode)`.
 * - Using a 1D `visited` array. If you mark `visited[node] = true`, you destroy the ability to revisit it. 
 *   You MUST use `visited[node][mask] = true`.
 * 
 * Pattern Recognition: 
 * - When you see: "Visit all nodes", "Shortest path", and "N <= 15".
 * - Think: BFS with Bitmask State.
 * - Transfers to: "Shortest Path to Get All Keys" (Where the mask represents keys collected).
 * 
 * Interview Script:
 * "Since we want the shortest path and edges are unweighted, I'll use BFS. However, because we are allowed to 
 * revisit nodes, a standard visited array would trap us. Since N is very small (max 12), I can represent the 
 * set of visited nodes as a bitmask. I'll expand the BFS state to be `(currentNode, visitedMask)`. 
 * To ensure I find the absolute shortest path, I'll initialize the queue with all N nodes, creating a 
 * multi-source BFS. The first path to reach a mask of `(1 << n) - 1` gives us the minimum steps. 
 * This runs in O(N^2 * 2^N) time and O(N * 2^N) space."
 *
 *
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the edges had weights?
 *  -> We cannot use a simple FIFO Queue because paths with more nodes might actually have a smaller total weight. 
 *     We would replace the Queue with a PriorityQueue (Dijkstra's Algorithm). The state remains `(node, mask)`, 
 *     but we sort by total distance.
 * 
 * F2: What if N is 100?
 *  -> The exact solution becomes computationally impossible (NP-Hard Traveling Salesperson variant). 
 *     We would have to use approximation algorithms (like Minimum Spanning Tree heuristics or Christofides) 
 *     or heuristic search (Simulated Annealing, Genetic Algorithms) to find a "good enough" path.
 * 
 * F3: What if we are required to START at node 0?
 *  -> The algorithm remains exactly the same, but our initialization step changes. Instead of looping through 
 *     all nodes and adding them to the queue, we ONLY add `(0, 1 << 0, 0)` to the queue initially.
 */

import java.util.LinkedList;
import java.util.Queue;

public class ShortestPathVisitingAllNodes {

    // Using a record to tightly package our BFS state.
    // node: The current node we are standing on.
    // mask: A bitmask representing all the nodes we have stepped on so far.
    // dist: The number of edges we've traversed to get here.
    record State(int node, int mask, int dist) {}

    /**
     * OPTIMAL APPROACH: State-Space BFS with Bitmask
     * Time Complexity: O(N^2 * 2^N)
     * Space Complexity: O(N * 2^N)
     */
    public int shortestPathLength(int[][] graph) {
        int n = graph.length;
        
        // Edge Case: If there's only 1 node, we're already done!
        if (n == 1) {
            return 0;
        }

        // targetMask represents a state where every node from 0 to N-1 has been visited.
        // e.g., for N=3, targetMask is (1 << 3) - 1 = 8 - 1 = 7 (Binary 111).
        int targetMask = (1 << n) - 1;

        // visited[node][mask] ensures we never process the exact same scenario twice.
        // For example, visited[2][5] means: "We are currently standing on node 2, and our 
        // history of visited nodes is {0, 2} (because 5 in binary is 101)".
        boolean[][] visited = new boolean[n][1 << n];
        
        Queue<State> queue = new LinkedList<>();

        // Multi-Source Initialization:
        // We don't know which node is the optimal starting point. 
        // So, we start from ALL of them simultaneously!
        for (int i = 0; i < n; i++) {
            // The initial mask has exactly the i-th bit set to 1.
            int initialMask = 1 << i;
            queue.offer(new State(i, initialMask, 0));
            visited[i][initialMask] = true;
        }

        // Standard BFS
        while (!queue.isEmpty()) {
            State curr = queue.poll();

            // Check all neighbors of the current node
            for (int nextNode : graph[curr.node()]) {
                
                // Calculate what the bitmask will look like after stepping onto 'nextNode'.
                // The bitwise OR (|) safely turns the bit to 1 whether it was previously 0 or 1.
                int nextMask = curr.mask() | (1 << nextNode);

                // If this step causes us to have visited all nodes, we are completely done.
                // Because BFS evaluates level-by-level, this is guaranteed to be the shortest path.
                if (nextMask == targetMask) {
                    return curr.dist() + 1;
                }

                // If we've never been in this exact (node, mask) state before, queue it up.
                if (!visited[nextNode][nextMask]) {
                    visited[nextNode][nextMask] = true;
                    queue.offer(new State(nextNode, nextMask, curr.dist() + 1));
                }
            }
        }

        // Fallback (Should never be reached if graph is connected)
        return -1;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        ShortestPathVisitingAllNodes solver = new ShortestPathVisitingAllNodes();

        System.out.println("--- Testing Shortest Path Visiting All Nodes ---");

        // Test Case 1: Standard Graph
        // Graph structure: 0-1, 0-2, 0-3
        // Optimal Path: 1 -> 0 -> 2 -> 0 -> 3 (Length 4)
        int[][] graph1 = {{1, 2, 3}, {0}, {0}, {0}};
        System.out.println("Test Case 1 (Expected 4): " + solver.shortestPathLength(graph1));

        // Test Case 2: Ring with a tail
        // 0-1-2-3-4-0 (Ring), 0-5 (Tail)
        // Optimal starts at 5, goes to 0, then wraps around the ring.
        int[][] graph2 = {{1, 2, 5}, {0, 2}, {0, 1, 3}, {2, 4}, {3, 0}, {0}};
        // Wait, looking closely at graph2 provided in standard leetcode examples:
        // graph = [[1],[0,2,4],[1,3,4],[2],[1,2]]
        int[][] leetcodeGraph2 = {{1}, {0,2,4}, {1,3,4}, {2}, {1,2}};
        // Optimal path: 0 -> 1 -> 4 -> 2 -> 3 (Length 4)
        System.out.println("Test Case 2 (Expected 4): " + solver.shortestPathLength(leetcodeGraph2));

        // Test Case 3: N = 1 (Single Node)
        int[][] graph3 = {{}};
        System.out.println("Test Case 3 (Expected 0): " + solver.shortestPathLength(graph3));

        // Test Case 4: The Line Graph
        // 0 - 1 - 2
        // Optimal: 0 -> 1 -> 2 (or reverse). Length 2.
        int[][] graph4 = {{1}, {0, 2}, {1}};
        System.out.println("Test Case 4 (Expected 2): " + solver.shortestPathLength(graph4));
    }
}
