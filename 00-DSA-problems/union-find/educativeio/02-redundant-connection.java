/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * You are given an undirected graph with 'n' nodes (labeled 1 to n) and 'n' edges.
 * The graph is connected and contains exactly one cycle.
 * Return an edge that can be removed to make the graph a valid tree of 'n' nodes.
 * If there are multiple answers, return the edge that occurs LAST in the input array.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Are the node labels 0-indexed or 1-indexed?
 *    (Nodes are 1 to n. This prevents off-by-one IndexOutOfBoundsExceptions in arrays).
 * 2. Why does the problem specify returning the edge that occurs "last" in the input?
 *    (Any edge in the cycle can be removed to form a tree. The "last" constraint forces a 
 *    deterministic answer and heavily influences our iteration strategy).
 * 3. Can the graph have multiple disconnected components?
 *    (No, the prompt guarantees the graph is connected. We don't have to worry about forests).
 * 4. Is the graph directed or undirected?
 *    (Undirected. This matters because cycle detection in directed graphs is entirely different, 
 *    involving in-degrees and back-edges).
 * 5. Are there self-loops or parallel edges?
 *    (No repeated edges, no self-loops as per constraints).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: We have N nodes and N edges. A valid tree with N nodes 
 * MUST have exactly N-1 edges. The 1 extra edge is what creates the single cycle.
 * We must identify the edges forming the cycle, and specifically pluck out the one 
 * that appears latest in the input array.
 * 
 * --- Approach 1: Brute Force (Remove and Check) ---
 * Idea: Since we need the *last* edge that breaks the cycle, let's just iterate 
 * through the `edges` array backwards. For each edge, pretend it doesn't exist. 
 * Run a Breadth-First Search (BFS) or Depth-First Search (DFS) on the remaining N-1 edges 
 * to see if we can visit all N nodes. If we can, the remaining graph is a connected 
 * tree, and the edge we removed is our answer!
 * What I'd naturally try: For loop backwards, build adjacency list, run BFS.
 * Why it works: Removing an edge that isn't part of the cycle will disconnect the graph. 
 * Removing an edge that IS part of the cycle leaves a connected tree.
 * Why it's too slow: We are building a graph and doing a full traversal for every single edge.
 * What work is repeated: We re-traverse the exact same stable parts of the graph N times.
 * 
 * Time Complexity: O(N^2) — because for each of the N edges, we run a BFS which takes O(V+E) = O(N) time.
 * Space Complexity: O(N) — because we build an adjacency list of size N and a visited array of size N.
 * (Note: Given N <= 100, N^2 = 10,000, which actually easily passes, but fails hard at scale).
 * 
 * --- Approach 2: Topological Sort / Degree Counting (Kahn's algorithm adaptation) ---
 * Idea: In a tree, leaf nodes have a degree of 1. If we iteratively remove nodes with degree 1 
 * (and their attached edges), the only things left at the end will be the nodes forming the cycle!
 * Once we know exactly which nodes are in the cycle, we scan the `edges` array backward and 
 * return the first edge whose both endpoints are in our cycle set.
 * Why it works: "Trimming the leaves" safely dissolves all tree branches, leaving only the loop.
 * Why it's too costly/complex: It requires building an adjacency list, a degree array, a queue for 
 * trimming, a set for the cycle nodes, and multiple passes over the data.
 * 
 * Time Complexity: O(N) — because building the graph, trimming leaves, and scanning edges all take linear time.
 * Space Complexity: O(N) — because of the adjacency list, degree array, and queue queue/set.
 * 
 * --- Approach 3: Union-Find (Disjoint Set) ---
 * Idea: Process the edges in the exact forward order they are given. We start with N isolated nodes. 
 * For each edge [u, v], we try to merge the set containing 'u' with the set containing 'v'. 
 * Observation: If we look at an edge [u, v] and find that 'u' and 'v' are *already* in the same set, 
 * it means there is already a path between them using previously processed edges. Adding this 
 * current edge creates a cycle!
 * Because we process left-to-right, the VERY FIRST edge that attempts to connect two already-connected 
 * nodes is mathematically guaranteed to be the edge in the cycle that appears LAST in the input array.
 * Why? A cycle requires all its edges to be complete. The one that appears latest in the input 
 * will be the one that closes the loop.
 * 
 * Time Complexity: O(N * α(N)) — because we process N edges, and Union-Find operations take amortized O(1) time (Inverse Ackermann function).
 * Space Complexity: O(N) — because we only need an array of size N+1 to track the parents of the sets. No adjacency list needed!
 * 
 * Interview Decision: I would write **Union-Find**. It is dramatically less code, uses less memory 
 * (no graph building), and perfectly leverages the "return the last occurring edge" constraint 
 * as a native feature of its left-to-right processing.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. The smallest possible graph: N=3 (a simple triangle).
 * 2. 1-based indexing: The nodes are 1 to N. A standard array of size N will crash on node N. 
 *    Must allocate size N+1.
 * 3. The cycle involves all nodes (a ring graph) vs the cycle involves only 3 nodes in a 100-node graph.
 *    (UF handles both identically well).
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "How do I know when a loop forms without traversing?"
 * Correctness intuition: If I am building a graph one edge at a time, a cycle forms EXACTLY 
 * at the moment I try to connect two nodes that already have a path between them.
 * 
 * --- Examples & ASCII ---
 * Edges: [[1,2], [2,3], [3,4], [1,4], [1,5]]
 * 
 * Step-by-step graph construction:
 * [1,2] -> 1-2
 * [2,3] -> 1-2-3
 * [3,4] -> 1-2-3-4
 * [1,4] -> 4 is already connected to 1 (via 3, 2). Adding [1,4] closes the loop! THIS is the redundant edge.
 * 
 * ASCII visualization:
 *     5 -- 1 -- 2
 *          |    |
 *          4 -- 3
 * 
 * --- Dry Run (Union-Find) ---
 * Edges: [[1,2], [2,3], [3,4], [1,4], [1,5]]
 * Init: parent = [0, 1, 2, 3, 4, 5] (each node is its own parent)
 * 
 * Iter 1: Edge [1, 2]. find(1)=1, find(2)=2. Different! Union them. parent[2] = 1.
 *         parent = [0, 1, 1, 3, 4, 5]
 * Iter 2: Edge [2, 3]. find(2)=1, find(3)=3. Different! Union them. parent[3] = 1.
 *         parent = [0, 1, 1, 1, 4, 5]
 * Iter 3: Edge [3, 4]. find(3)=1, find(4)=4. Different! Union them. parent[4] = 1.
 *         parent = [0, 1, 1, 1, 1, 5]
 * Iter 4: Edge [1, 4]. find(1)=1, find(4)=1. SAME! 
 *         They share a root. This edge forms a cycle. Return [1, 4].
 * 
 * --- Pitfalls ---
 * 1. Allocating `parent` array of size `N`. Since nodes are 1-indexed, accessing `parent[N]` throws OOB.
 * 2. Returning just a boolean. The problem asks for the *exact edge* `int[]`.
 * 3. Forgetting Path Compression in Union-Find. Without it, finding roots degrades to O(N) time per step 
 *    in a linear graph, making total time O(N^2).
 * 
 * --- Pattern Recognition ---
 * When you see: "Undirected graph", "Cycle detection", "Connectivity", "Adding edges sequentially".
 * Think: UNION-FIND.
 * Transfers to: Number of Connected Components, Kruskal's Minimum Spanning Tree, Accounts Merge.
 * 
 * --- Interview Script ---
 * "Since we want to find the edge that creates a cycle, and specifically the one that appears last 
 * in the input, Union-Find is the perfect fit. We can iterate through the edges in order, adding 
 * them to a Disjoint Set. If we process an edge and find that both its endpoints already share the 
 * same root parent, it means they are already connected. That current edge is the one closing the 
 * cycle, and because we are processing in order, it's inherently the last one in the input that 
 * forms the loop. That gives us O(N) time with path compression, and O(N) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the graph was DIRECTED? (This is "Redundant Connection II" on LeetCode).
 * A: Union-Find alone isn't enough because a directed cycle might exist, OR a node might have 
 *    an in-degree of 2 (two parents), which also invalidates a tree. We'd have to check in-degrees 
 *    first, identify the two edges pointing to the same node, and check if removing one fixes the graph.
 * Q2: What if we needed to return ALL edges that make up the cycle?
 * A: Union-Find doesn't store paths. I would switch to DFS. Keep a `parent` tracking array during 
 *    traversal. When DFS hits an already visited node, use the `parent` array to backtrack and collect 
 *    all nodes in the loop.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Disjoint Set / Union-Find.
 * Key observation: An edge creates a cycle if its vertices are already in the same component.
 * Memorize: The `find` function with path compression. It's 4 lines of magic.
 * Most common trap: 1-based indexing causing out-of-bounds errors.
 * Mental trigger: "Remove edge to make a tree" -> Union-Find.
 */

 public class RedundantConnection {

    /**
     * APPROACH 3: Union-Find (Optimal)
     */
    public int[] findRedundantConnection(int[][] edges) {
        // N is the number of edges. Since it's a tree + 1 edge, N is also the number of nodes.
        int n = edges.length;
        
        // 1-based indexing means we need size N + 1. 
        // Index 0 will simply be ignored.
        // Example for N=3: parent array size 4.
        int[] parent = new int[n + 1];
        
        // Initialize the disjoint set: every node is initially its own parent (root).
        for (int i = 1; i <= n; i++) {
            parent[i] = i;
        }
        
        // Process edges in the exact order they appear in the input
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            
            // If u and v share the same root, they are already connected.
            // This edge [u, v] is the redundant one that completes the cycle!
            if (find(parent, u) == find(parent, v)) {
                return edge;
            }
            
            // Otherwise, union the two components
            union(parent, u, v);
        }
        
        // Fallback (should not be reached given problem constraints of exactly one cycle)
        return new int[0];
    }
    
    /**
     * Helper: Finds the root representative of node 'i'.
     * Utilizes Path Compression to flatten the tree, ensuring near O(1) amortized time.
     */
    private int find(int[] parent, int i) {
        // If a node is its own parent, it's the root of the component.
        if (parent[i] == i) {
            return i;
        }
        // Path Compression: Instead of just returning the root, we update this node's
        // parent to point DIRECTLY to the root. Subsequent calls will be O(1).
        return parent[i] = find(parent, parent[i]);
    }
    
    /**
     * Helper: Unites the sets containing 'u' and 'v'.
     */
    private void union(int[] parent, int u, int v) {
        int rootU = find(parent, u);
        int rootV = find(parent, v);
        
        // Connect the roots. (For standard problems, arbitrarily making rootU point to rootV 
        // is fine. For extreme performance, we could use Union by Rank, but path compression 
        // alone is sufficient for N=100).
        if (rootU != rootV) {
            parent[rootU] = rootV;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        RedundantConnection solution = new RedundantConnection();
        
        // Test Case 1: Simple Triangle Cycle
        // Graph: 1 - 2 - 3 - 1
        // Edges: [1,2], [1,3], [2,3]
        // [2,3] completes the cycle because 1 and 2 are connected, and 1 and 3 are connected.
        int[][] edges1 = {{1, 2}, {1, 3}, {2, 3}};
        int[] res1 = solution.findRedundantConnection(edges1);
        System.out.println("Test 1 Result: [" + res1[0] + ", " + res1[1] + "]"); // Expected: [2, 3]

        // Test Case 2: Cycle with a tail (Matches ASCII example in comments)
        // Edges: [1,2], [2,3], [3,4], [1,4], [1,5]
        // 1-4 forms the cycle. 1-5 is a tail attached to the cycle.
        int[][] edges2 = {{1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}};
        int[] res2 = solution.findRedundantConnection(edges2);
        System.out.println("Test 2 Result: [" + res2[0] + ", " + res2[1] + "]"); // Expected: [1, 4]
        
        // Test Case 3: The cycle edge is not at the very end of the array
        // Edges: [1,2], [2,3], [3,1], [4,5], [1,4]
        // Cycle is 1-2-3-1. [3,1] forms the cycle, even though [4,5] and [1,4] appear later!
        int[][] edges3 = {{1, 2}, {2, 3}, {3, 1}, {4, 5}, {1, 4}};
        int[] res3 = solution.findRedundantConnection(edges3);
        System.out.println("Test 3 Result: [" + res3[0] + ", " + res3[1] + "]"); // Expected: [3, 1]
    }
}

