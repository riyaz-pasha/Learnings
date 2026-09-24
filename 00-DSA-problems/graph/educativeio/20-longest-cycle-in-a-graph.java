/**
 * ============================================================================
 * LONGEST CYCLE IN A GRAPH - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Can a node have a self-loop (an edge pointing to itself)?
 *    -> Why: Establishes the minimum possible cycle length. (Constraint says edges[i] != i, so no self-loops, min cycle length is 2).
 * Q: Are there completely disconnected components in the graph?
 *    -> Why: Confirms we can't just start from node 0 and expect to see everything; we must iterate through all nodes.
 * Q: Is it possible for a node to have NO outgoing edge?
 *    -> Why: Determines if we need to check for null/dead-end pointers. (Constraint says edges[i] == -1 for no edge).
 * Q: Can the graph have multiple independent cycles?
 *    -> Why: Confirms we need to keep a running maximum, not just return the first cycle we find.
 *
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We are given a directed graph where every node has *at most one* outgoing edge 
 * (a "pseudoforest" or "functional graph"). Because no node splits into two paths, paths can merge, 
 * but they never diverge. We need to find the longest cycle without re-traversing the "tails" 
 * (long chains of nodes leading into a cycle) over and over, which is the binding constraint.
 *
 * Approach 1: Naive Path Tracing (Brute Force)
 * - What I'd naturally try: Start a `while` loop from node 0. Keep track of the path using a `HashSet`. 
 *   If we hit a node already in the set, calculate the cycle length. Clear the set, then move to node 1, 
 *   node 2, etc., and repeat the exact same process.
 * - Why it works: It exhaustively traces the path from every single node until it hits a cycle or a dead end.
 * - Why it's too slow: It repeats massive amounts of work. If 1,000 nodes form a long line that eventually 
 *   flows into a cycle, we will traverse that cycle 1,000 separate times. 
 * - Time Complexity: O(N^2) — because for each of the N nodes, we might traverse up to N nodes before finding a cycle.
 * - Space Complexity: O(N) — because we store up to N nodes in a Set for the current path.
 *
 * Approach 2: Kahn's Algorithm / Topological Peeling (The "Elimination" Method)
 * - What work is being repeated in Brute Force: Tracing paths through nodes that we *already know* cannot 
 *   possibly be part of a cycle (like leaves/starting nodes).
 * - What property removes the bottleneck: In a graph where max out-degree is 1, a node with an in-degree of 0 
 *   is definitively a "tail" — it can NEVER be part of a cycle. If we remove it, the node it points to loses an 
 *   incoming edge. If that node's in-degree drops to 0, it's also a tail. 
 * - What I'd try: Count all in-degrees. Put all nodes with in-degree 0 in a Queue. Process the Queue, decrementing 
 *   neighbor in-degrees and queuing them if they hit 0. When the Queue is empty, the ONLY nodes left with 
 *   in-degrees > 0 are nodes strictly inside cycles! Then, just iterate through them to measure the cycle sizes.
 * - Time Complexity: O(N) — because we calculate in-degrees O(N), process the queue O(N), and measure the remaining cycles O(N).
 * - Space Complexity: O(N) — because we store an `inDegree` array of size N and a Queue that can hold up to N elements.
 * 
 * Approach 3: The Global Timer (The Optimum)
 * - What property improves the bottleneck further: Kahn's algorithm requires 3 separate phases (count degrees, 
 *   peel leaves, measure cycles). We can do this in exactly ONE phase if we can just tell the difference between 
 *   "I hit a cycle on my current walk" vs "I merged into a path from a previous walk".
 * - What I'd try: Maintain a single `time` array and a global `timer`. Iterate through all nodes. When walking a path, 
 *   assign `time[curr] = timer++`. If we hit a node that already has a timestamp, we check: 
 *   Was this timestamp assigned *before* I started my current walk? If yes, it's an old path (no new cycle). 
 *   If no, I just hit my own tail! The cycle length is exactly `timer - time[curr]`.
 * - Time Complexity: O(N) — because the global `timer` only ticks up when we visit a completely new node, 
 *   meaning each of the N nodes is visited and timestamped exactly once.
 * - Space Complexity: O(N) — because we only need a single integer array `time` of size N. No HashMaps, 
 *   no Sets, no Queues, no recursion stack.
 * - Decision: Approach 3 is what I'd write in a 45-minute interview. It is mathematically elegant, incredibly 
 *   fast (pure array lookups), and completely avoids the `StackOverflowError` risks of recursive DFS on large constraints.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - No Cycles: A straight line `0 -> 1 -> 2 -> -1`. Returns -1.
 * - Massive Single Cycle: `0 -> 1 -> 2 ... -> 99999 -> 0`. Fails naive recursive DFS immediately (StackOverflow). 
 *   The Iterative Global Timer handles this perfectly.
 * - Multiple Disjoint Cycles: Graph has a cycle of length 3 and a cycle of length 10. `maxCycle` tracks the 10.
 * - Dead Ends (-1): Checked safely in the `while` loop (`curr != -1`).
 *
 * 
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "The Functional Graph Merge"
 * Because every node has at most one outgoing edge, two paths can merge, but they can never diverge. 
 * Therefore, if you start a walk and hit a previously visited node, you are entirely done with the current walk. 
 * You either just closed a brand new cycle, or you merged onto a highway you already explored.
 *
 * ASCII Diagram (The Merge vs The Cycle):
 *    0 ---> 1 ---> 2 ---> 3
 *                  ^      |
 *                  |      v
 *                  5 <--- 4
 * 
 * Dry Run (Using Approach 3: Global Timer):
 * edges = [1, 2, 3, 4, 5, 2]
 * init: time = [0,0,0,0,0,0], timer = 1, maxCycle = -1
 * 
 * i = 0 (startTimer = 1)
 * - visit 0: time[0]=1. curr=1
 * - visit 1: time[1]=2. curr=2
 * - visit 2: time[2]=3. curr=3
 * - visit 3: time[3]=4. curr=4
 * - visit 4: time[4]=5. curr=5
 * - visit 5: time[5]=6. curr=2
 * - hit 2! time[2] is 3. 
 * - Check: is 3 >= startTimer(1)? YES! We hit our own path. 
 * - Cycle length = timer(7) - time[2](3) = 4. maxCycle = 4.
 * 
 * i = 1 (startTimer = 7)
 * - time[1] is already 2 (visited). Loop skips.
 *
 * Pitfalls:
 * - Using standard DFS recursion. With N=100,000, a cycle of length 100,000 will result in 100,000 frames 
 *   on the call stack, blowing up the JVM. Always trace functional graphs iteratively.
 * - Using a `HashSet` to track the current path. Clearing/allocating new Sets for every single node iteration 
 *   will trigger massive Garbage Collection overhead and easily Time Limit Exceed.
 *
 * Pattern Recognition: 
 * - When you see: "At most one outgoing edge" or "edges[i]".
 * - Think: Functional Graph. Use Topological Peeling (Kahn's) or a Global Timestamp array to find cycles in O(N).
 * - Transfers to: "Maximum Employees to Be Invited to a Meeting", "Find Closest Node to Given Two Nodes".
 *
 * 
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if a node could have MULTIPLE outgoing edges?
 *  -> The graph is no longer a functional graph. Paths can diverge, meaning a simple timestamp array isn't enough. 
 *     We would need to use Tarjan's Algorithm or Kosaraju's Algorithm to find Strongly Connected Components (SCCs).
 * 
 * F2: How would you return the actual nodes inside the longest cycle, instead of just the length?
 *  -> When a cycle is detected (`time[curr] >= startTimer`), `curr` is guaranteed to be a node inside the cycle. 
 *     We can simply do a `do-while` loop starting from `curr`, following the edges and adding them to a List, 
 *     until we reach `curr` again.
 *
 * F3: What if we want to find the node that leads to the longest cycle in the shortest amount of steps?
 *  -> We would reverse the graph (creating edges from the cycle outward) and run a Multi-Source BFS starting 
 *     simultaneously from all nodes belonging to the longest cycle.
 */

