import java.util.*;

/*
 * ==========================================================================================
 *                                   CLONE GRAPH
 * ==========================================================================================
 *
 * 1. PROBLEM RESTATEMENT IN SIMPLE TERMS
 * ------------------------------------------------------------------------------------------
 * You are given a node in a connected, undirected graph. Your job is to create a "deep copy" 
 * (a clone) of the entire graph. 
 * A deep copy means you cannot just copy the references to the existing nodes. You must 
 * create brand-new node objects in memory that have the exact same values and exact same 
 * connections (edges) as the original graph. Changing the clone should not affect the original.
 *
 *
 * 2. IDEA, INTUITION, AND KEY OBSERVATIONS
 * ------------------------------------------------------------------------------------------
 * - Graph Traversal: To copy the graph, we must visit every single node and every edge. 
 *   This immediately screams Graph Traversal (DFS or BFS).
 * - The "Cycle" Problem: Graphs can have cycles (e.g., A -> B -> C -> A). If we blindly 
 *   traverse and copy, we will get stuck in an infinite loop creating nodes.
 * - The "Duplicate Node" Problem: If Node A points to Node B, and Node C also points to 
 *   Node B, we shouldn't create two copies of Node B. Both cloned A and cloned C must point 
 *   to the SAME cloned B.
 * - The Solution (Map): We need a way to keep track of which nodes we have already copied. 
 *   A Hash Map is perfect for this. 
 *   Map mapping: <Original Node, Cloned Node>. 
 *   Whenever we encounter a node, we check the map. If it's there, we just link to the 
 *   already cloned node. If not, we create a clone, put it in the map, and traverse further.
 *
 *
 * 3. HOW TO IDENTIFY THE RIGHT APPROACH
 * ------------------------------------------------------------------------------------------
 * - The problem explicitly asks to process a graph and its connections. 
 * - Connected components + exhaustive search -> BFS or DFS.
 * - Need to track visited state + map old objects to new objects -> HashMap.
 *
 *
 * 4. CLARIFYING QUESTIONS TO ASK THE INTERVIEWER (And why they matter)
 * ------------------------------------------------------------------------------------------
 * Q1: Can the given node be null? (What happens if the graph is empty?)
 *     Why: Prevents NullPointerException. Determines if we need an initial null check.
 * Q2: Can the graph have disconnected components?
 *     Why: If yes, we'd need to iterate over all nodes (like a provided list of nodes) to 
 *          start DFS/BFS. The prompt says "connected graph", so one traversal is enough.
 * Q3: Can node values be duplicates?
 *     Why: If values are unique, we *could* theoretically map Integer -> Node. But mapping 
 *          Node -> Node is universally safer and handles duplicate values if they ever occur.
 * Q4: Are there self-loops or multiple edges between the same two nodes?
 *     Why: Affects our traversal. The prompt guarantees no self-loops/repeated edges, which 
 *          keeps our time complexity strictly O(V+E) without redundant edge processing.
 * Q5: How large is the graph (max nodes/depth)?
 *     Why: If the graph is a straight line of 100,000 nodes, DFS recursion will cause a 
 *          StackOverflowError. In that case, BFS or iterative DFS is required. The prompt 
 *          says max 100 nodes, so recursive DFS is perfectly safe.
 *
 *
 * 5. ASCII VISUALS / TRACING
 * ------------------------------------------------------------------------------------------
 * Consider this original graph represented as an adjacency list: [[2,4],[1,3],[2,4],[1,3]]
 * 
 * Original Graph:            Cloned Graph (Goal):
 *      1 ------- 2                1'------- 2'
 *      |         |                |         |
 *      |         |     ====>      |         |
 *      4 ------- 3                4'------- 3'
 *
 * Let's trace DFS on this graph starting at Node 1:
 * 1. Visit 1: Is 1 in Map? No. Create 1'. Map = {1: 1'}.
 *    Iterate neighbors of 1: (2 and 4).
 * 2.   -> Visit 2 (neighbor of 1): Is 2 in Map? No. Create 2'. Map = {1: 1', 2: 2'}.
 *         Add to 1'.neighbors: 2'
 *         Iterate neighbors of 2: (1 and 3).
 * 3.      -> Visit 1 (neighbor of 2): Is 1 in Map? Yes! Return 1'. 
 *            Add to 2'.neighbors: 1'.  <-- CYCLE HANDLED!
 * 4.      -> Visit 3 (neighbor of 2): Is 3 in Map? No. Create 3'. Map = {1:1', 2:2', 3:3'}.
 *            Add to 2'.neighbors: 3'.
 *            ... (continues to 4, then backtracks up) ...
 * 
 * 6. EXAMPLES AND IMPORTANT EDGE CASES
 * ------------------------------------------------------------------------------------------
 * - Edge Case 1: Empty Graph (Input: null) -> Must return null.
 * - Edge Case 2: Single Node (Input: Node with empty neighbors list) -> Should return 
 *                a single cloned node with an empty neighbors list.
 * - Failure of Naïve Approach: If you try to copy neighbors before putting the current 
 *   cloned node in the map, you will infinitely loop between Node 1 and Node 2.
 *
 *
 * 7. COMMON MISTAKES AND PITFALLS
 * ------------------------------------------------------------------------------------------
 * 1. Shallow Copy: Adding the *original* neighbors to the cloned node's neighbor list. 
 *    (e.g., cloneNode.neighbors.add(neighbor) instead of cloneNode.neighbors.add(clonedNeighbor)).
 * 2. Late Mapping: Putting the cloned node into the Hash Map *after* the recursive DFS 
 *    calls. This causes an infinite loop because the cycle check fails.
 * 3. Infinite Recursion: Forgetting the visited map entirely.
 *
 *
 * 8. INTERVIEW STRATEGY: HOW TO APPROACH AND EXPLAIN
 * ------------------------------------------------------------------------------------------
 * 1. State the problem back: "I need to do a deep copy of a graph. Pointers must map correctly."
 * 2. Highlight the main challenge: "The graph has cycles, so I must avoid infinite loops."
 * 3. Propose the data structure: "I will use a HashMap to keep track of OriginalNode -> ClonedNode."
 * 4. Propose the algorithm: "I can use either DFS or BFS. Both run in O(V + E) time."
 * 5. Code the DFS (it's shorter and cleaner). Mention BFS as an alternative if stack depth 
 *    is a concern.
 *
 * WHICH SOLUTION TO CHOOSE AND WHY?
 * Choose APPROACH 2 (Recursive DFS) for the actual interview coding phase. It is concise, 
 * easy to read, and less prone to off-by-one or queue-management bugs. If the interviewer 
 * asks about StackOverflow, immediately offer APPROACH 3 (BFS).
 * ==========================================================================================
 */

