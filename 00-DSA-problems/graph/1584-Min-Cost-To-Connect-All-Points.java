import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

class MinCostToConnectAllPointsPrims2 {
    public static final int INF = Integer.MAX_VALUE;

    public int minCostConnectPoints(int[][] points) {
        int vertices = points.length;
        int[] minDistances = initMinDistances(vertices);
        boolean[] visited = new boolean[vertices];
        int totalCost = 0;

        for (int i = 0; i < vertices; i++) {
            int minDistanceVertex = findMinDistanceVertex(minDistances, visited);
            visited[minDistanceVertex] = true;
            totalCost += minDistances[minDistanceVertex];
            for (int vertex = 0; vertex < vertices; vertex++) {
                if (!visited[vertex]) {
                    int distance = getDistance(points[minDistanceVertex], points[vertex]);
                    if (distance < minDistances[vertex]) {
                        minDistances[vertex] = distance;
                    }
                }
            }
        }
        return totalCost;
    }

    private int getDistance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private int findMinDistanceVertex(int[] distances, boolean[] visited) {
        int vertices = distances.length;
        int minDistanceVertex = -1;
        int minDistance = INF;
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (!visited[vertex] && distances[vertex] < minDistance) {
                minDistanceVertex = vertex;
                minDistance = distances[vertex];
            }
        }
        return minDistanceVertex;
    }

    private int[] initMinDistances(int n) {
        int[] minDistances = new int[n];
        Arrays.fill(minDistances, INF);
        minDistances[0] = 0;
        return minDistances;
    }
}


/*
 ============================================================
 LeetCode 1584: Min Cost to Connect All Points
 ============================================================

 Approach:
 ----------
 Prim's Algorithm (Minimum Spanning Tree)

 - Each point is a node
 - Edge weight = Manhattan distance
 - Graph is complete, so we generate edges lazily

 ============================================================
 */

class MinCostToConnectAllPointsPrims {

    /*
     ------------------------------------------------------------
     Edge representation for PriorityQueue
     ------------------------------------------------------------
     */
    static class Edge implements Comparable<Edge> {
        int from;
        int to;
        int weight;

        Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge other) {
            return this.weight - other.weight;
        }
    }

    public int minCostConnectPoints(int[][] points) {
        int n = points.length;

        boolean[] visited = new boolean[n];
        PriorityQueue<Edge> minHeap = new PriorityQueue<>();

        int totalCost = 0;
        int edgesUsed = 0;

        // Start from point 0
        visited[0] = true;

        // Add all edges from point 0
        for (int i = 1; i < n; i++) {
            minHeap.offer(new Edge(0, i, manhattan(points[0], points[i])));
        }

        /*
         --------------------------------------------------------
         Build MST
         --------------------------------------------------------
         */
        while (!minHeap.isEmpty() && edgesUsed < n - 1) {
            Edge edge = minHeap.poll();

            // Ignore edges leading to already visited nodes
            if (visited[edge.to]) {
                continue;
            }

            // Accept this edge
            visited[edge.to] = true;
            totalCost += edge.weight;
            edgesUsed++;

            // Add new candidate edges from the newly added node
            for (int next = 0; next < n; next++) {
                if (!visited[next]) {
                    minHeap.offer(
                        new Edge(edge.to, next,
                                 manhattan(points[edge.to], points[next]))
                    );
                }
            }
        }

        return totalCost;
    }

    /*
     ------------------------------------------------------------
     Manhattan distance helper
     ------------------------------------------------------------
     */
    private int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    /*
     ============================================================
     Time & Space Complexity Analysis
     ============================================================

     Let n = number of points

     Time Complexity:
     ----------------
     - Each point is added once to the MST
     - For each addition, we may push up to O(n) edges
     - Each heap operation costs O(log E), where E ≈ n²

     Overall:
     O(n² log n)

     Space Complexity:
     -----------------
     - PriorityQueue can hold up to O(n²) edges
     - visited[] array → O(n)

     Overall:
     O(n²)

     ============================================================
     */
}


class MinCostToConnectAllPointsKruskals {

    public int minCostConnectPoints(int[][] points) {
        int n = points.length;
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                edges.add(new Edge(i, j, getDistance(points[i], points[j])));
            }
        }
        Collections.sort(edges);
        int totalCost = 0, edgesUsed = 0;
        DisjointSet ds = new DisjointSet(n);
        for (Edge edge : edges) {
            if (ds.union(edge.from, edge.to)) {
                totalCost += edge.weight;
                edgesUsed++;
                if (edgesUsed == n - 1) {
                    break; // MST complete
                }
            }
        }
        return totalCost;
    }

    private int getDistance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }
}

