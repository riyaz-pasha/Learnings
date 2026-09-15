import java.util.*;

/*
 * ==========================================================================================
 *                                   TREE DIAMETER
 * ==========================================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ------------------------------------------------------------------------------------------
 * Restatement:
 * You are given an undirected tree (a connected graph with no cycles) represented by an array 
 * of edges. You need to find the "diameter" of this tree.
 * The diameter is the length of the longest path between ANY two nodes in the tree. 
 * The length is measured by the number of edges between them.
 * 
 * Idea, Intuition, and Key Observations:
 * - A tree has exactly N nodes and N-1 edges. There is exactly one unique path between 
 *   any two nodes.
 * - The naive way is to start from every single node, find the farthest node from it, and 
 *   keep track of the maximum distance. This takes O(N^2) time.
 * - Key Observation 1 (The 2-BFS/DFS Trick): If you start at ANY arbitrary node X and find 
 *   the farthest node from it, let's call it A. Node A is GUARANTEED to be one of the endpoints 
 *   of the tree's diameter. If you then find the farthest node from A, let's call it B. 
 *   The path from A to B is the diameter of the tree.
 * - Key Observation 2 (Tree DP / Post-order Traversal): Alternatively, we can root the tree 
 *   arbitrarily (e.g., at node 0). The longest path either passes through the root or is 
 *   entirely contained in one of its subtrees. For any given node, the longest path that 
 *   "peaks" at this node is the sum of the depths of its two deepest subtrees.
 *
 * How to identify the relevant algorithm/data structure:
 * - "Undirected tree", "longest path", "diameter" -> Standard graph traversal (BFS/DFS).
 * - 2-BFS or Tree DP are the textbook O(N) solutions for this specific problem.
 *
 *
 * 2. INTERVIEW CLARIFICATION
 * ------------------------------------------------------------------------------------------
 * Ask these questions before coding:
 * Q1: "What if there is only 1 node in the tree (N=1, edges=[])?"
 *     Why: Clarifies the base case. The diameter of a 1-node tree is 0 (no edges).
 * Q2: "Are the node values strictly 0 to N-1?"
 *     Why: Confirms we can use standard arrays/lists for our Adjacency List instead of HashMaps.
 * Q3: "Is the graph guaranteed to be fully connected and cycle-free?"
 *     Why: The problem says it's a "tree", which mathematically guarantees this. We don't 
 *          need to write cycle-detection code.
 * Q4: "Do we count nodes or edges for the diameter?"
 *     Why: The prompt specifies edges. (If it were nodes, the answer would be edges + 1).
 *
 *
 * 3. EXAMPLES & VISUALS
 * ------------------------------------------------------------------------------------------
 * Example Tree:
 * Edges: [[0,1], [1,2], [2,3], [1,4], [4,5]]
 * 
 * Visual:
 *        0
 *        |
 *        1
 *       / \
 *      2   4
 *     /     \
 *    3       5
 *
 * Longest path is between 3 and 5. Path: 3 -> 2 -> 1 -> 4 -> 5.
 * Number of edges = 4. Diameter = 4.
 *
 * Tracing the 2-BFS Trick:
 * 1. Start BFS from arbitrary node (let's pick 0).
 *    Farthest from 0 is either 3 or 5 (distance 3). Let's say BFS returns 3.
 * 2. Start BFS from 3.
 *    Farthest from 3 is 5.
 *    Distance from 3 to 5 is 4. Result = 4!
 * 
 * Tracing the Tree DP (DFS) Trick:
 * Root at 1:
 * - Subtree 0 has depth 1.
 * - Subtree 2 has depth 2 (2->3).
 * - Subtree 4 has depth 2 (4->5).
 * Max two depths for node 1 are 2 and 2. Sum = 4. Global Max = 4.
 *
 *
 * 4. SOLUTIONS
 * ------------------------------------------------------------------------------------------
 */

public class TreeDiameter {