// Definition for a Node structure.
class Node {
    public int val;
    public List<Node> neighbors;

    public Node() {
        val = 0;
        neighbors = new ArrayList<Node>();
    }

    public Node(int _val) {
        val = _val;
        neighbors = new ArrayList<Node>();
    }

    public Node(int _val, ArrayList<Node> _neighbors) {
        val = _val;
        neighbors = _neighbors;
    }
}

public class CloneGraph {

    /*
     * APPROACH 1: "BRUTE FORCE" / TWO-PASS APPROACH (Conceptual)
     * --------------------------------------------------------------------------------------
     * Idea: 
     * Pass 1: Traverse the graph (using BFS/DFS) purely to create all cloned nodes with 
     *         no edges. Store them in a Map.
     * Pass 2: Traverse the original graph again. For every original edge A -> B, look up 
     *         A' and B' in the map, and add B' to A'.neighbors.
     *
     * Why it's suboptimal: It requires traversing the graph twice. It's conceptually simple
     * but we can easily combine the creation of edges into the first pass, which is what 
     * Optimal DFS/BFS do.
     * 
     * Complexity: Time O(V + E), Space O(V). (Not implemented here as one-pass is standard).
     */


    /*
     * APPROACH 2: OPTIMAL DEPTH-FIRST SEARCH (DFS) - RECURSIVE
     * --------------------------------------------------------------------------------------
     * Step-by-step breakdown:
     * 1. Check if the starting node is null. Return null if so.
     * 2. Check if we've already cloned this node using our HashMap. If yes, return the clone.
     * 3. Create the clone for the current node.
     * 4. **CRITICAL STEP**: Add the original node and its clone to the map IMMEDIATELY, 
     *    before cloning neighbors. This prevents infinite loops.
     * 5. Iterate over the original node's neighbors. For each neighbor, recursively call DFS 
     *    and add the returned cloned neighbor to the current cloned node's neighbor list.
     * 6. Return the cloned node.
     *
     * Time Complexity: O(V + E) - We visit every vertex (V) exactly once and iterate through 
     *                  all its edges (E).
     * Space Complexity: O(V) - The HashMap stores V nodes. The recursion stack can also go 
     *                   up to V deep in the worst case (a single straight line graph).
     */
    
