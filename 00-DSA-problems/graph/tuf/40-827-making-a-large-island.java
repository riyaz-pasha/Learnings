/**
 * PROBLEM ANALYSIS:
 * ==================
 * Given an n x n binary matrix, change at most one 0 to 1 to maximize island size.
 * Need to find the largest possible island after this operation.
 * 
 * KEY INSIGHTS:
 * 1. Brute force: Try changing each 0 to 1 and count islands → O(n⁴) - TOO SLOW
 * 2. Better approach: 
 *    - First, identify all islands and their sizes
 *    - For each 0, check which unique islands it's adjacent to
 *    - The potential size = 1 + sum of adjacent island sizes
 * 3. Need to avoid counting same island multiple times (if 0 touches island from multiple sides)
 * 
 * WHY THIS WORKS:
 * - Pre-compute all island sizes with unique IDs
 * - When we flip a 0, it merges adjacent islands
 * - Use island IDs to avoid double-counting
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Start with brute force idea, recognize it's too slow
 * 2. Think: "Can we precompute something to make it faster?"
 * 3. Realize we need to know island sizes ahead of time
 * 4. Use DFS/BFS to label islands and track sizes
 * 5. For each 0, calculate potential merged island size
 * 
 * HOW TO COME UP WITH THIS SOLUTION:
 * ===================================
 * Step 1: Understand we need to try flipping each 0
 * Step 2: Realize counting islands after each flip is wasteful
 * Step 3: Think about what information we need:
 *         - Which islands exist?
 *         - What are their sizes?
 *         - Which islands are adjacent to each 0?
 * Step 4: Assign unique ID to each island during DFS
 * Step 5: For each 0, look at neighbors' island IDs
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class LargestIsland {
    
    // ============================================================================
    // APPROACH 1: DFS WITH ISLAND LABELING (OPTIMAL)
    // ============================================================================
    // Time Complexity: O(n²) - visit each cell constant times
    // Space Complexity: O(n²) - for grid labeling
    // This is THE solution for this problem
    
    /**
     * Main solution: Label islands with unique IDs, then try flipping each 0
     */
    public int largestIsland(int[][] grid) {
        int n = grid.length;
        
        // Map: island_id -> size of that island
        Map<Integer, Integer> islandSizes = new HashMap<>();
        
        // Label each island with unique ID (starting from 2)
        // 0 = water, 1 = unlabeled land, 2+ = labeled islands
        int islandId = 2;
        int maxSize = 0;
        
        // Phase 1: Label all islands and record their sizes
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int size = dfs(grid, i, j, islandId, n);
                    islandSizes.put(islandId, size);
                    maxSize = Math.max(maxSize, size); // Track max in case no 0 exists
                    islandId++;
                }
            }
        }
        
        // Phase 2: Try flipping each 0 and calculate potential island size
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) {
                    // Find all unique adjacent islands
                    Set<Integer> adjacentIslands = new HashSet<>();
                    
                    for (int k = 0; k < 4; k++) {
                        int ni = i + dx[k];
                        int nj = j + dy[k];
                        
                        if (ni >= 0 && ni < n && nj >= 0 && nj < n && grid[ni][nj] > 1) {
                            adjacentIslands.add(grid[ni][nj]);
                        }
                    }
                    
                    // Calculate potential size: 1 (flipped cell) + sum of adjacent islands
                    int potentialSize = 1;
                    for (int id : adjacentIslands) {
                        potentialSize += islandSizes.get(id);
                    }
                    
                    maxSize = Math.max(maxSize, potentialSize);
                }
            }
        }
        
        return maxSize;
    }
    
    /**
     * DFS to label an island with given ID and return its size
     */
    private int dfs(int[][] grid, int i, int j, int islandId, int n) {
        // Boundary check and water/already labeled check
        if (i < 0 || i >= n || j < 0 || j >= n || grid[i][j] != 1) {
            return 0;
        }
        
        // Label this cell
        grid[i][j] = islandId;
        
        // Count this cell + all connected cells
        int size = 1;
        size += dfs(grid, i - 1, j, islandId, n);
        size += dfs(grid, i + 1, j, islandId, n);
        size += dfs(grid, i, j - 1, islandId, n);
        size += dfs(grid, i, j + 1, islandId, n);
        
        return size;
    }
    
    // ============================================================================
    // APPROACH 2: UNION-FIND (ALTERNATIVE SOLUTION)
    // ============================================================================
    // Time Complexity: O(n²)
    // Space Complexity: O(n²)
    // Good if you want to practice Union-Find
    
    /**
     * Union-Find approach: Build connected components, then try flipping each 0
     */
    public int largestIslandUnionFind(int[][] grid) {
        int n = grid.length;
        UnionFind uf = new UnionFind(n * n);
        
        // Phase 1: Union all adjacent 1s
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int idx = i * n + j;
                    
                    // Check right neighbor
                    if (j + 1 < n && grid[i][j + 1] == 1) {
                        uf.union(idx, idx + 1);
                    }
                    
                    // Check down neighbor
                    if (i + 1 < n && grid[i + 1][j] == 1) {
                        uf.union(idx, idx + n);
                    }
                }
            }
        }
        
        // Find current max island size
        int maxSize = 0;
        for (int i = 0; i < n * n; i++) {
            if (grid[i / n][i % n] == 1) {
                maxSize = Math.max(maxSize, uf.getSize(i));
            }
        }
        
        // Phase 2: Try flipping each 0
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) {
                    Set<Integer> adjacentRoots = new HashSet<>();
                    
                    for (int k = 0; k < 4; k++) {
                        int ni = i + dx[k];
                        int nj = j + dy[k];
                        
                        if (ni >= 0 && ni < n && nj >= 0 && nj < n && grid[ni][nj] == 1) {
                            adjacentRoots.add(uf.find(ni * n + nj));
                        }
                    }
                    
                    int potentialSize = 1;
                    for (int root : adjacentRoots) {
                        potentialSize += uf.getSize(root);
                    }
                    
                    maxSize = Math.max(maxSize, potentialSize);
                }
            }
        }
        
        return maxSize;
    }
    
    /**
     * Union-Find with size tracking
     */
    class UnionFind {
        private int[] parent;
        private int[] size;
        
        public UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
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
                // Union by size
                if (size[rootX] < size[rootY]) {
                    parent[rootX] = rootY;
                    size[rootY] += size[rootX];
                } else {
                    parent[rootY] = rootX;
                    size[rootX] += size[rootY];
                }
            }
        }
        
        public int getSize(int x) {
            return size[find(x)];
        }
    }
    
    // ============================================================================
    // APPROACH 3: BFS WITH ISLAND LABELING (ITERATIVE ALTERNATIVE)
    // ============================================================================
    // Time Complexity: O(n²)
    // Space Complexity: O(n²)
    // Use if interviewer prefers iterative solutions
    
    /**
     * BFS version of island labeling
     */
    public int largestIslandBFS(int[][] grid) {
        int n = grid.length;
        Map<Integer, Integer> islandSizes = new HashMap<>();
        int islandId = 2;
        int maxSize = 0;
        
        // Phase 1: Label islands using BFS
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    int size = bfs(grid, i, j, islandId, n);
                    islandSizes.put(islandId, size);
                    maxSize = Math.max(maxSize, size);
                    islandId++;
                }
            }
        }
        
        // Phase 2: Try flipping each 0
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) {
                    Set<Integer> adjacentIslands = new HashSet<>();
                    
                    for (int k = 0; k < 4; k++) {
                        int ni = i + dx[k];
                        int nj = j + dy[k];
                        
                        if (ni >= 0 && ni < n && nj >= 0 && nj < n && grid[ni][nj] > 1) {
                            adjacentIslands.add(grid[ni][nj]);
                        }
                    }
                    
                    int potentialSize = 1;
                    for (int id : adjacentIslands) {
                        potentialSize += islandSizes.get(id);
                    }
                    
                    maxSize = Math.max(maxSize, potentialSize);
                }
            }
        }
        
        return maxSize;
    }
    
    /**
     * BFS to label island and return size
     */
    private int bfs(int[][] grid, int startI, int startJ, int islandId, int n) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startI, startJ});
        grid[startI][startJ] = islandId;
        
        int size = 0;
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int i = cell[0];
            int j = cell[1];
            size++;
            
            for (int k = 0; k < 4; k++) {
                int ni = i + dx[k];
                int nj = j + dy[k];
                
                if (ni >= 0 && ni < n && nj >= 0 && nj < n && grid[ni][nj] == 1) {
                    grid[ni][nj] = islandId;
                    queue.offer(new int[]{ni, nj});
                }
            }
        }
        
        return size;
    }
    
    // ============================================================================
    // TEST CASES WITH DETAILED EXPLANATIONS
    // ============================================================================
    
    public static void main(String[] args) {
        LargestIsland solution = new LargestIsland();
        
        // Test Case 1: Merge two islands
        System.out.println("Test 1: Merge two islands");
        int[][] grid1 = {{1, 0}, {0, 1}};
        System.out.println("Grid: [[1,0],[0,1]]");
        System.out.println("Expected: 3 (flip (0,1) or (1,0) to connect both islands)");
        System.out.println("Got: " + solution.largestIsland(grid1));
        /*
         * Initial state: Two separate islands of size 1 each
         * Flip (0,1): Connects (0,0) and (1,1) → size 3
         * Flip (1,0): Connects (0,0) and (1,1) → size 3
         */
        
        // Test Case 2: Expand existing island
        System.out.println("\nTest 2: Expand existing island");
        int[][] grid2 = {{1, 1}, {1, 0}};
        System.out.println("Grid: [[1,1],[1,0]]");
        System.out.println("Expected: 4 (flip (1,1) to make entire grid one island)");
        System.out.println("Got: " + solution.largestIsland(grid2));
        /*
         * Initial: One island of size 3
         * Flip (1,1): Adds to existing island → size 4
         */
        
        // Test Case 3: No 0s to flip
        System.out.println("\nTest 3: Already all 1s");
        int[][] grid3 = {{1, 1}, {1, 1}};
        System.out.println("Grid: [[1,1],[1,1]]");
        System.out.println("Expected: 4 (no flip needed)");
        System.out.println("Got: " + solution.largestIsland(grid3));
        /*
         * Grid is already one complete island
         * No improvement possible
         */
        
        // Test Case 4: All 0s
        System.out.println("\nTest 4: All water");
        int[][] grid4 = {{0, 0}, {0, 0}};
        System.out.println("Grid: [[0,0],[0,0]]");
        System.out.println("Expected: 1 (flip any cell creates island of size 1)");
        System.out.println("Got: " + solution.largestIsland(grid4));
        /*
         * No existing islands
         * Flipping any cell creates size 1 island
         */
        
        // Test Case 5: Merge three islands
        System.out.println("\nTest 5: Merge three islands");
        int[][] grid5 = {
            {1, 0, 1},
            {0, 0, 0},
            {1, 0, 1}
        };
        System.out.println("Grid: [[1,0,1],[0,0,0],[1,0,1]]");
        System.out.println("Expected: 5 (flip center (1,1) to merge all four corners)");
        System.out.println("Got: " + solution.largestIsland(grid5));
        /*
         * Four separate islands of size 1
         * Flipping (1,1) connects all four → size 5
         */
        
        // Test Case 6: Strategic flip location matters
        System.out.println("\nTest 6: Best flip location");
        int[][] grid6 = {
            {1, 1, 0},
            {1, 0, 1},
            {0, 1, 1}
        };
        System.out.println("Grid: [[1,1,0],[1,0,1],[0,1,1]]");
        System.out.println("Expected: 7 (flip (1,1) merges two islands)");
        System.out.println("Got: " + solution.largestIsland(grid6));
        /*
         * Island 1: (0,0),(0,1),(1,0) - size 3
         * Island 2: (1,2),(2,1),(2,2) - size 3
         * Flipping (1,1) merges both → 3 + 1 + 3 = 7
         */
        
        // Test Case 7: Single cell
        System.out.println("\nTest 7: Single cell grid");
        int[][] grid7 = {{0}};
        System.out.println("Grid: [[0]]");
        System.out.println("Expected: 1");
        System.out.println("Got: " + solution.largestIsland(grid7));
        
        // Test Case 8: Large connected island with one gap
        System.out.println("\nTest 8: Almost complete island");
        int[][] grid8 = {
            {1, 1, 1},
            {1, 0, 1},
            {1, 1, 1}
        };
        System.out.println("Grid: [[1,1,1],[1,0,1],[1,1,1]]");
        System.out.println("Expected: 9 (flip center to complete)");
        System.out.println("Got: " + solution.largestIsland(grid8));
        /*
         * One large island of size 8 surrounding a 0
         * Flipping the center makes it size 9
         */
    }
}

