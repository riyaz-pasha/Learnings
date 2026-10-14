/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given an array of `stones` where stones[i] = [x, y], we can remove a stone 
 * if it shares either the same row or the same column with another unremoved stone.
 * Return the MAXIMUM possible number of stones that can be removed.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can two stones exist at the exact same coordinate?
 *    (Assuming no based on standard graph problems, but if yes, we'd just count duplicates as free removals).
 * 2. Is there a specific order required for removal, or do we just need the max count?
 *    (Order doesn't matter for the output, which means we can look at the global structure rather than simulating step-by-step).
 * 3. What if a stone shares a row with one stone and a column with another?
 *    (It connects ALL of them together into one large group. Transitive connections matter).
 * 4. Are coordinates bounded?
 *    (Prompt says 0 <= xi, yi <= 50, but it's good to ask if this might scale to 10^4 or 10^9 to decide between array vs HashMap for coordinates).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: We want to maximize removals. If we greedily remove a stone 
 * that connects two different clusters, we might accidentally isolate a cluster, making 
 * its remaining stones un-removable. The constraint is avoiding orphans.
 * 
 * --- Approach 1: Brute Force Backtracking (Simulate Removals) ---
 * Idea: Try removing every valid stone. Recurse on the remaining stones. Keep track of 
 * the maximum depth we can reach. 
 * What I'd naturally try: A recursive function `solve(Set<Stones>)` that loops through all stones.
 * Why it's too slow/costly: For N stones, we have N choices, then N-1 choices. 
 * What work is repeated: We evaluate the same resulting board state through many different 
 * permutations of removal orders.
 * 
 * Time Complexity: O(N!) — because we explore all valid permutations of stone removals.
 * Space Complexity: O(N) — because the recursion stack goes at most N deep.
 * 
 * --- Approach 2: Depth-First Search (DFS) on Stones (Connected Components) ---
 * Observation: Think of the stones as nodes in a graph. An edge exists between two stones 
 * if they share a row or a column. 
 * Insight: In ANY connected graph of K nodes, we can always find a spanning tree. A tree 
 * always has at least one "leaf" node (degree 1). If we repeatedly remove the leaf nodes, 
 * we can remove K-1 nodes without ever disconnecting the remaining structure! We just leave 
 * 1 stone as the final anchor.
 * Idea: Build an adjacency list of stones. Run DFS to find how many connected components (C) exist. 
 * Max removed = Total Stones (N) - Number of Components (C).
 * 
 * Time Complexity: O(N^2) — because to build the adjacency list, we must compare every stone with every other stone to check if x1==x2 or y1==y2.
 * Space Complexity: O(N^2) — because the graph could have up to N^2 edges if all stones share rows/cols.
 * 
 * --- Approach 3: Union-Find on Coordinates (Optimal) ---
 * Idea: O(N^2) is slow. Instead of connecting Stone A to Stone B, what if we connect 
 * Row X to Column Y? 
 * When a stone is placed at (x, y), it acts as a bridge uniting the entity "Row x" 
 * with the entity "Column y". If another stone is at (x, z), it unites "Row x" with "Column z". 
 * By transitivity, Column y and Column z are now connected!
 * We iterate over the stones, and `union(x, y)`. Since X and Y domains might overlap 
 * (e.g., row 2 and col 2 are different concepts but same number), we differentiate 
 * columns by storing them as their bitwise NOT (`~y`), which safely maps 0 to -1, 1 to -2, etc.
 * 
 * Time Complexity: O(N * α(N)) — because we process N stones, and each Union operation takes amortized O(1) time. (α is the inverse Ackermann function).
 * Space Complexity: O(V) — where V is the number of UNIQUE rows and columns containing stones (up to 2*N). Vastly better than O(N^2) edges!
 * 
 * Interview Decision: I would immediately write **Union-Find on Coordinates**. 
 * It perfectly sidesteps building an N^2 adjacency list and demonstrates advanced 
 * knowledge of domain-separation in graphs.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. N = 1 (A single stone): Returns 0. Handled naturally (N=1, Components=1 -> 0).
 * 2. Completely disconnected stones: Result should be 0.
 * 3. All stones in a single straight line (same row): N stones, 1 component. Returns N-1.
 * 4. Coordinate overlapping: e.g., stone at (2, 2). If we don't differentiate Row 2 from Col 2, 
 *    the graph folds on itself improperly. (Our `~y` trick prevents this).
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "Do the nodes *have* to be the stones?"
 * Correctness intuition: The stones are actually the EDGES in this bipartite graph! 
 * A stone at (x, y) is an edge connecting the node "Row X" to the node "Column Y". 
 * The number of connected components of stones is exactly equal to the number of 
 * connected components of these coordinate nodes.
 * 
 * --- Examples & ASCII ---
 * Stones: [0,0], [0,1], [1,0], [1,2], [2,1], [2,2]
 * 
 * R0 - S - S - .
 * R1 - S - . - S
 * R2 - . - S - S
 *     C0  C1  C2
 * 
 * Every stone is reachable from every other stone through row/col hops. 
 * Total stones = 6. Connected components = 1. 
 * Max removed = 6 - 1 = 5.
 * 
 * --- Dry Run (Union-Find) ---
 * Stones: [0,0], [0,1], [1,1]
 * Init UF with `count = 0`.
 * 
 * Stone 0: [0, 0]
 *   Row 0 -> not seen, add to UF. (count = 1)
 *   Col ~0 (-1) -> not seen, add to UF. (count = 2)
 *   Union(0, -1). They are different, merge them. (count = 1)
 * 
 * Stone 1: [0, 1]
 *   Row 0 -> already seen.
 *   Col ~1 (-2) -> not seen, add. (count = 2)
 *   Union(0, -2). Different, merge. (count = 1)
 * 
 * Stone 2: [1, 1]
 *   Row 1 -> not seen, add. (count = 2)
 *   Col ~1 (-2) -> already seen.
 *   Union(1, -2). Different, merge. (count = 1)
 * 
 * Total Stones = 3. Final Count = 1.
 * Output: 3 - 1 = 2.
 * 
 * --- Pitfalls ---
 * 1. Confusing Row X and Col X. Using an offset (like `y + 10000`) works, but if 
 *    constraints change, it breaks. Bitwise NOT (`~y`) is mathematically foolproof.
 * 2. Overcounting components. If we initialize our Union-Find size to a static large 
 *    number, we'll count empty rows/cols as components. We must only count *active* coordinates.
 * 
 * --- Pattern Recognition ---
 * When you see: "Grid coordinates sharing row or col", "Rooks on a chessboard".
 * Think: Bipartite Graph / Coordinate Union-Find (Rows are nodes, Cols are nodes, items are edges).
 * Transfers to: Minimum Time to Visit All Points, Checking if a grid is fully connected.
 * 
 * --- Interview Script ---
 * "Instead of treating stones as nodes, which gives O(N^2) edges, I'll treat the rows and columns 
 * as nodes. Each stone at (x, y) is just an edge connecting Row x to Column y. This reduces the problem 
 * to finding the number of connected components in this coordinate graph. The maximum number of 
 * stones we can remove is simply the total number of stones minus the number of connected components. 
 * I'll use a Union-Find data structure backed by a HashMap to track the active coordinates, using bitwise 
 * NOT to securely separate row IDs from column IDs. This yields an O(N) time and space solution."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if we needed to return the exact SEQUENCE of removals?
 * A: Union-Find just tells us the max count. To get the sequence, we'd build the Adjacency List 
 *    (Approach 2) and perform a Topological Sort from the leaves inwards, always removing degree-1 
 *    nodes first, leaving the highest degree nodes for last.
 * Q2: What if stones are added dynamically (a stream of points)?
 * A: Our Union-Find approach supports dynamic insertions perfectly! We'd just process the new stone 
 *    `union(x, ~y)` and update our running count of components and total stones in O(1) amortized time.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Bipartite Graph / Union-Find on Coordinates.
 * Key observation: Stones are edges uniting Rows and Columns. Max Removals = Total Stones - Components.
 * Memorize: The `~y` bitwise NOT trick to cleanly separate two domains of numbers in a single Map.
 * Most common trap: Trying to greedily simulate removals and getting trapped in dead ends.
 * Mental trigger: "Share same row or column" -> Row/Col Union-Find.
 */

import java.util.*;

public class RemoveStones {

    public int removeStones(int[][] stones) {
        UnionFind uf = new UnionFind();
        
        // Treat each stone as an edge connecting Row x to Column y.
        for (int[] stone : stones) {
            int row = stone[0];
            // We use bitwise NOT to map y to a strictly negative number.
            // Example: y=0 -> ~y=-1. y=1 -> ~y=-2.
            // This ensures row indices (>=0) never collide with column indices (<0).
            int col = ~stone[1]; 
            
            uf.union(row, col);
        }
        
        // The maximum stones we can remove is total stones minus 
        // the number of disjoint components formed by their coordinates.
        return stones.length - uf.getComponentCount();
    }
    
    /**
     * A HashMap-based Disjoint Set. 
     * Using a map allows us to handle sparse coordinates smoothly without guessing 
     * a massive array size, and automatically tracks ONLY the coordinates that actually have stones.
     */
    private class UnionFind {
        private Map<Integer, Integer> parent;
        private int componentCount;
        
        public UnionFind() {
            parent = new HashMap<>();
            componentCount = 0;
        }
        
        // Find with Path Compression
        public int find(int i) {
            // If we've never seen this coordinate before, initialize it as its own root.
            if (!parent.containsKey(i)) {
                parent.put(i, i);
                componentCount++; // A new isolated component is born
            }
            
            // Traverse up to the root
            if (parent.get(i) != i) {
                // Path compression: point directly to the ultimate root
                parent.put(i, find(parent.get(i)));
            }
            
            return parent.get(i);
        }
        
        // Unites the set containing x with the set containing y
        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            // If they are in different sets, merge them and decrement our component tracker
            if (rootX != rootY) {
                parent.put(rootX, rootY);
                componentCount--; // Two components merged into one
            }
        }
        
        // Returns the number of distinct coordinate clusters
        public int getComponentCount() {
            return componentCount;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        RemoveStones solution = new RemoveStones();

        // Test Case 1: Standard interconnected cluster (Matches ASCII example in comments)
        // Stones: [0,0], [0,1], [1,0], [1,2], [2,1], [2,2]
        // All connected. N=6, Components=1. Expected Removals: 5.
        int[][] stones1 = {{0,0}, {0,1}, {1,0}, {1,2}, {2,1}, {2,2}};
        System.out.println("Test 1 Result: " + solution.removeStones(stones1)); // Expected: 5

        // Test Case 2: Disconnected components
        // Stones: [0,0], [0,2], [1,1], [2,0], [2,2]
        // Contains two distinct clusters: { [1,1] } and { [0,0], [0,2], [2,0], [2,2] }
        // N=5. Components=2. Expected Removals: 3.
        int[][] stones2 = {{0,0}, {0,2}, {1,1}, {2,0}, {2,2}};
        System.out.println("Test 2 Result: " + solution.removeStones(stones2)); // Expected: 3

        // Test Case 3: Completely isolated stone (Edge case)
        // Stones: [0,0]
        // N=1. Components=1. Expected Removals: 0.
        int[][] stones3 = {{0,0}};
        System.out.println("Test 3 Result: " + solution.removeStones(stones3)); // Expected: 0
        
        // Test Case 4: Overlapping coordinates X and Y
        // Stones: [2,2], [2,3], [3,2]
        // If we didn't differentiate Row 2 from Col 2, logic would collapse.
        // N=3. Components=1. Expected Removals: 2.
        int[][] stones4 = {{2,2}, {2,3}, {3,2}};
        System.out.println("Test 4 Result: " + solution.removeStones(stones4)); // Expected: 2
    }
}



import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode 947: Most Stones Removed with Same Row or Column
 *
 * PROBLEM:
 * Given stones[i] = [x, y], a stone can be removed if another
 * remaining stone shares its row (same x) or column (same y).
 *
 * Return the maximum number of stones that can be removed.
 *
 * ================================================================
 * CORE IDEA: CONNECTED COMPONENTS
 * ================================================================
 *
 * Think of stones as belonging to connected groups.
 *
 * Two stones are directly connected when:
 *   1. They share the same row, OR
 *   2. They share the same column.
 *
 * They are in the same connected component even if they are
 * connected indirectly through other stones.
 *
 * Example:
 *
 *   Stone A = (0, 0)
 *   Stone B = (0, 1)
 *   Stone C = (1, 1)
 *
 *   A ---- B
 *          |
 *          C
 *
 * A and C do not directly share a row or column, but they are
 * connected through B. All three belong to the same component.
 *
 * WHY IS THE ANSWER n - COMPONENTS?
 *
 * Suppose a connected component contains k stones.
 *
 * We can remove k - 1 stones, leaving one stone behind.
 * The last stone cannot be removed because no other stone remains
 * in its connected component to support its removal.
 *
 * Therefore:
 *
 *   Maximum removals = (size of component 1 - 1)
 *                    + (size of component 2 - 1)
 *                    + ...
 *                    + (size of component C - 1)
 *
 *                    = total stones - C
 *
 *   ANSWER = n - number of connected components
 *
 * We do not actually need to remove any stones. We only need
 * to identify the connected components.
 *
 * This is a perfect use case for Union-Find (DSU).
 *
 * ================================================================
 * APPROACH 1: STONE-BASED DSU + ROW/COLUMN MAPS
 * ================================================================
 *
 * IDEA:
 *   Each stone is a DSU node.
 *
 *   Stone 0 = DSU node 0
 *   Stone 1 = DSU node 1
 *   Stone 2 = DSU node 2
 *   ...
 *
 * HOW DO WE FIND CONNECTED STONES EFFICIENTLY?
 *
 * Maintain two maps:
 *
 *   rowMap: row coordinate    -> previously seen stone index
 *   colMap: column coordinate -> previously seen stone index
 *
 * For every stone i = (x, y):
 *
 *   1. If row x has appeared before, union the current stone
 *      with the previously seen stone in that row.
 *
 *   2. Update rowMap[x] to the current stone index.
 *
 *   3. If column y has appeared before, union the current stone
 *      with the previously seen stone in that column.
 *
 *   4. Update colMap[y] to the current stone index.
 *
 * WHY IS REMEMBERING ONLY ONE STONE PER ROW/COLUMN ENOUGH?
 *
 * Suppose a row contains stones A, B, C, D.
 *
 * Processing them in order creates these connections:
 *
 *   A -- B -- C -- D
 *
 * Every new stone is connected to a stone already belonging to
 * the same component. Therefore, all four stones become connected.
 *
 * We do not need to store every stone in the row or column.
 *
 * DSU COMPONENT COUNT:
 *
 * Initially, every stone is a separate component:
 *
 *   components = n
 *
 * Every successful union merges two different components:
 *
 *   components--
 *
 * At the end:
 *
 *   answer = n - components
 *
 * COMPLEXITY:
 *
 *   Time:  O(n * alpha(n)) expected, assuming expected O(1)
 *          HashMap operations.
 *
 *   Space: O(n), for DSU arrays and the two maps.
 *
 *   alpha(n) is the inverse Ackermann function, which is
 *   effectively constant for practical input sizes.
 *
 * RECOMMENDATION:
 *   This is the preferred interview solution because every
 *   DSU node represents an actual stone.
 */
public class MostStonesRemoved {

    /**
     * APPROACH 1: Stone-based Union-Find.
     *
     * @param stones each element contains [row, column]
     * @return maximum number of stones removable
     */
    public int removeStonesStoneBased(int[][] stones) {

        int n = stones.length;

        // Initially, each stone is its own connected component.
        // Therefore, the initial component count is n.
        StoneUnionFind uf = new StoneUnionFind(n);

        // Maps a row coordinate to a previously processed stone index.
        // Example: rowMap.get(2) = 5 means stone 5 was the latest
        // processed stone found in row 2.
        Map<Integer, Integer> rowMap = new HashMap<>();

        // Maps a column coordinate to a previously processed stone index.
        Map<Integer, Integer> colMap = new HashMap<>();

        // Process each stone exactly once.
        for (int i = 0; i < n; i++) {

            int row = stones[i][0];
            int col = stones[i][1];

            // -----------------------------------------------------
            // STEP 1: CONNECT STONES SHARING THE SAME ROW
            // -----------------------------------------------------

            // If this row already has a stone, the current stone
            // shares its row with that previously seen stone.
            if (rowMap.containsKey(row)) {

                int previousStone = rowMap.get(row);

                // Merge their connected components.
                //
                // They might already be in the same component
                // through some other connection. In that case,
                // union() will do nothing.
                uf.union(i, previousStone);
            }

            // Remember the current stone for this row.
            //
            // Overwriting the previous index is safe:
            // the current stone was just connected to the previous
            // stone, so both belong to the same component.
            rowMap.put(row, i);

            // -----------------------------------------------------
            // STEP 2: CONNECT STONES SHARING THE SAME COLUMN
            // -----------------------------------------------------

            if (colMap.containsKey(col)) {

                int previousStone = colMap.get(col);

                // Merge the current stone's component with the
                // component containing the previous stone in this column.
                uf.union(i, previousStone);
            }

            // Remember the current stone for this column.
            colMap.put(col, i);
        }

        // ---------------------------------------------------------
        // STEP 3: CALCULATE THE ANSWER
        // ---------------------------------------------------------

        // Each connected component must retain one stone.
        // All other stones can be removed.
        return n - uf.getComponentCount();
    }


    /**
     * Union-Find implementation for APPROACH 1.
     *
     * Every DSU element represents one stone.
     *
     * Two optimizations:
     *
     * 1. Path compression:
     *    During find(x), make x point directly to the root.
     *    Future lookups become faster.
     *
     * 2. Union by size:
     *    Attach the smaller component under the larger component's
     *    root to keep the trees shallow.
     */
    private static class StoneUnionFind {

        private final int[] parent;
        private final int[] size;

        // Number of connected components among the stones.
        private int components;

        StoneUnionFind(int n) {

            parent = new int[n];
            size = new int[n];

            // Initially, every stone is its own root.
            // Each component contains exactly one stone.
            components = n;

            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        /**
         * Returns the root representing the component containing x.
         */
        int find(int x) {

            // If x is its own parent, x is the root.
            if (parent[x] == x) {
                return x;
            }

            // PATH COMPRESSION:
            // Recursively find the root and make x point directly to it.
            parent[x] = find(parent[x]);

            return parent[x];
        }

        /**
         * Merges the components containing a and b.
         *
         * If they are already in the same component, nothing changes.
         */
        void union(int a, int b) {

            // Never merge nodes directly without first finding roots.
            int rootA = find(a);
            int rootB = find(b);

            // Same root means the two stones are already connected,
            // perhaps through an indirect chain of other stones.
            if (rootA == rootB) {
                return;
            }

            // UNION BY SIZE:
            // Keep the larger component's root as the new root.
            if (size[rootA] < size[rootB]) {

                int temp = rootA;
                rootA = rootB;
                rootB = temp;
            }

            // Attach the smaller component's root to the larger one.
            parent[rootB] = rootA;

            // Update the size of the resulting component.
            size[rootA] += size[rootB];

            // Two components have now become one.
            components--;
        }

        /**
         * Returns the number of connected components.
         */
        int getComponentCount() {
            return components;
        }
    }


    // =================================================================
    // APPROACH 2: COORDINATE-BASED DSU
    // =================================================================
    //
    // IDEA:
    //
    // Instead of making each stone a node, make each row and column
    // a separate node.
    //
    // A stone (x, y) represents an edge:
    //
    //       Row x -------- Column y
    //
    // For example, stones:
    //
    //       (0, 0)
    //       (0, 1)
    //       (1, 0)
    //
    // Produce these connections:
    //
    //       Row 0 -------- Column 0
    //          |                 |
    //          |                 |
    //       Column 1          Row 1
    //
    // All three stones belong to the same connected group.
    //
    // WHY DOES THIS REPRESENTATION WORK?
    //
    // Two stones sharing a row share the same row node.
    // Two stones sharing a column share the same column node.
    //
    // Therefore, a connected group of stones corresponds exactly
    // to a connected component of row/column nodes containing edges.
    //
    // ENCODING ROWS AND COLUMNS:
    //
    // We need distinct identifiers for row 0 and column 0.
    //
    // This implementation uses:
    //
    //       row node    = x
    //       column node = ~y
    //
    // In Java, ~y equals -y - 1.
    //
    // Since the constraints guarantee x >= 0 and y >= 0:
    //
    //       every row node    is nonnegative
    //       every column node is negative
    //
    // So a row identifier can never collide with a column identifier.
    //
    // Example:
    //
    //       Row 0    ->  0
    //       Row 1    ->  1
    //       Column 0 -> -1
    //       Column 1 -> -2
    //
    // The HashMap-based DSU creates nodes only when they are needed.
    // This also works when coordinates are large and sparse.
    //
    // COMPONENT COUNT:
    //
    // Initially, no coordinate nodes exist:
    //
    //       components = 0
    //
    // When find(x) encounters an unseen coordinate:
    //       create its node and increment components.
    //
    // When union(a, b) merges two distinct components:
    //       decrement components.
    //
    // At the end, getComponentCount() returns the number of coordinate
    // components containing the stones.
    //
    // ANSWER:
    //
    //       stones.length - number of connected components
    //
    // COMPLEXITY:
    //
    // Let m be the number of distinct row/column identifiers.
    // Here m <= 2n.
    //
    // With path compression AND union by size:
    //
    //       Expected time:  O(n * alpha(n))
    //       Expected space: O(n)
    //
    // HashMap operations are expected O(1).
    //
    // NOTE:
    // The bitwise-complement encoding relies on nonnegative coordinates.
    // For arbitrary signed coordinates, use coordinate compression
    // or separate row and column identifier namespaces instead.
    // =================================================================

    /**
     * APPROACH 2: Coordinate-based Union-Find.
     *
     * @param stones each element contains [row, column]
     * @return maximum number of stones removable
     */
    public int removeStonesCoordinateBased(int[][] stones) {

        CoordinateUnionFind uf = new CoordinateUnionFind();

        // Each stone connects its row node to its column node.
        for (int[] stone : stones) {

            int row = stone[0];

            // Convert the column coordinate to a negative identifier.
            // For example, column 0 becomes -1, column 1 becomes -2.
            int col = ~stone[1];

            // Register both coordinate nodes and connect them.
            //
            // find() creates a node if it has not been seen before.
            // union() merges their components when they are different.
            uf.union(row, col);
        }

        // Every connected coordinate component corresponds to one
        // connected group of stones.
        //
        // One stone must remain in each such group.
        return stones.length - uf.getComponentCount();
    }


    /**
     * Union-Find implementation for APPROACH 2.
     *
     * Unlike the first DSU, this implementation cannot use array
     * indices directly because encoded coordinates may be negative
     * or large. We therefore use HashMaps.
     */
    private static class CoordinateUnionFind {

        // parent.get(x) gives the parent of coordinate node x.
        private final Map<Integer, Integer> parent = new HashMap<>();

        // size.get(root) gives the size of a component's root.
        private final Map<Integer, Integer> size = new HashMap<>();

        // Number of connected components among registered coordinate nodes.
        private int components = 0;

        /**
         * Returns the root of x.
         *
         * If x has never appeared before, initialize it as a new
         * singleton component.
         */
        int find(int x) {

            // LAZY INITIALIZATION:
            // Only coordinates used by actual stones are registered.
            if (!parent.containsKey(x)) {

                parent.put(x, x);
                size.put(x, 1);

                // A new isolated component has been created.
                components++;
            }

            // A root is a node whose parent is itself.
            if (parent.get(x) == x) {
                return x;
            }

            // PATH COMPRESSION:
            // Replace x's parent with the ultimate root.
            int root = find(parent.get(x));

            parent.put(x, root);

            return root;
        }

        /**
         * Merges the components containing a and b.
         */
        void union(int a, int b) {

            // find() also initializes previously unseen nodes.
            int rootA = find(a);
            int rootB = find(b);

            // If the roots are equal, the nodes are already connected.
            if (rootA == rootB) {
                return;
            }

            // UNION BY SIZE:
            // Keep the larger component's root as the new root.
            int sizeA = size.get(rootA);
            int sizeB = size.get(rootB);

            if (sizeA < sizeB) {

                int tempRoot = rootA;
                rootA = rootB;
                rootB = tempRoot;

                int tempSize = sizeA;
                sizeA = sizeB;
                sizeB = tempSize;
            }

            // Attach the smaller component's root to the larger root.
            parent.put(rootB, rootA);

            // The merged component now contains both sizes.
            size.put(rootA, sizeA + sizeB);

            // Two components became one.
            components--;
        }

        /**
         * Returns the number of connected coordinate components.
         */
        int getComponentCount() {
            return components;
        }
    }


    // =================================================================
    // DEMONSTRATION
    // =================================================================

    public static void main(String[] args) {

        MostStonesRemoved solution = new MostStonesRemoved();

        int[][] stones = {
            {0, 0},  // Stone 0
            {0, 1},  // Stone 1: shares row 0 with Stone 0
            {1, 0},  // Stone 2: shares column 0 with Stone 0
            {3, 3}   // Stone 3: isolated from the first three stones
        };

        int result1 = solution.removeStonesStoneBased(stones);
        int result2 = solution.removeStonesCoordinateBased(stones);

        System.out.println("Stone-based DSU answer: " + result1);
        System.out.println("Coordinate-based DSU answer: " + result2);

        // Expected output:
        //
        // Stone-based DSU answer: 2
        // Coordinate-based DSU answer: 2
    }
}





import java.util.HashSet;
import java.util.Set;

/**
 * Problem: Most Stones Removed with Same Row or Column
 *
 * INTUITION
 * ---------
 * We can remove a stone if another remaining stone shares its row
 * or column.
 *
 * Instead of simulating removals, we identify connected groups.
 *
 * Two stones belong to the same group if they are connected:
 *   - Directly, by sharing a row or column.
 *   - Indirectly, through a chain of other stones.
 *
 * IMPORTANT OBSERVATION
 * ---------------------
 * If a connected group contains k stones, we can remove k - 1 stones.
 * One stone must remain in each group.
 *
 * Therefore:
 *
 *   Maximum removals = total stones - number of connected groups
 *
 * APPROACH: COORDINATE-BASED UNION-FIND
 * -------------------------------------
 * Instead of making each stone a DSU node, we make rows and columns
 * the nodes.
 *
 * A stone (row, column) connects its row node to its column node.
 *
 * Example:
 *
 *   Stone (0, 0) -> union(0, -1)
 *   Stone (0, 1) -> union(0, -2)
 *   Stone (1, 0) -> union(1, -1)
 *
 * We encode the column using -column - 1 so that:
 *
 *   Row coordinates    = 0, 1, 2, ...
 *   Column coordinates = -1, -2, -3, ...
 *
 * Since the given coordinates are nonnegative, row and column
 * identifiers never collide.
 */
class RemoveStones {

    /**
     * Returns the maximum number of stones that can be removed.
     *
     * @param stones stones[i][0] is the row coordinate
     *               stones[i][1] is the column coordinate
     */
    public static int removeStones(int[][] stones) {

        // Create an initially empty Union-Find structure.
        //
        // Unlike stone-based DSU, we do not know beforehand how many
        // distinct row and column nodes will be used.
        // The HashMap-based DSU creates nodes when they are encountered.
        UnionFind uf = new UnionFind();

        // STEP 1: Build connectivity between rows and columns.
        for (int[] stone : stones) {

            int row = stone[0];
            int col = stone[1];

            // Encode the column as a negative identifier.
            //
            // Examples:
            //   column 0 -> -1
            //   column 1 -> -2
            //   column 2 -> -3
            //
            // This prevents a row node from being confused with a
            // column node having the same original coordinate.
            int encodedColumn = -col - 1;

            // A stone represents a connection between its row and column.
            //
            // union() registers both nodes if necessary and merges
            // their components when they are not already connected.
            uf.union(row, encodedColumn);
        }

        // STEP 2: Count the connected components containing stones.
        //
        // The DSU contains row nodes and column nodes, not stone nodes.
        // Therefore, we must count components using the actual stones
        // instead of counting every root in the DSU.
        //
        // Every stone's row belongs to the component containing that
        // stone. All stones in the same connected group have the same
        // root for their row nodes.
        Set<Integer> components = new HashSet<>();

        for (int[] stone : stones) {

            int row = stone[0];

            // find(row) returns the representative of the component.
            //
            // HashSet automatically avoids counting the same component
            // multiple times.
            components.add(uf.find(row));
        }

        // STEP 3: Calculate the maximum removals.
        //
        // Each connected component must retain one stone.
        // All remaining stones can be removed.
        //
        // Example:
        //   Total stones = 4
        //   Components   = 2
        //   Answer       = 4 - 2 = 2
        return stones.length - components.size();
    }
}


import java.util.HashMap;
import java.util.Map;

/**
 * HashMap-based Disjoint Set Union (DSU).
 *
 * PURPOSE
 * -------
 * Maintains groups of connected coordinate nodes.
 *
 * Each node represents either:
 *   - A row coordinate, which is nonnegative.
 *   - An encoded column coordinate, which is negative.
 *
 * CORE OPERATIONS
 * ---------------
 * find(x):
 *   Returns the root representing the group containing x.
 *
 * union(x, y):
 *   Merges the groups containing x and y, if they are different.
 *
 * OPTIMIZATIONS
 * -------------
 * 1. Path compression:
 *    Makes future find() calls faster by pointing visited nodes
 *    directly to their root.
 *
 * 2. Union by rank:
 *    Attaches the shorter tree under the taller tree, keeping
 *    the DSU trees shallow.
 */
class UnionFind {

    // parents.get(x) tells us the parent of node x.
    //
    // If parents.get(x) == x, then x is a root.
    private final Map<Integer, Integer> parents = new HashMap<>();

    // ranks.get(root) estimates the height of the tree rooted at root.
    //
    // Rank is meaningful for roots. When two roots are merged:
    //   - Attach the shorter tree under the taller tree.
    //   - If their ranks are equal, choose one root and increase its rank.
    private final Map<Integer, Integer> ranks = new HashMap<>();

    /**
     * Finds the root of the component containing coordinate.
     *
     * @param coordinate row or encoded column identifier
     * @return root representing its connected component
     */
    public int find(int coordinate) {

        // LAZY INITIALIZATION
        //
        // If this coordinate has never been encountered, register it
        // as a new, independent component.
        //
        // This also makes find() safe to call on an unseen coordinate.
        if (!parents.containsKey(coordinate)) {
            parents.put(coordinate, coordinate);
            ranks.put(coordinate, 0);
        }

        // BASE CASE
        //
        // A node whose parent is itself is the root of its component.
        if (parents.get(coordinate) == coordinate) {
            return coordinate;
        }

        // RECURSIVE FIND + PATH COMPRESSION
        //
        // Follow parent pointers until we reach the root.
        // Then make the current node point directly to that root.
        //
        // Example:
        //
        //   2 -> 1 -> 0
        //
        // After find(2):
        //
        //   2 ------> 0
        //   1 ------> 0
        //
        // Future lookups along this path become faster.
        int root = find(parents.get(coordinate));

        parents.put(coordinate, root);

        return root;
    }

    /**
     * Merges the components containing x and y.
     *
     * If both nodes are already in the same component, no merge
     * is needed.
     */
    public void union(int x, int y) {

        // Always find the roots first.
        //
        // Do not assume x and y themselves are roots. They might be
        // somewhere inside their respective DSU trees.
        int rootX = find(x);
        int rootY = find(y);

        // If both roots are identical, the nodes are already connected.
        //
        // This can happen when:
        //   - The same relationship is encountered again.
        //   - They became connected through another row or column.
        if (rootX == rootY) {
            return;
        }

        // IMPORTANT:
        // Compare the ranks of the ROOTS, not the original x and y.
        //
        // Rank belongs to the root's tree. An arbitrary node's rank
        // does not tell us the height of its entire component.
        int rankX = ranks.get(rootX);
        int rankY = ranks.get(rootY);

        // CASE 1: Tree X is taller.
        //
        // Attach rootY under rootX.
        // The height of the resulting tree does not increase.
        if (rankX > rankY) {

            parents.put(rootY, rootX);

        // CASE 2: Tree Y is taller.
        //
        // Attach rootX under rootY.
        // Again, the height does not increase.
        } else if (rankY > rankX) {

            parents.put(rootX, rootY);

        // CASE 3: Both trees have equal rank.
        //
        // Choose rootY as the new root.
        // Since the two trees have equal height, attaching one under
        // the other increases the resulting tree's height by one.
        } else {

            parents.put(rootX, rootY);

            // Only the surviving root's rank increases.
            ranks.put(rootY, rankY + 1);
        }
    }

    /**
     * Optional helper useful for debugging.
     *
     * Returns the current parent mapping.
     * The map's shape may change after find() because of path compression.
     */
    public Map<Integer, Integer> getParents() {
        return parents;
    }
}

