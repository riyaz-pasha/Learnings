import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * You are given an n x n binary matrix grid where 1 represents land and 0
 * represents water.
 * 
 * An island is a 4-directionally connected group of 1's not connected to any
 * other 1's. There are exactly two islands in grid.
 * 
 * You may change 0's to 1's to connect the two islands to form one island.
 * 
 * Return the smallest number of 0's you must flip to connect the two islands.
 * 
 */

class ShortestBridge {

    /*
    ============================================================
    LeetCode 934: Shortest Bridge
    ============================================================

    Problem Summary:
    ----------------
    - Given a binary grid with exactly two islands (groups of 1s)
    - You can flip 0s to 1s
    - Return the minimum number of flips needed to connect the two islands

    Strategy:
    ---------
    1) Use DFS to find and mark the first island
    2) Use BFS to expand outward from the first island
    3) The first time BFS touches the second island → answer

    ============================================================
    */


    int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    public int shortestBridge(int[][] grid) {
        int n = grid.length;
        boolean[][] visited = new boolean[n][n];
        Queue<int[]> queue = findFirstIsland(grid, visited);
        return getStepsToReachSecondIsland(grid, visited, queue);
    }

    private int getStepsToReachSecondIsland(int[][] grid, boolean[][] visited, Queue<int[]> queue) {
        int n = grid.length;
        int steps = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] cell = queue.poll();

                for (int[] dir : directions) {
                    int x = cell[0] + dir[0];
                    int y = cell[1] + dir[1];

                    if (x >= 0 && y >= 0 && x < n && y < n && !visited[x][y]) {
                        if (grid[x][y] == 1) {
                            return steps;
                        }

                        visited[x][y] = true;
                        queue.offer(new int[] { x, y });
                    }
                }
            }
            steps++;
        }
        return steps;
    }

    private Queue<int[]> findFirstIsland(int[][] grid, boolean[][] visited) {
        int n = grid.length;
        Queue<int[]> queue = new LinkedList<>();

        // Find first island
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    dfs(grid, i, j, visited, queue);
                    return queue;
                }
            }
        }

        return queue;
    }

    private void dfs(int[][] grid, int i, int j, boolean[][] visited, Queue<int[]> queue) {
        int n = grid.length;
        if (i < 0 || j < 0 || i >= n || j >= n || visited[i][j] || grid[i][j] == 0) {
            return;
        }
        visited[i][j] = true;
        queue.offer(new int[] { i, j });

        for (int[] dir : directions) {
            dfs(grid, i + dir[0], j + dir[1], visited, queue);
        }
    }
    /*
     ============================================================
     Time & Space Complexity Analysis
     ============================================================

     Let n = grid dimension (n x n)

     Time Complexity:
     ----------------
     - DFS visits each cell at most once → O(n²)
     - BFS visits each cell at most once → O(n²)

     Total Time: O(n²)

     Space Complexity:
     -----------------
     - visited array → O(n²)
     - BFS queue → O(n²) in worst case
     - DFS recursion stack → O(n²) worst case

     Total Space: O(n²)

     This is optimal for grid traversal problems.
     ============================================================
     */
}

class Solution {

    /**
     * Optimal Solution: DFS to find first island + Multi-source BFS for shortest
     * bridge
     * Time: O(n²), Space: O(n²)
     */
    public int shortestBridge(int[][] grid) {
        int n = grid.length;
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[n][n];
        boolean found = false;

        // Step 1: Find the first island using DFS and add all its cells to queue
        for (int i = 0; i < n && !found; i++) {
            for (int j = 0; j < n && !found; j++) {
                if (grid[i][j] == 1) {
                    dfs(grid, i, j, queue, visited);
                    found = true;
                }
            }
        }

        // Step 2: Multi-source BFS from first island to find second island
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int row = curr[0], col = curr[1];

                for (int[] dir : directions) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n &&
                            !visited[newRow][newCol]) {

                        if (grid[newRow][newCol] == 1) {
                            return steps; // Found the second island
                        }

                        visited[newRow][newCol] = true;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }

