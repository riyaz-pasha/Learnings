/*
 * Given an n x n array of integers matrix, return the minimum sum of any
 * falling path through matrix.
 * 
 * A falling path starts at any element in the first row and chooses the element
 * in the next row that is either directly below or diagonally left/right.
 * Specifically, the next element from position (row, col) will be (row + 1, col
 * - 1), (row + 1, col), or (row + 1, col + 1).
 * 
 * Example 1:
 * Input: matrix = [[2,1,3],[6,5,4],[7,8,9]]
 * Output: 13
 * Explanation: There are two falling paths with a minimum sum as shown.
 * 
 * Example 2:
 * Input: matrix = [[-19,57],[-40,-5]]
 * Output: -59
 * Explanation: The falling path with a minimum sum is shown.
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

class MinFallingPathSum {

    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;
        int[][] dp = new int[n][n];

        for (int col = 0; col < n; col++) {
            dp[0][col] = matrix[0][col];
        }

        for (int row = 1; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int min = dp[row - 1][col];
                if (col > 0) {
                    min = Math.min(min, dp[row - 1][col - 1]);
                }
                if (col < n - 1) {
                    min = Math.min(min, dp[row - 1][col + 1]);
                }
                dp[row][col] = matrix[row][col] + min;
            }
        }

        int min = Integer.MAX_VALUE;
        for (int num : dp[n - 1]) {
            min = Math.min(min, num);
        }
        return min;
    }

}

// Solution 1: Top-Down with Memoization (Recursive)
// Time: O(n^2), Space: O(n^2)
class Solution1_TopDownMemo {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;
        Integer[][] memo = new Integer[n][n];

        int minSum = Integer.MAX_VALUE;

        // Try starting from each position in first row
        for (int col = 0; col < n; col++) {
            minSum = Math.min(minSum, dfs(matrix, 0, col, memo));
        }

        return minSum;
    }

    private int dfs(int[][] matrix, int row, int col, Integer[][] memo) {
        int n = matrix.length;

        // Out of bounds
        if (col < 0 || col >= n) {
            return Integer.MAX_VALUE;
        }

        // Base case: reached last row
        if (row == n - 1) {
            return matrix[row][col];
        }

        // Check memo
        if (memo[row][col] != null) {
            return memo[row][col];
        }

        // Three choices: left diagonal, straight down, right diagonal
        int leftDiag = dfs(matrix, row + 1, col - 1, memo);
        int straight = dfs(matrix, row + 1, col, memo);
        int rightDiag = dfs(matrix, row + 1, col + 1, memo);

        memo[row][col] = matrix[row][col] + Math.min(leftDiag, Math.min(straight, rightDiag));
        return memo[row][col];
    }
}

// Solution 2: Bottom-Up Dynamic Programming (2D)
// Time: O(n^2), Space: O(n^2)
class Solution2_BottomUp2D {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        // Create DP table
        int[][] dp = new int[n][n];

        // Initialize last row
        for (int j = 0; j < n; j++) {
            dp[n - 1][j] = matrix[n - 1][j];
        }

        // Build up from second-to-last row
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = matrix[i][j] + dp[i + 1][j]; // straight down

                // Left diagonal
                if (j > 0) {
                    dp[i][j] = Math.min(dp[i][j], matrix[i][j] + dp[i + 1][j - 1]);
                }

                // Right diagonal
                if (j < n - 1) {
                    dp[i][j] = Math.min(dp[i][j], matrix[i][j] + dp[i + 1][j + 1]);
                }
            }
        }

        // Find minimum in first row
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            result = Math.min(result, dp[0][j]);
        }

        return result;
    }
}

// Solution 3: Space Optimized Bottom-Up (1D)
// Time: O(n^2), Space: O(n)
class Solution3_SpaceOptimized1D {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        // Use current and previous row arrays
        int[] prev = matrix[n - 1].clone(); // Start with last row
        int[] curr = new int[n];

        // Build up from second-to-last row
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < n; j++) {
                curr[j] = matrix[i][j] + prev[j]; // straight down

                // Left diagonal
                if (j > 0) {
                    curr[j] = Math.min(curr[j], matrix[i][j] + prev[j - 1]);
                }

                // Right diagonal
                if (j < n - 1) {
                    curr[j] = Math.min(curr[j], matrix[i][j] + prev[j + 1]);
                }
            }

            // Swap arrays
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        // Find minimum in the result (prev after processing)
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            result = Math.min(result, prev[j]);
        }

        return result;
    }
}

// Solution 4: In-Place Modification
// Time: O(n^2), Space: O(1)
class Solution4_InPlace {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        // Modify matrix in-place, starting from second-to-last row
        for (int i = n - 2; i >= 0; i--) {
            for (int j = 0; j < n; j++) {
                int minFromBelow = matrix[i + 1][j]; // straight down

                // Left diagonal
                if (j > 0) {
                    minFromBelow = Math.min(minFromBelow, matrix[i + 1][j - 1]);
                }

                // Right diagonal
                if (j < n - 1) {
                    minFromBelow = Math.min(minFromBelow, matrix[i + 1][j + 1]);
                }

                matrix[i][j] += minFromBelow;
            }
        }

        // Find minimum in first row
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            result = Math.min(result, matrix[0][j]);
        }

        return result;
    }
}

// Solution 5: Forward DP (Top-Down Building)
// Time: O(n^2), Space: O(n^2)
class Solution5_ForwardDP {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        int[][] dp = new int[n][n];

        // Initialize first row
        for (int j = 0; j < n; j++) {
            dp[0][j] = matrix[0][j];
        }

        // Build row by row
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + matrix[i][j]; // from above

                // From left diagonal
                if (j > 0) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 1][j - 1] + matrix[i][j]);
                }

                // From right diagonal
                if (j < n - 1) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 1][j + 1] + matrix[i][j]);
                }
            }
        }

        // Find minimum in last row
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            result = Math.min(result, dp[n - 1][j]);
        }

        return result;
    }
}

// Solution 6: With Path Reconstruction
// Time: O(n^2), Space: O(n^2)
class Solution6_WithPath {
    public class Result {
        int minSum;
        List<int[]> path; // List of [row, col] positions

        Result(int sum, List<int[]> path) {
            this.minSum = sum;
            this.path = new ArrayList<>(path);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Min Sum: ").append(minSum).append("\nPath: ");
            for (int i = 0; i < path.size(); i++) {
                int[] pos = path.get(i);
                sb.append("(").append(pos[0]).append(",").append(pos[1]).append(")");
                if (i < path.size() - 1)
                    sb.append(" -> ");
            }
            return sb.toString();
        }
    }

    public Result minFallingPathSumWithPath(int[][] matrix) {
        int n = matrix.length;

        // DP table for minimum sums
        int[][] dp = new int[n][n];
        // Parent table to track path
        int[][] parent = new int[n][n];

        // Initialize first row
        for (int j = 0; j < n; j++) {
            dp[0][j] = matrix[0][j];
            parent[0][j] = -1; // No parent for first row
        }

        // Build DP table
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + matrix[i][j];
                parent[i][j] = j;

                // Check left diagonal
                if (j > 0 && dp[i - 1][j - 1] + matrix[i][j] < dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - 1] + matrix[i][j];
                    parent[i][j] = j - 1;
                }

                // Check right diagonal
                if (j < n - 1 && dp[i - 1][j + 1] + matrix[i][j] < dp[i][j]) {
                    dp[i][j] = dp[i - 1][j + 1] + matrix[i][j];
                    parent[i][j] = j + 1;
                }
            }
        }

        // Find minimum in last row and its position
        int minSum = Integer.MAX_VALUE;
        int endCol = 0;
        for (int j = 0; j < n; j++) {
            if (dp[n - 1][j] < minSum) {
                minSum = dp[n - 1][j];
                endCol = j;
            }
        }

        // Reconstruct path
        List<int[]> path = new ArrayList<>();
        int col = endCol;
        for (int row = n - 1; row >= 0; row--) {
            path.add(0, new int[] { row, col });
            if (row > 0) {
                col = parent[row][col];
            }
        }

        return new Result(minSum, path);
    }
}

// Solution 7: Using Min-Heap (Dijkstra-like)
// Time: O(n^3 * log(n^2)), Space: O(n^2)
class Solution7_MinHeap {
    private static class State {
        int row, col, sum;

        State(int row, int col, int sum) {
            this.row = row;
            this.col = col;
            this.sum = sum;
        }
    }

    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        PriorityQueue<State> pq = new PriorityQueue<>((a, b) -> a.sum - b.sum);
        Set<String> visited = new HashSet<>();

        // Add all starting positions
        for (int j = 0; j < n; j++) {
            pq.offer(new State(0, j, matrix[0][j]));
        }

        while (!pq.isEmpty()) {
            State current = pq.poll();

            String key = current.row + "," + current.col;
            if (visited.contains(key))
                continue;
            visited.add(key);

            // If reached last row
            if (current.row == n - 1) {
                return current.sum;
            }

            // Add next row positions
            int nextRow = current.row + 1;
            int[] nextCols = { current.col - 1, current.col, current.col + 1 };

            for (int nextCol : nextCols) {
                if (nextCol >= 0 && nextCol < n) {
                    String nextKey = nextRow + "," + nextCol;
                    if (!visited.contains(nextKey)) {
                        pq.offer(new State(nextRow, nextCol,
                                current.sum + matrix[nextRow][nextCol]));
                    }
                }
            }
        }

        return -1; // Should never reach here
    }
}

// Solution 8: Two-Pass Space Optimized
// Time: O(n^2), Space: O(n)
class Solution8_TwoPassOptimized {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;

        int[] dp = matrix[0].clone(); // Start with first row

        for (int i = 1; i < n; i++) {
            int[] newDp = new int[n];

            for (int j = 0; j < n; j++) {
                newDp[j] = dp[j] + matrix[i][j]; // from above

                // From left diagonal
                if (j > 0) {
                    newDp[j] = Math.min(newDp[j], dp[j - 1] + matrix[i][j]);
                }

                // From right diagonal
                if (j < n - 1) {
                    newDp[j] = Math.min(newDp[j], dp[j + 1] + matrix[i][j]);
                }
            }

            dp = newDp;
        }

        // Find minimum
        int result = Integer.MAX_VALUE;
        for (int val : dp) {
            result = Math.min(result, val);
        }

        return result;
    }
}

// Test class with comprehensive testing
class TestFallingPathSum {
    public static void main(String[] args) {
        // Initialize all solutions
        Solution1_TopDownMemo sol1 = new Solution1_TopDownMemo();
        Solution2_BottomUp2D sol2 = new Solution2_BottomUp2D();
        Solution3_SpaceOptimized1D sol3 = new Solution3_SpaceOptimized1D();
        Solution4_InPlace sol4 = new Solution4_InPlace();
        Solution5_ForwardDP sol5 = new Solution5_ForwardDP();
        Solution6_WithPath sol6 = new Solution6_WithPath();
        Solution7_MinHeap sol7 = new Solution7_MinHeap();
        Solution8_TwoPassOptimized sol8 = new Solution8_TwoPassOptimized();

        // Test case 1: [[2,1,3],[6,5,4],[7,8,9]]
        int[][] matrix1 = { { 2, 1, 3 }, { 6, 5, 4 }, { 7, 8, 9 } };

        System.out.println("Test 1 - Expected: 13");
        System.out.println("Top-Down Memo: " + sol1.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("Bottom-Up 2D: " + sol2.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("Space Optimized: " + sol3.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("In-Place: " + sol4.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("Forward DP: " + sol5.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("Min-Heap: " + sol7.minFallingPathSum(deepCopy(matrix1)));
        System.out.println("Two-Pass: " + sol8.minFallingPathSum(deepCopy(matrix1)));

        // Show path
        Solution6_WithPath.Result result1 = sol6.minFallingPathSumWithPath(deepCopy(matrix1));
        System.out.println(result1);

        // Test case 2: [[-19,57],[-40,-5]]
        int[][] matrix2 = { { -19, 57 }, { -40, -5 } };

        System.out.println("\nTest 2 - Expected: -59");
        System.out.println("Bottom-Up 2D: " + sol2.minFallingPathSum(deepCopy(matrix2)));
        System.out.println("Space Optimized: " + sol3.minFallingPathSum(deepCopy(matrix2)));

        Solution6_WithPath.Result result2 = sol6.minFallingPathSumWithPath(deepCopy(matrix2));
        System.out.println(result2);

        // Test case 3: Single element
        int[][] matrix3 = { { 5 } };

        System.out.println("\nTest 3 - Expected: 5 (single element)");
        System.out.println("Bottom-Up 2D: " + sol2.minFallingPathSum(deepCopy(matrix3)));

        // Test case 4: 2x2 matrix
        int[][] matrix4 = { { 1, 2 }, { 3, 4 } };

        System.out.println("\nTest 4 - Expected: 5 (1->4)");
        System.out.println("Bottom-Up 2D: " + sol2.minFallingPathSum(deepCopy(matrix4)));

        Solution6_WithPath.Result result4 = sol6.minFallingPathSumWithPath(deepCopy(matrix4));
        System.out.println(result4);

        // Performance test with larger matrix
        int[][] largeMatrix = generateRandomMatrix(100, -50, 50);
        long start = System.currentTimeMillis();
        int result = sol3.minFallingPathSum(largeMatrix);
        long end = System.currentTimeMillis();
        System.out.println("\nPerformance Test (100x100): " + result +
                " in " + (end - start) + "ms");
    }

    private static int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

    private static int[][] generateRandomMatrix(int n, int min, int max) {
        Random rand = new Random(42); // Fixed seed for reproducibility
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = rand.nextInt(max - min + 1) + min;
            }
        }
        return matrix;
    }
}