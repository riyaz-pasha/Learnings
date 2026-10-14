/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a 2D binary grid of size m x n (initially all water/0), and an array of 
 * `positions` where land (1) is added step-by-step.
 * Return an array of integers representing the number of islands present in 
 * the grid after each addition operation.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can the `positions` array contain duplicate coordinates?
 *    (Yes, streams of operations can be noisy. If we place land where there is 
 *    already land, the count shouldn't change).
 * 2. Are we constrained by memory if m and n are massive?
 *    (The constraint says m * n <= 10^4, so an array of size M*N is perfectly 
 *    safe. If it were 10^9, we'd need a HashMap-based approach).
 * 3. Does diagonal adjacency count for islands?
 *    (No, strictly horizontal and vertical connections, standard for island problems).
 * 4. Can positions fall outside the grid bounds?
 *    (Constraints say 0 <= ri < m and 0 <= ci < n, so we don't need to worry about 
 *    rogue coordinates beyond the grid, though standard boundary checks are still needed).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: We are asked for a running count over K operations. 
 * Re-evaluating the entire board from scratch after every single drop of land 
 * ignores the fact that 99% of the board's state hasn't changed.
 * 
 * --- Approach 1: Brute Force (BFS/DFS on every step) ---
 * Idea: For every position in the input, mark the grid at (r, c) as 1. Then, 
 * pretend this is the classic "Number of Islands" problem. Loop through the 
 * entire M x N grid, launching a DFS/BFS every time we see an unvisited '1'.
 * What I'd naturally try: An outer loop over `positions`, and inside it, a 
 * double-for loop over the grid with a nested DFS.
 * Why it's too slow/costly: If we add K pieces of land, we scan the whole 
 * M x N grid K times. 
 * What work is repeated: We endlessly re-discover and re-traverse the exact 
 * same established islands just to count them again.
 * Property that fixes this: Island counts change LOCALLY. When you add a piece 
 * of land, the global count increases by 1. If that new land touches an existing 
 * island, they merge (count - 1). If it touches TWO distinct islands, they all 
 * merge into one (count - 2). We only need to check the 4 immediate neighbors!
 * 
 * Time Complexity: O(K * (M * N)) — because for each of the K operations, we do a full O(V+E) traversal of the grid.
 * Space Complexity: O(M * N) — because of the grid and the recursion stack for DFS/visited array.
 * 
 * --- Approach 2: Union-Find / Disjoint Set (Optimal) ---
 * Idea: Flatten the 2D grid into a 1D array where `index = r * n + c`. 
 * Start with a global `count = 0`. 
 * When land is added at (r, c):
 *   1. If it's already land, just record the current count and continue.
 *   2. Otherwise, mark it as land and do `count++`.
 *   3. Look at its up, down, left, right neighbors.
 *   4. If a neighbor is ALSO land, find the "root" of the neighbor's island 
 *      and the "root" of the new land's island.
 *   5. If they have different roots, `union` them and do `count--`.
 * Why it works: Union-Find is purpose-built for dynamic connectivity. It tracks 
 * "which group does this belong to" efficiently as elements are added over time.
 * 
 * Time Complexity: O(K * α(M*N)) — For each of the K positions, we do at most 4 Union-Find operations. With path compression, this takes amortized O(1) time (Inverse Ackermann function α). Initializing the parent array takes O(M*N). Total time is practically O(K + M*N).
 * Space Complexity: O(M * N) — because we maintain a `parent` array of size M*N to track the roots of each cell.
 * 
 * Interview Decision: I would write **Union-Find**. Brute force will definitely Time Out (TLE) 
 * on large inputs. Union-Find perfectly answers the "dynamic connectivity" requirement 
 * of the problem and shows strong data structure mastery.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. Overlapping/Duplicate positions: `positions = [[0,0], [0,0]]`. The second operation 
 *    should just append the current count, not increment it.
 * 2. Merging multiple islands at once: A U-shaped island is bridged by placing a block 
 *    in the center, reducing the count by 1. Placing a cross-center connecting 4 distinct 
 *    islands reduces the count by 3.
 * 3. 1D Grids: m = 1 or n = 1. The index math (r * n + c) works flawlessly here.
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "How does the total count change mathematically when I place one block?"
 * Correctness intuition: Base state is `count + 1` (a new isolated island). For every 
 * adjacent land cell that belongs to a DIFFERENT island root, they merge, yielding `count - 1`.
 * 
 * --- Examples & ASCII ---
 * m = 3, n = 3. Positions: [0,0], [0,2], [0,1]
 * 
 * Step 1: Add (0,0)      Step 2: Add (0,2)      Step 3: Add (0,1)
 * 1 0 0                  1 0 1                  1 1 1
 * 0 0 0                  0 0 0                  0 0 0
 * 0 0 0                  0 0 0                  0 0 0
 * Count = 1              Count = 2              Count: 2 + 1 - 2 merges = 1!
 * 
 * --- Dry Run (Union-Find) ---
 * Operations: [0,0], [0,2], [0,1]. Grid 3x3.
 * Init: parent array of size 9, all filled with -1 (meaning water). count = 0.
 * 
 * Pos [0,0]: index = 0. parent[0] = 0. count = 1. Neighbors? None valid/land. Result: 1
 * Pos [0,2]: index = 2. parent[2] = 2. count = 2. Neighbors? [0,1] is water. Result: 2
 * Pos [0,1]: index = 1. parent[1] = 1. count = 3.
 *   - Neighbor Left (0,0): index 0. Land! Union(1, 0). Different roots -> merge. count = 2.
 *   - Neighbor Right (0,2): index 2. Land! Union(1, 2). Different roots -> merge. count = 1.
 * Result: 1
 * Output array: [1, 2, 1]
 * 
 * --- Pitfalls ---
 * 1. Forgetting to handle duplicate positions. If you unconditionally do `count++`, 
 *    adding [0,0] twice will incorrectly boost the count.
 * 2. Initializing `parent` array with 0. In a flattened grid, 0 is a valid index 
 *    (row 0, col 0). You must initialize with -1 to indicate "unexplored water".
 * 
 * --- Pattern Recognition ---
 * When you see: "Grid state changing over time", "Adding connections dynamically", "Counting components".
 * Think: Union-Find (Disjoint Set).
 * Transfers to: Bricks Falling When Hit, Number of Connected Components, Redundant Connection.
 * 
 * --- Interview Script ---
 * "Since we're building the grid progressively and need a running count of connected components, 
 * running DFS every time would be O(K*M*N), which is way too slow. Instead, this is a classic 
 * dynamic connectivity problem perfect for Union-Find. I'll map the 2D coordinates to a 1D index. 
 * Every time we add land, I increment the island count. Then, I check its 4 neighbors. If a neighbor 
 * is already land, I union their sets. Every successful union means two separate islands just merged 
 * into one, so I decrement the global count. This gives us amortized O(1) time per operation, 
 * making the whole process O(K + M*N)."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if m and n are huge (e.g., 10^9) but positions length is small (10^4)?
 * A: Creating a 1D array of size 10^18 would throw an OutOfMemoryError. I would change the 
 *    `parent` array into a `HashMap<Long, Long>` where the key is the flattened index. 
 *    We only allocate memory for the coordinates actually touched.
 * Q2: What if we can also REMOVE land dynamically?
 * A: Standard Union-Find cannot easily "un-union" things. We would either need to process 
 *    operations in reverse (Time Travel approach, like in "Bricks Falling When Hit"), or 
 *    keep a more complex Dynamic Connectivity graph structure (Link-Cut trees, which are overkill).
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Union-Find / Disjoint Set on a flattened grid.
 * Key observation: count changes by +1 for new land, and -1 for every successful neighbor union.
 * Memorize: Flatten 2D to 1D via `index = r * cols + c`.
 * Most common trap: Not checking for duplicate input operations (adding land where it already exists).
 * Mental trigger: "Add element, count islands" -> Union-Find.
 */

import java.util.*;

public class NumberOfIslandsII {

    public List<Integer> numIslands2(int m, int n, int[][] positions) {
        List<Integer> result = new ArrayList<>();
        
        // Edge case: no operations
        if (positions == null || positions.length == 0) return result;
        
        // 1. Initialize Union-Find
        // size m * n represents the flattened 2D grid.
        UnionFind uf = new UnionFind(m * n);
        
        // Direction vectors for checking Up, Down, Left, Right
        // E.g., adding (0,1) to (r,c) goes right.
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        // Keep track of the current number of islands globally
        int currentIslandCount = 0;
        
        // 2. Process each land addition
        for (int[] pos : positions) {
            int r = pos[0];
            int c = pos[1];
            
            // Flatten 2D coordinate to 1D index
            int index = r * n + c;
            
            // Pitfall Prevention: If it's already land, don't recount it!
            if (uf.isLand(index)) {
                result.add(currentIslandCount);
                continue;
            }
            
            // Turn water into a new isolated island
            uf.setParent(index);
            currentIslandCount++; // Assume it's a new disconnected island initially
            
            // 3. Look at all 4 adjacent neighbors
            for (int[] dir : directions) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                int neighborIndex = nr * n + nc;
                
                // If neighbor is within bounds AND is land
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && uf.isLand(neighborIndex)) {
                    // Try to connect the newly added land with the neighboring land
                    boolean merged = uf.union(index, neighborIndex);
                    
                    // If they had different roots and successfully merged, 
                    // two islands just became one, so we decrement the count.
                    if (merged) {
                        currentIslandCount--;
                    }
                }
            }
            
            // Record the state of the board after this operation
            result.add(currentIslandCount);
        }
        
        return result;
    }
    
    /**
     * Standard Disjoint Set (Union-Find) tailored for grid operations.
     */
    private class UnionFind {
        private int[] parent;
        
        public UnionFind(int size) {
            parent = new int[size];
            // Initialize with -1 to indicate "Water"
            // (Since 0 is a valid grid index, we can't use 0 as the default 'empty' state)
            Arrays.fill(parent, -1);
        }
        
        // Checks if a specific cell has been turned into land
        public boolean isLand(int i) {
            return parent[i] != -1;
        }
        
        // Initializes a new piece of land to be its own root
        public void setParent(int i) {
            parent[i] = i;
        }
        
        // Find with path compression (keeps the tree completely flat for near O(1) lookups)
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            return parent[i] = find(parent[i]); 
        }
        
        // Unites two sets. Returns TRUE if a merge actually happened (they were separate),
        // Returns FALSE if they were already part of the same island.
        public boolean union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            
            if (rootI != rootJ) {
                parent[rootI] = rootJ; // Connect them
                return true;
            }
            return false;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        NumberOfIslandsII solution = new NumberOfIslandsII();

        // Test Case 1: Standard bridging example (Matches ASCII example in comments)
        // Add (0,0), then (0,2), then bridge with (0,1)
        int m1 = 3, n1 = 3;
        int[][] pos1 = {{0,0}, {0,2}, {0,1}};
        // Expected: [1, 2, 1]
        System.out.println("Test 1 Result: " + solution.numIslands2(m1, n1, pos1));

        // Test Case 2: Redundant / Duplicate operations
        // Add same coordinate twice.
        int m2 = 3, n2 = 3;
        int[][] pos2 = {{0,0}, {0,1}, {1,2}, {1,2}};
        // Expected: [1, 1, 2, 2]
        System.out.println("Test 2 Result: " + solution.numIslands2(m2, n2, pos2));

        // Test Case 3: Merging a U-Shape (connecting 3 regions at once)
        // Add (0,0), (0,2), (2,1) -> 3 isolated islands
        // Add (0,1) -> connects (0,0) and (0,2). Count drops from 3 to 2.
        // Add (1,1) -> connects the top bar to (2,1). Count drops from 2 to 1.
        int m3 = 3, n3 = 3;
        int[][] pos3 = {{0,0}, {0,2}, {2,1}, {0,1}, {1,1}};
        // Expected: [1, 2, 3, 2, 1]
        System.out.println("Test 3 Result: " + solution.numIslands2(m3, n3, pos3));
        
        // Test Case 4: 1D grid edge case
        int m4 = 1, n4 = 5;
        int[][] pos4 = {{0,1}, {0,3}, {0,2}};
        // Expected: [1, 2, 1]
        System.out.println("Test 4 Result: " + solution.numIslands2(m4, n4, pos4));
    }
}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Number of Islands II
 *
 * Problem:
 * Initially, every cell in an m x n grid is water.
 * Each operation converts one cell into land.
 *
 * After every operation, return the number of islands.
 *
 * Connectivity:
 * - Horizontal and vertical connections count.
 * - Diagonal connections do not count.
 *
 * Island = connected component of land cells.
 */
