/**
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Can n or m be 1?"
 *   Absolutely, a tree can consist of a single node (with an empty edges array). 
 *   Our algorithm must handle 0-length diameters and not throw IndexOutOfBoundsExceptions.
 * - "Are the edges weighted?"
 *   No. If they were, the mathematical trick for finding tree centers (radius = ceil(diameter/2)) 
 *   would fail, forcing us to use a dynamic programming approach to find the exact eccentricities.
 * - "Do we need to return the actual nodes we connect, or just the resulting diameter?"
 *   Just the resulting diameter. This saves us from having to reconstruct the exact path 
 *   or explicitly locate the center nodes.
 * - "Are we guaranteed that the input arrays form valid, fully connected trees?"
 *   Yes, constraints state they are valid trees, saving us from checking for cycles or disconnected components.
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We are joining two separate trees with one edge. The diameter of the new combined tree 
 * will be the maximum of three possible values:
 *   1. The diameter of the first tree (D1).
 *   2. The diameter of the second tree (D2).
 *   3. The longest path that crosses the new bridge edge.
 * To minimize the overall diameter, we must minimize that crossing path.
 * 
 * --- APPROACH 1: Brute Force (Try all pairs) ---
 * 1. What I'd naturally try: Loop through every node `u` in Tree 1 and every node `v` in Tree 2. 
 *    Connect them, and run a full search to find the new diameter. Keep track of the minimum.
 * 2. Why it works: It exhaustively tests every possible physical connection.
 * 3. Why it's too slow: For N and M up to 100,000, checking N*M pairs is 10^10 combinations. 
 *    Inside that, checking the diameter takes O(N+M) time. 
 * 4. Time Complexity: O(N * M * (N + M)) — Guaranteed Time Limit Exceeded.
 * 5. Space Complexity: O(N + M) — For the combined tree representation.
 * 
 * --- APPROACH 2: Find all eccentricities (Tree DP) ---
 * 1. What I'd try next: The longest path crossing a bridge (u, v) is:
 *    (Max distance from u to any node in T1) + 1 + (Max distance from v to any node in T2).
 *    This "max distance from a node" is called its *eccentricity*. If we run a Tree DP to find 
 *    the eccentricity of every single node in both trees, we just take the minimum eccentricity 
 *    from T1 and the minimum eccentricity from T2, add 1, and we have our minimized crossing path!
 * 2. Why it works: It breaks the problem into independent searches on T1 and T2.
 * 3. The bottleneck: Calculating the eccentricity of all nodes using DP is O(N+M), but the code 
 *    is highly complex, involving 'longest' and 'second longest' path tracking per node. 
 * 4. Time Complexity: O(N + M) — Fast enough!
 * 5. Space Complexity: O(N + M) — For DP arrays and adjacency lists.
 * 
 * [The Core Observation for the Ultimate Optimal]
 * Do we actually need to calculate the eccentricity of *every* node? 
 * In an unweighted tree, the node with the minimum eccentricity is the *center* of the tree. 
 * The minimum eccentricity (the radius) has a strict mathematical relationship with the diameter: 
 * Radius = ceil(Diameter / 2). 
 * We don't need DP to find all eccentricities. We ONLY need to find the diameters of the two trees!
 * 
 * --- APPROACH 3: 2-BFS Diameter and Math (The Optimal Way) ---
 * 1. How it works: 
 *    - To find the diameter of a tree, pick any node and run BFS to find the farthest node, A.
 *    - Run BFS from A to find the farthest node, B. The distance from A to B is the diameter.
 *    - Do this for T1 to get D1, and for T2 to get D2.
 *    - The radius (minimized eccentricity) is R1 = ceil(D1 / 2) and R2 = ceil(D2 / 2).
 *    - The final answer is `max(D1, D2, R1 + R2 + 1)`.
 * 2. Time Complexity: O(N + M) — We run BFS exactly twice per tree. Each BFS visits nodes and 
 *    edges once. No complex DP state tracking is required.
 * 3. Space Complexity: O(N + M) — For the adjacency list and the BFS queue. This is optimal.
 * 
 * [Which one I'd write in an interview]
 * Approach 3 (2-BFS + Math). It avoids the massive code overhead of Tree DP (which is prone to 
 * edge-case bugs under pressure) and demonstrates a deep understanding of graph theory properties 
 * (specifically, that Radius = ceil(Diameter / 2) on unweighted trees).
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Single Node Trees (n = 1, m = 1): `edges1` and `edges2` are empty. 
 *   Our BFS starts at node 0 and returns distance 0. D1 = 0, D2 = 0.
 *   Result becomes `max(0, 0, ceil(0/2) + ceil(0/2) + 1) = 1`. Correct!
 * - Deep line graph vs Single node: One tree is a straight line of 100,000 nodes, the other is 1 node. 
 *   The math seamlessly handles it: joining the 1 node to the center of the massive line doesn't 
 *   increase the overall diameter.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * You don't need to know *which* nodes to connect. You just need to know that connecting 
 * the centers of the two trees provides the mathematically shortest possible bridge path. 
 * Integer division `(d + 1) / 2` perfectly simulates `ceil(d / 2)`.
 * 
 * [Examples & Diagram]
 * Tree 1 (Diameter 4):
 * A - B - C - D - E
 * Center is C. Radius = 2.
 * 
 * Tree 2 (Diameter 5):
 * U - V - W - X - Y - Z
 * Centers are W or X. Radius = 3.
 * 
 * If we connect C and W:
 * Longest path in T1 is 4. Longest in T2 is 5.
 * Longest crossing path: A to Z -> Dist(A->C) + 1 (Bridge) + Dist(W->Z)
 *                         -> 2 + 1 + 3 = 6.
 * Final Diameter = max(4, 5, 6) = 6.
 * 
 * [Dry Run (Optimal)]
 * T1: D1 = 4. `r1 = (4 + 1) / 2 = 2`.
 * T2: D2 = 2. `r2 = (2 + 1) / 2 = 1`.
 * Cross Path = 2 + 1 + 1 = 4.
 * Return `max(4, 2, 4) = 4`.
 * 
 * [Pitfalls]
 * - Using DFS for the diameter search in Java. With N = 100,000, a skewed tree (linked list) 
 *   will cause a `StackOverflowError` in a recursive DFS. Always use Iterative BFS.
 * - Missing the outer `Math.max`. The new crossing path isn't ALWAYS the new diameter! 
 *   If you connect a tiny tree to the center of a massive tree, the massive tree's original 
 *   diameter remains the overall longest path.
 * 
 * [Pattern Recognition]
 * When you see: "Minimize the longest path" + "Connecting trees".
 * Think: Tree Centers. The center minimizes the distance to all other nodes.
 * When you see: "Diameter of an unweighted tree".
 * Think: 2-BFS method.
 * 
 * [Interview Script]
 * "Instead of exhaustively testing connections or writing a complex Tree DP to find eccentricities, 
 * we can rely on graph theory. The best nodes to connect are the centers of both trees. The 
 * maximum distance from the center to any node is the tree's radius. Since the radius is just 
 * ceil(diameter / 2), we only need to find the diameters of the two trees. I'll use the classic 
 * 2-BFS technique to find the diameters iteratively to avoid StackOverflows on deep trees, calculate 
 * their radii, and return the maximum of the individual diameters or the new cross-bridge path. 
 * This reduces the problem to strict O(N + M) time and space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the edges had weights (e.g., distances between cities)?
 * A: Our `(d+1)/2` trick relies on uniform step costs. For weighted trees, the center doesn't 
 *    cleanly split the diameter in half mathematically. We would need to either track the exact 
 *    path during our 2-BFS and walk halfway back along the weighted edges to find the center, 
 *    or use the Tree DP approach to find true eccentricities.
 * 
 * Q: What if we were given K trees to connect instead of just 2?
 * A: We would find the radii of all K trees. We would then take the tree with the absolute 
 *    largest radius, and use it as the "hub". We'd connect the centers of all other K-1 trees 
 *    directly to the center of this hub tree.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MinimumTreeDiameter {

    /**
     * Finds the minimum possible diameter of a tree formed by connecting two trees.
     * 
     * @param edges1 Edges of the first tree
     * @param edges2 Edges of the second tree
     * @return Minimum possible diameter
     */
    public static int minimumDiameterAfterMerge(int[][] edges1, int[][] edges2) {
        // Find the diameter of both trees. 
        // We pass the number of nodes which is (edges.length + 1) for a valid tree.
        int d1 = getDiameter(edges1.length + 1, edges1);
        int d2 = getDiameter(edges2.length + 1, edges2);

        // Calculate the radii. 
        // In Java, integer division automatically floors. 
        // Adding 1 before dividing achieves the equivalent of Math.ceil(d / 2.0).
        int r1 = (d1 + 1) / 2;
        int r2 = (d2 + 1) / 2;

        // The diameter is the maximum of:
        // 1. Existing diameter of Tree 1
        // 2. Existing diameter of Tree 2
        // 3. The new path crossing the bridge: Radius1 + Bridge(1) + Radius2
        return Math.max(Math.max(d1, d2), r1 + r2 + 1);
    }

    /**
     * Helper to find the diameter of an unweighted tree using the 2-BFS method.
     */
    private static int getDiameter(int n, int[][] edges) {
        // Edge case: single node tree has 0 edges and diameter 0
        if (n == 1) {
            return 0;
        }

        // 1. Build Adjacency List. 
        // Using an array of Lists is faster and more memory efficient than a Map.
        List<Integer>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }
        for (int[] edge : edges) {
            adj[edge[0]].add(edge[1]);
            adj[edge[1]].add(edge[0]);
        }

        // 2. First BFS: Start from an arbitrary node (node 0) 
        // to find the farthest node in the tree.
        int[] farthestResult = bfs(0, n, adj);
        int farthestNode = farthestResult[0];

        // 3. Second BFS: Start from the farthest node found.
        // The maximum distance found now is the true diameter of the tree.
        int[] diameterResult = bfs(farthestNode, n, adj);
        
        // Return the max distance
        return diameterResult[1];
    }

    /**
     * Standard Iterative BFS to prevent StackOverflow on massive skewed trees.
     * 
     * @return int[] where index 0 is the farthest node, and index 1 is its distance.
     */
    private static int[] bfs(int startNode, int n, List<Integer>[] adj) {
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        
        // Concrete state: Initially starting node at distance 0
        queue.offer(startNode);
        visited[startNode] = true;
        
        int lastNode = startNode;
        int distance = -1; // Starts at -1 so the first layer (startNode) bumps it to 0
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            distance++;
            
            // Process layer by layer to keep track of distance accurately
            for (int i = 0; i < levelSize; i++) {
                int current = queue.poll();
                lastNode = current; // Continually update to track the very last node visited
                
                for (int neighbor : adj[current]) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        return new int[]{lastNode, distance};
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test 1: Standard trees (as per diagram logic)
        // T1: 0-1-2-3 (Line of 4 nodes, D1=3, R1=2)
        int[][] edges1 = {{0,1}, {1,2}, {2,3}};
        // T2: 0-1-2 (Line of 3 nodes, D2=2, R2=1)
        int[][] edges2 = {{0,1}, {1,2}};
        // Crossing: 2 + 1 + 1 = 4. Max(3, 2, 4) = 4.
        System.out.println("Test 1 (Standard): " + minimumDiameterAfterMerge(edges1, edges2));
        // Expected: 4

        // Test 2: Single node tree vs Huge tree
        // T1: 0 (No edges, D1=0, R1=0)
        int[][] edges3 = {};
        // T2: Star graph, node 0 is center, 1-4 are leaves. (D2=2, R2=1)
        int[][] edges4 = {{0,1}, {0,2}, {0,3}, {0,4}};
        // Crossing: 0 + 1 + 1 = 2. Max(0, 2, 2) = 2.
        System.out.println("Test 2 (Single vs Star): " + minimumDiameterAfterMerge(edges3, edges4));
        // Expected: 2 (Joining the single node to the star's center keeps diameter at 2)

        // Test 3: Two single node trees
        System.out.println("Test 3 (Both Single): " + minimumDiameterAfterMerge(new int[][]{}, new int[][]{}));
        // Expected: 1 (Connecting 0 to 0 forms a line of 2 nodes, D=1)
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: The 2-BFS technique to find the diameter of an unweighted tree.
 * - Key observation: Connecting the centers of two trees mathematically guarantees 
 *   the shortest crossing path. A tree's radius is exactly `ceil(Diameter / 2)`.
 * - Most common trap: Writing a recursive DFS to find the diameter instead of BFS. 
 *   In Java, a recursive call stack over 10,000 deep throws a StackOverflowError, 
 *   which will crash on a linked-list-shaped tree within the 100,000 node constraint.
 * - Mental trigger: "Longest path" + "Unweighted tree" = 2-BFS Diameter trick.
 */


import java.util.*;

/**
 * ============================================================
 *  Problem: Minimum Diameter After Merging Two Trees
 * ============================================================
 *
 *  You are given 2 trees:
 *      Tree1 (n nodes)
 *      Tree2 (m nodes)
 *
 *  You can connect ANY node from Tree1 to ANY node from Tree2 using 1 edge.
 *
 *  Goal:
 *      Return the MINIMUM possible diameter after merging.
 *
 * ============================================================
 * 🧠 CORE INTUITION (VERY IMPORTANT)
 * ============================================================
 *
 * After merging, the longest path (diameter) can be:
 *
 *  1. Completely inside Tree1 → d1
 *  2. Completely inside Tree2 → d2
 *  3. A path that goes from Tree1 → Tree2 (via new edge)
 *
 *  So:
 *      finalDiameter = max(d1, d2, crossTreePath)
 *
 * ------------------------------------------------------------
 * Now the key question:
 *      How to MINIMIZE crossTreePath ?
 *
 * ------------------------------------------------------------
 * If we connect random nodes → diameter can be huge ❌
 * If we connect endpoints → worst possible ❌
 *
 * ✅ BEST strategy:
 *      Connect "CENTERS" of both trees
 *
 * Why?
 *      Center = node which minimizes max distance to all nodes
 *
 * ------------------------------------------------------------
 * Define:
 *      radius = max distance from center to any node
 *
 * Then:
 *      crossTreePath = radius1 + 1 + radius2
 *
 * ------------------------------------------------------------
 * FINAL FORMULA:
 *
 *      answer = max(
 *          d1,
 *          d2,
 *          r1 + r2 + 1
 *      )
 *
 * ============================================================
 * 🧠 HOW TO FIND DIAMETER?
 * ============================================================
 *
 * Use BFS twice (classic trick):
 *
 *  1. Start BFS from any node → find farthest node A
 *  2. Start BFS from A → find farthest node B
 *  3. Distance A → B = diameter
 *
 * Why this works?
 *      In a tree, one endpoint of diameter is always
 *      farthest from any arbitrary node.
 *
 * ============================================================
 * ⏱️ Complexity
 * ============================================================
 *
 * Time  : O(N + M)
 * Space : O(N + M)
 *
 * ============================================================
 */
public class MinDiameterAfterMerge {

    /**
     * Main function
     */
    public int minimumDiameterAfterMerge(int[][] edges1, int[][] edges2) {

        // Step 1: Compute diameter of both trees
        int d1 = getDiameter(edges1);
        int d2 = getDiameter(edges2);

        // Step 2: Convert diameter → radius
        // radius = ceil(diameter / 2)
        int r1 = (d1 + 1) / 2;
        int r2 = (d2 + 1) / 2;

        /**
         * Step 3: Final answer
         *
         * max of:
         * 1. diameter inside tree1
         * 2. diameter inside tree2
         * 3. cross-tree path (best possible when connecting centers)
         */
        return Math.max(
                Math.max(d1, d2),
                r1 + r2 + 1
        );
    }

    /**
     * ============================================================
     * Computes diameter of a tree using 2 BFS
     * ============================================================
     */
    private int getDiameter(int[][] edges) {

        // Number of nodes in tree = edges + 1
        int n = edges.length + 1;

        // Build adjacency list
        List<List<Integer>> graph = buildGraph(n, edges);

        /**
         * Step 1:
         * BFS from any node (0) to find farthest node A
         */
        int farthestFromStart = bfs(0, graph).node;

        /**
         * Step 2:
         * BFS from A → gives actual diameter
         */
        BFSResult result = bfs(farthestFromStart, graph);

        return result.distance; // diameter
    }

    /**
     * ============================================================
     * BFS Helper
     * ============================================================
     *
     * Returns:
     *  - farthest node
     *  - distance to that node
     *
     * Think:
     * BFS explores level by level
     * Last level reached = farthest distance
     */
    private BFSResult bfs(int start, List<List<Integer>> graph) {

        Queue<Integer> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[graph.size()];

        queue.offer(start);
        visited[start] = true;

        int farthestNode = start;
        int distance = 0; // number of edges

        /**
         * Level order BFS
         */
        while (!queue.isEmpty()) {

            int size = queue.size();

            // Process one level
            for (int i = 0; i < size; i++) {
                int node = queue.poll();

                // Last processed node at this level becomes farthest
                farthestNode = node;

                for (int neighbor : graph.get(node)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        queue.offer(neighbor);
                    }
                }
            }

            /**
             * If more nodes exist → we are moving to next level
             * So increase distance
             */
            if (!queue.isEmpty()) {
                distance++;
            }
        }

        return new BFSResult(farthestNode, distance);
    }

    /**
     * ============================================================
     * Build adjacency list
     * ============================================================
     */
    private List<List<Integer>> buildGraph(int n, int[][] edges) {

        List<List<Integer>> graph = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] e : edges) {
            int u = e[0];
            int v = e[1];

            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        return graph;
    }

    /**
     * Small helper record (Java 16+ feature)
     */
    private record BFSResult(int node, int distance) {}
}