class Edge implements Comparable<Edge> {
    int from, to, weight;

    public Edge(int from, int to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge other) {
        return this.weight - other.weight;
    }
}

class DisjointSet {
    int[] parent, rank;

    public DisjointSet(int n) {
        this.parent = new int[n];
        this.rank = new int[n];
        for (int i = 0; i < n; i++) {
            this.parent[i] = i;
            this.rank[i] = 0;
        }
    }

    public int find(int i) {
        if (this.parent[i] != i) {
            this.parent[i] = this.find(this.parent[i]);
        }
        return this.parent[i];
    }

    public boolean union(int i, int j) {
        int rooI = this.find(i), rootJ = this.find(j);
        if (rooI == rootJ) {
            return false;
        }
        if (this.rank[rooI] < this.rank[rootJ]) {
            this.parent[rooI] = rootJ;
        } else if (this.rank[rooI] > this.rank[rootJ]) {
            this.parent[rootJ] = rooI;
        } else {
            this.parent[rootJ] = rooI;
            this.rank[rooI]++;
        }
        return true;
    }
}

/**
 * PROBLEM ANALYSIS:
 * ==================
 * Given points on a 2D plane, connect all points with minimum total Manhattan distance.
 * Need to find a Minimum Spanning Tree (MST) where edge weight = Manhattan distance.
 * 
 * KEY INSIGHTS:
 * 1. This is a classic Minimum Spanning Tree (MST) problem
 * 2. Complete graph: every point can connect to every other point
 * 3. Edge weight = Manhattan distance: |x1-x2| + |y1-y2|
 * 4. Need exactly n-1 edges to connect n points (tree property)
 * 5. Two main algorithms: Kruskal's and Prim's
 * 
 * MST ALGORITHMS:
 * 1. Kruskal's: Sort all edges, add smallest edges that don't create cycle
 *    - Use Union-Find to detect cycles
 *    - Time: O(E log E) where E = n² edges
 * 
 * 2. Prim's: Start from one point, greedily add closest unvisited point
 *    - Use Priority Queue to find minimum edge
 *    - Time: O(n² log n) with PQ
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Recognize this as MST problem ("connect all with minimum cost")
 * 2. Decide between Kruskal's (edge-based) vs Prim's (vertex-based)
 * 3. For complete graph, both work well
 * 4. Kruskal's easier to implement with Union-Find
 * 
 * HOW TO COME UP WITH SOLUTION:
 * ==============================
 * Step 1: Understand "connect all points" → need spanning tree
 * Step 2: "Minimum cost" → need MINIMUM spanning tree
 * Step 3: Recall MST algorithms from CS fundamentals
 * Step 4: Choose Kruskal's or Prim's based on comfort level
 * Step 5: Implement with appropriate data structures
 */


class MinCostConnectPoints {
    
    // ============================================================================
    // APPROACH 1: KRUSKAL'S ALGORITHM (MOST INTUITIVE)
    // ============================================================================
    // Time Complexity: O(n² log n) - creating and sorting n² edges
    // Space Complexity: O(n²) - storing all edges
    // Best for interviews - straightforward implementation
    