public class LongestCycle {

    /**
     * OPTIMAL APPROACH: Global Timer (Timestamp Array)
     * Time Complexity: O(N) — Every node is visited and assigned a timestamp exactly once.
     * Space Complexity: O(N) — Only one integer array of size N is used. Zero recursion stack.
     */
    public int longestCycle(int[] edges) {
        int n = edges.length;
        
        // `time[i]` stores the exact global tick when node `i` was visited.
        int[] time = new int[n];
        int timer = 1;
        int maxCycle = -1;
        
        for (int i = 0; i < n; i++) {
            // If the node has already been visited in ANY previous walk, skip it.
            if (time[i] > 0) {
                continue;
            }
            
            int curr = i;
            // Record the timer value exactly when this specific walk started.
            // This allows us to distinguish between merging into an old path vs hitting our own tail.
            int startTimer = timer;
            
            // Walk the path until we hit a dead end (-1) or a previously visited node.
            while (curr != -1 && time[curr] == 0) {
                time[curr] = timer++;
                curr = edges[curr];
            }
            
            // If we didn't hit a dead end, we hit a visited node.
            // Was it visited during THIS specific walk?
            if (curr != -1 && time[curr] >= startTimer) {
                // We closed a cycle!
                // The length is the difference between the current time and the time we first saw the node.
                int cycleLength = timer - time[curr];
                maxCycle = Math.max(maxCycle, cycleLength);
            }
        }
        
        return maxCycle;
    }