    /*
     * APPROACH 1: THE TWO-BFS METHOD (Optimal & Highly Intuitive)
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * We use Breadth-First Search (BFS) twice. 
     * First BFS finds the farthest node from node 0. (This node is an endpoint of the diameter).
     * Second BFS finds the farthest node from that endpoint. The distance found is the diameter.
     * 
     * Why it works:
     * Proof by contradiction: If the farthest node from a random start wasn't an endpoint of 
     * the longest path, it would mean there is another node further away, which violates the 
     * definition of "farthest node" in a fully connected acyclic graph.
     * 
     * Time Complexity: O(N) 
     * - We build the adjacency list in O(N).
     * - We run BFS twice. Each BFS visits every node and edge exactly once. O(N).
     * 
     * Space Complexity: O(N)
     * - Adjacency list takes O(N) space (N nodes + 2N edges).
     * - Queue and visited array take O(N) space.
     * 
     * Trade-offs:
     * Two passes instead of one, but avoids deep recursion. Very safe against StackOverflow 
     * for deep/unbalanced trees.
     */

    public int treeDiameterTwoBFS(int n, int[][] edges) {
        if (n <= 1) return 0;

        // 1. Build Adjacency List
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] edge : edges) {
            adj.get(edge[0]).add(edge[1]);
            adj.get(edge[1]).add(edge[0]);
        }

        // 2. First BFS from node 0 to find one endpoint of the diameter
        int[] firstBfsResult = bfs(0, n, adj);
        int farthestNode = firstBfsResult[0];

        // 3. Second BFS from the found endpoint to find the actual diameter
        int[] secondBfsResult = bfs(farthestNode, n, adj);
        
        return secondBfsResult[1]; // Return the max distance
    }

    // Helper method for BFS. Returns an array: {farthest_node, max_distance}
    private int[] bfs(int startNode, int n, List<List<Integer>> adj) {
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[n];
        
        queue.offer(startNode);
        visited[startNode] = true;
        
        int distance = -1;
        int farthestNode = startNode;

        while (!queue.isEmpty()) {
            int size = queue.size();
            distance++; // Increment distance per level

            for (int i = 0; i < size; i++) {
                int curr = queue.poll();
                farthestNode = curr; // Track the last node processed
                
                for (int neighbor : adj.get(curr)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        return new int[]{farthestNode, distance};
    }

    /*
     * APPROACH 2: SINGLE DFS / TREE DP (Optimal & Most Elegant) [RECOMMENDED]
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * Treat node 0 as the root. For every node, we want to know the maximum depth of its 
     * branches. If a node has multiple branches, the longest path passing through this node 
     * is the sum of its TWO deepest branches.
     * We calculate this recursively bottom-up, keeping a global maximum.
     * 
     * Why it works:
     * Every path in a tree has a unique "highest" node (the node closest to the chosen root). 
     * By calculating the sum of the top 2 deepest subtrees for EVERY node, we are guaranteed 
     * to test the "highest" node of the true diameter.
     * 
     * Time Complexity: O(N)
     * - We visit every node exactly once during the DFS.
     * 
     * Space Complexity: O(N)
     * - Adjacency list takes O(N).
     * - The recursion call stack can go up to O(N) deep in a worst-case "line" graph.
     * 
     * When to use: 
     * Present this in the interview. It's only 1 pass, requires less code, and demonstrates 
     * a strong grasp of Tree Dynamic Programming / Post-order traversal.
     */
    
    // Global variable to keep track of the maximum diameter found
    private int maxDiameter = 0;

    public int treeDiameterDFS(int n, int[][] edges) {
        if (n <= 1) return 0;

        // 1. Build Adjacency List
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] edge : edges) {
            adj.get(edge[0]).add(edge[1]);
            adj.get(edge[1]).add(edge[0]);
        }

        // 2. Reset global variable (important for multiple test cases)
        maxDiameter = 0;
        
        // 3. Start DFS from node 0. We pass -1 as the parent since 0 has no parent.
        dfs(0, -1, adj);
        
        return maxDiameter;
    }

    /**
     * DFS returns the maximum depth (number of edges) from the current node down to a leaf.
     */
    private int dfs(int curr, int parent, List<List<Integer>> adj) {
        int deepestSubtree = 0;
        int secondDeepestSubtree = 0;

        // Traverse all neighbors
        for (int neighbor : adj.get(curr)) {
            // Because it's an undirected tree, we must ignore the edge leading back to the parent
            if (neighbor == parent) continue;

            // Get the depth of the branch going into this neighbor
            int branchDepth = dfs(neighbor, curr, adj);

            // Update the top 2 deepest subtrees
            if (branchDepth > deepestSubtree) {
                secondDeepestSubtree = deepestSubtree;
                deepestSubtree = branchDepth;
            } else if (branchDepth > secondDeepestSubtree) {
                secondDeepestSubtree = branchDepth;
            }
        }

        // The longest path peaking at this 'curr' node uses its two deepest subtrees
        maxDiameter = Math.max(maxDiameter, deepestSubtree + secondDeepestSubtree);

        // Tell the parent our maximum depth. (+1 for the edge connecting parent to curr)
        return deepestSubtree + 1;
    }


    /*
     * 5. EDGE CASES & MISTAKES
     * ------------------------------------------------------------------------------------------
     * - Edge Case 1: N = 1 (No edges). Both loops correctly handle it and return 0. 
     *   (If we don't handle `N<=1` early, creating the adj list might throw exceptions).
     * - Edge Case 2: N = 2 (1 edge). The longest path is exactly 1.
     * - Edge Case 3: A "Star" graph (1 center node, N-1 leaves). DFS handles this naturally; 
     *   top 2 branches will have depth 1, sum = 2.
     * 
     * Common Mistakes:
     * - Forgetting the `if (neighbor == parent) continue;` check in the DFS. In an undirected 
     *   graph without a `visited` array, this causes an immediate infinite loop (StackOverflow).
     * - Counting nodes instead of edges. If the diameter is 4 edges, it has 5 nodes. Make 
     *   sure you return the correct metric.
     * - Initializing `distance = 0` in BFS before the while loop, which results in counting 
     *   nodes. Initializing `distance = -1` perfectly counts edges.
     *
     *
     * 6. INTERVIEW STRATEGY
     * ------------------------------------------------------------------------------------------
     * 1. Understand -> Explain what a tree diameter is out loud.
     * 2. Clarify -> Ask about N=1 and if edges are undirected.
     * 3. Approach -> Start by suggesting the Two-BFS method: "A cool graph theory trick is 
     *    running BFS twice. Farthest from start is endpoint A. Farthest from A is endpoint B."
     * 4. Optimize -> "However, we can do this in a single pass using Post-Order DFS. For every 
     *    node, we calculate the max depth of its children and combine the top two."
     * 5. Code -> Code the Single DFS approach. It's shorter and highlights recursion skills.
     * 6. Dry Run -> Trace the top 2 branch logic on a simple 'Y' shaped graph.
     *
     *
     * 7. SOLUTION COMPARISON
     * ------------------------------------------------------------------------------------------
     * Approach       | Time   | Space  | Trade-offs                     | Recommendation
     * ---------------|--------|--------|--------------------------------|-----------------
     * Two-BFS Trick  | O(N)   | O(N)   | Two passes, immune to stack ovf| Great fallback
     * Single DFS DP  | O(N)   | O(N)   | Elegant, one pass, stack limit | 🥇 Primary Choice
     *
     *
     * 8. FOLLOW-UP QUESTIONS & ANSWERS
     * ------------------------------------------------------------------------------------------
     * F1: "What if the edges had weights (e.g., distances between cities)?"
     * A1: Both algorithms still work perfectly! For Two-BFS, we track the cumulative weight 
     *     instead of level-count. For DFS, instead of `+ 1`, we do `+ weight`.
     *
     * F2: "Can you return the ACTUAL path of the diameter, not just the length?"
     * A2: The Two-BFS method makes this much easier. During the second BFS from node A to 
     *     node B, we can keep a `parent[]` array. Once we find B, we backtrack using the 
     *     `parent` array to construct the exact path.
     *
     * F3: "What if the graph has cycles (it's not a tree)?"
     * A3: Then this problem becomes the "Longest Path Problem" in a general graph, which is 
     *     NP-Hard. We cannot solve it in O(N).
     *
     * F4: "How would you find the 'Center' of this tree?"
     * A4: The center lies exactly in the middle of the diameter path. Alternatively, we can 
     *     use Topological Sort (Kahn's Algorithm) to repeatedly peel off leaf nodes (degree 1) 
     *     until 1 or 2 nodes remain.
     *
     *
     * 9. FINAL TAKEAWAYS
     * ------------------------------------------------------------------------------------------
     * - Undirected Trees don't strictly need a `visited` boolean array for DFS. Passing the 
     *   `parent` node and checking `if (neighbor == parent)` is enough to prevent backtracking!
     * - When you hear "Tree Diameter", automatically think: "Top 2 depths of subtrees".
     * - The 2-BFS trick is highly specific to trees, but extremely useful to keep in your back 
     *   pocket for path-reconstruction follow-ups.
     */

    // Test Runner
    public static void main(String[] args) {
        TreeDiameter solution = new TreeDiameter();

        // Normal Case: Y-shaped tree
        //   0-1-2-3
        //     |
        //     4-5
        // Diameter is between 3 and 5. Path: 3-2-1-4-5 (4 edges)
        int[][] edges1 = {{0,1}, {1,2}, {2,3}, {1,4}, {4,5}};
        System.out.println("Test 1 (DFS) Expected: 4 | Actual: " + solution.treeDiameterDFS(6, edges1));
        System.out.println("Test 1 (BFS) Expected: 4 | Actual: " + solution.treeDiameterTwoBFS(6, edges1));

        // Edge Case: N = 1 (No edges)
        int[][] edges2 = {};
        System.out.println("Test 2 Expected: 0 | Actual: " + solution.treeDiameterDFS(1, edges2));

        // Edge Case: N = 2 (1 edge)
        int[][] edges3 = {{0,1}};
        System.out.println("Test 3 Expected: 1 | Actual: " + solution.treeDiameterDFS(2, edges3));

        // Star Graph: Center is 0, connected to 1, 2, 3, 4
        // Diameter should be 2.
        int[][] edges4 = {{0,1}, {0,2}, {0,3}, {0,4}};
        System.out.println("Test 4 Expected: 2 | Actual: " + solution.treeDiameterDFS(5, edges4));
    }
}