    /**
     * Kruskal's Algorithm:
     * 1. Create all possible edges with their costs
     * 2. Sort edges by cost (ascending)
     * 3. Use Union-Find to add edges without creating cycles
     * 4. Stop when we have n-1 edges (spanning tree complete)
     */
    public int minCostConnectPoints(int[][] points) {
        int n = points.length;
        
        // Step 1: Create all edges
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int cost = manhattanDistance(points[i], points[j]);
                edges.add(new Edge(i, j, cost));
            }
        }
        
        // Step 2: Sort edges by cost
        Collections.sort(edges, (a, b) -> a.cost - b.cost);
        
        // Step 3: Use Union-Find to build MST
        UnionFind uf = new UnionFind(n);
        int totalCost = 0;
        int edgesAdded = 0;
        
        for (Edge edge : edges) {
            // Try to add edge if it doesn't create cycle
            if (uf.union(edge.from, edge.to)) {
                totalCost += edge.cost;
                edgesAdded++;
                
                // MST complete when we have n-1 edges
                if (edgesAdded == n - 1) {
                    break;
                }
            }
        }
        
        return totalCost;
    }
    
    /**
     * Edge representation for Kruskal's algorithm
     */
    class Edge {
        int from, to, cost;
        
        Edge(int from, int to, int cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
        }
    }
    
    /**
     * Union-Find for cycle detection
     */
    class UnionFind {
        private int[] parent;
        private int[] rank;
        
        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
        }
        
        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }
        
        // Returns true if union was performed (elements were in different sets)
        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            if (rootX == rootY) {
                return false; // Already connected, would create cycle
            }
            
            // Union by rank
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
            
            return true;
        }
    }
    
    /**
     * Calculate Manhattan distance between two points
     */
    private int manhattanDistance(int[] p1, int[] p2) {
        return Math.abs(p1[0] - p2[0]) + Math.abs(p1[1] - p2[1]);
    }
    
    // ============================================================================
    // APPROACH 2: PRIM'S ALGORITHM (ELEGANT ALTERNATIVE)
    // ============================================================================
    // Time Complexity: O(n² log n) - n iterations, each with PQ operations
    // Space Complexity: O(n) - priority queue and visited array
    // More space-efficient, no need to store all edges
    
    /**
     * Prim's Algorithm:
     * 1. Start from any point (say point 0)
     * 2. Maintain a set of visited points
     * 3. Always add the minimum-cost edge connecting visited to unvisited
     * 4. Continue until all points are visited
     */
    public int minCostConnectPointsPrim(int[][] points) {
        int n = points.length;
        boolean[] visited = new boolean[n];
        
        // PQ: [cost, pointIndex]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        
        // Start from point 0
        pq.offer(new int[]{0, 0}); // cost 0 to include starting point
        
        int totalCost = 0;
        int pointsConnected = 0;
        
        while (!pq.isEmpty() && pointsConnected < n) {
            int[] current = pq.poll();
            int cost = current[0];
            int point = current[1];
            
            // Skip if already visited
            if (visited[point]) {
                continue;
            }
            
            // Add this point to MST
            visited[point] = true;
            totalCost += cost;
            pointsConnected++;
            
            // Add all edges from this point to unvisited points
            for (int next = 0; next < n; next++) {
                if (!visited[next]) {
                    int edgeCost = manhattanDistance(points[point], points[next]);
                    pq.offer(new int[]{edgeCost, next});
                }
            }
        }
        
        return totalCost;
    }
    
    // ============================================================================
    // APPROACH 3: OPTIMIZED PRIM'S (NO PRIORITY QUEUE)
    // ============================================================================
    // Time Complexity: O(n²) - no log factor from PQ
    // Space Complexity: O(n)
    // Best complexity but slightly more code
    
    /**
     * Optimized Prim's without PQ:
     * Track minimum cost to connect each unvisited point
     * In each iteration, find the minimum among all unvisited points
     */
    public int minCostConnectPointsOptimized(int[][] points) {
        int n = points.length;
        boolean[] visited = new boolean[n];
        
        // minCost[i] = minimum cost to connect point i to MST
        int[] minCost = new int[n];
        Arrays.fill(minCost, Integer.MAX_VALUE);
        minCost[0] = 0; // Start from point 0
        
        int totalCost = 0;
        
        for (int i = 0; i < n; i++) {
            // Find unvisited point with minimum cost
            int minPoint = -1;
            int minValue = Integer.MAX_VALUE;
            
            for (int j = 0; j < n; j++) {
                if (!visited[j] && minCost[j] < minValue) {
                    minValue = minCost[j];
                    minPoint = j;
                }
            }
            
            // Add this point to MST
            visited[minPoint] = true;
            totalCost += minValue;
            
            // Update costs for remaining unvisited points
            for (int j = 0; j < n; j++) {
                if (!visited[j]) {
                    int cost = manhattanDistance(points[minPoint], points[j]);
                    minCost[j] = Math.min(minCost[j], cost);
                }
            }
        }
        
        return totalCost;
    }
    
    // ============================================================================
    // APPROACH 4: KRUSKAL'S WITH EARLY TERMINATION OPTIMIZATION
    // ============================================================================
    // Time Complexity: O(n² log n)
    // Space Complexity: O(n²)
    // Same as Approach 1 but with explicit early termination
    
    /**
     * Kruskal's with optimization: stop as soon as MST is complete
     */
    public int minCostConnectPointsKruskalOpt(int[][] points) {
        int n = points.length;
        if (n <= 1) return 0;
        
        // Generate all edges
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);
        
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int cost = Math.abs(points[i][0] - points[j][0]) 
                         + Math.abs(points[i][1] - points[j][1]);
                pq.offer(new int[]{i, j, cost});
            }
        }
        
        UnionFind uf = new UnionFind(n);
        int totalCost = 0;
        int edgesAdded = 0;
        
        while (!pq.isEmpty() && edgesAdded < n - 1) {
            int[] edge = pq.poll();
            if (uf.union(edge[0], edge[1])) {
                totalCost += edge[2];
                edgesAdded++;
            }
        }
        
        return totalCost;
    }
    
    // ============================================================================
    // TEST CASES WITH DETAILED EXPLANATIONS
    // ============================================================================
    
    public static void main(String[] args) {
        MinCostConnectPoints solution = new MinCostConnectPoints();
        
        // Test Case 1: Example from problem
        System.out.println("Test 1: Basic example");
        int[][] points1 = {{0,0},{2,2},{3,10},{5,2},{7,0}};
        System.out.println("Points: [[0,0],[2,2],[3,10],[5,2],[7,0]]");
        System.out.println("Expected: 20");
        System.out.println("Got (Kruskal): " + solution.minCostConnectPoints(points1));
        System.out.println("Got (Prim): " + solution.minCostConnectPointsPrim(points1));
        System.out.println("Got (Optimized): " + solution.minCostConnectPointsOptimized(points1));
        /*
         * MST edges:
         * (0,0) - (2,2): cost 4
         * (2,2) - (5,2): cost 3
         * (5,2) - (7,0): cost 4
         * (2,2) - (3,10): cost 9
         * Total: 20
         */
        
        // Test Case 2: Triangle
        System.out.println("\nTest 2: Three points");
        int[][] points2 = {{3,12},{-2,5},{-4,1}};
        System.out.println("Points: [[3,12],[-2,5],[-4,1]]");
        System.out.println("Expected: 18");
        System.out.println("Got: " + solution.minCostConnectPoints(points2));
        /*
         * Distances:
         * (3,12) - (-2,5): |3-(-2)| + |12-5| = 5 + 7 = 12
         * (3,12) - (-4,1): |3-(-4)| + |12-1| = 7 + 11 = 18
         * (-2,5) - (-4,1): |-2-(-4)| + |5-1| = 2 + 4 = 6
         * MST: Pick 12 and 6 (avoid 18)
         * Total: 18
         */
        
        // Test Case 3: Single point
        System.out.println("\nTest 3: Single point");
        int[][] points3 = {{0,0}};
        System.out.println("Expected: 0 (no connections needed)");
        System.out.println("Got: " + solution.minCostConnectPoints(points3));
        
        // Test Case 4: Two points
        System.out.println("\nTest 4: Two points");
        int[][] points4 = {{0,0},{1,1}};
        System.out.println("Points: [[0,0],[1,1]]");
        System.out.println("Expected: 2 (|0-1| + |0-1| = 2)");
        System.out.println("Got: " + solution.minCostConnectPoints(points4));
        
        // Test Case 5: Points in a line
        System.out.println("\nTest 5: Collinear points");
        int[][] points5 = {{0,0},{1,0},{2,0},{3,0}};
        System.out.println("Points: [[0,0],[1,0],[2,0],[3,0]]");
        System.out.println("Expected: 3 (connect adjacent points)");
        System.out.println("Got: " + solution.minCostConnectPoints(points5));
        /*
         * Best MST: 0-1-2-3 (chain)
         * Cost: 1 + 1 + 1 = 3
         */
        
        // Test Case 6: Square corners
        System.out.println("\nTest 6: Square");
        int[][] points6 = {{0,0},{0,1},{1,0},{1,1}};
        System.out.println("Points: [[0,0],[0,1],[1,0],[1,1]]");
        System.out.println("Expected: 3");
        System.out.println("Got: " + solution.minCostConnectPoints(points6));
        /*
         * Square with side length 1
         * Need 3 edges, each with cost 1
         * Total: 3
         */
        
        // Test Case 7: Clustered points
        System.out.println("\nTest 7: Distant clusters");
        int[][] points7 = {{0,0},{0,1},{100,100},{100,101}};
        System.out.println("Points: [[0,0],[0,1],[100,100],[100,101]]");
        System.out.println("Got: " + solution.minCostConnectPoints(points7));
        /*
         * Two clusters far apart
         * Connect within each cluster (cost 1 each)
         * Connect clusters (cost 200)
         * Total: 1 + 1 + 200 = 202
         */
        
        // Test Case 8: Negative coordinates
        System.out.println("\nTest 8: Negative coordinates");
        int[][] points8 = {{-1,-1},{1,1},{-1,1},{1,-1}};
        System.out.println("Points: [[-1,-1],[1,1],[-1,1],[1,-1]]");
        System.out.println("Got: " + solution.minCostConnectPoints(points8));
        /*
         * Diamond shape
         * Distance from corner to corner: 4
         * Distance from corner to adjacent: 2
         * MST uses 3 edges of length 2
         * Total: 6
         */
    }
}