public class NumberOfIslandsII {

    // Four possible directions of movement.
    // We intentionally exclude diagonal directions.
    private static final int[][] DIRECTIONS = {
        {-1, 0},  // Up
        {1, 0},   // Down
        {0, -1},  // Left
        {0, 1}    // Right
    };

    // ========================================================================
    // APPROACH 1: ARRAY-BASED UNION-FIND
    // ========================================================================
    //
    // Recommended for the given constraints.
    //
    // Time:
    //   O(m * n + k * alpha(m * n))
    //   Initialization takes O(m * n).
    //   Each operation performs at most four unions.
    //
    // Space:
    //   O(m * n) auxiliary space.
    //
    // Why this is efficient:
    //   Arrays provide constant-time indexing and avoid HashMap overhead.
    //
    // Key idea:
    //   1. Adding a new land cell creates one island: count++.
    //   2. Every successful union merges two islands: count--.
    //   3. If two cells already belong to the same island, do nothing.
    //
    // ========================================================================

    /**
     * Returns the island count after every land-addition operation.
     */
    public List<Integer> numIslands2(
            int m,
            int n,
            int[][] positions) {

        // Preallocate the result list because we know the number of operations.
        List<Integer> result = new ArrayList<>(positions.length);

        // Convert each 2D cell (row, col) into a unique 1D index:
        //
        // index = row * n + col
        //
        // Therefore, the complete grid can be represented by m * n elements.
        int totalCells = m * n;

        // Track which cells have become land.
        //
        // A cell may have a valid DSU index while still being water.
        // This array prevents water cells from participating in unions.
        boolean[][] hasLand = new boolean[m][n];

        // Create a DSU capable of representing every grid cell.
        UnionFind uf = new UnionFind(totalCells);

        // Process operations in the exact order provided.
        for (int[] position : positions) {

            int row = position[0];
            int col = position[1];

            // Convert the current cell to its unique 1D index.
            int current = row * n + col;

            // ------------------------------------------------------------
            // STEP 1: Handle duplicate operations.
            // ------------------------------------------------------------
            //
            // Adding land to an already-land cell changes nothing.
            // In particular, it must not increment the island count again.
            if (hasLand[row][col]) {
                result.add(uf.getCount());
                continue;
            }

            // ------------------------------------------------------------
            // STEP 2: Activate the new land cell.
            // ------------------------------------------------------------

            hasLand[row][col] = true;

            // Initially, the new cell is a separate connected component.
            // Therefore, the island count increases by exactly one.
            uf.addLand(current);

            // ------------------------------------------------------------
            // STEP 3: Connect the new cell to existing neighboring land.
            // ------------------------------------------------------------

            for (int[] direction : DIRECTIONS) {

                int neighborRow = row + direction[0];
                int neighborCol = col + direction[1];

                // Ignore neighbors outside the grid boundaries.
                if (neighborRow < 0 || neighborRow >= m
                        || neighborCol < 0 || neighborCol >= n) {
                    continue;
                }

                // Water cells cannot be part of a land connection.
                if (!hasLand[neighborRow][neighborCol]) {
                    continue;
                }

                // Convert the valid neighboring cell to its 1D index.
                int neighbor = neighborRow * n + neighborCol;

                // If these cells belong to different islands, union()
                // merges them and decrements the island count.
                //
                // If they are already connected, union() does nothing.
                uf.union(current, neighbor);
            }

            // ------------------------------------------------------------
            // STEP 4: Record the result after all four neighbors are checked.
            // ------------------------------------------------------------

            result.add(uf.getCount());
        }

        return result;
    }