import java.util.*;

/**
 * ================================================================
 * TREE DIAMETER - ALL POSSIBLE APPROACHES (INTERVIEW READY FILE)
 * ================================================================
 *
 * Problem:
 * --------
 * Given an undirected tree with n nodes (0 to n-1),
 * return the DIAMETER (longest path in terms of edges).
 *
 * Key Property:
 * -------------
 * Tree = connected + acyclic
 *
 * Diameter = longest path between ANY two nodes
 *
 *
 * ================================================================
 * 🧠 HOW TO THINK (INTERVIEW INSIGHT)
 * ================================================================
 *
 * Whenever you see:
 *   👉 "Longest path in a tree"
 *
 * Think:
 *   1. BFS twice (MOST COMMON)
 *   2. DFS height-based
 *   3. Leaf trimming (Topological / Kahn's)
 *   4. Tree DP (same as DFS but conceptualized differently)
 *
 *
 * ================================================================
 * 📌 APPROACH 1: 2 BFS (MOST IMPORTANT)
 * ================================================================
 *
 * IDEA:
 * -----
 * 1. Pick ANY node (0)
 * 2. Find farthest node A
 * 3. From A find farthest node B
 * 4. Distance(A, B) = DIAMETER
 *
 * WHY IT WORKS?
 * -------------
 * Longest path endpoints are always leaf nodes in a tree.
 *
 *
 * TIME:  O(N)
 * SPACE: O(N)
 *
 * BEST FOR INTERVIEWS ✅
 */
