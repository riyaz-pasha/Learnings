/**
 * ============================================================================
 * PATH WITH MAXIMUM PROBABILITY - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Can the probability of an edge be exactly 0?
 *    -> Why: Determines if we can permanently discard certain paths immediately (multiplying by 0 makes the whole path 0).
 * Q: If there is no path between start and end, what should I return?
 *    -> Why: Establishes the default behavior for disconnected components. (Rule: Return 0).
 * Q: Are there multiple edges between the exact same pair of nodes?
 *    -> Why: If yes, we'd need to pre-process the graph to only keep the edge with the highest probability to save time. (Rule: No, at most one).
 * Q: Can paths contain cycles, and how should they be handled?
 *    -> Why: In probability, visiting a cycle of probabilities <= 1 will only shrink the probability. We must ensure our algorithm doesn't infinitely loop on cycles.
 * Q: Will floating-point underflow be an issue if the graph is huge and probabilities are very small?
 *    -> Why: A product of a thousand 0.1s might round to 0.0 in standard double precision. (Usually safe for LeetCode constraints, but shows deep domain knowledge).
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to traverse a graph to find a specific path from A to B. But instead of the 
 * standard "minimize the sum of weights", we need to "maximize the product of weights", where all weights 
 * are probabilities (0.0 to 1.0). The binding constraint is that branching factors can be huge, so we need 
 * an algorithm that prunes sub-optimal paths immediately.
 * 
 * Approach 1: Depth-First Search (DFS) / Backtracking
 * - What I'd try: Start at the `start` node. Recursively traverse all neighbors, carrying a `running_prob`. 
 *   If we reach `end`, update `max_prob = max(max_prob, running_prob)`. Keep a `visited` set to avoid infinite loops.
 * - Why it works: Exhaustively explores every simple path in the graph, guaranteeing we see the best one.
 * - Why it's too slow: It blindly explores everything. A path might start with a 0.0001 probability and still 
 *   explore a massive subtree, even though we already found a path with 0.9 probability. 
 * - Time Complexity: O(V!) — because in a dense graph, DFS explores every possible permutation of paths 
 *   between nodes, leading to an exponential combinatorial explosion.
 * - Space Complexity: O(V + E) — because we store the adjacency list O(E) and the recursion stack can go O(V) 
 *   deep in the worst case.
 * 
 * Approach 2: Breadth-First Search (BFS) with Queue / SPFA
 * - What work is being repeated in DFS: We explore deep sub-graphs with terrible starting probabilities.
 * - What property removes the bottleneck: We can maintain an array `maxProb[i]` tracking the best probability 
 *   to reach node `i`. Using a Queue, we push `(node, current_prob)`. If `current_prob * edge_prob > maxProb[neighbor]`, 
 *   we update the neighbor and push it to the queue.
 * - Why it works: It prunes paths that are strictly worse than what we've already found.
 * - Why it's too slow (for some graphs): Standard BFS evaluates by "fewest edges", not "best probability". 
 *   We might reach node X via 1 edge (prob: 0.1) and process all its neighbors. Later, we reach node X via 
 *   4 edges (prob: 0.9). We have to update X and re-queue it, causing redundant downstream processing.
 * - Time Complexity: O(V * E) — because in a worst-case graph, a node can be updated and re-pushed to the 
 *   queue up to V-1 times.
 * - Space Complexity: O(V + E) — because we store the adjacency list O(E), the `maxProb` array O(V), and 
 *   the queue which can grow to O(V).
 * 
 * Approach 3: Dijkstra's Algorithm with a Max-Heap (The Optimum)
 * - What property removes the SPFA bottleneck: Multiplying a probability by another probability (<= 1) 
 *   *always* makes it smaller or equal. It can never grow. Therefore, if we always process the node with the 
 *   *highest probability seen so far*, its path is permanently optimal. No future path can possibly reach 
 *   it with a higher probability.
 * - What I'd try: Swap the Queue for a `PriorityQueue` (Max-Heap) ordered by probability descending. 
 *   Pop the node with the highest probability. Lock it in. Push its updated neighbors. The moment we pop 
 *   the `end` node, we can return immediately!
 * - Time Complexity: O(E log E) — because we process at most E edges, and each push/pop operation on the 
 *   Max-Heap takes O(log E) since the heap size is bounded by E.
 * - Space Complexity: O(V + E) — because the graph adjacency list requires O(E) space, the `maxProb` 
 *   array requires O(V) space, and the PriorityQueue holds up to O(E) elements in the worst case (lazy deletion).
 * - Decision: Approach 3 is the undisputed champion here. It beautifully maps the SSSP concept to probability, 
 *   and its greedy nature guarantees we never do redundant work.
 *
 * 
 * 3. EDGE CASES
 * ------------------------------------------------
 * - Disconnected components: Start and end have no path between them. `maxProb[end]` remains 0.0.
 * - Edges with 0.0 probability: Instantly zero-out the path. The heap will naturally push these to the 
 *   very bottom, preventing us from wasting time exploring them.
 * - Start == End: The constraint says `start != end`, but if it weren't, the answer is 1.0 (no edges needed).
 * - Sparse vs Dense graphs: Dijkstra handles both efficiently, but in extremely dense graphs, a classic array-based 
 *   Dijkstra (O(V^2)) might outperform the Heap-based O(E log E). Given constraints (E <= 2*10^4), Heap is perfect.
 * 
 * 
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight ("The Probability-Distance Isomorphism"): 
 * Why does Dijkstra work for multiplication? Dijkstra relies on paths strictly getting "longer" (monotonicity). 
 * When dealing with probabilities $P \in [0, 1]$, $P_1 \times P_2 \le P_1$. The product strictly shrinks. 
 * If you imagine $1.0$ as a distance of $0$, and $0.0$ as a distance of $\infty$, multiplying probabilities 
 * behaves exactly like adding distances! (Mathematically: $\max \prod P_i \iff \min \sum -\log(P_i)$).
 * 
 * ASCII Diagram & Dry Run:
 * Consider start = 0, end = 2.
 * 
 *       (0.5)
 *    0 ------- 1
 *     \       /
 * (0.2)\     /(0.5)
 *       \   /
 *         2
 * 
 * Arrays: maxProb = [1.0, 0.0, 0.0]
 * Max-Heap = [(node: 0, prob: 1.0)]
 * 
 * Step 1: Pop (0, 1.0).
 *         - Neighbor 1: 1.0 * 0.5 = 0.5. maxProb[1] = 0.5. Push (1, 0.5).
 *         - Neighbor 2: 1.0 * 0.2 = 0.2. maxProb[2] = 0.2. Push (2, 0.2).
 *         Heap: [(1, 0.5), (2, 0.2)]
 * 
 * Step 2: Pop (1, 0.5) <- Dijkstra optimally picks node 1 over node 2!
 *         - Neighbor 0: 0.5 * 0.5 = 0.25. (Worse than maxProb[0]=1.0). Ignore.
 *         - Neighbor 2: 0.5 * 0.5 = 0.25. (Better than maxProb[2]=0.2!). 
 *           maxProb[2] = 0.25. Push (2, 0.25).
 *         Heap: [(2, 0.25), (2, 0.2)]
 * 
 * Step 3: Pop (2, 0.25). 
 *         - Node 2 is the `end` node! Return 0.25 immediately.
 * 
 * 
 * 5. PITFALLS & PATTERN RECOGNITION
 * ------------------------------------------------
 * Pitfalls:
 * - Using a Min-Heap instead of a Max-Heap. Standard Dijkstra uses a Min-Heap for distances. Here we need a Max-Heap!
 * - Initializing `maxProb` with `Integer.MIN_VALUE`. Probability defaults to 0.0, and `maxProb[start]` must be 1.0.
 * - Missing the lazy deletion step: `if (curr.prob < maxProb[curr.node]) continue;` This prevents TLE when 
 *   a node gets added to the heap multiple times before being processed.
 * 
 * Pattern Recognition: 
 * - When you see: "Maximum product of numbers <= 1" OR "Shortest path but not sum of weights".
 * - Think: Dijkstra's with a Max-Heap (or taking negative logs to convert to standard Dijkstra).
 * - Transfers to: "Network Delay Time", "Cheapest Flights Within K Stops", "Path With Minimum Effort".
 * 
 * 
 * 6. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the graph is huge, and multiplying 10,000 probabilities causes floating-point underflow (rounds to 0.0)?
 *  -> We transform the problem using logarithms. Since $\log(a \times b) = \log(a) + \log(b)$, we can maximize 
 *     the sum of logs. Even better, since probabilities $\le 1$, their logs are negative. We can negate them to make 
 *     them positive weights: $\text{Weight} = -\log(P)$. Now we can use a standard Min-Heap Dijkstra to minimize 
 *     the sum of these positive weights. This entirely prevents underflow.
 * 
 * F2: What if we want the path with the *maximum* probability, but in the event of a tie, we want the path 
 *     with the *fewest edges*?
 *  -> We expand our heap state to `(prob, distance, node)`. Our Max-Heap comparator would first check 
 *     `Double.compare(b.prob, a.prob)`. If they are equal, it falls back to `Integer.compare(a.distance, b.distance)`. 
 *     We also update our `maxProb` array tracking to allow updates if the prob is equal but distance is strictly less.
 * 
 * 
 * 7. SUMMARY
 * ------------------------------------------------
 * Core pattern: Dijkstra's Algorithm adapted for multiplication (Max-Heap).
 * Key observation: Multiplying values $\le 1.0$ is monotonic (strictly non-increasing).
 * Memorize: Java Max-Heap comparator syntax: `(a, b) -> Double.compare(b.prob, a.prob)`.
 * Most common trap: Forgetting to set `maxProb[start] = 1.0` (leaving it as 0.0 means all multiplications equal 0).
 */

