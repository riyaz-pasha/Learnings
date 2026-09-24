import java.util.*;

class NumberOfIslandsII {

    static class UnionFind {
        int[] parent;
        int[] rank;
        int count; // number of connected components (islands)

        UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            Arrays.fill(parent, -1); // -1 means water (not added yet)
            count = 0;
        }

        public boolean isLand(int x) {
            return parent[x] != -1;
        }

        public void addLand(int x) {
            if (parent[x] != -1) return; // already land
            parent[x] = x;
            rank[x] = 0;
            count++;
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public void union(int a, int b) {
            int pa = find(a);
            int pb = find(b);

            if (pa == pb) return;

            if (rank[pa] < rank[pb]) {
                parent[pa] = pb;
            } else if (rank[pb] < rank[pa]) {
                parent[pb] = pa;
            } else {
                parent[pb] = pa;
                rank[pa]++;
            }

            count--; // merged two islands into one
        }
    }

    public List<Integer> numIslands2(int m, int n, int[][] positions) {

        UnionFind uf = new UnionFind(m * n);
        List<Integer> result = new ArrayList<>();

        int[][] dirs = {
                {1, 0}, {-1, 0},
                {0, 1}, {0, -1}
        };

        for (int[] pos : positions) {
            int r = pos[0];
            int c = pos[1];

            int id = r * n + c;

            // duplicate land
            if (uf.isLand(id)) {
                result.add(uf.count);
                continue;
            }

            uf.addLand(id);

            // union with 4 neighbors if they are land
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];

                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;

                int nid = nr * n + nc;

                if (uf.isLand(nid)) {
                    uf.union(id, nid);
                }
            }

            result.add(uf.count);
        }

        return result;
    }

    /*
     * ⏱️ Complexity
     * Let P = positions.length
     * Each union/find is almost O(1): α(m*n)
     * Total: O(P * α(m*n)) ≈ O(P)
     * Space: O(m*n)
     */
}

/**
 * PROBLEM: Number of Islands II (LeetCode 305) - HARD
 * 
 * ============================================================================
 * INTERVIEW APPROACH - PATTERN RECOGNITION
 * ============================================================================
 * 
 * 1. PROBLEM RECOGNITION (First 30 seconds):
 *    Keywords: "add land", "count islands", "dynamic/incremental"
 *    Pattern: UNION-FIND (Disjoint Set Union)
 *    
 *    RED FLAGS that scream Union-Find:
 *    - Dynamic connectivity (adding elements over time)
 *    - Need to track connected components after each operation
 *    - "Merge" or "group" operations
 *    - Need to answer "how many groups?" repeatedly
 * 
 * 2. WHY NOT DFS/BFS?
 *    - DFS/BFS would require O(m*n) per operation
 *    - With k operations, total would be O(k*m*n) - TOO SLOW
 *    - Union-Find gives us O(k*α(m*n)) which is nearly O(k)
 * 
 * 3. KEY INSIGHT:
 *    "Each land cell is a node. When we add land, we union it with
 *     adjacent land cells. Track number of components."
 * 
 * 4. CRITICAL QUESTIONS TO ASK:
 *    - Can we add land to same position twice? (Problem says duplicate positions possible)
 *    - Are m,n guaranteed to be positive? (Yes)
 *    - Can positions be out of bounds? (No, guaranteed valid)
 *    - Return format? (List of island counts after each operation)
 * 
 * 5. EDGE CASES:
 *    - Empty positions array
 *    - Single position
 *    - Duplicate positions (should not increase count)
 *    - All positions form one island
 *    - All positions are isolated islands
 *    - Positions that merge multiple islands into one
 * 
 * ============================================================================
 * COMPLEXITY ANALYSIS (Say this upfront in interview)
 * ============================================================================
 * 
 * Time: O(k * α(m*n)) where k = number of operations, α = inverse Ackermann
 *       - α is effectively constant (< 5 for all practical purposes)
 *       - So practically O(k)
 * 
 * Space: O(m*n) for Union-Find arrays
 * 
 * Compare to naive DFS approach: O(k * m * n) - much worse!
 * 
 * ============================================================================
 */


class Solution {
    
