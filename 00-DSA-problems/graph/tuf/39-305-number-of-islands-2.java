/**
 * PROBLEM ANALYSIS:
 * ==================
 * Given an m x n grid initially filled with water, we add land one position at a time.
 * After each addition, we need to count the number of islands.
 * An island is surrounded by water and formed by connecting adjacent lands (horizontally/vertically).
 * 
 * KEY INSIGHTS:
 * 1. This is a DYNAMIC connectivity problem - islands merge as we add land
 * 2. Union-Find is PERFECT for this - it efficiently handles merging components
 * 3. We need to track count after EACH addition (not just final state)
 * 4. Need to handle duplicate positions (adding land where land already exists)
 * 
 * WHY UNION-FIND?
 * - Each land cell can be part of exactly one island (connected component)
 * - When we add land, we may merge up to 4 existing islands (4 directions)
 * - Union-Find tracks components efficiently with nearly O(1) operations
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Recognize this as "Number of Islands" but with DYNAMIC updates
 * 2. Static DFS/BFS won't work - too slow to recompute after each addition
 * 3. Need incremental solution → Union-Find
 * 4. Handle edge cases: duplicates, out of bounds, merging islands
 * 
 * HOW TO COME UP WITH THIS SOLUTION:
 * ===================================
 * Step 1: Realize brute force (DFS after each add) is O(k * m * n) - too slow
 * Step 2: Need to track components dynamically → Union-Find
 * Step 3: When adding land:
 *         - If water → becomes new island (count++)
 *         - Check 4 neighbors, union with existing land
 *         - Each successful union reduces count by 1
 * Step 4: Handle 2D grid → convert to 1D index: row * cols + col
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NumberOfIslandsII {
    
    // ============================================================================
    // APPROACH 1: UNION-FIND (OPTIMAL SOLUTION)
    // ============================================================================
    // Time Complexity: O(k * α(m*n)) where k = positions.length, α = inverse Ackermann
    //                  Practically O(k) since α is nearly constant
    // Space Complexity: O(m * n) for parent and rank arrays
    // This is THE solution for this problem
    
    /**
     * Main solution using Union-Find to track dynamic island formation
     */
    public List<Integer> numIslands2(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        
        // Union-Find with count tracking
        UnionFind uf = new UnionFind(m * n);
        
        // Track which cells have land (initially all water)
        boolean[][] hasLand = new boolean[m][n];
        
        // Four directions: up, down, left, right
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] pos : positions) {
            int row = pos[0];
            int col = pos[1];
            
            // Edge case: duplicate position (land already exists)
            if (hasLand[row][col]) {
                result.add(uf.getCount());
                continue;
            }
            
            // Add new land - increases island count by 1 initially
            hasLand[row][col] = true;
            int index = row * n + col;
            uf.addLand(index);
            
            // Check all 4 neighbors
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                // Check bounds and if neighbor is land
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n 
                    && hasLand[newRow][newCol]) {
                    
                    int neighborIndex = newRow * n + newCol;
                    uf.union(index, neighborIndex);
                }
            }
            
            result.add(uf.getCount());
        }
        
        return result;
    }
    
    /**
     * Union-Find optimized for this problem
     * Key feature: tracks count of components dynamically
     */
    class UnionFind {
        private int[] parent;
        private int[] rank;
        private int count; // Number of islands
        
        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            count = 0;
            
            // Initialize: -1 means water (not initialized)
            Arrays.fill(parent, -1);
        }
        
        /**
         * Add new land cell - initially it's its own island
         */
        public void addLand(int x) {
            if (parent[x] != -1) {
                return; // Already land
            }
            parent[x] = x;
            rank[x] = 1;
            count++; // New island formed
        }
        
        /**
         * Find with path compression
         */
        public int find(int x) {
            if (parent[x] == -1) {
                return -1; // Water cell
            }
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }
        
        /**
         * Union two land cells - merges islands if they're different
         */
        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            // Both must be land and in different islands
            if (rootX == -1 || rootY == -1 || rootX == rootY) {
                return;
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
            
            count--; // Merged two islands into one
        }
        
        public int getCount() {
            return count;
        }
    }
    
    // ============================================================================
    // APPROACH 2: UNION-FIND WITH HASHMAP (SPACE OPTIMIZED)
    // ============================================================================
    // Time Complexity: O(k * α(k)) - only track actual land cells
    // Space Complexity: O(k) - much better when grid is sparse
    // Use this when m*n is very large but k is small
    
    /**
     * Space-optimized version using HashMap instead of arrays
     * Only stores land cells - great for sparse grids
     */
    public List<Integer> numIslands2Optimized(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        UnionFindMap uf = new UnionFindMap();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        Set<String> land = new HashSet<>();
        
        for (int[] pos : positions) {
            int row = pos[0];
            int col = pos[1];
            String key = row + "," + col;
            
            // Handle duplicate
            if (land.contains(key)) {
                result.add(uf.getCount());
                continue;
            }
            
            // Add new land
            land.add(key);
            uf.addLand(key);
            
            // Check neighbors
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                    String neighborKey = newRow + "," + newCol;
                    if (land.contains(neighborKey)) {
                        uf.union(key, neighborKey);
                    }
                }
            }
            
            result.add(uf.getCount());
        }
        
        return result;
    }
    
    /**
     * HashMap-based Union-Find for sparse data
     */
    class UnionFindMap {
        private Map<String, String> parent = new HashMap<>();
        private Map<String, Integer> rank = new HashMap<>();
        private int count = 0;
        
        public void addLand(String x) {
            if (parent.containsKey(x)) {
                return;
            }
            parent.put(x, x);
            rank.put(x, 1);
            count++;
        }
        
        public String find(String x) {
            if (!parent.containsKey(x)) {
                return null;
            }
            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x)));
            }
            return parent.get(x);
        }
        
        public void union(String x, String y) {
            String rootX = find(x);
            String rootY = find(y);
            
            if (rootX == null || rootY == null || rootX.equals(rootY)) {
                return;
            }
            
            int rankX = rank.get(rootX);
            int rankY = rank.get(rootY);
            
            if (rankX < rankY) {
                parent.put(rootX, rootY);
            } else if (rankX > rankY) {
                parent.put(rootY, rootX);
            } else {
                parent.put(rootY, rootX);
                rank.put(rootX, rankX + 1);
            }
            
            count--;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    // ============================================================================
    // APPROACH 3: BRUTE FORCE DFS (FOR COMPARISON - NOT RECOMMENDED)
    // ============================================================================
    // Time Complexity: O(k * m * n) - recompute islands after each addition
    // Space Complexity: O(m * n)
    // Only use if interviewer insists or for small inputs
    
    /**
     * Brute force: run DFS after each land addition
     * TOO SLOW for large inputs but helps understand the problem
     */
    public List<Integer> numIslands2BruteForce(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        int[][] grid = new int[m][n];
        
        for (int[] pos : positions) {
            int row = pos[0];
            int col = pos[1];
            
            grid[row][col] = 1; // Add land
            
            // Count islands using DFS
            int count = countIslandsDFS(grid, m, n);
            result.add(count);
        }
        
        return result;
    }
    
    private int countIslandsDFS(int[][] grid, int m, int n) {
        boolean[][] visited = new boolean[m][n];
        int count = 0;
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    dfs(grid, visited, i, j, m, n);
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private void dfs(int[][] grid, boolean[][] visited, int row, int col, int m, int n) {
        if (row < 0 || row >= m || col < 0 || col >= n 
            || visited[row][col] || grid[row][col] == 0) {
            return;
        }
        
        visited[row][col] = true;
        dfs(grid, visited, row - 1, col, m, n);
        dfs(grid, visited, row + 1, col, m, n);
        dfs(grid, visited, row, col - 1, m, n);
        dfs(grid, visited, row, col + 1, m, n);
    }
    
    // ============================================================================
    // TEST CASES WITH DETAILED EXPLANATION
    // ============================================================================
    
    public static void main(String[] args) {
        NumberOfIslandsII solution = new NumberOfIslandsII();
        
        // Test Case 1: Basic example with merging
        System.out.println("Test 1:");
        int[][] positions1 = {{0,0},{0,1},{1,2},{2,1}};
        List<Integer> result1 = solution.numIslands2(3, 3, positions1);
        System.out.println("Expected: [1, 1, 2, 3]");
        System.out.println("Got:      " + result1);
        /*
         * Explanation:
         * Add (0,0): 1 island
         * Add (0,1): connects to (0,0) → still 1 island
         * Add (1,2): not connected → 2 islands
         * Add (2,1): not connected → 3 islands
         */
        
        // Test Case 2: All positions merge into one island
        System.out.println("\nTest 2:");
        int[][] positions2 = {{0,0},{0,1},{1,0},{1,1}};
        List<Integer> result2 = solution.numIslands2(2, 2, positions2);
        System.out.println("Expected: [1, 1, 1, 1]");
        System.out.println("Got:      " + result2);
        /*
         * All positions are adjacent, forming one connected island
         */
        
        // Test Case 3: Duplicate positions
        System.out.println("\nTest 3:");
        int[][] positions3 = {{0,0},{0,0},{1,1}};
        List<Integer> result3 = solution.numIslands2(2, 2, positions3);
        System.out.println("Expected: [1, 1, 2]");
        System.out.println("Got:      " + result3);
        /*
         * Second (0,0) is duplicate - count stays same
         */
        
        // Test Case 4: Complex merging scenario
        System.out.println("\nTest 4:");
        int[][] positions4 = {{0,0},{1,1},{0,1}};
        List<Integer> result4 = solution.numIslands2(2, 2, positions4);
        System.out.println("Expected: [1, 2, 1]");
        System.out.println("Got:      " + result4);
        /*
         * Add (0,0): 1 island
         * Add (1,1): not connected → 2 islands
         * Add (0,1): connects both → 1 island (merges two islands)
         */
        
        // Test Case 5: Empty positions
        System.out.println("\nTest 5:");
        int[][] positions5 = {};
        List<Integer> result5 = solution.numIslands2(3, 3, positions5);
        System.out.println("Expected: []");
        System.out.println("Got:      " + result5);
        
        // Test Case 6: Single cell grid
        System.out.println("\nTest 6:");
        int[][] positions6 = {{0,0}};
        List<Integer> result6 = solution.numIslands2(1, 1, positions6);
        System.out.println("Expected: [1]");
        System.out.println("Got:      " + result6);
        
        // Test optimized version with large sparse grid
        System.out.println("\nTest 7 (Optimized for sparse grid):");
        int[][] positions7 = {{0,0},{1000,1000},{2000,2000}};
        List<Integer> result7 = solution.numIslands2Optimized(3000, 3000, positions7);
        System.out.println("Expected: [1, 2, 3]");
        System.out.println("Got:      " + result7);
        /*
         * Demonstrates space efficiency: O(k) instead of O(m*n)
         */
    }
}

/**
 * COMPREHENSIVE INTERVIEW GUIDE:
 * ===============================
 * 
 * 1. PROBLEM RECOGNITION:
 * ----------------------
 * When you see "Number of Islands" + "adding positions dynamically":
 * → Immediately think Union-Find
 * → This is NOT a regular DFS/BFS problem
 * 
 * 2. KEY OBSERVATIONS TO MENTION:
 * -------------------------------
 * a) "This is a dynamic connectivity problem"
 * b) "We need incremental updates, not full recomputation"
 * c) "Union-Find is perfect because it tracks connected components efficiently"
 * d) "Each land addition may merge up to 4 existing islands"
 * 
 * 3. SOLUTION WALKTHROUGH:
 * ------------------------
 * Step 1: Initialize Union-Find with m*n size
 * Step 2: Track which cells have land (boolean array or set)
 * Step 3: For each position:
 *         - If duplicate, just add current count
 *         - Otherwise, add land (count++)
 *         - Check 4 neighbors, union with any adjacent land
 *         - Each successful union decreases count
 * 
 * 4. COMPLEXITY ANALYSIS:
 * -----------------------
 * Time: O(k * α(m*n)) ≈ O(k) practically
 *       - k operations, each with nearly O(1) union-find ops
 * Space: O(m*n) for parent/rank arrays
 *        - Can optimize to O(k) with HashMap if grid is sparse
 * 
 * 5. EDGE CASES TO DISCUSS:
 * -------------------------
 * ✓ Duplicate positions (land already exists)
 * ✓ Empty positions array
 * ✓ Single cell grid
 * ✓ All positions form one island
 * ✓ No adjacent positions (all separate islands)
 * ✓ Positions that merge multiple islands
 * ✓ Out of bounds (if input validation needed)
 * 
 * 6. COMMON MISTAKES TO AVOID:
 * ----------------------------
 * ✗ Forgetting to handle duplicates
 * ✗ Not checking if neighbor is land before union
 * ✗ Incrementing count during union (should decrement)
 * ✗ Using parent[x] = -1 without checking in find()
 * ✗ Not using path compression (makes it slower)
 * ✗ Trying to use DFS/BFS (too slow)
 * 
 * 7. OPTIMIZATION DISCUSSIONS:
 * ----------------------------
 * Q: "Can we optimize space?"
 * A: Yes! Use HashMap when m*n >> k (sparse grid)
 *    Space reduces from O(m*n) to O(k)
 * 
 * Q: "What if positions can be added and removed?"
 * A: Much harder! Would need different approach (possibly segment tree)
 * 
 * Q: "What about diagonal connections?"
 * A: Just add 4 more directions: {{-1,-1},{-1,1},{1,-1},{1,1}}
 * 
 * 8. COMPARISON WITH SIMILAR PROBLEMS:
 * ------------------------------------
 * - Number of Islands I: Static grid → Use DFS/BFS
 * - Number of Islands II: Dynamic additions → Use Union-Find
 * - Max Area of Island: Need component sizes → Union-Find with size tracking
 * - Surrounded Regions: Need boundary info → DFS from borders
 * 
 * 9. INTERVIEW COMMUNICATION TIPS:
 * --------------------------------
 * ✓ Start with brute force: "We could DFS after each add, but that's O(k*m*n)"
 * ✓ Explain why Union-Find: "We need to track components dynamically"
 * ✓ Walk through an example: Show how islands merge
 * ✓ Mention optimizations: HashMap for sparse grids
 * ✓ Discuss trade-offs: Space vs implementation complexity
 * 
 * 10. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * --------------------------------------
 * - "What if we can also remove land?" (Much harder)
 * - "Can you find the largest island at each step?" (Track component sizes)
 * - "What if grid is 3D?" (Same approach, 6 directions)
 * - "What about concurrent updates?" (Need thread-safe Union-Find)
 * 
 * 11. CODE QUALITY CHECKLIST:
 * ---------------------------
 * ✓ Clear variable names
 * ✓ Helper methods for 2D→1D conversion
 * ✓ Path compression in find()
 * ✓ Union by rank
 * ✓ Handle edge cases gracefully
 * ✓ Add comments for tricky parts
 * 
 * 12. TIME MANAGEMENT:
 * -------------------
 * 0-5 min:  Understand problem, ask clarifying questions
 * 5-10 min: Explain approach and why Union-Find
 * 10-30 min: Implement solution
 * 30-35 min: Walk through test cases
 * 35-40 min: Discuss optimizations and edge cases
 * 
 * Remember: This is a HARD problem on LeetCode. If you can code Union-Find
 * correctly and explain the intuition, you're doing excellent!
 */


/*
 ============================================================
 LeetCode 305: Number of Islands II
 ============================================================

 Core Idea:
 ----------
 This is a DYNAMIC CONNECTIVITY problem.

 We start with an empty grid (all water).
 Each operation ADDS land at a given position.
 After each addition, we must return the number of islands.

 DFS/BFS after each add is too slow.
 Union-Find (Disjoint Set Union) is the correct tool.

 ============================================================
 */

class NumberOfIslandsII2 {

    /*
     ------------------------------------------------------------
     Union-Find (Disjoint Set Union) with Path Compression
     ------------------------------------------------------------
     */
    static class UnionFind {
        int[] parent;
        int[] rank;

        UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];

            // Initialize all nodes as "not connected yet"
            // We will activate them lazily when land is added
            Arrays.fill(parent, -1);
        }

        /*
         Find with path compression
         */
        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        /*
         Union two components
         Returns true if a merge happened
         Returns false if already connected
         */
        boolean union(int x, int y) {
            int px = find(x);
            int py = find(y);

            if (px == py) return false;

            // Union by rank
            if (rank[px] < rank[py]) {
                parent[px] = py;
            } else if (rank[px] > rank[py]) {
                parent[py] = px;
            } else {
                parent[py] = px;
                rank[px]++;
            }
            return true;
        }

        /*
         Activate a node (turn water into land)
         */
        void activate(int x) {
            parent[x] = x;
            rank[x] = 0;
        }

        /*
         Check if a node is active land
         */
        boolean isActive(int x) {
            return parent[x] != -1;
        }
    }

    /*
     ============================================================
     MAIN SOLUTION
     ============================================================
     */
    public static List<Integer> numIslands2(int m, int n, int[][] positions) {

        List<Integer> result = new ArrayList<>();
        UnionFind uf = new UnionFind(m * n);

        int islands = 0;

        // Directions: up, down, left, right
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        for (int[] pos : positions) {
            int r = pos[0];
            int c = pos[1];

            int index = r * n + c;

            // Case 1: duplicate add → no change
            if (uf.isActive(index)) {
                result.add(islands);
                continue;
            }

            // Case 2: new land
            uf.activate(index);
            islands++; // new island initially

            // Try to union with 4 neighbors
            for (int[] d : directions) {
                int nr = r + d[0];
                int nc = c + d[1];

                // Boundary check
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;

                int neighborIndex = nr * n + nc;

                // Only union with ACTIVE land neighbors
                if (uf.isActive(neighborIndex)) {
                    // If union happened, we merged two islands
                    if (uf.union(index, neighborIndex)) {
                        islands--;
                    }
                }
            }

            result.add(islands);
        }

        return result;
    }

    /*
     ============================================================
     DRIVER (for local testing)
     ============================================================
     */
    public static void main(String[] args) {
        int m = 3, n = 3;
        int[][] positions = {
            {0,0},
            {0,1},
            {1,2},
            {2,1}
        };

        System.out.println(numIslands2(m, n, positions));
        // Expected: [1, 1, 2, 3]
    }
}