    // Global map for DFS to track visited/cloned nodes
    private Map<Node, Node> dfsVisitedMap = new HashMap<>();

    public Node cloneGraphDFS(Node node) {
        // Base case: empty graph
        if (node == null) {
            return null;
        }

        // If the node was already cloned, return the cloned instance from the map
        if (dfsVisitedMap.containsKey(node)) {
            return dfsVisitedMap.get(node);
        }

        // Create the clone for the current node (without neighbors initially)
        Node cloneNode = new Node(node.val);

        // MUST PUT IN MAP BEFORE RECURSING NEIGHBORS!
        // This ensures that if a neighbor points back to this node, it will 
        // find 'cloneNode' in the map and successfully return it, breaking the cycle.
        dfsVisitedMap.put(node, cloneNode);

        // Iterate through all neighbors of the original node
        for (Node neighbor : node.neighbors) {
            // Recursively clone the neighbor and add it to the cloned node's neighbor list
            cloneNode.neighbors.add(cloneGraphDFS(neighbor));
        }

        return cloneNode;
    }


    /*
     * APPROACH 3: OPTIMAL BREADTH-FIRST SEARCH (BFS) - ITERATIVE
     * --------------------------------------------------------------------------------------
     * Step-by-step breakdown:
     * 1. Check for null base case.
     * 2. Initialize a HashMap to map Original Node -> Cloned Node.
     * 3. Initialize a Queue for BFS traversal.
     * 4. Create a clone of the starting node, put it in the map, and add original to Queue.
     * 5. While Queue is not empty:
     *    a. Pop the current node.
     *    b. Iterate over its neighbors.
     *    c. If a neighbor is NOT in the map (hasn't been cloned yet):
     *         i. Create a clone for it.
     *         ii. Put it in the map.
     *         iii. Add original neighbor to Queue so we process its edges later.
     *    d. Add the cloned neighbor to the cloned current node's neighbor list.
     *
     * Time Complexity: O(V + E) - Every node is added to the queue once, and we look at 
     *                  each edge twice (once from each side).
     * Space Complexity: O(V) - The HashMap stores V nodes. The Queue can hold at most O(V) 
     *                   nodes (specifically, the maximum width of the graph).
     */
    public Node cloneGraphBFS(Node node) {
        if (node == null) {
            return null;
        }

        Map<Node, Node> bfsVisitedMap = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();

        // Start processing the first node
        Node startClone = new Node(node.val);
        bfsVisitedMap.put(node, startClone);
        queue.add(node); // Queue ALWAYS stores ORIGINAL nodes to explore their neighbors

        while (!queue.isEmpty()) {
            // Get the next original node to process
            Node current = queue.poll();

            // Iterate through its original neighbors
            for (Node neighbor : current.neighbors) {
                
                // If neighbor hasn't been cloned yet
                if (!bfsVisitedMap.containsKey(neighbor)) {
                    // Clone it and add to map
                    bfsVisitedMap.put(neighbor, new Node(neighbor.val));
                    // Add the original neighbor to queue so we can process ITS neighbors later
                    queue.add(neighbor);
                }

                // Important: Link the current cloned node to the cloned neighbor.
                // bfsVisitedMap.get(current) gets the cloned current node.
                // bfsVisitedMap.get(neighbor) gets the cloned neighbor node.
                bfsVisitedMap.get(current).neighbors.add(bfsVisitedMap.get(neighbor));
            }
        }

        return startClone;
    }

