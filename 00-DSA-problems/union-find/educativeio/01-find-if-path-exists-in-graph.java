/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a 2D list `edges` representing a bidirectional graph with `n` vertices 
 * (labeled 0 to n-1), determine if a valid path exists from `source` to `destination`.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can source and destination be the same node? 
 *    (Prevents missing the O(1) early exit if we don't need to traverse at all).
 * 2. Is it possible for the graph to be completely disconnected or for nodes to have no edges?
 *    (Ensures we handle isolated nodes properly, especially if source != dest but edges is empty).
 * 3. Can the graph fit in memory, or is it too massive/distributed?
 *    (Dictates whether we can build an adjacency list or if we need a memory-efficient approach).
 * 4. Are we expecting multiple queries on the same graph, or just a one-off query?
 *    (If multiple queries, Union-Find or pre-computing connected components is much better than running BFS every time).
 * 5. Do we need to return the shortest path, or just a boolean existence check?
 *    (Confirming we only need boolean allows us to use DFS or Union-Find, saving the path-tracking overhead).
 *
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * Constraint: We need to traverse nodes via edges, but graphs can contain cycles. 
 * The binding constraint is preventing infinite loops and avoiding redundant exploration 
 * of paths we've already checked.
 * 
 * --- Approach 1: Brute Force (Naïve Recursive DFS without state) ---
 * Idea: Start at `source`. Look at all connected neighbors. Recursively ask "can this 
 * neighbor reach the `destination`?" If any say yes, return true.
 * What I'd naturally try: Just follow the edges as if they were a tree.
 * Why it's too slow/costly: Graphs have cycles. 0 connected to 1, 1 connected to 0. 
 * We will endlessly bounce between 0 and 1, causing a StackOverflow. Even without 
 * strict cycles, we might explore the same sub-graph millions of times from different entry points.
 * What work is repeated: Exploring nodes we've already determined don't lead to the destination (or are already being explored).
 * Property/Observation that fixes this: A node only needs to be processed ONCE. If we 
 * can't reach the destination from node X the first time, we won't be able to the second time either.
 * 
 * Time Complexity: O(∞) — because it gets trapped in cycles, or O(V!) in acyclic dense graphs by exploring every possible permutation of paths.
 * Space Complexity: O(V) — because the recursion stack will grow as deep as the longest path, which could be all V vertices.
 * 
 * --- Approach 2: Breadth-First Search (BFS) with Visited Set ---
 * Idea: Build an adjacency list. Use a queue to explore the graph level by level. 
 * Maintain a `visited` boolean array. Before adding a neighbor to the queue, check 
 * if it's visited. If so, skip it.
 * Why it works: Cycles are naturally broken because we never process a visited node twice.
 * Why it leads to the next approach: BFS is great and optimal for time. However, 
 * building an adjacency list requires O(V + E) space. For just checking "are these 
 * two things connected?", allocating dynamic lists for every vertex is heavy on memory allocations.
 * 
 * Time Complexity: O(V + E) — because we build the graph in O(E) time, and then in the worst case, we visit every vertex once O(V) and check every edge twice O(E). This is fast enough for V=10^5.
 * Space Complexity: O(V + E) — because the adjacency list stores every edge, and the queue + visited array store up to V elements.
 * 
 * --- Approach 3: Union-Find (Disjoint Set) ---
 * Idea: Instead of traversing, think of edges as "gluing" nodes into components. 
 * We iterate through the edge list once, joining nodes. At the end, we just ask: 
 * "Are source and destination in the same glued component?"
 * Why it works: Reachability in an undirected graph is an equivalence relation 
 * (transitive, symmetric, reflexive).
 * 
 * Time Complexity: O(E * α(V)) — because we process E edges, and each Union/Find operation takes effectively O(1) amortized time (inverse Ackermann function α).
 * Space Complexity: O(V) — because we only need an array of size V to store parent pointers (and maybe rank). We completely avoid building the O(E) adjacency list!
 * 
 * Interview Decision: In a 45-minute interview, if asked purely for boolean reachability, 
 * I would write **BFS** first. It demonstrates fundamental graph traversal which interviewers 
 * love to see. If they ask to optimize space or handle multiple queries, I would pivot to **Union-Find**.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. `source == destination`: Must return true immediately, even if `edges` is empty.
 * 2. Graph is completely disconnected (0 edges) and `source != destination`: Return false.
 * 3. Linear graph (0-1-2-3-4-5) where we must traverse V-1 edges to find the answer.
 * 4. Two disjoint subgraphs (0-1-2 and 3-4-5). Source in one, dest in the other.
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "Do I actually care about the *sequence* of nodes taken to get there?"
 * Correctness intuition: If I don't care about the path, I just care if they belong 
 * to the same "island" (connected component).
 * 
 * --- Examples & ASCII ---
 * Graph:
 *   0 --- 1 --- 2
 *         |
 *         3
 *   
 *   4 --- 5
 * 
 * source = 0, destination = 3. -> TRUE.
 * source = 0, destination = 5. -> FALSE. (Isolated islands).
 * 
 * --- Dry Run (BFS on the true example above) ---
 * Init: target = 3. queue = [0], visited = {0:T, 1:F, 2:F, 3:F, 4:F, 5:F}
 * Iteration 1:
 *   - pop 0. Is 0 == 3? No.
 *   - neighbors of 0: [1].
 *   - 1 is not visited. Mark 1 visited. queue = [1].
 * Iteration 2:
 *   - pop 1. Is 1 == 3? No.
 *   - neighbors of 1: [0, 2, 3].
 *   - 0 is visited, skip.
 *   - 2 is not visited. Mark visited. queue = [2].
 *   - 3 is not visited. Mark visited. queue = [2, 3].
 * Iteration 3:
 *   - pop 2. Is 2 == 3? No.
 *   - neighbors of 2: [1]. 1 is visited. queue = [3].
 * Iteration 4:
 *   - pop 3. Is 3 == 3? YES. Return TRUE.
 * 
 * --- Pitfalls ---
 * 1. Marking a node visited *after* popping it from the queue instead of *before* pushing it. 
 *    This causes multiple identical nodes to enter the queue if multiple paths lead to them, blowing up memory to O(E).
 * 2. Building a directed adjacency list (`adj.get(u).add(v)`) but forgetting the reverse (`adj.get(v).add(u)`).
 * 
 * --- Pattern Recognition ---
 * When you see "undirected graph connectivity" or "path exists boolean", think BFS/DFS or Union-Find.
 * Transfers to: Number of Connected Components, Graph Valid Tree, Friend Circles.
 * 
 * --- Interview Script ---
 * "Since we just need to know if a path exists and don't need the path itself, this is a classic reachability problem. 
 * We can model this by building an adjacency list and running a Breadth-First Search from the source. 
 * To avoid infinite loops in cyclic graphs, we'll keep a boolean visited array. We'll mark nodes as visited 
 * right as we put them in the queue. If we pop our destination, we return true. If the queue runs dry, we return false. 
 * That gives us O(V + E) time and O(V + E) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if we needed to return the actual path?
 * A: Union-Find wouldn't work. We'd use BFS, and instead of a boolean visited array, 
 *    we'd keep a `parent` array to backtrack from `destination` to `source`.
 * Q2: What if we have millions of `validPath` queries on the SAME graph?
 * A: BFS takes O(V+E) per query. I would pre-process the graph with Union-Find or 
 *    assign component IDs using DFS. Then each query is O(1) time.
 * Q3: What if the edges are added dynamically over time, and we query intermixed?
 * A: Standard BFS fails as we'd re-run it constantly. Union-Find is perfect here, 
 *    as we can lazily unite components and answer connectivity queries in nearly O(1) time.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Graph Traversal / Connected Components.
 * Key observation: Reachability doesn't require knowing the path.
 * Memorize: The BFS template (Queue + Visited Set) and Union-Find template.
 * Most common trap: Forgetting to mark visited *before* enqueueing.
 * Mental trigger: "Valid path exists?" -> BFS / Union-Find.
 */

import java.util.*;

public class ValidPathGraph {

    /**
     * APPROACH 2: Breadth-First Search (BFS)
     */
    public boolean validPathBFS(int n, int[][] edges, int source, int destination) {
        // Base case / Early exit: If we're already there, cost is 0.
        if (source == destination) return true;

        // 1. Build the Adjacency List
        // Example on small instance (edges = [[0,1], [1,2]]):
        // adj = [ [1], [0,2], [1] ]
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        
        // Populate undirected edges
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            adj.get(u).add(v);
            adj.get(v).add(u); // Must be bidirectional!
        }

        // 2. Queue for BFS and Visited array to prevent cycles
        // visited: index represents node ID. true means we've seen it.
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        // 3. Initialize start state
        queue.offer(source);
        visited[source] = true; // Mark visited IMMEDIATELY to prevent redundant enqueues

        // 4. Traverse
        while (!queue.isEmpty()) {
            // Retrieve and remove the head of the queue
            int curr = queue.poll();

            // If we've reached our destination, we're done.
            if (curr == destination) {
                return true;
            }

            // Iterate over all connected neighbors
            for (int neighbor : adj.get(curr)) {
                // Only process unvisited neighbors
                if (!visited[neighbor]) {
                    visited[neighbor] = true; // Mark BEFORE enqueuing (crucial pitfall)
                    queue.offer(neighbor);
                }
            }
        }

        // If queue depletes and we never hit destination, no path exists.
        return false;
    }

    /**
     * APPROACH 3: Union-Find (Disjoint Set)
     */
    public boolean validPathUF(int n, int[][] edges, int source, int destination) {
        if (source == destination) return true;

        // parent array: holds the representative/root of the component a node belongs to.
        // Example: If node 2 and 3 are connected, and 0 is their root, parent[2] = 0, parent[3] = 0.
        int[] parent = new int[n];
        
        // Initially, every node is its own parent (n disjoint sets)
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        // Process each edge and union their sets
        for (int[] edge : edges) {
            union(parent, edge[0], edge[1]);
        }

        // If source and destination share the same root, they are connected!
        return find(parent, source) == find(parent, destination);
    }

    // Helper: Finds the root representative of node 'i' with Path Compression
    private int find(int[] parent, int i) {
        if (parent[i] == i) {
            return i;
        }
        // Path compression: flatten the tree by pointing directly to the root
        return parent[i] = find(parent, parent[i]); 
    }

    // Helper: Unites the sets containing 'u' and 'v'
    private void union(int[] parent, int u, int v) {
        int rootU = find(parent, u);
        int rootV = find(parent, v);
        
        // If they have different roots, connect them by making one root point to the other
        if (rootU != rootV) {
            parent[rootU] = rootV;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        ValidPathGraph solution = new ValidPathGraph();

        // Test Case 1: Standard Connected Graph (Matches ASCII example)
        // 0 - 1 - 2, 1 - 3
        int n1 = 4;
        int[][] edges1 = {{0, 1}, {1, 2}, {1, 3}};
        System.out.println("Test 1 BFS: " + solution.validPathBFS(n1, edges1, 0, 3)); // Expected: true
        System.out.println("Test 1 UF:  " + solution.validPathUF(n1, edges1, 0, 3));  // Expected: true

        // Test Case 2: Disconnected Subgraphs
        // 0 - 1, 2 - 3 (No path from 0 to 3)
        int n2 = 4;
        int[][] edges2 = {{0, 1}, {2, 3}};
        System.out.println("Test 2 BFS: " + solution.validPathBFS(n2, edges2, 0, 3)); // Expected: false
        System.out.println("Test 2 UF:  " + solution.validPathUF(n2, edges2, 0, 3));  // Expected: false

        // Test Case 3: Edge Case - Same Source and Destination (0 edges)
        int n3 = 1;
        int[][] edges3 = {};
        System.out.println("Test 3 BFS: " + solution.validPathBFS(n3, edges3, 0, 0)); // Expected: true
        
        // Test Case 4: Edge Case - Disconnected Graph entirely, source != destination
        int n4 = 2;
        int[][] edges4 = {};
        System.out.println("Test 4 UF:  " + solution.validPathUF(n4, edges4, 0, 1));  // Expected: false
    }
}

