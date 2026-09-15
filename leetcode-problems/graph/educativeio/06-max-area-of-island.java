/*
 * ==========================================================================================
 *                                  MAX AREA OF ISLAND
 * ==========================================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ------------------------------------------------------------------------------------------
 * Restatement:
 * You are given a 2D grid made of 0s (water) and 1s (land). Islands are formed by 1s that 
 * touch each other vertically or horizontally (not diagonally). 
 * You need to find the largest single island in the grid and return its area (the number 
 * of 1s it contains). If there's no land, return 0.
 *
 * Idea, Intuition, and Key Observations:
 * - This is fundamentally a "Connected Components" problem on a grid.
 * - Each '1' is a node, and adjacent '1's have edges between them.
 * - To find the area of an island, we just need to start at a '1', count it, and explore 
 *   its neighbors, summing up the counts.
 * - Once we visit a '1', we MUST mark it as visited (e.g., changing it to 0 or using a 
 *   visited boolean array) so we don't count it twice or get stuck in an infinite loop.
 * - We repeat this exploration for every unvisited '1' in the grid and keep track of the 
 *   maximum area seen so far.
 *
 * How to identify the relevant algorithm/data structure:
 * - "Connected components", "Grid", "4-directionally", "Shortest path/Max area": 
 *   These are classic triggers for Graph Traversal algorithms: DFS (Depth-First Search) or 
 *   BFS (Breadth-First Search).
 *
 *
 * 2. INTERVIEW CLARIFICATION
 * ------------------------------------------------------------------------------------------
 * Ask these questions before writing any code:
 * 
 * Q1: "Can I modify the input grid to mark visited cells?"
 *     Why: Modifying the grid saves O(m*n) memory. If the grid is read-only, you MUST allocate 
 *          a `boolean[][] visited` array.
 * Q2: "What is the maximum size of the grid?"
 *     Why: If the grid is massive (e.g., 10,000 x 10,000), a recursive DFS might cause a 
 *          StackOverflowError. You would need to use BFS or an iterative DFS. (Here, it's 
 *          50x50, so recursion is perfectly safe).
 * Q3: "Do diagonal connections count as part of an island?"
 *     Why: Changes the neighbor exploration logic from 4 directions to 8 directions. (Problem 
 *          says no, but good to clarify).
 * Q4: "What should I return if the grid is empty or contains only 0s?"
 *     Why: Establishes base cases. (Expected return is 0).
 * Q5: "Are the grid values strictly integers 0 and 1, or could they be characters '0' and '1'?"
 *     Why: Prevents a silly type mismatch bug (e.g., grid[r][c] == 1 vs == '1').
 *
 *
 * 3. EXAMPLES & VISUALS
 * ------------------------------------------------------------------------------------------
 * Normal Case:
 * Grid:
 * 0 0 1 1 0
 * 0 0 1 0 0
 * 0 0 0 0 1
 * 1 1 0 0 1
 * 
 * Step-by-Step Execution:
 * 1. Scan left to right, top to bottom.
 * 2. Find first '1' at (0, 2). Launch DFS.
 *    - (0, 2) is 1. Mark as 0. Area = 1.
 *    - Go Right to (0, 3). It's 1. Mark as 0. Area = 1 + 1 = 2.
 *    - Go Down from (0, 2) to (1, 2). It's 1. Mark as 0. Area = 2 + 1 = 3.
 *    DFS finishes. Max Area = 3.
 * 3. Continue scan. Find '1' at (2, 4). Launch DFS.
 *    - Connects to (3, 4). Area = 2. Max Area remains 3.
 * 4. Continue scan. Find '1' at (3, 0). Launch DFS.
 *    - Connects to (3, 1). Area = 2. Max Area remains 3.
 * 5. Return 3.
 *
 * Brute Force / Incorrect Approach Visual:
 * What if we don't mark visited?
 * 1 - 1
 * When at (0,0), we look right to (0,1). 
 * When at (0,1), we look left to (0,0). 
 * Infinite loop! Visited tracking is mandatory.
 *
 * 
 * 4. SOLUTIONS
 * ------------------------------------------------------------------------------------------
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class MaxAreaOfIsland {

    /*
     * APPROACH 1: DEPTH FIRST SEARCH (DFS) - Recursive [OPTIMAL & RECOMMENDED]
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * Use recursion to dive as deep as possible into an island. For every cell, its area 
     * is 1 (itself) plus the area of its 4 neighbors.
     * 
     * Why it works:
     * By sinking the island (changing 1 to 0) as we visit, we ensure each cell is counted 
     * exactly once. The recursion naturally sums up the components.
     * 
     * Time Complexity: O(M * N)
     * Every cell is visited at most a few times (once in the nested loops, and up to 4 times 
     * checked as a neighbor).
     * 
     * Space Complexity: O(M * N) in the worst case for the recursion stack (if the entire 
     * grid is one giant zigzag island).
     * 
     * Interview Recommendation: 
     * Write this one. It's the cleanest, most concise, and shows you understand graph traversal.
     */
    public int maxAreaOfIslandDFS(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int maxArea = 0;
        int rows = grid.length;
        int cols = grid[0].length;

        // Iterate through every cell in the grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // When we find land, calculate its full area using DFS
                if (grid[r][c] == 1) {
                    int currentArea = dfs(grid, r, c);
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }
        return maxArea;
    }

    private int dfs(int[][] grid, int r, int c) {
        // Base case: check bounds and check if it's water (or already visited)
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] == 0) {
            return 0;
        }

        // Mark current cell as visited by sinking it (1 -> 0)
        grid[r][c] = 0;

        // Area is 1 (current cell) + area of 4 surrounding cells
        return 1 + dfs(grid, r + 1, c)   // Down
                 + dfs(grid, r - 1, c)   // Up
                 + dfs(grid, r, c + 1)   // Right
                 + dfs(grid, r, c - 1);  // Left
    }

    /*
     * APPROACH 2: BREADTH FIRST SEARCH (BFS) - Iterative
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * Instead of recursion, use a Queue to explore level-by-level (radiating outwards).
     * Modern Java Note: We use a `record` to hold coordinates cleanly instead of `int[]`.
     * 
     * Why it works:
     * Works exactly like DFS but avoids the call stack, protecting against StackOverflow on 
     * extremely large grids.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(min(M, N)) - The queue holds at most the perimeter of the island.
     * 
     * When to use: 
     * Present this if the interviewer says "The grid is 100,000 x 100,000, what happens to DFS?"
     */
    
    // Modern Java Feature: Record for clean coordinate representation
    private record Cell(int r, int c) {}

    public int maxAreaOfIslandBFS(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int maxArea = 0;
        int rows = grid.length;
        int cols = grid[0].length;
        
        // Direction vectors for cleanly iterating through Up, Down, Left, Right
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1) {
                    int currentArea = 0;
                    // Use Deque as it is faster than LinkedList for standard Queue operations
                    Deque<Cell> queue = new ArrayDeque<>();
                    queue.offer(new Cell(r, c));
                    grid[r][c] = 0; // Mark visited IMMEDIATELY when adding to queue

                    while (!queue.isEmpty()) {
                        Cell curr = queue.poll();
                        currentArea++;

                        for (int[] dir : directions) {
                            int nr = curr.r() + dir[0];
                            int nc = curr.c() + dir[1];

                            // Bounds check and unvisited check
                            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == 1) {
                                queue.offer(new Cell(nr, nc));
                                // Crucial: Mark visited as soon as it enters the queue 
                                // to prevent duplicate processing
                                grid[nr][nc] = 0; 
                            }
                        }
                    }
                    maxArea = Math.max(maxArea, currentArea);
                }
            }
        }
        return maxArea;
    }

    /*
     * APPROACH 3: UNION-FIND (Disjoint Set)
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * Treat the grid as a graph of size M*N. Connect adjacent '1's using a Disjoint Set 
     * Data Structure. Keep track of the size of each set.
     * 
     * Why it works:
     * Union-Find groups elements together efficiently.
     * 
     * Time Complexity: O(M * N * α(M*N)) where α is the inverse Ackermann function (nearly O(1)).
     * Space Complexity: O(M * N) for the parent and size arrays.
     * 
     * Trade-offs:
     * Much more boilerplate code. Slower in practice due to array lookups.
     * When to use: ONLY if the interviewer explicitly asks "Can you solve this using Union-Find?" 
     * or if the grid is updating dynamically (e.g., adding land over time).
     */
    public int maxAreaOfIslandUF(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int rows = grid.length;
        int cols = grid[0].length;
        UnionFind uf = new UnionFind(rows * cols);
        boolean hasLand = false;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1) {
                    hasLand = true;
                    // 1D index mapping
                    int id1 = r * cols + c; 
                    uf.setInit(id1);

                    // Only need to check right and down to avoid redundant unions
                    if (r + 1 < rows && grid[r + 1][c] == 1) {
                        int id2 = (r + 1) * cols + c;
                        uf.setInit(id2);
                        uf.union(id1, id2);
                    }
                    if (c + 1 < cols && grid[r][c + 1] == 1) {
                        int id2 = r * cols + (c + 1);
                        uf.setInit(id2);
                        uf.union(id1, id2);
                    }
                }
            }
        }
        return hasLand ? uf.getMaxSize() : 0;
    }

    class UnionFind {
        int[] parent;
        int[] size;
        int maxSize = 1;

        public UnionFind(int totalNodes) {
            parent = new int[totalNodes];
            size = new int[totalNodes];
            // Initialize all as -1. We will set to 1 when a land is discovered.
            for (int i = 0; i < totalNodes; i++) parent[i] = -1;
        }

        public void setInit(int i) {
            if (parent[i] == -1) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        public int find(int i) {
            if (parent[i] == i) return i;
            return parent[i] = find(parent[i]); // Path compression
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                // Union by size
                if (size[rootI] < size[rootJ]) {
                    parent[rootI] = rootJ;
                    size[rootJ] += size[rootI];
                    maxSize = Math.max(maxSize, size[rootJ]);
                } else {
                    parent[rootJ] = rootI;
                    size[rootI] += size[rootJ];
                    maxSize = Math.max(maxSize, size[rootI]);
                }
            }
        }

        public int getMaxSize() {
            return maxSize;
        }
    }

    /*
     * 6. EDGE CASES & MISTAKES
     * ------------------------------------------------------------------------------------------
     * - Edge Case 1: Grid is all 0s. Handled correctly (returns 0).
     * - Edge Case 2: Grid is all 1s. DFS will visit all, return M*N. Stack depth = M*N.
     * - Mistake 1 (BFS): Marking a cell visited AFTER popping it from the queue instead of 
     *   when adding it. This causes multiple neighbors to add the SAME cell to the queue, 
     *   leading to OutOfMemoryError.
     * - Mistake 2: Forgetting bounds checks, throwing ArrayIndexOutOfBoundsException.
     * - Mistake 3: Off-by-one in DFS summing. e.g., returning `dfs() + dfs()` instead of 
     *   `1 + dfs() + dfs()`.
     *
     * 
     * 7. INTERVIEW STRATEGY
     * ------------------------------------------------------------------------------------------
     * Step 1. Understand & Restate: "I'll find the max contiguous 1s. A 4-directional search."
     * Step 2. Clarify: "Can I modify the grid? Are diagonals allowed?"
     * Step 3. Approach: "I will iterate through the grid. When I hit a 1, I'll trigger a DFS 
     *         to count the island size and sink it to 0. I'll maintain a global max."
     * Step 4. Code: Write the DFS cleanly. Use descriptive variable names (`rows`, `cols`).
     * Step 5. Dry Run: Trace a small 3x3 grid out loud. Show how the stack unwinds and adds 1.
     * Step 6. Complexity: State O(M*N) time and space.
     *
     *
     * 8. SOLUTION COMPARISON
     * ------------------------------------------------------------------------------------------
     * Approach     | Time      | Space       | Trade-offs                     | Recommendation
     * -------------|-----------|-------------|--------------------------------|---------------
     * DFS          | O(M*N)    | O(M*N)      | Call stack limit risk          | 🥇 Primary
     * BFS          | O(M*N)    | O(min(M,N)) | Slightly more code             | 🥈 Backup
     * Union-Find   | O(M*N)    | O(M*N)      | Complex, slow constant factors | 🥉 Avoid unless forced
     *
     * 
     * 9. FOLLOW-UP QUESTIONS & ANSWERS
     * ------------------------------------------------------------------------------------------
     * F1: "What if the grid is strictly read-only?"
     * A1: I would allocate a `boolean[][] visited = new boolean[m][n]`. This uses O(M*N) 
     *     auxiliary space. Inside my DFS, instead of `grid[r][c] = 0`, I'd do `visited[r][c] = true`.
     * 
     * F2: "What if the grid is too massive for memory (e.g., stored on disk)?"
     * A2: We couldn't hold the grid or a `visited` array in memory. We would process it in 
     *     chunks, or represent islands using disjoint sets (Union-Find) processing boundary 
     *     merges as chunks are loaded.
     * 
     * F3: "What if we count diagonal connections?"
     * A3: I would simply update my DFS/BFS directions array to include all 8 neighbors 
     *     instead of 4 (add [1,1], [1,-1], [-1,1], [-1,-1]).
     * 
     * F4: "What if islands are added dynamically over time? How to query max area efficiently?"
     * A4: This is "Number of Islands II". DFS/BFS would take O(M*N) per addition. Instead, 
     *     Union-Find handles dynamic additions in nearly O(1) time per operation. We'd track 
     *     the `maxSize` inside the Union-Find structure.
     *
     *
     * 10. FINAL TAKEAWAYS
     * ------------------------------------------------------------------------------------------
     * - Graph traversal on grids usually means implicitly converting grid cells to graph nodes.
     * - "Sinking" islands (mutating state) is the standard trick to save space if allowed.
     * - Always remember your 3 base cases in grid DFS: 
     *   1) Bounds check 2) Visited check 3) Condition check.
     */

    // Test Runner
    public static void main(String[] args) {
        MaxAreaOfIsland solution = new MaxAreaOfIsland();

        int[][] grid1 = {
            {0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0},
            {0, 0, 0, 0, 0},
            {1, 1, 0, 0, 1},
            {1, 1, 0, 0, 1}
        };
        
        // Note: Because DFS mutates the grid, we must pass deep copies to test all methods.
        System.out.println("DFS Max Area: " + solution.maxAreaOfIslandDFS(deepCopy(grid1))); // Expected: 4
        System.out.println("BFS Max Area: " + solution.maxAreaOfIslandBFS(deepCopy(grid1))); // Expected: 4
        System.out.println("UF  Max Area: " + solution.maxAreaOfIslandUF(deepCopy(grid1)));  // Expected: 4

        int[][] emptyGrid = {{0,0}, {0,0}};
        System.out.println("Empty Grid DFS: " + solution.maxAreaOfIslandDFS(emptyGrid)); // Expected: 0
    }

    private static int[][] deepCopy(int[][] original) {
        if (original == null) return null;
        int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i].clone();
        }
        return result;
    }
}


