// is it a square matrix has different rows and cols size?
// can numbers be zero and negative?
// is it strictly increasing or can we include same number twice?
// can we move diagonally?

class LongestIncreasingPath {

    // 4 possible movement directions: down, up, right, left
    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    /**
     * Returns the length of the longest strictly increasing path
     * in the given matrix.
     *
     * Time Complexity: O(rows * cols)
     * Space Complexity: O(rows * cols)
     */
    public int longestIncreasingPath(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        // dp[r][c] = length of the longest increasing path
        // starting FROM cell (r, c)
        Integer[][] dp = new Integer[rows][cols];

        int answer = 0;

        // Try starting DFS from every cell
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                answer = Math.max(answer, dfs(matrix, r, c, dp));
            }
        }

        return answer;
    }

    /**
     * dfs(r, c) returns the longest increasing path
     * starting from cell (r, c).
     *
     * Example:
     * matrix =
     * [
     *   [9, 9, 4],
     *   [6, 6, 8],
     *   [2, 1, 1]
     * ]
     *
     * dfs(2, 1) = 1 → 2 → 6 → 9 = length 4
     */
    private int dfs(int[][] matrix, int r, int c, Integer[][] dp) {

        // ---------------- MEMOIZATION ----------------
        // If we already computed the answer for this cell,
        // return it directly to avoid recomputation.
        if (dp[r][c] != null) {
            return dp[r][c];
        }

        // At minimum, the path includes the cell itself
        int best = 1;

        // Try all 4 directions
        for (int[] d : DIRS) {
            int nr = r + d[0];
            int nc = c + d[1];

            // ---------------- BOUNDARY CHECK ----------------
            if (nr < 0 || nc < 0 ||
                nr >= matrix.length || nc >= matrix[0].length) {
                continue;
            }

            // ---------------- INCREASING CONDITION ----------------
            // We can only move to a strictly larger value
            if (matrix[nr][nc] > matrix[r][c]) {

                // If we move to (nr, nc), the path length becomes:
                // 1 (current cell) + dfs(nr, nc)
                best = Math.max(best, 1 + dfs(matrix, nr, nc, dp));
            }
        }

        // Store result before returning
        dp[r][c] = best;
        return best;
    }
}
