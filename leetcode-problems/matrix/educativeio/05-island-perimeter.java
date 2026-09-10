/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given a 2D grid representing a map where 1 denotes land and 0 denotes water.
 * The land cells connect horizontally and vertically to form exactly one island.
 * Our goal is to calculate the perimeter of this island. 
 * The perimeter is the total length of the boundary separating the land from the water 
 * (or the edge of the grid).
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Is it guaranteed that the grid will always have at least one land cell?
 *    (Yes, constraints say exactly one island).
 * 2. Can the island touch the boundaries of the grid?
 *    (Yes, and the grid boundaries act as water, contributing to the perimeter).
 * 3. Does the grid contain any "lakes" (water completely surrounded by land)?
 *    (The problem specifies there are no lakes, meaning no internal enclosed water).
 * 4. Are we allowed to modify the input grid?
 *    (For standard iterative approaches, we don't need to. For DFS, we might mutate 
 *    it to mark visited cells, or use an extra visited array. Always good to ask).
 * 5. Can the grid be a 1D array (e.g., 1 row or 1 column)?
 *    (Yes, dimensions can be 1xN or Nx1).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Single Cell Island: [[1]] -> Perimeter should be 4.
 * 2. Completely Land Grid: [[1, 1], [1, 1]] -> Perimeter is 8 (all outer edges).
 * 3. Linear Island: [[1, 1, 1, 1]] -> Perimeter is 4 + 4 + 2 = 10.
 * 4. Island with corners/bends: 
 *    [[0, 1, 0],
 *     [1, 1, 1],
 *     [0, 1, 0]] -> A plus shape. Perimeter is 12.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Boundary Checking (The Intuitive Approach):
 *    - Iterate through every cell in the grid.
 *    - When we find a land cell (1), it inherently has 4 sides.
 *    - We look at its 4 immediate neighbors (up, down, left, right).
 *    - If a neighbor is water (0) or out of bounds, that side contributes 1 to the perimeter.
 *    - Time: O(R * C), Space: O(1).
 * 
 * 2. Math / Edge Counting (The Optimal "Clever" Approach):
 *    - Iterate through the grid.
 *    - Count the total number of land cells (let's call it `islands`).
 *    - Count the number of shared internal edges. To avoid double counting, we only check 
 *      RIGHT and DOWN for each land cell.
 *    - If the cell below is also land, that's one shared edge.
 *    - If the cell to the right is also land, that's another shared edge.
 *    - Since two adjacent land cells "hide" 2 sides of the perimeter (one from each cell), 
 *      the formula is: Total Perimeter = (islands * 4) - (shared_edges * 2).
 *    - Time: O(R * C), Space: O(1).
 * 
 * 3. DFS / BFS Graph Traversal (The Overkill / Universal Graph Approach):
 *    - Find the very first land cell.
 *    - Launch a Depth-First Search (DFS) from it.
 *    - When traversing, if we step out of bounds or into water, it means we hit a boundary, 
 *      so return 1 (contributing to perimeter).
 *    - If we step into a visited land cell, return 0.
 *    - Time: O(R * C), Space: O(R * C) for the call stack. 
 *    - Note: This is less efficient for this specific problem but highly reusable if the 
 *      problem gets modified (e.g., "Find the max perimeter among MULTIPLE islands").
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Math / Edge Counting Approach)
 * ============================================================================
 * Grid:
 * [0, 1, 0]
 * [1, 1, 1]
 * 
 * Loop i, j:
 * (0, 1) is 1. islands = 1.
 *   - check down: (1, 1) is 1. shared_edges = 1.
 *   - check right: (0, 2) is 0.
 * 
 * (1, 0) is 1. islands = 2.
 *   - check down: out of bounds.
 *   - check right: (1, 1) is 1. shared_edges = 2.
 * 
 * (1, 1) is 1. islands = 3.
 *   - check down: out of bounds.
 *   - check right: (1, 2) is 1. shared_edges = 3.
 * 
 * (1, 2) is 1. islands = 4.
 *   - check down: out of bounds.
 *   - check right: out of bounds.
 * 
 * Result: islands = 4, shared_edges = 3.
 * Perimeter = (4 * 4) - (3 * 2) = 16 - 6 = 10.
 */

public class IslandPerimeter {

    public static void main(String[] args) {
        int[][] grid1 = {
            {0, 1, 0, 0},
            {1, 1, 1, 0},
            {0, 1, 0, 0},
            {1, 1, 0, 0}
        };
        
        int[][] grid2 = {
            {1}
        };

        System.out.println("--- Test Case 1 ---");
        System.out.println("Solution 1 (Boundary Check): " + islandPerimeterBoundary(grid1));
        System.out.println("Solution 2 (Math Counting):  " + islandPerimeterMath(grid1));
        System.out.println("Solution 3 (DFS Traversal):  " + islandPerimeterDFS(grid1));
        System.out.println();

        System.out.println("--- Test Case 2 ---");
        System.out.println("Solution 1 (Boundary Check): " + islandPerimeterBoundary(grid2));
        System.out.println("Solution 2 (Math Counting):  " + islandPerimeterMath(grid2));
        System.out.println("Solution 3 (DFS Traversal):  " + islandPerimeterDFS(grid2));
    }