import java.util.*;

public class PathWithMaximumProbability {

    // Record to clearly encapsulate graph edges and PriorityQueue states.
    // Using a record is clean, immutable, and ideal for graph node representations.
    record State(int node, double prob) {}

    /**
     * OPTIMAL APPROACH: Dijkstra's Algorithm (Max-Heap)
     * Time Complexity: O(E log E) — because we relax up to E edges, and each heap push/pop takes O(log E).
     * Space Complexity: O(V + E) — because we store the graph O(E), maxProb array O(V), and PQ up to O(E).
     */
    public double maxProbability(int n, int[][] edges, double[] succProb, int start, int end) {
        // Step 1: Build the Adjacency List
        // Map: Node -> List of (Neighbor, EdgeProbability)
        List<List<State>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        
        for (int i = 0; i < edges.length; i++) {
            int u = edges[i][0];
            int v = edges[i][1];
            double prob = succProb[i];
            
            // Undirected graph, so add edge in both directions
            graph.get(u).add(new State(v, prob));
            graph.get(v).add(new State(u, prob));
        }

        // Step 2: Initialize Tracking Array
        // maxProb[i] holds the max probability found so far to reach node i
        double[] maxProb = new double[n];
        maxProb[start] = 1.0; // Probability to reach start from start is 100%

        // Step 3: Initialize Priority Queue (Max-Heap)
        // We want the node with the HIGHEST probability at the top.
        PriorityQueue<State> pq = new PriorityQueue<>((a, b) -> Double.compare(b.prob(), a.prob()));
        pq.offer(new State(start, 1.0));

        // Step 4: Process the Graph
        while (!pq.isEmpty()) {
            State current = pq.poll();
            int currNode = current.node();
            double currProb = current.prob();

            // Early exit: First time we pop the target, it's guaranteed to be the max possible probability
            if (currNode == end) {
                return currProb;
            }

            // Lazy Deletion / Stale State Pruning:
            // If we've previously found a better path to this node before popping this state, discard it.
            if (currProb < maxProb[currNode]) {
                continue;
            }

            // Explore all neighbors
            for (State neighborState : graph.get(currNode)) {
                int nextNode = neighborState.node();
                double edgeProb = neighborState.prob();
                
                // Calculate the probability of reaching the neighbor through the current node
                double nextProb = currProb * edgeProb;

                // If this path is strictly better than the best known path to the neighbor...
                if (nextProb > maxProb[nextNode]) {
                    maxProb[nextNode] = nextProb;
                    pq.offer(new State(nextNode, nextProb));
                }
            }
        }

        // Step 5: If the queue exhausts and we never popped the 'end' node, it's unreachable.
        return 0.0;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        PathWithMaximumProbability solver = new PathWithMaximumProbability();

        System.out.println("--- Testing Path with Maximum Probability ---");

        // Test Case 1: Standard Example (From Dry Run Diagram)
        // Path 0->1->2 yields 0.5 * 0.5 = 0.25
        // Path 0->2 yields 0.2
        int n1 = 3;
        int[][] edges1 = {{0, 1}, {1, 2}, {0, 2}};
        double[] succProb1 = {0.5, 0.5, 0.2};
        int start1 = 0, end1 = 2;
        System.out.println("Test Case 1 (Expected 0.25): " + 
            solver.maxProbability(n1, edges1, succProb1, start1, end1));

        // Test Case 2: Direct path is better
        // Path 0->1->2 yields 0.5 * 0.5 = 0.25
        // Path 0->2 yields 0.3
        int n2 = 3;
        int[][] edges2 = {{0, 1}, {1, 2}, {0, 2}};
        double[] succProb2 = {0.5, 0.5, 0.3};
        int start2 = 0, end2 = 2;
        System.out.println("Test Case 2 (Expected 0.30): " + 
            solver.maxProbability(n2, edges2, succProb2, start2, end2));

        // Test Case 3: Disconnected graph / Unreachable target
        int n3 = 3;
        int[][] edges3 = {{0, 1}};
        double[] succProb3 = {0.5};
        int start3 = 0, end3 = 2;
        System.out.println("Test Case 3 (Expected 0.00): " + 
            solver.maxProbability(n3, edges3, succProb3, start3, end3));

        // Test Case 4: Long chain with 1.0 probabilities (simulating free moves)
        int n4 = 4;
        int[][] edges4 = {{0, 1}, {1, 2}, {2, 3}};
        double[] succProb4 = {1.0, 1.0, 1.0};
        int start4 = 0, end4 = 3;
        System.out.println("Test Case 4 (Expected 1.00): " + 
            solver.maxProbability(n4, edges4, succProb4, start4, end4));
            
        // Test Case 5: 0.0 Probability trap
        int n5 = 3;
        int[][] edges5 = {{0, 1}, {1, 2}, {0, 2}};
        double[] succProb5 = {0.5, 0.0, 0.1};
        int start5 = 0, end5 = 2;
        System.out.println("Test Case 5 (Expected 0.10): " + 
            solver.maxProbability(n5, edges5, succProb5, start5, end5));
    }
}