class Solution {

    /**
     * Directions array → represents 4-directional movement
     * (down, up, right, left)
     *
     * Why we use this?
     * → Avoid writing repetitive code
     * → Standard pattern for grid traversal problems
     */
    private static final int[][] DIRS = {
        {1, 0},   // down
        {-1, 0},  // up
        {0, 1},   // right
        {0, -1}   // left
    };

    public int maxAreaOfIsland(int[][] grid) {

        int m = grid.length;
        int n = grid[0].length;

        int maxArea = 0; // stores the maximum island area found

        /**
         * Step 1: Traverse every cell in the grid
         *
         * Why?
         * → We don't know where islands start
         * → So we scan the entire grid
         */
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                /**
                 * Step 2: If we find land (1),
                 * start BFS to explore the entire island
                 */
                if (grid[i][j] == 1) {

                    // Compute area of this island
                    int currentIslandArea = bfs(grid, i, j);

                    // Update global maximum
                    maxArea = Math.max(maxArea, currentIslandArea);
                }
            }
        }

        return maxArea;
    }

    /**
     * BFS function → explores one island fully
     *
     * @param grid → input matrix
     * @param r → starting row
     * @param c → starting column
     *
     * @return area (number of cells in this island)
     */
    private int bfs(int[][] grid, int r, int c) {

        /**
         * Queue for BFS traversal
         * Each element = cell (row, col)
         */
        Queue<int[]> queue = new ArrayDeque<>();

        // Add starting cell (first land cell of this island)
        queue.offer(new int[]{r, c});

        /**
         * Mark as visited
         *
         * IMPORTANT:
         * → We modify grid itself instead of using visited[][]
         * → Saves space
         */
        grid[r][c] = 0;

        int area = 0; // counts number of cells in this island

        /**
         * Step 3: BFS traversal
         *
         * Idea:
         * → Expand layer by layer
         * → Visit all connected land cells
         */
        while (!queue.isEmpty()) {

            int[] cell = queue.poll();

            // Count this cell as part of island
            area++;

            /**
             * Step 4: Explore all 4 neighbors
             */
            for (int[] d : DIRS) {

                int nr = cell[0] + d[0]; // new row
                int nc = cell[1] + d[1]; // new col

                /**
                 * Step 5: Check validity
                 *
                 * Conditions:
                 * → within bounds
                 * → must be land (1)
                 */
                if (nr >= 0 && nc >= 0 &&
                    nr < grid.length && nc < grid[0].length &&
                    grid[nr][nc] == 1) {

                    /**
                     * Mark visited immediately (VERY IMPORTANT)
                     *
                     * Why here?
                     * → Prevent multiple insertions into queue
                     * → Avoid infinite loops / duplicate work
                     */
                    grid[nr][nc] = 0;

                    // Add to queue for further exploration
                    queue.offer(new int[]{nr, nc});
                }
            }
        }

        /**
         * After BFS completes:
         * → Entire island is explored
         * → 'area' contains its size
         */
        return area;
    }
}
