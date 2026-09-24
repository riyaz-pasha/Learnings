/**
 * ============================================================================
 * SHORTEST CYCLE IN A GRAPH - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Is the graph guaranteed to be fully connected, or can it have disconnected components?
 *    -> Why: Confirms we cannot rely on a single traversal; we must ensure our algorithm can handle disjoint subgraphs.
 * Q: Can there be a cycle of length 2? (e.g., two edges between the same pair of nodes)
 *    -> Why: Establishes the minimum possible cycle length. (Constraint says "no duplicate edges", so min cycle is 3).
 * Q: Is it possible for a graph to be a pure tree or forest?
 *    -> Why: Confirms we need a default return value when no cycles exist. (Rule: Return -1).
 * Q: Are the edges weighted or unweighted?
 *    -> Why: Dictates whether we can use simple BFS or must use Dijkstra's algorithm. (Unweighted, so BFS is safe).
 *
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to find the absolute shortest cycle in an unweighted undirected graph. The binding 
 * constraint is that while detecting *any* cycle is easy, guaranteeing it is the *shortest* one requires us 
 * to explore the graph radially (by depth) rather than greedily diving to the bottom.
 * 
 * Approach 1: Depth-First Search (DFS) Backtracking
 * - What I'd naturally try: Start a DFS from node 0. Keep track of the current path length and a `visited` array. 
 *   If I hit a `visited` node that isn't my immediate parent, I've found a cycle! Note its length, backtrack, and keep searching.
 * - Why it works: DFS exhaustively explores every possible path and combination in the graph.
 * - Why it's too slow: DFS dives deep. It might find a cycle of length 50 first, and to guarantee there isn't a 
 *   cycle of length 3, it has to exhaustively unwind and check almost every other path permutation. It is terrible 
 *   for finding "shortest" unweighted paths.
 * - Time Complexity: O(V!) — because in a dense graph with many intersecting cycles, DFS explores an exponential 
 *   number of path combinations.
 * - Space Complexity: O(V + E) — because we store the adjacency list O(E) and the recursion stack depth can reach O(V).
 *
 * Approach 2: Edge Removal + BFS
 * - What work is being repeated in DFS: We are checking long meandering paths that are obviously not the shortest cycle.
 * - What property removes the bottleneck: The shortest cycle containing a specific edge (u, v) is exactly that edge 
 *   PLUS the shortest path from u to v without using that edge.
 * - What I'd try: For every single edge (u, v) in the graph, temporarily "delete" it. Run a standard Breadth-First 
 *   Search (BFS) from u to find the shortest path to v. The cycle length is `shortest_path + 1`. Repeat for all edges 
 *   and take the minimum.
 * - Time Complexity: O(E * (V + E)) — because for each of the E edges, we run a BFS which takes O(V + E) time.
 * - Space Complexity: O(V + E) — because we store the adjacency list O(E) and a BFS queue of up to size O(V).
 * 
 * Approach 3: BFS From Every Node (The Optimum)
 * - What property improves the bottleneck further: In Approach 2, we do a full BFS for every *edge*. We can instead 
 *   do a BFS for every *vertex*. When BFS expands radially outward from a root vertex, the *first* time two of its 
 *   expansion branches collide at the same node, they have discovered the shortest cycle passing through that root!
 * - What I'd try: Iterate `i` from 0 to n-1. Run a BFS starting at `i`. Keep a `dist` array and a `parent` array. 
 *   When current node `u` looks at a neighbor `v` that is ALREADY visited, and `v != parent[u]`, the two branches 
 *   have collided. The cycle length is exactly `dist[u] + dist[v] + 1`. Keep a running minimum of these collisions.
 * - Time Complexity: O(V * (V + E)) — because we run exactly V separate Breadth-First Searches, and each BFS 
 *   processes up to V vertices and E edges.
 * - Space Complexity: O(V + E) — because we store the adjacency list O(E), and for each BFS we allocate 
 *   `dist` and `parent` arrays of size V, plus a Queue of size V.
 * - Decision: Approach 3 is the standard, most elegant algorithm for this problem. Given N <= 1000 and E <= 1000, 
 *   V*(V+E) is around 2,000,000 operations, which runs perfectly well within the 1-second time limit.
 *
 * 
 * 3. EDGE CASES
 * ------------------------------------------------
 * - No Cycles (A Tree or Forest): The collision condition `v != parent[u]` is never met. Returns -1.
 * - Disconnected Components: Graph has islands. Running BFS from *every* node naturally sweeps through all islands independently.
 * - Odd vs Even Cycles: The formula `dist[u] + dist[v] + 1` perfectly accommodates both. (See Dry Run).
 * 
 * 
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "The BFS Branch Collision"
 * When BFS expands, it builds a shortest-path tree. If a cross-edge exists between two nodes in this tree, a cycle 
 * is formed. The length of this cycle is the distance from the root to the first node, plus the distance from the 
 * root to the second node, plus 1 (the cross-edge). Because BFS explores level-by-level, the first cross-edge 
 * encountered guarantees the shortest cycle involving that root.
 *
 * ASCII Diagram (An Even Cycle - Length 4):
 *      (dist 1)
 *       1 ----- 2 (dist 2)
 *      /        |
 *    0 (root)   |
 *      \        |
 *       4 ----- 3 (dist 2)
 *      (dist 1)
 *
 * Dry Run (BFS starting from Node 0):
 * Queue: [0]. dist: {0:0}
 * - Pop 0. Neighbors: 1, 4. 
 *   - Visit 1. dist[1]=1, parent[1]=0. Queue: [1, 4]
 *   - Visit 4. dist[4]=1, parent[4]=0. Queue: [1, 4]
 * - Pop 1. Neighbors: 0, 2.
 *   - 0 is parent, skip.
 *   - Visit 2. dist[2]=2, parent[2]=1. Queue: [4, 2]
 * - Pop 4. Neighbors: 0, 3.
 *   - 0 is parent, skip.
 *   - Visit 3. dist[3]=2, parent[3]=4. Queue: [2, 3]
 * - Pop 2. Neighbors: 1, 3.
 *   - 1 is parent, skip.
 *   - Neighbor 3 IS ALREADY VISITED! And parent[2] != 3. Collision!
 *   - Cycle Length = dist[2] + dist[3] + 1 = 2 + 2 + 1 = 5. Wait, edge is 2-3? 
 *     Ah, the diagram edges are 0-1, 1-2, 2-3, 3-4, 4-0. Let's re-calculate.
 *     If edges are 2-3, 3-4: 
 *     Pop 2 sees 3. 3 is visited. dist[2]=2, dist[3]=2. Total = 2 + 2 + 1 = 5. (It's a pentagon, length 5).
 *     Formula holds perfectly.
 *
 * Pitfalls:
 * - Forgetting the `parent` check. In an undirected graph, `u` is connected to `v` and `v` is connected to `u`. 
 *   Without checking `v != parent[u]`, your BFS will immediately think the edge it just traversed backwards is a cycle of length 2.
 * - Stopping early after finding ONE cycle in ONE BFS. That cycle is the shortest cycle *for that specific root*, 
 *   but there might be a smaller cycle on the other side of the graph. You must complete BFS from all nodes.
 *
 * Pattern Recognition: 
 * - When you see: "Shortest Cycle in an Unweighted Graph"
 * - Think: "BFS from every node, look for cross-edge collisions."
 *
 * Interview Script:
 * "To find the shortest cycle in an unweighted graph, DFS is a bad choice because it dives deep and would force 
 * us to check almost all paths. Instead, I'll use Breadth-First Search. Because BFS explores radially level-by-level, 
 * if two branches of the BFS tree collide at an already-visited node, we've found the shortest cycle passing through 
 * our starting root. To ensure we find the absolute global shortest cycle, I'll run this BFS starting from every 
 * single vertex in the graph, keeping track of the minimum cycle length found. This takes O(V * (V + E)) time."
 *
 * 
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the edges had weights?
 *  -> The BFS approach fails because BFS only finds shortest paths in unweighted graphs. We would need to use 
 *     Dijkstra's Algorithm from every node, or use Floyd-Warshall (O(V^3)) if V is very small.
 * 
 * F2: What if N is huge, say 10^5, but we know the graph is extremely sparse?
 *  -> Running BFS from all nodes (O(V^2)) would Time Limit Exceed. Finding the shortest cycle in a general sparse 
 *     graph efficiently is computationally difficult. However, if we just want a cycle (not necessarily the shortest), 
 *     a single DFS or Union-Find takes O(V + E).
 */