    /**
     * ALTERNATIVE APPROACH: Kahn's Algorithm (Topological Peeling)
     * Included to demonstrate the elimination technique, which is excellent for handling functional graphs.
     * Time Complexity: O(N)
     * Space Complexity: O(N)
     */
    public int longestCycleKahns(int[] edges) {
        int n = edges.length;
        int[] inDegree = new int[n];
        
        // Step 1: Count in-degrees
        for (int i = 0; i < n; i++) {
            if (edges[i] != -1) {
                inDegree[edges[i]]++;
            }
        }
        
        // Step 2: Peel away the "tails" (nodes with in-degree 0)
        // Note: Using an array as a queue is a fast, lightweight alternative to LinkedList.
        int[] queue = new int[n];
        int head = 0, tail = 0;
        
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue[tail++] = i;
            }
        }
        
        boolean[] visited = new boolean[n];
        while (head < tail) {
            int curr = queue[head++];
            visited[curr] = true; // Mark non-cycle nodes as visited
            
            int next = edges[curr];
            if (next != -1) {
                inDegree[next]--;
                if (inDegree[next] == 0) {
                    queue[tail++] = next;
                }
            }
        }
        
        // Step 3: Anything left unvisited is strictly part of a cycle. Measure them.
        int maxCycle = -1;
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                int curr = i;
                int length = 0;
                
                // Traverse the cycle
                while (!visited[curr]) {
                    visited[curr] = true;
                    curr = edges[curr];
                    length++;
                }
                
                maxCycle = Math.max(maxCycle, length);
            }
        }
        
        return maxCycle;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        LongestCycle solver = new LongestCycle();
        
        System.out.println("--- Testing Longest Cycle in a Graph ---");

        // Test Case 1: Standard Example with a tail and a cycle (From Dry Run)
        // 0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 2 (Cycle 2-3-4-5 is length 4)
        int[] edges1 = {1, 2, 3, 4, 5, 2};
        System.out.println("Test Case 1 (Expected 4): " + solver.longestCycle(edges1));
        
        // Test Case 2: No Cycles (Straight Line to Dead End)
        // 0 -> 1 -> 2 -> -1
        int[] edges2 = {1, 2, -1};
        System.out.println("Test Case 2 (Expected -1): " + solver.longestCycle(edges2));
        
        // Test Case 3: Multiple Disjoint Cycles
        // Cycle 1: 0 -> 1 -> 0 (Length 2)
        // Cycle 2: 2 -> 3 -> 4 -> 2 (Length 3)
        int[] edges3 = {1, 0, 3, 4, 2};
        System.out.println("Test Case 3 (Expected 3): " + solver.longestCycle(edges3));

        // Test Case 4: Merging into a previously visited cycle
        // 0 -> 1 -> 2 -> 0 (Length 3)
        // 3 -> 1 (Tail merging into cycle)
        // 4 -> 3 (Tail merging into tail)
        int[] edges4 = {1, 2, 0, 1, 3};
        System.out.println("Test Case 4 (Expected 3): " + solver.longestCycle(edges4));
        
        // Test Case 5: Verification of Kahn's Alternative implementation
        System.out.println("Test Case 1 via Kahn's (Expected 4): " + solver.longestCycleKahns(edges1));
    }
}

