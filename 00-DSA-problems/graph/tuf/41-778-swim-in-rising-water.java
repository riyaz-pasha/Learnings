/**
 * PROBLEM ANALYSIS:
 * ==================
 * Given an n×n grid with elevations, find minimum time t such that we can swim from
 * (0,0) to (n-1,n-1), where at time t, we can only visit cells with elevation ≤ t.
 * 
 * KEY INSIGHTS:
 * 1. At time t, we can visit any cell with elevation ≤ t
 * 2. We need minimum t such that there exists a path from start to end
 * 3. The answer is the MAXIMUM elevation along the MINIMUM path
 * 4. This is essentially "find path that minimizes the maximum edge weight"
 * 
 * PROBLEM VARIATIONS:
 * - Minimize sum of edges → Dijkstra
 * - Minimize max of edges → Modified Dijkstra or Binary Search + BFS
 * - This is a "minimax path" or "bottleneck shortest path" problem
 * 
 * WHY DIFFERENT APPROACHES WORK:
 * 1. Binary Search + BFS: Try different times, check if path exists
 * 2. Dijkstra (Modified): Track max elevation seen so far instead of sum
 * 3. Union-Find: Add cells in increasing elevation order, check connectivity
 * 4. Dijkstra (Priority Queue): Prioritize paths with smaller max elevation
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Recognize this is a path-finding problem with special constraints
 * 2. Realize we need minimum "water level" (time) to connect start and end
 * 3. Think about how to model: graph where edge weight = max(node elevations)
 * 4. Choose approach: Dijkstra with PQ is most intuitive
 * 
 * HOW TO COME UP WITH SOLUTION:
 * ==============================
 * Step 1: Understand that time t allows us to visit cells with elevation ≤ t
 * Step 2: Realize answer is minimum t such that a valid path exists
 * Step 3: Think: "What determines minimum t?" → Highest elevation on our path
 * Step 4: Model as graph problem: minimize maximum edge weight
 * Step 5: Use modified Dijkstra or binary search
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

class SwimInRisingWater {
    
    // ============================================================================
    // APPROACH 1: DIJKSTRA WITH PRIORITY QUEUE (MOST INTUITIVE)
    // ============================================================================
    // Time Complexity: O(n² log n) - each cell visited once, PQ operations
    // Space Complexity: O(n²) - visited array and priority queue
    // Best for interviews - clean and intuitive
    
    /**
     * Modified Dijkstra: track maximum elevation on path instead of sum
     * At each step, choose path with smallest "max elevation seen so far"
     */
    public int swimInWater(int[][] grid) {
        int n = grid.length;
        
        // PQ: [maxElevationSoFar, row, col]
        // Sort by maxElevationSoFar (smallest first)
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        
        boolean[][] visited = new boolean[n][n];
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        // Start from (0, 0) with elevation grid[0][0]
        pq.offer(new int[]{grid[0][0], 0, 0});
        
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int maxElev = current[0];
            int row = current[1];
            int col = current[2];
            
            // If reached destination, return the max elevation on this path
            if (row == n - 1 && col == n - 1) {
                return maxElev;
            }
            
            // Skip if already visited
            if (visited[row][col]) {
                continue;
            }
            visited[row][col] = true;
            
            // Explore neighbors
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n 
                    && !visited[newRow][newCol]) {
                    
                    // The new max elevation is the max of current path and neighbor's elevation
                    int newMaxElev = Math.max(maxElev, grid[newRow][newCol]);
                    pq.offer(new int[]{newMaxElev, newRow, newCol});
                }
            }
        }
        
        return -1; // Should never reach here if input is valid
    }
    
    // ============================================================================
    // APPROACH 2: BINARY SEARCH + BFS (ELEGANT ALTERNATIVE)
    // ============================================================================
    // Time Complexity: O(n² log(max_elevation)) - binary search × BFS
    // Space Complexity: O(n²) - for BFS queue and visited
    // Good when you want to show problem-solving creativity
    
    /**
     * Binary search on the answer (time/water level)
     * For each candidate time, check if path exists using BFS
     */
    public int swimInWaterBinarySearch(int[][] grid) {
        int n = grid.length;
        
        // Binary search on time: [max(start, end), max_elevation_in_grid]
        int left = Math.max(grid[0][0], grid[n-1][n-1]);
        int right = n * n - 1; // Max possible elevation
        
        int result = right;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            // Check if we can reach destination at time 'mid'
            if (canReach(grid, mid, n)) {
                result = mid;
                right = mid - 1; // Try smaller time
            } else {
                left = mid + 1; // Need more time
            }
        }
        
        return result;
    }
    
    /**
     * BFS to check if destination is reachable at given time
     */
    private boolean canReach(int[][] grid, int time, int n) {
        // Can only start if starting cell elevation <= time
        if (grid[0][0] > time) {
            return false;
        }
        
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[n][n];
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        queue.offer(new int[]{0, 0});
        visited[0][0] = true;
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            
            if (row == n - 1 && col == n - 1) {
                return true;
            }
            
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n 
                    && !visited[newRow][newCol] && grid[newRow][newCol] <= time) {
                    
                    visited[newRow][newCol] = true;
                    queue.offer(new int[]{newRow, newCol});
                }
            }
        }
        
        return false;
    }
    
    // ============================================================================
    // APPROACH 3: UNION-FIND (CREATIVE SOLUTION)
    // ============================================================================
    // Time Complexity: O(n² log n) - sorting + union-find operations
    // Space Complexity: O(n²)
    // Shows deep understanding of the problem structure
    
    /**
     * Process cells in increasing elevation order
     * When start and end become connected, that elevation is the answer
     */
    public int swimInWaterUnionFind(int[][] grid) {
        int n = grid.length;
        
        // Create list of cells sorted by elevation
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cells.add(new int[]{grid[i][j], i, j});
            }
        }
        
        // Sort by elevation
        Collections.sort(cells, (a, b) -> a[0] - b[0]);
        
        UnionFind uf = new UnionFind(n * n);
        boolean[][] added = new boolean[n][n];
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        int start = 0;
        int end = n * n - 1;
        
        // Add cells one by one in increasing elevation order
        for (int[] cell : cells) {
            int elevation = cell[0];
            int row = cell[1];
            int col = cell[2];
            int idx = row * n + col;
            
            added[row][col] = true;
            
            // Union with adjacent cells that are already added
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n 
                    && added[newRow][newCol]) {
                    
                    int neighborIdx = newRow * n + newCol;
                    uf.union(idx, neighborIdx);
                }
            }
            
            // Check if start and end are connected
            if (uf.find(start) == uf.find(end)) {
                return elevation;
            }
        }
        
        return -1;
    }
    
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
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }
        
        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            if (rootX != rootY) {
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }
    
    // ============================================================================
    // APPROACH 4: DFS WITH MEMOIZATION (LESS EFFICIENT BUT INTERESTING)
    // ============================================================================
    // Time Complexity: O(n² × max_elevation)
    // Space Complexity: O(n²)
    // Not recommended but good for understanding
    
    /**
     * Try each possible time, use DFS to check if path exists
     */
    public int swimInWaterDFS(int[][] grid) {
        int n = grid.length;
        int maxElev = 0;
        
        // Find maximum elevation in grid
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                maxElev = Math.max(maxElev, grid[i][j]);
            }
        }
        
        // Try each time from max(start, end) to maxElev
        int minTime = Math.max(grid[0][0], grid[n-1][n-1]);
        
        for (int time = minTime; time <= maxElev; time++) {
            boolean[][] visited = new boolean[n][n];
            if (dfs(grid, 0, 0, time, visited, n)) {
                return time;
            }
        }
        
        return maxElev;
    }
    
    private boolean dfs(int[][] grid, int row, int col, int time, 
                       boolean[][] visited, int n) {
        if (row < 0 || row >= n || col < 0 || col >= n 
            || visited[row][col] || grid[row][col] > time) {
            return false;
        }
        
        if (row == n - 1 && col == n - 1) {
            return true;
        }
        
        visited[row][col] = true;
        
        return dfs(grid, row - 1, col, time, visited, n)
            || dfs(grid, row + 1, col, time, visited, n)
            || dfs(grid, row, col - 1, time, visited, n)
            || dfs(grid, row, col + 1, time, visited, n);
    }
    
    // ============================================================================
    // TEST CASES WITH DETAILED EXPLANATIONS
    // ============================================================================
    
    public static void main(String[] args) {
        SwimInRisingWater solution = new SwimInRisingWater();
        
        // Test Case 1: Simple 2×2 grid
        System.out.println("Test 1: Small grid");
        int[][] grid1 = {{0, 2}, {1, 3}};
        System.out.println("Grid: [[0,2],[1,3]]");
        System.out.println("Expected: 3");
        System.out.println("Got (Dijkstra): " + solution.swimInWater(grid1));
        System.out.println("Got (Binary Search): " + solution.swimInWaterBinarySearch(grid1));
        System.out.println("Got (Union-Find): " + solution.swimInWaterUnionFind(grid1));
        /*
         * Explanation:
         * Time 0: Can only be at (0,0)
         * Time 1: Can reach (1,0) but not (0,1) or (1,1)
         * Time 2: Can reach (0,1) but not (1,1)
         * Time 3: Can reach all cells including (1,1)
         * Path: (0,0) → (0,1) → (1,1) or (0,0) → (1,0) → (1,1)
         * Max elevation on path: 3
         */
        
        // Test Case 2: Larger spiral grid
        System.out.println("\nTest 2: Spiral grid");
        int[][] grid2 = {
            {0, 1, 2, 3, 4},
            {24, 23, 22, 21, 5},
            {12, 13, 14, 15, 16},
            {11, 17, 18, 19, 20},
            {10, 9, 8, 7, 6}
        };
        System.out.println("Expected: 16");
        System.out.println("Got: " + solution.swimInWater(grid2));
        /*
         * The grid forms a spiral
         * Optimal path goes around the outer edge
         * Bottleneck is cell (2,4) with elevation 16
         */
        
        // Test Case 3: Monotonically increasing path
        System.out.println("\nTest 3: Increasing path");
        int[][] grid3 = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8}
        };
        System.out.println("Expected: 8");
        System.out.println("Got: " + solution.swimInWater(grid3));
        /*
         * Best path: right then down (or down then right)
         * Must pass through 8 at destination
         */
        
        // Test Case 4: All same elevation
        System.out.println("\nTest 4: Flat grid");
        int[][] grid4 = {
            {5, 5, 5},
            {5, 5, 5},
            {5, 5, 5}
        };
        System.out.println("Expected: 5");
        System.out.println("Got: " + solution.swimInWater(grid4));
        /*
         * All cells have same elevation
         * Answer is that elevation
         */
        
        // Test Case 5: High start/end
        System.out.println("\nTest 5: High endpoints");
        int[][] grid5 = {
            {10, 1, 2},
            {3, 4, 5},
            {6, 7, 15}
        };
        System.out.println("Expected: 15");
        System.out.println("Got: " + solution.swimInWater(grid5));
        /*
         * Start at 10, end at 15
         * Must wait for time 15 to reach destination
         * Even if path through middle is easier
         */
        
        // Test Case 6: Single cell
        System.out.println("\nTest 6: Single cell");
        int[][] grid6 = {{7}};
        System.out.println("Expected: 7");
        System.out.println("Got: " + solution.swimInWater(grid6));
        /*
         * Start and end are same cell
         * Answer is its elevation
         */
        
        // Test Case 7: Bottleneck in middle
        System.out.println("\nTest 7: Bottleneck");
        int[][] grid7 = {
            {0, 1, 2},
            {3, 100, 5},
            {6, 7, 8}
        };
        System.out.println("Expected: 8");
        System.out.println("Got: " + solution.swimInWater(grid7));
        /*
         * Middle cell has very high elevation (100)
         * Best to go around: right-down or down-right
         * Max on path: 8
         */
    }
}