/**
 * COMPREHENSIVE INTERVIEW GUIDE:
 * ===============================
 * 
 * 1. PROBLEM RECOGNITION:
 * ----------------------
 * Key phrases that trigger this pattern:
 * - "Change at most one" → Try all possibilities, but efficiently
 * - "Maximize island size" → Need to consider all flips
 * - "4-directionally connected" → DFS/BFS for island detection
 * 
 * 2. INITIAL OBSERVATIONS TO MENTION:
 * -----------------------------------
 * a) "Brute force would be O(n⁴): for each 0, flip it and count islands"
 * b) "We can do better by preprocessing: label islands first"
 * c) "Key insight: when we flip a 0, we're merging adjacent islands"
 * d) "Need to avoid double-counting if a 0 touches same island multiple times"
 * 
 * 3. SOLUTION WALKTHROUGH:
 * ------------------------
 * Step 1: Label all islands with unique IDs (2, 3, 4, ...)
 *         - Use DFS/BFS starting from each unlabeled 1
 *         - Track size of each island in a HashMap
 * 
 * Step 2: For each 0 in the grid:
 *         - Check its 4 neighbors
 *         - Collect unique island IDs (use Set to avoid duplicates)
 *         - Calculate: 1 + sum of adjacent island sizes
 * 
 * Step 3: Return maximum size found
 *         - Don't forget: max might be existing island if no 0s
 * 
 * 4. WHY LABELING WORKS:
 * ----------------------
 * Without labeling:
 *   Grid: [[1,1],[1,0]]
 *   If we flip (1,1), need to count island size → O(n²)
 * 
 * With labeling:
 *   Grid after labeling: [[2,2],[2,0]]
 *   Island 2 has size 3 (stored in map)
 *   Flip (1,1): neighbors = {2}, size = 1 + 3 = 4 → O(1)
 * 
 * 5. COMPLEXITY ANALYSIS:
 * -----------------------
 * Time Complexity: O(n²)
 *   - Phase 1 (Labeling): O(n²) - visit each cell once
 *   - Phase 2 (Try flips): O(n²) - check each cell, O(4) neighbor checks
 *   - Total: O(n²)
 * 
 * Space Complexity: O(n²)
 *   - Grid storage (can modify in-place if allowed)
 *   - Island sizes map: O(number of islands) ≤ O(n²)
 *   - DFS recursion stack: O(n²) worst case
 * 
 * 6. EDGE CASES TO DISCUSS:
 * -------------------------
 * ✓ All 1s (no 0 to flip) → return n²
 * ✓ All 0s (no islands) → return 1
 * ✓ Single cell grid → return 1
 * ✓ No 0 touches any island → max is largest existing island
 * ✓ One 0 touches same island from multiple sides → use Set to avoid double-count
 * ✓ Grid with multiple disconnected islands
 * 
 * 7. COMMON MISTAKES TO AVOID:
 * ----------------------------
 * ✗ Forgetting to track max of existing islands (when no good flip exists)
 * ✗ Double-counting an island when 0 touches it from multiple sides
 * ✗ Starting island IDs from 0 or 1 (conflicts with grid values)
 * ✗ Not checking bounds before accessing neighbors
 * ✗ Modifying original grid without permission (ask interviewer)
 * ✗ Forgetting the "+1" when calculating potential size (the flipped cell itself)
 * 
 * 8. OPTIMIZATION DISCUSSIONS:
 * ----------------------------
 * Q: "Can we do better than O(n²)?"
 * A: No, we must examine every cell at least once
 * 
 * Q: "Can we save space?"
 * A: If allowed to modify input, we can label in-place
 *    Otherwise, O(n²) is necessary for tracking
 * 
 * Q: "What if we can flip k cells instead of 1?"
 * A: Much harder! Would need different approach (possibly DP or advanced search)
 * 
 * Q: "What about diagonal connections?"
 * A: Just add 4 more directions to check (8-directional)
 * 
 * 9. DFS VS BFS VS UNION-FIND:
 * ----------------------------
 * DFS: 
 *   + Simple to implement
 *   + Clean recursion
 *   - Stack overflow risk for very large islands
 * 
 * BFS:
 *   + Iterative (no stack overflow)
 *   + Level-by-level exploration
 *   - Slightly more code
 * 
 * Union-Find:
 *   + Good for thinking about connected components
 *   + Useful if problem extends to dynamic updates
 *   - More complex implementation
 *   - Not significantly better here
 * 
 * Recommendation: Use DFS (simplest) or BFS (safest)
 * 
 * 10. INTERVIEW COMMUNICATION STRATEGY:
 * -------------------------------------
 * 1. Start: "This is about maximizing island size with one flip"
 * 2. Brute force: "We could try every 0 and recount, but that's O(n⁴)"
 * 3. Key insight: "We can precompute island sizes with unique labels"
 * 4. Draw example: Show labeling process on small grid
 * 5. Walk through: "For each 0, we check 4 neighbors and sum unique islands"
 * 6. Edge cases: "Need to handle all 1s, all 0s, and avoid double-counting"
 * 7. Complexity: "O(n²) time and space, which is optimal"
 * 
 * 11. VARIANTS OF THIS PROBLEM:
 * -----------------------------
 * - "Flip k cells" → More complex, need different strategy
 * - "Minimize island count" → Similar approach, different objective
 * - "Maximum perimeter after flip" → Track perimeter during labeling
 * - "3D grid" → Same logic, 6 directions instead of 4
 * 
 * 12. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * --------------------------------------
 * - "What if grid is very sparse (mostly 0s)?" → Still O(n²)
 * - "Can you return which cell to flip?" → Track argmax during phase 2
 * - "What if we can flip 0→1 or 1→0?" → Need to try both operations
 * - "How would you parallelize this?" → Phase 1 harder, Phase 2 easy
 * 
 * 13. CODE QUALITY CHECKLIST:
 * ---------------------------
 * ✓ Clean helper methods (dfs/bfs separate)
 * ✓ Meaningful variable names (islandId, not id)
 * ✓ Constants for directions (reusable)
 * ✓ Clear phase separation in comments
 * ✓ Handle all edge cases gracefully
 * ✓ Efficient data structures (HashSet for uniqueness)
 * 
 * 14. TIME MANAGEMENT (45 MIN INTERVIEW):
 * ---------------------------------------
 * 0-5 min:   Understand problem, clarify edge cases
 * 5-10 min:  Explain approach with example
 * 10-30 min: Code solution (phase 1 & 2)
 * 30-35 min: Walk through test cases
 * 35-40 min: Discuss complexity and optimizations
 * 40-45 min: Handle follow-up questions
 * 
 * Remember: This is a MEDIUM problem, but labeling technique is the key insight.
 * If you can identify that we need to "mark islands with unique IDs", you're 80% there!
 */


