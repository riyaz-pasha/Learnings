import java.util.*;
/*
 * There is an undirected graph with n nodes, where each node is numbered
 * between 0 and n - 1. You are given a 2D array graph, where graph[u] is an
 * array of nodes that node u is adjacent to. More formally, for each v in
 * graph[u], there is an undirected edge between node u and node v. The graph
 * has the following properties:
 * 
 * There are no self-edges (graph[u] does not contain u).
 * There are no parallel edges (graph[u] does not contain duplicate values).
 * If v is in graph[u], then u is in graph[v] (the graph is undirected).
 * The graph may not be connected, meaning there may be two nodes u and v such
 * that there is no path between them.
 * A graph is bipartite if the nodes can be partitioned into two independent
 * sets A and B such that every edge in the graph connects a node in set A and a
 * node in set B.
 * 
 * Return true if and only if it is bipartite.
 * 
 */

class Solution {

    /**
     * Approach 1: BFS with Coloring
     * Time: O(V + E), Space: O(V)
     */
    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] colors = new int[n]; // 0 = uncolored, 1 = red, -1 = blue

        // Check each component (graph might not be connected)
        for (int i = 0; i < n; i++) {
            if (colors[i] == 0) { // Unvisited node
                if (!bfsCheck(graph, i, colors)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean bfsCheck(int[][] graph, int start, int[] colors) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);
        colors[start] = 1; // Start with color 1

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int neighbor : graph[node]) {
                if (colors[neighbor] == 0) {
                    // Uncolored neighbor - color with opposite color
                    colors[neighbor] = -colors[node];
                    queue.offer(neighbor);
                } else if (colors[neighbor] == colors[node]) {
                    // Same color as current node - not bipartite
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Approach 2: DFS with Coloring
     * Time: O(V + E), Space: O(V) for recursion stack
     */
    public boolean isBipartiteV2(int[][] graph) {
        int n = graph.length;
        int[] colors = new int[n]; // 0 = uncolored, 1 = red, -1 = blue

        // Check each component
        for (int i = 0; i < n; i++) {
            if (colors[i] == 0) {
                if (!dfsCheck(graph, i, 1, colors)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean dfsCheck(int[][] graph, int node, int color, int[] colors) {
        colors[node] = color;

        for (int neighbor : graph[node]) {
            if (colors[neighbor] == 0) {
                // Uncolored neighbor - try to color with opposite color
                if (!dfsCheck(graph, neighbor, -color, colors)) {
                    return false;
                }
            } else if (colors[neighbor] == color) {
                // Same color conflict
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 3: Union-Find approach
     * Time: O(V + E * α(V)), Space: O(V)
     * Note: More complex but demonstrates alternative thinking
     */
    public boolean isBipartiteV3(int[][] graph) {
        int n = graph.length;
        UnionFind uf = new UnionFind(2 * n); // Each node has two versions: original and "opposite"

        for (int u = 0; u < n; u++) {
            for (int v : graph[u]) {
                // If u and v are in same set, or u_opposite and v_opposite are in same set
                if (uf.connected(u, v) || uf.connected(u + n, v + n)) {
                    return false;
                }

                // Union u with v_opposite, and u_opposite with v
                uf.union(u, v + n);
                uf.union(u + n, v);
            }
        }

        return true;
    }

    class UnionFind {
        private int[] parent;
        private int[] rank;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int px = find(x), py = find(y);
            if (px == py)
                return;

            if (rank[px] < rank[py]) {
                parent[px] = py;
            } else if (rank[px] > rank[py]) {
                parent[py] = px;
            } else {
                parent[py] = px;
                rank[px]++;
            }
        }

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }
    }

    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: [[1,2,3],[0,2],[0,1,3],[0,2]] - Not bipartite
        // Forms a cycle of odd length (0-1-2-0), impossible to 2-color
        int[][] graph1 = { { 1, 2, 3 }, { 0, 2 }, { 0, 1, 3 }, { 0, 2 } };
        System.out.println("Test 1 (should be false): " + sol.isBipartite(graph1));

        // Test case 2: [[1,3],[0,2],[1,3],[0,2]] - Bipartite
        // Can color as: {0,2} = red, {1,3} = blue
        int[][] graph2 = { { 1, 3 }, { 0, 2 }, { 1, 3 }, { 0, 2 } };
        System.out.println("Test 2 (should be true): " + sol.isBipartite(graph2));

        // Test case 3:
        // [[],[2,4,6],[1,4,8,9],[7,8],[1,2,8,9],[6,9],[1,5,7,8,9],[3,6,9],[2,3,4,6,9],[2,4,5,6,7,8]]
        // Complex graph - bipartite
        int[][] graph3 = { {}, { 2, 4, 6 }, { 1, 4, 8, 9 }, { 7, 8 }, { 1, 2, 8, 9 }, { 6, 9 }, { 1, 5, 7, 8, 9 },
                { 3, 6, 9 }, { 2, 3, 4, 6, 9 }, { 2, 4, 5, 6, 7, 8 } };
        System.out.println("Test 3: " + sol.isBipartite(graph3));

        // Test case 4: Empty components
        int[][] graph4 = { { 1 }, { 0 }, { 3 }, { 2 }, {} };
        System.out.println("Test 4 (should be true): " + sol.isBipartite(graph4));
    }
}

/**
 * Algorithm Explanation:
 * 
 * A graph is bipartite if and only if it contains no odd-length cycles.
 * We can check this by trying to 2-color the graph.
 * 
 * BFS/DFS Approach:
 * 1. Try to color each node with one of two colors (1 or -1)
 * 2. For each unvisited node, start BFS/DFS and color it
 * 3. Color all neighbors with the opposite color
 * 4. If we encounter a neighbor with the same color, return false
 * 5. Handle disconnected components by checking all nodes
 * 
 * Key Points:
 * - Use colors array: 0 = unvisited, 1 = color1, -1 = color2
 * - Graph might be disconnected, so check all components
 * - If any component fails 2-coloring, entire graph is not bipartite
 * 
 * Time Complexity: O(V + E) - visit each node and edge once
 * Space Complexity: O(V) - for colors array and BFS queue/DFS stack
 * 
 * Union-Find Approach:
 * - Create 2n nodes: original nodes and their "opposites"
 * - For each edge (u,v), union u with v_opposite and u_opposite with v
 * - If u and v end up in same component, not bipartite
 */

 /*
 * UNION-FIND APPROACH FOR BIPARTITE GRAPH DETECTION - COMPLETE TRACING
 * 
 * CORE CONCEPT:
 * - Create 2n nodes: 0 to n-1 (original nodes) and n to 2n-1 (opposite/complement nodes)
 * - For each edge (u,v), if graph is bipartite:
 *   * u and v must be in different sets (different colors)
 *   * u and v_opposite must be in same set (u is same color as v's opposite)
 *   * u_opposite and v must be in same set (u's opposite is same color as v)
 * 
 * WHY THIS WORKS:
 * - If u and v are connected by an edge, they must have different colors
 * - So u should be grouped with v_opposite (both have same color)
 * - And u_opposite should be grouped with v (both have same color)
 * - If we ever find u and v in same group, they have same color = NOT BIPARTITE
 * 
 * EXAMPLE 1: BIPARTITE GRAPH
 * Graph: [[1,3],[0,2],[1,3],[0,2]]
 * Edges: (0,1), (0,3), (1,2), (2,3)
 * Expected result: TRUE (can color {0,2} red, {1,3} blue)
 * 
 * INITIALIZATION:
 * n = 4, so we create UnionFind with 2*4 = 8 nodes
 * Nodes 0,1,2,3 = original nodes
 * Nodes 4,5,6,7 = opposite nodes (4=0', 5=1', 6=2', 7=3')
 * 
 * Initial parent array: [0,1,2,3,4,5,6,7]
 * Initial rank array:   [0,0,0,0,0,0,0,0]
 * Each node is its own parent initially
 * 
 * PROCESSING EDGES:
 * 
 * Node u=0, neighbors=[1,3]:
 * 
 *   Edge (0,1):
 *   - Check if connected(0,1): find(0)=0, find(1)=1 → false ✓
 *   - Check if connected(4,5): find(4)=4, find(5)=5 → false ✓
 *   - Union(0,5): union(0,1+4) - connect node 0 with node 1's opposite
 *     * find(0)=0, find(5)=5, rank[0]=rank[5]=0
 *     * Make 5 parent of 0: parent[0]=5
 *     * Increment rank[5]=1
 *     * Result: parent=[5,1,2,3,4,5,6,7], rank=[0,0,0,0,0,1,0,0]
 *   - Union(4,1): union(0+4,1) - connect node 0's opposite with node 1
 *     * find(4)=4, find(1)=1, rank[4]=0, rank[1]=0
 *     * Make 1 parent of 4: parent[4]=1
 *     * Increment rank[1]=1
 *     * Result: parent=[5,1,2,3,1,5,6,7], rank=[0,1,0,0,0,1,0,0]
 * 
 *   Current groups: {0,5}, {1,4}, {2}, {3}, {6}, {7}
 *   Interpretation: Node 0 same color as node 1's opposite
 *                  Node 0's opposite same color as node 1
 * 
 *   Edge (0,3):
 *   - Check if connected(0,3): find(0)=5, find(3)=3 → false ✓
 *   - Check if connected(4,7): find(4)=1, find(7)=7 → false ✓
 *   - Union(0,7): union(0,3+4) - connect node 0 with node 3's opposite
 *     * find(0)=5, find(7)=7, rank[5]=1, rank[7]=0
 *     * Make 5 parent of 7: parent[7]=5
 *     * Result: parent=[5,1,2,3,1,5,6,5], rank=[0,1,0,0,0,1,0,0]
 *   - Union(4,3): union(0+4,3) - connect node 0's opposite with node 3
 *     * find(4)=1, find(3)=3, rank[1]=1, rank[3]=0
 *     * Make 1 parent of 3: parent[3]=1
 *     * Result: parent=[5,1,2,1,1,5,6,5], rank=[0,1,0,0,0,1,0,0]
 * 
 *   Current groups: {0,5,7}, {1,3,4}, {2}, {6}
 *   Interpretation: Nodes 0,3 have opposite colors (different groups)
 * 
 * Node u=1, neighbors=[0,2]:
 * 
 *   Edge (1,0): Already processed as (0,1), skip
 * 
 *   Edge (1,2):
 *   - Check if connected(1,2): find(1)=1, find(2)=2 → false ✓
 *   - Check if connected(5,6): find(5)=5, find(6)=6 → false ✓
 *   - Union(1,6): union(1,2+4) - connect node 1 with node 2's opposite
 *     * find(1)=1, find(6)=6, rank[1]=1, rank[6]=0
 *     * Make 1 parent of 6: parent[6]=1
 *     * Result: parent=[5,1,2,1,1,5,1,5], rank=[0,1,0,0,0,1,0,0]
 *   - Union(5,2): union(1+4,2) - connect node 1's opposite with node 2
 *     * find(5)=5, find(2)=2, rank[5]=1, rank[2]=0
 *     * Make 5 parent of 2: parent[2]=5
 *     * Result: parent=[5,1,5,1,1,5,1,5], rank=[0,1,0,0,0,1,0,0]
 * 
 *   Current groups: {0,2,5,7}, {1,3,4,6}
 *   Interpretation: Nodes 1,2 have opposite colors
 * 
 * Node u=2, neighbors=[1,3]:
 *   All edges already processed, skip
 * 
 * Node u=3, neighbors=[0,2]:
 *   All edges already processed, skip
 * 
 * FINAL STATE:
 * Groups: {0,2,5,7}, {1,3,4,6}
 * - Group 1 contains: original nodes {0,2} and opposite nodes {1',3'}
 * - Group 2 contains: original nodes {1,3} and opposite nodes {0',2'}
 * 
 * This means:
 * - Nodes 0,2 have same color (let's say RED)
 * - Nodes 1,3 have same color (let's say BLUE)
 * - No conflicts found → BIPARTITE = TRUE
 * 
 * 
 * EXAMPLE 2: NON-BIPARTITE GRAPH
 * Graph: [[1,2,3],[0,2],[0,1,3],[0,2]]
 * Contains cycle: 0-1-2-0 (odd length = 3)
 * Expected result: FALSE
 * 
 * INITIALIZATION:
 * n = 4, UnionFind with 8 nodes: 0,1,2,3,4,5,6,7
 * parent = [0,1,2,3,4,5,6,7], rank = [0,0,0,0,0,0,0,0]
 * 
 * PROCESSING:
 * 
 * Node u=0, neighbors=[1,2,3]:
 * 
 *   Edge (0,1):
 *   - connected(0,1)=false, connected(4,5)=false ✓
 *   - Union(0,5): parent=[5,1,2,3,4,5,6,7]
 *   - Union(4,1): parent=[5,1,2,3,1,5,6,7]
 *   Groups: {0,5}, {1,4}, {2}, {3}, {6}, {7}
 * 
 *   Edge (0,2):
 *   - connected(0,2)=false, connected(4,6)=false ✓
 *   - Union(0,6): Groups become {0,5,6}, {1,4}, {2}, {3}, {7}
 *   - Union(4,2): Groups become {0,5,6}, {1,2,4}, {3}, {7}
 * 
 *   Edge (0,3):
 *   - connected(0,3)=false, connected(4,7)=false ✓
 *   - Union(0,7): Groups become {0,5,6,7}, {1,2,4}, {3}
 *   - Union(4,3): Groups become {0,5,6,7}, {1,2,3,4}
 * 
 * Node u=1, neighbors=[0,2]:
 * 
 *   Edge (1,0): Skip (already processed)
 * 
 *   Edge (1,2):
 *   - Check connected(1,2): Both in group {1,2,3,4} → find(1)=find(2) → TRUE
 *   - CONFLICT DETECTED! Nodes 1 and 2 are in same group
 *   - This means they would have same color, but they're connected by edge
 *   - Return FALSE immediately
 * 
 * RESULT: FALSE (Not bipartite)
 * 
 * WHY IT FAILED:
 * The cycle 0-1-2-0 creates a contradiction:
 * - From edge (0,1): node 0 and node 1 need different colors
 * - From edge (0,2): node 0 and node 2 need different colors  
 * - From edge (1,2): node 1 and node 2 need different colors
 * - But if 0≠1 and 0≠2, then 1 and 2 must have same color as 0's opposite
 * - This violates the constraint that 1≠2
 * 
 * ALGORITHM SUMMARY:
 * 1. Create 2n nodes in UnionFind (originals + opposites)
 * 2. For each edge (u,v):
 *    a. Check if u and v are already in same group → return false
 *    b. Check if u' and v' are already in same group → return false  
 *    c. Union u with v' (same color group)
 *    d. Union u' with v (same color group)
 * 3. If no conflicts found → return true
 * 
 * TIME COMPLEXITY: O(E * α(V)) where α is inverse Ackermann function
 * SPACE COMPLEXITY: O(V) for the UnionFind structure
 */

 class Solution2 {

     public boolean isBipartite(int[][] graph) {
         int n = graph.length;
         UnionFind uf = new UnionFind(n);

         for (int u = 0; u < n; u++) {
             int[] neighbors = graph[u];
             for (int v : neighbors) {
                 // If u and v are in the same set, not bipartite
                 if (uf.find(u) == uf.find(v)) {
                     return false;
                 }
                 // Union the first neighbor of u with v (group all neighbors together)
                 uf.union(graph[u][0], v);
             }
         }

         return true;
     }

     class UnionFind {
         int[] parent;

         UnionFind(int size) {
             parent = new int[size];
             for (int i = 0; i < size; i++)
                 parent[i] = i;
         }

         int find(int x) {
             if (parent[x] != x) {
                 parent[x] = find(parent[x]); // Path compression
             }
             return parent[x];
         }

         void union(int x, int y) {
             int px = find(x), py = find(y);
             if (px != py) {
                 parent[px] = py;
             }
         }
     }

 }

 /*
 * SIMPLIFIED UNION-FIND APPROACH FOR BIPARTITE GRAPH DETECTION
 * 
 * BRILLIANT INSIGHT:
 * - In a bipartite graph, all neighbors of a node must be in the SAME partition
 * - So we can group all neighbors of each node together
 * - If we ever find a node in the same group as one of its neighbors → NOT BIPARTITE
 * 
 * KEY STRATEGY:
 * For each node u with neighbors [v1, v2, v3, ...]:
 * 1. Check if u is already in same group as any neighbor → return false
 * 2. Union all neighbors together (they must all be in same partition)
 * 
 * WHY THIS WORKS:
 * - If graph is bipartite, neighbors of u are all in opposite partition from u
 * - So all neighbors of u should be grouped together (same partition)
 * - If u ever ends up in same group as its neighbor → contradiction
 * 
 * EXAMPLE 1: BIPARTITE GRAPH  
 * Graph: [[1,3],[0,2],[1,3],[0,2]]
 * Visual: 0---1---2
 *         |       |
 *         3-------+
 * Expected partitions: {0,2} and {1,3}
 * 
 * INITIALIZATION:
 * n = 4, parent = [0,1,2,3] (each node is its own parent)
 * 
 * PROCESSING:
 * 
 * u=0, neighbors=[1,3]:
 * - Check if find(0) == find(1): find(0)=0, find(1)=1 → 0≠1 ✓
 * - Check if find(0) == find(3): find(0)=0, find(3)=3 → 0≠3 ✓
 * - Union neighbors: union(graph[0][0], 3) = union(1, 3)
 *   * find(1)=1, find(3)=3, parent[1]=3
 *   * parent = [0,3,2,3]
 * - Groups: {0}, {1,3}, {2}
 * 
 * u=1, neighbors=[0,2]:
 * - Check if find(1) == find(0): find(1)=3, find(0)=0 → 3≠0 ✓
 * - Check if find(1) == find(2): find(1)=3, find(2)=2 → 3≠2 ✓
 * - Union neighbors: union(graph[1][0], 2) = union(0, 2)
 *   * find(0)=0, find(2)=2, parent[0]=2
 *   * parent = [2,3,2,3]
 * - Groups: {0,2}, {1,3}
 * 
 * u=2, neighbors=[1,3]:
 * - Check if find(2) == find(1): find(2)=2, find(1)=3 → 2≠3 ✓
 * - Check if find(2) == find(3): find(2)=2, find(3)=3 → 2≠3 ✓
 * - Union neighbors: union(graph[2][0], 3) = union(1, 3)
 *   * find(1)=3, find(3)=3 → already in same group, no change
 *   * parent = [2,3,2,3]
 * - Groups: {0,2}, {1,3}
 * 
 * u=3, neighbors=[0,2]:
 * - Check if find(3) == find(0): find(3)=3, find(0)=2 → 3≠2 ✓
 * - Check if find(3) == find(2): find(3)=3, find(2)=2 → 3≠2 ✓
 * - Union neighbors: union(graph[3][0], 2) = union(0, 2)
 *   * find(0)=2, find(2)=2 → already in same group, no change
 *   * parent = [2,3,2,3]
 * - Groups: {0,2}, {1,3}
 * 
 * FINAL RESULT: TRUE
 * Final groups: {0,2}, {1,3} - Perfect bipartition!
 * 
 * 
 * EXAMPLE 2: NON-BIPARTITE GRAPH
 * Graph: [[1,2,3],[0,2],[0,1,3],[0,2]]
 * Contains odd cycle: 0-1-2-0
 * 
 * INITIALIZATION:
 * parent = [0,1,2,3]
 * 
 * PROCESSING:
 * 
 * u=0, neighbors=[1,2,3]:
 * - Check conflicts: find(0)≠find(1), find(0)≠find(2), find(0)≠find(3) ✓
 * - Union neighbors starting with first: union(1,2), then union(1,3)
 *   * After union(1,2): parent = [0,2,2,3], groups: {0}, {1,2}, {3}
 *   * After union(1,3): parent = [0,2,2,2], groups: {0}, {1,2,3}
 * - Current groups: {0}, {1,2,3}
 * 
 * u=1, neighbors=[0,2]:
 * - Check if find(1) == find(0): find(1)=2, find(0)=0 → 2≠0 ✓
 * - Check if find(1) == find(2): find(1)=2, find(2)=2 → 2==2 ✗
 * - CONFLICT DETECTED! Node 1 is in same group as its neighbor 2
 * - Return FALSE immediately
 * 
 * RESULT: FALSE (Not bipartite)
 * 
 * WHY IT FAILED:
 * - Node 0 forced all its neighbors {1,2,3} into same group
 * - But node 1 is also connected to node 2
 * - This creates contradiction: 1 and 2 must be in same partition (from node 0's perspective)
 *   but also in different partitions (since they're directly connected)
 * - This happens because of the odd cycle 0-1-2-0
 * 
 * 
 * EXAMPLE 3: COMPLEX BIPARTITE GRAPH
 * Graph: [[1,3],[0,2,4],[1,5],[0,4,5],[1,3],[2,3]]
 * Expected partitions: {0,2,4} and {1,3,5}
 * 
 * TRACE:
 * Initial: parent = [0,1,2,3,4,5]
 * 
 * u=0, neighbors=[1,3]: union(1,3) → groups: {0}, {1,3}, {2}, {4}, {5}
 * u=1, neighbors=[0,2,4]: 1≠0, 1≠2, 1≠4 ✓, union(0,2), union(0,4) → groups: {0,2,4}, {1,3}, {5}
 * u=2, neighbors=[1,5]: 2≠1, 2≠5 ✓, union(1,5) → groups: {0,2,4}, {1,3,5}
 * u=3, neighbors=[0,4,5]: 3≠0, 3≠4, 3≠5 ✓, union(0,4) (no change), union(0,5) → groups: {0,2,4,5}, {1,3}
 * Wait... this would create wrong grouping!
 * 
 * Actually, let me retrace u=3:
 * When we reach u=3 with neighbors=[0,4,5]:
 * Current groups: {0,2,4}, {1,3,5}
 * - find(3) = 5 (after path compression from group {1,3,5})
 * - find(0) = 0 (from group {0,2,4})  
 * - find(4) = 0 (from group {0,2,4})
 * - find(5) = 5 (from group {1,3,5})
 * - Check: 5≠0 ✓, 5≠0 ✓, but 5==5 ✗
 * - CONFLICT: Node 3 is in same group as neighbor 5
 * 
 * Hmm, this suggests the graph might not be bipartite. Let me verify...
 * Actually, looking at connections: 3 connects to 0,4,5 and 5 connects to 2,3
 * So we have edge 3-5, which means 3 and 5 should be in different partitions.
 * But from u=2 processing, we put 1 and 5 in same group as node 1.
 * And from u=0 processing, we put 1 and 3 in same group.
 * So 3 and 5 end up in same group, creating conflict when we find edge 3-5.
 * 
 * This correctly identifies that the graph is NOT bipartite!
 * 
 * ALGORITHM CORRECTNESS:
 * The algorithm works because:
 * 1. In bipartite graph, all neighbors of any node must be in same partition
 * 2. We group neighbors together and check for conflicts
 * 3. Any conflict means the graph has odd cycle → not bipartite
 * 
 * TIME COMPLEXITY: O(V + E * α(V)) where α is inverse Ackermann
 * SPACE COMPLEXITY: O(V) for parent array
 * 
 * ADVANTAGES over 2n approach:
 * - Simpler to understand and implement
 * - Uses only n nodes instead of 2n
 * - More intuitive: directly groups nodes that should be in same partition
 * - Same time complexity but better space constant
 */