    /**
     * Union-Find implementation for the dense grid.
     *
     * parent[x] == -1 means the cell has not been activated as land.
     * parent[x] == x means x is the root of an active component.
     *
     * rank[root] estimates the height of the component tree.
     */
    private static class UnionFind {

        private final int[] parent;
        private final int[] rank;

        // Number of connected components containing active land cells.
        private int count;

        UnionFind(int totalCells) {

            parent = new int[totalCells];
            rank = new int[totalCells];

            // -1 is our marker for water / inactive cells.
            //
            // This is important because the DSU is allocated for the entire
            // grid, even though initially every cell is water.
            Arrays.fill(parent, -1);

            // Java initializes rank[] to zero automatically.
            //
            // The first activation initializes a land cell's rank to 0.
            count = 0;
        }

        /**
         * Activates a cell as land.
         *
         * Initially, this cell is its own connected component.
         */
        void addLand(int x) {

            // Defensive check: never activate an existing land cell twice.
            if (parent[x] != -1) {
                return;
            }

            // A new component starts with itself as its root.
            parent[x] = x;

            // A singleton tree has rank zero.
            rank[x] = 0;

            // One new island has been created.
            count++;
        }

        /**
         * Finds the root of the component containing x.
         *
         * Path compression makes future find operations faster by
         * redirecting visited nodes directly toward the root.
         */
        int find(int x) {

            // Water cells do not belong to any active component.
            if (parent[x] == -1) {
                return -1;
            }

            // A root is identified by parent[x] == x.
            if (parent[x] != x) {

                // Recursively find the root and compress the path.
                parent[x] = find(parent[x]);
            }

            return parent[x];
        }