import java.util.*;

/**
 * ============================================================
 *  MINIMUM DIAMETER AFTER MERGING TWO TREES
 * ============================================================
 *
 * Problem:
 *
 * We have two trees:
 *
 *     Tree 1
 *     Tree 2
 *
 * We can choose ANY node from Tree 1 and ANY node from Tree 2
 * and connect them with ONE new edge.
 *
 * We need to return the SMALLEST possible diameter of the
 * resulting tree.
 *
 *
 * ============================================================
 * 1. FIRST UNDERSTAND WHAT HAPPENS WHEN WE CONNECT THE TREES
 * ============================================================
 *
 * Suppose:
 *
 *          Tree 1                    Tree 2
 *
 *             A                         X
 *            / \                       / \
 *           B   C                     Y   Z
 *
 *
 * We choose:
 *
 *             C -------- Y
 *                  new edge
 *
 *
 * Now the result is one larger tree.
 *
 *
 * The new diameter can come from THREE possibilities:
 *
 *
 *     Case 1:
 *     Entirely inside Tree 1
 *
 *     Case 2:
 *     Entirely inside Tree 2
 *
 *     Case 3:
 *     Starts in Tree 1 and ends in Tree 2
 *
 *
 * Therefore:
 *
 *     finalDiameter =
 *         max(
 *             diameter(Tree1),
 *             diameter(Tree2),
 *             longestCrossTreePath
 *         )
 *
 *
 * ============================================================
 * 2. WHAT IS THE LONGEST CROSS-TREE PATH?
 * ============================================================
 *
 * Suppose we connect:
 *
 *             A -------- B
 *                 new edge
 *
 * where:
 *
 *     A belongs to Tree 1
 *     B belongs to Tree 2
 *
 *
 * Consider a path:
 *
 *     some node in Tree 1
 *             |
 *             v
 *             A
 *             |
 *        new edge
 *             |
 *             B
 *             |
 *             v
 *     some node in Tree 2
 *
 *
 * The longest such path is:
 *
 *     farthestDistance(Tree1, A)
 *   + 1
 *   + farthestDistance(Tree2, B)
 *
 *
 * So:
 *
 *     crossTreeDiameter =
 *         eccentricity(A)
 *       + 1
 *       + eccentricity(B)
 *
 *
 * where eccentricity(node) means:
 *
 *     maximum distance from this node to any other node.
 *
 *
 * ============================================================
 * 3. WHICH NODES SHOULD WE CONNECT?
 * ============================================================
 *
 * We want to minimize:
 *
 *     eccentricity(A) + 1 + eccentricity(B)
 *
 * Therefore we want the node with the smallest eccentricity
 * in each tree.
 *
 *
 * Which nodes have the smallest eccentricity?
 *
 *                         THE CENTER(S)
 *
 * of the tree.
 *
 *
 * A tree can have:
 *
 *     - one center
 *     - two centers
 *
 *
 * Example with odd diameter:
 *
 *             A
 *             |
 *             B
 *             |
 *             C
 *             |
 *             D
 *             |
 *             E
 *
 * Diameter = 4
 *
 * Center:
 *
 *             C
 *
 * Its maximum distance to any node = 2.
 *
 *
 * Example with even number of edges in the middle:
 *
 *             A
 *             |
 *             B
 *             |
 *             C
 *             |
 *             D
 *
 * Diameter = 3
 *
 * Centers:
 *
 *             B -- C
 *
 * Both B and C have eccentricity 2.
 *
 *
 * ============================================================
 * 4. CONNECTION TO TREE DIAMETER
 * ============================================================
 *
 * Let:
 *
 *     d1 = diameter of Tree 1
 *     d2 = diameter of Tree 2
 *
 *
 * The minimum eccentricity of a tree is:
 *
 *     ceil(diameter / 2)
 *
 * This is called the tree's RADIUS.
 *
 * Therefore:
 *
 *     radius1 = ceil(d1 / 2)
 *     radius2 = ceil(d2 / 2)
 *
 *
 * The best possible cross-tree path is:
 *
 *     radius1 + 1 + radius2
 *
 *
 * Therefore the final answer is:
 *
 *     max(
 *         d1,
 *         d2,
 *         radius1 + 1 + radius2
 *     )
 *
 *
 * ============================================================
 * 5. WHY DO WE USE ceil(d / 2)?
 * ============================================================
 *
 * Java integer division gives:
 *
 *     5 / 2 = 2
 *
 * But we need:
 *
 *     ceil(5 / 2) = 3
 *
 * A convenient integer formula is:
 *
 *     (d + 1) / 2
 *
 *
 * Examples:
 *
 *     d = 4
 *
 *     (4 + 1) / 2
 *     = 5 / 2
 *     = 2
 *
 *
 *     d = 5
 *
 *     (5 + 1) / 2
 *     = 6 / 2
 *     = 3
 *
 *
 * Therefore:
 *
 *     radius = (diameter + 1) / 2
 *
 *
 * ============================================================
 * 6. WHY IS THE CENTER THE BEST CONNECTION POINT?
 * ============================================================
 *
 * Consider a path:
 *
 *                 A
 *                 |
 *                 B
 *                 |
 *                 C
 *                 |
 *                 D
 *                 |
 *                 E
 *
 * Diameter = 4
 *
 * If we connect Tree 2 at A:
 *
 *     farthest distance from A = 4
 *
 * If we connect Tree 2 at C:
 *
 *     farthest distance from C = 2
 *
 * Clearly C is better.
 *
 *
 * The center minimizes the maximum distance to all nodes.
 *
 * That maximum distance is the radius.
 *
 *
 * ============================================================
 * 7. HOW DO WE FIND THE DIAMETER?
 * ============================================================
 *
 * There is a very useful tree property:
 *
 *
 *     BFS/DFS from ANY node
 *         |
 *         v
 *     find a farthest node A
 *         |
 *         v
 *     BFS/DFS from A
 *         |
 *         v
 *     find farthest node B
 *         |
 *         v
 *     distance(A, B) = DIAMETER
 *
 *
 * Why?
 *
 * In a tree, if we start from any node and go to the farthest
 * node, that farthest node is an endpoint of a diameter.
 *
 * Then starting from that endpoint and finding the farthest node
 * gives the other endpoint of the diameter.
 *
 *
 * ============================================================
 * 8. EXAMPLE OF DIAMETER FINDING
 * ============================================================
 *
 * Tree:
 *
 *         0
 *         |
 *         1
 *         |
 *         2
 *        / \
 *       3   4
 *           |
 *           5
 *
 *
 * Start BFS from 0.
 *
 * Distances:
 *
 *     0 -> 0
 *     1 -> 1
 *     2 -> 2
 *     3 -> 3
 *     4 -> 3
 *     5 -> 4
 *
 * Farthest = 5.
 *
 *
 * Now BFS from 5:
 *
 *     5 -> 0
 *     4 -> 1
 *     2 -> 2
 *     1 -> 3
 *     0 -> 4
 *     3 -> 5
 *
 * Farthest = 3.
 *
 * Therefore:
 *
 *     diameter = distance(5, 3) = 5
 *
 *
 * ============================================================
 * 9. IMPORTANT: n AND m CAN BE 100,000
 * ============================================================
 *
 * We should NOT use an O(n^2) approach.
 *
 * We also don't need to calculate the eccentricity of every
 * node individually.
 *
 * We only need:
 *
 *     diameter(Tree1)
 *     diameter(Tree2)
 *
 * Each diameter can be found using two BFS traversals.
 *
 * Therefore:
 *
 *     Tree 1 -> 2 BFS
 *     Tree 2 -> 2 BFS
 *
 * Total:
 *
 *     O(n + m)
 *
 *
 * ============================================================
 * 10. COMPLETE FORMULA
 * ============================================================
 *
 * Let:
 *
 *     d1 = diameter of Tree 1
 *     d2 = diameter of Tree 2
 *
 * Then:
 *
 *     r1 = ceil(d1 / 2)
 *        = (d1 + 1) / 2
 *
 *     r2 = ceil(d2 / 2)
 *        = (d2 + 1) / 2
 *
 *
 * The best cross-tree path:
 *
 *     r1 + 1 + r2
 *
 *
 * Final answer:
 *
 *     max(
 *         d1,
 *         d2,
 *         r1 + 1 + r2
 *     )
 *
 *
 * ============================================================
 * 11. WALKTHROUGH
 * ============================================================
 *
 * Suppose:
 *
 *     Tree 1 diameter = 4
 *     Tree 2 diameter = 5
 *
 *
 * Tree 1:
 *
 *     radius1 = ceil(4 / 2)
 *             = 2
 *
 * Tree 2:
 *
 *     radius2 = ceil(5 / 2)
 *             = 3
 *
 *
 * Connect their centers:
 *
 *     longest path from Tree1 center
 *              = 2
 *
 *     new edge
 *              = 1
 *
 *     longest path from Tree2 center
 *              = 3
 *
 *
 * Therefore cross-tree path:
 *
 *     2 + 1 + 3
 *     = 6
 *
 *
 * Existing diameters:
 *
 *     Tree1 = 4
 *     Tree2 = 5
 *
 *
 * Final:
 *
 *     max(4, 5, 6)
 *     = 6
 *
 *
 * ============================================================
 * 12. WHY NOT CONNECT DIAMETER ENDPOINTS?
 * ============================================================
 *
 * Suppose:
 *
 *     Tree 1:
 *
 *     A -------- ... -------- C
 *
 *     A and C are diameter endpoints.
 *
 * Their eccentricity is large.
 *
 * If we connect Tree 2 to A:
 *
 *     longest cross path
 *         becomes
 *             radius? NO
 *
 *             eccentricity(A)
 *           + 1
 *           + eccentricity(B)
 *
 * Since eccentricity(A) is the diameter itself for a diameter
 * endpoint, this can create a much larger diameter.
 *
 * We want to minimize the maximum distance from the connection
 * node to the rest of the tree.
 *
 * Therefore we connect the CENTERS.
 *
 *
 * ============================================================
 * 13. SPECIAL CASE: n = 1
 * ============================================================
 *
 * Tree:
 *
 *     0
 *
 * Diameter:
 *
 *     0
 *
 * Radius:
 *
 *     (0 + 1) / 2
 *     = 0
 *
 * If both trees contain one node:
 *
 *     0 ---- 0
 *
 * The resulting diameter is:
 *
 *     1
 *
 *
 * Formula:
 *
 *     max(
 *         0,
 *         0,
 *         0 + 1 + 0
 *     )
 *
 *     = 1
 *
 *
 * ============================================================
 * 14. SPECIAL CASE: BOTH TREES ARE LONG PATHS
 * ============================================================
 *
 * Tree 1:
 *
 *     diameter = 6
 *
 * Tree 2:
 *
 *     diameter = 6
 *
 *
 * radius:
 *
 *     3 and 3
 *
 * Connect centers:
 *
 *     3 + 1 + 3 = 7
 *
 * Existing diameter = 6.
 *
 * Final answer:
 *
 *     7
 *
 *
 * ============================================================
 * 15. WHY THE FINAL max() IS NECESSARY
 * ============================================================
 *
 * Imagine:
 *
 *     Tree 1 diameter = 100
 *     Tree 2 diameter = 1
 *
 * Their radii:
 *
 *     Tree 1 = 50
 *     Tree 2 = 1
 *
 * Cross path:
 *
 *     50 + 1 + 1
 *     = 52
 *
 * But Tree 1 already contains a path of length 100.
 *
 * Connecting the trees cannot destroy that path.
 *
 * Therefore the resulting tree's diameter is at least 100.
 *
 * Final:
 *
 *     max(100, 1, 52)
 *     = 100
 *
 *
 * ============================================================
 * 16. IMPLEMENTATION DETAILS
 * ============================================================
 *
 * We convert the edge arrays into adjacency lists.
 *
 * Example:
 *
 *     edges = {
 *         {0, 1},
 *         {1, 2},
 *         {1, 3}
 *     }
 *
 * becomes:
 *
 *     0 -> [1]
 *     1 -> [0, 2, 3]
 *     2 -> [1]
 *     3 -> [1]
 *
 *
 * Since the input is guaranteed to be a tree:
 *
 *     number of edges = nodes - 1
 *
 * and there are no cycles.
 *
 *
 * ============================================================
 * 17. BFS IMPLEMENTATION
 * ============================================================
 *
 * The helper:
 *
 *     bfsFarthest(graph, start)
 *
 * returns:
 *
 *     [farthestNode, distance]
 *
 * First call:
 *
 *     bfsFarthest(graph, 0)
 *
 * gives us one diameter endpoint.
 *
 * Second call:
 *
 *     bfsFarthest(graph, endpoint)
 *
 * gives us the diameter.
 *
 *
 * ============================================================
 * 18. COMPLEXITY
 * ============================================================
 *
 * Tree 1:
 *
 *     Building adjacency list = O(n)
 *     Two BFS traversals       = O(n)
 *
 * Tree 2:
 *
 *     Building adjacency list = O(m)
 *     Two BFS traversals       = O(m)
 *
 *
 * Total:
 *
 *     O(n + m)
 *
 *
 * Space:
 *
 *     O(n + m)
 *
 * for adjacency lists and BFS queues/visited arrays.
 *
 *
 * ============================================================
 * 19. CORE PATTERN TO REMEMBER
 * ============================================================
 *
 * The entire problem reduces to:
 *
 *
 *     1. Find diameter of Tree 1
 *
 *     2. Find diameter of Tree 2
 *
 *     3. Convert diameter to radius:
 *
 *            radius = ceil(diameter / 2)
 *
 *     4. Connect the centers:
 *
 *            radius1 + 1 + radius2
 *
 *     5. The resulting diameter is:
 *
 *            max(
 *                diameter1,
 *                diameter2,
 *                radius1 + 1 + radius2
 *            )
 *
 *
 * This is a very common tree-diameter pattern.
 */
