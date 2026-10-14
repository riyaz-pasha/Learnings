/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * An n x n grid is composed of 1 x 1 squares where each square consists of a 
 * '/', '\', or a blank space ' '. These characters divide the square into 
 * adjacent regions. Given the grid represented as a string array, return the 
 * number of adjacent regions.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can the grid contain invalid characters? 
 *    (Confirms we only need to handle '/', '\', and ' ').
 * 2. Does a completely empty grid of size N x N count as 1 region?
 *    (Yes, all empty space connects into a single region, preventing off-by-one logic errors).
 * 3. Are the backslashes strictly escaped as "\\" in the input strings?
 *    (Yes, so string.charAt(j) == '\\' is the correct check [1]).
 * 4. Can we assume the grid is always perfectly square (N x N)?
 *    (Yes, constraints say grid.length == grid[i].length).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: Slashes are "infinitely thin" boundaries. You cannot standardly 
 * Flood-Fill (DFS/BFS) a normal 2D matrix because a single cell can contain TWO separate 
 * regions (e.g., a '/' separates the top-left from the bottom-right). We need a way to 
 * represent the fractional space inside a single cell.
 * 
 * --- Approach 1: Grid Upscaling (3x3 Zoom) + DFS ---
 * Idea: If slashes are too thin, make them thick! We can convert every 1x1 cell into 
 * a 3x3 grid of pixels. 
 * - A ' ' becomes a 3x3 block of 0s.
 * - A '/' becomes a diagonal of 1s from top-right to bottom-left.
 * - A '\' becomes a diagonal of 1s from top-left to bottom-right.
 * Why 3x3 and not 2x2? Because in a 2x2 grid, two parallel slashes ("/" next to "/") 
 * would touch corners and block the space between them. 3x3 leaves a guaranteed 1-pixel 
 * path for spaces to connect. After building this 3N x 3N grid, we just count the 0-components using DFS.
 * Why it works: It reduces the problem to standard "Number of Islands".
 * Why it's costly: We are allocating a matrix 9 times larger than the original. 
 * 
 * Time Complexity: O(N^2) — because we process 9 * N^2 cells, which simplifies to O(N^2) [1].
 * Space Complexity: O(N^2) — because the 3N x 3N grid and recursion stack take O(N^2) memory.
 * 
 * --- Approach 2: Union-Find (Disjoint Set) on Cell Triangles ---
 * Idea: Instead of pixels, let's look at the geometry. Any square can be divided by 
 * its two diagonals into 4 triangular regions: Top (0), Right (1), Bottom (2), Left (3) [2].
 * What I'd naturally try: For every cell, look at the character. 
 * - If ' ', all 4 triangles are the same region -> Union(0,1,2,3).
 * - If '/', the slash blocks the middle, leaving Top-Left and Bottom-Right -> Union(0,3) and Union(1,2) [2].
 * - If '\', Top-Right and Bottom-Left merge -> Union(0,1) and Union(2,3) [2].
 * Property/Observation: Adjacent cells ALWAYS connect. The Bottom triangle of cell (r, c) 
 * ALWAYS connects to the Top triangle of cell (r+1, c). The Right triangle of cell (r, c) 
 * ALWAYS connects to the Left triangle of cell (r, c+1) [2].
 * 
 * Time Complexity: O(N^2 * α(N^2)) — because we process N^2 cells, doing a constant number of Union operations per cell. α is the inverse Ackermann function (effectively O(1)) [2, 4]. 
 * Space Complexity: O(N^2) — because the Disjoint Set parent array needs size 4 * N^2. No deep recursion stacks or 2D arrays! [4]
 * 
 * Interview Decision: I would write **Union-Find**. It is incredibly elegant, doesn't require 
 * expanding the grid, avoids StackOverflow risks from deep DFS traversals, and perfectly 
 * demonstrates mathematical modeling of a graph.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. N = 1, grid = [" "]: Minimum size, empty. Should return 1.
 * 2. All slashes forming a maze: grid = ["/\", "\/"]. Returns multiple isolated components.
 * 3. Escaped backslashes: Understanding that string length N might look different in source 
 *    code (e.g., "\\/" is length 2, not 3) [1].
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "How can I represent multiple states inside a single cell?"
 * Correctness intuition: By pre-slicing EVERY cell into 4 quadrants (an X shape), 
 * we account for every possible split. The character in the cell just decides which 
 * of those internal quadrants we "glue" back together.
 * 
 * --- Examples & ASCII ---
 * Cell split into 4 triangles:
 *  \  0  /
 *   \   /
 * 3  \ /  1
 *    / \
 *   /   \
 *  /  2  \
 * 
 * If the char is '/' (bottom-left to top-right):
 * Triangles 0 and 3 are glued together.
 * Triangles 1 and 2 are glued together.
 * 
 * --- Dry Run (Union-Find) ---
 * Grid: [" /", "/ "] (N=2)
 * Initial components = 2 * 2 * 4 = 16. [2]
 * 
 * Cell(0,0) = ' '.
 *   - Internal: Union(0-1, 1-2, 2-3). Components drops by 3 -> 13.
 *   - External: Connect Right(1) to Cell(0,1) Left(3). Drops by 1 -> 12.
 *   - External: Connect Bottom(2) to Cell(1,0) Top(0). Drops by 1 -> 11. [2]
 * 
 * Cell(0,1) = '/'.
 *   - Internal: Union(0-3) and Union(1-2). Drops by 2 -> 9. [2]
 *   - External: Connect Bottom(2) to Cell(1,1) Top(0). Drops by 1 -> 8.
 * 
 * Cell(1,0) = '/'.
 *   - Internal: Union(0-3) and Union(1-2). Drops by 2 -> 6. [2]
 *   - External: Connect Right(1) to Cell(1,1) Left(3). Drops by 1 -> 5.
 * 
 * Cell(1,1) = ' '.
 *   - Internal: Union(0-1, 1-2, 2-3). Drops by 3 -> 2. [2]
 * 
 * Final output: 2 regions!
 * 
 * --- Pitfalls ---
 * 1. Forgetting that Java Strings escape backslashes. If you check `c == '\'`, it won't compile. You must use `c == '\\'` [1].
 * 2. Messing up the cell indexing math. The formula for the base index of a cell's triangles is `(r * N + c) * 4` [2].
 * 
 * --- Pattern Recognition ---
 * When you see: "Regions divided by lines", "Counting enclosed spaces".
 * Think: Upscaling + Islands, OR Internal-Region Union-Find.
 * Transfers to: Number of Islands, Evaluate Division, Hexagonal Grid pathfinding.
 * 
 * --- Interview Script ---
 * "Since a single cell can contain multiple separate regions if cut by a slash, standard DFS won't work directly. 
 * I'll model this using a Union-Find data structure. I'll imagine every cell is divided into 4 triangles: Top, Right, Bottom, and Left. 
 * First, I'll initialize a disjoint set with 4 * N^2 elements representing these triangles. 
 * As I iterate through the grid, I'll union adjacent triangles between neighboring cells. 
 * Then, based on whether the cell is blank, a forward slash, or a backslash, I'll union the internal triangles appropriately. 
 * Every successful union reduces our total region count by 1. By the end, the remaining count is our answer. 
 * That gives us O(N^2) time and O(N^2) space." [4]
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if we needed to find the AREA of the largest region?
 * A: Union-Find struggles here because a triangle cut by a slash has a different 
 *    real area than a combined empty cell. I would pivot to the 3x3 Upscaling + DFS 
 *    approach, where each '0' pixel represents a uniform unit of area.
 * Q2: What if the grid was 100,000 x 100,000 and we couldn't store it all in memory?
 * A: We'd process it row by row. We only ever need the *current* row and the *previous* row 
 *    in memory to resolve the Union-Find external connections. We'd maintain a rolling UF state.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Disjoint Set / Union-Find on sub-cell geometry [2].
 * Key observation: Every cell has 4 border regions regardless of how it's sliced.
 * Memorize: The 0(Top), 1(Right), 2(Bottom), 3(Left) triangle mapping.
 * Most common trap: `c == '\'` syntax error.
 * Mental trigger: "Slashes dividing regions" -> 4-Triangle Union Find.
 */

public class RegionsCutBySlashes {

    public int regionsBySlashes(String[] grid) {
        int n = grid.length;
        
        // 4 triangles per cell, N*N cells
        UnionFind uf = new UnionFind(n * n * 4);
        
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                
                // Base index for the 4 triangles of the current cell (r, c)
                int base = 4 * (r * n + c);
                
                char ch = grid[r].charAt(c);
                
                // 1. Connect INTERNAL triangles based on the character [2]
                if (ch == '/') {
                    // '/' connects Top(0) to Left(3), and Right(1) to Bottom(2)
                    uf.union(base + 0, base + 3);
                    uf.union(base + 1, base + 2);
                } else if (ch == '\\') {
                    // '\' connects Top(0) to Right(1), and Bottom(2) to Left(3)
                    uf.union(base + 0, base + 1);
                    uf.union(base + 2, base + 3);
                } else {
                    // ' ' connects all 4 together
                    uf.union(base + 0, base + 1);
                    uf.union(base + 1, base + 2);
                    uf.union(base + 2, base + 3);
                }
                
                // 2. Connect EXTERNAL triangles to adjacent cells [2]
                // Connect Right(1) of current cell to Left(3) of right neighbor
                if (c + 1 < n) {
                    int rightNeighborBase = 4 * (r * n + (c + 1));
                    uf.union(base + 1, rightNeighborBase + 3);
                }
                
                // Connect Bottom(2) of current cell to Top(0) of bottom neighbor
                if (r + 1 < n) {
                    int bottomNeighborBase = 4 * ((r + 1) * n + c);
                    uf.union(base + 2, bottomNeighborBase + 0);
                }
            }
        }
        
        return uf.getCount();
    }

    /**
     * Standard Disjoint Set (Union-Find) class
     */
    private class UnionFind {
        private int[] parent;
        private int count; // Tracks the number of disjoint regions
        
        public UnionFind(int size) {
            parent = new int[size];
            count = size; // Initially, every triangle is its own region
            
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }
        
        // Find the root of element i with Path Compression [2]
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            return parent[i] = find(parent[i]); 
        }
        
        // Merges two sets. Returns true if they were successfully merged.
        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            
            // If they are not already in the same set, merge them
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
                count--; // Merging two components reduces total count by 1 [2]
            }
        }
        
        public int getCount() {
            return count;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        RegionsCutBySlashes solution = new RegionsCutBySlashes();

        // Test Case 1: Simple separated slashes
        // [ " /" ]
        // [ "/ " ]
        String[] grid1 = {" /", "/ "};
        System.out.println("Test 1 Result: " + solution.regionsBySlashes(grid1)); // Expected: 2 [1]

        // Test Case 2: One continuous region
        // [ " /" ]
        // [ "  " ]
        String[] grid2 = {" /", "  "};
        System.out.println("Test 2 Result: " + solution.regionsBySlashes(grid2)); // Expected: 1 [1]

        // Test Case 3: A diamond shape made of slashes (Escaped backslashes!)
        // [ "/\\" ]
        // [ "\\/" ]
        String[] grid3 = {"/\\", "\\/"};
        System.out.println("Test 3 Result: " + solution.regionsBySlashes(grid3)); // Expected: 5 [1]
        
        // Test Case 4: Completely blank grid
        // [ "  " ]
        // [ "  " ]
        String[] grid4 = {"  ", "  "};
        System.out.println("Test 4 Result: " + solution.regionsBySlashes(grid4)); // Expected: 1
    }
}