import java.util.*;

/**
 * Maximum Probability Path
 *
 * Problem:
 * --------
 * We are given an undirected graph where every edge has a probability
 * of successfully traversing that edge.
 *
 * We need to find the path from `start` to `end` whose overall
 * probability of success is maximum.
 *
 * If a path contains edges with probabilities:
 *
 *     p1, p2, p3, ...
 *
 * then the probability of successfully completing the entire path is:
 *
 *     p1 * p2 * p3 * ...
 *
 *
 * Example:
 * --------
 *
 *     A --0.5-- B --0.8-- C
 *
 * Probability of A -> B -> C:
 *
 *     0.5 * 0.8 = 0.4
 *
 *
 * Approach:
 * ---------
 * This is essentially Dijkstra's algorithm with a small modification.
 *
 * Normal Dijkstra:
 *
 *     Find the path with MINIMUM total cost.
 *
 * Here:
 *
 *     Find the path with MAXIMUM total probability.
 *
 * Instead of ADDING edge weights, we MULTIPLY probabilities.
 *
 *
 * Why does Dijkstra work?
 * -----------------------
 *
 * Every probability is between 0 and 1.
 *
 * Therefore, multiplying by another probability can never increase
 * the current probability:
 *
 *     currentProbability * edgeProbability <= currentProbability
 *
 * For example:
 *
 *     0.8 * 0.9 = 0.72
 *     0.8 * 0.5 = 0.40
 *
 * So once the node with the highest probability is removed from
 * the max-heap, we know that we cannot later find a better path
 * to that node.
 *
 *
 * Important data structures:
 * --------------------------
 *
 * 1. adjacencyList
 *
 *    Stores all neighbors of every node along with the probability
 *    of traversing the corresponding edge.
 *
 *
 * 2. maxProbability
 *
 *    maxProbability[node] represents:
 *
 *        The highest probability we currently know for reaching
 *        `node` from `start`.
 *
 *
 * 3. priorityQueue
 *
 *    A MAX-HEAP containing nodes that we can explore next.
 *
 *    The node with the highest probability is always processed first.
 *
 *
 * Time Complexity:
 * ----------------
 *
 * Let:
 *
 *     V = number of vertices
 *     E = number of edges
 *
 * Building the graph:
 *
 *     O(V + E)
 *
 * Dijkstra:
 *
 *     O((V + E) log V)
 *
 * Overall:
 *
 *     O((V + E) log V)
 *
 *
 * Space Complexity:
 * -----------------
 *
 *     O(V + E)
 */
