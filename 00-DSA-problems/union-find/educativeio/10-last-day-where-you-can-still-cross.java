/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a 1-based grid of dimensions `rows` x `cols` (initially all 0s / land).
 * You are given a 1-based array `waterCells` where waterCells[i] = [r, c] 
 * represents the cell that turns into water (1) on day i + 1.
 * You can walk only on land (4 cardinal directions).
 * Find the last day you can walk from the top row to the bottom row.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Is the `waterCells` array 0-indexed or 1-indexed in code?
 *    (The coordinates inside it are 1-based. The array itself is 0-indexed in Java. We must subtract 1 from coordinates to map to standard 2D arrays).
 * 2. Can we move diagonally?
 *    (No, the prompt specifies the four cardinal directions only).
 * 3. Does the path have to start at a specific cell on the top row?
 *    (No, we can start at ANY land cell in the top row and end at ANY land cell in the bottom row).
 * 4. Will there always be a valid path on day 0?
 *    (Yes, because the grid starts entirely as land).
 * 5. Are we guaranteed that the grid will eventually be fully flooded?
 *    (Yes, `waterCells.length == rows * cols`).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: As days progress, land cells turn to water. Once a cell 
 * becomes water, it NEVER turns back to land. This creates a strictly monotonic property:
 * If we can cross on Day X, we can cross on all days before X.
 * If we CANNOT cross on Day Y, we cannot cross on any day after Y.
 * 
 * --- Approach 1: Brute Force (Simulation + BFS) ---
 * Idea: For every single day, update the grid with the new water cell. Then, run a 
 * Breadth-First Search (BFS) from the entire top row to see if we can reach the bottom.
 * What I'd naturally try: A loop over `waterCells`, maintaining a grid, and running BFS.
 * Why it works: BFS accurately finds paths in unweighted grids.
 * Why it's too slow/costly: We run a full grid traversal for every single day. 
 * What work is repeated: We traverse the exact same top-half paths day after day until 
 * the bottom finally gets blocked off.
 * 
 * Time Complexity: O(N^2) where N = rows * cols. Processing day D takes O(N) BFS.
 * Space Complexity: O(N) for the BFS queue and grid.
 * 
 * --- Approach 2: Binary Search + BFS ---
 * Idea: Because of the monotonic property, we don't need to check every day sequentially. 
 * We can binary search the answer! Pick a middle day. Build the grid at that exact day 
 * (by filling in water cells from day 0 to mid). Run BFS. If we can cross, the answer 
 * is >= mid. If not, < mid.
 * Why it works: Binary search efficiently hones in on the exact threshold day.
 * 
 * Time Complexity: O(N * log N). We do log(N) BFS runs, each taking O(N) time.
 * Space Complexity: O(N) to build the grid state and run the BFS queue.
 * (This is highly practical and often the expected answer in interviews).
 * 
 * --- Approach 3: Union-Find going Backwards (Time Travel) - Optimal ---
 * Idea: Breaking graphs apart (water blocking land) is mathematically difficult. 
 * Building graphs up (adding land) is easy using a Disjoint Set (Union-Find).
 * What if we start on the LAST day when the grid is 100% water? 
 * We iterate backwards through `waterCells`, changing water back into land. 
 * When a cell becomes land, we `union` it with adjacent land cells. 
 * We use a "Virtual Top Node" connected to all top-row cells, and a "Virtual Bottom Node" 
 * connected to all bottom-row cells. The moment `find(Top) == find(Bottom)`, a path exists!
 * 
 * Time Complexity: O(N * α(N)) — For N cells, we do 4 unions per cell. α is the inverse Ackermann function, effectively O(1). This is incredibly fast.
 * Space Complexity: O(N) — For the Union-Find parent array and grid tracking.
 * 
 * Interview Decision: I would quickly explain Binary Search + BFS as the intuitive solution, 
 * but write **Union-Find Backwards** in a 45-minute interview. It demonstrates elite 
 * problem-solving skills (time reversal) and data structure mastery.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. 1-Row Grid (`rows == 1`): The virtual top and virtual bottom both connect to this 
 *    single row. As soon as one cell is land, it unions with both top and bottom, triggering success instantly.
 * 2. Massive Snake Path: A path that zig-zags wildly. (DFS might hit StackOverflow here, 
 *    which is why BFS or Union-Find is preferred).
 * 3. 1-based indexing: `waterCells[i]` contains coordinates like [1, 1], but arrays 
 *    are 0-based in Java. Must shift everything by -1.
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "Is it easier to track when things break apart, or when things join together?"
 * Correctness intuition: Connectivity is easy to track (Union-Find). Disconnectivity is hard. 
 * Reversing time turns a disconnect problem into a connect problem.
 * 
 * --- Dry Run (Union-Find Backwards) ---
 * Grid 2x2. waterCells = [[1,1],[2,1],[1,2],[2,2]]
 * We want the last day. Start with all water (Day 4).
 * UF Size = 6 (Top=0, Bottom=5, nodes 1 to 4).
 * 
 * i = 3 (Day 4). Cell [2,2]. Make land. Connects to Bottom(5).
 * i = 2 (Day 3). Cell [1,2]. Make land. Connects to Top(0). Connects to [2,2]! 
 *   -> Top(0) is now unioned with Bottom(5)! 
 *   -> Return `i` which is 2. (The answer is Day 2).
 * 
 * --- Pitfalls ---
 * 1. Flattening 2D to 1D index: `r * cols + c` gives 0 to N-1. But we need indices 
 *    for Virtual Top and Bottom. Add + 1 to shift all grid nodes, making Top=0.
 * 2. Returning the wrong day: If we process index `i` going backwards and it connects, 
 *    it means ON day `i+1` (where this cell was water) we COULD NOT cross. But BEFORE day `i+1` 
 *    (which is day `i`), this cell was land, and we COULD cross. Thus, return `i`.
 * 
 * --- Pattern Recognition ---
 * When you see: "Day by day grid changes", "When does the path break?", "Monotonic threshold".
 * Think: Binary Search + BFS -OR- Reverse Time Union-Find.
 * Transfers to: Bricks Falling When Hit, Swim in Rising Water, Path With Maximum Minimum Value.
 * 
 * --- Interview Script ---
 * "Since water cells permanently block paths, there's a monotonic threshold day where crossing 
 * becomes impossible. We could binary search that day and run BFS. However, a more optimal 
 * approach is to run time backwards. If we start with a completely flooded grid and add land 
 * back step-by-step, we turn a graph-destruction problem into a dynamic connectivity problem. 
 * I'll use a Union-Find structure with a Virtual Top and Virtual Bottom node. As soon as adding 
 * a piece of land connects the Top and Bottom, we've found our threshold day. This achieves O(N) 
 * time complexity."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if we needed to return the actual path?
 * A: Union-Find only tracks connectivity, not the paths themselves. I would pivot to the 
 *    Binary Search + BFS approach. Once the threshold day is found, I'd trace the `parent` 
 *    pointers from the final BFS run to reconstruct the exact route.
 * Q2: What if the grid is 1,000,000 x 1,000,000, but there are only 1,000 water cells?
 * A: An array of size 10^12 would cause OutOfMemory. I'd change the Union-Find parent 
 *    structure from a flat array `int[]` to a `HashMap<Long, Long>` to map sparse 
 *    coordinates dynamically, only consuming memory for affected cells.
 */