class Solution_BFS {

    public int treeDiameter(int[][] edges) {
        int n = edges.length + 1;

        List<List<Integer>> graph = buildGraph(n, edges);

        // Step 1: BFS from arbitrary node (0)
        BFSResult first = bfs(0, graph);

        // Step 2: BFS from farthest node found
        BFSResult second = bfs(first.node, graph);

        // Diameter = distance from second BFS
        return second.distance;
    }

    /**
     * Standard BFS that tracks farthest node + distance
     */
    private BFSResult bfs(int start, List<List<Integer>> graph) {
        Queue<Integer> queue = new ArrayDeque<>();
        boolean[] visited = new boolean[graph.size()];

        queue.offer(start);
        visited[start] = true;

        int farthestNode = start;
        int distance = -1;

        // Level-order traversal
        while (!queue.isEmpty()) {
            int size = queue.size();
            distance++; // each level = 1 edge farther

            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                farthestNode = node;

                for (int nei : graph.get(node)) {
                    if (!visited[nei]) {
                        visited[nei] = true;
                        queue.offer(nei);
                    }
                }
            }
        }

        return new BFSResult(farthestNode, distance);
    }

    private List<List<Integer>> buildGraph(int n, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (int[] e : edges) {
            graph.get(e[0]).add(e[1]);
            graph.get(e[1]).add(e[0]);
        }

        return graph;
    }

    // Java record (cleaner return object)
    private record BFSResult(int node, int distance) {}
}