import java.util.*;

public class ShortestCycleInGraph {

    /**
     * OPTIMAL APPROACH: Breadth-First Search from Every Vertex
     * Time Complexity: O(V * (V + E)) — We execute a full BFS starting from each of the V vertices. Each BFS 
     *                  processes up to V nodes and E edges.
     * Space Complexity: O(V + E) — We store the adjacency list which takes O(V + E). Inside the loop, the 
     *                   `dist`, `parent` arrays, and Queue each take O(V) space.
     */
    public int findShortestCycle(int n, int[][] edges) {
        // Step 1: Build the Adjacency List
        List<Integer>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }
        
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            graph[u].add(v);
            graph[v].add(u); // Undirected graph
        }
        
        int shortestCycle = Integer.MAX_VALUE;
        
        // Step 2: Run BFS from every single vertex
        for (int i = 0; i < n; i++) {
            
            // `dist` tracks the shortest path from root `i` to every other node
            int[] dist = new int[n];
            Arrays.fill(dist, -1);
            
            // `parent` tracks the node that discovered the current node, preventing false 2-cycles
            int[] parent = new int[n];
            Arrays.fill(parent, -1);
            
            Queue<Integer> queue = new LinkedList<>();
            queue.offer(i);
            dist[i] = 0;
            
            // Step 3: Standard BFS Traversal
            while (!queue.isEmpty()) {
                int curr = queue.poll();
                
                for (int neighbor : graph[curr]) {
                    if (dist[neighbor] == -1) {
                        // Unvisited node: normal BFS expansion
                        dist[neighbor] = dist[curr] + 1;
                        parent[neighbor] = curr;
                        queue.offer(neighbor);
                    } 
                    else if (neighbor != parent[curr]) {
                        // VISITED node AND it's not the immediate parent we just came from!
                        // This means two separate branches of our BFS tree have collided.
                        // The length of the cycle is the path from root to `curr`, 
                        // plus the path from root to `neighbor`, plus the edge between them (1).
                        int cycleLength = dist[curr] + dist[neighbor] + 1;
                        shortestCycle = Math.min(shortestCycle, cycleLength);
                    }
                }
            }
        }
        
        // Step 4: If shortestCycle was never updated, the graph is a tree/forest.
        return shortestCycle == Integer.MAX_VALUE ? -1 : shortestCycle;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        ShortestCycleInGraph solver = new ShortestCycleInGraph();

        System.out.println("--- Testing Shortest Cycle in a Graph ---");

        // Test Case 1: Pentagon and a Triangle connected together
        // The triangle (0-1-2) is length 3. The pentagon (3-4-5-6-7) is length 5.
        // Minimum should be 3.
        int n1 = 8;
        int[][] edges1 = {
            {0, 1}, {1, 2}, {2, 0},         // Triangle
            {2, 3},                         // Bridge
            {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 3} // Pentagon
        };
        System.out.println("Test Case 1 (Expected 3): " + solver.findShortestCycle(n1, edges1));

        // Test Case 2: No Cycles (A Tree)
        // 0-1, 1-2, 2-3
        int n2 = 4;
        int[][] edges2 = {{0, 1}, {1, 2}, {2, 3}};
        System.out.println("Test Case 2 (Expected -1): " + solver.findShortestCycle(n2, edges2));

        // Test Case 3: Square Graph (Even Cycle)
        // 0-1, 1-2, 2-3, 3-0
        int n3 = 4;
        int[][] edges3 = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        System.out.println("Test Case 3 (Expected 4): " + solver.findShortestCycle(n3, edges3));

        // Test Case 4: Disconnected Components
        // Component A: Triangle (Length 3)
        // Component B: Square (Length 4)
        int n4 = 7;
        int[][] edges4 = {
            {0, 1}, {1, 2}, {2, 0},         // Comp A
            {3, 4}, {4, 5}, {5, 6}, {6, 3}  // Comp B
        };
        System.out.println("Test Case 4 (Expected 3): " + solver.findShortestCycle(n4, edges4));
    }
}