import java.util.*;

/**
 * Longest Cycle in a Directed Graph
 *
 * ================================================================
 * PROBLEM
 * ================================================================
 *
 * We are given a directed graph represented by:
 *
 *     edges[i] = j
 *
 * This means:
 *
 *     i -> j
 *
 * If:
 *
 *     edges[i] == -1
 *
 * then node `i` has no outgoing edge.
 *
 *
 * Important property:
 * -------------------
 *
 * Every node has AT MOST ONE outgoing edge.
 *
 * For example:
 *
 *     0 -> 1
 *     1 -> 2
 *     2 -> 0
 *
 *     3 -> 4
 *     4 -> -1
 *
 *
 * This type of graph is called a:
 *
 *     Functional Graph
 *
 *
 * We need to find the length of the LONGEST cycle.
 *
 * If there is no cycle:
 *
 *     return -1
 *
 *
 * ================================================================
 * KEY OBSERVATION
 * ================================================================
 *
 * In a normal directed graph, a node can have many outgoing edges.
 *
 * Here every node has at most ONE outgoing edge.
 *
 * Therefore, starting from any node gives us a single chain:
 *
 *
 *     A -> B -> C -> D -> ...
 *
 *
 * Eventually one of two things happens:
 *
 *     1. We reach -1
 *
 *             A -> B -> C -> -1
 *
 *
 *     2. We reach a node we have already seen.
 *
 *             A -> B -> C
 *                  ^    |
 *                  |____|
 *
 *
 * If we reach a node that appeared DURING THE CURRENT TRAVERSAL,
 * then we found a cycle.
 *
 *
 * ================================================================
 * IMPORTANT: GLOBAL VISITED VS CURRENT PATH
 * ================================================================
 *
 * This is the most important concept in this problem.
 *
 *
 * Suppose:
 *
 *     0 -> 1 -> 2 -> 3 -> 1
 *
 *
 * Starting from 0:
 *
 *     0 -> 1 -> 2 -> 3 -> 1
 *
 * When we reach 1 again, we know:
 *
 *     1 -> 2 -> 3 -> 1
 *
 * is a cycle.
 *
 * Its length is:
 *
 *     3
 *
 *
 * But imagine that we later start from another node that eventually
 * reaches node 2.
 *
 * We do NOT want to process the whole chain again.
 *
 * Therefore we maintain:
 *
 *     globallyVisited[node]
 *
 * to mean:
 *
 *     "This node has already been completely processed by some
 *      previous traversal."
 *
 *
 * Separately, we need:
 *
 *     visitTime[node]
 *
 * to tell us whether the node belongs to the CURRENT traversal.
 *
 *
 * ================================================================
 * WHY TIMESTAMPS?
 * ================================================================
 *
 * Instead of creating a new HashMap for every starting node,
 * we give every visited node a timestamp.
 *
 * Example:
 *
 * Traversal starting from node 0:
 *
 *     node:       0   1   2   3
 *     visitTime:  1   2   3   4
 *
 *
 * Suppose we encounter node 2 again.
 *
 * Current time = 5
 *
 * node 2 was first seen at time 3.
 *
 * Therefore:
 *
 *     cycleLength = 5 - 3
 *                 = 2
 *
 * More generally:
 *
 *     cycleLength = currentTime - visitTime[cycleStart]
 *
 *
 * ================================================================
 * WHY CAN WE DETECT A CYCLE USING visitTime?
 * ================================================================
 *
 * Suppose the traversal is:
 *
 *     A -> B -> C -> D -> B
 *
 * We recorded:
 *
 *     A = time 1
 *     B = time 2
 *     C = time 3
 *     D = time 4
 *
 * Now we reach B again at time 5.
 *
 * The cycle is:
 *
 *     B -> C -> D -> B
 *
 * Number of nodes:
 *
 *     5 - 2 = 3
 *
 *
 * ================================================================
 * EXAMPLE
 * ================================================================
 *
 * edges =
 *
 *     [3, 3, 4, 2, 3]
 *
 * Graph:
 *
 *     0 -> 3
 *     1 -> 3
 *     2 -> 4
 *     3 -> 2
 *     4 -> 3
 *
 *
 * Starting from 0:
 *
 *     0 -> 3 -> 2 -> 4 -> 3
 *
 * Cycle:
 *
 *     3 -> 2 -> 4 -> 3
 *
 * Length:
 *
 *     3
 *
 *
 * ================================================================
 * WHY O(n)?
 * ================================================================
 *
 * Each node becomes globally visited only once.
 *
 * Once:
 *
 *     globallyVisited[node] == true
 *
 * we never traverse from that node again.
 *
 * Therefore, across ALL starting points, the total number of
 * node visits is at most O(n).
 *
 * Time:
 *
 *     O(n)
 *
 * Space:
 *
 *     O(n)
 *
 *
 * ================================================================
 */
