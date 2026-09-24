/**
 * ============================================================================
 * MINIMUM EDGE REVERSALS SO EVERY NODE IS REACHABLE - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Are the reversals calculated independently for each starting node? 
 *    -> Why: Clarifies whether we are finding one global graph configuration, or computing a separate "what-if" scenario for every single node. (Rule: Independent scenarios).
 * Q: Is the graph guaranteed to be a single connected tree if we ignore directions?
 *    -> Why: Prevents us from needing to handle isolated, unreachable components where the answer would be impossible. (Constraint says yes).
 * Q: The constraints state N can be up to 10^5. Should I be cautious about recursion depth?
 *    -> Why: Demonstrates knowledge of JVM limits. A straight-line tree of 100,000 nodes will cause a `StackOverflowError` in a standard Java DFS. (Points toward an iterative BFS approach).
 * Q: Do I need to return the actual edges to reverse, or just the count?
 *    -> Why: Dictates whether we store heavy collections of edges or just lightweight integer counters.
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: For a single node to reach all other nodes in a tree, edges must point strictly "outward" 
 * from that root. Reversing an inward-pointing edge costs 1. We must find this total cost for *every* node 
 * acting as the root. The binding constraint is time: with N = 100,000, running a full traversal for every 
 * single node takes O(N^2) time, which will immediately Time Limit Exceed (TLE).
 *
 * Approach 1: Brute Force (N Independent Traversals)
 * - What I'd try: Iterate through every node from 0 to N-1. For each node, treat it as the root and run a 
 *   standard Breadth-First Search (BFS). If we traverse an edge against its original direction, add 1 to the cost.
 * - Why it works: In a tree, there is exactly one path between any two nodes. Exhaustively traversing from 
 *   each root guarantees we count the exact number of required reversals for that specific root.
 * - Why it's too slow: It does massive amounts of redundant work. If node 0 and node 1 are connected, the 
 *   paths from node 1 to the rest of the graph are 99.9% identical to the paths from node 0.
 * - Time Complexity: O(N^2) — For each of the N nodes, we traverse all N-1 edges in a full BFS.
 * - Space Complexity: O(N) — We store the adjacency list of size 2*(N-1) and a BFS queue of size up to N.
 *
 * Approach 2: Memoized DFS on Directed Edges (Subtree DP)
 * - What work is being repeated: Evaluating the cost of traversing a specific subtree over and over.
 * - What property removes the bottleneck: We can break the problem into overlapping subproblems. Let 
 *   `dp(u, v)` be the cost to orient all edges in the subtree of `v` away from `v`, assuming we came from `u`. 
 *   We can cache this. Since it's a tree, there are exactly 2*(N-1) directed edges, meaning only 200,000 
 *   states to ever calculate!
 * - What I'd try: A recursive DFS that returns the cost. If `memo.containsKey("u,v")`, return it. Else, compute 
 *   it by summing the costs of all branches from `v` (except going back to `u`), cache it, and return.
 * - Time Complexity: O(N) — We compute the cost for each of the 2*(N-1) directed edges exactly once.
 * - Space Complexity: O(N) — We store the cache Map, the adjacency list, and the recursion stack.
 *   *Wait, why is this not the optimum?* Because in Java, hashing 200,000 String keys or Object Pairs creates 
 *   massive constant-factor overhead, garbage collection lag, and risks `StackOverflowError`.
 *
 * Approach 3: Algebraic Re-rooting / 2-Pass Tree DP (The Optimum)
 * - What property removes the bottleneck: We don't need a heavy cache at all. We just need to observe 
 *   the *mathematical differential* when the root shifts by one node. 
 *   If we know the total cost when node A is the root (`ans[A]`), and we shift the root to its neighbor B, 
 *   what changes? *Only the edge between A and B!* Every other edge in the graph maintains its exact same 
 *   "outward" relationship relative to the root.
 * - What I'd try: 
 *   1. Do one standard BFS from node 0 to find `ans[0]`. 
 *   2. Do a second BFS starting from 0 to "push" the answers down to its neighbors using a simple formula:
 *      `ans[neighbor] = ans[current] + (cost to traverse neighbor->current) - (cost to traverse current->neighbor)`.
 * - Time Complexity: O(N) — We process every node and edge exactly twice using pure O(1) arithmetic.
 * - Space Complexity: O(N) — Adjacency list and Queue. No expensive HashMaps, no recursion stack.
 * - Decision: Approach 3 is what I'd write in an interview. It's the pinnacle of "Tree DP", completely 
 *   sidestepping recursion limits and memory overhead while remaining brilliantly elegant.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - The Star Graph (Outward): Node 0 is center, all edges point away from 0. `ans[0] = 0`. All leaf nodes 
 *   will have `ans[leaf] = 1` because only the edge connecting them to 0 needs to be reversed.
 * - The Star Graph (Inward): Node 0 is center, all edges point toward 0. `ans[0] = N - 1`. All leaf nodes 
 *   will have `ans[leaf] = N - 2`.
 * - The Line Graph: 0 -> 1 -> 2 -> ... -> N-1. `ans` array will be `[0, 1, 2, ..., N-1]`.
 * - N = 2: The absolute minimum graph (1 edge). The logic naturally holds without special base cases.
 *
 *
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "The Differential Shift". 
 * Moving the root from `u` to `v` only alters the perspective of the single edge connecting them. 
 * Because `cost(u->v) + cost(v->u) = 1` (one direction is free, the other costs 1), the math simplifies:
 * `ans[v] = ans[u] + 1 - 2 * cost(u->v)`
 *
 * ASCII Diagram & Dry Run:
 * Original graph: 0 -> 1 <- 2
 *
 * Pass 1 (Root is 0):
 * - Traverse 0 to 1. Edge is 0->1. Points away from 0 (good!). Cost = 0.
 * - Traverse 1 to 2. Edge is 1<-2. Points toward 0 (bad!). Cost = 1.
 * - Total cost for root 0: `ans[0] = 1`.
 *
 * Pass 2 (Pushing down from 0):
 * - Push from 0 to 1:
 *   - `ans[1] = ans[0] + 1 - 2 * cost(0->1)`
 *   - `ans[1] = 1 + 1 - 2*(0) = 2`. (Manual check: if 1 is root, 1->0 costs 1, 1->2 costs 1. Total 2. Correct!).
 * - Push from 1 to 2:
 *   - `ans[2] = ans[1] + 1 - 2 * cost(1->2)`
 *   - `ans[2] = 2 + 1 - 2*(1) = 1`. (Manual check: if 2 is root, 2->1 costs 0, 1->0 costs 1. Total 1. Correct!).
 * 
 * Result array: [1, 2, 1]
 *
 * Pitfalls:
 * - DFS Stack Overflow: LeetCode test cases for Tree DP often include a linked-list shaped tree of 100,000 nodes 
 *   specifically to crash Java DFS solutions. Implementing the 2 passes using Iterative BFS with a Queue completely 
 *   immunizes your code against this.
 *
 * Pattern Recognition: 
 * - When you see: "Do X for EVERY node in a tree" where X is a global property (distances, path sums, reversals).
 * - Think: "Tree DP / Re-rooting". Calculate the answer for an arbitrary root (node 0), then derive the answers 
 *   for its neighbors based on what changes across the connecting edge.
 * - Transfers to: "Sum of Distances in Tree", "Difference Between Maximum and Minimum Price Sum".
 *
 *
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if reversing edges wasn't a flat cost of 1, but each edge had a specific `weight[i]` to reverse?
 *  -> The differential formula naturally adapts! 
 *     `ans[v] = ans[u] + cost(v->u) - cost(u->v)`. We just sum the actual weights instead of +/- 1.
 * 
 * F2: What if we only want to return the nodes that require the *absolute minimum* number of reversals?
 *  -> Generate the `ans` array identically. Then, do a single O(N) pass to find the minimum value, and a 
 *     second pass to collect all node indices that match that minimum into a List.
 * 
 * F3: What if the graph is NOT a tree, but contains cycles?
 *  -> Re-rooting completely fails. In a cyclic graph, shifting the root doesn't just change one edge; it 
 *     can fundamentally alter the optimal shortest paths for the entire network. You would have to use 
 *     Dijkstra's Algorithm (or Floyd-Warshall if N is small) to solve it.
 */