/**
 * COMPREHENSIVE INTERVIEW GUIDE:
 * ===============================
 * 
 * 1. PROBLEM RECOGNITION:
 * ----------------------
 * Key phrases that identify MST problems:
 * - "Connect all points/nodes"
 * - "Minimum cost/weight"
 * - "All points connected" = spanning tree
 * - "Exactly one path between any two" = tree property
 * 
 * This is a TEXTBOOK Minimum Spanning Tree (MST) problem!
 * 
 * 2. MST FUNDAMENTALS TO KNOW:
 * ----------------------------
 * DEFINITION:
 * - Spanning tree: subgraph that connects all vertices, no cycles
 * - Minimum spanning tree: spanning tree with minimum total edge weight
 * - For n vertices, MST has exactly n-1 edges
 * 
 * TWO CLASSIC ALGORITHMS:
 * a) Kruskal's Algorithm (edge-based):
 *    - Sort all edges by weight
 *    - Add edges in order, skip if creates cycle
 *    - Use Union-Find for cycle detection
 * 
 * b) Prim's Algorithm (vertex-based):
 *    - Start from arbitrary vertex
 *    - Greedily add minimum edge to unvisited vertex
 *    - Use Priority Queue to find minimum
 * 
 * 3. WHEN TO USE WHICH:
 * ---------------------
 * KRUSKAL'S:
 * + Good for sparse graphs (few edges)
 * + Easier to understand and implement
 * + Natural with Union-Find
 * - Must store all edges (O(E) space)
 * - O(E log E) sorting
 * 
 * PRIM'S:
 * + Good for dense graphs (many edges)
 * + More space-efficient (no edge storage)
 * + Can optimize to O(n²) without PQ
 * - Slightly more complex logic
 * 
 * FOR THIS PROBLEM:
 * - Complete graph (n² edges)
 * - Both algorithms work equally well
 * - Kruskal's slightly easier to code
 * - RECOMMEND: Kruskal's for interview
 * 
 * 4. SOLUTION WALKTHROUGH (KRUSKAL'S):
 * ------------------------------------
 * Step 1: Generate all possible edges
 *         - For n points, create n(n-1)/2 edges
 *         - Edge weight = Manhattan distance
 * 
 * Step 2: Sort edges by weight (ascending)
 * 
 * Step 3: Initialize Union-Find with n components
 * 
 * Step 4: Process edges in sorted order:
 *         - If edge connects different components → add it
 *         - If edge would create cycle → skip it
 *         - Stop when MST has n-1 edges
 * 
 * Step 5: Return total cost
 * 
 * 5. COMPLEXITY ANALYSIS:
 * -----------------------
 * KRUSKAL'S:
 * Time: O(n² log n)
 *   - Generate edges: O(n²)
 *   - Sort edges: O(n² log n)
 *   - Union-Find ops: O(n² × α(n)) ≈ O(n²)
 *   - Dominated by sorting
 * Space: O(n²) for storing edges
 * 
 * PRIM'S (with PQ):
 * Time: O(n² log n)
 *   - n iterations
 *   - Each iteration: O(n) to add edges + O(log n) PQ ops
 *   - Total: O(n² log n)
 * Space: O(n) for PQ and visited array
 * 
 * PRIM'S (optimized):
 * Time: O(n²)
 *   - n iterations
 *   - Each iteration: O(n) to find min + O(n) to update
 *   - Total: O(n²)
 * Space: O(n)
 * 
 * BEST: Optimized Prim's has best time complexity!
 * 
 * 6. EDGE CASES TO DISCUSS:
 * -------------------------
 * ✓ Single point (n=1) → cost = 0
 * ✓ Two points → cost = Manhattan distance
 * ✓ All points at same location → cost = 0
 * ✓ Points in a line → connect adjacent points
 * ✓ Negative coordinates → Manhattan distance still works
 * ✓ Large distances → int overflow? (use long if needed)
 * ✓ Maximum n=1000 → O(n²) is acceptable
 * 
 * 7. COMMON MISTAKES TO AVOID:
 * ----------------------------
 * ✗ Forgetting MST needs exactly n-1 edges
 * ✗ Not checking for cycle in Kruskal's
 * ✗ Using Euclidean instead of Manhattan distance
 * ✗ Not handling single point edge case
 * ✗ Creating duplicate edges (i→j and j→i)
 * ✗ Integer overflow for large coordinates
 * ✗ Not early terminating after n-1 edges
 * ✗ Confusing visited vs Union-Find parent
 * 
 * 8. INTERVIEW COMMUNICATION STRATEGY:
 * ------------------------------------
 * 1. Recognize: "This is a Minimum Spanning Tree problem"
 * 2. Explain MST: "Need to connect all points with minimum total cost"
 * 3. Choose algorithm: "I'll use Kruskal's with Union-Find"
 * 4. Outline steps: Describe the 4 steps clearly
 * 5. Complexity: "O(n² log n) time, O(n²) space"
 * 6. Code: Implement cleanly with helper methods
 * 7. Test: Walk through small example
 * 8. Optimize: Mention Prim's optimized version if time permits
 * 
 * 9. DETAILED EXAMPLE WALKTHROUGH:
 * --------------------------------
 * Points: [[0,0],[2,2],[3,10],[5,2],[7,0]]
 * 
 * Step 1: Generate all edges
 * (0,1): |0-2| + |0-2| = 4
 * (0,2): |0-3| + |0-10| = 13
 * (0,3): |0-5| + |0-2| = 7
 * (0,4): |0-7| + |0-0| = 7
 * (1,2): |2-3| + |2-10| = 9
 * (1,3): |2-5| + |2-2| = 3
 * (1,4): |2-7| + |2-0| = 7
 * (2,3): |3-5| + |10-2| = 10
 * (2,4): |3-7| + |10-0| = 14
 * (3,4): |5-7| + |2-0| = 4
 * 
 * Step 2: Sort edges
 * [3, 4, 4, 7, 7, 7, 9, 10, 13, 14]
 * 
 * Step 3: Kruskal's execution
 * Edge (1,3), cost 3: Add (components: {0},{1,3},{2},{4})
 * Edge (0,1), cost 4: Add (components: {0,1,3},{2},{4})
 * Edge (3,4), cost 4: Add (components: {0,1,3,4},{2})
 * Edge (0,3), cost 7: Skip (cycle)
 * Edge (0,4), cost 7: Skip (cycle)
 * Edge (1,4), cost 7: Skip (cycle)
 * Edge (1,2), cost 9: Add (components: {0,1,2,3,4})
 * STOP: Have n-1 = 4 edges
 * 
 * Total cost: 3 + 4 + 4 + 9 = 20 ✓
 * 
 * 10. COMPARISON WITH RELATED PROBLEMS:
 * -------------------------------------
 * - Kruskal's MST → This problem (complete graph)
 * - Prim's MST → Network connections (sparse graph)
 * - Dijkstra's → Shortest path from one source
 * - Bellman-Ford → Shortest path with negative weights
 * - Floyd-Warshall → All-pairs shortest paths
 * - Steiner Tree → MST with optional vertices (NP-hard)
 * 
 * 11. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * --------------------------------------
 * Q: "What if we use Euclidean distance instead?"
 * A: Same algorithms work, just change distance formula
 * 
 * Q: "Can you find the actual edges in the MST?"
 * A: Yes, store edges as we add them in Kruskal's
 * 
 * Q: "What if some points can't be connected?"
 * A: Check if final component count = 1 using Union-Find
 * 
 * Q: "What if edges have different costs not based on distance?"
 * A: Same approach, just use given costs instead of computing
 * 
 * Q: "How to handle 3D points?"
 * A: Extend Manhattan distance: |x1-x2| + |y1-y2| + |z1-z2|
 * 
 * Q: "Can we do better than O(n² log n)?"
 * A: Yes! Optimized Prim's is O(n²), but can't do better asymptotically
 * 
 * 12. OPTIMIZATION DISCUSSIONS:
 * -----------------------------
 * Q: "Why is O(n²) the lower bound?"
 * A: Must examine all n² edges at least once in complete graph
 * 
 * Q: "When would sparse graph algorithms help?"
 * A: If we could prune edges (e.g., only connect nearby points)
 *    But problem requires considering all possible connections
 * 
 * Q: "Space optimization?"
 * A: Prim's uses O(n) vs Kruskal's O(n²)
 *    For interviews, either is fine
 * 
 * 13. MST PROPERTIES TO KNOW:
 * ---------------------------
 * ✓ MST has exactly n-1 edges for n vertices
 * ✓ MST is acyclic (it's a tree)
 * ✓ Removing any edge disconnects the graph
 * ✓ Adding any edge creates exactly one cycle
 * ✓ MST might not be unique (multiple MSTs with same weight)
 * ✓ Cut property: minimum edge crossing any cut is in some MST
 * ✓ Cycle property: maximum edge in any cycle is not in MST
 * 
 * 14. WHEN TO USE MST IN REAL WORLD:
 * ----------------------------------
 * - Network design (minimize cable length)
 * - Circuit design (minimize wire length)
 * - Clustering algorithms (hierarchical clustering)
 * - Approximation for TSP
 * - Image segmentation
 * - Transportation networks
 * 
 * 15. KEY INSIGHTS TO EMPHASIZE:
 * ------------------------------
 * ✓ "This is a classic MST problem"
 * ✓ "Kruskal's with Union-Find is clean and efficient"
 * ✓ "Complete graph means O(n²) edges"
 * ✓ "Manhattan distance is the edge weight"
 * ✓ "Need exactly n-1 edges for spanning tree"
 * ✓ "Can optimize Prim's to O(n²) without PQ"
 * 
 * 16. TIME MANAGEMENT (45 MIN INTERVIEW):
 * ---------------------------------------
 * 0-5 min:   Recognize MST, explain approach
 * 5-10 min:  Discuss Kruskal's vs Prim's
 * 10-30 min: Implement Kruskal's solution
 * 30-35 min: Walk through test case
 * 35-40 min: Analyze complexity
 * 40-45 min: Discuss optimizations and alternatives
 * 
 * Remember: MST is a FUNDAMENTAL algorithm that every CS student should know.
 * If you can implement Kruskal's or Prim's correctly, you demonstrate solid
 * understanding of graph algorithms!
 */