import java.util.*;

public class LastDayToCross {

    /**
     * APPROACH 1: Union-Find (Going backwards / Time Travel)
     * Time: O(R * C * α(R * C))
     * Space: O(R * C)
     */
    public int latestDayToCross(int row, int col, int[][] waterCells) {
        int n = row * col;
        
        // Disjoint Set for N cells + 2 virtual nodes
        // Virtual Top = 0
        // Virtual Bottom = n + 1
        // Grid cells map to 1 through n.
        UnionFind uf = new UnionFind(n + 2);
        int topNode = 0;
        int bottomNode = n + 1;
        
        // Grid to track which cells are currently land (1) or water (0)
        // Starts entirely as water (0) because we are at the end of time.
        int[][] grid = new int[row][col];
        
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        // Traverse time backwards!
        for (int i = waterCells.length - 1; i >= 0; i--) {
            // Convert 1-based input coordinates to 0-based for arrays
            int r = waterCells[i][0] - 1;
            int c = waterCells[i][1] - 1;
            
            // Add the land back
            grid[r][c] = 1;
            
            // Map 2D coordinate to 1D ID (shifted by +1 to leave 0 for topNode)
            int currentId = r * col + c + 1;
            
            // If it's in the top row, connect to Virtual Top
            if (r == 0) {
                uf.union(topNode, currentId);
            }
            // If it's in the bottom row, connect to Virtual Bottom
            if (r == row - 1) {
                uf.union(bottomNode, currentId);
            }
            
            // Connect to adjacent lands
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                
                if (nr >= 0 && nr < row && nc >= 0 && nc < col && grid[nr][nc] == 1) {
                    int neighborId = nr * col + nc + 1;
                    uf.union(currentId, neighborId);
                }
            }
            
            // The moment Top and Bottom share the same root, we have a path!
            // The index 'i' in the array represents the day this cell BECOMES water (day i+1).
            // So if adding it as land creates a path, the last valid crossing day is 'i'.
            if (uf.find(topNode) == uf.find(bottomNode)) {
                return i;
            }
        }
        
