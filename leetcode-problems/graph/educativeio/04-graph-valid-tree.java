import java.util.*;

/*
 * ==========================================================================================
 *                                     GRAPH VALID TREE
 * ==========================================================================================
 *
 * 1. PROBLEM RESTATEMENT IN SIMPLE TERMS
 * ------------------------------------------------------------------------------------------
 * You are given N nodes and a list of undirected edges connecting them. 
 * You need to determine if these edges form a "valid tree". 
 * A graph is a valid tree if and only if:
 *   1. It is fully connected (you can reach any node from any other node).
 *   2. It has absolutely no cycles (no closed loops).
 *
 *
 * 2. IDEA, INTUITION, AND KEY OBSERVATIONS
 * ------------------------------------------------------------------------------------------
 * - Graph Theory Magic Rule: For a graph with N nodes to be a valid tree, it MUST have 
 *   EXACTLY (N - 1) edges.
 *   - If edges < N - 1: The graph cannot possibly be fully connected.
 *   - If edges >= N: The graph is guaranteed to have at least one cycle.
 * - This single mathematical property trivializes the problem! If we first check that 
 *   edges.length == N - 1, we only need to verify ONE of the two conditions:
 *   EITHER verify it is fully connected, OR verify it has no cycles. 
 *   If one is true (along with N - 1 edges), the other is mathematically guaranteed to be true.
 *
 *
 * 3. HOW TO IDENTIFY THE RIGHT APPROACH
 * ------------------------------------------------------------------------------------------
 * - Cycle detection and connectivity are classic "Union-Find" (Disjoint Set) problems. 
 *   Union-Find excels at quickly finding cycles in undirected graphs.
 * - Alternatively, Graph Traversal (DFS/BFS) is the standard way to explore connectivity.
 * - Because we can use the "N - 1" edge rule, Union-Find becomes extremely concise and elegant.
 *
 *
 * 4. CLARIFYING QUESTIONS TO ASK THE INTERVIEWER (And why they matter)
 * ------------------------------------------------------------------------------------------
 * Q1: What happens if N = 1 and there are no edges?
 *     Why: A single node with no edges is technically a valid tree. We need to handle this.
 *          (Our N-1 edges rule holds: 1 - 1 = 0 edges, which matches).
 * Q2: Are there duplicate edges or self-loops in the input?
 *     Why: The constraints state "no repeated edges" and "x != y" (no self-loops), 
 *          so we don't need to write code to sanitize the input.
 * Q3: Can the graph have completely disconnected components?
 *     Why: Yes, an input could have N-1 edges but form a cycle in one component and leave 
 *          another node isolated. We must detect either the cycle or the isolation.
 *
 *
 * 5. ASCII VISUALS / TRACING
 * ------------------------------------------------------------------------------------------
 * Valid Tree: N = 5, edges = [[0,1], [0,2], [0,3], [1,4]]
 * Edges = 4 (which is N - 1). 
 *       3
 *       |
 *   1 - 0 - 2
 *   |
 *   4
 * Tracing Union-Find: All nodes start in their own sets: {0} {1} {2} {3} {4}
 * Edge [0,1] -> Union: {0,1}
 * Edge [0,2] -> Union: {0,1,2}
 * Edge [0,3] -> Union: {0,1,2,3}
 * Edge [1,4] -> Union: {0,1,2,3,4}. No cycles found. Result: TRUE.
 *
 * Invalid Tree (Cycle & Disconnected): N = 5, edges = [[0,1], [1,2], [2,0], [3,4]]
 * Edges = 4 (matches N - 1, but let's look closer).
 *   0 - 1       3 - 4
 *    \ /
 *     2
 * Tracing Union-Find: 
 * Edge [0,1] -> Union: {0,1}
 * Edge [1,2] -> Union: {0,1,2}
 * Edge [2,0] -> 2 and 0 are ALREADY in the same set {0,1,2}. CYCLE DETECTED!
 * Result: FALSE.
 *
 *
 * 6. EXAMPLES AND IMPORTANT EDGE CASES
 * ------------------------------------------------------------------------------------------
 * - Edge Case 1: N = 1, edges = [] -> Returns True.
 * - Edge Case 2: N = 2, edges = [] -> Returns False (edges < N - 1).
 * - Edge Case 3: N = 4, edges = [[0,1], [2,3]] -> Returns False (edges < N - 1).
 *
 *
 * 7. COMMON MISTAKES AND PITFALLS
 * ------------------------------------------------------------------------------------------
 * 1. Forgetting the N - 1 check. If you omit this, you must write complicated DFS logic to 
 *    detect cycles (keeping track of the "parent" node to avoid falsely identifying the 
 *    immediate backward edge as a cycle).
 * 2. In DFS, checking for visited nodes but forgetting to ensure that EVERY node was visited. 
 *    If the graph is disconnected, DFS might finish without finding a cycle, but it's still 
 *    not a valid tree.
 *
 *
 * 8. INTERVIEW STRATEGY: HOW TO APPROACH AND EXPLAIN
 * ------------------------------------------------------------------------------------------
 * 1. Start by stating the Graph Theory rule: "A valid tree of N nodes must have exactly N-1 edges."
 * 2. Explain that checking `edges.length != n - 1` immediately eliminates graphs with cycles 
 *    (too many edges) or disconnected graphs (too few edges).
 * 3. Propose Union-Find as the most optimal way to detect cycles among the remaining edges.
 * 4. Write the Union-Find solution. It requires very little code and shows mastery of 
 *    advanced data structures. 
 * 5. If asked for an alternative, explain the BFS/DFS approach.
 * ==========================================================================================
 */

