/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * There are n houses. We must supply water to all of them. 
 * We can either build a well at a house (cost in `wells` array) or lay pipes 
 * between houses (cost in `pipes` array). Return the minimum total cost to 
 * ensure every house has water.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can a house just rely on its own well and not connect to any pipes?
 *    (Yes. If pipes are insanely expensive, building N independent wells is valid).
 * 2. Are there any isolated houses with no pipes connecting to them?
 *    (Yes, and for those, we are forced to build a well).
 * 3. Multiple pipes can exist between the same pair of houses. How do we handle this?
 *    (We only care about the cheapest pipe connecting them. Our sorting strategy should naturally discard the expensive duplicates).
 * 4. Is the graph guaranteed to be fully connectable?
 *    (Yes, because in the worst-case scenario, we can always just build a well at every single house).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: Every house needs water, but water can originate from 
 * MULTIPLE sources (wells) and spread. Most graph algorithms (like standard Dijkstra 
 * or minimum spanning tree) assume a single connected component or a single source. 
 * Here, we don't know *how many* wells we should build.
 * 
 * --- Approach 1: Brute Force (Subset of Wells) ---
 * Idea: Try every possible combination of well placements. For $N$ houses, there are 
 * $2^N$ possible ways to place wells. For each combination, check if we can connect 
 * the remaining houses via pipes, and calculate the cost. Keep the minimum.
 * What I'd naturally try: Backtracking to generate subsets, then Kruskal's for the rest.
 * Why it's too slow/costly: Exponential time complexity makes it instantly fail for $N=10000$.
 * What work is repeated: We calculate spanning trees for nearly identical graph states millions of times.
 * Property that fixes this: We need an algorithm that can *dynamically* choose between 
 * a well and a pipe based purely on cost, in a single pass.
 * 
 * Time Complexity: O(2^N * E \log V) — because we run an MST algorithm for every subset of wells.
 * Space Complexity: O(V + E) — to store the graph.
 * 
 * --- Approach 2: Multi-Source Prim's Algorithm ---
 * Idea: Start with a Priority Queue. Push ALL well costs into the PQ. This is like 
 * saying "I can start my tree at any house, and the cost to enter the graph there is the well cost." 
 * Pull the cheapest option. If it's a new house, mark it watered, and add all its pipes to the PQ.
 * Why it works: It organically grows regions of water, merging them when a pipe is cheaper 
 * than building a new well. 
 * 
 * Time Complexity: O((V + E) \log(V + E)) — because we process up to V wells and E pipes through a Priority Queue.
 * Space Complexity: O(V + E) — because we build an adjacency list and store edges in the PQ.
 * 
 * --- Approach 3: The "Dummy Node" + Kruskal's MST (Optimal & Most Elegant) ---
 * Idea: How do we turn this weird "node cost" (wells) vs "edge cost" (pipes) problem 
 * into a standard textbook problem? 
 * Add a magical "Dummy Node" (Node 0) representing the underground water aquifer. 
 * Building a well at House 1 is EXACTLY the same as laying a pipe from Node 0 to Node 1!
 * Suddenly, the wells aren't special node properties anymore. They are just regular edges 
 * connected to Node 0. 
 * Now, the problem perfectly reduces to: "Find the Minimum Spanning Tree (MST) of this 
 * new graph with N+1 nodes."
 * 
 * Time Complexity: O((V + E) \log(V + E)) — because we collect all $N$ wells and $E$ pipes into a single array and sort it. Sorting dominates the time. The Union-Find operations take amortized O(1).
 * Space Complexity: O(V + E) — because we store all edges in a list to sort them, and use an array of size $V+1$ for the Union-Find parent pointers.
 * 
 * Interview Decision: I would write **Approach 3 (Dummy Node + Kruskal's)**. 
 * It is a brilliant mathematical reduction. By adding one dummy node, we delete hundreds 
 * of lines of custom multi-source logic and replace it with a standard 10-line Kruskal's template.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. Wells are cheaper than all pipes: MST will naturally pick all $N$ edges connecting to Node 0.
 * 2. One well is cheap, all other wells are exorbitant, pipes are cheap: MST picks the 1 cheap edge to Node 0, then uses pipes for the rest.
 * 3. Duplicate pipes with different costs: E.g., [1, 2, 50] and [1, 2, 10]. Sorting handles this automatically. The cost-10 pipe is processed first and unions the nodes. When the cost-50 pipe is reached, the nodes are already connected, so it's skipped.
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "How can I translate a 'node cost' into an 'edge cost'?"
 * Correctness intuition: If every house must connect to water, and water comes from a 
 * universal source, then every valid configuration is just a tree connecting houses to 
 * that universal source. This is the exact definition of a Minimum Spanning Tree.
 * 
 * --- Examples & ASCII ---
 * Houses: 1, 2. 
 * Wells: [1, 100]. (Well at 1 costs 1, well at 2 costs 100).
 * Pipes: [[1, 2, 10]].
 * 
 * Before Dummy Node:
 *   [House 1] <--- cost 10 ---> [House 2]
 *   (well: 1)                   (well: 100)
 * 
 * After Dummy Node (Node 0):
 *           [Node 0 - Aquifer]
 *             /             \
 *       cost 1               cost 100
 *           /                 \
 *       [House 1] --cost 10-- [House 2]
 * 
 * MST logic: 
 * 1. Sort edges: [0-1: 1], [1-2: 10], [0-2: 100].
 * 2. Pick 0-1 (cost 1). Nodes {0, 1} connected.
 * 3. Pick 1-2 (cost 10). Nodes {0, 1, 2} connected.
 * 4. Total Cost = 11. (We saved 89 by not building well 2!)
 * 
 * --- Dry Run (Kruskal's) ---
 * n = 3. wells = [1, 2, 2]. pipes = [[1, 2, 1], [2, 3, 1]]
 * Edges generated:
 * Wells: [0,1,cost=1], [0,2,cost=2], [0,3,cost=2]
 * Pipes: [1,2,cost=1], [2,3,cost=1]
 * 
 * Sorted edges:
 * 1. [0,1, 1]
 * 2. [1,2, 1]
 * 3. [2,3, 1]
 * 4. [0,2, 2]
 * 5. [0,3, 2]
 * 
 * Process:
 * - pop [0,1, 1]: union(0,1). Cost = 1.
 * - pop [1,2, 1]: union(1,2). Cost = 1 + 1 = 2.
 * - pop [2,3, 1]: union(2,3). Cost = 2 + 1 = 3.
 * - Need exactly N=3 edges. We have 3 edges! Break.
 * Return 3.
 * 
 * --- Pitfalls ---
 * 1. 0-Indexed vs 1-Indexed arrays. `wells` is 0-indexed (wells[0] is house 1). `pipes` use 1-indexed house numbers. You must align them carefully.
 * 2. Forgetting that a Minimum Spanning Tree of N+1 nodes (Dummy + N houses) requires exactly N edges.
 * 
 * --- Pattern Recognition ---
 * When you see: "Give every node X", "You can generate X at a node or pass it via edges".
 * Think: Dummy Node + Minimum Spanning Tree.
 * Transfers to: Minimum Cost to Connect Sticks (similar greedy logic), Network Delay Time.
 * 
 * --- Interview Script ---
 * "This looks like a graph connectivity problem, but the choice between building a well at a node 
 * versus laying a pipe makes standard MST tricky. To unify them, I'll introduce a virtual 'Dummy Node' 0 
 * representing the ultimate water source. Building a well at house 'i' is mathematically identical to 
 * laying a pipe from Node 0 to house 'i'. Now, we just have N+1 nodes and a list of edges. 
 * I can collect all well-edges and pipe-edges into a single list, sort them by cost, and run Kruskal's 
 * Algorithm using a Disjoint Set. The moment all houses are connected to the dummy node's component, 
 * we have our minimum cost. This runs in O(E log E) time and O(V + E) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the graph was incredibly dense (almost every house has a pipe to every other house)?
 * A: Kruskal's takes O(E log E) due to sorting. If E is roughly V^2, sorting takes O(V^2 log V). 
 *    In a strictly dense graph, Prim's Algorithm using an Adjacency Matrix runs in O(V^2) time, 
 *    which would actually be faster than Kruskal's.
 * Q2: What if we wanted to minimize the MAX cost of any single pipe/well we build, rather than the sum?
 * A: Kruskal's naturally solves the "Minimax Path" problem. The very last edge added to complete the 
 *    MST is guaranteed to be the minimum possible bottleneck edge to connect the graph!
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Graph Reduction via Dummy Node + Kruskal's MST.
 * Key observation: A well is just an edge to an invisible universal water source.
 * Memorize: `edges.add(new int[]{0, i, wells[i - 1]});`
 * Most common trap: Array indexing mismatches between the 0-indexed wells array and 1-indexed pipes.
 * Mental trigger: "Cost to supply independently OR cost to connect" -> Dummy Node MST.
 */

import java.util.*;

public class WaterSupply {

    public int minCostToSupplyWater(int n, int[] wells, int[][] pipes) {
        // 1. Create a unified list of all possible "edges"
        // An edge is represented as: {nodeA, nodeB, cost}
        List<int[]> edges = new ArrayList<>();
        
        // 2. Add the "Well" edges. 
        // We use Node 0 as the universal dummy water source.
        for (int i = 1; i <= n; i++) {
            // wells array is 0-indexed, so house i's well cost is at i-1.
            // Example: edge from 0 to 1 with cost wells[0]
            edges.add(new int[]{0, i, wells[i - 1]});
        }
        
        // 3. Add the regular pipe edges.
        for (int[] pipe : pipes) {
            edges.add(pipe);
        }
        
        // 4. Sort all edges ascending by cost (Greedy approach for Kruskal's)
        edges.sort((a, b) -> Integer.compare(a[2], b[2]));
        
        // 5. Initialize Union-Find for N + 1 nodes (Node 0 to Node N)
        UnionFind uf = new UnionFind(n + 1);
        
        int totalMinCost = 0;
        int edgesAdded = 0;
        
        // 6. Process edges from cheapest to most expensive
        for (int[] edge : edges) {
            int house1 = edge[0];
            int house2 = edge[1];
            int cost = edge[2];
            
            // If union returns true, it means house1 and house2 were NOT previously connected.
            // Connecting them does not form a cycle, so we add this edge to our MST.
            if (uf.union(house1, house2)) {
                totalMinCost += cost;
                edgesAdded++;
                
                // Early exit optimization: 
                // A spanning tree for N+1 nodes requires exactly N edges.
                if (edgesAdded == n) {
                    break;
                }
            }
        }
        
        return totalMinCost;
    }
    
    /**
     * Standard Disjoint Set (Union-Find) helper class.
     * Manages connectivity between components.
     */
    private class UnionFind {
        private int[] parent;
        
        public UnionFind(int n) {
            parent = new int[n];
            // Initially, every node is its own root
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }
        
        // Find the root of node 'i' using Path Compression
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Compress the path by pointing directly to the ultimate root
            return parent[i] = find(parent[i]); 
        }
        
        // Unites two sets. Returns true if they were successfully merged,
        // false if they were already in the same set (which would form a cycle).
        public boolean union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
                return true;
            }
            return false;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        WaterSupply solution = new WaterSupply();

        // Test Case 1: Standard case. (Matches Dry Run in comments)
        // Wells at 1, 2, 3 cost [1, 2, 2]
        // Pipes between 1-2 (cost 1), 2-3 (cost 1)
        // Optimal: Build well at 1 (cost 1), pipe 1-2 (cost 1), pipe 2-3 (cost 1) -> Total 3.
        int n1 = 3;
        int[] wells1 = {1, 2, 2};
        int[][] pipes1 = {{1, 2, 1}, {2, 3, 1}};
        System.out.println("Test 1 Result: " + solution.minCostToSupplyWater(n1, wells1, pipes1)); // Expected: 3

        // Test Case 2: Wells are cheaper than all pipes
        // Wells cost [1, 1]. Pipe costs 10.
        // Optimal: Build wells at both houses. Total 2.
        int n2 = 2;
        int[] wells2 = {1, 1};
        int[][] pipes2 = {{1, 2, 10}};
        System.out.println("Test 2 Result: " + solution.minCostToSupplyWater(n2, wells2, pipes2)); // Expected: 2

        // Test Case 3: Duplicate pipes with different costs
        // Pipe 1-2 has cost 10 and cost 2. 
        // Optimal: Well at 1 (cost 1), pipe 1-2 (cost 2). Total 3.
        int n3 = 2;
        int[] wells3 = {1, 100};
        int[][] pipes3 = {{1, 2, 10}, {1, 2, 2}};
        System.out.println("Test 3 Result: " + solution.minCostToSupplyWater(n3, wells3, pipes3)); // Expected: 3
    }
}



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaterDistribution {

    // Each edge represents one available construction option:
    // either a well (from virtual node 0) or a pipe (between houses).
    private record Edge(int from, int to, int cost) {}

    // Disjoint Set Union (Union-Find) efficiently tracks which houses
    // belong to the same connected component of our growing MST.
    private static final class DSU {
        private final int[] parent;
        private final int[] size;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];

            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        // Find the representative of x's component.
        // Path compression makes future find operations faster by
        // connecting visited nodes directly to the representative.
        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        // Merge two different components.
        // Union by size attaches the smaller component to the larger one.
        // Returns true if a merge happened, false if already connected.
        boolean union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);

            // An edge within the same component would create a cycle,
            // so Kruskal must reject it.
            if (rootA == rootB) {
                return false;
            }

            if (size[rootA] < size[rootB]) {
                int temp = rootA;
                rootA = rootB;
                rootB = temp;
            }

            parent[rootB] = rootA;
            size[rootA] += size[rootB];

            return true;
        }
    }

    public static long minCostToSupplyWater(
            int n, int[] wells, int[][] pipes) {

        // There are n real houses numbered 1..n and one virtual
        // water-source node numbered 0.
        //
        // The virtual node converts the well-building decision into
        // an ordinary graph edge:
        // (0, i, wells[i - 1]) means "build a well at house i".
        //
        // A pipe [u, v, cost] remains the edge (u, v, cost).
        // Therefore, choosing edges in an MST chooses wells and pipes
        // together, minimizing their combined construction cost.
        List<Edge> edges = new ArrayList<>(n + pipes.length);

        for (int house = 1; house <= n; house++) {
            edges.add(new Edge(0, house, wells[house - 1]));
        }

        for (int[] pipe : pipes) {
            edges.add(new Edge(pipe[0], pipe[1], pipe[2]));
        }

        // Kruskal considers the cheapest available option first.
        // Duplicate pipes are safe: each entry is simply another edge.
        edges.sort(Comparator.comparingInt(Edge::cost));

        // There are n + 1 vertices, so a spanning tree needs exactly n
        // accepted edges. DSU initially treats every vertex separately.
        DSU dsu = new DSU(n + 1);
        long totalCost = 0;
        int edgesUsed = 0;

        for (Edge edge : edges) {

            // Accept only edges joining different components.
            // This prevents cycles and maintains a valid forest.
            if (dsu.union(edge.from(), edge.to())) {
                totalCost += edge.cost();
                edgesUsed++;

                // All vertices are connected once n edges are accepted.
                // The virtual source is connected to every real house,
                // directly or through a path, so every house has water.
                if (edgesUsed == n) {
                    return totalCost;
                }
            }
        }

        // This cannot happen for valid input: every house has a
        // virtual-source well edge, so the augmented graph is connected.
        throw new IllegalStateException("All houses should be connectable");
    }

    public static void main(String[] args) {
        int n = 3;
        int[] wells = {5, 8, 6};

        int[][] pipes = {
            {1, 2, 2},
            {2, 3, 3},
            {1, 3, 10}
        };

        System.out.println(minCostToSupplyWater(n, wells, pipes));
        // Output: 10
    }
}