        return 0;
    }
    
    /**
     * Standard Union-Find (Disjoint Set) class with Path Compression.
     */
    private class UnionFind {
        int[] parent;
        
        public UnionFind(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }
        
        // Find with path compression (keeps the tree flat)
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            return parent[i] = find(parent[i]);
        }
        
        // Union two sets
        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
            }
        }
    }

    /**
     * APPROACH 2: Binary Search + BFS 
     * Provided for comprehensive interview context.
     * Time: O(R * C * log(R * C))
     * Space: O(R * C)
     */
    public int latestDayToCrossBS(int row, int col, int[][] waterCells) {
        int left = 1;
        int right = waterCells.length;
        int ans = 0;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (canCrossOnDay(row, col, waterCells, mid)) {
                ans = mid;    // mid is possible, try a later day
                left = mid + 1;
            } else {
                right = mid - 1; // mid is blocked, try an earlier day
            }
        }
        return ans;
    }
    
    // Helper function for Binary Search
    private boolean canCrossOnDay(int row, int col, int[][] waterCells, int day) {
        int[][] grid = new int[row][col]; // 0 = land, 1 = water
        
        // Flood the cells for the given 'day'
        for (int i = 0; i < day; i++) {
            grid[waterCells[i][0] - 1][waterCells[i][1] - 1] = 1;
        }
        
        Queue<int[]> queue = new LinkedList<>();
        
        // Start BFS from every land cell in the top row
        for (int c = 0; c < col; c++) {
            if (grid[0][c] == 0) {
                queue.offer(new int[]{0, c});
                grid[0][c] = -1; // Mark as visited to prevent duplicate processing
            }
        }
        
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1];
            
            // Reached the bottom row!
            if (r == row - 1) {
                return true;
            }
            
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                
                if (nr >= 0 && nr < row && nc >= 0 && nc < col && grid[nr][nc] == 0) {
                    grid[nr][nc] = -1; // Mark visited
                    queue.offer(new int[]{nr, nc});
                }
            }
        }
        return false;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        LastDayToCross solution = new LastDayToCross();
        
        // Test Case 1: Simple 2x2 grid
        // Cells flooded: (1,1) -> (2,1) -> (1,2) -> (2,2)
        // Day 1: (1,1) water. Can still cross down right side.
        // Day 2: (2,1) water. Can still cross down right side.
        // Day 3: (1,2) water. Top row is totally blocked. Cannot cross.
        // Answer: 2
        int r1 = 2, c1 = 2;
        int[][] waterCells1 = {{1,1}, {2,1}, {1,2}, {2,2}};
        
        System.out.println("Test 1 (UF): " + solution.latestDayToCross(r1, c1, waterCells1)); // Expected 2
        System.out.println("Test 1 (BS): " + solution.latestDayToCrossBS(r1, c1, waterCells1)); // Expected 2
        
        // Test Case 2: Snake path (2x2)
        // Flooded: (1,1) -> (1,2) -> (2,1) -> (2,2)
        // Day 1: (1,1) water. Cross via (1,2) -> (2,2).
        // Day 2: (1,2) water. Top row blocked.
        // Answer: 1
        int r2 = 2, c2 = 2;
        int[][] waterCells2 = {{1,1}, {1,2}, {2,1}, {2,2}};
        System.out.println("Test 2 (UF): " + solution.latestDayToCross(r2, c2, waterCells2)); // Expected 1
        
        // Test Case 3: 1 Column edge case
        // Any water immediately breaks the path.
        // Day 1 blocks it.
        // Answer: 0
        int r3 = 3, c3 = 1;
        int[][] waterCells3 = {{1,1}, {2,1}, {3,1}};
        System.out.println("Test 3 (UF): " + solution.latestDayToCross(r3, c3, waterCells3)); // Expected 0
    }
}