public class LongestCycleInGraph {

    /**
     * Finds the length of the longest cycle in the graph.
     *
     * @param edges
     *        edges[i] = destination of node i
     *        -1 means node i has no outgoing edge
     *
     * @return length of the longest cycle, or -1 if no cycle exists
     */
    public int longestCycle(int[] edges) {

        int numberOfNodes = edges.length;


        /*
         * ============================================================
         * STEP 1: GLOBAL VISITED ARRAY
         * ============================================================
         *
         * globallyVisited[node] tells us whether this node has
         * already been processed as part of some previous traversal.
         *
         * Example:
         *
         *     globallyVisited[3] = true
         *
         * means:
         *
         *     "We have already followed the graph starting from
         *      some node and processed node 3."
         *
         *
         * Why do we need this?
         *
         * Without it, we might repeatedly traverse the same chain.
         *
         * Example:
         *
         *     0 -> 1 -> 2 -> 3 -> -1
         *
         * Starting from 0 processes:
         *
         *     0,1,2,3
         *
         * Starting from 1 would unnecessarily process:
         *
         *     1,2,3
         *
         * Starting from 2:
         *
         *     2,3
         *
         * etc.
         *
         * Global visited prevents all of this.
         */
        boolean[] globallyVisited =
                new boolean[numberOfNodes];


        /*
         * ============================================================
         * STEP 2: VISIT TIMESTAMP ARRAY
         * ============================================================
         *
         * visitTime[node] stores the timestamp at which this node
         * was visited DURING ITS CURRENT TRAVERSAL.
         *
         * Example:
         *
         *     visitTime[0] = 10
         *     visitTime[1] = 11
         *     visitTime[2] = 12
         *
         *
         * IMPORTANT:
         *
         * We use the timestamp to determine whether a repeated node
         * belongs to the CURRENT traversal.
         *
         *
         * Why not just use globallyVisited?
         *
         * Because:
         *
         *     globallyVisited[node] == true
         *
         * only tells us:
         *
         *     "This node was seen sometime in the past."
         *
         * It does NOT tell us whether the node is part of the
         * current path.
         *
         *
         * Example:
         *
         *     Previous traversal:
         *
         *         5 -> 6 -> 7 -> -1
         *
         * Later:
         *
         *         0 -> 5
         *
         * When we reach 5, it is globally visited.
         *
         * But that does NOT mean:
         *
         *     0 -> 5
         *
         * contains a cycle.
         *
         * It simply reaches a chain that was already processed.
         */
        int[] visitTime =
                new int[numberOfNodes];


        /*
         * Stores the longest cycle discovered so far.
         *
         * -1 means:
         *
         *     "No cycle has been found yet."
         */
        int longestCycleLength = -1;


        /*
         * A monotonically increasing timestamp.
         *
         * Every time we visit a new node, we assign the current
         * timestamp to it and then increment the timestamp.
         *
         * Starting from 1 makes the default value:
         *
         *     visitTime[node] == 0
         *
         * mean:
         *
         *     "This node has never received a timestamp."
         */
        int currentTime = 1;


        /*
         * ============================================================
         * STEP 3: START A TRAVERSAL FROM EVERY UNPROCESSED NODE
         * ============================================================
         */
        for (int startingNode = 0;
             startingNode < numberOfNodes;
             startingNode++) {


            /*
             * If this node was already processed as part of an
             * earlier traversal, there is no reason to start again.
             */
            if (globallyVisited[startingNode]) {
                continue;
            }


            /*
             * `currentNode` represents the node we are currently
             * following.
             *
             * Because every node has at most one outgoing edge,
             * there is no need for a DFS stack or recursive DFS.
             *
             * We can simply follow:
             *
             *     currentNode -> edges[currentNode]
             */
            int currentNode = startingNode;


            /*
             * ========================================================
             * STEP 4: FOLLOW THE CHAIN
             * ========================================================
             *
             * Continue while:
             *
             *     1. We haven't reached -1.
             *     2. We haven't reached a globally processed node.
             *
             *
             * Why stop at a globally visited node?
             *
             * Because any cycle involving that node would already
             * have been discovered when that node was processed.
             */
            while (currentNode != -1 &&
                   !globallyVisited[currentNode]) {


                /*
                 * Mark this node as globally processed.
                 */
                globallyVisited[currentNode] = true;


                /*
                 * Record the timestamp at which this node was
                 * encountered in this traversal.
                 */
                visitTime[currentNode] = currentTime;


                /*
                 * Move to the only possible next node.
                 */
                currentNode = edges[currentNode];


                /*
                 * Increment the timestamp for the next node.
                 */
                currentTime++;
            }


            /*
             * ========================================================
             * STEP 5: DETERMINE WHY THE TRAVERSAL STOPPED
             * ========================================================
             *
             * There are two possibilities.
             *
             * --------------------------------------------------------
             * Case 1:
             *
             *     currentNode == -1
             *
             * We reached the end of a chain.
             *
             * Example:
             *
             *     0 -> 1 -> 2 -> -1
             *
             * There is no cycle.
             *
             *
             * --------------------------------------------------------
             * Case 2:
             *
             *     currentNode != -1
             *
             * We encountered a node that was already globally visited.
             *
             * But this alone does NOT necessarily mean we found a
             * cycle in the current traversal.
             *
             * The node could have been visited during an EARLIER
             * traversal.
             *
             * Therefore we need one more check.
             */
            if (currentNode != -1) {


                /*
                 * ====================================================
                 * STEP 6: CHECK WHETHER THE NODE BELONGS TO CURRENT
                 *         TRAVERSAL
                 * ====================================================
                 *
                 * We compare the timestamp of the node against the
                 * timestamps belonging to the current traversal.
                 *
                 *
                 * Since `currentTime` has been incremented after
                 * visiting each node, the current traversal contains
                 * timestamps in the range:
                 *
                 *     [startTime, currentTime - 1]
                 *
                 *
                 * We need to know where this traversal started.
                 *
                 * The easiest way is to derive the starting timestamp
                 * from the timestamp assigned to `startingNode`.
                 *
                 * However, a cleaner implementation is to capture
                 * the timestamp BEFORE starting the traversal.
                 */
            }
        }

        return longestCycleLength;
    }
}