class Solution {

    /**
     * Represents one edge in the adjacency list.
     *
     * `destinationNode`
     *     The node this edge leads to.
     *
     * `edgeProbability`
     *     Probability of successfully traversing this edge.
     */
    record Edge(int destinationNode, double edgeProbability) {}

    /**
     * Represents one entry in the priority queue.
     *
     * `node`
     *     Current node.
     *
     * `pathProbability`
     *     Probability of reaching this node from `start`
     *     using the path represented by this state.
     */
    record State(int node, double pathProbability) {}

    public static double maxProbability(
            int n,
            int[][] edges,
            double[] succProb,
            int start,
            int end) {

        /*
         * ============================================================
         * STEP 1: BUILD THE GRAPH
         * ============================================================
         *
         * The input gives us an edge list:
         *
         *     edges[i] = [a, b]
         *
         * Because the graph is UNDIRECTED:
         *
         *     a -> b
         *     b -> a
         *
         * So we store both directions in the adjacency list.
         *
         * Example:
         *
         *     edges = [[0, 1], [1, 2]]
         *
         * becomes:
         *
         *     0 -> 1
         *     1 -> 0, 2
         *     2 -> 1
         */

        List<List<Edge>> adjacencyList = new ArrayList<>();

        // Create an empty neighbor list for every node.
        for (int node = 0; node < n; node++) {
            adjacencyList.add(new ArrayList<>());
        }

        /*
         * Add every undirected edge to the graph.
         */
        for (int edgeIndex = 0; edgeIndex < edges.length; edgeIndex++) {

            int firstNode = edges[edgeIndex][0];
            int secondNode = edges[edgeIndex][1];

            double edgeProbability = succProb[edgeIndex];

            // firstNode -> secondNode
            adjacencyList
                    .get(firstNode)
                    .add(new Edge(secondNode, edgeProbability));

            // secondNode -> firstNode
            adjacencyList
                    .get(secondNode)
                    .add(new Edge(firstNode, edgeProbability));
        }


        /*
         * ============================================================
         * STEP 2: TRACK THE BEST PROBABILITY FOR EACH NODE
         * ============================================================
         *
         * maxProbability[node] means:
         *
         *     "What is the maximum probability with which we
         *      currently know how to reach this node from start?"
         *
         * Initially, we don't know how to reach any node.
         *
         * So:
         *
         *     maxProbability[all nodes] = 0
         *
         * except for `start`.
         *
         *
         * Why is start = 1.0?
         *
         * We are already at the start node.
         *
         * There is no edge to traverse yet, so the probability
         * of being at start is 100%.
         *
         *     probability(start -> start) = 1.0
         */

        double[] maxProbability = new double[n];

        maxProbability[start] = 1.0;


        /*
         * ============================================================
         * STEP 3: CREATE A MAX-HEAP
         * ============================================================
         *
         * Normal Java PriorityQueue is a MIN-HEAP.
         *
         * But we need the node with the HIGHEST probability first.
         *
         * Therefore, we reverse the comparator and create a MAX-HEAP.
         *
         * Example:
         *
         *     Queue:
         *
         *     (node=3, probability=0.9)
         *     (node=5, probability=0.7)
         *     (node=2, probability=0.4)
         *
         * The queue will remove:
         *
         *     0.9 first
         *     0.7 second
         *     0.4 third
         */

        PriorityQueue<State> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(State::pathProbability)
                                .reversed()
                );