    /*
     * ==========================================================================================
     * INTERVIEWER FOLLOW-UP QUESTIONS & ANSWERS
     * ==========================================================================================
     * 
     * F1: What if the graph is too deep and recursive DFS causes a StackOverflow?
     * Ans: We switch to the iterative BFS approach using a Queue, or implement an iterative 
     *      DFS using a Stack. The heap memory limit is much larger than the call stack limit.
     * 
     * F2: What if this was a Directed Graph? Does the algorithm change?
     * Ans: No, the exact same logic works! Both DFS and BFS simply follow directed edges 
     *      instead of undirected ones. The visited Map prevents infinite loops in directed 
     *      cycles just as effectively.
     *
     * F3: What if multiple components exist in the graph (i.e., not a single connected graph)?
     * Ans: The current signature only takes one 'Node', meaning we can only clone the 
     *      connected component attached to that node. If the input was a List of all Graph 
     *      nodes, we would iterate through the list, and for any node not yet in our Map, 
     *      we would initiate a BFS/DFS from it.
     *
     * F4: Can we optimize the space complexity further?
     * Ans: O(V) space is strictly necessary because we MUST store the newly created V nodes 
     *      in memory to return them, and we MUST keep a mapping to resolve cyclic references.
     *      We cannot optimize space below O(V).
     * 
     * F5: Is it possible to solve this using an array instead of a HashMap?
     * Ans: Yes, if the constraint states Node values are 1 to N consecutively. 
     *      We could use `Node[] visited = new Node[101];` (since max nodes = 100).
     *      `visited[node.val]` would store the cloned node. Array access is O(1) and has 
     *      less overhead than HashMap. But HashMap is preferred because it handles arbitrary 
     *      node values without assuming contiguous integers.
     * ==========================================================================================
     */


    // --------------------------------------------------------------------------------------
    // TEST RUNNER CODE
    // --------------------------------------------------------------------------------------
    public static void main(String[] args) {
        CloneGraph solution = new CloneGraph();

        System.out.println("--- Building Original Graph ---");
        // Building graph: [[2,4],[1,3],[2,4],[1,3]] 
        // 1 - 2
        // |   |
        // 4 - 3
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        Node node3 = new Node(3);
        Node node4 = new Node(4);

        node1.neighbors = Arrays.asList(node2, node4);
        node2.neighbors = Arrays.asList(node1, node3);
        node3.neighbors = Arrays.asList(node2, node4);
        node4.neighbors = Arrays.asList(node1, node3);

        System.out.println("Original Node 1 HashCode: " + node1.hashCode());

        // Test DFS
        System.out.println("\n--- Testing DFS Clone ---");
        Node dfsClone = solution.cloneGraphDFS(node1);
        System.out.println("DFS Cloned Node 1 HashCode: " + dfsClone.hashCode());
        System.out.println("Are original and cloned identical objects in memory? " + (node1 == dfsClone));
        printGraph(dfsClone);

        // Test BFS
        System.out.println("\n--- Testing BFS Clone ---");
        Node bfsClone = solution.cloneGraphBFS(node1);
        System.out.println("BFS Cloned Node 1 HashCode: " + bfsClone.hashCode());
        System.out.println("Are original and cloned identical objects in memory? " + (node1 == bfsClone));
        printGraph(bfsClone);
    }

    // Helper method to print the graph structure to verify correctness
    private static void printGraph(Node node) {
        if (node == null) {
            System.out.println("Graph is empty.");
            return;
        }

        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        visited.add(node);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            System.out.print("Node " + current.val + " neighbors: [ ");
            for (Node neighbor : current.neighbors) {
                System.out.print(neighbor.val + " ");
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
            System.out.println("]");
        }
    }
}


import java.util.*;