public class RegionCut {

    public static int regionsBySlashes(String[] grid) {

        int n = grid.length;

        // =========================================================
        // STEP 1: REPRESENT EACH CELL AS FOUR TRIANGLES
        // =========================================================
        //
        // A cell can contain '/', '\', or a blank space.
        //
        // Treating the entire cell as one node is incorrect because
        // a diagonal can split the cell into two disconnected areas.
        //
        // Instead, divide every cell into four triangular parts:
        //
        //                  TOP (0)
        //                     |
        //                     |
        //          LEFT (3) - + - RIGHT (1)
        //                     |
        //                     |
        //                BOTTOM (2)
        //
        // Each cell gets four unique IDs:
        //   top    = base
        //   right  = base + 1
        //   bottom = base + 2
        //   left   = base + 3
        //
        // There are n * n cells and 4 triangles per cell.
        // Therefore, the total number of DSU elements is 4 * n * n.
        //
        // Initially, every triangle is its own connected component.

        UnionFind uf = new UnionFind(n * n * 4);


        // =========================================================
        // STEP 2: PROCESS EVERY CELL
        // =========================================================
        //
        // For each cell:
        //   1. Connect the appropriate triangles INSIDE the cell.
        //   2. Connect its bottom triangle to the cell below.
        //   3. Connect its right triangle to the cell on the right.
        //
        // We process bottom and right neighbors because every shared
        // boundary will then be processed exactly once.

        for (int row = 0; row < n; row++) {

            String str = grid[row];

            for (int col = 0; col < n; col++) {

                char ch = str.charAt(col);

                // -------------------------------------------------
                // STEP 2A: CALCULATE THE FOUR TRIANGLE IDs
                // -------------------------------------------------
                //
                // First calculate the ID of the first triangle
                // belonging to the current cell.
                //
                // Flatten the 2D cell coordinates into a 1D index:
                //
                //     cellIndex = row * n + col
                //
                // Multiply by 4 because every cell has four triangles.
                //
                // Example: n = 2
                //
                // Cell (0, 0): base = 0  -> IDs 0, 1, 2, 3
                // Cell (0, 1): base = 4  -> IDs 4, 5, 6, 7
                // Cell (1, 0): base = 8  -> IDs 8, 9, 10, 11
                // Cell (1, 1): base = 12 -> IDs 12, 13, 14, 15

                int root = (row * n + col) * 4;

                int top = root;          // Triangle at the top
                int right = root + 1;    // Triangle on the right
                int bottom = root + 2;   // Triangle at the bottom
                int left = root + 3;     // Triangle on the left


                // -------------------------------------------------
                // STEP 2B: CONNECT TRIANGLES INSIDE THE CURRENT CELL
                // -------------------------------------------------
                //
                // union(a, b) means:
                // The two triangles belong to the same connected
                // region, provided they weren't already connected.
                //
                // The character determines which triangles can
                // connect without crossing a diagonal.


                if (ch == ' ') {

                    // CASE 1: BLANK CELL
                    //
                    // There is no diagonal, so all four triangles
                    // belong to one connected open area.
                    //
                    // Three successful unions are sufficient to
                    // connect four initially separate components.
                    //
                    // Before: T    R    B    L  -> 4 components
                    //
                    // After:  T -- R -- B -- L  -> 1 component

                    uf.union(top, right);
                    uf.union(right, bottom);
                    uf.union(bottom, left);

                } else if (ch == '/') {

                    // CASE 2: FORWARD SLASH
                    //
                    // The diagonal runs from bottom-left to top-right.
                    //
                    // The diagonal separates the cell into two sides:
                    //
                    //   Side 1: TOP connects to LEFT
                    //   Side 2: RIGHT connects to BOTTOM
                    //
                    // Do not connect the two sides through the slash.
                    //
                    // Before: T    R    B    L
                    //
                    // After:  T -- L       R -- B
                    //         2 components

                    uf.union(top, left);
                    uf.union(bottom, right);

                } else if (ch == '\\') {

                    // CASE 3: BACKSLASH
                    //
                    // The diagonal runs from top-left to bottom-right.
                    //
                    // This time the two sides are:
                    //
                    //   Side 1: TOP connects to RIGHT
                    //   Side 2: LEFT connects to BOTTOM
                    //
                    // These two pairs remain separate inside the cell.

                    uf.union(top, right);
                    uf.union(left, bottom);
                }


                // -------------------------------------------------
                // STEP 2C: CONNECT TO THE CELL BELOW
                // -------------------------------------------------
                //
                // The bottom edge of the current cell touches the
                // top edge of the cell immediately below it.
                //
                // Therefore:
                //
                //   current.bottom <-> below.top
                //
                // We must check row + 1 < n to ensure a cell below
                // actually exists.
                //
                // If this is the last row, there is no cell below.

                if (row + 1 < n) {

                    // Find the base ID of the cell below:
                    // coordinates = (row + 1, col)

                    int bottomCellRoot = ((row + 1) * n + col) * 4;

                    // The top triangle of that cell has offset 0.
                    int bottomCellTop = bottomCellRoot;

                    // Join the two open areas touching at this edge.
                    uf.union(bottom, bottomCellTop);
                }


                // -------------------------------------------------
                // STEP 2D: CONNECT TO THE CELL ON THE RIGHT
                // -------------------------------------------------
                //
                // The right edge of the current cell touches the
                // left edge of the cell immediately to its right.
                //
                // Therefore:
                //
                //   current.right <-> rightNeighbor.left
                //
                // We must check col + 1 < n to ensure the neighbor
                // exists. Otherwise, the current cell is at the
                // right boundary of the grid.

                if (col + 1 < n) {

                    // Find the base ID of the cell to the right:
                    // coordinates = (row, col + 1)

                    int rightCellRoot = (row * n + (col + 1)) * 4;

                    // The left triangle has offset 3.
                    int rightCellLeft = rightCellRoot + 3;

                    // Join the open areas touching at this edge.
                    uf.union(right, rightCellLeft);
                }
            }
        }


        // =========================================================
        // STEP 3: COUNT THE REMAINING CONNECTED COMPONENTS
        // =========================================================
        //
        // Initially, every triangle was an independent component.
        //
        // Every successful union merges two different components.
        // Repeated unions between triangles already in the same
        // component do not create any new connection or region.
        //
        // After processing all cells and all shared boundaries,
        // each remaining DSU component represents one region.
        //
        // A triangle is a component representative if find(i) == i.
        //
        // Count those representatives to obtain the answer.

        int regionsCount = 0;

        for (int i = 0; i < n * n * 4; i++) {

            // A root is the representative of its component.
            // Every component has exactly one root.
            if (uf.find(i) == i) {
                regionsCount++;
            }
        }

        return regionsCount;
    }