public class MinimumDiameterAfterMergingTrees {

    /**
     * Returns the minimum possible diameter after connecting
     * any node of Tree 1 with any node of Tree 2.
     */
    public int minimumDiameterAfterMerge(
        int[][] edges1,
        int[][] edges2
    ) {

        /*
         * Number of nodes:
         *
         *     edges.length = nodes - 1
         *
         * Therefore:
         *
         *     nodes = edges.length + 1
         */
        int n = edges1.length + 1;
        int m = edges2.length + 1;

        /*
         * Build adjacency lists for both trees.
         */
        List<Integer>[] tree1 = buildGraph(n, edges1);
        List<Integer>[] tree2 = buildGraph(m, edges2);

        /*
         * Find the diameter of each tree.
         */
        int diameter1 = findDiameter(tree1);
        int diameter2 = findDiameter(tree2);

        /*
         * The center minimizes the maximum distance to all nodes.
         *
         * For a tree with diameter d:
         *
         *     radius = ceil(d / 2)
         *
         * Using integer arithmetic:
         *
         *     ceil(d / 2) = (d + 1) / 2
         */
        int radius1 = (diameter1 + 1) / 2;
        int radius2 = (diameter2 + 1) / 2;

        /*
         * If we connect the centers:
         *
         *     farthest node in Tree 1
         *                |
         *             radius1
         *                |
         *             center1
         *                |
         *             new edge = 1
         *                |
         *             center2
         *                |
         *             radius2
         *                |
         *     farthest node in Tree 2
         *
         * Cross-tree longest path:
         *
         *     radius1 + 1 + radius2
         */
        int crossTreeDiameter =
            radius1 + 1 + radius2;

        /*
         * The final tree contains both original trees.
         *
         * Therefore their original diameters cannot disappear.
         *
         * So the final diameter must be at least:
         *
         *     diameter1
         *     diameter2
         *
         * The best possible cross-tree diameter is:
         *
         *     crossTreeDiameter
         *
         * Therefore take the maximum.
         */
        return Math.max(
            Math.max(diameter1, diameter2),
            crossTreeDiameter
        );
    }