public class GraphValidTree {

    /*
     * APPROACH 1: UNION-FIND (OPTIMAL & HIGHLY RECOMMENDED)
     * --------------------------------------------------------------------------------------
     * Idea: 
     * 1. Check if edges.length == N - 1. If not, return false immediately.
     * 2. Initialize a parent array where every node is its own parent.
     * 3. Iterate through edges. For each edge [u, v], find the root parent of u and v.
     * 4. If they have the SAME root parent, it means they are already connected by some 
     *    other path. Adding this edge creates a cycle. Return false.
     * 5. If they have DIFFERENT root parents, union them (make one the parent of the other).
     * 6. If we process all edges without finding a cycle, and we have N-1 edges, it MUST 
     *    be a fully connected valid tree. Return true.
     *
     * Complexity:
     * - Time: O(N + E * α(N)), where α is the Inverse Ackermann function. With path 
     *         compression, this is nearly O(1) per edge. Total time is effectively O(N).
     * - Space: O(N) for the parent array.
     */
    public boolean validTreeUnionFind(int n, int[][] edges) {
        // Condition 1: A valid tree MUST have exactly N - 1 edges
        if (edges.length != n - 1) {
            return false;
        }

        // Initialize Union-Find array. Each node is initially its own parent.
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        // Process each edge
        for (int[] edge : edges) {
            int node1 = edge[0];
            int node2 = edge[1];

            int root1 = findRoot(parent, node1);
            int root2 = findRoot(parent, node2);

            // If both nodes share the same root, a cycle exists!
            if (root1 == root2) {
                return false;
            }

            // Union: merge the two sets by pointing one root to the other
            parent[root1] = root2;
        }

        // If we reach here, there are no cycles and exactly N-1 edges. It is a valid tree.
        return true;
    }

    // Helper method for Union-Find with Path Compression optimization
    private int findRoot(int[] parent, int node) {
        if (parent[node] == node) {
            return node;
        }
        // Path compression: point the node directly to the ultimate root
        parent[node] = findRoot(parent, parent[node]);
        return parent[node];
    }


    /*
     * APPROACH 2: DEPTH-FIRST SEARCH (DFS) - ALTERNATIVE
     * --------------------------------------------------------------------------------------
     * Idea: 
     * 1. Check if edges.length == N - 1. If not, return false.
     * 2. Build an adjacency list to represent the graph.
     * 3. Start a DFS from node 0. Keep track of visited nodes.
     * 4. Because we ALREADY checked that edges == N - 1, we don't need complex cycle 
     *    detection logic (passing parent nodes around). We just need to check if DFS 
     *    manages to visit EVERY node.
     * 5. If visited count == N, it's connected (and thus a valid tree).
     *
     * Complexity:
     * - Time: O(N) because we only have N - 1 edges to traverse.
     * - Space: O(N) for the adjacency list, visited array, and recursion stack.
     */
    public boolean validTreeDFS(int n, int[][] edges) {
        // Condition 1: A valid tree MUST have exactly N - 1 edges
        if (edges.length != n - 1) {
            return false;
        }

        // Build the adjacency list
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            adjList.get(edge[0]).add(edge[1]);
            adjList.get(edge[1]).add(edge[0]);
        }