/**
 * ================================================================
 * 📌 APPROACH 2: DFS (HEIGHT BASED)
 * ================================================================
 *
 * IDEA:
 * -----
 * At every node:
 *   - Get top 2 maximum subtree heights
 *   - Candidate diameter = sum of those 2 heights
 *
 * WHY?
 * ----
 * Longest path can pass THROUGH a node.
 *
 *
 * TIME:  O(N)
 * SPACE: O(H) recursion stack
 *
 * Very important conceptually ⭐
 */
class Solution_DFS {

    private int diameter = 0;

    public int treeDiameter(int[][] edges) {
        int n = edges.length + 1;

        List<List<Integer>> graph = buildGraph(n, edges);

        dfs(0, -1, graph);

        return diameter;
    }

    /**
     * Returns height of subtree rooted at node
     */
    private int dfs(int node, int parent, List<List<Integer>> graph) {

        int max1 = 0; // largest height
        int max2 = 0; // second largest height

        for (int nei : graph.get(node)) {
            if (nei == parent) continue;

            int height = dfs(nei, node, graph) + 1;

            // maintain top 2 heights
            if (height > max1) {
                max2 = max1;
                max1 = height;
            } else if (height > max2) {
                max2 = height;
            }
        }

        // Update global diameter
        diameter = Math.max(diameter, max1 + max2);

        return max1;
    }

    private List<List<Integer>> buildGraph(int n, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (int[] e : edges) {
            graph.get(e[0]).add(e[1]);
            graph.get(e[1]).add(e[0]);
        }
        return graph;
    }
}


/**
 * ================================================================
 * 📌 APPROACH 3: LEAF TRIMMING (TOPOLOGICAL BFS)
 * ================================================================
 *
 * IDEA:
 * -----
 * Repeatedly remove leaf nodes layer by layer.
 *
 * Similar to:
 *   - Finding center of tree
 *   - Kahn's Algorithm
 *
 * Key observation:
 * ----------------
 * Each layer removal shrinks diameter from both ends.
 *
 * If layers removed = L:
 *   diameter = 2*L (if 1 node left)
 *   diameter = 2*L + 1 (if 2 nodes left)
 *
 *
 * TIME:  O(N)
 * SPACE: O(N)
 *
 * Less common but VERY clever 💡
 */