    /**
     * SOLUTION 1: Boundary Checking
     * 
     * Idea: Look at every cell. If it's land, check its 4 neighbors. 
     * If a neighbor is water or off the grid, it contributes to the perimeter.
     * 
     * Time Complexity: O(R * C) where R is rows and C is columns.
     * Space Complexity: O(1).
     */
    public static int islandPerimeterBoundary(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int rows = grid.length;
        int cols = grid[0].length;
        int perimeter = 0;
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                
                // Only process land cells
                if (grid[r][c] == 1) {
                    
                    // Check UP
                    if (r == 0 || grid[r - 1][c] == 0) perimeter++;
                    
                    // Check DOWN
                    if (r == rows - 1 || grid[r + 1][c] == 0) perimeter++;
                    
                    // Check LEFT
                    if (c == 0 || grid[r][c - 1] == 0) perimeter++;
                    
                    // Check RIGHT
                    if (c == cols - 1 || grid[r][c + 1] == 0) perimeter++;
                }
            }
        }
        
        return perimeter;
    }

    /**
     * SOLUTION 2: Math / Edge Counting (Most Optimal/Elegant)
     * 
     * Idea: Every land cell brings 4 sides. Every shared edge between two land 
     * cells hides 2 sides. We only check Right and Down to avoid double counting.
     * 
     * Time Complexity: O(R * C).
     * Space Complexity: O(1).
     */
    public static int islandPerimeterMath(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int rows = grid.length;
        int cols = grid[0].length;
        
        int landCells = 0;
        int sharedEdges = 0;
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                
                if (grid[r][c] == 1) {
                    landCells++; // Found a piece of land
                    
                    // Check if the cell directly BELOW is also land
                    if (r < rows - 1 && grid[r + 1][c] == 1) {
                        sharedEdges++;
                    }
                    
                    // Check if the cell directly to the RIGHT is also land
                    if (c < cols - 1 && grid[r][c + 1] == 1) {
                        sharedEdges++;
                    }
                }
            }
        }
        
        // Final math formula
        return (landCells * 4) - (sharedEdges * 2);
    }

    /**
     * SOLUTION 3: DFS Traversal
     * 
     * Idea: Treat the grid as a graph. Find the first land cell and traverse. 
     * Hitting a boundary or water counts as a perimeter edge.
     * Note: We mutate the grid to mark visited cells as '-1' to save space, 
     * but we should reset it if the input needs to be preserved.
     * 
     * Time Complexity: O(R * C).
     * Space Complexity: O(R * C) worst case for the recursion stack (if the grid is a snake).
     */
    public static int islandPerimeterDFS(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int rows = grid.length;
        int cols = grid[0].length;
        
        // Find the first land cell and trigger DFS
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 1) {
                    // Since there's only one island, the first DFS will find the whole perimeter
                    return dfs(grid, r, c);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Helper method for DFS traversal.
     */
    private static int dfs(int[][] grid, int r, int c) {
        // Base Case 1: Out of bounds. This is a perimeter edge!
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length) {
            return 1;
        }
        
        // Base Case 2: Water. This is a perimeter edge!
        if (grid[r][c] == 0) {
            return 1;
        }
        
        // Base Case 3: Already visited land cell. Does NOT contribute to perimeter.
        if (grid[r][c] == -1) {
            return 0;
        }
        
        // Mark current cell as visited
        grid[r][c] = -1;
        
        // Explore all 4 directions and accumulate the perimeter
        int totalPerimeter = 0;
        totalPerimeter += dfs(grid, r + 1, c); // DOWN
        totalPerimeter += dfs(grid, r - 1, c); // UP
        totalPerimeter += dfs(grid, r, c + 1); // RIGHT
        totalPerimeter += dfs(grid, r, c - 1); // LEFT
        
        return totalPerimeter;
    }
    
    /**
     * ============================================================================
     * FOLLOW-UPS
     * ============================================================================
     * 1. What if there are multiple islands and you need to return the max perimeter?
     *    Answer: The Math/Counting approach fails here because it calculates the sum 
     *    of all perimeters globally. You would HAVE to use the DFS/BFS approach to 
     *    isolate each island and calculate its perimeter individually, keeping a 
     *    running maximum.
     * 
     * 2. What if the grid contains internal lakes (0s surrounded by 1s) and the 
     *    lake boundaries don't count towards the perimeter?
     *    Answer: You would need to distinguish between "ocean water" and "lake water".
     *    You could run a DFS starting from the boundaries of the grid (where r=0, r=R-1, 
     *    c=0, c=C-1) and mark all reachable 0s as "Ocean". Then, when calculating the 
     *    perimeter of the 1s, only add to the perimeter if the neighbor is an "Ocean" cell 
     *    or out of bounds.
     */
}