        /**
         * Merges the components containing x and y.
         *
         * @return true if two different components were merged;
         *         false if no merge was necessary.
         */
        boolean union(int x, int y) {

            // Find the representative of each component.
            int rootX = find(x);
            int rootY = find(y);

            // A water cell cannot be connected.
            if (rootX == -1 || rootY == -1) {
                return false;
            }

            // If both cells have the same root, they already belong
            // to the same island.
            //
            // Do not decrement the island count in this case.
            if (rootX == rootY) {
                return false;
            }

            // ------------------------------------------------------------
            // Union by rank:
            // Attach the shallower tree below the deeper tree.
            // ------------------------------------------------------------

            if (rank[rootX] < rank[rootY]) {

                // rootY's tree is taller, so attach rootX below rootY.
                parent[rootX] = rootY;

            } else if (rank[rootX] > rank[rootY]) {

                // rootX's tree is taller, so attach rootY below rootX.
                parent[rootY] = rootX;

            } else {

                // Both trees have equal rank.
                // Choose rootX as the new root.
                parent[rootY] = rootX;

                // The resulting tree's rank increases by one.
                rank[rootX]++;
            }

            // Exactly two connected components became one.
            count--;

            return true;
        }

        /**
         * Returns the current number of islands.
         */
        int getCount() {
            return count;
        }
    }