import java.util.*;

public class MinimumEdgeReversals {

    // Record to store graph edges clearly. 
    // `cost` is 0 if traversing this edge aligns with the original directed edge, 1 if it opposes it.
    record Edge(int to, int cost) {}

    /**
     * OPTIMAL APPROACH: 2-Pass Tree DP (Algebraic Re-rooting via Iterative BFS)
     * Time Complexity: O(N) — We process all nodes and edges exactly twice.
     * Space Complexity: O(N) — Adjacency list and BFS Queue. No recursion stack.
     */
    public int[] minEdgeReversals(int n, int[][] edges) {
        // Step 1: Build the Adjacency List
        List<Edge>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }
        
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            
            // Moving u -> v is moving WITH the original direction. Cost = 0.
            graph[u].add(new Edge(v, 0));
            
            // Moving v -> u is moving AGAINST the original direction. Cost = 1.
            graph[v].add(new Edge(u, 1));
        }
        
        // Array to store the final answers for each node
        int[] ans = new int[n];
        
        // Step 2: First Pass (BFS) - Calculate the total reversals if Node 0 is the root.
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        
        queue.offer(0);
        visited[0] = true;
        
        int ansForZero = 0;
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            
            for (Edge edge : graph[curr]) {
                if (!visited[edge.to()]) {
                    visited[edge.to()] = true;
                    ansForZero += edge.cost();
                    queue.offer(edge.to());
                }
            }
        }
        
        ans[0] = ansForZero;
        
        // Step 3: Second Pass (BFS) - Push the answer down to all other nodes.
        // We reuse the queue and reset the visited array.
        queue.offer(0);
        Arrays.fill(visited, false);
        visited[0] = true;
        
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            
            for (Edge edge : graph[curr]) {
                int neighbor = edge.to();
                
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    
                    // The Differential Shift Formula:
                    // If moving curr -> neighbor costs 0, moving neighbor -> curr would cost 1. 
                    // So neighbor pays 1 more than curr. (+1)
                    // If moving curr -> neighbor costs 1, moving neighbor -> curr would cost 0. 
                    // So neighbor pays 1 less than curr. (-1)
                    // Math: ans[curr] + 1 - 2 * cost(curr -> neighbor)
                    ans[neighbor] = ans[curr] + 1 - 2 * edge.cost();
                    
                    queue.offer(neighbor);
                }
            }
        }
        
        return ans;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        MinimumEdgeReversals solver = new MinimumEdgeReversals();

        System.out.println("--- Testing Minimum Edge Reversals ---");

        // Test Case 1: Standard Example (From Dry Run)
        // 0 -> 1 <- 2
        int n1 = 3;
        int[][] edges1 = {{0, 1}, {2, 1}};
        // Expected: [1, 2, 1]
        System.out.println("Test Case 1: " + Arrays.toString(solver.minEdgeReversals(n1, edges1)));

        // Test Case 2: Outward Star Graph
        // 0 -> 1, 0 -> 2, 0 -> 3
        int n2 = 4;
        int[][] edges2 = {{0, 1}, {0, 2}, {0, 3}};
        // Expected: [0, 1, 1, 1]
        System.out.println("Test Case 2: " + Arrays.toString(solver.minEdgeReversals(n2, edges2)));

        // Test Case 3: Inward Star Graph
        // 1 -> 0, 2 -> 0, 3 -> 0
        int n3 = 4;
        int[][] edges3 = {{1, 0}, {2, 0}, {3, 0}};
        // Expected: [3, 2, 2, 2]
        System.out.println("Test Case 3: " + Arrays.toString(solver.minEdgeReversals(n3, edges3)));

        // Test Case 4: Line Graph
        // 0 -> 1 -> 2 -> 3
        int n4 = 4;
        int[][] edges4 = {{0, 1}, {1, 2}, {2, 3}};
        // Expected: [0, 1, 2, 3]
        System.out.println("Test Case 4: " + Arrays.toString(solver.minEdgeReversals(n4, edges4)));
        
        // Test Case 5: Complex Tree
        // 0 -> 1, 1 <- 2, 2 -> 3, 3 <- 4
        int n5 = 5;
        int[][] edges5 = {{0, 1}, {2, 1}, {2, 3}, {4, 3}};
        // 0 visits 1 (cost 0), 1 visits 2 (cost 1), 2 visits 3 (cost 0), 3 visits 4 (cost 1). ans[0] = 2.
        // Push 0 to 1: ans[1] = 2 + 1 - 0 = 3.
        // Push 1 to 2: ans[2] = 3 + 1 - 2(1) = 2.
        // Expected: [2, 3, 2, 3, 2]
        System.out.println("Test Case 5: " + Arrays.toString(solver.minEdgeReversals(n5, edges5)));
    }
}

