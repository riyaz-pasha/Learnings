/*
 * Given an m x n binary matrix mat, return the distance of the nearest 0 for
 * each cell.
 * 
 * The distance between two cells sharing a common edge is 1.
 * 
 * https://assets.leetcode.com/uploads/2021/04/24/01-2-grid.jpg
 * Input: mat = [[0,0,0],[0,1,0],[1,1,1]]
 * Output: [[0,0,0],[0,1,0],[1,2,1]]
 */

import java.util.*;

class Matrix01Distance {

    // Solution 1: Multi-source BFS (Most Efficient)
    // Time: O(m×n), Space: O(m×n) extra space
    public int[][] updateMatrix(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        int[][] result = new int[m][n];
        Queue<int[]> queue = new LinkedList<>();

        // Initialize: add all 0s to queue and mark 1s as unvisited
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    result[i][j] = 0;
                    queue.offer(new int[] { i, j });
                } else {
                    result[i][j] = Integer.MAX_VALUE; // Mark as unvisited
                }
            }
        }

        // 4 directions: up, down, left, right
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // BFS from all 0s simultaneously
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                // Check bounds and if we found a shorter path
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                    if (result[newRow][newCol] > result[row][col] + 1) {
                        result[newRow][newCol] = result[row][col] + 1;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }
        }

        return result;
    }

    // Solution 2: Dynamic Programming (Two-pass)
    // Time: O(m×n), Space: O(1) extra space
    public int[][] updateMatrixDP(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        int[][] result = new int[m][n];

        // Initialize
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    result[i][j] = 0;
                } else {
                    result[i][j] = Integer.MAX_VALUE - 1; // Avoid overflow
                }
            }
        }

        // First pass: top-left to bottom-right
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (result[i][j] != 0) {
                    if (i > 0) {
                        result[i][j] = Math.min(result[i][j], result[i - 1][j] + 1);
                    }
                    if (j > 0) {
                        result[i][j] = Math.min(result[i][j], result[i][j - 1] + 1);
                    }
                }
            }
        }

        // Second pass: bottom-right to top-left
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (result[i][j] != 0) {
                    if (i < m - 1) {
                        result[i][j] = Math.min(result[i][j], result[i + 1][j] + 1);
                    }
                    if (j < n - 1) {
                        result[i][j] = Math.min(result[i][j], result[i][j + 1] + 1);
                    }
                }
            }
        }

        return result;
    }

    // Solution 3: BFS from each 1 (Less efficient, for comparison)
    public int[][] updateMatrixBruteForce(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        int[][] result = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    result[i][j] = 0;
                } else {
                    result[i][j] = bfsFromCell(mat, i, j);
                }
            }
        }

        return result;
    }

    private int bfsFromCell(int[][] mat, int startRow, int startCol) {
        int m = mat.length, n = mat[0].length;
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[m][n];
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        queue.offer(new int[] { startRow, startCol, 0 });
        visited[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], dist = curr[2];

            if (mat[row][col] == 0) {
                return dist;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                        && !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    queue.offer(new int[] { newRow, newCol, dist + 1 });
                }
            }
        }

        return -1; // Should never reach here for valid input
    }

    // Solution 4: Optimized Multi-source BFS with visited array
    public int[][] updateMatrixOptimized(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        int[][] result = new int[m][n];
        boolean[][] visited = new boolean[m][n];
        Queue<int[]> queue = new LinkedList<>();

        // Add all 0s to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    queue.offer(new int[] { i, j, 0 });
                    visited[i][j] = true;
                }
            }
        }

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], dist = curr[2];
            result[row][col] = dist;

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                        && !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    queue.offer(new int[] { newRow, newCol, dist + 1 });
                }
            }
        }

        return result;
    }

    // Utility method to print matrix
    public void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println();
    }

    // Test method
    public static void main(String[] args) {
        Matrix01Distance solution = new Matrix01Distance();

        // Test case 1
        int[][] mat1 = { { 0, 0, 0 }, { 0, 1, 0 }, { 1, 1, 1 } };
        System.out.println("Input:");
        solution.printMatrix(mat1);

        System.out.println("Multi-source BFS:");
        int[][] result1 = solution.updateMatrix(mat1);
        solution.printMatrix(result1);

        // Test case 2
        int[][] mat2 = { { 0, 1, 1, 0 }, { 1, 1, 1, 1 }, { 1, 1, 1, 1 }, { 0, 1, 1, 0 } };
        System.out.println("Input:");
        solution.printMatrix(mat2);

        System.out.println("Dynamic Programming:");
        int[][] result2 = solution.updateMatrixDP(mat2);
        solution.printMatrix(result2);

        // Test case 3 - single cell
        int[][] mat3 = { { 1 } };
        System.out.println("Edge case - single 1:");
        solution.printMatrix(mat3);
        // This would be invalid input as there's no 0, but let's handle it

        // Test case 4
        int[][] mat4 = { { 0 } };
        System.out.println("Edge case - single 0:");
        solution.printMatrix(mat4);
        int[][] result4 = solution.updateMatrix(mat4);
        solution.printMatrix(result4);
    }

}

class Matrix01 {
    public int[][] updateMatrix(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        int[][] res = new int[m][n];

        Queue<int[]> queue = new LinkedList<>();
        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                if (mat[row][col] == 0) {
                    res[row][col] = 0;
                    queue.offer(new int[] { row, col });
                } else {
                    res[row][col] = -1;
                }
            }
        }

        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        int[] cell;
        int row, col, newRow, newCol;
        while (!queue.isEmpty()) {
            cell = queue.poll();
            row = cell[0];
            col = cell[1];
            for (int[] dir : directions) {
                newRow = row + dir[0];
                newCol = col + dir[1];
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n && res[newRow][newCol] == -1) {

                    res[newRow][newCol] = res[row][col] + 1;
                    queue.offer(new int[] { newRow, newCol });

                }
            }
        }
        return res;
    }

       /**
     * Explanation of the BFS Approach:
     *
     * Initialization:
     *   - Create a 'dist' matrix of the same size as the input 'mat' to store the distances.
     *   - Initialize a Queue to perform BFS.
     *   - Iterate through the input matrix:
     *     - If a cell is 0:
     *       - Set its distance in 'dist' to 0.
     *       - Add its coordinates to the queue.
     *     - If a cell is 1:
     *       - Mark its distance in 'dist' as -1 (or any value indicating it hasn't been visited yet).
     *
     * BFS Traversal:
     *   - While the queue is not empty:
     *     - Dequeue a cell's coordinates (row, col).
     *     - Explore its four neighbors (up, down, left, right).
     *     - For each neighbor (newRow, newCol):
     *       - Check if the neighbor is within the matrix boundaries.
     *       - Check if the neighbor has not been visited yet (i.e., dist[newRow][newCol] == -1).
     *       - If both conditions are true:
     *         - Set dist[newRow][newCol] = dist[row][col] + 1.
     *         - Enqueue the neighbor's coordinates.
     *
     * Return Result:
     *   - After the BFS completes, the 'dist' matrix will contain the shortest distance
     *     of each cell to the nearest 0.
     *   - Return this matrix.
     */
}