class Solution_LeafTrimming {

    public int treeDiameter(int[][] edges) {
        int n = edges.length + 1;

        if (n == 1) return 0;

        List<Set<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new HashSet<>());

        for (int[] e : edges) {
            graph.get(e[0]).add(e[1]);
            graph.get(e[1]).add(e[0]);
        }

        List<Integer> leaves = new ArrayList<>();

        // Step 1: Find initial leaves
        for (int i = 0; i < n; i++) {
            if (graph.get(i).size() == 1) {
                leaves.add(i);
            }
        }

        int remainingNodes = n;
        int layers = 0;

        // Remove leaves layer by layer
        while (remainingNodes > 2) {
            remainingNodes -= leaves.size();
            List<Integer> newLeaves = new ArrayList<>();

            for (int leaf : leaves) {
                int neighbor = graph.get(leaf).iterator().next();

                graph.get(neighbor).remove(leaf);

                if (graph.get(neighbor).size() == 1) {
                    newLeaves.add(neighbor);
                }
            }

            leaves = newLeaves;
            layers++;
        }

        // Final diameter calculation
        if (remainingNodes == 1) {
            return 2 * layers;
        } else {
            return 2 * layers + 1;
        }
    }
}


/**
 * ================================================================
 * 📌 APPROACH 4: TREE DP (SAME AS DFS BUT FORMALIZED)
 * ================================================================
 *
 * IDEA:
 * -----
 * dp[node] = height of subtree
 *
 * diameter = max(dp[child1] + dp[child2])
 *
 * This is SAME as DFS but written in DP mindset.
 *
 *
 * TIME:  O(N)
 * SPACE: O(N)
 */
class Solution_TreeDP {

    private int diameter = 0;

    public int treeDiameter(int[][] edges) {
        int n = edges.length + 1;

        List<List<Integer>> graph = buildGraph(n, edges);

        computeHeight(0, -1, graph);

        return diameter;
    }

    private int computeHeight(int node, int parent, List<List<Integer>> graph) {

        List<Integer> heights = new ArrayList<>();

        for (int nei : graph.get(node)) {
            if (nei == parent) continue;

            heights.add(computeHeight(nei, node, graph) + 1);
        }

        // Sort descending to pick top 2
        heights.sort(Collections.reverseOrder());

        int max1 = heights.size() > 0 ? heights.get(0) : 0;
        int max2 = heights.size() > 1 ? heights.get(1) : 0;

        diameter = Math.max(diameter, max1 + max2);

        return max1;
    }

    private List<List<Integer>> buildGraph(int n, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (int[] e : edges) {
            graph.get(e[0]).add(e[1]);
            graph.get(e[1]).add(e[0]);
        }
        return graph;
    }
}


/**
 * ================================================================
 * 🏁 FINAL SUMMARY (VERY IMPORTANT)
 * ================================================================
 *
 * 1. 2 BFS  → ⭐ BEST (easy + reliable)
 * 2. DFS    → ⭐ deep understanding
 * 3. Leaf trimming → ⭐ advanced trick
 * 4. Tree DP → ⭐ conceptual clarity
 *
 *
 * ================================================================
 * 🧠 INTERVIEW SHORTCUT
 * ================================================================
 *
 * If stuck:
 *   👉 Always go with 2 BFS
 *
 * If interviewer pushes:
 *   👉 Switch to DFS (height-based)
 *
 *
 * ================================================================
 * 🔥 BONUS QUESTIONS THEY ASK
 * ================================================================
 *
 * - What if weighted tree? → Use Dijkstra instead of BFS
 * - What if binary tree? → Same DFS logic
 * - Can we do in one pass? → YES (DFS)
 *
 * ================================================================
 */

