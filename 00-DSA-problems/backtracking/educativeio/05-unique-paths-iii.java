/**
 * ============================================================================
 * UNIQUE PATHS III - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * Imagine you are placed in a maze. You have a designated starting point (1) 
 * and an ending point (2). Some areas are blocked by walls (-1), and the rest 
 * are empty rooms (0). Your objective is to walk from the start to the end 
 * with a very strict rule: you MUST visit every single empty room exactly once. 
 * If you reach the end but skipped a room, that path doesn't count. We need to 
 * find the total number of valid paths that satisfy this condition.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can the grid be entirely filled with obstacles except for start and end?
 * A: Yes, and if they are adjacent, the answer is 1. If not, 0.
 * 
 * Q: Am I allowed to modify the grid during my traversal?
 * A: This is an important question. Modifying the grid in-place (changing 0 to -1 
 *    temporarily) saves space. If the interviewer says no, we can use a Bitmask 
 *    or a visited boolean array.
 * 
 * Q: How large can the grid get?
 * A: The constraints specify m * n <= 20. This is the biggest hint! 20 is a very 
 *    small number, meaning an O(3^N) or O(4^N) exponential backtracking solution 
 *    will easily run within time limits. 
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - GRAPH TRAVERSAL: This is a classic Depth-First Search (DFS) problem. 
 * - HAMILTONIAN PATH: We are essentially looking for a Hamiltonian Path (a path 
 *   that visits every vertex exactly once) on a grid graph, which is an NP-hard 
 *   problem. That's why the grid size is restricted to 20!
 * - COUNTING FIRST: How do we know if a path is valid when we reach the destination?
 *   We must know beforehand exactly how many empty squares exist. We can do an 
 *   initial scan of the grid to count the '0's and find the starting coordinates.
 * - BACKTRACKING: When we visit an empty square, we mark it as visited (like an 
 *   obstacle '-1'), decrease our count of squares to visit, and explore neighbors. 
 *   When we retreat (backtrack), we unmark the square and restore the count.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Immediately point out the constraint `m * n <= 20`. Tell the interviewer
 *   that this small constraint strongly suggests Backtracking/DFS.
 * - Step 2: Explain the two-pass approach. Pass 1: find the start and count the 
 *   walkable squares. Pass 2: recursively explore all paths (DFS).
 * - Step 3: Implement the In-Place DFS first. It's the most space-efficient.
 * - Step 4: If asked for a solution that doesn't modify the input array, propose
 *   the Bitmask DFS solution. Since max cells = 20, an integer (32 bits) can easily
 *   represent the visited state of the entire board!
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Grid: 
 * [ 1,  0,  0,  0]
 * [ 0,  0,  0,  0]
 * [ 0,  0,  2, -1]
 * 
 * Total walkable squares (0s) = 9
 * We start at (0,0). Every time we step on a '0', we change it to '-1' and 
 * remaining_walkable = remaining_walkable - 1.
 * If we step on '2' and remaining_walkable == -1 (because we decrement when 
 * stepping off the last 0), we found a valid path!
 */

import java.util.*;

public class UniquePathsIII {