import java.util.*;

/**
 * 🔥 GOOGLE INTERVIEW LEVEL SOLUTION
 *
 * Problem:
 * For every node, find minimum edge reversals so that all nodes
 * are reachable starting from that node.
 *
 * ------------------------------------------------------------
 * 🧠 INTERVIEW THINKING PROCESS (WHAT YOU SHOULD SAY)
 * ------------------------------------------------------------
 *
 * Step 1: Brute Force Idea
 * --------------------------------
 * For each node:
 *   - Try to reach all nodes
 *   - Count reversals needed
 *
 * Time Complexity: O(n^2) ❌ (Too slow for n = 1e5)
 *
 *
 * Step 2: Observation
 * --------------------------------
 * - Graph is a TREE (n nodes, n-1 edges)
 * - Only ONE path between any two nodes
 *
 * → This is important because:
 *   We don't have multiple choices → decisions are deterministic
 *
 *
 * Step 3: Reframe the Problem
 * --------------------------------
 * Instead of solving for every node independently:
 *
 * 👉 Solve for ONE node (root = 0)
 * 👉 Then reuse that result for others
 *
 *
 * Step 4: Key Trick (MOST IMPORTANT)
 * --------------------------------
 * Convert directed edge into:
 *
 *   u → v (cost = 0)   // already correct direction
 *   v → u (cost = 1)   // needs reversal
 *
 * WHY?
 * Because now:
 *   → Problem becomes: count how many "wrong edges" we traverse
 *
 *
 * Step 5: Compute answer[0]
 * --------------------------------
 * DFS from node 0:
 *   - If we use edge with cost=1 → reversal needed
 *
 * This gives:
 *   answer[0] = total reversals needed when root = 0
 *
 *
 * Step 6: Re-rooting (CORE IDEA)
 * --------------------------------
 * Now move root from parent → child
 *
 * Suppose we move root from u → v:
 *
 * Case 1: edge was u → v (cost = 0)
 *   - Earlier: correct direction
 *   - Now: wrong direction
 *   → Need +1 reversal
 *
 * Case 2: edge was v → u (cost = 1)
 *   - Earlier: wrong direction
 *   - Now: correct direction
 *   → Need -1 reversal
 *
 * 👉 Formula:
 *   if cost == 0 → answer[v] = answer[u] + 1
 *   if cost == 1 → answer[v] = answer[u] - 1
 *
 *
 * Step 7: Final Plan
 * --------------------------------
 * 1. Build graph with cost edges
 * 2. DFS → compute answer[0]
 * 3. DFS → re-root and fill all answers
 *
 *
 * ------------------------------------------------------------
 * ⏱ Complexity
 * ------------------------------------------------------------
 * Time: O(n)
 * Space: O(n)
 *
 */