    // ========================================================================
    // APPROACH 2: SPARSE UNION-FIND WITH INTEGER IDS
    // ========================================================================
    //
    // Time:
    //   Expected O(k * alpha(k)) for processing operations,
    //   excluding output storage and assuming ordinary HashMap operations.
    //
    // Space:
    //   O(k) auxiliary space.
    //
    // When to use:
    //   m * n is extremely large, but only k cells become land.
    //
    // Improvement over the string-key version:
    //   Use integer IDs instead of strings such as "2,3".
    //   This avoids building and hashing coordinate strings repeatedly.
    //
    // Important:
    //   row * n + col must fit in an int. For grids whose total number
    //   of cells exceeds Integer.MAX_VALUE, use long IDs instead.
    //
    // ========================================================================

    /**
     * Sparse-grid implementation.
     *
     * Unlike Approach 1, this method does not allocate arrays for every
     * grid cell. The DSU stores only cells that have actually become land.
     */
    public List<Integer> numIslands2Sparse(
            int m,
            int n,
            int[][] positions) {

        List<Integer> result = new ArrayList<>(positions.length);

        // All DSU maps contain active land cells only.
        SparseUnionFind uf = new SparseUnionFind();

        for (int[] position : positions) {

            int row = position[0];
            int col = position[1];

            // Every coordinate has a unique ID for the given grid.
            int current = row * n + col;

            // If parent contains the ID, the cell is already land.
            // This avoids needing a separate boolean grid or HashSet.
            if (uf.isLand(current)) {
                result.add(uf.getCount());
                continue;
            }

            // Activate the new cell as its own island.
            uf.addLand(current);

            // Try to connect it to each existing neighboring land cell.
            for (int[] direction : DIRECTIONS) {

                int neighborRow = row + direction[0];
                int neighborCol = col + direction[1];

                // Ensure that the neighbor is inside the grid.
                if (neighborRow < 0 || neighborRow >= m
                        || neighborCol < 0 || neighborCol >= n) {
                    continue;
                }

                int neighbor = neighborRow * n + neighborCol;

                // A neighbor must already be land.
                if (uf.isLand(neighbor)) {

                    // Decrease the count only if a real merge occurs.
                    uf.union(current, neighbor);
                }
            }

            // Store the island count for this operation.
            result.add(uf.getCount());
        }

        return result;
    }

