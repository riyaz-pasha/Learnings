/**
 * ============================================================================
 * FLOOD FILL ALGORITHM - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * Imagine opening Microsoft Paint and using the "Paint Bucket" tool. When you 
 * click on a specific pixel, the tool changes the color of that pixel and all 
 * connected pixels (up, down, left, right) that share the same original color. 
 * We are given a 2D array representing this image, starting coordinates (sr, sc), 
 * and a new target color. Our goal is to simulate this Paint Bucket tool.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can the grid be empty? 
 * A: The constraints say 1 <= grid.length <= 30, so it will always have at least one pixel.
 * 
 * Q: What if the target color is exactly the same as the original color?
 * A: This is the MOST IMPORTANT edge case. If we don't handle this, our algorithms 
 *    might get caught in an infinite loop because the color never changes.
 * 
 * Q: Are diagonal connections considered?
 * A: No, the problem specifies "4-directionally adjacent" (up, down, left, right).
 * 
 * Q: Can we modify the grid in-place, or should we return a new grid?
 * A: Returning the modified original grid (in-place) is optimal for space.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - This is fundamentally a Graph Traversal problem. 
 * - The grid is an implicit graph where each cell is a node, and the edges connect 
 *   it to its 4 cardinal neighbors.
 * - We only traverse to neighbors if their color matches the original color of 
 *   the starting pixel.
 * - EDGE CASE AVOIDANCE: Always check `if (originalColor == targetColor) return grid;` 
 *   early. Otherwise, we'll keep visiting the same pixels infinitely.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Start by writing down the base check (is the new color the same as the old?).
 * - Step 2: Mention that this can be solved using either Depth-First Search (DFS) 
 *   or Breadth-First Search (BFS).
 * - Step 3: DFS is usually faster to type out and highly elegant because of recursion. 
 *   Offer to code the Recursive DFS first.
 * - Step 4: Mention the Time and Space Complexity explicitly. 
 *   (Time: O(M*N) to visit every cell, Space: O(M*N) for the recursion call stack or queue).
 * - Step 5: If the interviewer asks about call stack limitations (StackOverflowError), 
 *   propose BFS or an Iterative DFS using an explicit Stack on the heap.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Grid:                Start: sr=1, sc=1 (Value 1)      Target: 2
 * 
 * Before:              Traversal Steps:                 After:
 * [1, 1, 1]            (1,1) -> changes to 2            [2, 2, 2]
 * [1, 1, 0]            spreads left, up, right          [2, 2, 0]
 * [1, 0, 1]            spreads to all connected 1s      [2, 0, 1]
 * 
 * Notice that the bottom-right '1' is NOT changed because it's not connected 
 * 4-directionally to the other '1's!
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

class FloodFill {

    /**
     * SOLUTION 1: Recursive Depth-First Search (DFS)
     * ------------------------------------------------------------------------
     * Pros: Extremely clean, short, and intuitive. Standard interview choice.
     * Cons: Uses the JVM call stack. If the grid is massive, it could trigger 
     * a StackOverflowError (though safe here since max size is 30x30).
     * 
     * Time Complexity: O(M * N) where M is rows, N is columns.
     * Space Complexity: O(M * N) worst-case recursion stack depth.
     */
    public int[][] floodFillDFS(int[][] grid, int sr, int sc, int target) {
        int originalColor = grid[sr][sc];
        
        // Edge Case: If the color is already the target, do nothing.
        // This prevents an infinite recursion loop.
        if (originalColor != target) {
            dfs(grid, sr, sc, originalColor, target);
        }
        return grid;
    }

    private void dfs(int[][] grid, int r, int c, int originalColor, int target) {
        // Base case / Boundary checks:
        // 1. Out of bounds checking
        // 2. Pixel does not match the original color we are replacing
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != originalColor) {
            return;
        }

        // Change the current pixel color
        grid[r][c] = target;

        // Recursively visit all 4 neighbors
        dfs(grid, r + 1, c, originalColor, target); // Down
        dfs(grid, r - 1, c, originalColor, target); // Up
        dfs(grid, r, c + 1, originalColor, target); // Right
        dfs(grid, r, c - 1, originalColor, target); // Left
    }

    /**
     * SOLUTION 2: Breadth-First Search (BFS) Using Iterative Queue
     * ------------------------------------------------------------------------
     * Pros: Avoids JVM call stack limits. Explores radially.
     * Feature Highlight: We use Java 14+ `record` to cleanly define coordinate pairs
     * instead of using `int[]` arrays, improving memory overhead and readability.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M * N) worst-case queue size.
     */
    public int[][] floodFillBFS(int[][] grid, int sr, int sc, int target) {
        int originalColor = grid[sr][sc];
        if (originalColor == target) return grid;

        // Modern Java Feature: Record for clean, immutable data carriers
        record Point(int r, int c) {}

        Queue<Point> queue = new LinkedList<>();
        queue.offer(new Point(sr, sc));
        
        // Change color immediately when adding to queue to avoid duplicate processing
        grid[sr][sc] = target; 

        // 4-directional moves: Right, Left, Down, Up
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            for (int[] dir : directions) {
                int newR = current.r() + dir[0];
                int newC = current.c() + dir[1];

                // Check bounds and color
                if (newR >= 0 && newR < grid.length && 
                    newC >= 0 && newC < grid[0].length && 
                    grid[newR][newC] == originalColor) {
                    
                    grid[newR][newC] = target;
                    queue.offer(new Point(newR, newC));
                }
            }
        }
        return grid;
    }

    /**
     * SOLUTION 3: Iterative Depth-First Search (DFS) Using a Stack
     * ------------------------------------------------------------------------
     * Pros: Simulates the recursive approach perfectly but handles stack memory 
     * explicitly on the heap (using Deque). Prevents StackOverflowErrors.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M * N) worst-case stack size.
     */
    public int[][] floodFillIterativeDFS(int[][] grid, int sr, int sc, int target) {
        int originalColor = grid[sr][sc];
        if (originalColor == target) return grid;

        record Point(int r, int c) {}
        
        // Deque is the recommended collection type for stacks in modern Java
        Deque<Point> stack = new ArrayDeque<>();
        stack.push(new Point(sr, sc));
        
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!stack.isEmpty()) {
            Point current = stack.pop();
            
            // Re-check before updating, as it might have been updated by another path
            if (grid[current.r()][current.c()] == originalColor) {
                grid[current.r()][current.c()] = target;

                // Push valid neighbors to the stack
                for (int[] dir : directions) {
                    int newR = current.r() + dir[0];
                    int newC = current.c() + dir[1];

                    if (newR >= 0 && newR < grid.length && 
                        newC >= 0 && newC < grid[0].length && 
                        grid[newR][newC] == originalColor) {
                        stack.push(new Point(newR, newC));
                    }
                }
            }
        }
        return grid;
    }

    /**
     * UTILITY: Print the grid elegantly using Java Streams.
     */
    private static void printGrid(int[][] grid, String label) {
        System.out.println("--- " + label + " ---");
        Arrays.stream(grid)
              .map(row -> Arrays.stream(row)
                                .mapToObj(String::valueOf)
                                .collect(Collectors.joining("  ")))
              .forEach(System.out::println);
        System.out.println();
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        FloodFill solution = new FloodFill();

        // Deep copy of initial grid to test multiple methods independently
        int[][] originalGrid = {
            {1, 1, 1},
            {1, 1, 0},
            {1, 0, 1}
        };

        // Test 1: Recursive DFS
        int[][] gridForDFS = Arrays.stream(originalGrid).map(int[]::clone).toArray(int[][]::new);
        solution.floodFillDFS(gridForDFS, 1, 1, 2);
        printGrid(gridForDFS, "Result after Recursive DFS");

        // Test 2: BFS with Queue
        int[][] gridForBFS = Arrays.stream(originalGrid).map(int[]::clone).toArray(int[][]::new);
        solution.floodFillBFS(gridForBFS, 1, 1, 2);
        printGrid(gridForBFS, "Result after BFS");

        // Test 3: Iterative DFS with Stack
        int[][] gridForIterative = Arrays.stream(originalGrid).map(int[]::clone).toArray(int[][]::new);
        solution.floodFillIterativeDFS(gridForIterative, 1, 1, 2);
        printGrid(gridForIterative, "Result after Iterative DFS");
    }
}