    /**
     * Builds an adjacency list from the edge list.
     */
    private List<Integer>[] buildGraph(
        int nodeCount,
        int[][] edges
    ) {

        /*
         * Java does not allow:
         *
         *     new List<Integer>[nodeCount]
         *
         * directly because of generic-array restrictions.
         *
         * This cast is the standard approach.
         */
        @SuppressWarnings("unchecked")
        List<Integer>[] graph = new List[nodeCount];

        /*
         * Initialize every adjacency list.
         */
        for (int node = 0; node < nodeCount; node++) {
            graph[node] = new ArrayList<>();
        }

        /*
         * Because the tree is undirected:
         *
         *     a -- b
         *
         * means:
         *
         *     a -> b
         *     b -> a
         */
        for (int[] edge : edges) {

            int a = edge[0];
            int b = edge[1];

            graph[a].add(b);
            graph[b].add(a);
        }

        return graph;
    }

    /**
     * Finds the diameter of a tree using two BFS traversals.
     */
    private int findDiameter(List<Integer>[] graph) {

        /*
         * --------------------------------------------------------
         * BFS #1
         * --------------------------------------------------------
         *
         * Start from any node.
         *
         * We choose node 0.
         *
         * The farthest node found from 0 is one endpoint of
         * a diameter.
         */
        BFSResult first = bfsFarthest(graph, 0);

        /*
         * --------------------------------------------------------
         * BFS #2
         * --------------------------------------------------------
         *
         * Start from the endpoint found by BFS #1.
         *
         * The farthest distance from this endpoint is the
         * diameter of the tree.
         */
        BFSResult second =
            bfsFarthest(graph, first.node);

        return second.distance;
    }