    /**
     * SOLUTION 1: DFS Backtracking with In-Place Grid Modification
     * ------------------------------------------------------------------------
     * Pros: Most optimal space complexity. Modifies the grid temporarily to 
     * act as a 'visited' set, completely avoiding extra allocations.
     * Cons: Mutates input parameters (which some strict guidelines discourage).
     * 
     * Time Complexity: O(4 * 3^(N)) where N is the number of empty squares. 
     * We have 4 choices initially, and roughly 3 choices thereafter.
     * Space Complexity: O(N) for the recursion call stack depth.
     */
    public int uniquePathsIII_DFS(int[][] grid) {
        int emptySquares = 0;
        int startRow = 0;
        int startCol = 0;

        // Pass 1: Find the starting point and count the total empty squares
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == 1) {
                    startRow = r;
                    startCol = c;
                } else if (grid[r][c] == 0) {
                    emptySquares++;
                }
            }
        }

        // Pass 2: Start DFS from the starting point
        // We add 1 to emptySquares because we need to count reaching the target '2' as a valid step
        return dfs(grid, startRow, startCol, emptySquares + 1);
    }

    private int dfs(int[][] grid, int r, int c, int remainingSquares) {
        // Boundary checks and obstacle/visited checks
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] == -1) {
            return 0;
        }

        // Base case: Reached the ending point
        if (grid[r][c] == 2) {
            // If we have visited exactly all required squares, this path is valid (1)
            return remainingSquares == 0 ? 1 : 0;
        }

        // Store the original state of the cell to backtrack later
        int temp = grid[r][c];

        // Mark the current cell as visited (act like an obstacle)
        grid[r][c] = -1;

        // Explore all 4 cardinal directions
        int paths = 0;
        paths += dfs(grid, r + 1, c, remainingSquares - 1); // Down
        paths += dfs(grid, r - 1, c, remainingSquares - 1); // Up
        paths += dfs(grid, r, c + 1, remainingSquares - 1); // Right
        paths += dfs(grid, r, c - 1, remainingSquares - 1); // Left

        // Backtrack: Restore the cell to its original state so other paths can use it
        grid[r][c] = temp;

        return paths;
    }

    /**
     * SOLUTION 2: DFS Backtracking using Bitmask (Immutable Input)
     * ------------------------------------------------------------------------
     * Pros: Does NOT modify the original grid. It uses a 32-bit integer to 
     * track the visited state of up to 20 cells (perfect for this constraint).
     * Feature Highlight: Shows deep understanding of bit manipulation and 
     * non-destructive algorithms.
     * 
     * Time Complexity: O(3^N)
     * Space Complexity: O(N) for recursion stack.
     */
    public int uniquePathsIII_Bitmask(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        
        int startRow = 0, startCol = 0;
        int targetMask = 0; // Will represent all walk-able squares as '1's

        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 1) {
                    startRow = r;
                    startCol = c;
                }
                // We consider '0' and '2' as squares we must eventually clear
                if (grid[r][c] == 0 || grid[r][c] == 2) {
                    int bitPosition = r * n + c;
                    targetMask |= (1 << bitPosition); // Set the bit to 1
                }
            }
        }

        return dfsBitmask(grid, startRow, startCol, targetMask, 0, m, n);
    }

    private int dfsBitmask(int[][] grid, int r, int c, int targetMask, int currentMask, int m, int n) {
        // Bounds check and obstacle check
        if (r < 0 || r >= m || c < 0 || c >= n || grid[r][c] == -1) {
            return 0;
        }

        int bitPosition = r * n + c;

        // If this square is already visited in our current path, return 0
        // (Note: grid[start][start] is '1', which is not in targetMask, so it's safely ignored)
        if ((currentMask & (1 << bitPosition)) != 0) {
            return 0;
        }

        // If we reach the end, check if our currentMask matches the targetMask
        if (grid[r][c] == 2) {
            currentMask |= (1 << bitPosition); // Add the ending point to mask
            return currentMask == targetMask ? 1 : 0;
        }

        // Mark current cell as visited in our mask
        if (grid[r][c] == 0) {
            currentMask |= (1 << bitPosition);
        }

        // Recurse
        int paths = 0;
        paths += dfsBitmask(grid, r + 1, c, targetMask, currentMask, m, n);
        paths += dfsBitmask(grid, r - 1, c, targetMask, currentMask, m, n);
        paths += dfsBitmask(grid, r, c + 1, targetMask, currentMask, m, n);
        paths += dfsBitmask(grid, r, c - 1, targetMask, currentMask, m, n);

        // No need to backtrack the mask explicitly because integers are passed by value in Java!
        // The recursive calls have their own copies of 'currentMask'.

        return paths;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        UniquePathsIII solver = new UniquePathsIII();

        // Test Case 1
        int[][] grid1 = {
            {1, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 2, -1}
        };
        
        System.out.println("--- Test Case 1 ---");
        System.out.println("DFS In-Place (Expected 2): " + solver.uniquePathsIII_DFS(grid1));
        
        // Resetting grid1 since DFS modifies it (though our backtrack perfectly restores it)
        int[][] grid1_copy = {
            {1, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 2, -1}
        };
        System.out.println("DFS Bitmask  (Expected 2): " + solver.uniquePathsIII_Bitmask(grid1_copy));


        // Test Case 2 (No valid paths because it's impossible to visit every square)
        int[][] grid2 = {
            {1, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 2}
        };
        System.out.println("\n--- Test Case 2 ---");
        System.out.println("DFS In-Place (Expected 4): " + solver.uniquePathsIII_DFS(grid2));

        // Test Case 3 (Obstacle blocks the only path)
        int[][] grid3 = {
            {0, 1},
            {2, 0}
        };
        System.out.println("\n--- Test Case 3 ---");
        System.out.println("DFS Bitmask  (Expected 0): " + solver.uniquePathsIII_Bitmask(grid3));
    }
}

public class Solution {

    // 4-directional movement (Down, Up, Right, Left)
    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    public int uniquePathsIII(int[][] grid) {

        int rows = grid.length;
        int cols = grid[0].length;

        boolean[][] visited = new boolean[rows][cols];

        int startRow = 0, startCol = 0;
        int totalWalkableCells = 0;

        /*
         * STEP 1: Preprocessing
         * -----------------------------------
         * - Count all non-obstacle cells (0, 1, 2)
         * - Find starting position
         */
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (grid[r][c] != -1) {
                    totalWalkableCells++; // cells we must visit exactly once
                }

                if (grid[r][c] == 1) {
                    startRow = r;
                    startCol = c;
                }
            }
        }

        /*
         * STEP 2: Start DFS
         *
         * remaining = how many cells still need to be visited
         */
        return dfs(grid, visited, startRow, startCol, totalWalkableCells);
    }

    private int dfs(int[][] grid, boolean[][] visited, int r, int c, int remaining) {

        /*
         * BASE CASE 1: Invalid move
         * -----------------------------------
         * - Out of bounds
         * - Obstacle (-1)
         * - Already visited
         */
        if (r < 0 || c < 0 || r >= grid.length || c >= grid[0].length
                || grid[r][c] == -1 || visited[r][c]) {
            return 0;
        }

        /*
         * BASE CASE 2: Reached destination (cell == 2)
         *
         * IMPORTANT:
         * We can only accept this path if ALL cells are visited.
         *
         * Why remaining == 1?
         * - Current cell (2) is the last remaining cell
         */
        if (grid[r][c] == 2) {
            return (remaining == 1) ? 1 : 0;
        }

        /*
         * CHOICE:
         * Mark current cell as visited
         */
        visited[r][c] = true;

        int totalPaths = 0;

        /*
         * EXPLORE:
         * Try all 4 directions
         *
         * remaining - 1 because we are consuming this cell
         */
        for (int[] dir : DIRS) {
            int newRow = r + dir[0];
            int newCol = c + dir[1];

            totalPaths += dfs(grid, visited, newRow, newCol, remaining - 1);
        }

        /*
         * BACKTRACK:
         * Undo the choice so other paths can reuse this cell
         */
        visited[r][c] = false;

        return totalPaths;
    }
}