    /**
     * Sparse DSU that stores only active land cells.
     */
    private static class SparseUnionFind {

        // parent contains an entry only for active land cells.
        private final Map<Integer, Integer> parent = new HashMap<>();

        // Rank is stored only for active cells.
        private final Map<Integer, Integer> rank = new HashMap<>();

        private int count = 0;

        /**
         * Checks whether a cell has been activated as land.
         */
        boolean isLand(int x) {
            return parent.containsKey(x);
        }

        /**
         * Adds a new singleton component.
         */
        void addLand(int x) {

            // Avoid duplicate activation.
            if (parent.containsKey(x)) {
                return;
            }

            parent.put(x, x);
            rank.put(x, 0);

            // The new land cell initially forms one island.
            count++;
        }

        /**
         * Finds the component root with path compression.
         */
        int find(int x) {

            // This method is called for active cells only.
            int p = parent.get(x);

            if (p != x) {
                // Recursively find the root.
                // Store it directly to compress the path.
                p = find(p);
                parent.put(x, p);
            }

            return p;
        }

        /**
         * Merges two distinct components, if necessary.
         */
        boolean union(int x, int y) {

            int rootX = find(x);
            int rootY = find(y);

            // Both cells are already part of the same island.
            if (rootX == rootY) {
                return false;
            }

            int rankX = rank.get(rootX);
            int rankY = rank.get(rootY);

            // Attach the shallower tree beneath the deeper tree.
            if (rankX < rankY) {

                parent.put(rootX, rootY);

            } else if (rankX > rankY) {

                parent.put(rootY, rootX);

            } else {

                parent.put(rootY, rootX);

                // Equal ranks mean the new tree becomes one level taller.
                rank.put(rootX, rankX + 1);
            }

            // One successful merge means one fewer island.
            count--;

            return true;
        }

        int getCount() {
            return count;
        }
    }


