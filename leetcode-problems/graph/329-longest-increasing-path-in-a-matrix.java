import java.util.LinkedList;
import java.util.Queue;
/*
 * Given an m x n integers matrix, return the length of the longest increasing
 * path in matrix.
 * 
 * From each cell, you can either move in four directions: left, right, up, or
 * down. You may not move diagonally or move outside the boundary (i.e.,
 * wrap-around is not allowed).
 * 
 * Example 1:
 * Input: matrix = [[9,9,4],[6,6,8],[2,1,1]]
 * Output: 4
 * Explanation: The longest increasing path is [1, 2, 6, 9].
 * 
 * Example 2:
 * Input: matrix = [[3,4,5],[3,2,6],[2,2,1]]
 * Output: 4
 * Explanation: The longest increasing path is [3, 4, 5, 6]. Moving diagonally
 * is not allowed.
 * 
 * Example 3:
 * Input: matrix = [[1]]
 * Output: 1
 */

class LongestIncreasingPath {

    /**
     * Solution 1: DFS with Memoization (RECOMMENDED)
     * Time Complexity: O(m * n) - each cell is computed at most once
     * Space Complexity: O(m * n) - for memoization table and recursion stack
     */
    public int longestIncreasingPath1(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return 0;
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int[][] memo = new int[m][n]; // memoization table
        int maxPath = 1;

        // Try starting from each cell
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxPath = Math.max(maxPath, dfs(matrix, i, j, memo));
            }
        }

        return maxPath;
    }

    private int dfs(int[][] matrix, int row, int col, int[][] memo) {
        if (memo[row][col] != 0) {
            return memo[row][col]; // Already computed
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int maxLength = 1; // At least the current cell

        // 4 directions: up, down, left, right
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // Check bounds and increasing condition
            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                    && matrix[newRow][newCol] > matrix[row][col]) {
                maxLength = Math.max(maxLength, 1 + dfs(matrix, newRow, newCol, memo));
            }
        }

        memo[row][col] = maxLength;
        return maxLength;
    }

    /**
     * Solution 2: Topological Sort (Kahn's Algorithm)
     * Time Complexity: O(m * n) - each cell is processed once
     * Space Complexity: O(m * n) - for queue and in-degree matrix
     */
    public int longestIncreasingPath2(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return 0;
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int[][] indegree = new int[m][n]; // number of smaller neighbors
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // Calculate in-degrees (number of smaller neighbors for each cell)
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int[] dir : directions) {
                    int ni = i + dir[0];
                    int nj = j + dir[1];
                    if (ni >= 0 && ni < m && nj >= 0 && nj < n
                            && matrix[ni][nj] < matrix[i][j]) {
                        indegree[i][j]++;
                    }
                }
            }
        }

        // Start with cells that have no smaller neighbors (local minima)
        Queue<int[]> queue = new LinkedList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (indegree[i][j] == 0) {
                    queue.offer(new int[] { i, j });
                }
            }
        }

        int levels = 0;

        // Process level by level
        while (!queue.isEmpty()) {
            int size = queue.size();
            levels++;

            for (int k = 0; k < size; k++) {
                int[] current = queue.poll();
                int row = current[0];
                int col = current[1];

                // Check all neighbors
                for (int[] dir : directions) {
                    int newRow = row + dir[0];
                    int newCol = col + dir[1];

                    if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                            && matrix[newRow][newCol] > matrix[row][col]) {
                        indegree[newRow][newCol]--;
                        if (indegree[newRow][newCol] == 0) {
                            queue.offer(new int[] { newRow, newCol });
                        }
                    }
                }
            }
        }

        return levels;
    }

    /**
     * Solution 3: DFS without Memoization (Inefficient - for comparison)
     * Time Complexity: O(2^(m*n)) - exponential, very slow
     * Space Complexity: O(m * n) - recursion stack
     */
    public int longestIncreasingPath3(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return 0;
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int maxPath = 1;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxPath = Math.max(maxPath, dfsWithoutMemo(matrix, i, j, -1));
            }
        }

        return maxPath;
    }

    private int dfsWithoutMemo(int[][] matrix, int row, int col, int prevValue) {
        int m = matrix.length;
        int n = matrix[0].length;

        if (row < 0 || row >= m || col < 0 || col >= n || matrix[row][col] <= prevValue) {
            return 0;
        }

        int maxLength = 1;
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            maxLength = Math.max(maxLength, 1 + dfsWithoutMemo(matrix, newRow, newCol, matrix[row][col]));
        }

        return maxLength;
    }

    /**
     * Solution 4: Enhanced DFS with Path Reconstruction
     * Time Complexity: O(m * n)
     * Space Complexity: O(m * n)
     */
    public int longestIncreasingPath4(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return 0;
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int[][] memo = new int[m][n];
        int maxPath = 1;
        int[] startCell = new int[2];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int pathLength = dfsEnhanced(matrix, i, j, memo);
                if (pathLength > maxPath) {
                    maxPath = pathLength;
                    startCell[0] = i;
                    startCell[1] = j;
                }
            }
        }

        // Optional: Print the actual longest path
        System.out.println("Longest path starts at: (" + startCell[0] + ", " + startCell[1] + ")");
        System.out.println("Path value: " + matrix[startCell[0]][startCell[1]]);

        return maxPath;
    }

    private int dfsEnhanced(int[][] matrix, int row, int col, int[][] memo) {
        if (memo[row][col] != 0) {
            return memo[row][col];
        }

        int m = matrix.length;
        int n = matrix[0].length;
        int maxLength = 1;
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                    && matrix[newRow][newCol] > matrix[row][col]) {
                maxLength = Math.max(maxLength, 1 + dfsEnhanced(matrix, newRow, newCol, memo));
            }
        }

        memo[row][col] = maxLength;
        return maxLength;
    }

    // Test methods
    public static void main(String[] args) {
        LongestIncreasingPath solution = new LongestIncreasingPath();

        // Test Case 1
        int[][] matrix1 = { { 9, 9, 4 }, { 6, 6, 8 }, { 2, 1, 1 } };
        System.out.println("Test Case 1: [[9,9,4],[6,6,8],[2,1,1]]");
        System.out.println("Expected: 4");
        System.out.println("Solution 1 (DFS + Memo): " + solution.longestIncreasingPath1(matrix1));
        System.out.println("Solution 2 (Topological): " + solution.longestIncreasingPath2(matrix1));
        System.out.println("Solution 4 (Enhanced): " + solution.longestIncreasingPath4(matrix1));
        System.out.println();

        // Test Case 2
        int[][] matrix2 = { { 3, 4, 5 }, { 3, 2, 6 }, { 2, 2, 1 } };
        System.out.println("Test Case 2: [[3,4,5],[3,2,6],[2,2,1]]");
        System.out.println("Expected: 4");
        System.out.println("Solution 1 (DFS + Memo): " + solution.longestIncreasingPath1(matrix2));
        System.out.println("Solution 2 (Topological): " + solution.longestIncreasingPath2(matrix2));
        System.out.println();

        // Test Case 3
        int[][] matrix3 = { { 1 } };
        System.out.println("Test Case 3: [[1]]");
        System.out.println("Expected: 1");
        System.out.println("Solution 1 (DFS + Memo): " + solution.longestIncreasingPath1(matrix3));
        System.out.println("Solution 2 (Topological): " + solution.longestIncreasingPath2(matrix3));
    }

}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1 - DFS with Memoization (RECOMMENDED):
 * - Time Complexity: O(m * n)
 * Each cell is computed at most once due to memoization
 * Each cell is visited at most once in DFS
 * - Space Complexity: O(m * n)
 * O(m * n) for memoization table
 * O(m * n) for recursion stack in worst case
 * 
 * Solution 2 - Topological Sort:
 * - Time Complexity: O(m * n)
 * Each cell is processed exactly once
 * Each edge (neighbor relationship) is processed once
 * - Space Complexity: O(m * n)
 * O(m * n) for in-degree matrix
 * O(m * n) for queue in worst case
 * 
 * Solution 3 - DFS without Memoization:
 * - Time Complexity: O(2^(m*n)) - exponential
 * Without memoization, same subproblems are solved repeatedly
 * In worst case, explores all possible paths
 * - Space Complexity: O(m * n) for recursion stack
 * 
 * Solution 4 - Enhanced DFS:
 * - Time Complexity: O(m * n)
 * - Space Complexity: O(m * n)
 * - Additional feature: can track the starting position of longest path
 * 
 * KEY INSIGHTS:
 * 1. **Memoization is Critical**: Without it, the problem becomes exponential
 * 2. **No Cycles**: Since we only move to strictly greater values, there are no
 * cycles
 * 3. **DAG Structure**: The problem can be viewed as finding longest path in a
 * DAG
 * 4. **Multiple Valid Approaches**: Both DFS with memoization and topological
 * sort work well
 * 
 * WHEN TO USE WHICH:
 * - **DFS with Memoization**: Most intuitive, easier to implement
 * - **Topological Sort**: Good when you want to process in a specific order
 * - **Enhanced Version**: When you need additional information about the path
 * 
 * OPTIMIZATION TIPS:
 * 1. Use memoization to avoid recomputation
 * 2. Consider the direction of search (increasing vs decreasing)
 * 3. Early termination when possible
 * 4. Space optimization possible with iterative approaches
 */
