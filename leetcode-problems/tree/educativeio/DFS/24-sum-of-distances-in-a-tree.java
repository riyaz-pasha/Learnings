import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ============================================================================
 * STUDY NOTE: SUM OF DISTANCES IN TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Can the total sum of distances for a node exceed the 32-bit integer limit?"
 *   (Crucial mathematical check: Max N is 30,000. In the worst case—a straight line—the maximum 
 *   distance sum is roughly N^2 / 2, which is (30,000)^2 / 2 = 450,000,000. This safely fits 
 *   inside a signed 32-bit int (limit 2.14 billion). We don't need longs.)
 * - "Can N be 1, and if so, how should the empty edges array be handled?"
 *   (Confirms edge case behavior: a tree of 1 node has 0 edges and the distance to all 'other' nodes is 0.)
 * - "Is the input strictly guaranteed to be a single connected component (a valid tree)?"
 *   (Eliminates the need for cyclic checks or tracking unvisited disjoint subgraphs.)
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We need the total distance from EVERY node to all other nodes. 
 * The bottleneck is finding a way to calculate this globally without triggering an isolated, 
 * full-depth traversal for every single node.
 *
 * Approach 1: BFS/DFS from Every Node (The Naive Brute Force)
 * What I'd naturally try: We know how to find the shortest path in an unweighted graph using BFS. 
 * I will loop from 0 to N-1. For each node 'i', I'll run a complete BFS to calculate its distance 
 * to all other N-1 nodes, sum them up, and store it in ans[i].
 * Why it works: BFS inherently explores level-by-level, cleanly accumulating distances.
 * Why it's too slow/costly: We repeat the exact same edge traversals from slightly different 
 * perspectives thousands of times.
 * - Time Complexity: O(N^2) — Running an O(N) BFS exactly N times. For N = 30,000, this is ~900 
 *   million operations, which will instantly trigger a Time Limit Exceeded (TLE) error.
 * - Space Complexity: O(N) — for the adjacency list and the BFS queue.
 *
 * Approach 2: Tree DP / "Rerooting" Technique (The Optimal Masterpiece)
 * What I'd naturally try: Instead of calculating from scratch, how does the answer change if I 
 * just step to an adjacent node? If I know the sum of distances for a Parent node, and I move 
 * the "root" of my perspective to its Child node, what exactly shifts in the math?
 * 
 * The Mathematical Insight:
 * When you step from a Parent to a Child:
 *   1. You get 1 step CLOSER to all nodes residing in the Child's subtree.
 *   2. You get 1 step FARTHER away from all other nodes in the entire tree.
 * 
 * If we know `count[Child]` (the number of nodes in the Child's subtree, including the child itself):
 *   - The distance sum DECREASES by `count[Child]`.
 *   - The distance sum INCREASES by `N - count[Child]`.
 *   - Formula: `ans[Child] = ans[Parent] - count[Child] + (N - count[Child])`
 *
 * How to execute this:
 * Pass 1 (Bottom-Up Post-Order DFS): We plant the tree arbitrarily at Node 0. We recursively 
 * pull up data to calculate `count[i]` for all nodes, AND we calculate the true `ans[0]` (the 
 * sum of distances if Node 0 is the root).
 * Pass 2 (Top-Down Pre-Order DFS): Starting from Node 0 (where we know the exact correct answer), 
 * we push the answer down to its children using our O(1) rerooting formula.
 *
 * - Time Complexity: O(N) — because we only perform two DFS traversals over the tree's N nodes.
 * - Space Complexity: O(N) — because we store the adjacency list, the `count` array, the `ans` array, 
 *   and use O(N) recursion stack memory.
 *
 * The Interview Choice:
 * Always write Approach 2. The "Rerooting" dynamic programming pattern on trees is an elite 
 * algorithmic concept. Writing it cleanly demonstrates extreme proficiency with graph math and 
 * two-pass DFS architectures.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Single Node (N = 1) -> No edges. Must safely return [0] without crashing on empty arrays.
 * 2. Star Graph (One center, many leaves) -> Tests shallow depth but massive breadth. 
 *    The rerooting formula easily identifies that moving to a leaf increases distance heavily.
 * 3. Line Graph / Skewed Tree -> Tests the worst-case recursion stack depth (Stack space O(N)).
 *
 *
 * 4. DRY RUN (Optimal Rerooting DP)
 * ----------------------------------------------------------------------------
 * Tree: 
 * N = 4, Edges = [[0,1], [0,2], [2,3]]
 * Structure: 1 - 0 - 2 - 3
 *
 * Pass 1 (Bottom-Up Post-Order DFS starting at 0):
 * - dfs(1): No children. count[1] = 1. ans[1] (local) = 0.
 * - dfs(3): No children. count[3] = 1. ans[3] (local) = 0.
 * - dfs(2): One child (3). 
 *           count[2] = 1 + count[3] = 2.
 *           ans[2] = ans[3] + count[3] = 1.
 * - dfs(0): Children (1, 2).
 *           count[0] = 1 + count[1] + count[2] = 1 + 1 + 2 = 4.
 *           ans[0] = (ans[1] + count[1]) + (ans[2] + count[2]) = (0 + 1) + (1 + 2) = 4.
 * End of Pass 1: count = [4, 1, 2, 1]. ans[0] = 4.
 *
 * Pass 2 (Top-Down Pre-Order DFS pushing from 0):
 * - We know ans[0] = 4.
 * - To Node 1: ans[1] = ans[0] - count[1] + (N - count[1]) 
 *                     = 4 - 1 + (4 - 1) = 6.
 * - To Node 2: ans[2] = ans[0] - count[2] + (N - count[2])
 *                     = 4 - 2 + (4 - 2) = 4.
 *   - From Node 2 to Node 3: ans[3] = ans[2] - count[3] + (N - count[3])
 *                                   = 4 - 1 + (4 - 1) = 6.
 * End of Pass 2: ans = [4, 6, 4, 6].
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the edges had weights (e.g., distances aren't just 1)?
 * A1: The rerooting formula adapts easily. If the edge weight between Parent and Child is W:
 *     Instead of getting 1 step closer/farther, we get W steps closer/farther.
 *     Formula: `ans[Child] = ans[Parent] - (count[Child] * W) + ((N - count[Child]) * W)`.
 *
 * Q2: What if the graph contained cycles (i.e., wasn't a tree)?
 * A2: Rerooting relies fundamentally on the property that cutting any edge splits a tree into 
 *     two disjoint components (a child's subtree and the rest of the tree). Cycles break this. 
 *     For general graphs, we would have to use algorithms like Floyd-Warshall O(V^3) or 
 *     Dijkstra from every node O(V * E log V).
 */

public class SumOfDistancesInTreeStudy {

    // Arrays to hold our vital state across both DFS passes
    private int[] answer;
    private int[] count;
    private List<List<Integer>> graph;

    /**
     * APPROACH 2: TREE DP / REROOTING (The Optimal Solution)
     * Time: O(N) | Space: O(N)
     */
    public int[] sumOfDistancesInTree(int n, int[][] edges) {
        answer = new int[n];
        count = new int[n];
        graph = new ArrayList<>();

        // Initialize adjacency list
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Populate adjacency list for an undirected tree
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // Pass 1: Bottom-up Post-Order DFS
        // We arbitrarily root the tree at node 0.
        // This pass computes the subtree node counts and calculates the true answer for node 0.
        postOrderDfs(0, -1);

        // Pass 2: Top-down Pre-Order DFS
        // We use the computed answer of node 0 to derive the answers for all other nodes in O(1) steps.
        preOrderDfs(0, -1, n);

        return answer;
    }

    /**
     * Helper for Pass 1: Post-Order (Process children first, then bubble up to parent)
     *
     * @param node   The current node we are processing.
     * @param parent The node we came from (to prevent infinite looping backwards).
     */
    private void postOrderDfs(int node, int parent) {
        // A node's subtree count inherently includes itself
        count[node] = 1;

        for (int child : graph.get(node)) {
            if (child == parent) continue; // Don't walk back up the tree

            // Ask the child to calculate its subtree count and internal distances first
            postOrderDfs(child, node);

            // Once the child is processed, bubble the state up to the parent
            count[node] += count[child];
            
            // The distance from the parent to all nodes in the child's subtree is:
            // The internal distances inside the child's subtree (answer[child])
            // PLUS 1 step for every node in the child's subtree (count[child]) to cross the parent-child edge.
            answer[node] += answer[child] + count[child];
        }
    }

    /**
     * Helper for Pass 2: Pre-Order (Use the parent's fully baked answer to calculate the child's)
     *
     * @param node   The current node (acts as the parent offering data).
     * @param parent The node we came from.
     * @param n      Total number of nodes in the tree.
     */
    private void preOrderDfs(int node, int parent, int n) {
        for (int child : graph.get(node)) {
            if (child == parent) continue;

            // REROOTING FORMULA:
            // We shift our perspective (root) from 'node' down to 'child'.
            // Nodes in the child's subtree get 1 step closer (- count[child]).
            // All other nodes in the tree get 1 step farther (+ (n - count[child])).
            answer[child] = answer[node] - count[child] + (n - count[child]);

            // Now that the child has its true global answer, it acts as the parent for its descendants
            preOrderDfs(child, node, n);
        }
    }


    /**
     * APPROACH 1: BRUTE FORCE USING BFS
     * Time: O(N^2) | Space: O(N)
     * Included strictly to demonstrate the raw logic we are optimizing.
     * (Do not use this for N = 30,000 as it will TLE).
     */
    public int[] sumOfDistancesBruteForce(int n, int[][] edges) {
        List<List<Integer>> localGraph = new ArrayList<>();
        for (int i = 0; i < n; i++) localGraph.add(new ArrayList<>());
        for (int[] e : edges) {
            localGraph.get(e[0]).add(e[1]);
            localGraph.get(e[1]).add(e[0]);
        }

        int[] bruteAnswer = new int[n];

        // Run a full BFS for every single node in the tree
        for (int i = 0; i < n; i++) {
            int totalDist = 0;
            Queue<int[]> queue = new LinkedList<>(); // {currentNode, currentDistance}
            boolean[] visited = new boolean[n];
            
            queue.offer(new int[]{i, 0});
            visited[i] = true;

            while (!queue.isEmpty()) {
                int[] curr = queue.poll();
                int u = curr[0];
                int dist = curr[1];
                
                totalDist += dist;

                for (int neighbor : localGraph.get(u)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        queue.offer(new int[]{neighbor, dist + 1});
                    }
                }
            }
            bruteAnswer[i] = totalDist;
        }
        
        return bruteAnswer;
    }


    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        SumOfDistancesInTreeStudy study = new SumOfDistancesInTreeStudy();

        // Standard Case (From Dry Run):
        // 1 - 0 - 2 - 3
        int n1 = 4;
        int[][] edges1 = {{0, 1}, {0, 2}, {2, 3}};
        System.out.println("Standard Line-ish Graph:");
        System.out.println("Optimal:     " + Arrays.toString(study.sumOfDistancesInTree(n1, edges1))); // Expected: [4, 6, 4, 6]
        System.out.println("Brute Force: " + Arrays.toString(study.sumOfDistancesBruteForce(n1, edges1)));
        
        System.out.println("\n--------------------------------------------------\n");

        // Edge Case 1: Star Graph
        // Node 0 is the center, 1 to 4 are leaves
        int n2 = 5;
        int[][] edges2 = {{0, 1}, {0, 2}, {0, 3}, {0, 4}};
        System.out.println("Star Graph:");
        System.out.println("Optimal:     " + Arrays.toString(study.sumOfDistancesInTree(n2, edges2))); // Expected: [4, 7, 7, 7, 7]
        System.out.println("Brute Force: " + Arrays.toString(study.sumOfDistancesBruteForce(n2, edges2)));

        System.out.println("\n--------------------------------------------------\n");

        // Edge Case 2: Single Node
        int n3 = 1;
        int[][] edges3 = {};
        System.out.println("Single Node Graph:");
        System.out.println("Optimal:     " + Arrays.toString(study.sumOfDistancesInTree(n3, edges3))); // Expected: [0]
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Rerooting dynamic programming on trees (Two-Pass DFS).
 * Key Observation: Moving one step down a tree brings you exactly 1 step closer to 
 *                  everything in the child's subtree, and 1 step further from everything else.
 * Memorize: `ans[child] = ans[parent] - count[child] + (N - count[child])`.
 * Most Common Trap: Trying to run a traversal for every single node ($O(N^2)$), 
 *                   which fails constraints heavily. 
 * One-Line Mental Trigger: "Sum of distances from all nodes? Calculate for root, then push down via rerooting math."
 * ============================================================================
 */