            steps++;
        }

        return -1; // Should never reach here given problem constraints
    }

    private void dfs(int[][] grid, int row, int col, Queue<int[]> queue, boolean[][] visited) {
        int n = grid.length;
        if (row < 0 || row >= n || col < 0 || col >= n ||
                visited[row][col] || grid[row][col] == 0) {
            return;
        }

        visited[row][col] = true;
        queue.offer(new int[] { row, col }); // Add to BFS queue

        // Explore all 4 directions
        dfs(grid, row - 1, col, queue, visited);
        dfs(grid, row + 1, col, queue, visited);
        dfs(grid, row, col - 1, queue, visited);
        dfs(grid, row, col + 1, queue, visited);
    }

    /**
     * Alternative: Mark first island with 2, then BFS
     * Time: O(n²), Space: O(n²)
     */
    public int shortestBridgeV2(int[][] grid) {
        int n = grid.length;
        Queue<int[]> queue = new LinkedList<>();
        boolean found = false;

        // Step 1: Find and mark first island as 2
        for (int i = 0; i < n && !found; i++) {
            for (int j = 0; j < n && !found; j++) {
                if (grid[i][j] == 1) {
                    markIsland(grid, i, j, queue);
                    found = true;
                }
            }
        }

        // Step 2: BFS from first island (marked as 2) to find second island (still 1)
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int row = curr[0], col = curr[1];

                for (int[] dir : dirs) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n) {
                        if (grid[newRow][newCol] == 1) {
                            return steps; // Found second island
                        }
                        if (grid[newRow][newCol] == 0) {
                            grid[newRow][newCol] = 2; // Mark as visited water
                            queue.offer(new int[] { newRow, newCol });
                        }
                    }
                }
            }

            steps++;
        }

        return -1;
    }

    private void markIsland(int[][] grid, int row, int col, Queue<int[]> queue) {
        int n = grid.length;
        if (row < 0 || row >= n || col < 0 || col >= n || grid[row][col] != 1) {
            return;
        }

        grid[row][col] = 2; // Mark as part of first island
        queue.offer(new int[] { row, col });

        // Mark all connected land cells
        markIsland(grid, row - 1, col, queue);
        markIsland(grid, row + 1, col, queue);
        markIsland(grid, row, col - 1, queue);
        markIsland(grid, row, col + 1, queue);
    }

    /**
     * Brute Force: Try all possible bridges (for understanding)
     * Time: O(n⁴), Space: O(n²) - Very inefficient
     */
    public int shortestBridgeBruteForce(int[][] grid) {
        int n = grid.length;
        List<int[]> island1 = new ArrayList<>();
        List<int[]> island2 = new ArrayList<>();

        // Find both islands
        boolean[][] visited = new boolean[n][n];
        boolean foundFirst = false;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    List<int[]> currentIsland = new ArrayList<>();
                    dfsCollect(grid, i, j, visited, currentIsland);

                    if (!foundFirst) {
                        island1 = currentIsland;
                        foundFirst = true;
                    } else {
                        island2 = currentIsland;
                        break;
                    }
                }
            }
        }

        // Find minimum Manhattan distance between islands
        int minDistance = Integer.MAX_VALUE;
        for (int[] cell1 : island1) {
            for (int[] cell2 : island2) {
                int distance = Math.abs(cell1[0] - cell2[0]) + Math.abs(cell1[1] - cell2[1]) - 1;
                minDistance = Math.min(minDistance, distance);
            }
        }

        return minDistance;
    }

    private void dfsCollect(int[][] grid, int row, int col, boolean[][] visited, List<int[]> island) {
        int n = grid.length;
        if (row < 0 || row >= n || col < 0 || col >= n ||
                visited[row][col] || grid[row][col] == 0) {
            return;
        }

        visited[row][col] = true;
        island.add(new int[] { row, col });

        dfsCollect(grid, row - 1, col, visited, island);
        dfsCollect(grid, row + 1, col, visited, island);
        dfsCollect(grid, row, col - 1, visited, island);
        dfsCollect(grid, row, col + 1, visited, island);
    }

    /**
     * Clean implementation with helper methods
     */
    public int shortestBridgeClean(int[][] grid) {
        int n = grid.length;
        Queue<int[]> queue = new LinkedList<>();

        // Find first island and prepare for BFS
        if (findFirstIsland(grid, queue)) {
            return bfsToSecondIsland(grid, queue);
        }

        return -1;
    }

    private boolean findFirstIsland(int[][] grid, Queue<int[]> queue) {
        int n = grid.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    markFirstIsland(grid, i, j, queue);
                    return true;
                }
            }
        }
        return false;
    }

    private void markFirstIsland(int[][] grid, int row, int col, Queue<int[]> queue) {
        int n = grid.length;
        if (row < 0 || row >= n || col < 0 || col >= n || grid[row][col] != 1) {
            return;
        }

        grid[row][col] = -1; // Mark as first island
        queue.offer(new int[] { row, col });

        markFirstIsland(grid, row - 1, col, queue);
        markFirstIsland(grid, row + 1, col, queue);
        markFirstIsland(grid, row, col - 1, queue);
        markFirstIsland(grid, row, col + 1, queue);
    }

    private int bfsToSecondIsland(int[][] grid, Queue<int[]> queue) {
        int n = grid.length;
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int distance = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int row = curr[0], col = curr[1];

                for (int[] dir : dirs) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (isValid(newRow, newCol, n)) {
                        if (grid[newRow][newCol] == 1) {
                            return distance; // Found second island
                        }
                        if (grid[newRow][newCol] == 0) {
                            grid[newRow][newCol] = -1; // Mark as visited
                            queue.offer(new int[] { newRow, newCol });
                        }
                    }
                }
            }

            distance++;
        }

        return -1;
    }

    private boolean isValid(int row, int col, int n) {
        return row >= 0 && row < n && col >= 0 && col < n;
    }

    // Test cases
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: Simple case
        int[][] grid1 = {
                { 0, 1 },
                { 1, 0 }
        };
        System.out.println("Test 1: " + sol.shortestBridge(grid1)); // Expected: 1

        // Test case 2: Larger islands
        int[][] grid2 = {
                { 0, 1, 0 },
                { 0, 0, 0 },
                { 0, 0, 1 }
        };
        System.out.println("Test 2: " + sol.shortestBridge(grid2)); // Expected: 2

        // Test case 3: Complex case
        int[][] grid3 = {
                { 1, 1, 1, 1, 1 },
                { 1, 0, 0, 0, 1 },
                { 1, 0, 1, 0, 1 },
                { 1, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1 }
        };
        System.out.println("Test 3: " + sol.shortestBridge(grid3)); // Expected: 1

        // Test case 4: Diagonal islands
        int[][] grid4 = {
                { 1, 1, 0, 0, 0 },
                { 1, 0, 0, 0, 0 },
                { 0, 0, 0, 1, 1 },
                { 0, 0, 0, 1, 1 },
                { 0, 0, 0, 0, 0 }
        };
        System.out.println("Test 4: " + sol.shortestBridge(grid4)); // Expected: 1

        // Test different implementations
        System.out.println("Test 1 (V2): " + sol.shortestBridgeV2(copyGrid(grid1)));
        System.out.println("Test 1 (Clean): " + sol.shortestBridgeClean(copyGrid(grid1)));
    }

    // Helper method to copy grid for testing
    private static int[][] copyGrid(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

}