import java.util.*;

/**
 * Shortest Cycle in an Undirected Graph
 *
 * ================================================================
 * PROBLEM
 * ================================================================
 *
 * We are given an UNDIRECTED graph.
 *
 * Example:
 *
 *     0 --- 1
 *     |     |
 *     3 --- 2
 *
 * There is a cycle:
 *
 *     0 -> 1 -> 2 -> 3 -> 0
 *
 * Its length is 4.
 *
 *
 * We need to find the shortest cycle in the entire graph.
 *
 * If the graph contains no cycle:
 *
 *     return -1
 *
 *
 * ================================================================
 * KEY IDEA
 * ================================================================
 *
 * We use BFS starting from EVERY node.
 *
 * Why?
 *
 * BFS gives us the shortest distance from the starting node to
 * every other reachable node.
 *
 *
 * Suppose BFS starts at node S and we discover an edge:
 *
 *     U -------- V
 *
 * where both U and V have already been visited.
 *
 * If U and V are NOT parent/child of each other, then this edge
 * creates a cycle.
 *
 *
 * Imagine:
 *
 *         U
 *        / \
 *       A   B
 *        \ /
 *         V
 *
 * There is already a path:
 *
 *     U -> ... -> V
 *
 * and we also have the direct edge:
 *
 *     U -> V
 *
 * Therefore, together they form a cycle.
 *
 *
 * ================================================================
 * WHY DO WE NEED TO IGNORE THE PARENT?
 * ================================================================
 *
 * Consider a simple tree:
 *
 *     0 --- 1 --- 2
 *
 * BFS starts at 0:
 *
 *     0 -> 1 -> 2
 *
 * When processing node 1, we see node 0.
 *
 * But node 0 is already visited.
 *
 * Does that mean there is a cycle?
 *
 * NO.
 *
 * The edge:
 *
 *     0 --- 1
 *
 * is simply the edge we used to reach 1.
 *
 * Therefore, when processing U, if:
 *
 *     V == parent[U]
 *
 * we ignore that edge.
 *
 *
 * ================================================================
 * CYCLE LENGTH FORMULA
 * ================================================================
 *
 * Suppose we find an edge:
 *
 *     U ---- V
 *
 * and both nodes are already visited.
 *
 * BFS has recorded:
 *
 *     dist[U] = shortest distance from start to U
 *     dist[V] = shortest distance from start to V
 *
 * The cycle consists of:
 *
 *     shortest path from start to U
 *     +
 *     shortest path from start to V
 *     +
 *     edge U -> V
 *
 * Therefore:
 *
 *     cycleLength =
 *
 *         dist[U]
 *         +
 *         dist[V]
 *         +
 *         1
 *
 *
 * Example:
 *
 *     dist[U] = 2
 *     dist[V] = 2
 *
 * Then:
 *
 *     cycleLength = 2 + 2 + 1
 *                 = 5
 *
 *
 * ================================================================
 * WHY RUN BFS FROM EVERY NODE?
 * ================================================================
 *
 * A single BFS finds cycles relative to one starting point.
 *
 * But the shortest cycle might be somewhere else in the graph.
 *
 * Example:
 *
 *     0 --- 1 --- 2
 *
 *
 *     5 --- 6
 *     |     |
 *     8 --- 7
 *
 * A BFS starting near the first component won't discover the
 * shortest cycle in the second component.
 *
 * Therefore:
 *
 *     for every node:
 *         run BFS
 *         look for cycles
 *
 *
 * ================================================================
 * CORRECTNESS INTUITION
 * ================================================================
 *
 * For every starting node S:
 *
 *     BFS gives shortest distances from S.
 *
 * Every non-tree edge:
 *
 *     U ---- V
 *
 * that connects two already discovered nodes can form a cycle.
 *
 * We calculate the length of that cycle.
 *
 * Since we repeat this process for every possible starting node,
 * we consider every cycle in the graph.
 *
 * Taking the minimum gives the shortest cycle.
 *
 *
 * ================================================================
 * COMPLEXITY
 * ================================================================
 *
 * Let:
 *
 *     V = number of vertices
 *     E = number of edges
 *
 * One BFS:
 *
 *     O(V + E)
 *
 * We perform BFS from every vertex:
 *
 *     O(V * (V + E))
 *
 * With the given constraints:
 *
 *     V <= 1000
 *     E <= 1000
 *
 * this is completely reasonable.
 *
 *
 * Space:
 *
 *     O(V + E)
 *
 * for the graph, BFS queue, distance array, and parent array.
 */