import java.util.*;

/**
 * Longest Cycle in a Directed Graph
 *
 * ---------------------------------------------------------------
 * Graph Structure
 * ---------------------------------------------------------------
 *
 * edges[i] = j
 *
 * means:
 *
 *     i -> j
 *
 * If:
 *
 *     edges[i] == -1
 *
 * then node i has no outgoing edge.
 *
 * Every node has AT MOST one outgoing edge.
 *
 *
 * Example:
 *
 *     edges = [3, 3, 4, 2, 3]
 *
 * gives:
 *
 *     0 -> 3
 *     1 -> 3
 *     2 -> 4
 *     3 -> 2
 *     4 -> 3
 *
 *
 * Cycle:
 *
 *     3 -> 2 -> 4 -> 3
 *
 * Length = 3
 *
 *
 * ---------------------------------------------------------------
 * Core Idea
 * ---------------------------------------------------------------
 *
 * Starting from any node produces a single chain:
 *
 *     A -> B -> C -> D -> ...
 *
 * Eventually:
 *
 *     A. We reach -1
 *
 *        A -> B -> C -> -1
 *
 *     OR
 *
 *     B. We reach a node already seen in the CURRENT traversal
 *
 *        A -> B -> C
 *             ^    |
 *             |____|
 *
 * The second case means we found a cycle.
 *
 *
 * ---------------------------------------------------------------
 * Two kinds of information
 * ---------------------------------------------------------------
 *
 * 1. globallyVisited
 *
 *    Tells us whether a node has already been completely processed
 *    by some traversal.
 *
 *    This prevents repeatedly walking the same chains.
 *
 *
 * 2. visitTime
 *
 *    Stores when the node was visited.
 *
 *    This allows us to calculate the cycle length:
 *
 *        cycleLength =
 *            currentTime - visitTime[cycleStart]
 *
 *
 * ---------------------------------------------------------------
 * Why timestamps?
 * ---------------------------------------------------------------
 *
 * Suppose the traversal is:
 *
 *     5 -> 7 -> 2 -> 9 -> 7
 *
 * Assign timestamps:
 *
 *     5 -> 10
 *     7 -> 11
 *     2 -> 12
 *     9 -> 13
 *
 * We reach 7 again at time 14.
 *
 * Cycle starts at timestamp 11.
 *
 * Therefore:
 *
 *     cycleLength = 14 - 11 = 3
 *
 * The cycle is:
 *
 *     7 -> 2 -> 9 -> 7
 *
 *
 * ---------------------------------------------------------------
 * Complexity
 * ---------------------------------------------------------------
 *
 * Each node is globally processed at most once.
 *
 * Time:
 *
 *     O(n)
 *
 * Space:
 *
 *     O(n)
 */