import java.util.Arrays;

class Solution {

    // These are the only legal moves in the problem.
    // We cannot move diagonally.
    private static final int[][] DIRECTIONS = {
        {-1, 0}, // Up
        { 1, 0}, // Down
        { 0,-1}, // Left
        { 0, 1}  // Right
    };

    public int latestDayToCross(int rows, int cols, int[][] waterCells) {

        int totalCells = rows * cols;

        // Real grid cells use IDs 0 through totalCells - 1.
        // The two additional IDs represent imaginary boundary nodes.
        int TOP = totalCells;
        int BOTTOM = totalCells + 1;

        DSU dsu = new DSU(totalCells + 2);

        // Initially, we are at the final day: every cell is water.
        // land[r][c] becomes true only when we restore that cell.
        boolean[][] land = new boolean[rows][cols];

        // Process flooding events backward.
        //
        // waterCells[i] was flooded on forward day i + 1.
        // Restoring that cell reverses the flood and returns us to day i.
        //
        // We move from the last event toward the first event.
        for (int i = totalCells - 1; i >= 0; i--) {

            // Convert the problem's 1-based coordinates into
            // Java's 0-based row and column indices.
            int r = waterCells[i][0] - 1;
            int c = waterCells[i][1] - 1;

            // Step 1: Restore this cell from water to land.
            land[r][c] = true;

            // Step 2: Convert its grid coordinates to a unique DSU ID.
            int current = r * cols + c;

            // Step 3: Connect the restored cell to the virtual boundaries.
            //
            // Any top-row land cell can be a starting point.
            if (r == 0) {
                dsu.union(current, TOP);
            }

            // Any bottom-row land cell can be a destination.
            if (r == rows - 1) {
                dsu.union(current, BOTTOM);
            }

            // Step 4: Connect this cell to all neighboring land cells.
            for (int[] direction : DIRECTIONS) {

                int nr = r + direction[0];
                int nc = c + direction[1];

                // Ignore neighbors outside the grid.
                if (nr < 0 || nr >= rows ||
                    nc < 0 || nc >= cols) {
                    continue;
                }

                // A water neighbor cannot be part of a land path.
                // Only merge cells if the neighbor has already
                // been restored and is currently land.
                if (land[nr][nc]) {
                    int neighbor = nr * cols + nc;

                    // Merge the two connected components.
                    dsu.union(current, neighbor);
                }
            }

            // Step 5: Check whether a complete crossing now exists.
            //
            // If TOP and BOTTOM have the same representative, there is
            // a connected chain of land cells from the top to the bottom.
            if (dsu.find(TOP) == dsu.find(BOTTOM)) {

                // CRITICAL OFF-BY-ONE DETAIL:
                //
                // waterCells[i] flooded on forward day i + 1.
                // We have just undone that event, so we are back on day i.
                //
                // This is the latest successful forward day.
                return i;
            }
        }

        // Day 0 has every cell as land, so a crossing always exists.
        // For valid input, the loop must eventually find the connection.
        return 0;
    }

    private static class DSU {

        // parent[x] tells us the parent of node x in the DSU tree.
        private final int[] parent;

        // size[x] is the size of the component whose root is x.
        // It is used to merge smaller components into larger ones.
        private final int[] size;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];

            // Initially, every cell and virtual node is its own component.
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int x) {

            // If x is its own parent, it is the representative of its group.
            if (parent[x] == x) {
                return x;
            }

            // Path compression:
            // Make x point directly to the root, so future find operations
            // can reach the representative faster.
            return parent[x] = find(parent[x]);
        }

        void union(int a, int b) {

            // Find the representatives of the two groups.
            int rootA = find(a);
            int rootB = find(b);

            // Already connected: merging them again is unnecessary.
            if (rootA == rootB) {
                return;
            }

            // Union by size:
            // Attach the smaller component to the larger one.
            // This prevents DSU trees from becoming unnecessarily deep.
            if (size[rootA] < size[rootB]) {
                int temp = rootA;
                rootA = rootB;
                rootB = temp;
            }

            // Merge rootB's component into rootA's component.
            parent[rootB] = rootA;
            size[rootA] += size[rootB];
        }
    }
}