/*
 ============================================================
 Making A Large Island — UNION FIND VERSION
 ============================================================

 We DO NOT modify the grid values.
 Grid stays 0 / 1 throughout.

 Instead:
 - Each cell has an index: (r * n + c)
 - Union adjacent land cells
 - Track component sizes in DSU

 ============================================================
 */

class MakingALargeIslandUF {

    static class UnionFind {
        int[] parent;
        int[] size;

        UnionFind(int n) {
            parent = new int[n];
            size = new int[n];

            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // path compression
            }
            return parent[x];
        }

        void union(int x, int y) {
            int px = find(x);
            int py = find(y);

            if (px == py) return;

            // union by size
            if (size[px] < size[py]) {
                parent[px] = py;
                size[py] += size[px];
            } else {
                parent[py] = px;
                size[px] += size[py];
            }
        }
    }

    public static int largestIsland(int[][] grid) {
        int n = grid.length;
        UnionFind uf = new UnionFind(n * n);

        int[][] dirs = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        /*
         ------------------------------------------------------------
         PHASE 1: Union adjacent land cells
         ------------------------------------------------------------
         */
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 1) {
                    int id1 = r * n + c;

                    for (int[] d : dirs) {
                        int nr = r + d[0];
                        int nc = c + d[1];

                        if (nr < 0 || nr >= n || nc < 0 || nc >= n)
                            continue;

                        if (grid[nr][nc] == 1) {
                            int id2 = nr * n + nc;
                            uf.union(id1, id2);
                        }
                    }
                }
            }
        }

        int maxArea = 0;
        boolean hasZero = false;

        /*
         ------------------------------------------------------------
         PHASE 2: Try flipping each 0
         ------------------------------------------------------------
         */
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {

                if (grid[r][c] == 0) {
                    hasZero = true;
                    Set<Integer> seenParents = new HashSet<>();
                    int newArea = 1; // flipped cell

                    for (int[] d : dirs) {
                        int nr = r + d[0];
                        int nc = c + d[1];

                        if (nr < 0 || nr >= n || nc < 0 || nc >= n)
                            continue;

                        if (grid[nr][nc] == 1) {
                            int parent = uf.find(nr * n + nc);
                            if (seenParents.add(parent)) {
                                newArea += uf.size[parent];
                            }
                        }
                    }

                    maxArea = Math.max(maxArea, newArea);
                }
            }
        }

        /*
         ------------------------------------------------------------
         EDGE CASE: All cells are 1
         ------------------------------------------------------------
         */
        if (!hasZero) {
            return n * n;
        }

        return maxArea;
    }

    /*
     ------------------------------------------------------------
     Driver
     ------------------------------------------------------------
     */
    public static void main(String[] args) {
        int[][] grid1 = {{1,0},{0,1}};
        int[][] grid2 = {{1,1},{1,0}};
        int[][] grid3 = {{1,1},{1,1}};

        System.out.println(largestIsland(grid1)); // 3
        System.out.println(largestIsland(grid2)); // 4
        System.out.println(largestIsland(grid3)); // 4
    }
}