        /*
         * Initially, we are at `start` with probability 1.0.
         *
         * This is better than putting all neighbors of start
         * into the queue manually.
         *
         * From start, we can then explore its neighbors exactly
         * like normal Dijkstra.
         */
        priorityQueue.offer(new State(start, 1.0));


        /*
         * ============================================================
         * STEP 4: DIJKSTRA / BEST-FIRST SEARCH
         * ============================================================
         */
        while (!priorityQueue.isEmpty()) {

            /*
             * Remove the state with the HIGHEST probability.
             *
             * This is the key idea of Dijkstra.
             */
            State currentState = priorityQueue.poll();

            int currentNode = currentState.node();
            double currentPathProbability =
                    currentState.pathProbability();


            /*
             * ========================================================
             * STEP 5: EARLY EXIT
             * ========================================================
             *
             * If we remove `end` from the MAX-HEAP, it means this
             * is currently the highest-probability path available.
             *
             * Could another path to `end` appear later?
             *
             * No.
             *
             * Why?
             *
             * Because every edge probability is <= 1.
             *
             * Any path extending from another state can only keep
             * or reduce its probability.
             *
             * Therefore, when `end` is the highest-probability state
             * in the queue, its probability is optimal.
             */
            if (currentNode == end) {
                return currentPathProbability;
            }


            /*
             * ========================================================
             * STEP 6: IGNORE OUTDATED STATES
             * ========================================================
             *
             * The same node can be inserted into the priority queue
             * multiple times.
             *
             * Example:
             *
             *     We first discover:
             *
             *         0 -> A = 0.4
             *
             *     Later, we discover a better path:
             *
             *         0 -> B -> A = 0.7
             *
             * Both states may exist in the priority queue:
             *
             *         A, 0.4
             *         A, 0.7
             *
             * Once 0.7 becomes the best known probability,
             * processing the 0.4 state is unnecessary.
             *
             * So we skip it.
             */
            if (currentPathProbability < maxProbability[currentNode]) {
                continue;
            }


            /*
             * ========================================================
             * STEP 7: EXPLORE NEIGHBORS
             * ========================================================
             *
             * Suppose:
             *
             *     currentNode = A
             *     probability of reaching A = 0.6
             *
             * and:
             *
             *     A -> B has probability 0.8
             *
             * Then:
             *
             *     probability of reaching B through A
             *
             *         = 0.6 * 0.8
             *         = 0.48
             */

            for (Edge edge : adjacencyList.get(currentNode)) {

                int neighborNode = edge.destinationNode();
                double edgeProbability = edge.edgeProbability();


                /*
                 * Calculate the probability of reaching the neighbor
                 * through the current node.
                 */
                double newPathProbability =
                        currentPathProbability * edgeProbability;


                /*
                 * ====================================================
                 * STEP 8: RELAXATION
                 * ====================================================
                 *
                 * This is the equivalent of "relaxation" in
                 * traditional Dijkstra.
                 *
                 * Normal Dijkstra asks:
                 *
                 *     "Did we find a shorter path?"
                 *
                 * Here we ask:
                 *
                 *     "Did we find a path with higher probability?"
                 *
                 * If yes:
                 *
                 *     1. Update the best probability.
                 *     2. Add the new state to the priority queue.
                 */

                if (newPathProbability > maxProbability[neighborNode]) {

                    // We found a better path to this neighbor.
                    maxProbability[neighborNode] = newPathProbability;

                    // Explore this improved state later.
                    priorityQueue.offer(
                            new State(
                                    neighborNode,
                                    newPathProbability
                            )
                    );
                }
            }
        }


        /*
         * If the priority queue becomes empty without reaching `end`,
         * there is no path from start to end.
         *
         * The problem asks us to return 0 in this case.
         */
        return 0.0;
    }
}