        // Set to track visited nodes
        boolean[] visited = new boolean[n];
        
        // Start DFS from node 0
        dfsTraverse(adjList, visited, 0);

        // Check if all nodes were visited
        for (boolean isVisited : visited) {
            if (!isVisited) {
                return false; // Graph is disconnected
            }
        }

        return true;
    }

    // Standard recursive DFS helper
    private void dfsTraverse(List<List<Integer>> adjList, boolean[] visited, int node) {
        if (visited[node]) return;
        
        visited[node] = true;
        
        for (int neighbor : adjList.get(node)) {
            if (!visited[neighbor]) {
                dfsTraverse(adjList, visited, neighbor);
            }
        }
    }


    /*
     * ==========================================================================================
     * INTERVIEWER FOLLOW-UP QUESTIONS & ANSWERS
     * ==========================================================================================
     * 
     * F1: "What if the input could contain duplicate edges or self-loops? Does N-1 still work?"
     * Ans: No, if there are duplicates (e.g., [0,1], [0,1]) or self loops (e.g., [0,0]), the 
     *      N-1 edge count check will yield false positives for cycles. We would first need to 
     *      clean the input (e.g., using a HashSet of edges) or rely purely on strict DFS 
     *      traversal keeping track of the `parent` node to detect true cycles.
     * 
     * F2: "Why did you use an array for Union-Find instead of a Map<Integer, Integer>?"
     * Ans: Because the nodes are labeled perfectly from 0 to N-1. Array indexing is O(1) and 
     *      has practically zero overhead compared to the hashing operations and object 
     *      allocations required by a HashMap. It's much faster and memory-efficient.
     *
     * F3: "Can we use BFS instead of DFS?"
     * Ans: Absolutely. We just swap the recursive stack with a Queue. We start by putting 
     *      Node 0 in the queue, mark it visited, and explore neighbors. At the end, we still 
     *      just check if `visitedNodesCount == N`.
     *
     * F4: "In your Union-Find, you didn't use 'Union by Rank'. Is that an issue?"
     * Ans: For general graphs, lacking Union by Rank makes the worst-case time complexity O(N) 
     *      per find operation. However, because we implemented 'Path Compression' in `findRoot`, 
     *      the trees stay very flat. In practice, Path Compression alone is extremely fast and 
     *      keeps code simple for interviews. If asked, I could easily add a `rank[]` array.
     * ==========================================================================================
     */


    // --------------------------------------------------------------------------------------
    // TEST RUNNER CODE
    // --------------------------------------------------------------------------------------
    public static void main(String[] args) {
        GraphValidTree solution = new GraphValidTree();

        // Test Case 1: Valid Tree
        int n1 = 5;
        int[][] edges1 = {{0, 1}, {0, 2}, {0, 3}, {1, 4}};
        System.out.println("Test 1 (Expected true): " + solution.validTreeUnionFind(n1, edges1));

        // Test Case 2: Cycle exists (Too many edges)
        int n2 = 5;
        int[][] edges2 = {{0, 1}, {1, 2}, {2, 3}, {1, 3}, {1, 4}};
        System.out.println("Test 2 (Expected false - cycle): " + solution.validTreeUnionFind(n2, edges2));

        // Test Case 3: Disconnected components (Right number of edges, but a cycle exists)
        int n3 = 5;
        int[][] edges3 = {{0, 1}, {1, 2}, {2, 0}, {3, 4}};
        System.out.println("Test 3 (Expected false - disconnected): " + solution.validTreeUnionFind(n3, edges3));

        // Test Case 4: Single Node
        int n4 = 1;
        int[][] edges4 = {};
        System.out.println("Test 4 (Expected true - single node): " + solution.validTreeUnionFind(n4, edges4));
        
        // Test Case 5: Disconnected components (Too few edges)
        int n5 = 4;
        int[][] edges5 = {{0, 1}, {2, 3}};
        System.out.println("Test 5 (Expected false - disconnected): " + solution.validTreeDFS(n5, edges5));
    }
}


