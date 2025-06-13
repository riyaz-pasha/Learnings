import java.util.*;
/*
 * Given a 2D grid consists of 0s (land) and 1s (water). An island is a maximal
 * 4-directionally connected group of 0s and a closed island is an island
 * totally (all left, top, right, bottom) surrounded by 1s.
 * 
 * Return the number of closed islands.
 * 
 * Input: grid =
 * [[1,1,1,1,1,1,1,0],[1,0,0,0,0,1,1,0],[1,0,1,0,1,1,1,0],[1,0,0,0,0,1,0,1],[1,1
 * ,1,1,1,1,1,0]]
 * Output: 2
 * Explanation:
 * Islands in gray are closed because they are completely surrounded by water
 * (group of 1s).
 * 
 * Input: grid = [
 * [1,1,1,1,1,1,1],
 * [1,0,0,0,0,0,1],
 * [1,0,1,1,1,0,1],
 * [1,0,1,0,1,0,1],
 * [1,0,1,1,1,0,1],
 * [1,0,0,0,0,0,1],
 * [1,1,1,1,1,1,1]]
 * Output: 2
 */

// Solution 1: DFS - Mark boundary islands first, then count remaining islands
// Time Complexity: O(m * n), Space Complexity: O(m * n) for recursion stack
class Solution {
    public int closedIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Step 1: Mark all islands connected to boundary as "visited" (set to 1)
        // Check first and last rows
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 0)
                markIsland(grid, 0, j); // First row
            if (grid[m - 1][j] == 0)
                markIsland(grid, m - 1, j); // Last row
        }

        // Check first and last columns
        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 0)
                markIsland(grid, i, 0); // First column
            if (grid[i][n - 1] == 0)
                markIsland(grid, i, n - 1); // Last column
        }

        // Step 2: Count remaining islands (these are closed islands)
        int closedIslands = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) {
                    markIsland(grid, i, j); // Mark this island as visited
                    closedIslands++;
                }
            }
        }

        return closedIslands;
    }

    private void markIsland(int[][] grid, int i, int j) {
        // Check bounds and if current cell is water or already visited
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] == 1) {
            return;
        }

        // Mark current land cell as visited (convert to water)
        grid[i][j] = 1;

        // Explore all 4 directions
        markIsland(grid, i + 1, j); // Down
        markIsland(grid, i - 1, j); // Up
        markIsland(grid, i, j + 1); // Right
        markIsland(grid, i, j - 1); // Left
    }
}

// Solution 2: BFS Approach
// Time Complexity: O(m * n), Space Complexity: O(m * n) for queue
class Solution2 {
    public int closedIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Step 1: Mark all boundary-connected islands using BFS
        Queue<int[]> queue = new LinkedList<>();

        // Add all boundary land cells to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 || i == m - 1 || j == 0 || j == n - 1) && grid[i][j] == 0) {
                    queue.offer(new int[] { i, j });
                    grid[i][j] = 1;
                }
            }
        }

        // BFS to mark all connected land cells
        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < m && ny >= 0 && ny < n && grid[nx][ny] == 0) {
                    grid[nx][ny] = 1;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        // Step 2: Count remaining islands
        int closedIslands = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) {
                    bfsMarkIsland(grid, i, j);
                    closedIslands++;
                }
            }
        }

        return closedIslands;
    }

    private void bfsMarkIsland(int[][] grid, int startI, int startJ) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { startI, startJ });
        grid[startI][startJ] = 1;

        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0], y = cell[1];

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < grid.length && ny >= 0 && ny < grid[0].length && grid[nx][ny] == 0) {
                    grid[nx][ny] = 1;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }
    }

}

// Solution 3: DFS with separate visited array (preserves original grid)
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution3 {

    public int closedIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];

        // Step 1: Mark all boundary-connected islands as visited
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 0 && !visited[0][j]) {
                dfs(grid, visited, 0, j);
            }
            if (grid[m - 1][j] == 0 && !visited[m - 1][j]) {
                dfs(grid, visited, m - 1, j);
            }
        }

        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 0 && !visited[i][0]) {
                dfs(grid, visited, i, 0);
            }
            if (grid[i][n - 1] == 0 && !visited[i][n - 1]) {
                dfs(grid, visited, i, n - 1);
            }
        }

        // Step 2: Count remaining unvisited islands
        int closedIslands = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0 && !visited[i][j]) {
                    dfs(grid, visited, i, j);
                    closedIslands++;
                }
            }
        }

        return closedIslands;
    }

    private void dfs(int[][] grid, boolean[][] visited, int i, int j) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                visited[i][j] || grid[i][j] == 1) {
            return;
        }

        visited[i][j] = true;

        dfs(grid, visited, i + 1, j);
        dfs(grid, visited, i - 1, j);
        dfs(grid, visited, i, j + 1);
        dfs(grid, visited, i, j - 1);
    }

}

// Solution 4: Single pass DFS - check if island touches boundary during
// traversal
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution4 {

    public int closedIsland(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];
        int closedIslands = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0 && !visited[i][j]) {
                    if (isClosedIsland(grid, visited, i, j)) {
                        closedIslands++;
                    }
                }
            }
        }

        return closedIslands;
    }

    private boolean isClosedIsland(int[][] grid, boolean[][] visited, int i, int j) {
        // If we reach boundary, this island is not closed
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length) {
            return false;
        }

        // If we hit water or already visited cell, continue
        if (grid[i][j] == 1 || visited[i][j]) {
            return true;
        }

        // Mark as visited
        visited[i][j] = true;

        // Check all 4 directions - all must be closed for island to be closed
        boolean down = isClosedIsland(grid, visited, i + 1, j);
        boolean up = isClosedIsland(grid, visited, i - 1, j);
        boolean right = isClosedIsland(grid, visited, i, j + 1);
        boolean left = isClosedIsland(grid, visited, i, j - 1);

        return down && up && right && left;
    }

}

// Example usage and test cases
class TestClosedIslands {
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] grid1 = {
                { 1, 1, 1, 1, 1, 1, 1, 0 },
                { 1, 0, 0, 0, 0, 1, 1, 0 },
                { 1, 0, 1, 0, 1, 1, 1, 0 },
                { 1, 0, 0, 0, 0, 1, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 1, 0 }
        };
        System.out.println("Test 1: " + solution.closedIsland(grid1)); // Expected: 2

        // Test case 2
        int[][] grid2 = {
                { 0, 0, 1, 0, 0 },
                { 0, 1, 0, 1, 0 },
                { 0, 1, 1, 1, 0 }
        };
        System.out.println("Test 2: " + solution.closedIsland(grid2)); // Expected: 1

        // Test case 3
        int[][] grid3 = {
                { 1, 1, 1, 1, 1, 1, 1 },
                { 1, 0, 0, 0, 0, 0, 1 },
                { 1, 0, 1, 1, 1, 0, 1 },
                { 1, 0, 1, 0, 1, 0, 1 },
                { 1, 0, 1, 1, 1, 0, 1 },
                { 1, 0, 0, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 1 }
        };
        System.out.println("Test 3: " + solution.closedIsland(grid3)); // Expected: 2
    }

}