    /**
     * BFS from a given node.
     *
     * Returns:
     *
     *     the farthest node
     *     and its distance from start
     */
    private BFSResult bfsFarthest(
        List<Integer>[] graph,
        int start
    ) {

        int nodeCount = graph.length;

        /*
         * Since this is a tree, there is exactly one simple path
         * between any two nodes.
         *
         * visited[] prevents us from going back and forth across
         * an edge.
         */
        boolean[] visited = new boolean[nodeCount];

        Queue<Integer> queue = new ArrayDeque<>();

        /*
         * Start BFS.
         */
        queue.offer(start);
        visited[start] = true;

        /*
         * Distance of each BFS level.
         */
        int distance = 0;

        /*
         * Track the farthest node encountered.
         */
        int farthestNode = start;

        while (!queue.isEmpty()) {

            /*
             * Process one complete BFS level.
             */
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {

                int current = queue.poll();

                /*
                 * Since this node belongs to the current level,
                 * it is currently the farthest distance we have
                 * reached.
                 */
                farthestNode = current;

                /*
                 * Visit all neighbors.
                 */
                for (int neighbor : graph[current]) {

                    if (visited[neighbor]) {
                        continue;
                    }

                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }

            /*
             * All newly added nodes are one edge farther away.
             */
            distance++;
        }

        /*
         * Because distance was incremented after processing the
         * final level, the actual distance of farthestNode is:
         *
         *     distance - 1
         *
         * Example:
         *
         *     Single node:
         *
         *     process node at distance 0
         *     distance becomes 1
         *
         * Actual answer = 0.
         */
        return new BFSResult(
            farthestNode,
            distance - 1
        );
    }

    /**
     * Small record used to return two values from BFS.
     */
    private record BFSResult(
        int node,
        int distance
    ) {
    }
}