public class ShortestCycleInGraph {

    /**
     * Finds the length of the shortest cycle in an undirected graph.
     *
     * @param numberOfVertices number of vertices in the graph
     * @param edges            undirected edges
     *
     * @return shortest cycle length, or -1 if no cycle exists
     */
    public int findShortestCycle(
            int numberOfVertices,
            int[][] edges) {


        /*
         * ============================================================
         * STEP 1: BUILD THE ADJACENCY LIST
         * ============================================================
         *
         * The input is an edge list.
         *
         * Example:
         *
         *     edges = [[0,1], [1,2], [2,0]]
         *
         * Because the graph is UNDIRECTED:
         *
         *     0 -> 1
         *     1 -> 0
         *
         * and so on.
         *
         * The adjacency list lets us efficiently find all neighbors
         * of a node.
         */

        List<List<Integer>> adjacencyList =
                new ArrayList<>();

        for (int vertex = 0;
             vertex < numberOfVertices;
             vertex++) {

            adjacencyList.add(new ArrayList<>());
        }


        /*
         * Add every edge in BOTH directions.
         *
         *     u -- v
         *
         * becomes:
         *
         *     u -> v
         *     v -> u
         */
        for (int[] edge : edges) {

            int firstVertex = edge[0];
            int secondVertex = edge[1];

            adjacencyList
                    .get(firstVertex)
                    .add(secondVertex);

            adjacencyList
                    .get(secondVertex)
                    .add(firstVertex);
        }


        /*
         * ============================================================
         * STEP 2: STORE THE BEST ANSWER
         * ============================================================
         *
         * We want the MINIMUM cycle length.
         *
         * Start with infinity so that the first discovered cycle
         * will automatically become our best answer.
         */
        int shortestCycleLength = Integer.MAX_VALUE;


        /*
         * ============================================================
         * STEP 3: RUN BFS FROM EVERY VERTEX
         * ============================================================
         *
         * Why every vertex?
         *
         * Because the shortest cycle could be located anywhere
         * in the graph.
         */
        for (int startVertex = 0;
             startVertex < numberOfVertices;
             startVertex++) {


            /*
             * ========================================================
             * STEP 4: DISTANCE ARRAY
             * ========================================================
             *
             * distance[v] represents:
             *
             *     shortest number of edges from startVertex to v.
             *
             *
             * Example:
             *
             *     start = 0
             *
             *     0 -- 1 -- 2
             *
             * distances:
             *
             *     distance[0] = 0
             *     distance[1] = 1
             *     distance[2] = 2
             *
             *
             * -1 means:
             *
             *     "This vertex has not been visited yet."
             */
            int[] distance =
                    new int[numberOfVertices];

            Arrays.fill(distance, -1);


            /*
             * ========================================================
             * STEP 5: PARENT ARRAY
             * ========================================================
             *
             * parent[v] tells us:
             *
             *     Which vertex did we use to reach v during BFS?
             *
             *
             * Example:
             *
             *     0 -> 1 -> 2
             *
             * then:
             *
             *     parent[1] = 0
             *     parent[2] = 1
             *
             *
             * We need this to distinguish:
             *
             *     normal BFS tree edge
             *
             * from:
             *
             *     an edge that creates a cycle.
             */
            int[] parent =
                    new int[numberOfVertices];

            Arrays.fill(parent, -1);


            /*
             * ========================================================
             * STEP 6: CREATE BFS QUEUE
             * ========================================================
             */
            Queue<Integer> queue =
                    new ArrayDeque<>();


            /*
             * ========================================================
             * STEP 7: INITIALIZE BFS
             * ========================================================
             *
             * We start at startVertex.
             *
             * Distance from start to itself is 0.
             */
            queue.offer(startVertex);
            distance[startVertex] = 0;


            /*
             * ========================================================
             * STEP 8: BFS
             * ========================================================
             */
            while (!queue.isEmpty()) {

                /*
                 * Remove the next vertex from the BFS queue.
                 */
                int currentVertex = queue.poll();


                /*
                 * Examine every neighbor of currentVertex.
                 */
                for (int neighborVertex :
                        adjacencyList.get(currentVertex)) {


                    /*
                     * =================================================
                     * CASE 1: NEIGHBOR HAS NOT BEEN VISITED
                     * =================================================
                     *
                     * This is normal BFS exploration.
                     */
                    if (distance[neighborVertex] == -1) {

                        /*
                         * The neighbor is one edge farther from
                         * the start.
                         */
                        distance[neighborVertex] =
                                distance[currentVertex] + 1;


                        /*
                         * Record how we reached this vertex.
                         */
                        parent[neighborVertex] =
                                currentVertex;


                        /*
                         * Add it to the BFS queue.
                         */
                        queue.offer(neighborVertex);
                    }


                    /*
                     * =================================================
                     * CASE 2: NEIGHBOR IS ALREADY VISITED
                     * =================================================
                     *
                     * This edge may indicate a cycle.
                     *
                     * But we must first make sure it is NOT simply
                     * the edge we used to reach currentVertex.
                     */
                    else if (parent[currentVertex] != neighborVertex) {


                        /*
                         * =================================================
                         * CYCLE FOUND
                         * =================================================
                         *
                         * We have an edge:
                         *
                         *     currentVertex -- neighborVertex
                         *
                         * Both vertices are already reachable from
                         * startVertex.
                         *
                         * The cycle length is:
                         *
                         *     distance[currentVertex]
                         *     +
                         *     distance[neighborVertex]
                         *     +
                         *     1
                         *
                         * The +1 represents the current edge:
                         *
                         *     currentVertex -- neighborVertex
                         */
                        int cycleLength =
                                distance[currentVertex]
                                + distance[neighborVertex]
                                + 1;


                        /*
                         * Keep the shortest cycle found so far.
                         */
                        shortestCycleLength =
                                Math.min(
                                        shortestCycleLength,
                                        cycleLength
                                );
                    }
                }
            }
        }


        /*
         * ============================================================
         * STEP 9: RETURN RESULT
         * ============================================================
         *
         * If shortestCycleLength was never changed, no cycle exists.
         *
         * Otherwise return the shortest cycle length.
         */
        return shortestCycleLength == Integer.MAX_VALUE
                ? -1
                : shortestCycleLength;
    }
}