public class MinEdgeReversalGoogleStyle {

    /**
     * Java 16+ record (clean representation of edge)
     */
    record Edge(int node, int cost) {}

    public int[] minEdgeReversals(int n, int[][] edges) {

        // Step 1: Build graph
        List<List<Edge>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] e : edges) {
            int u = e[0];
            int v = e[1];

            // Original direction
            graph.get(u).add(new Edge(v, 0)); // no reversal
            graph.get(v).add(new Edge(u, 1)); // reversal needed
        }

        int[] answer = new int[n];

        // Step 2: Compute answer for root = 0
        boolean[] visited = new boolean[n];
        answer[0] = dfsCount(0, graph, visited);

        // Step 3: Re-rooting DFS
        Arrays.fill(visited, false);
        dfsReroot(0, graph, visited, answer);

        return answer;
    }

    /**
     * DFS to compute answer[0]
     *
     * Idea:
     * Count how many edges we traverse with cost=1
     */
    private int dfsCount(int node, List<List<Edge>> graph, boolean[] visited) {
        visited[node] = true;

        int reversals = 0;

        for (Edge nei : graph.get(node)) {
            if (!visited[nei.node]) {

                /**
                 * If cost = 1 → we need to reverse this edge
                 */
                reversals += nei.cost;

                /**
                 * Continue DFS
                 */
                reversals += dfsCount(nei.node, graph, visited);
            }
        }

        return reversals;
    }

    /**
     * Re-rooting DFS
     *
     * Core idea:
     * Move root from parent → child and adjust answer
     */
    private void dfsReroot(int node, List<List<Edge>> graph,
                           boolean[] visited, int[] answer) {

        visited[node] = true;

        for (Edge nei : graph.get(node)) {
            if (!visited[nei.node]) {

                /**
                 * Transition logic:
                 *
                 * If edge was:
                 *   node → nei.node (cost=0)
                 *   → becomes WRONG → +1
                 *
                 * If edge was:
                 *   nei.node → node (cost=1)
                 *   → becomes CORRECT → -1
                 */
                if (nei.cost == 0) {
                    answer[nei.node] = answer[node] + 1;
                } else {
                    answer[nei.node] = answer[node] - 1;
                }

                dfsReroot(nei.node, graph, visited, answer);
            }
        }
    }

    /**
     * 🔍 Dry Run Example (you can explain this in interview)
     *
     * Input:
     * n = 5
     * edges = [[0,1],[2,1],[3,2],[3,4]]
     *
     * Step 1:
     * Compute answer[0]
     *
     * Step 2:
     * Re-root step by step
     *
     * Output:
     * [2,3,2,1,2]
     */
    public static void main(String[] args) {
        MinEdgeReversalGoogleStyle sol = new MinEdgeReversalGoogleStyle();

        int n = 5;
        int[][] edges = {
                {0,1},
                {2,1},
                {3,2},
                {3,4}
        };

        System.out.println(Arrays.toString(sol.minEdgeReversals(n, edges)));
    }
}
