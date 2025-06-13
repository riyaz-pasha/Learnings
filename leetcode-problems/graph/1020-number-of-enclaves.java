import java.util.*;
/*
 * You are given an m x n binary matrix grid, where 0 represents a sea cell and
 * 1 represents a land cell.
 * 
 * A move consists of walking from one land cell to another adjacent
 * (4-directionally) land cell or walking off the boundary of the grid.
 * 
 * Return the number of land cells in grid for which we cannot walk off the
 * boundary of the grid in any number of moves.
 * 
 * Input: grid = [[0,0,0,0],[1,0,1,0],[0,1,1,0],[0,0,0,0]]
 * Output: 3
 * Explanation: There are three 1s that are enclosed by 0s, and one 1 that is
 * not enclosed because its on the boundary.
 * 
 * Input: grid = [[0,1,1,0],[0,0,1,0],[0,0,1,0],[0,0,0,0]]
 * Output: 0
 * Explanation: All 1s are either on the boundary or can reach the boundary.
 * 
 */

// Solution 1: DFS Approach
// Time Complexity: O(m * n), Space Complexity: O(m * n) for recursion stack
class Solution {

    public int numEnclaves(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Mark all land cells connected to boundary as visited (set to 0)
        // Check first and last rows
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1)
                dfs(grid, 0, j); // First row
            if (grid[m - 1][j] == 1)
                dfs(grid, m - 1, j); // Last row
        }

        // Check first and last columns
        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 1)
                dfs(grid, i, 0); // First column
            if (grid[i][n - 1] == 1)
                dfs(grid, i, n - 1); // Last column
        }

        // Count remaining land cells (enclaves)
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1)
                    count++;
            }
        }

        return count;
    }

    private void dfs(int[][] grid, int i, int j) {
        // Check bounds and if current cell is water or already visited
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] == 0) {
            return;
        }

        // Mark current land cell as visited
        grid[i][j] = 0;

        // Explore all 4 directions
        dfs(grid, i + 1, j); // Down
        dfs(grid, i - 1, j); // Up
        dfs(grid, i, j + 1); // Right
        dfs(grid, i, j - 1); // Left
    }

}

// Solution 2: BFS Approach
// Time Complexity: O(m * n), Space Complexity: O(m * n) for queue
class Solution2 {

    public int numEnclaves(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();

        // Add all boundary land cells to queue and mark them as visited
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 || i == m - 1 || j == 0 || j == n - 1) && grid[i][j] == 1) {
                    queue.offer(new int[] { i, j });
                    grid[i][j] = 0;
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

                if (nx >= 0 && nx < m && ny >= 0 && ny < n && grid[nx][ny] == 1) {
                    grid[nx][ny] = 0;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        // Count remaining land cells
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1)
                    count++;
            }
        }

        return count;
    }

}

// Solution 3: DFS with separate visited array (preserves original grid)
// Time Complexity: O(m * n), Space Complexity: O(m * n)
class Solution3 {

    public int numEnclaves(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];

        // Mark all land cells connected to boundary
        for (int j = 0; j < n; j++) {
            if (grid[0][j] == 1 && !visited[0][j]) {
                dfs(grid, visited, 0, j);
            }
            if (grid[m - 1][j] == 1 && !visited[m - 1][j]) {
                dfs(grid, visited, m - 1, j);
            }
        }

        for (int i = 0; i < m; i++) {
            if (grid[i][0] == 1 && !visited[i][0]) {
                dfs(grid, visited, i, 0);
            }
            if (grid[i][n - 1] == 1 && !visited[i][n - 1]) {
                dfs(grid, visited, i, n - 1);
            }
        }

        // Count unvisited land cells
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    count++;
                }
            }
        }

        return count;
    }

    private void dfs(int[][] grid, boolean[][] visited, int i, int j) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                visited[i][j] || grid[i][j] == 0) {
            return;
        }

        visited[i][j] = true;

        dfs(grid, visited, i + 1, j);
        dfs(grid, visited, i - 1, j);
        dfs(grid, visited, i, j + 1);
        dfs(grid, visited, i, j - 1);
    }

}

// Example usage and test cases
class TestEnclaves {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[][] grid1 = {
                { 0, 0, 0, 0 },
                { 1, 0, 1, 0 },
                { 0, 1, 1, 0 },
                { 0, 0, 0, 0 }
        };
        System.out.println("Test 1: " + solution.numEnclaves(grid1)); // Expected: 3

        // Test case 2
        int[][] grid2 = {
                { 0, 1, 1, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 0, 0 }
        };
        System.out.println("Test 2: " + solution.numEnclaves(grid2)); // Expected: 0

        // Test case 3
        int[][] grid3 = {
                { 0, 0, 0, 1, 1, 1, 0, 1, 0, 0 },
                { 1, 1, 0, 0, 0, 1, 0, 1, 1, 1 },
                { 0, 0, 0, 1, 1, 1, 0, 1, 0, 0 },
                { 0, 1, 1, 0, 0, 0, 1, 0, 1, 0 },
                { 0, 1, 1, 1, 1, 1, 0, 0, 1, 0 },
                { 0, 0, 1, 0, 1, 1, 1, 1, 0, 1 },
                { 0, 1, 1, 0, 0, 0, 1, 1, 1, 1 },
                { 0, 0, 1, 0, 0, 1, 0, 1, 0, 1 },
                { 1, 0, 1, 0, 1, 1, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 1, 1, 0, 0, 0, 1 }
        };
        System.out.println("Test 3: " + solution.numEnclaves(grid3)); // Expected: 3
    }

}
