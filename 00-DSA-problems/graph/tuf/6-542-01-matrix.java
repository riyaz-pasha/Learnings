import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

class Matrix01 {

    record Entry(int row, int col, int distance) {
    }

    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    public int[][] updateMatrix(int[][] mat) {
        int rows = mat.length;
        int cols = mat[0].length;
        Queue<Entry> queue = new LinkedList<>();
        boolean[][] visited = new boolean[rows][cols];

        // 1️⃣ Initialize queue with all 0s
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mat[r][c] == 0) {
                    queue.offer(new Entry(r, c, 0));
                    visited[r][c] = true;
                }
            }
        }

        // 2️⃣ BFS to find shortest distance to 0
        while (!queue.isEmpty()) {
            Entry entry = queue.poll();
            int row = entry.row(), col = entry.col(), dist = entry.distance();

            for (int[] dir : DIRECTIONS) {
                int nr = row + dir[0];
                int nc = col + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    mat[nr][nc] = dist + 1;
                    queue.offer(new Entry(nr, nc, dist + 1));
                }
            }
        }

        return mat;
    }
}

class Solution {

    private record Cell(int row, int col) {
    }

    // 4 directions: up, down, left, right
    private static final int[][] DIRECTIONS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    public int[][] updateMatrix(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;

        int[][] distance = new int[m][n];
        boolean[][] visited = new boolean[m][n];
        Queue<Cell> queue = new LinkedList<>();

        // Step 1: Add all 0 cells to queue and mark as visited
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    queue.offer(new Cell(i, j));
                    visited[i][j] = true;
                    distance[i][j] = 0;
                }
            }
        }

        // Step 2: Multi-source BFS from all 0 cells
        while (!queue.isEmpty()) {
            Cell current = queue.poll();

            // Explore all 4 neighbors
            for (int[] dir : DIRECTIONS) {
                int newRow = current.row() + dir[0];
                int newCol = current.col() + dir[1];

                // Check bounds and if not visited
                if (isValid(newRow, newCol, m, n) && !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    distance[newRow][newCol] = distance[current.row()][current.col()] + 1;
                    queue.offer(new Cell(newRow, newCol));
                }
            }
        }

        return distance;
    }

    private boolean isValid(int row, int col, int m, int n) {
        return row >= 0 && row < m && col >= 0 && col < n;
    }
}

class MatrixAllSolutions {

    // =================================================================
    // APPROACH 1: BRUTE FORCE - BFS from each cell
    // Time: O(m*n * m*n) = O((m*n)^2)
    // Space: O(m*n) for queue
    // =================================================================