    // =============================================================
    // UNION-FIND / DISJOINT SET UNION (DSU)
    // =============================================================
    //
    // DSU maintains groups of connected triangles.
    //
    // find(x):
    //   Returns the representative (root) of x's component.
    //
    // union(a, b):
    //   Merges the components containing a and b, if they differ.
    //
    // Path compression makes future find operations faster.
    // Union by size keeps the trees shallow.

    static class UnionFind {

        private final int[] parent;

        // Despite the original variable name "rank", this array
        // actually stores component SIZE, not tree height/rank.
        private final int[] rank;


        // ---------------------------------------------------------
        // CONSTRUCTOR
        // ---------------------------------------------------------
        //
        // Initially, every triangle is an independent component.
        //
        // Example for n = 1:
        // There are four triangles, so:
        //
        // parent = [0, 1, 2, 3]
        // rank   = [1, 1, 1, 1]
        //
        // There are initially four separate components.

        public UnionFind(int n) {

            parent = new int[n];
            rank = new int[n];

            for (int i = 0; i < n; i++) {

                // Every element starts as its own root.
                parent[i] = i;

                // Every component initially contains one element.
                rank[i] = 1;
            }
        }


        // ---------------------------------------------------------
        // FIND WITH PATH COMPRESSION
        // ---------------------------------------------------------
        //
        // Find the root of the component containing v.
        //
        // If v is not itself the root, recursively find its root
        // and update parent[v] to point directly to that root.
        //
        // This shortens the path and speeds up future operations.

        public int find(int v) {

            if (parent[v] != v) {

                // Path compression:
                // Instead of retaining a long parent chain,
                // point v directly to the component representative.
                parent[v] = find(parent[v]);
            }

            return parent[v];
        }


        // ---------------------------------------------------------
        // UNION BY SIZE
        // ---------------------------------------------------------
        //
        // Merge the components containing v1 and v2.
        //
        // Important:
        // If both elements already have the same root, they are
        // already connected. In that case, do nothing.
        //
        // Otherwise, attach the smaller component under the larger
        // component to keep the DSU trees shallow.

        public void union(int v1, int v2) {

            // Find the representative of each component.
            int p1 = find(v1);
            int p2 = find(v2);

            // Already connected: no merge is necessary.
            if (p1 == p2) {
                return;
            }

            // Attach the smaller component under the larger one.
            if (rank[p1] > rank[p2]) {

                parent[p2] = p1;

                // p1 now contains the elements from both components.
                rank[p1] += rank[p2];

            } else {

                parent[p1] = p2;

                // p2 now contains the elements from both components.
                rank[p2] += rank[p1];
            }
        }
    }
}