    /**
     * APPROACH 1: UNION-FIND (OPTIMAL SOLUTION)
     * 
     * ALGORITHM:
     * 1. Initialize Union-Find structure for m*n grid
     * 2. For each position (x, y) being added:
     *    a. If already land, skip (handle duplicates)
     *    b. Mark as land, increment island count
     *    c. Check 4 neighbors (up, down, left, right)
     *    d. For each neighbor that is land, union them
     *    e. Each successful union decrements island count
     * 3. Record island count after each operation
     * 
     * INTERVIEW TIP: Draw a small example grid and show how islands merge!
     */
    public List<Integer> numIslands2(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        
        // Edge case: no positions
        if (positions == null || positions.length == 0) {
            return result;
        }
        
        UnionFind uf = new UnionFind(m * n);
        
        // Track which cells are land (water by default)
        // CRITICAL: We need this to avoid connecting water cells!
        boolean[][] isLand = new boolean[m][n];
        
        // Directions: up, down, left, right
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        // Process each position
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            
            // HANDLE DUPLICATES: If already land, count doesn't change
            if (isLand[x][y]) {
                result.add(uf.getCount());
                continue;
            }
            
            // Mark as land and increment island count
            isLand[x][y] = true;
            uf.addLand();
            
            // Convert 2D coordinate to 1D index for Union-Find
            int currentId = x * n + y;
            
            // Check all 4 neighbors
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];
                