/*
 ============================================================
 LeetCode 1584: Min Cost to Connect All Points
 ============================================================

 Problem Type:
 -------------
 Minimum Spanning Tree (MST)

 Graph Model:
 ------------
 - Each point is a node
 - Edge weight between points i and j:
   |xi - xj| + |yi - yj|  (Manhattan distance)
 - Complete graph

 Strategy:
 ----------
 Use Prim's Algorithm to build MST incrementally.

 ============================================================
 */

class Solution {

    public int minCostConnectPoints(int[][] points) {
        int n = points.length;

        // minDist[i] = minimum cost to connect point i to the MST
        int[] minDist = new int[n];
        Arrays.fill(minDist, Integer.MAX_VALUE);

        // visited[i] = whether point i is already in MST
        boolean[] visited = new boolean[n];

        int totalCost = 0;

        // Start from point 0
        minDist[0] = 0;

        // We need to add exactly n points to the MST
        for (int i = 0; i < n; i++) {

            // Pick the unvisited point with smallest connection cost
            int curr = -1;
            int currMin = Integer.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && minDist[j] < currMin) {
                    currMin = minDist[j];
                    curr = j;
                }
            }

            // Add this point to MST
            visited[curr] = true;
            totalCost += currMin;

            // Update distances to remaining points
            for (int next = 0; next < n; next++) {
                if (!visited[next]) {
                    int cost = manhattan(points[curr], points[next]);
                    minDist[next] = Math.min(minDist[next], cost);
                }
            }
        }

        return totalCost;
    }

    // Manhattan distance helper
    private int manhattan(int[] p1, int[] p2) {
        return Math.abs(p1[0] - p2[0]) + Math.abs(p1[1] - p2[1]);
    }
}