    public int[][] updateMatrixBruteForce(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
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
        int m = mat.length;
        int n = mat[0].length;
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[m][n];
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        queue.offer(new int[] { startRow, startCol, 0 });
        visited[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], dist = curr[2];

            if (mat[row][col] == 0) {
                return dist;
            }

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                        && !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    queue.offer(new int[] { newRow, newCol, dist + 1 });
                }
            }
        }
        return -1; // Should never reach here
    }

    // =================================================================
    // APPROACH 2: MULTI-SOURCE BFS (OPTIMAL)
    // Time: O(m*n)
    // Space: O(m*n)
    // =================================================================

    public int[][] updateMatrixMultiSourceBFS(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        int[][] distance = new int[m][n];
        boolean[][] visited = new boolean[m][n];
        Queue<int[]> queue = new LinkedList<>();
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // Add all 0 cells to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    queue.offer(new int[] { i, j });
                    visited[i][j] = true;
                }
            }
        }

        // BFS from all 0s simultaneously
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n
                        && !visited[newRow][newCol]) {
                    visited[newRow][newCol] = true;
                    distance[newRow][newCol] = distance[row][col] + 1;
                    queue.offer(new int[] { newRow, newCol });
                }
            }
        }

        return distance;
    }

    // =================================================================
    // APPROACH 3: MULTI-SOURCE BFS (Space Optimized - No visited array)
    // Time: O(m*n)
    // Space: O(m*n) for queue only
    // =================================================================

    public int[][] updateMatrixBFSOptimized(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        int[][] distance = new int[m][n];
        Queue<int[]> queue = new LinkedList<>();
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        // Initialize: 0 for zeros, MAX_VALUE for ones
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    queue.offer(new int[] { i, j });
                    distance[i][j] = 0;
                } else {
                    distance[i][j] = Integer.MAX_VALUE;
                }
            }
        }

        // BFS
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            for (int[] dir : dirs) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n) {
                    // Only process if we found a shorter distance
                    if (distance[newRow][newCol] > distance[row][col] + 1) {
                        distance[newRow][newCol] = distance[row][col] + 1;
                        queue.offer(new int[] { newRow, newCol });
                    }
                }
            }
        }

        return distance;
    }

    // =================================================================
    // APPROACH 4: DYNAMIC PROGRAMMING (Two-Pass)
    // Time: O(m*n)
    // Space: O(1) if we modify input, O(m*n) otherwise
    // =================================================================

    public int[][] updateMatrixDP(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        int[][] dp = new int[m][n];
        int MAX = m + n; // Maximum possible distance in grid

        // Initialize
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = (mat[i][j] == 0) ? 0 : MAX;
            }
        }

        // Pass 1: Top-left to bottom-right (check top and left)
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (dp[i][j] > 0) {
                    if (i > 0) {
                        dp[i][j] = Math.min(dp[i][j], dp[i - 1][j] + 1);
                    }
                    if (j > 0) {
                        dp[i][j] = Math.min(dp[i][j], dp[i][j - 1] + 1);
                    }
                }
            }
        }

        // Pass 2: Bottom-right to top-left (check bottom and right)
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (dp[i][j] > 0) {
                    if (i < m - 1) {
                        dp[i][j] = Math.min(dp[i][j], dp[i + 1][j] + 1);
                    }
                    if (j < n - 1) {
                        dp[i][j] = Math.min(dp[i][j], dp[i][j + 1] + 1);
                    }
                }
            }
        }

        return dp;
    }

    // =================================================================
    // APPROACH 5: DFS with Memoization (NOT RECOMMENDED - can be slow)
    // Time: O(m*n) best case, O((m*n)^2) worst case
    // Space: O(m*n) for memoization + O(m*n) recursion stack
    // =================================================================

    public int[][] updateMatrixDFS(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        int[][] memo = new int[m][n];

        for (int i = 0; i < m; i++) {
            Arrays.fill(memo[i], -1);
        }

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dfs(mat, memo, i, j);
            }
        }

        return memo;
    }

    private int dfs(int[][] mat, int[][] memo, int i, int j) {
        int m = mat.length;
        int n = mat[0].length;

        if (i < 0 || i >= m || j < 0 || j >= n) {
            return Integer.MAX_VALUE - 1; // Avoid overflow
        }

        if (mat[i][j] == 0) {
            return 0;
        }

        if (memo[i][j] != -1) {
            return memo[i][j];
        }

        // Try all 4 directions
        int up = dfs(mat, memo, i - 1, j);
        int down = dfs(mat, memo, i + 1, j);
        int left = dfs(mat, memo, i, j - 1);
        int right = dfs(mat, memo, i, j + 1);

        memo[i][j] = Math.min(Math.min(up, down), Math.min(left, right)) + 1;
        return memo[i][j];
    }

    // =================================================================
    // TEST HELPER
    // =================================================================

    public static void main(String[] args) {
        MatrixAllSolutions sol = new MatrixAllSolutions();

        int[][] mat = {
                { 0, 0, 0 },
                { 0, 1, 0 },
                { 1, 1, 1 }
        };

        System.out.println("Input:");
        printMatrix(mat);

        System.out.println("\nApproach 1 - Brute Force:");
        printMatrix(sol.updateMatrixBruteForce(mat));

        System.out.println("\nApproach 2 - Multi-Source BFS:");
        printMatrix(sol.updateMatrixMultiSourceBFS(mat));

        System.out.println("\nApproach 3 - BFS Optimized:");
        printMatrix(sol.updateMatrixBFSOptimized(mat));

        System.out.println("\nApproach 4 - Dynamic Programming:");
        printMatrix(sol.updateMatrixDP(mat));

        System.out.println("\nApproach 5 - DFS with Memoization:");
        printMatrix(sol.updateMatrixDFS(mat));
    }

    private static void printMatrix(int[][] mat) {
        for (int[] row : mat) {
            System.out.println(Arrays.toString(row));
        }
    }
}