/**
 * COMPREHENSIVE INTERVIEW GUIDE:
 * ===============================
 * 
 * 1. PROBLEM RECOGNITION:
 * ----------------------
 * Key phrases that identify this problem type:
 * - "minimum time until" → optimization problem
 * - "water rises gradually" → threshold increases over time
 * - "elevation" + "reachability" → constrained path-finding
 * - "can swim if elevation ≤ t" → binary condition on connectivity
 * 
 * This is a MINIMAX PATH problem (minimize the maximum edge weight)
 * 
 * 2. INITIAL OBSERVATIONS TO MENTION:
 * -----------------------------------
 * a) "This is different from regular shortest path (Dijkstra sums weights)"
 * b) "We want minimum time t such that path exists at that time"
 * c) "Answer = maximum elevation on the minimum-max-elevation path"
 * d) "Can model as graph where we minimize the maximum edge on path"
 * e) "Several approaches work: Dijkstra, Binary Search, Union-Find"
 * 
 * 3. WHY EACH APPROACH WORKS:
 * ---------------------------
 * 
 * DIJKSTRA (MODIFIED):
 * - Instead of tracking sum of path weights, track MAX
 * - PQ prioritizes paths with smallest max-elevation-so-far
 * - First time we reach destination = optimal answer
 * - Intuition: Greedily explore lowest-cost options first
 * 
 * BINARY SEARCH + BFS:
 * - Answer is in range [max(start, end), max_in_grid]
 * - Binary search on this range
 * - For each candidate time, BFS checks if path exists
 * - Intuition: If we can reach at time t, we can also reach at t+1
 * 
 * UNION-FIND:
 * - Process cells in increasing elevation order
 * - Union adjacent cells as we add them
 * - When start and end connect, that elevation is answer
 * - Intuition: As time increases, more cells become "available"
 * 
 * 4. SOLUTION WALKTHROUGH (DIJKSTRA):
 * -----------------------------------
 * Step 1: Initialize PQ with starting cell and its elevation
 * Step 2: Pop cell with smallest max-elevation-so-far
 * Step 3: If destination reached, return max-elevation
 * Step 4: For each neighbor:
 *         - Calculate new max = max(current_max, neighbor_elevation)
 *         - Add to PQ if not visited
 * Step 5: Repeat until destination found
 * 
 * 5. COMPLEXITY ANALYSIS:
 * -----------------------
 * DIJKSTRA:
 * Time: O(n² log n)
 *   - n² cells, each processed once
 *   - Each cell does O(4) neighbor checks
 *   - PQ operations: O(log n²) = O(log n)
 * Space: O(n²) for visited array and PQ
 * 
 * BINARY SEARCH + BFS:
 * Time: O(n² log(max_elevation))
 *   - Binary search: O(log(max_elevation)) iterations
 *   - Each BFS: O(n²)
 * Space: O(n²) for BFS
 * 
 * UNION-FIND:
 * Time: O(n² log n)
 *   - Sorting: O(n² log n)
 *   - Union-Find ops: O(n² × α(n)) ≈ O(n²)
 * Space: O(n²)
 * 
 * 6. EDGE CASES TO DISCUSS:
 * -------------------------
 * ✓ Single cell grid (1×1) → return grid[0][0]
 * ✓ Start elevation > end elevation → still need to consider path
 * ✓ All cells same elevation → return that elevation
 * ✓ Very high start or end → answer is at least max(start, end)
 * ✓ Bottleneck in middle → path must go through high elevation
 * ✓ Multiple paths available → algorithm finds best automatically
 * 
 * 7. COMMON MISTAKES TO AVOID:
 * ----------------------------
 * ✗ Using regular Dijkstra (summing elevations) → WRONG
 * ✗ Forgetting that start elevation matters → must wait for grid[0][0]
 * ✗ Not using visited array → can revisit cells, infinite loop
 * ✗ Thinking answer is always at destination → could be earlier in path
 * ✗ Binary search wrong range → must start from max(start, end)
 * ✗ In Union-Find, forgetting to mark cells as added
 * ✗ Confusion about "time t" = "water level t" = "max elevation allowed"
 * 
 * 8. WHICH APPROACH TO USE IN INTERVIEW:
 * --------------------------------------
 * 
 * RECOMMEND: Modified Dijkstra (Approach 1)
 * Why?
 * + Most intuitive once you understand minimax path
 * + Natural extension of standard Dijkstra
 * + Clean code, easy to explain
 * + Optimal complexity
 * 
 * ALTERNATIVE: Binary Search + BFS (Approach 2)
 * When?
 * + If you're more comfortable with BFS than Dijkstra
 * + Shows creative problem-solving
 * + Easy to verify correctness
 * 
 * ADVANCED: Union-Find (Approach 3)
 * When?
 * + Want to show deep understanding
 * + Interviewer asks for alternative
 * + Problem has follow-up about dynamic updates
 * 
 * 9. INTERVIEW COMMUNICATION STRATEGY:
 * ------------------------------------
 * 1. Clarify: "So we need minimum time where start and end are connected?"
 * 2. Observe: "This is like shortest path but minimizing MAX not SUM"
 * 3. Propose: "I'll use modified Dijkstra tracking max elevation on path"
 * 4. Draw: Show small example with path options
 * 5. Code: Write clean implementation with comments
 * 6. Test: Walk through example case step by step
 * 7. Optimize: Discuss alternatives if time permits
 * 
 * 10. DETAILED EXAMPLE WALKTHROUGH:
 * ---------------------------------
 * Grid: [[0,2],[1,3]]
 * 
 * Dijkstra Execution:
 * 
 * Initial: PQ = [(0, 0, 0)]  // (maxElev, row, col)
 *          visited = []
 * 
 * Step 1: Pop (0, 0, 0)
 *         Mark (0,0) visited
 *         Add neighbors:
 *           - (2, 0, 1) → to reach (0,1), max = max(0, 2) = 2
 *           - (1, 1, 0) → to reach (1,0), max = max(0, 1) = 1
 *         PQ = [(1, 1, 0), (2, 0, 1)]
 * 
 * Step 2: Pop (1, 1, 0)
 *         Mark (1,0) visited
 *         Add neighbors:
 *           - (3, 1, 1) → to reach (1,1), max = max(1, 3) = 3
 *         PQ = [(2, 0, 1), (3, 1, 1)]
 * 
 * Step 3: Pop (2, 0, 1)
 *         Mark (0,1) visited
 *         Add neighbors:
 *           - (3, 1, 1) → to reach (1,1), max = max(2, 3) = 3
 *         PQ = [(3, 1, 1), (3, 1, 1)]
 * 
 * Step 4: Pop (3, 1, 1)
 *         This is destination! Return 3
 * 
 * 11. COMPARISON WITH RELATED PROBLEMS:
 * -------------------------------------
 * - Regular Shortest Path → sum of weights → Dijkstra
 * - This Problem → max of weights → Modified Dijkstra
 * - Minimum Spanning Tree → sum of edges → Kruskal/Prim
 * - Widest Path → maximize minimum edge → Similar technique
 * - Network Flow → maximize flow → Different algorithm
 * 
 * 12. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * --------------------------------------
 * Q: "What if we can move diagonally?"
 * A: Add 4 more directions to check (8 total)
 * 
 * Q: "What if grid is 3D?"
 * A: Same approach, 6 directions instead of 4
 * 
 * Q: "Can you find the actual path, not just the time?"
 * A: Track parent pointers during Dijkstra, backtrack at end
 * 
 * Q: "What if elevations can be negative?"
 * A: Still works! Just adjust binary search range if needed
 * 
 * Q: "What if we want k-th best path instead of best?"
 * A: Much harder, need k-shortest paths algorithm
 * 
 * Q: "Can you optimize for very sparse connectivity?"
 * A: Union-Find approach naturally handles this
 * 
 * 13. OPTIMIZATION DISCUSSIONS:
 * -----------------------------
 * Q: "Can we do better than O(n² log n)?"
 * A: No, must examine all cells at least once (O(n²) lower bound)
 * 
 * Q: "What if grid is huge but destination is close?"
 * A: Dijkstra still optimal - explores only what's needed
 * 
 * Q: "Space optimization?"
 * A: Could use bit manipulation for visited if n is large
 * 
 * 14. KEY INSIGHTS TO MENTION:
 * ----------------------------
 * ✓ "This is a minimax path problem, not standard shortest path"
 * ✓ "Answer = maximum elevation on optimal path"
 * ✓ "We can use modified Dijkstra, tracking max instead of sum"
 * ✓ "Alternative: binary search on answer + BFS verification"
 * ✓ "Creative solution: Union-Find with cells sorted by elevation"
 * 
 * 15. TIME MANAGEMENT (45 MIN INTERVIEW):
 * ---------------------------------------
 * 0-5 min:   Understand problem, clarify constraints
 * 5-10 min:  Discuss approach (minimax path concept)
 * 10-25 min: Implement Dijkstra solution
 * 25-30 min: Walk through test case
 * 30-35 min: Discuss complexity
 * 35-40 min: Mention alternative approaches
 * 40-45 min: Handle follow-ups
 * 
 * Remember: This is a HARD problem. The key insight is recognizing it as
 * a minimax path problem. Once you have that, modified Dijkstra is natural!
 */