public class LongestCycleInGraph {

    public int longestCycle(int[] edges) {

        int numberOfNodes = edges.length;


        /*
         * -----------------------------------------------------------
         * globallyVisited[node]
         *
         * true means:
         *
         *     This node has already been processed during an earlier
         *     traversal.
         *
         * This prevents us from repeatedly walking the same chain.
         * -----------------------------------------------------------
         */
        boolean[] globallyVisited =
                new boolean[numberOfNodes];


        /*
         * -----------------------------------------------------------
         * visitTime[node]
         *
         * Stores the timestamp at which the node was visited.
         *
         * Example:
         *
         *     visitTime[5] = 10
         *     visitTime[7] = 11
         *     visitTime[2] = 12
         *
         * A value of 0 means the node has never been assigned a
         * timestamp.
         * -----------------------------------------------------------
         */
        int[] visitTime =
                new int[numberOfNodes];


        /*
         * Longest cycle found so far.
         *
         * -1 means no cycle has been found.
         */
        int longestCycleLength = -1;


        /*
         * Global timestamp.
         *
         * It keeps increasing throughout the entire algorithm.
         *
         * We deliberately DO NOT reset this for every starting node.
         */
        int currentTime = 1;


        /*
         * Try starting a traversal from every node.
         */
        for (int startingNode = 0;
             startingNode < numberOfNodes;
             startingNode++) {


            /*
             * This node has already been processed as part of an
             * earlier traversal.
             *
             * Therefore, everything reachable from it has already
             * been handled as well.
             */
            if (globallyVisited[startingNode]) {
                continue;
            }


            /*
             * Remember the timestamp at which THIS traversal starts.
             *
             * This is extremely important.
             *
             * Later, if we encounter a globally visited node, we need
             * to distinguish:
             *
             *     "Was it visited during THIS traversal?"
             *
             * from:
             *
             *     "Was it visited during an EARLIER traversal?"
             */
            int traversalStartTime = currentTime;


            /*
             * Start following the chain.
             */
            int currentNode = startingNode;


            /*
             * --------------------------------------------------------
             * Follow the graph.
             * --------------------------------------------------------
             *
             * Because every node has at most one outgoing edge,
             * there is only one possible next node:
             *
             *     currentNode -> edges[currentNode]
             */
            while (currentNode != -1 &&
                   !globallyVisited[currentNode]) {

                /*
                 * Mark node as globally processed.
                 */
                globallyVisited[currentNode] = true;


                /*
                 * Record when this node was reached in this traversal.
                 */
                visitTime[currentNode] = currentTime;


                /*
                 * Move to the next node.
                 */
                currentNode = edges[currentNode];


                /*
                 * Advance the timestamp.
                 */
                currentTime++;
            }


            /*
             * --------------------------------------------------------
             * Why did we stop?
             * --------------------------------------------------------
             *
             * If:
             *
             *     currentNode == -1
             *
             * then we reached the end of a chain.
             *
             * No cycle.
             */
            if (currentNode == -1) {
                continue;
            }


            /*
             * --------------------------------------------------------
             * We reached a globally visited node.
             *
             * Does this mean there is a cycle?
             *
             * NOT NECESSARILY.
             *
             * It could be a node visited by an earlier traversal.
             *
             * Example:
             *
             *     First traversal:
             *
             *         5 -> 6 -> 7 -> -1
             *
             *     Second traversal:
             *
             *         0 -> 5
             *
             * When second traversal reaches 5:
             *
             *     globallyVisited[5] == true
             *
             * But there is no cycle.
             *
             * --------------------------------------------------------
             */


            /*
             * Check whether the node was visited during THIS traversal.
             *
             * If:
             *
             *     visitTime[currentNode] >= traversalStartTime
             *
             * then the node was encountered after this traversal began.
             *
             * Therefore it belongs to the current path.
             *
             * That means we found a cycle.
             */
            if (visitTime[currentNode] >= traversalStartTime) {

                /*
                 * Example:
                 *
                 *     traversalStartTime = 10
                 *
                 *     Node timestamps:
                 *
                 *         A = 10
                 *         B = 11
                 *         C = 12
                 *         D = 13
                 *
                 *     We reach B again.
                 *
                 *     currentTime = 14
                 *
                 * Cycle:
                 *
                 *         B -> C -> D -> B
                 *
                 * Cycle length:
                 *
                 *         14 - 11 = 3
                 */
                int cycleLength =
                        currentTime - visitTime[currentNode];


                /*
                 * Keep the largest cycle found.
                 */
                longestCycleLength =
                        Math.max(
                                longestCycleLength,
                                cycleLength
                        );
            }
        }


        /*
         * Return:
         *
         *     longest cycle length
         *
         * or:
         *
         *     -1 if no cycle exists.
         */
        return longestCycleLength;
    }
}

