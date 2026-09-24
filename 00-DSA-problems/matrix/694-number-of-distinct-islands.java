/*
 * Given a 2D grid grid of 0s and 1s, where 1 represents land and 0 represents
 * water. An island is formed by connecting adjacent lands horizontally or
 * vertically.
 * Two islands are considered distinct if and only if one island is not a
 * translation of the other.
 * 
 * You need to count the number of distinct islands in the grid.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

class Solution {

    // Solution 1: DFS with Path Signature (Optimal)
    public int numDistinctIslands(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        Set<String> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    StringBuilder path = new StringBuilder();
                    dfs(grid, i, j, i, j, path, "S"); // S for start
                    distinctIslands.add(path.toString());
                }
            }
        }

        return distinctIslands.size();
    }

    private void dfs(int[][] grid, int i, int j, int startI, int startJ,
            StringBuilder path, String direction) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                grid[i][j] == 0) {
            return;
        }

        grid[i][j] = 0; // Mark as visited
        path.append(direction);

        // Explore all 4 directions
        dfs(grid, i + 1, j, startI, startJ, path, "D"); // Down
        dfs(grid, i - 1, j, startI, startJ, path, "U"); // Up
        dfs(grid, i, j + 1, startI, startJ, path, "R"); // Right
        dfs(grid, i, j - 1, startI, startJ, path, "L"); // Left

        path.append("B"); // Backtrack marker
    }

    // Solution 2: DFS with Relative Coordinates (Alternative)
    public int numDistinctIslands2(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        Set<List<List<Integer>>> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    List<List<Integer>> island = new ArrayList<>();
                    dfsCollectCells(grid, i, j, i, j, island);
                    distinctIslands.add(island);
                }
            }
        }

        return distinctIslands.size();
    }

    private void dfsCollectCells(int[][] grid, int i, int j, int startI, int startJ,
            List<List<Integer>> island) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                grid[i][j] == 0) {
            return;
        }

        grid[i][j] = 0; // Mark as visited
        // Add relative coordinates
        island.add(Arrays.asList(i - startI, j - startJ));

        // Explore all 4 directions
        dfsCollectCells(grid, i + 1, j, startI, startJ, island);
        dfsCollectCells(grid, i - 1, j, startI, startJ, island);
        dfsCollectCells(grid, i, j + 1, startI, startJ, island);
        dfsCollectCells(grid, i, j - 1, startI, startJ, island);
    }

    // Solution 3: BFS with Path Signature
    public int numDistinctIslands3(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        Set<String> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    String signature = bfsGetSignature(grid, i, j);
                    distinctIslands.add(signature);
                }
            }
        }

        return distinctIslands.size();
    }

    private String bfsGetSignature(int[][] grid, int startI, int startJ) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { startI, startJ });
        grid[startI][startJ] = 0;

        List<String> path = new ArrayList<>();
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        String[] dirNames = { "D", "U", "R", "L" };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int i = curr[0], j = curr[1];

            for (int d = 0; d < 4; d++) {
                int ni = i + directions[d][0];
                int nj = j + directions[d][1];

                if (ni >= 0 && ni < grid.length && nj >= 0 && nj < grid[0].length &&
                        grid[ni][nj] == 1) {
                    grid[ni][nj] = 0;
                    queue.offer(new int[] { ni, nj });
                    path.add(dirNames[d]);
                }
            }
            path.add("B"); // End of current cell exploration
        }

        return String.join("", path);
    }

    // Solution 4: Non-destructive version (preserves original grid)
    public int numDistinctIslandsNonDestructive(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        boolean[][] visited = new boolean[m][n];
        Set<String> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1 && !visited[i][j]) {
                    StringBuilder path = new StringBuilder();
                    dfsNonDestructive(grid, visited, i, j, i, j, path, "S");
                    distinctIslands.add(path.toString());
                }
            }
        }

        return distinctIslands.size();
    }

    private void dfsNonDestructive(int[][] grid, boolean[][] visited, int i, int j,
            int startI, int startJ, StringBuilder path, String direction) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                grid[i][j] == 0 || visited[i][j]) {
            return;
        }

        visited[i][j] = true;
        path.append(direction);

        // Explore all 4 directions
        dfsNonDestructive(grid, visited, i + 1, j, startI, startJ, path, "D");
        dfsNonDestructive(grid, visited, i - 1, j, startI, startJ, path, "U");
        dfsNonDestructive(grid, visited, i, j + 1, startI, startJ, path, "R");
        dfsNonDestructive(grid, visited, i, j - 1, startI, startJ, path, "L");

        path.append("B"); // Backtrack marker
    }

}

// Test class
class TestDistinctIslands {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1
        int[][] grid1 = {
                { 1, 1, 0, 0, 0 },
                { 1, 1, 0, 0, 0 },
                { 0, 0, 0, 1, 1 },
                { 0, 0, 0, 1, 1 }
        };
        System.out.println("Test 1: " + solution.numDistinctIslandsNonDestructive(deepCopy(grid1)));
        // Expected: 1 (both islands have same shape)

        // Test Case 2
        int[][] grid2 = {
                { 1, 1, 0, 1, 1 },
                { 1, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 1 },
                { 1, 1, 0, 1, 1 }
        };
        System.out.println("Test 2: " + solution.numDistinctIslandsNonDestructive(deepCopy(grid2)));
        // Expected: 3 (three different shapes)

        // Test Case 3
        int[][] grid3 = {
                { 1, 1, 1 },
                { 0, 1, 0 },
                { 0, 1, 0 },
                { 1, 0, 0 }
        };
        System.out.println("Test 3: " + solution.numDistinctIslandsNonDestructive(deepCopy(grid3)));
        // Expected: 2
    }

    private static int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

}