    // ========================================================================
    // APPROACH 3: SPARSE UNION-FIND WITH STRING KEYS
    // ========================================================================
    //
    // Time:
    //   Expected O(k * alpha(k)) DSU work, with additional string creation
    //   and hashing costs for coordinates.
    //
    // Space:
    //   O(k) entries, but strings generally have greater memory overhead
    //   than integer keys.
    //
    // When to use:
    //   Mainly for comparison or when string identifiers are already
    //   natural inputs to the problem.
    //
    // For this grid problem, Approach 2 is usually preferable.
    //
    // ========================================================================

    /**
     * String-key implementation similar to the original version.
     *
     * A coordinate such as (2, 3) is represented by the key "2,3".
     */
    public List<Integer> numIslands2StringKeys(
            int m,
            int n,
            int[][] positions) {

        List<Integer> result = new ArrayList<>(positions.length);

        StringUnionFind uf = new StringUnionFind();

        // We need a way to check whether a neighboring coordinate is land.
        Set<String> land = new HashSet<>();

        for (int[] position : positions) {

            int row = position[0];
            int col = position[1];

            // Create a unique coordinate key.
            String current = key(row, col);

            // Duplicate operation: nothing changes.
            if (land.contains(current)) {
                result.add(uf.getCount());
                continue;
            }

            // Activate this coordinate.
            land.add(current);
            uf.addLand(current);

            // Check all four neighboring cells.
            for (int[] direction : DIRECTIONS) {

                int neighborRow = row + direction[0];
                int neighborCol = col + direction[1];

                if (neighborRow < 0 || neighborRow >= m
                        || neighborCol < 0 || neighborCol >= n) {
                    continue;
                }

                String neighbor = key(neighborRow, neighborCol);

                // Only active land can participate in a union.
                if (land.contains(neighbor)) {
                    uf.union(current, neighbor);
                }
            }

            result.add(uf.getCount());
        }

        return result;
    }

    /**
     * Converts coordinates into an unambiguous string key.
     */
    private static String key(int row, int col) {
        return row + "," + col;
    }

    /**
     * String-based Union-Find.
     */
    private static class StringUnionFind {

        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Integer> rank = new HashMap<>();

        private int count = 0;

        /**
         * Adds a new coordinate as its own component.
         */
        void addLand(String x) {

            if (parent.containsKey(x)) {
                return;
            }

            parent.put(x, x);
            rank.put(x, 0);
            count++;
        }

        /**
         * Finds the root of the component containing x.
         */
        String find(String x) {

            // Return null if the coordinate is not active.
            if (!parent.containsKey(x)) {
                return null;
            }

            String p = parent.get(x);

            if (!p.equals(x)) {

                // Path compression: store the root directly.
                p = find(p);
                parent.put(x, p);
            }

            return p;
        }

        /**
         * Merges two distinct components.
         */
        boolean union(String x, String y) {

            String rootX = find(x);
            String rootY = find(y);

            // Defensive check for inactive cells.
            if (rootX == null || rootY == null) {
                return false;
            }

            // No merge is required if both cells share a root.
            if (rootX.equals(rootY)) {
                return false;
            }

            int rankX = rank.get(rootX);
            int rankY = rank.get(rootY);

            // Union by rank.
            if (rankX < rankY) {

                parent.put(rootX, rootY);

            } else if (rankX > rankY) {

                parent.put(rootY, rootX);

            } else {

                parent.put(rootY, rootX);
                rank.put(rootX, rankX + 1);
            }

            count--;

            return true;
        }

        int getCount() {
            return count;
        }
    }


    // ========================================================================
    // QUICK TEST
    // ========================================================================

    public static void main(String[] args) {

        NumberOfIslandsII solution = new NumberOfIslandsII();

        int m = 3;
        int n = 3;

        int[][] positions = {
            {0, 0},
            {0, 1},
            {1, 2},
            {2, 1},
            {1, 1}
        };

        // All three approaches should return [1, 1, 2, 3, 1].
        System.out.println("Array DSU: "
                + solution.numIslands2(m, n, positions));

        System.out.println("Sparse integer DSU: "
                + solution.numIslands2Sparse(m, n, positions));

        System.out.println("String-key DSU: "
                + solution.numIslands2StringKeys(m, n, positions));
    }
}