/**
 * ============================================================
 * 🔥 Clone Graph - BFS (Single Pass) — INTERVIEW GOLD
 * ============================================================
 *
 * 🧠 CORE IDEA:
 *
 * We traverse the graph using BFS.
 *
 * At ANY moment:
 *      If a node is seen → ensure its clone exists
 *
 * So we maintain:
 *      Map<Original Node, Cloned Node>
 *
 * ------------------------------------------------------------
 * 💡 KEY INSIGHT (THIS IS THE SHIFT)
 *
 * Old Thinking:
 *      "First create nodes, then connect edges"
 *
 * New Thinking:
 *      "Create nodes lazily WHEN needed"
 *
 * ------------------------------------------------------------
 * 🚀 ALGORITHM:
 *
 * 1. Create clone of root
 * 2. Start BFS
 * 3. For every node:
 *      - For each neighbor:
 *          a) If neighbor not cloned → create + push to queue
 *          b) Connect clone(current) → clone(neighbor)
 *
 * ------------------------------------------------------------
 *
 * Time Complexity  : O(V + E)
 * Space Complexity : O(V)
 *
 * ============================================================
 */

public class CloneGraph {

    public static Node cloneGraph(Node root) {

        // Edge case
        if (root == null) return null;

        // Map to store original -> clone
        Map<Node, Node> oldToNew = new HashMap<>();

        // BFS queue
        Queue<Node> queue = new ArrayDeque<>();

        // ----------------------------------------------------
        // STEP 1: Create clone of root
        // ----------------------------------------------------
        oldToNew.put(root, new Node(root.data));

        // Start BFS
        queue.offer(root);

        // ----------------------------------------------------
        // STEP 2: BFS traversal
        // ----------------------------------------------------
        while (!queue.isEmpty()) {

            Node current = queue.poll();

            // Clone node of current
            Node cloneCurrent = oldToNew.get(current);

            // Traverse neighbors
            for (Node neighbor : current.neighbors) {

                // ------------------------------------------------
                // CASE 1: Neighbor not yet cloned
                // ------------------------------------------------
                if (!oldToNew.containsKey(neighbor)) {

                    // Create clone
                    oldToNew.put(neighbor, new Node(neighbor.data));

                    // Add to queue for future processing
                    queue.offer(neighbor);
                }

                // ------------------------------------------------
                // CASE 2: Connect clone graph
                // ------------------------------------------------
                // IMPORTANT:
                // Always connect CLONE -> CLONE
                cloneCurrent.neighbors.add(oldToNew.get(neighbor));
            }
        }

        return oldToNew.get(root);
    }
}

import java.util.*;

/**
 * ============================================================
 * 🔥 Clone Graph - DFS (Recursive)
 * ============================================================
 *
 * 🧠 CORE IDEA:
 *
 * Use recursion to traverse graph
 *
 * ------------------------------------------------------------
 * 💡 KEY INSIGHT:
 *
 * - If node already cloned → return it (avoid cycles)
 * - Else:
 *      1. Create clone
 *      2. Recursively clone neighbors
 *
 * ------------------------------------------------------------
 *
 * This naturally becomes SINGLE PASS due to recursion
 *
 * ------------------------------------------------------------
 *
 * Time Complexity  : O(V + E)
 * Space Complexity : O(V) + recursion stack
 *
 * ============================================================
 */

public class CloneGraphDFS {

    public static Node cloneGraph(Node root) {

        if (root == null) return null;

        Map<Node, Node> oldToNew = new HashMap<>();

        return dfs(root, oldToNew);
    }

    private static Node dfs(Node node, Map<Node, Node> oldToNew) {

        // ----------------------------------------------------
        // BASE CASE:
        // If already cloned → return existing clone
        // ----------------------------------------------------
        if (oldToNew.containsKey(node)) {
            return oldToNew.get(node);
        }

        // ----------------------------------------------------
        // STEP 1: Create clone
        // ----------------------------------------------------
        Node clone = new Node(node.data);

        // Save mapping BEFORE recursion
        // (prevents infinite loop in cycles)
        oldToNew.put(node, clone);

        // ----------------------------------------------------
        // STEP 2: Clone neighbors recursively
        // ----------------------------------------------------
        for (Node neighbor : node.neighbors) {

            // Recursively clone neighbor
            Node clonedNeighbor = dfs(neighbor, oldToNew);

            // Connect clone graph
            clone.neighbors.add(clonedNeighbor);
        }

        return clone;
    }
}