                // Check bounds and if neighbor is land
                if (newX >= 0 && newX < m && newY >= 0 && newY < n && isLand[newX][newY]) {
                    int neighborId = newX * n + newY;
                    
                    // Union current land with neighbor land
                    // This might merge islands (count decreases)
                    uf.union(currentId, neighborId);
                }
            }
            
            // Record current island count
            result.add(uf.getCount());
        }
        
        return result;
    }
    
    /**
     * UNION-FIND DATA STRUCTURE (Optimized with Path Compression + Union by Rank)
     * 
     * This is the STANDARD implementation you should memorize for interviews!
     * 
     * Key features:
     * 1. Path Compression in find() - flattens tree
     * 2. Union by Rank - keeps tree balanced
     * 3. Component counting - tracks number of disjoint sets
     */
    class UnionFind {
        private int[] parent;  // parent[i] = parent of node i
        private int[] rank;    // rank[i] = approximate depth of tree rooted at i
        private int count;     // number of disjoint sets (islands)
        
        /**
         * Initialize Union-Find for n elements
         * Initially, each element is its own parent (n disjoint sets)
         */
        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            count = 0; // Start with 0 islands (all water)
            
            // Initialize: each node is its own parent
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }
        
        /**
         * Find root of x with PATH COMPRESSION
         * 
         * Path Compression: Make every node on path point directly to root
         * This flattens the tree structure for future operations
         * 
         * Example:
         * Before: 1 -> 2 -> 3 -> 4 (root)
         * After find(1): 1 -> 4, 2 -> 4, 3 -> 4 (all point to root)
         * 
         * Time: O(α(n)) amortized, where α is inverse Ackermann
         */
        public int find(int x) {
            if (parent[x] != x) {
                // Recursively find root and compress path
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }
        
        /**
         * Union two elements with UNION BY RANK
         * 
         * Union by Rank: Attach smaller tree under root of larger tree
         * Keeps tree height logarithmic
         * 
         * CRITICAL for this problem: Decrements count when merging two islands!
         * 
         * Time: O(α(n)) amortized
         */
        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            // Already in same set - no merge needed
            if (rootX == rootY) {
                return;
            }
            
            // IMPORTANT: Merging two islands into one
            // Two separate islands become one island
            count--;
            
            // Union by rank: attach smaller tree under larger
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                // Equal rank: choose one as root, increment its rank
                parent[rootY] = rootX;
                rank[rootX]++;
            }
        }
        
        /**
         * Add a new land cell (increases island count)
         * Called when converting water to land
         */
        public void addLand() {
            count++;
        }
        
        /**
         * Get current number of islands
         */
        public int getCount() {
            return count;
        }
    }
    
    /**
     * APPROACH 2: UNION-FIND WITH SIZE TRACKING (Alternative Implementation)
     * 
     * Instead of rank, track actual size of each component
     * Some prefer this as it's more intuitive
     * 
     * Performance is equivalent to rank-based approach
     */
    public List<Integer> numIslands2WithSize(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        UnionFindWithSize uf = new UnionFindWithSize(m * n);
        boolean[][] isLand = new boolean[m][n];
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            
            if (isLand[x][y]) {
                result.add(uf.getCount());
                continue;
            }
            
            isLand[x][y] = true;
            uf.addLand();
            int currentId = x * n + y;
            
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];
                
                if (newX >= 0 && newX < m && newY >= 0 && newY < n && isLand[newX][newY]) {
                    int neighborId = newX * n + newY;
                    uf.union(currentId, neighborId);
                }
            }
            
            result.add(uf.getCount());
        }
        
        return result;
    }
    
    /**
     * Union-Find with SIZE instead of RANK
     * Union by size: attach smaller component to larger component
     */
    class UnionFindWithSize {
        private int[] parent;
        private int[] size;  // size[i] = number of nodes in tree rooted at i
        private int count;
        
        public UnionFindWithSize(int n) {
            parent = new int[n];
            size = new int[n];
            count = 0;
            
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
            
            if (rootX == rootY) {
                return;
            }
            
            count--;
            
            // Union by size: attach smaller to larger
            if (size[rootX] < size[rootY]) {
                parent[rootX] = rootY;
                size[rootY] += size[rootX];
            } else {
                parent[rootY] = rootX;
                size[rootX] += size[rootY];
            }
        }
        
        public void addLand() {
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * APPROACH 3: NAIVE DFS APPROACH (For Comparison - DON'T USE IN INTERVIEW)
     * 
     * This is what you might think of first, but it's TOO SLOW
     * Time: O(k * m * n) where k = number of operations
     * 
     * Only show this to demonstrate why Union-Find is better!
     */
    public List<Integer> numIslands2Naive(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        boolean[][] grid = new boolean[m][n];
        
        for (int[] pos : positions) {
            grid[pos[0]][pos[1]] = true;
            
            // Count islands using DFS - O(m*n) per operation!
            result.add(countIslandsDFS(grid, m, n));
        }
        
        return result;
    }
    
    private int countIslandsDFS(boolean[][] grid, int m, int n) {
        boolean[][] visited = new boolean[m][n];
        int count = 0;
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] && !visited[i][j]) {
                    dfs(grid, visited, i, j, m, n);
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private void dfs(boolean[][] grid, boolean[][] visited, int x, int y, int m, int n) {
        if (x < 0 || x >= m || y < 0 || y >= n || !grid[x][y] || visited[x][y]) {
            return;
        }
        
        visited[x][y] = true;
        dfs(grid, visited, x + 1, y, m, n);
        dfs(grid, visited, x - 1, y, m, n);
        dfs(grid, visited, x, y + 1, m, n);
        dfs(grid, visited, x, y - 1, m, n);
    }
    
    /**
     * ========================================================================
     * COMPREHENSIVE TEST SUITE
     * ========================================================================
     */
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        // TEST CASE 1: Basic example from problem
        System.out.println("Test 1 - Basic example:");
        int[][] positions1 = {{0,0}, {0,1}, {1,2}, {2,1}};
        System.out.println(sol.numIslands2(3, 3, positions1));
        // Expected: [1, 1, 2, 3]
        // Explanation:
        // After (0,0): 1 island
        // After (0,1): Still 1 island (merged with (0,0))
        // After (1,2): 2 islands (isolated from first)
        // After (2,1): 3 islands (isolated from all)
        System.out.println();
        
        // TEST CASE 2: Islands that merge
        System.out.println("Test 2 - Merging islands:");
        int[][] positions2 = {{0,0}, {0,2}, {0,1}};
        System.out.println(sol.numIslands2(1, 3, positions2));
        // Expected: [1, 2, 1]
        // (0,0): 1 island
        // (0,2): 2 islands
        // (0,1): Merges them into 1 island
        System.out.println();
        
        // TEST CASE 3: Duplicate positions
        System.out.println("Test 3 - Duplicate positions:");
        int[][] positions3 = {{0,0}, {0,1}, {0,0}, {1,1}};
        System.out.println(sol.numIslands2(2, 2, positions3));
        // Expected: [1, 1, 1, 1]
        // (0,0): 1 island
        // (0,1): 1 island (merged)
        // (0,0): Duplicate, still 1 island
        // (1,1): 1 island (merged)
        System.out.println();
        
        // TEST CASE 4: Single cell
        System.out.println("Test 4 - Single cell:");
        int[][] positions4 = {{0,0}};
        System.out.println(sol.numIslands2(1, 1, positions4));
        // Expected: [1]
        System.out.println();
        
        // TEST CASE 5: All isolated islands
        System.out.println("Test 5 - All isolated:");
        int[][] positions5 = {{0,0}, {0,2}, {2,0}, {2,2}};
        System.out.println(sol.numIslands2(3, 3, positions5));
        // Expected: [1, 2, 3, 4]
        System.out.println();
        
        // TEST CASE 6: Complex merging (X pattern)
        System.out.println("Test 6 - Complex X pattern:");
        int[][] positions6 = {{0,0}, {2,2}, {1,1}, {0,2}, {2,0}};
        System.out.println(sol.numIslands2(3, 3, positions6));
        // Expected: [1, 2, 1, 2, 1]
        // (0,0): 1
        // (2,2): 2
        // (1,1): Merges to 1
        // (0,2): 2
        // (2,0): Merges all to 1
        System.out.println();
        
        // TEST CASE 7: Empty positions
        System.out.println("Test 7 - Empty positions:");
        int[][] positions7 = {};
        System.out.println(sol.numIslands2(3, 3, positions7));
        // Expected: []
        System.out.println();
        
        // TEST CASE 8: Large grid with sequential additions
        System.out.println("Test 8 - Sequential row:");
        int[][] positions8 = {{0,0}, {0,1}, {0,2}, {0,3}, {0,4}};
        System.out.println(sol.numIslands2(1, 5, positions8));
        // Expected: [1, 1, 1, 1, 1] (keeps merging into one)
        System.out.println();
        
        // Demonstrate complexity difference
        System.out.println("\n=== Performance Comparison ===");
        System.out.println("Union-Find: O(k * α(mn)) ≈ O(k)");
        System.out.println("DFS Naive:  O(k * m * n)");
        System.out.println("For k=1000, m=100, n=100:");
        System.out.println("Union-Find: ~1000 operations");
        System.out.println("DFS:        ~10,000,000 operations (10000x slower!)");
    }
}

/**
 * ============================================================================
 * INTERVIEW STRATEGY - DETAILED TIMELINE
 * ============================================================================
 * 
 * 0-2 MINUTES: Problem Understanding
 * - "So we're dynamically adding land and need island count after each addition"
 * - "This is definitely a Union-Find problem - dynamic connectivity"
 * - Ask: "Can positions repeat? Need to handle that"
 * - Draw small example on whiteboard
 * 
 * 2-5 MINUTES: Approach Discussion
 * - "First thought might be DFS after each operation, but that's O(k*m*n)"
 * - "Union-Find is perfect here - near O(1) per operation"
 * - "Each land cell is a node. When adding land, union with neighbors"
 * - "Track component count: +1 when adding, -1 when merging"
 * - Mention time complexity: O(k * α(mn)) ≈ O(k)
 * 
 * 5-8 MINUTES: Design Details
 * - "Need boolean[][] to track which cells are land"
 * - "Convert 2D coordinates to 1D for Union-Find: index = x*n + y"
 * - "Check 4 neighbors: up, down, left, right"
 * - "Handle duplicates: if already land, just return current count"
 * 
 * 8-25 MINUTES: Implementation
 * - Write Union-Find class first (interviewer loves this!)
 * - Implement find() with path compression
 * - Implement union() with union by rank
 * - Add addLand() and getCount() methods
 * - Implement main algorithm with neighbor checking
 * 
 * 25-30 MINUTES: Testing
 * - Walk through Test Case 1 step by step
 * - Show how islands merge in Test Case 2
 * - Verify duplicate handling in Test Case 3
 * 
 * 30-35 MINUTES: Edge Cases
 * - Empty positions
 * - Single cell
 * - All isolated vs all merged
 * - Duplicate positions
 * 
 * 35-40 MINUTES: Optimizations & Follow-ups
 * - Discuss path compression vs no compression
 * - Union by rank vs union by size
 * - Space optimization possibilities
 * 
 * 40-45 MINUTES: Alternative approaches
 * - Mention DFS approach and why it's slower
 * - Discuss when Union-Find is the right choice
 * 
 * ============================================================================
 * CRITICAL POINTS TO EMPHASIZE
 * ============================================================================
 * 
 * 1. WHY UNION-FIND:
 *    "This is THE classic use case for Union-Find - dynamic connectivity
 *     with repeated queries. DFS would be O(mn) per query, Union-Find is
 *     essentially O(1) per query with optimizations."
 * 
 * 2. PATH COMPRESSION:
 *    "Makes find() nearly constant time. After finding root once, all nodes
 *     on path point directly to root. Amortizes to O(α(n))."
 * 
 * 3. UNION BY RANK:
 *    "Keeps tree balanced. Always attach shorter tree to taller tree.
 *     Without this, tree could become a linked list (O(n) operations)."
 * 
 * 4. COUNTING TRICK:
 *    "Key insight: Start with 0 islands. +1 when adding land. -1 when union
 *     succeeds (merging two islands). This maintains accurate count."
 * 
 * 5. 2D TO 1D MAPPING:
 *    "Standard trick: index = row * numCols + col. Allows using 1D array
 *     for Union-Find while working with 2D grid."
 * 
 * ============================================================================
 * COMMON MISTAKES TO AVOID
 * ============================================================================
 * 
 * ❌ MISTAKE 1: Forgetting to check if cell is already land
 *    Impact: Wrong count when duplicates exist
 *    Fix: if (isLand[x][y]) continue;
 * 
 * ❌ MISTAKE 2: Not checking neighbor bounds
 *    Impact: ArrayIndexOutOfBoundsException
 *    Fix: if (newX >= 0 && newX < m && newY >= 0 && newY < n)
 * 
 * ❌ MISTAKE 3: Forgetting to check if neighbor is land
 *    Impact: Union with water cells, wrong structure
 *    Fix: && isLand[newX][newY]
 * 
 * ❌ MISTAKE 4: Wrong count logic
 *    Impact: Incorrect island counts
 *    Fix: +1 on addLand(), -1 on successful union
 * 
 * ❌ MISTAKE 5: Not implementing path compression
 *    Impact: Degrades to O(n) per operation
 *    Fix: parent[x] = find(parent[x]) in find()
 * 
 * ❌ MISTAKE 6: Wrong 2D to 1D conversion
 *    Impact: Wrong cells being unioned
 *    Fix: Use index = x * n + y (not x * m + y)
 * 
 * ❌ MISTAKE 7: Decrementing count on failed union
 *    Impact: Count goes negative or too low
 *    Fix: Only count-- when rootX != rootY
 * 
 * ============================================================================
 * FOLLOW-UP QUESTIONS & ANSWERS
 * ============================================================================
 * 
 * Q1: "What if we also need to support removing land?"
 * A: Union-Find doesn't support efficient disconnect. Would need different
 *    approach - maybe segment tree or maintain adjacency lists with versioning.
 * 
 * Q2: "Can you optimize space to O(k) instead of O(mn)?"
 * A: Yes! Use HashMap<Integer, Integer> for parent/rank instead of arrays.
 *    Only store entries for actual land cells. Trade some speed for space.
 * 
 * Q3: "What if we need to find the largest island after each operation?"
 * A: Track size of each component in Union-Find. Update during union.
 *    Maintain max size variable. O(1) extra per operation.
 * 
 * Q4: "How would you parallelize this for very large k?"
 * A: Tricky because operations are sequential. Could use parallel Union-Find
 *    (research topic). Or batch operations and merge results.
 * 
 * Q5: "What if positions can be negative coordinates?"
 * A: Normalize coordinates: shift by min values. Or use 2D HashMap.
 * 
 * Q6: "Can you return which positions merged islands?"
 * A: Track before/after count. If count decreased, this position was a bridge.
 *    Store those positions separately.
 * 
 * Q7: "Memory limit: can only store O(k) space, not O(mn)"
 * A: Use HashMap-based Union-Find. Only create entries for actual land.
 *    parent = new HashMap<Integer, Integer>();
 * 
 * ============================================================================
 * RELATED PROBLEMS (Mention to show breadth)
 * ============================================================================
 * 
 * - Number of Islands I (LC 200): Basic DFS/BFS
 * - Number of Islands II (LC 305): This problem - Union-Find
 * - Number of Distinct Islands (LC 694): DFS + hashing
 * - Max Area of Island (LC 695): DFS with size tracking
 * - Accounts Merge (LC 721): Union-Find on emails
 * - Friend Circles (LC 547): Union-Find on people
 * - Redundant Connection (LC 684): Union-Find cycle detection
 * 
 * ============================================================================
 * KEY TAKEAWAYS FOR INTERVIEWS
 * ============================================================================
 * 
 * 1. Union-Find is THE answer for dynamic connectivity problems
 * 2. Always implement both path compression AND union by rank
 * 3. Converting 2D to 1D: index = row * cols + col
 * 4. Track component count: +1 add, -1 merge
 * 5. Don't forget to check if cells are already processed
 * 6. Time complexity: O(k * α(mn)) where α ≈ constant
 * 7. This is a HARD problem - if you solve it well, strong hire signal!
 * 
 * ============================================================================
 */
